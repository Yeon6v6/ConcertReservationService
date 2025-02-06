package kr.hhplus.be.server.api.token.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.dto.response.RedisTokenResult;
import kr.hhplus.be.server.api.token.application.dto.response.TokenResult;
import kr.hhplus.be.server.api.token.application.service.TokenService;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import kr.hhplus.be.server.api.token.presentation.dto.TokenRequest;
import kr.hhplus.be.server.api.token.presentation.dto.TokenIssueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tokens")
@Tag(name = "Token", description =  "토큰 API")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/issue")
    public ResponseEntity<?> issueToken(@Valid @RequestBody TokenRequest request) {
        try {
            if (request.getUserId() == null) {
                throw new CustomException(TokenErrorCode.TOKEN_INVALID_REQUEST);
            }

            // 토큰 발급
            RedisTokenResult tokenResult = tokenService.issueToken(request.getUserId());

            System.out.println("TokenResult: " + tokenResult);

            // 대기열 순서와 대기열 통과 여부 계산
            Long queueSort = tokenResult.id(); // ID를 대기열 순서로 사용
            boolean hasPassedQueue = tokenResult.status().equals(TokenStatus.ACTIVE.toString());

            // TokenIssueResponse 생성
            TokenIssueResponse tokenIssueResponse = TokenIssueResponse.from(tokenResult, queueSort, hasPassedQueue);

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", tokenResult.tokenValue());

            // ApiResponse와 헤더를 함께 반환
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ApiResponse.success(
                            "토큰 발급 및 대기열 등록이 완료되었습니다.",
                            tokenIssueResponse
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(
                    ApiResponse.error(e.getErrorCode().getName(), e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("INTERNAL_SERVER_ERROR", "알 수 없는 오류가 발생했습니다.")
            );
        }
    }
}
