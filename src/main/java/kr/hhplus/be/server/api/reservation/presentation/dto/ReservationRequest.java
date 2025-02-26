package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {
    private Long userId;
    private Long concertId;
    private Long seatId;
    private int seatNo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;    // 예약 할 콘서트 날짜
    private Long price;


    /**
     * Presentation DTO -> Service DTO 변환
     */
    public ReservationCommand toCommand(Long concertId) {
        if (concertId == null) {
            throw new IllegalArgumentException("concertId가 null입니다.");
        }

        return new ReservationCommand(
                this.userId,
                concertId,
                this.seatId,
                this.seatNo,
                this.date,
                null
        );
    }
}