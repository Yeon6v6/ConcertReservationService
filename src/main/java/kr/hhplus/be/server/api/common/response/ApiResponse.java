package kr.hhplus.be.server.api.common.response;

import kr.hhplus.be.server.api.common.response.dto.ResponseDTO;
import org.springframework.http.ResponseEntity;

public class ApiResponse {

    public static <T> ResponseEntity<ResponseDTO<T>> success(String message, T data) {
        return ResponseEntity.ok(new ResponseDTO<>(true, message, data));
    }

    public static ResponseEntity<ResponseDTO<Object>> error(String errorName, String message) {
        return ResponseEntity.badRequest().body(new ResponseDTO<>(false, errorName + ": " + message, null));
    }
}
