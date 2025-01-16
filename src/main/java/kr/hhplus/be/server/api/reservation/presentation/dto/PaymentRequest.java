package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentRequest {
    private Long userId;
    private PaymentInfo paymentInfo;

    public PaymentCommand toCommand(Long reservationId) {
        return new PaymentCommand(
                this.userId,
                reservationId,
                this.paymentInfo.getAmount(),
                this.paymentInfo.getMethod()
        );
    }
}
