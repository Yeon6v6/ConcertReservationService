package kr.hhplus.be.server.api.token;

import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private TokenQueueRepository tokenQueueRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Test
    void 발급받은_토큰의_순위_확인() {
        Long tokenId = 1L;
        Long rank = 3L;

        when(tokenQueueRepository.getQueuePosition(tokenId)).thenReturn(rank);

        Long result = tokenQueueRepository.getQueuePosition(tokenId);

        assertEquals(rank, result);
        verify(tokenQueueRepository).getQueuePosition(tokenId);
    }

    @Test
    void 스케줄러로_토큰_통과_확인() {
        Set<String> passedTokens = Set.of("1", "2", "3");

        when(tokenQueueRepository.processQueue(3)).thenReturn(passedTokens);

        Set<String> result = tokenQueueRepository.processQueue(3);

        assertNotNull(result);
        assertEquals(passedTokens.size(), result.size());
        verify(tokenQueueRepository).processQueue(3);
    }

    @Test
    void 만료기한_설정_및_활성_상태_검증() {
        Long tokenId = 1L;
        long expirationTime = Instant.now().getEpochSecond() + 3600;

        tokenRepository.setTokenExpiration(tokenId, expirationTime);

        verify(zSetOperations).add(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(tokenId.toString()), eq((double) expirationTime));

        when(zSetOperations.score(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(tokenId.toString()))).thenReturn((double) expirationTime);

        boolean isActive = tokenRepository.isValidToken(tokenId);

        assertTrue(isActive);
        verify(zSetOperations).score(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(tokenId.toString()));
    }

    @Test
    void 만료된_토큰_삭제_검증() {
        Set<Object> expiredTokens = Set.of("101", "102");

        when(zSetOperations.rangeByScore(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(0D), anyDouble())).thenReturn(expiredTokens);

        tokenRepository.removeExpiredTokens();

        verify(zSetOperations).rangeByScore(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(0D), anyDouble());
        expiredTokens.forEach(token -> verify(zSetOperations).remove(eq(TokenConstants.TOKEN_ACTIVE_PREFIX), eq(token)));
    }
}