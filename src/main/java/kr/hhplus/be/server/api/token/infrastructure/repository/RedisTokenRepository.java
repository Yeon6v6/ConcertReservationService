package kr.hhplus.be.server.api.token.infrastructure.repository;

import kr.hhplus.be.server.api.common.cache.RedisKeyGenerator;
import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository implements TokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ZSetOperations<String, Object> zSetOperations;
    private final HashOperations<String, Object, Object> hashOperations;

    private String tokenKey(Long tokenId) {
        return RedisKeyGenerator.tokenIdKey(tokenId);
    }

    /**
     * 토큰 ID 생성
     */
    @Override
    public Long generateTokenId() {
        return redisTemplate.opsForValue().increment(TokenConstants.TOKEN_ID_PREFIX + "counter", 1);
    }

    /**
     * 토큰 저장
     */
    @Override
    public void saveToken(Long tokenId, Long userId) {
        String key = tokenKey(tokenId);
        hashOperations.put(key, "userId", userId.toString());
        hashOperations.put(key, "status", "PENDING");
    }

    /**
     * 특정 토큰의 TTL 연장 (최대 30분까지)
     */
    @Override
    public boolean extendTokenTTL(Long tokenId) {
        Double currentTTL = zSetOperations.score(TokenConstants.TOKEN_ACTIVE_PREFIX, tokenId.toString());
        if (currentTTL == null) return false;

        long newExpiration = (long) Math.min(currentTTL + TokenConstants.TTL_INCREMENT, Instant.now().getEpochSecond() + TokenConstants.MAX_TTL_SECONDS);
        zSetOperations.add(TokenConstants.TOKEN_ACTIVE_PREFIX, tokenId.toString(), newExpiration);
        return true;
    }

    /**
     * 만료기간 지정
     */
    @Override
    public void setTokenExpiration(Long tokenId, long expirationTime) {
        long newExpiration = Math.min(expirationTime, Instant.now().getEpochSecond() + TokenConstants.MAX_TTL_SECONDS);
        zSetOperations.add(TokenConstants.TOKEN_ACTIVE_PREFIX, tokenId.toString(), newExpiration);
    }

    /**
     * 특정 토큰이 유효한지 확인
     */
    @Override
    public boolean isValidToken(Long tokenId) {
        Double expirationTime = zSetOperations.score(TokenConstants.TOKEN_ACTIVE_PREFIX, tokenId.toString());
        return expirationTime != null && expirationTime > Instant.now().getEpochSecond();
    }

    /**
     * 토큰 순서 확인
     */
    @Override
    public Long getQueuePosition(Long tokenId) {
        return zSetOperations.rank(TokenConstants.TOKEN_QUEUE_PREFIX, tokenId.toString());
    }

    /**
     * 만료된 토큰 삭제
     */
    @Override
    public void removeExpiredTokens() {
        long currentTime = Instant.now().getEpochSecond();
        Set<Object> expiredTokens = zSetOperations.rangeByScore(TokenConstants.TOKEN_ACTIVE_PREFIX, 0, currentTime);
        if (expiredTokens != null) {
            expiredTokens.forEach(tokenObj -> zSetOperations.remove(TokenConstants.TOKEN_ACTIVE_PREFIX, tokenObj));
        }
    }

    /**
     * 토큰 삭제
     */
    @Override
    public void deleteToken(Long tokenId) {
        redisTemplate.delete(tokenKey(tokenId));
    }
}