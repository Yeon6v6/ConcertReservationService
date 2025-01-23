package kr.hhplus.be.server.api.concert.exception;


import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SeatErrorCode implements ErrorCode {
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND", "좌석 정보를 찾을 수 없습니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "SEAT_ALREADY_RESERVED", "이미 예약된 좌석입니다."),
    SEAT_UNAVAILABLE(HttpStatus.BAD_REQUEST, "SEAT_UNAVAILABLE", "선택한 좌석은 사용할 수 없습니다."),
    SEAT_NOT_RESERVED(HttpStatus.BAD_REQUEST, "SEAT_NOT_RESERVED", "좌석이 예약되지 않았습니다."),
    SEAT_LOCKED(HttpStatus.CONFLICT, "SEAT_LOCKED", "이미 선택된 좌석입니다.");

    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    SeatErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}
