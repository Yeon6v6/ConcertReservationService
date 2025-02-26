package kr.hhplus.be.server.api.token.presentation.dto;

import kr.hhplus.be.server.api.token.application.dto.response.RedisTokenResult;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenIssueResponse {
    private Long id;
    private String token;  // 토큰 값 추가
    private Long queuePosition;  // 대기열 순서
    private LocalDateTime expiredAt; // 만료 시간
    private String status; // 토큰 상태 추가
    private boolean hasPassedQueue; // 대기열 통과 여부

    public static TokenIssueResponse from(RedisTokenResult redisTokenResult) {
        boolean hasPassedQueue = "ACTIVE".equals(redisTokenResult.status());
        return new TokenIssueResponse(
                redisTokenResult.id(), // token Id
                redisTokenResult.tokenValue(),  // token 값
                redisTokenResult.queuePosition(),  // queuePosition 값
                redisTokenResult.expiredAt(),  // 만료 시간
                redisTokenResult.status(),  // 상태 값 (ACTIVE, WAITING 등)
                hasPassedQueue
        );
    }
}
