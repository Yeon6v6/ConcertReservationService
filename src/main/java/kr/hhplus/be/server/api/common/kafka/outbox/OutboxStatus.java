package kr.hhplus.be.server.api.common.kafka.outbox;

public enum OutboxStatus {
    PENDING,
    SENDING,
    SENT,
    DEAD
}
