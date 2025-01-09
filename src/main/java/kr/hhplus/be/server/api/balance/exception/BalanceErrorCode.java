package kr.hhplus.be.server.api.balance.exception;

import kr.hhplus.be.server.api.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BalanceErrorCode implements ErrorCode {
    BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "BALANCE_NOT_FOUND", "잔액 정보를 찾을 수 없습니다."),
    BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "BALANCE_INSUFFICIENT", "잔액이 부족합니다."),
    BALANCE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BALANCE_UPDATE_FAILED", "잔액 업데이트에 실패했습니다."),
    BALANCE_LOCK_FAILED(HttpStatus.CONFLICT, "BALANCE_LOCK_FAILED", "잔액 잠금 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String name;
    private final String message;

    BalanceErrorCode(HttpStatus httpStatus, String name, String message) {
        this.httpStatus = httpStatus;
        this.name = name;
        this.message = message;
    }
}