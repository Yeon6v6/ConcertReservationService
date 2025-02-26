package kr.hhplus.be.server.api.concert.application.dto.response;

import kr.hhplus.be.server.api.concert.domain.entity.Seat;

import java.time.LocalDate;

public record ConcertSeatResult (
    Long id,
    Long concertId,
    LocalDate scheduleDate,
    int seatNumber,
    String status,
    Long price
) {
    public static ConcertSeatResult from(Seat seat){
        ConcertSeatResult concertSeatResult = new ConcertSeatResult(seat.getId(), seat.getConcertId(), seat.getScheduleDate(), seat.getSeatNumber(), String.valueOf(seat.getStatus()), seat.getPrice());
        return concertSeatResult;
    }
}
