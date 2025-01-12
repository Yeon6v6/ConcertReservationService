package kr.hhplus.be.server.api.balance.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BalanceResponse {
    private Long userId;
    private Long amount;
}
