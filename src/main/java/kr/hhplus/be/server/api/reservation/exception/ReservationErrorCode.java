package kr.hhplus.be.server.api.reservation.exception;

import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReservationErrorCode implements ErrorCode {
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약 정보를 찾을 수 없습니다."),
    RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "RESERVATION_ALREADY_EXISTS", "중복된 예약 정보입니다."),
    RESERVATION_EXPIRED(HttpStatus.BAD_REQUEST, "RESERVATION_EXPIRED", "예약이 만료되었습니다."),
    RESERVATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "RESERVATION_INVALID_REQUEST", "잘못된 예약 요청입니다."),
    INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_STATUS", "예약 상태가 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    ReservationErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}