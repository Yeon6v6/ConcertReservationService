package kr.hhplus.be.server.api.token.application.scheduler;

import kr.hhplus.be.server.api.token.TokenConstants;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private static final int BATCH_SIZE = 100; // 한 번에 들여보낼 인원
    private final TokenQueueRepository tokenQueueRepository;
    private final TokenRepository tokenRepository;

    /**
     * 일정 주기마다 일부 인원을 대기열에서 통과
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void processQueue() {
        log.info("토큰 대기열 처리 시작: {}", Instant.now());
        Set<String> passedTokens = tokenQueueRepository.processQueue(BATCH_SIZE);
        if (passedTokens.isEmpty()) {
            log.info("처리 된 토큰이 없습니다.");
            return;
        }

        long expirationTime = Instant.now()
                .plusSeconds(TokenConstants.INITIAL_TTL_SECONDS) // 기본 10분 TTL 설정
                .getEpochSecond();
        log.info("계산된 만료 시간(에포크 초): {}", expirationTime);

        for (String tokenId : passedTokens) {
            tokenQueueRepository.removeTokenQueue(Long.parseLong(tokenId));
            tokenRepository.setTokenExpiration(Long.parseLong(tokenId), expirationTime);
            log.info("토큰 {} 처리됨: 대기열에서 제거하고 만료 시간 설정 완료.", tokenId);
        }
    }

    /**
     * 만료된 토큰 제거 (1분 주기 유지)
     */
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    public void removeExpiredTokens() {
        tokenRepository.removeExpiredTokens();
        log.info("만료된 토큰 제거 완료.");
    }

    /**
     * 오래된 대기열 데이터 정리 (10분마다 실행)
     */
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    public void removeExpiredQueueEntries() {
        tokenQueueRepository.removeExpiredQueueEntries();
        log.info("오래된 대기열 항목 정리 완료.");
    }
}
