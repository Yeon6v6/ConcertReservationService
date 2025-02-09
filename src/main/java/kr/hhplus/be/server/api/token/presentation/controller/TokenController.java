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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    @PostMapping("/issue")
    public ResponseEntity<?> issueToken(@Valid @RequestBody TokenRequest request) {
        logger.info("[TOKEN CONTROLLER] 토큰 발급 요청: {}", request);
        try {
            if (request.getUserId() == null) {
                logger.error("[TOKEN CONTROLLER] userId 누락: {}", request);
                throw new CustomException(TokenErrorCode.TOKEN_INVALID_REQUEST);
            }

            if (tokenService.isUserAlreadyInQueue(request.getUserId())) {
                logger.error("[TOKEN CONTROLLER] 사용자 {}는 이미 대기열에 있습니다.", request.getUserId());
                throw new CustomException(TokenErrorCode.USER_ALREADY_IN_QUEUE);
            }

            // 토큰 발급
            RedisTokenResult tokenResult = tokenService.issueToken(request.getUserId());

            // 대기열 순서와 대기열 통과 여부 계산
            Long queueSort = tokenResult.id(); // ID를 대기열 순서로 사용
            boolean hasPassedQueue = tokenResult.status().equals(TokenStatus.ACTIVE.toString());

            // TokenIssueResponse 생성
            TokenIssueResponse tokenIssueResponse = TokenIssueResponse.from(tokenResult, queueSort, hasPassedQueue);

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", tokenResult.tokenValue());

            // ApiResponse와 헤더를 함께 반환
            logger.info("[TOKEN CONTROLLER] 토큰 발급 완료: userId={}, tokenId={}, queueSort={} ",
                    request.getUserId(), tokenResult.id(), queueSort);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ApiResponse.success(
                            "토큰 발급 및 대기열 등록이 완료되었습니다.",
                            tokenIssueResponse
                    ));
        } catch (CustomException e) {
            logger.error("[TOKEN CONTROLLER] 발급 중 오류 발생: {}, ErrorCode: {}", e.getMessage(), e.getErrorCode().getName());
            return ResponseEntity.status(e.getHttpStatus()).body(
                    ApiResponse.error(e.getErrorCode().getName(), e.getMessage())
            );
        } catch (Exception e) {
            logger.error("[TOKEN CONTROLLER] 알 수 없는 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("INTERNAL_SERVER_ERROR", "알 수 없는 오류가 발생했습니다.")
            );
        }
    }
}
