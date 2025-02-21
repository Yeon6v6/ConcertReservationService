package kr.hhplus.be.server.api.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "test-topic" })
@EnableKafka
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",  // Embedded Kafka의 기본 설정값 사용
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.retries=5"  // 메시지 전송 실패 시 재시도하도록 설정
})
public class KafkaIntegrationTest {
    private static final String TOPIC = "test-topic";

    // 테스트마다 새로 초기화할 latch와 메시지
    private CountDownLatch latch;
    private String receivedMessage;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    public void setUp() throws InterruptedException {
        latch = new CountDownLatch(1);
        receivedMessage = null;

        // Embedded Kafka가 완전히 실행될 시간을 줌
        Thread.sleep(3000);
    }

    /**
     * Kafka의 Consumer 역할을 수행하는 Listener
     * 메시지를 받으면 receivedMessage를 설정하고 latch를 카운트다운
     */
    @KafkaListener(topics = TOPIC, groupId = "test-group")
    public void consume(ConsumerRecord<String, String> record) {
        System.out.println("Received message: " + record.value());
        this.receivedMessage = record.value();
        latch.countDown();
    }

    /**
     * 성공 테스트 : Producer가 전송한 메시지를 Consumer가 소비함
     */
    @Test
    public void testSendMessage() throws InterruptedException {
        String testMessage = "Kafka test message";

        kafkaTemplate.send(TOPIC, testMessage);
        kafkaTemplate.flush();

        // 메시지 수신까지 대기 시간 설정
        boolean messageReceived = latch.await(15, TimeUnit.SECONDS);

        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage).isEqualTo(testMessage);
    }

    /**
     * 실패 테스트 : Producer가 전송한 메시지를 Consumer가 다른 메시지를 소비한 것
     * @throws InterruptedException
     */
    @Test
    public void testSendFailMessage() throws InterruptedException {
        String expectedMessage = "Kafka test message";
        String wrongMessage = "Wrong message";

        kafkaTemplate.send(TOPIC, wrongMessage);
        kafkaTemplate.flush();

        // 메시지 수신까지 대기 시간 설정
        boolean messageReceived = latch.await(15, TimeUnit.SECONDS);

        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage).isEqualTo(expectedMessage);
    }
}
