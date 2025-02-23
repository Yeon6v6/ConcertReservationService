package kr.hhplus.be.server.api.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.common.exception.CustomException;

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

