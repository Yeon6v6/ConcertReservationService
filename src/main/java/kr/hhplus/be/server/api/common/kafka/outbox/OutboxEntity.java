package kr.hhplus.be.server.api.common.kafka.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private String messageKey;     // 메시지 key
    @Lob
    @Column(columnDefinition = "TEXT", nullable = true)
    private String payload; // 직렬화된 이벤트/메시지

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int retryCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriedAt;
}
