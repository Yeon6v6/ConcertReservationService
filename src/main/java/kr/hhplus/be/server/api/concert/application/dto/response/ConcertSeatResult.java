package kr.hhplus.be.server.api.concert.application.dto.response;

import kr.hhplus.be.server.api.concert.domain.entity.Seat;

import java.time.LocalDate;

public record ConcertSeatResult (
    Long id,
    LocalDate scheduleId,
    int seatNumber,
    String status,
    Long price
) {
    public static ConcertSeatResult from(Seat seat){
        return new ConcertSeatResult(seat.getConcertId(), seat.getScheduleDate(), seat.getSeatNumber(), String.valueOf(seat.getStatus()), seat.getPrice());
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(this.status);
    }
}
