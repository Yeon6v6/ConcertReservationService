package kr.hhplus.be.server.api.common.kafka.outbox;

import kr.hhplus.be.server.api.common.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;

    private static final int MAX_RETRY_COUNT = 5; // 최대 재시도 횟수

    @Scheduled(fixedDelay = 60000) // 60초마다 실행
    @Transactional
    public void processOutbox() {
        log.info("[OutboxScheduler] PENDING 메시지 재처리 시작");

        List<OutboxEntity> messages = outboxRepository.findByStatusInAndRetryCountLessThan(
                List.of(OutboxStatus.PENDING), MAX_RETRY_COUNT
        );

        for (OutboxEntity message : messages) {
            try {
                // 중복 방지를 위해 먼저 상태를 업데이트
                message.setStatus(OutboxStatus.SENDING);
                outboxRepository.save(message);

                kafkaProducer.sendMessage(message.getTopic(), message.getMessageKey(), message.getPayload());

                message.setLastTriedAt(LocalDateTime.now());
                outboxRepository.save(message);

                log.info("[OutboxScheduler] Outbox 메시지 id={} Kafka 재발행 완료", message.getId());
            } catch (Exception e) {
                // 재처리 횟수 증가
                message.setRetryCount(message.getRetryCount() + 1);
                message.setLastTriedAt(LocalDateTime.now());

                if (message.getRetryCount() >= MAX_RETRY_COUNT) {
                    // 최대 재시도 횟수 초과 → DEAD 상태로 변경
                    message.setStatus(OutboxStatus.DEAD);
                    log.error("[OutboxScheduler] Outbox 메시지 id={} DEAD 상태로 변경 (재시도 초과)", message.getId());
                } else {
                    // PENDING 유지 (다음 주기에 다시 재시도)
                    message.setStatus(OutboxStatus.PENDING);
                    log.info("[OutboxScheduler] Outbox 메시지 id={} 재시도 횟수={}", message.getId(), message.getRetryCount());
                }
                outboxRepository.save(message);
            }
        }
    }
}
