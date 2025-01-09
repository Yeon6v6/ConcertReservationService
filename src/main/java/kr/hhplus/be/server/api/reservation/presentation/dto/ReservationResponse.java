package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.*;

import java.time.LocalDate;
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
    private String status;
    private long reservedUntil; // Unix Timestamp로 변환된 만료 시간

    /**
     * Reservation Entity => ReservationReponse로 변환
     */
    public static ReservationResponse fromEntity(Reservation reservation) {
        return ReservationResponse.builder()
                .reservationId(reservation.getId())
                .concertId(reservation.getConcertId())
                .date(reservation.getScheduleDate())
                .seatNo(reservation.getSeatNumber())
                .status(String.valueOf(reservation.getStatus()))
                .reservedUntil(reservation.getExpiredAt().atZone(ZoneId.systemDefault()).toEpochSecond())
                .build();
    }
}
