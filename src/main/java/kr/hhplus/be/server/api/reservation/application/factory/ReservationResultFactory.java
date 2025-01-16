package kr.hhplus.be.server.api.reservation.application.factory;

import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReservationResultFactory {

    public ReservationResult createResult(Reservation reservation) {
        return new ReservationResult(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getConcertId(),
                reservation.getScheduleDate(),
                reservation.getSeatId(),
                reservation.getSeatNumber(),
                reservation.getExpiredAt()
        );
    }
}
