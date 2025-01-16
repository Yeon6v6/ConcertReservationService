package kr.hhplus.be.server.api.token;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.dto.response.TokenResult;
import kr.hhplus.be.server.api.token.application.service.TokenService;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import kr.hhplus.be.server.api.token.scheduler.QueueScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@EnableScheduling
@ActiveProfiles("test")
public class TokenIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private QueueScheduler queueScheduler;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }

    @Test
    void 대기열_토큰_발급_성공() {
        // Given
        Long userId = 1L;

        // When
        TokenResult tokenResult = tokenService.issueToken(userId);

        // Then
        assertThat(tokenResult).isNotNull();
        assertThat(tokenResult.userId()).isEqualTo(userId);
        assertThat(tokenResult.status()).isEqualTo(TokenStatus.PENDING);
    }

    @Test
    void 유효한_토큰으로_검증_및_만료시간_연장_성공() {
        // Given
        String tokenValue = "valid-token";
        Token token = Token.builder()
                .id(1L)
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .expiredAt(LocalDateTime.now().plusMinutes(5))  // 초기 만료 시간
                .maxExpiredAt(LocalDateTime.now().plusMinutes(30))  // 최대 만료 시간
                .build();
        tokenRepository.save(token);

        // When
        tokenService.validateAndExtendToken(tokenValue);

        // Then
        Token updatedToken = tokenRepository.findByToken(tokenValue).orElse(null);
        assertThat(updatedToken).isNotNull();
        assertThat(updatedToken.getExpiredAt()).isAfter(LocalDateTime.now());
        assertThat(updatedToken.getExpiredAt()).isBefore(updatedToken.getMaxExpiredAt());
    }

    @Test
    void 만료된_토큰_검증_실패() {
        // Given
        String tokenValue = "expired-token";
        Token token = Token.builder()
                .id(1L)
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .expiredAt(LocalDateTime.now().minusMinutes(1))  // 이미 만료된 상태
                .build();
        tokenRepository.save(token);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertThat(exception.getErrorCode()).isEqualTo(TokenErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 스케줄러_대기열_토큰_처리_성공() throws InterruptedException {
        // Given
        String tokenValue = "pending-token";
        Token token = Token.builder()
                .id(1L)
                .token(tokenValue)
                .status(TokenStatus.PENDING)
                .build();
        tokenRepository.save(token);

        // When
        //Thread.sleep(6000); // 스케줄러 실행 대기
        queueScheduler.processQueue();

        // Then
        Token updatedToken = tokenRepository.findByToken(tokenValue).orElse(null);
        assertThat(updatedToken).isNotNull();
        assertThat(updatedToken.getStatus()).isEqualTo(TokenStatus.ACTIVE);
    }

    @Test
    void 스케줄러_비활성화된_토큰_만료_성공() throws InterruptedException {
        // Given
        String tokenValue = "valid-token";
        Token token = Token.builder()
                .id(1L)
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .lastRequestAt(LocalDateTime.now().minusMinutes(15)) // 마지막 요청으로부터 응답 15분 지남
                .build();
        tokenRepository.save(token);

        // When
        // Thread.sleep(6000); // 스케줄러 실행 대기
        queueScheduler.expireInactiveActiveTokens();

        // Then
        Token expiredToken = tokenRepository.findByToken(tokenValue).orElse(null);
        assertThat(expiredToken).isNotNull();
        assertThat(expiredToken.getStatus()).isEqualTo(TokenStatus.EXPIRED);
    }
}
