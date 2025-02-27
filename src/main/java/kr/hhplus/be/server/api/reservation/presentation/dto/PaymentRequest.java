package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentRequest {
    private Long seatId;
    private Long userId;
    private PaymentInfo paymentInfo;

    public PaymentCommand toCommand(Long reservationId) {
        return new PaymentCommand(
                reservationId,
                this.seatId,
                this.userId,
                this.paymentInfo.getAmount(),
                this.paymentInfo.getMethod()
        );
    }
}
