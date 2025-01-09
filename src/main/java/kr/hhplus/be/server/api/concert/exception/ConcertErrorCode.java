package kr.hhplus.be.server.api.concert.exception;

import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ConcertErrorCode implements ErrorCode {
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT_NOT_FOUND", "콘서트 정보를 찾을 수 없습니다."),
    CONCERT_FULL(HttpStatus.CONFLICT, "CONCERT_FULL", "콘서트가 매진되었습니다."),
    CONCERT_DATE_INVALID(HttpStatus.BAD_REQUEST, "CONCERT_DATE_INVALID", "유효하지 않은 콘서트 날짜입니다.");

    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    ConcertErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}
