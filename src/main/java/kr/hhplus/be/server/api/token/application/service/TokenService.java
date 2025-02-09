package kr.hhplus.be.server.api.token.application.service;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.dto.response.RedisTokenResult;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import kr.hhplus.be.server.api.token.presentation.controller.TokenController;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;
    private final TokenQueueRepository tokenQueueRepository;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    /**
     * 토큰 발급 (상태: PENDING)
     */
    public RedisTokenResult issueToken(Long userId) {
        logger.info("[TOKEN ISSUE] 사용자 {}가 토큰 발급을 요청함", userId);

        if (isUserAlreadyInQueue(userId)) {
            logger.error("[TOKEN ISSUE] 사용자 {}는 이미 대기열에 등록되어 있음", userId);
            throw new CustomException(TokenErrorCode.USER_ALREADY_IN_QUEUE);
        }

        // 토큰 ID 생성
        Long tokenId = tokenRepository.generateTokenId();
        String tokenValue = UUID.randomUUID().toString();
        logger.info("[TOKEN ISSUE] 생성된 토큰 ID: {}, 사용자 ID: {}", tokenId, userId);

        // Redis Hash에 토큰 정보 저장
        tokenRepository.saveToken(tokenId, userId);
        logger.debug("[TOKEN ISSUE] 토큰 정보 저장 완료: {}", tokenId);

        // 대기열에 토큰 ID 추가
        tokenQueueRepository.enqueue(tokenId, userId);
        logger.info("[TOKEN ISSUE] 토큰 {}이(가) 대기열에 추가됨", tokenId);

        // 대기 순위 조회
        Long queuePosition = tokenQueueRepository.getQueuePosition(tokenId);
        logger.info("[TOKEN ISSUE] 토큰 {}의 대기 순위: {}", tokenId, queuePosition);

        if (queuePosition == null) {
            logger.error("[TOKEN ISSUE] 토큰 {}이 대기열에 정상적으로 등록되지 않음", tokenId);
            throw new CustomException(TokenErrorCode.QUEUE_POSITION_NOT_FOUND);
        }

        return RedisTokenResult.of(tokenId, tokenValue, TokenStatus.PENDING.toString(), queuePosition, null);
    }

    /**
     * 해당 사용자가 이미 대기열에 있는지 확인
     */
    public boolean isUserAlreadyInQueue(Long userId) {
        return tokenQueueRepository.isUserInQueue(userId);
    }

    /**
     * 대기열에서 userId 기반으로 TokenId 조회
     */
    public Long getTokenIdByUserId(Long userId) {
        return tokenQueueRepository.getTokenByUserId(userId);
    }

    /**
     * 토큰 강제 만료
     */
    public void expireToken(Long tokenId) {
        tokenRepository.deleteToken(tokenId); // Redis에서 삭제하여 만료 처리
        tokenQueueRepository.removeTokenQueue(tokenId); // 대기열에서도 삭제
    }
}