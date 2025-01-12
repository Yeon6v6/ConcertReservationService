package kr.hhplus.be.server.api.reservation.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReservationServiceRequest {
    private Long concertId;
    private Long userId;
    private LocalDate scheduleDate;
    private Long seatId;
    private int seatNumber;
}
