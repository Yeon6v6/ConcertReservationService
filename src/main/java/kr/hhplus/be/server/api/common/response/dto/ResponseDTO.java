package kr.hhplus.be.server.api.common.response.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResponseDTO<T> {
    private final boolean success; // 요청 성공 여부 (true: 성공, false: 실패)
    private final String message;  // 응답 메시지
    private final T data;          // 응답 데이터 (성공 시 데이터 객체, 실패 시 null)
}
