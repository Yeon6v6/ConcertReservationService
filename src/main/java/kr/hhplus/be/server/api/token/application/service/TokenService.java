package kr.hhplus.be.server.api.token.application.service;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.dto.response.TokenResult;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import kr.hhplus.be.server.api.token.domain.validator.TokenValidator;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;
    private final TokenValidator tokenValidator;

    /**
     * 토큰 발급 (초기 상태는 PENDING / 대기시간 부여 안함)
     */
    public TokenResult issueToken(Long userId) {
        Token token = Token.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .status(TokenStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Token savedToken = tokenRepository.save(token);
        return TokenResult.from(savedToken);
    }

    /**
     * 대기열 통과: 상태 변경 및 만료 시간 부여
     */
    @Transactional
    public void processNextInQueue() {
        Token nextToken = tokenRepository.findFirstByStatusOrderByIdAsc(TokenStatus.PENDING);

        if (nextToken != null) {
            LocalDateTime now = LocalDateTime.now();
            nextToken.activate(now.plusMinutes(10), now.plusMinutes(30)); // 만료 시간 설정
            tokenRepository.save(nextToken); // 기존 객체에 대해 변경 사항 저장
        }
    }

    /**
     * 요청 시 토큰 검증 및 만료 시간 연장
     */
    // 1. TokenErrorCode.TOKEN_NOT_FOUND 검증
    // 2.
    @Transactional
    public void validateAndExtendToken(String tokenValue) {
        Token token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new CustomException(TokenErrorCode.TOKEN_NOT_FOUND));

        tokenValidator.validateTokenState(token);
        tokenValidator.validateTokenExtension(token, 5);

        token.extendExpiration(5); // 만료 시간 5분 연장
        tokenRepository.save(token);
    }

    /**
     * 요청 없는 ACTIVE 토큰 처리
     * 10분 이상 요청이 없는 ACTIVE 토큰을 EXPIRED로 변경
     */
    @Transactional
    public void expireInactiveActiveTokens() {
        LocalDateTime now = LocalDateTime.now();

        // ACTIVE 상태의 모든 토큰 조회
        List<Token> tokens = tokenRepository.findAllByStatus(TokenStatus.ACTIVE);

        for (Token token : tokens) {
            if (token.getLastRequestAt().plusMinutes(10).isBefore(now)) {
                token.updateStatus(TokenStatus.EXPIRED); // 상태를 EXPIRED로 변경
                tokenRepository.save(token); // 상태 저장
            }
        }
    }

    /**
     * 특정 토큰을 즉시 만료 처리
     */
    @Transactional
    public void expireToken(Long userId) {
        Token token = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(TokenErrorCode.TOKEN_NOT_FOUND));

        token.updateStatus(TokenStatus.EXPIRED); // 상태를 EXPIRED로 변경
        tokenRepository.save(token);
    }
}
