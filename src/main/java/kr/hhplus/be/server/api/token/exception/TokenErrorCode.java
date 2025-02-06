package kr.hhplus.be.server.api.token.exception;

import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TokenErrorCode implements ErrorCode {
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "TOKEN_NOT_FOUND", "토큰 정보를 찾을 수 없습니다."),
    TOKEN_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "TOKEN_INVALID_REQUEST", "요청 데이터가 유효하지 않습니다."),
    TOKEN_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_INVALID_RESPONSE", "토큰 응답이 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_MAX_EXTENSION_EXCEEDED(HttpStatus.BAD_REQUEST, "TOKEN_MAX_EXTENSION_EXCEEDED", "최대 연장 시간을 초과할 수 없습니다."),
    TOKEN_NOT_PASSED_QUEUE(HttpStatus.BAD_REQUEST, "TOKEN_NOT_IN_QUEUE", "토큰이 대기열을 통과하지 못했습니다."),
    QUEUE_POSITION_NOT_FOUND(HttpStatus.BAD_REQUEST, "QUEUE_POSITION_NOT_FOUND", "토큰 대기열 순번 조회에 실패했습니다."),
    TOKEN_NOT_ACTIVE(HttpStatus.FORBIDDEN, "TOKEN_NOT_ACTIVE", "토큰이 활성 상태가 아닙니다.");


    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    TokenErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}