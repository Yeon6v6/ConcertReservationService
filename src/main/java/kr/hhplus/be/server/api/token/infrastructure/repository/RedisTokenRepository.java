package kr.hhplus.be.server.api.token.infrastructure.repository;

import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository implements TokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    private String tokenKey(Long tokenId) {
        return "token:id:" + tokenId;
    }

    @Override
    public Long generateTokenId() {
        return redisTemplate.opsForValue().increment("token:id:counter", 1);
    }

    @Override
    public void saveToken(Long tokenId, Long userId, String tokenValue) {
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", tokenId);
        tokenMap.put("userId", userId);
        tokenMap.put("token", tokenValue);
        tokenMap.put("status", "PENDING");
        tokenMap.put("createdAt", LocalDateTime.now().toString());

        redisTemplate.opsForHash().putAll(tokenKey(tokenId), tokenMap);
    }

    @Override
    public void saveTokenMapping(String tokenValue, Long tokenId) {
        redisTemplate.opsForValue().set("token:value:" + tokenValue, tokenId.toString());
    }

    @Override
    public void deleteToken(Long tokenId) {
        redisTemplate.delete(tokenKey(tokenId));
    }
}