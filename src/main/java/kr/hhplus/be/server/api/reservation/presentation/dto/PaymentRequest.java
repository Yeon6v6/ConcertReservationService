package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentRequest {
    private Long userId;
    private PaymentInfo paymentInfo;

    /**
     * Presentation DTO -> Service DTO 변환
     */
    public PaymentServiceRequest toServiceDTO(Long reservationId) {
        return PaymentServiceRequest.builder()
                .userId(userId)
                .reservationId(reservationId)
                .price(paymentInfo.getPrice())
                .paymentMethod(paymentInfo.getMethod())
                .build();
    }
}
