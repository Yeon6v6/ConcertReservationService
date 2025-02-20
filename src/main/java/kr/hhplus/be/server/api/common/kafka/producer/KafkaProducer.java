package kr.hhplus.be.server.api.common.kafka.producer;

import kr.hhplus.be.server.api.common.kafka.event.DomainEvent;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    public <T extends DomainEvent> void sendMessage(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message);
    }
}
