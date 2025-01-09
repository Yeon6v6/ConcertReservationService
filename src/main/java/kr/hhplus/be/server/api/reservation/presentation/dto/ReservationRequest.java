package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
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
    public ReservationServiceRequest toServiceDTO(Long concertId) {
        return ReservationServiceRequest.builder()
                .concertId(concertId)
                .scheduleDate(this.date)
                .seatId(this.seatId)
                .seatNumber(this.seatNo)
                .build();
    }
}