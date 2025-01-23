package kr.hhplus.be.server.api.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLockManager {
    private final StringRedisTemplate redisTemplate;

    /**
     * 락 획득
     */
    public boolean lock(String key, long timeoutInSeconds) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", timeoutInSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 락 해제
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}