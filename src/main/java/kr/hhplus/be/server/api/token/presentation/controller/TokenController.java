package kr.hhplus.be.server.api.token.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.common.response.dto.ResponseDTO;
import kr.hhplus.be.server.api.token.application.dto.response.RedisTokenResult;
import kr.hhplus.be.server.api.token.application.service.TokenService;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import kr.hhplus.be.server.api.token.presentation.dto.TokenIssueResponse;
import kr.hhplus.be.server.api.token.presentation.dto.TokenRequest;
import kr.hhplus.be.server.api.token.presentation.dto.TokenStatusResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tokens")
@Tag(name = "Token", description =  "토큰 API")
public class TokenController {

    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    @PostMapping("/issue")
    public ResponseEntity<ResponseDTO<Object>> issueToken(@Valid @RequestBody TokenRequest request) {
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

            // TokenIssueResponse 생성
            TokenIssueResponse tokenIssueResponse = TokenIssueResponse.from(tokenResult);

            // ApiResponse와 헤더를 함께 반환
            logger.info("[TOKEN CONTROLLER] 토큰 발급 완료: userId={}, token={}, queuePosition={}",
                    request.getUserId(), tokenResult.tokenValue(), tokenResult.queuePosition());

            // ResponseEntity.ok()를 사용하지 않고 바로 반환
            return ApiResponse.success("토큰 발급 및 대기열 등록이 완료되었습니다.", tokenIssueResponse);
        } catch (CustomException e) {
            logger.error("[TOKEN CONTROLLER] 발급 중 오류 발생: {}, ErrorCode: {}", e.getMessage(), e.getErrorCode().getName());
            return ApiResponse.error(e.getErrorCode().getName(), e.getMessage());
        } catch (Exception e) {
            logger.error("[TOKEN CONTROLLER] 알 수 없는 오류 발생", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "알 수 없는 오류가 발생했습니다.");
        }
    }

    /**
     * 토큰 상태 조회 API
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTokenStatus(@RequestParam("tokenId") Long tokenId) {
        TokenStatusResponse tokenStatus = tokenService.getTokenStatus(tokenId);
        if (tokenStatus == null) {
            return ApiResponse.error("Token Not Found", "해당 토큰이 존재하지 않습니다.");
        }
        return ApiResponse.success("토큰 상태 조회 성공", tokenStatus);
    }
}
