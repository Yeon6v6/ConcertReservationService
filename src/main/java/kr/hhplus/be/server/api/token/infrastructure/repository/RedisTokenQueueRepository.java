package kr.hhplus.be.server.api.token.infrastructure.repository;

import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RedisTokenQueueRepository implements TokenQueueRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final HashOperations<String, Object, Object> hashOperations;

    @Override
    public void enqueue(Long tokenId, Long userId) {
        stringRedisTemplate.opsForList().rightPush(TokenConstants.TOKEN_QUEUE_KEY, tokenId.toString());
        hashOperations.put(TokenConstants.TOKEN_USER_KEY, userId.toString(), tokenId.toString()); // Hash에 저장
    }

    @Override
    public void removeToken(Long tokenId) {
        stringRedisTemplate.opsForList().remove(TokenConstants.TOKEN_QUEUE_KEY, 1, tokenId.toString());

        // Hash에서 userId에 해당하는 tokenId 삭제
        hashOperations.entries(TokenConstants.TOKEN_USER_KEY).forEach((userId, storedTokenId) -> {
            if (storedTokenId.equals(tokenId.toString())) {
                hashOperations.delete(TokenConstants.TOKEN_USER_KEY, userId);
            }
        });
    }

    @Override
    public Long getQueuePosition(Long tokenId) {
//        return stringRedisTemplate.opsForList().indexOf(TokenConstants.TOKEN_QUEUE_KEY, tokenId.toString());
        List<String> tokens = stringRedisTemplate.opsForList().range(TokenConstants.TOKEN_QUEUE_KEY, 0, -1);
        if (tokens != null) {
            int index = tokens.indexOf(tokenId.toString());
            return index >= 0 ? (long) index : null;
        }
        return null;
    }

    @Override
    public Long getTokenByUserId(Long userId) {
        String tokenId = (String) hashOperations.get(TokenConstants.TOKEN_USER_KEY, userId.toString());
        return tokenId != null ? Long.valueOf(tokenId) : null;
    }
}
