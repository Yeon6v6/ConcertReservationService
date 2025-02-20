package kr.hhplus.be.server.api.common.kafka.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 도메인 이벤트 공통 인터페이스
 */
public interface DomainEvent {
    String getTopic();
    String getKey();
    default String getPayload() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Payload 직렬화 실패", e);
        }
    }
}

