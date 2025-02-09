package kr.hhplus.be.server.api.token.application.dto.response;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

public record RedisTokenResult(Long id, String tokenValue, String status, Long queuePosition, LocalDateTime expiredAt) {

    public static RedisTokenResult of(Long id, String tokenValue, String status, Long queuePosition, LocalDateTime expiredAt) {
        return new RedisTokenResult(id, tokenValue, status, queuePosition, expiredAt);
    }

}
