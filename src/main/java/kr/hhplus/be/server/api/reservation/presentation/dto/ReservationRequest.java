package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
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
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;    // 예약 할 콘서트 날짜
    private Long seatId;
    private Integer seatNo;

    /**
     * Presentation DTO -> Service DTO 변환
     */
    public ReservationCommand toCommand(Long concertId) {
        return new ReservationCommand(
                this.userId,
                this.seatId,
                this.seatNo,
                concertId,
                this.date,
                null
        );
    }
}