package kr.hhplus.be.server.api.common.type;

public enum ReservationStatus {
    PENDING, //대기중(좌석 예약만 한 상태)
    PAID, //예약 확정(결제까지 진행 한 상태)
}