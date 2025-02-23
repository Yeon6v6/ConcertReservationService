package kr.hhplus.be.server.api.common.kafka.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxEntity;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxRepository;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxStatus;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatReservedEvent;
import kr.hhplus.be.server.api.reservation.application.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationConsumer {

    private ReservationService reservationService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"seatReserved-topic", "seatPaid-topic"}, groupId = "concert")
    public void listen(String message) {
        // outbox update 처리
        log.info("[ReservationConsumer] 메시지 수신: {}", message);

        try {
            // JSON 메시지를 ConcertSeatReservedEvent 로 변환
            ConcertSeatReservedEvent event = objectMapper.readValue(message, ConcertSeatReservedEvent.class);

            // Outbox 상태 업데이트
            Optional<OutboxEntity> outboxEntityOptional = outboxRepository.findByMessageKey(String.valueOf(event.getReservationId()));

            if (outboxEntityOptional.isPresent()) {
                OutboxEntity outboxEntity = outboxEntityOptional.get();
                outboxEntity.setStatus(OutboxStatus.SENT);
                outboxRepository.save(outboxEntity);

                log.info("[ReservationConsumer] Outbox 상태 업데이트 완료: reservationId={}", event.getReservationId());
            } else {
                log.warn("[ReservationConsumer] Outbox에서 해당 메시지를 찾을 수 없음: reservationId={}", event.getReservationId());
            }

        } catch (Exception e) {
            try {
                // Outbox에서 해당 메시지를 조회
                Optional<OutboxEntity> outboxEntityOptional = outboxRepository.findByMessageKey(message);

                if (outboxEntityOptional.isPresent()) {
                    // 기존 메시지가 있다면 상태를 PENDING으로 변경 (재처리 가능하게)
                    OutboxEntity outboxEntity = outboxEntityOptional.get();
                    outboxEntity.setStatus(OutboxStatus.PENDING);
                    outboxEntity.setRetryCount(outboxEntity.getRetryCount() + 1);
                    outboxRepository.save(outboxEntity);
                    log.warn("[ReservationConsumer] 메시지를 PENDING 상태로 변경 (재처리 예정): reservationId={}", outboxEntity.getMessageKey());
                }

            } catch (Exception ex) {
                log.error("[ReservationConsumer] Outbox에 메시지를 저장하는 중 오류 발생: {}", ex.getMessage(), ex);
            }
        }
    }
}
