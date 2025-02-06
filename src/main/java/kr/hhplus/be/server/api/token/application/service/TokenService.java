package kr.hhplus.be.server.api.token.application.service;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.dto.response.RedisTokenResult;
import kr.hhplus.be.server.api.token.domain.repository.TokenQueueRepository;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import lombok.RequiredArgsConstructor;
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

    /**
     * 토큰 발급 (상태: PENDING)
     * - Redis INCR를 이용해 고유 토큰 ID 생성
     * - 토큰 정보를 Redis Hash에 저장
     * - 토큰 값과 토큰 ID의 매핑도 저장 (토큰 값으로 조회 시 사용)
     * - 대기열에 토큰 ID 등록
     * - 대기순위를 포함하여 반환
     */
    public RedisTokenResult issueToken(Long userId) {
        // 토큰 ID 생성
        Long tokenId = tokenRepository.generateTokenId();
        String tokenValue = UUID.randomUUID().toString();

        // Redis Hash에 토큰 정보 저장
        tokenRepository.saveToken(tokenId, userId, tokenValue);
        // 토큰 값과 토큰 ID 매핑 저장
        tokenRepository.saveTokenMapping(tokenValue, tokenId);
        // 대기열에 토큰 ID 추가
        tokenQueueRepository.enqueue(tokenId, userId);

        // 대기 순위 조회
        Long queuePosition = tokenQueueRepository.getQueuePosition(tokenId);
        if (queuePosition == null) {
            throw new CustomException(TokenErrorCode.QUEUE_POSITION_NOT_FOUND);
        }

        // RedisTokenResult DTO로 변환하여 반환
        return RedisTokenResult.of(tokenId, tokenValue, TokenStatus.PENDING.toString(), queuePosition, null);
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
        tokenQueueRepository.removeToken(tokenId); // 대기열에서도 삭제
    }
}