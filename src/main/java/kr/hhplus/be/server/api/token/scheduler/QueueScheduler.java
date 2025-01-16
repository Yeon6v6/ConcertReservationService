package kr.hhplus.be.server.api.token.scheduler;

import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.service.TokenService;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final TokenService tokenService;

    /**
     * 대기열에서 PENDING -> ACTIVE 상태 전환
     */
    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void processQueue() {
        tokenService.processNextInQueue();
    }

    /**
     * 10분 이상 요청 없는 ACTIVE 토큰을 EXPIRED로 처리
     */
    @Scheduled(cron = "0 */10 * * * ?") // 매 10분마다 실행
//    @Scheduled(fixedRate = 5000)
    public void expireInactiveActiveTokens() {
        tokenService.expireInactiveActiveTokens();
    }
}
