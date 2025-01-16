package kr.hhplus.be.server.api.reservation.presentation.dto;

import lombok.Getter;

@Getter
public class PaymentInfo {
    private Long amount;
    private String method;
}
