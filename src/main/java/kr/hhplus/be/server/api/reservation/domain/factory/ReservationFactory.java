package kr.hhplus.be.server.api.reservation.domain.factory;

import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationFactory {

    public Reservation createReservation(ReservationCommand command) {
        return Reservation.create(
                command.userId(),
                command.seatId(),
                command.seatNumber(),
                command.concertId(),
                command.scheduleDate(),
                command.price() == null ? -1L : command.price()
        );
    }
}
