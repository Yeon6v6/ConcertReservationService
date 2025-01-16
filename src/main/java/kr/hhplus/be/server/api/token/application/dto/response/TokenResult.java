package kr.hhplus.be.server.api.token.application.dto.response;

import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.domain.entity.Token;

import java.time.LocalDateTime;

public record TokenResult(
        Long id,
        String tokenValue,
        Long userId,
        TokenStatus status,
        LocalDateTime expiredAt,    // 만료 시간
        LocalDateTime maxExpiredAt, // 절대 만료 시간
        LocalDateTime createdAt,    // 생성 시간
        LocalDateTime lastRequestAt // 마지막 요청 시간
) {
    public static TokenResult from(Token token) {
        return new TokenResult(
                token.getId(),
                token.getToken(),
                token.getUserId(),
                token.getStatus(),
                token.getExpiredAt(),
                token.getMaxExpiredAt(),
                token.getCreatedAt(),
                token.getLastRequestAt()
        );
    }
}
