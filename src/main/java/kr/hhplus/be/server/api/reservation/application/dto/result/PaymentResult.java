package kr.hhplus.be.server.api.reservation.application.dto.result;

import java.time.LocalDateTime;

public record PaymentResult(
        Long reservationId,
        String seatStatus,          // 좌석 상태
        Long remainingBalance,      // 잔액
        Long seatPrice,             // 좌석 가격
        Long paidAmount,            // 실제 결제 금액
        LocalDateTime paidAt        // 결제 시간
) {
}
