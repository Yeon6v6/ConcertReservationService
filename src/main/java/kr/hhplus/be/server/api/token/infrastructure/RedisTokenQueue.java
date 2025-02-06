package kr.hhplus.be.server.api.token.infrastructure;

import kr.hhplus.be.server.api.token.TokenConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisTokenQueue {
    private final ZSetOperations<String, Object> zSetOperations;
    private final HashOperations<String, Object, Object> hashOperations;

    /**
     * 토큰을 대기열에 추가
     */
    public void enqueue(Long tokenId, Long userId) {
        long score = Instant.now().getEpochSecond();  // 현재 시간을 ZSET의 score로 사용
        zSetOperations.add(TokenConstants.TOKEN_QUEUE_KEY, tokenId.toString(), score);
        hashOperations.put(TokenConstants.TOKEN_USER_KEY, userId.toString(), tokenId.toString());
    }

    /**
     * 특정 토큰의 대기열 순위 조회 (ZRANK 사용)
     */
    public Long getQueuePosition(Long tokenId) {
        return zSetOperations.rank(TokenConstants.TOKEN_QUEUE_KEY, tokenId.toString());
    }

    /**
     * 사용자 ID로 대기열 내 토큰 조회
     */
    public String getTokenByUserId(Long userId) {
        return (String) hashOperations.get(TokenConstants.TOKEN_USER_KEY, userId.toString());
    }

    /**
     * 대기열에서 특정 수만큼 통과시키기
     */
    public Set<String> processQueue(int batchSize) {
        Set<ZSetOperations.TypedTuple<Object>> result =
                zSetOperations.rangeWithScores(TokenConstants.TOKEN_QUEUE_KEY, 0, batchSize - 1);

        if (result == null || result.isEmpty()) return Set.of();

        Set<String> passedTokens = result.stream()
                .map(tuple -> (String)tuple.getValue())
                .collect(Collectors.toSet());

        // 대기열에서 제거
        zSetOperations.remove(TokenConstants.TOKEN_QUEUE_KEY, passedTokens.toArray());

        return passedTokens;
    }

    /**
     * TTL 설정 (만료 시간 부여)
     */
    public void setTokenExpiration(String tokenId, long expirationTime) {
        zSetOperations.add(TokenConstants.ACTIVE_TOKENS_KEY, tokenId, expirationTime);
    }

    /**
     * 특정 토큰의 TTL 검증 (Interceptor에서 사용)
     */
    public boolean isValidToken(Long tokenId) {
        Double expirationTime = zSetOperations.score(TokenConstants.ACTIVE_TOKENS_KEY, tokenId.toString());

        // 만료 시간이 현재 시간보다 크다면 유효
        return expirationTime != null && expirationTime > Instant.now().getEpochSecond();
    }

    /**
     * 특정 토큰의 활성화 TTL 연장 (최대 30분까지)
     */
    public boolean extendTokenTTL(Long tokenId) {
        Double currentTTL = zSetOperations.score(TokenConstants.ACTIVE_TOKENS_KEY, tokenId.toString());

        if (currentTTL == null) {
            return false;
        }

        long currentTime = Instant.now().getEpochSecond();
        long maxTTLTime = currentTime + TokenConstants.MAX_TTL_SECONDS;

        long newExpiration = (long)(currentTTL + TokenConstants.TTL_INCREMENT);

        if (newExpiration > maxTTLTime) {
            newExpiration = maxTTLTime;
        }

        zSetOperations.add(TokenConstants.ACTIVE_TOKENS_KEY, tokenId.toString(), newExpiration);
        return true;
    }

    /**
     * 활성화 목록에서 만료된 토큰 제거 (Scheduler에서 사용)
     */
    public void removeExpiredTokens() {
        long currentTime = Instant.now().getEpochSecond();
        Set<Object> expiredTokens = zSetOperations.rangeByScore(TokenConstants.ACTIVE_TOKENS_KEY, 0, currentTime);

        if (expiredTokens != null) {
            for (Object tokenObj : expiredTokens) {
                String tokenId = (String) tokenObj;
                zSetOperations.remove(TokenConstants.ACTIVE_TOKENS_KEY, tokenId);

                hashOperations.entries(TokenConstants.TOKEN_USER_KEY).forEach((userId, storedTokenId) -> {
                    if (storedTokenId.equals(tokenId)) {
                        hashOperations.delete(TokenConstants.TOKEN_USER_KEY, userId);
                    }
                });
            }
        }
    }

    /**
     * 특정 토큰을 활성화 목록에서 제거
     */
    public void removeToken(Long tokenId) {
        zSetOperations.remove(TokenConstants.TOKEN_QUEUE_KEY, tokenId);
        zSetOperations.remove(TokenConstants.ACTIVE_TOKENS_KEY, tokenId);
        hashOperations.entries(TokenConstants.TOKEN_USER_KEY).forEach((userId, storedTokenId) -> {
            if (storedTokenId.equals(tokenId.toString())) {
                hashOperations.delete(TokenConstants.TOKEN_USER_KEY, userId);
            }
        });
    }
}
