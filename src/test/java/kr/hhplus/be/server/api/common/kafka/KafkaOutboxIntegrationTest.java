package kr.hhplus.be.server.api.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.common.kafka.consumer.DataPlatformConsumer;
import kr.hhplus.be.server.api.common.kafka.consumer.ReservationConsumer;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxEntity;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxRepository;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxScheduler;
import kr.hhplus.be.server.api.common.kafka.outbox.OutboxStatus;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatPaidEvent;
import kr.hhplus.be.server.api.reservation.application.event.ConcertSeatReservedEvent;
import kr.hhplus.be.server.api.reservation.application.event.listener.SeatPaidEventListener;
import kr.hhplus.be.server.api.reservation.application.event.listener.SeatReservedEventListener;
import kr.hhplus.be.server.api.reservation.infrastructure.MockDataPlatformApiClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class KafkaOutboxIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 목으로 등록된 KafkaTemplate 및 MockDataPlatformApiClient를 주입받음
    @MockitoSpyBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MockDataPlatformApiClient mockDataPlatformApiClient;

    @Autowired
    private DataPlatformConsumer dataPlatformConsumer;

    @Autowired
    private ReservationConsumer reservationConsumer;

    @Autowired
    private SeatReservedEventListener seatReservedEventListener;

    @Autowired
    private SeatPaidEventListener seatPaidEventListener;

    @BeforeEach
    public void setUp() {
        outboxRepository.deleteAll();
//        Mockito.reset(kafkaTemplate, mockDataPlatformApiClient);
    }

    /**
     * [예약완료 이벤트 테스트]
     * - 트랜잭션 내에서 ConcertSeatReservedEvent가 발행되면,
     *   BEFORE_COMMIT 시점에 Outbox에 저장되고, AFTER_COMMIT 이후 KafkaProducer가 sendMessage()를 호출
     */
    @Test
    public void 예약완료_이벤트_Outbox_및_Kafka_발행_테스트() {
        // 테스트용 이벤트 생성 (reservationId=1, seatNumber=101)
        ConcertSeatReservedEvent event = new ConcertSeatReservedEvent(1L, 101);

        // 트랜잭션 내에서 이벤트 발행 → 트랜잭션 커밋 시 BEFORE/AFTER_COMMIT 이벤트 리스너 동작
        transactionTemplate.execute(status -> {
            publisher.publishEvent(event);
            return null;
        });

        // Outbox에 저장되었는지 검증
        List<OutboxEntity> outboxList = outboxRepository.findAll();
        assertFalse(outboxList.isEmpty(), "Outbox에 이벤트가 저장되어야 합니다.");
        OutboxEntity outbox = outboxList.get(0);
        assertEquals("seatReserved-topic", outbox.getTopic());
        assertEquals(String.valueOf(1L), outbox.getMessageKey());

        // AFTER_COMMIT 처리로 kafkaTemplate.send()가 호출되었는지 확인
        verify(kafkaTemplate, times(1)).send(eq(event.getTopic()), eq(event.getKey()), anyString());
    }

    /**
     * [결제완료 이벤트 테스트]
     * - 트랜잭션 내에서 ConcertSeatPaidEvent가 발행되면,
     *   BEFORE_COMMIT 시점에 Outbox 저장, AFTER_COMMIT 후 Kafka 발행
     */
    @Test
    public void 결제완료_이벤트_Outbox_및_Kafka_발행_테스트() {
        // 테스트용 이벤트 생성 (reservationId=2, userId=1001, seatId=201, paidAmount=5000)
        ConcertSeatPaidEvent event = new ConcertSeatPaidEvent(2L, 1001L, 201L, 5000L);

        transactionTemplate.execute(status -> {
            publisher.publishEvent(event);
            return null;
        });

        List<OutboxEntity> outboxList = outboxRepository.findAll();
        assertFalse(outboxList.isEmpty(), "Outbox에 이벤트가 저장되어야 합니다.");
        OutboxEntity outbox = outboxList.get(0);
        assertEquals("seatPaid-topic", outbox.getTopic());
        assertEquals(String.valueOf(2L), outbox.getMessageKey());

        verify(kafkaTemplate, times(1)).send(eq(event.getTopic()), eq(event.getKey()), anyString());
    }

    /**
     * [Outbox 재처리 및 재시도 실패 테스트]
     * - Kafka 발행 시 예외가 발생하면 OutboxScheduler가 재처리하여 retryCount가 증가하며,
     *   최대 재시도(MAX_RETRY_COUNT=5) 초과 시 DEAD 상태로 전환
     */
    @Test
    public void Outbox_재처리_및_재시도_실패_테스트() {
        // Outbox에 재처리 대상 메시지 추가
        OutboxEntity outbox = new OutboxEntity();
        outbox.setTopic("seatReserved-topic");
        outbox.setMessageKey("3");
        outbox.setPayload("dummy payload");
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outboxRepository.save(outbox);

        // kafkaTemplate.send() 호출 시 항상 예외 발생하도록 설정
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka send failure"));

        // 최대 재시도 횟수보다 많이 호출하여 상태가 DEAD로 전환되는지 확인
        for (int i = 0; i < 6; i++) {
            outboxScheduler.processOutbox();
        }

        Optional<OutboxEntity> optOutbox = outboxRepository.findByMessageKey("3");
        assertTrue(optOutbox.isPresent(), "Outbox 메시지가 존재해야 합니다.");
        OutboxEntity updatedOutbox = optOutbox.get();
        assertTrue(updatedOutbox.getRetryCount() >= 5, "최소 5회 이상 재시도되어야 합니다.");
        assertEquals(OutboxStatus.DEAD, updatedOutbox.getStatus(), "최대 재시도 후 DEAD 상태여야 합니다.");
    }

    /**
     * [데이터 플랫폼 컨슈머 테스트]
     * - DataPlatformConsumer는 서로 다른 group("DataPlatform")으로 동작하며,
     *   JSON 메시지를 역직렬화한 후, MockDataPlatformApiClient를 호출하여 API 연동을 수행
     */
    @Test
    public void 데이터_플랫폼_컨슈머_메시지_처리_테스트() throws Exception {
        // 테스트용 seatReserved 이벤트 생성 및 JSON 직렬화
        ConcertSeatReservedEvent event = new ConcertSeatReservedEvent(4L, 202);
        String messageJson = objectMapper.writeValueAsString(event);

        // Kafka ConsumerRecord 모의 생성 (topic: seatReserved-topic)
        ConsumerRecord<String, String> record = new ConsumerRecord<>("seatReserved-topic", 0, 0L, event.getKey(), messageJson);

        // 데이터 플랫폼 컨슈머의 listen() 호출
        dataPlatformConsumer.listen(record, messageJson);

        // MockDataPlatformApiClient.sendSeatReservationInfo()가 호출되었는지 검증
        verify(mockDataPlatformApiClient, times(1)).sendSeatReservationInfo(any(ConcertSeatReservedEvent.class));
    }

    /**
     * [예약 시스템 컨슈머 테스트]
     * - ReservationConsumer는 메시지 수신 후 Outbox의 상태를 SENT로 업데이트
     */
    @Test
    public void 예약_시스템_컨슈머_Outbox_상태_업데이트_테스트() throws Exception {
        // Outbox에 기존 PENDING 메시지 추가 (reservationId=5)
        OutboxEntity outbox = new OutboxEntity();
        outbox.setTopic("seatReserved-topic");
        outbox.setMessageKey("5");
        outbox.setPayload("dummy payload");
        outbox.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(outbox);

        // 테스트용 ConcertSeatReservedEvent 생성 및 JSON 직렬화
        ConcertSeatReservedEvent event = new ConcertSeatReservedEvent(5L, 303);
        String messageJson = objectMapper.writeValueAsString(event);

        // ReservationConsumer의 listen() 호출
        reservationConsumer.listen(messageJson);

        // Outbox에서 해당 메시지의 상태가 SENT로 업데이트 되었는지 검증
        Optional<OutboxEntity> optOutbox = outboxRepository.findByMessageKey("5");
        assertTrue(optOutbox.isPresent(), "Outbox 메시지가 존재해야 합니다.");
        OutboxEntity updatedOutbox = optOutbox.get();
        assertEquals(OutboxStatus.SENT, updatedOutbox.getStatus(), "메시지 상태가 SENT여야 합니다.");
    }
}
