package kr.hhplus.be.server.api.reservation.infrastructure;

import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatPaidEvent;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockDataPlatformApiClient implements DataPlatformApiClient {

    @Override
    public void sendSeatReservationInfo(ConcertSeatReservedEvent event) {
        log.info("[MockDataPlatformApiClient] 예약 정보 전송: reservationId={}, seatNumber={}",
                event.getReservationId(), event.getSeatNumber());
    }

    @Override
    public void sendSeatPaidInfo(ConcertSeatPaidEvent event) {
        log.info("[MockDataPlatformApiClient] 결제 정보 전송: reservationId={}, paidAmount={}",
                event.getReservationId(), event.getFinalPrice());
    }
}
