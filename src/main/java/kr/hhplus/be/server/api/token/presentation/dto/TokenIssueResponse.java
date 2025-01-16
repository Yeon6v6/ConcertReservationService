package kr.hhplus.be.server.api.token.presentation.dto;

import kr.hhplus.be.server.api.token.application.dto.response.TokenResult;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenIssueResponse {
    private Long queueSort; // 대기열 순서
    private LocalDateTime expiredAt; // 만료 시간 (Epoch Second)
    private boolean hasPassedQueue; // 대기열 통과 여부

    public static TokenIssueResponse from(TokenResult tokenResult, Long queueSort, boolean hasPassedQueue) {
        return new TokenIssueResponse(
                queueSort,
                tokenResult.expiredAt(),
                hasPassedQueue
        );
    }
}
