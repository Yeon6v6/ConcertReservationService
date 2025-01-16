package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.result.PaymentResult;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long reservationId;
    private String seatStatus;          // 좌석 상태
    private Long remainingBalance;      // 남은 잔액
    private Long seatPrice;             // 좌석 가격
    private Long paidAmount;            // 실제 결제 금액
    private LocalDateTime paidAt;  // 결제 시간


    public static PaymentResponse fromResult(PaymentResult result) {
        return PaymentResponse.builder()
                .reservationId(result.reservationId())
                .seatStatus(result.seatStatus())
                .remainingBalance(result.remainingBalance())
                .seatPrice(result.seatPrice())
                .paidAmount(result.paidAmount())
                .paidAt(result.paidAt())
                .build();
    }
}
