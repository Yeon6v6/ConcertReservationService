package kr.hhplus.be.server.api.token.scheduler;

import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
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
    private final TokenQueueRepository tokenQueueRepository;
    private final TokenRepository tokenRepository;

    /**
     * 일정 주기마다 일부 인원을 대기열에서 통과
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void processQueue() {
        Set<String> passedTokens = tokenQueueRepository.processQueue(BATCH_SIZE);
        if (passedTokens.isEmpty()) return;

        long expirationTime = Instant.now().plusSeconds(TokenConstants.INITIAL_TTL_SECONDS).getEpochSecond(); // 기본 10분 TTL 설정

        for (String tokenId : passedTokens) {
            tokenQueueRepository.removeTokenQueue(Long.parseLong(tokenId));
            tokenRepository.setTokenExpiration(Long.parseLong(tokenId), expirationTime);
        }
    }

    /**
     * 만료된 토큰 제거 (1분 주기 유지)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void removeExpiredTokens() {
        tokenRepository.removeExpiredTokens();
    }

    /**
     * 오래된 대기열 데이터 정리 (10분마다 실행)
     */
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    public void removeExpiredQueueEntries() {
        tokenQueueRepository.removeExpiredQueueEntries();
    }

}