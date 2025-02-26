package kr.hhplus.be.server.api.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 도메인 이벤트 공통 인터페이스
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface DomainEvent {
//    ObjectMapper objectMapper = new ObjectMapper();

    String getTopic();
    String getKey();

}

