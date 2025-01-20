package kr.hhplus.be.server.api.token.domain.validator;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenValidator {

    public void validateTokenState(Token token) {
        if (token == null) {
            throw new CustomException(TokenErrorCode.TOKEN_NOT_FOUND);
        }

        // 토큰 상태가 ACTIVE가 아닌 경우
        if (token.getStatus() != TokenStatus.ACTIVE) {
            throw new CustomException(TokenErrorCode.TOKEN_NOT_ACTIVE);
        }

        // expiredAt이 null인 경우(대기열을 통과하지 못한 것으로 간주)
        if (token.getExpiredAt() == null) {
            throw new CustomException(TokenErrorCode.TOKEN_NOT_PASSED_QUEUE);
        }

        // expiredAt이 만료된 경우
        if (token.isExpired()) {
            throw new CustomException(TokenErrorCode.TOKEN_EXPIRED);
        }
    }

    public void validateTokenExtension(Token token, int addMinutes) {
        if (token.getExpiredAt() != null && token.getExpiredAt().plusMinutes(addMinutes).isAfter(token.getMaxExpiredAt())) {
            throw new CustomException(TokenErrorCode.TOKEN_MAX_EXTENSION_EXCEEDED);
        }
    }
}