package kr.hhplus.be.server.api.reservation.application.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentServiceRequest {
    private Long userId;
    private Long reservationId;
    private Long seatId;
    private Long price;
    private String paymentMethod;
}
