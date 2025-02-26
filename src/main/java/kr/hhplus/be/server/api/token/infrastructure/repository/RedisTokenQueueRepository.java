package kr.hhplus.be.server.api.token.infrastructure.repository;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisTokenQueueRepository implements TokenQueueRepository {
    private final ZSetOperations<String, Object> zSetOperations;
    private final HashOperations<String, Object, Object> hashOperations;

    /**
     * 사용자가 이미 대기열에 있는지 확인
     */
    @Override
    public boolean isUserInQueue(Long userId) {
        return hashOperations.hasKey(TokenConstants.TOKEN_USER_PREFIX, userId.toString());
    }

    /**
     * 대기열에 토큰 추가 (중복 방지)
     */
    @Override
    public void enqueue(Long tokenId, Long userId) {
        long score = Instant.now().getEpochSecond();
        zSetOperations.add(TokenConstants.TOKEN_QUEUE_PREFIX, tokenId.toString(), score);
        hashOperations.put(TokenConstants.TOKEN_USER_PREFIX, userId.toString(), tokenId.toString());
    }

    /**
     * 특정 사용자의 토큰 ID 조회
     */
    @Override
    public Long getTokenByUserId(Long userId) {
        String tokenId = (String) hashOperations.get(TokenConstants.TOKEN_USER_PREFIX, userId.toString());
        return tokenId != null ? Long.valueOf(tokenId) : null;
    }

    /**
     * 특정 토큰의 대기열 순위 조회 (ZSET 사용)
     */
    @Override
    public Long getQueuePosition(Long tokenId) {
        return zSetOperations.rank(TokenConstants.TOKEN_QUEUE_PREFIX, tokenId.toString());
    }

    /**
     * 대기열에서 토큰 삭제
     */
    @Override
    public void removeTokenQueue(Long tokenId) {
        zSetOperations.remove(TokenConstants.TOKEN_QUEUE_PREFIX, tokenId.toString());

        // `TOKEN_USER_PREFIX`에서 `tokenId`와 연결된 `userId` 제거
        hashOperations.entries(TokenConstants.TOKEN_USER_PREFIX).forEach((userId, storedToken) -> {
            if (storedToken.equals(tokenId.toString())) {
                hashOperations.delete(TokenConstants.TOKEN_USER_PREFIX, userId);
            }
        });
    }

    /**
     * 일정 수의 대기열을 처리 (통과된 토큰 즉시 삭제)
     */
    @Override
    public Set<String> processQueue(int batchSize) {
        Set<Object> tokens = zSetOperations.range(TokenConstants.TOKEN_QUEUE_PREFIX, 0, batchSize - 1);
        if (tokens == null || tokens.isEmpty()) return Set.of();

        tokens.forEach(token -> {
            zSetOperations.remove(TokenConstants.TOKEN_QUEUE_PREFIX, token);
            hashOperations.entries(TokenConstants.TOKEN_USER_PREFIX).forEach((userId, storedToken) -> {
                if (storedToken.equals(token.toString())) {
                    hashOperations.delete(TokenConstants.TOKEN_USER_PREFIX, userId);
                }
            });
        });

        return tokens.stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * 만료된 대기열 데이터 삭제
     */
    @Override
    public void removeExpiredQueueEntries() {
        long currentTime = Instant.now().getEpochSecond();
        Set<Object> expiredTokens = zSetOperations.rangeByScore(TokenConstants.TOKEN_QUEUE_PREFIX, 0, currentTime - TokenConstants.QUEUE_TTL_SECONDS);
        if (expiredTokens != null) {
            expiredTokens.forEach(tokenObj -> {
                zSetOperations.remove(TokenConstants.TOKEN_QUEUE_PREFIX, tokenObj);
                hashOperations.entries(TokenConstants.TOKEN_USER_PREFIX).forEach((userId, storedToken) -> {
                    if (storedToken.equals(tokenObj.toString())) {
                        hashOperations.delete(TokenConstants.TOKEN_USER_PREFIX, userId);
                    }
                });
            });
        }
    }
}
