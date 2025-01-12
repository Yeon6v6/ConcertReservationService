package kr.hhplus.be.server.api.common.type;

public enum TokenStatus {
    PENDING,    // 대기열에서 대기 중
    ACTIVE,     // 작업 완료 또는 활성 상태
    EXPIRED,    // 만료된 상태
}
