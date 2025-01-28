package kr.hhplus.be.server.api.user.application.service;

import kr.hhplus.be.server.api.common.lock.util.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRequestHandler {
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService; // UserService를 내부적으로 호출

    public void addMessageToQueue(Long userId, String action, Long amount) {
        String message = String.format("%s:%d:%d", action, userId, amount);
        redisTemplate.opsForList().rightPush("queue:user:" + userId, message); // 대기열에 메시지 추가
    }

    public void handelChargeBalance(Long userId, Long amount) {
        userService.chargeBalance(userId, amount);
    }

    public void handleDeductBalance(Long userId, Long amount) {
        userService.deductBalance(userId, amount); // UserService 호출
    }
}
