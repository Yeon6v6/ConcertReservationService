package kr.hhplus.be.server.api.common.lock.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(String channel, Object message) {
        stringRedisTemplate.convertAndSend(channel, message);
    }
}
