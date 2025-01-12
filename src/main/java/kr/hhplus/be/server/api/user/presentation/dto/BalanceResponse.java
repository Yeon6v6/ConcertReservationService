package kr.hhplus.be.server.api.user.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BalanceResponse {
    private Long userId;
    private Long amount;
}
