package kr.hhplus.be.server.api.reservation.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxEntity;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxRepository;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxStatus;
import kr.hhplus.be.server.api.common.kafka.producer.KafkaProducer;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatPaidEventListener {

    private final OutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    // BEFORE_COMMIT: 트랜잭션이 커밋되기 전에 Outbox 테이블에 메시지 저장
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBeforeCommit(ConcertSeatPaidEvent event) throws JsonProcessingException {
        log.info("[ReservationEventListener] 트랜잭션 커밋 전 Outbox 저장: reservationId={}", event.getReservationId());

        OutboxEntity outboxEvent = new OutboxEntity();
        outboxEvent.setTopic(event.getTopic());
        outboxEvent.setMessageKey(event.getKey());
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);

        outboxRepository.save(outboxEvent);
    }

    // AFTER_COMMIT: 트랜잭션이 성공적으로 커밋된 후 Kafka로 메시지 전송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(ConcertSeatPaidEvent event) throws JsonProcessingException {
        log.info("[ReservationEventListener] 트랜잭션 커밋 후 Kafka 발행 시작: reservationId={}", event.getReservationId());
//        kafkaProducer.sendMessage(event);
        kafkaProducer.sendMessage(event.getTopic(), event.getKey(), objectMapper.writeValueAsString(event));
    }
}
