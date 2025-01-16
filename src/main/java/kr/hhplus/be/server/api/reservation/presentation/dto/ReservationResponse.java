package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long reservationId;
    private Long concertId;
    private LocalDate date;
    private Long seatId;
    private int seatNo;
    private LocalDateTime reservedUntil;

    /**
     * ReservationResult => ReservationReponse로 변환
     */
    public static ReservationResponse fromResult(ReservationResult result) {
        return ReservationResponse.builder()
                .reservationId(result.reservationId())
                .concertId(result.concertId())
                .date(result.scheduleDate())
                .seatId(result.seatId())
                .seatNo(result.seatNumber())
                .reservedUntil(result.expiredAt())
                .build();
    }
}
