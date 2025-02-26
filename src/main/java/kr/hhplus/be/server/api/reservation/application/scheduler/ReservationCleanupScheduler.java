package kr.hhplus.be.server.api.reservation.application.scheduler;

import kr.hhplus.be.server.api.reservation.application.facade.ReservationFacade;
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
public class ReservationCleanupScheduler {

    private final ReservationFacade reservationFacade;

    /**
     * 매 5분마다 만료된 예약 정리
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanupExpiredReservations() {
        log.info("[ReservationCleanupScheduler] 만료된 예약 정리 실행");
        reservationFacade.cleanupExpiredReservations();
    }
}
