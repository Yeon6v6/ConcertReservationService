package kr.hhplus.be.server.api.user.infrastructure;

import kr.hhplus.be.server.api.common.lock.RedisLockManager;
import kr.hhplus.be.server.api.common.lock.util.RedisPublisher;
import kr.hhplus.be.server.api.common.lock.util.RedisSubscriber;
import kr.hhplus.be.server.api.user.application.service.UserRequestHandler;
import kr.hhplus.be.server.api.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMessageListener implements RedisSubscriber {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisPublisher redisPublisher;
    private final UserRequestHandler userRequestHandler;
    private final RedisLockManager lockManager;

    @Override
    public void handleMessage(String message) {
        System.out.println("[Listener] 메시지 처리 시작: " + message);
        String[] parts = message.split(":");
        String action = parts[0];
        Long userId = Long.parseLong(parts[1]);
        Long amount = parts.length > 2 ? Long.parseLong(parts[2]) : null;

        String lockKey = "lock:user_balance:" + userId;

        if ("COMPLETE".equals(action)) {
            // Lock 해제 및 다음 메시지 처리
            lockManager.unlock(lockKey);
            processNextMessage(userId);
            return;
        }

        // 다른 요청이 이미 처리 중인 경우는 무시
        if(!lockManager.lock(lockKey, 10)) {
            return;
        }

        try{
            if("CHARGE".equals(action)) {
                userRequestHandler.handelChargeBalance(userId, amount);
            }else if("DEDUCT".equals(action)) {
                userRequestHandler.handleDeductBalance(userId, amount);
            }

            // COMPLETE 메시지 발행
            redisPublisher.publish("user:balance", String.format("COMPLETE:%d", userId));

        }finally {
            lockManager.unlock(lockKey);
        }
    }

    private void processNextMessage(Long userId) {
        String nextMessage = redisTemplate.opsForList().leftPop("queue:user:" + userId); // 대기열에서 다음 메시지 가져오기
        if (nextMessage != null) {
            handleMessage(nextMessage); // 다음 메시지 처리
        }
    }

}
