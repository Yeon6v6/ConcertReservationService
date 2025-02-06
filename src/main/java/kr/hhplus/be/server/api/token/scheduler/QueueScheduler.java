package kr.hhplus.be.server.api.token.scheduler;

import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.infrastructure.RedisTokenQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private static final int BATCH_SIZE = 10; // 한 번에 들여보낼 인원
    private final RedisTokenQueue redisTokenQueue;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 일정 주기마다 일부 인원을 대기열에서 통과
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void processQueue() {
        Set<String> passedTokens = redisTokenQueue.processQueue(BATCH_SIZE);
        if (passedTokens.isEmpty()) return;

        long expirationTime = Instant.now().plusSeconds(600).getEpochSecond(); // 10분 TTL 설정

        for (String tokenId : passedTokens) {
            redisTemplate.opsForZSet().remove(TokenConstants.TOKEN_QUEUE_KEY, tokenId); // 대기열에서 제거
            // 통과한 토큰에 TTL(만료 시간) 부여
            redisTemplate.opsForHash().put(TokenConstants.TOKEN_ID_PREFIX + tokenId, "expiredAt", expirationTime);
            redisTokenQueue.setTokenExpiration(tokenId, expirationTime);
        }
    }

    /**
     * 만료된 사용자 제거
     */
    @Scheduled(fixedRate = 60000)
    public void removeExpiredTokens() {
        redisTokenQueue.removeExpiredTokens();
    }

}