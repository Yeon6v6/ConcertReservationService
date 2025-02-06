package kr.hhplus.be.server.api.token;

import kr.hhplus.be.server.api.token.infrastructure.RedisTokenQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RedisTokenQueue redisTokenQueue;

    @BeforeEach
    void 초기화() {
        redisTokenQueue = new RedisTokenQueue(zSetOperations, hashOperations);
    }

    // 1) 발급받고 순위 확인하기
    @Test
    void 발급받은_토큰의_순위_확인() {
        Long tokenId = 100L;
        Long userId = 200L;

        // 토큰 발급 (대기열에 추가)
        redisTokenQueue.enqueue(tokenId, userId);

        // 순위를 조회할 때 0L(첫번째)을 반환하도록
        when(zSetOperations.rank(TokenConstants.TOKEN_QUEUE_KEY, tokenId.toString())).thenReturn(0L);

        Long rank = redisTokenQueue.getQueuePosition(tokenId);
        assertNotNull(rank);
        assertEquals(0L, rank);

        verify(zSetOperations).add(eq(TokenConstants.TOKEN_QUEUE_KEY), eq(tokenId.toString()), anyDouble());
        verify(hashOperations).put(eq(TokenConstants.TOKEN_USER_KEY), eq(userId.toString()), eq(tokenId.toString()));
        verify(zSetOperations).rank(eq(TokenConstants.TOKEN_QUEUE_KEY), eq(tokenId.toString()));
    }

    // 2) 스케줄러 돌려서 일정 토큰 통과되었는지 확인하기
    @Test
    void 스케줄러로_토큰_통과_확인() {
        int batchSize = 2;
        String tokenString = "101";

        // processQueue에서 사용할 TypedTuple 모의 객체 생성
        ZSetOperations.TypedTuple<Object> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn(tokenString);
        Set<ZSetOperations.TypedTuple<Object>> tupleSet = Set.of(tuple);

        when(zSetOperations.rangeWithScores(TokenConstants.TOKEN_QUEUE_KEY, 0, batchSize - 1))
                .thenReturn(tupleSet);

        Set<String> passedTokens = redisTokenQueue.processQueue(batchSize);
        assertNotNull(passedTokens);
        assertTrue(passedTokens.contains(tokenString));

        verify(zSetOperations).remove(eq(TokenConstants.TOKEN_QUEUE_KEY), any());
    }

    // 3) 통과된 토큰의 만료기한이 잘 설정되었는지, 상태가 Active인지 조회
    @Test
    void 만료기한_설정_및_활성_상태_검증() {
        Long tokenId = 101L;
        long expirationTime = Instant.now().getEpochSecond() + 3600; // 현재 시간 기준 1시간 후

        redisTokenQueue.setTokenExpiration(tokenId.toString(), expirationTime);

        // 만료시간보다 미래이면 Active 상태로 간주하도록 stubbing
        when(zSetOperations.score(TokenConstants.ACTIVE_TOKENS_KEY, tokenId.toString()))
                .thenReturn((double) expirationTime);

        boolean isActive = redisTokenQueue.isValidToken(tokenId);
        assertTrue(isActive);

        verify(zSetOperations).add(eq(TokenConstants.ACTIVE_TOKENS_KEY), eq(tokenId.toString()), eq((double) expirationTime));
        verify(zSetOperations).score(eq(TokenConstants.ACTIVE_TOKENS_KEY), eq(tokenId.toString()));
    }

    // 4) 만료된 토큰이 스케줄러로 잘 삭제되는지 조회하기
    @Test
    void 만료된_토큰_삭제_검증() {
        // 만료된 토큰 목록 예시
        Set<Object> expiredTokens = Set.of("202");
        when(zSetOperations.rangeByScore(eq(TokenConstants.ACTIVE_TOKENS_KEY), eq(0L), anyLong()))
                .thenReturn(expiredTokens);

        // 사용자-토큰 매핑 예시: "202"에 해당하는 user1만 삭제되어야 함
        Map<Object, Object> userTokenMap = new HashMap<>();
        userTokenMap.put("user1", "202");
        userTokenMap.put("user2", "otherToken");
        when(hashOperations.entries(TokenConstants.TOKEN_USER_KEY)).thenReturn(userTokenMap);

        redisTokenQueue.removeExpiredTokens();

        verify(zSetOperations, times(1)).remove(TokenConstants.ACTIVE_TOKENS_KEY, "202");
        verify(hashOperations, times(1)).delete(TokenConstants.TOKEN_USER_KEY, "user1");
        verify(hashOperations, never()).delete(TokenConstants.TOKEN_USER_KEY, "user2");
    }
}