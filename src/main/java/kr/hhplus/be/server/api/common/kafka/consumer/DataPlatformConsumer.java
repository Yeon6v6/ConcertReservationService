package kr.hhplus.be.server.api.common.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatPaidEvent;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatReservedEvent;
import kr.hhplus.be.server.api.reservation.infrastructure.MockDataPlatformApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataPlatformConsumer {

    private final MockDataPlatformApiClient mockDataPlatformApiClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"seatReserved-topic", "seatPaid-topic"}, groupId = "DataPlatform")
    public void listen(ConsumerRecord<String, String> record, String message) {
        String topic = record.topic();
        log.info("[DataPlatformConsumer] 메시지 수신: {}", message);

        try {
            // Mock API 호출
            if("seatReserved-topic".equals(topic)){
                ConcertSeatReservedEvent event = objectMapper.readValue(message, ConcertSeatReservedEvent.class);
                mockDataPlatformApiClient.sendSeatReservationInfo(event);
            }else if("seatPaid-topic".equals(topic)){
                ConcertSeatPaidEvent event = objectMapper.readValue(message, ConcertSeatPaidEvent.class);
                mockDataPlatformApiClient.sendSeatPaidInfo(event);
            }else{
                log.warn("[ReservationConsumer] 알 수 없는 토픽에서 메시지 수신: topic={}", topic);
            }
        } catch (Exception e) {
            log.error("[DataPlatformConsumer] 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
