package kr.hhplus.be.server.api.reservation.application.dto.command;

import java.time.LocalDate;

public record ReservationCommand (
        Long userId,
        Long concertId,
        Long seatId,
        int seatNumber,
        LocalDate scheduleDate,
        Long price
){
    public static ReservationCommand of(
            Long userId,
            Long seatId,
            int seatNumber,
            Long concertId,
            LocalDate scheduleDate,
            Long price
    ) {
        return new ReservationCommand(userId, concertId, seatId, seatNumber, scheduleDate, price);
    }
}
