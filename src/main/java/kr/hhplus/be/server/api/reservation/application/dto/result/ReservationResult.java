package kr.hhplus.be.server.api.reservation.application.dto.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResult (
        Long reservationId,
        Long userId,
        Long concertId,
        LocalDate scheduleDate,
        Long seatId,
        int seatNumber,
        LocalDateTime expiredAt
){
}
