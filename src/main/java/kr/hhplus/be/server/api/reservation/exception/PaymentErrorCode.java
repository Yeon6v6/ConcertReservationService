package kr.hhplus.be.server.api.reservation.exception;

import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_FAILED", "결제 처리에 실패했습니다."),
    PAYMENT_DUPLICATE(HttpStatus.CONFLICT, "PAYMENT_DUPLICATE", "중복 결제가 감지되었습니다.");

    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    PaymentErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}