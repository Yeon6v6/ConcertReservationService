package kr.hhplus.be.server.api.token.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenStatusResponse {
    private Long tokenId;
    private String status; // 예: "PENDING", "ACTIVE"
    private Long ttl;      // 남은 TTL (초 단위)
}
