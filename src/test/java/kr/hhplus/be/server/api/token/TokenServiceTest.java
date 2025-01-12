package kr.hhplus.be.server.api.token;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.application.service.TokenService;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import kr.hhplus.be.server.api.token.domain.validator.TokenValidator;
import kr.hhplus.be.server.api.token.exception.TokenErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    TokenService tokenService;

    TokenRepository tokenRepository;

    TokenValidator tokenValidator;

    //Validator로 인해 직접 주입
    @BeforeEach
    void setUp() {
        tokenValidator = new TokenValidator();
        tokenRepository = mock(TokenRepository.class);
        tokenService = new TokenService(tokenRepository, tokenValidator);
    }

    @Test
    void 대기열_토큰_발급_성공() {
        // given: 테스트 데이터와 Mock 동작 정의
        Long userId = 1L;

        // Token 객체 생성
        Token token = Token.builder()
                .id(1L)
                .userId(userId)
                .build();

        //tokenRepository.save(any(Token.class))는 save 메서드가 호출될 때 Token 객체를 어떤 값으로 전달하든(즉, "any" Token 객체로) 동작을 설정하는 의미
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        // when
        Token result = tokenService.issueToken(userId);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(userId, result.getUserId());
        verify(tokenRepository, times(1)).save(any(Token.class));

    }

    @Test
    void 존재하지_않는_토큰_검증_실패() {
        // Given
        String tokenValue = "non-existent-token";

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertEquals(TokenErrorCode.TOKEN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 유효한_토큰으로_검증_및_만료시간_연장_성공() {
        // given
        String tokenValue = "valid-token";
        Token token = Token.builder()
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .expiredAt(LocalDateTime.now().plusMinutes(5)) // 초기 만료 시간 설정
                .maxExpiredAt(LocalDateTime.now().plusMinutes(30)) // 최대 만료 시간 설정
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        // when
        tokenService.validateAndExtendToken(tokenValue);

        // then
        assertNotNull(token.getExpiredAt());
        assertTrue(token.getExpiredAt().isBefore(token.getMaxExpiredAt()));
        verify(tokenRepository, times(1)).save(token);
    }


    @Test
    void 토큰이_존재하지_않음() {
        // given
        String tokenValue = "non-existent-token";
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertEquals(TokenErrorCode.TOKEN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 토큰_상태가_ACTIVE가_아님() {
        // given
        String tokenValue = "pending-token";
        Token token = Token.builder()
                .token(tokenValue)
                .status(TokenStatus.PENDING) // 상태가 PENDING
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertEquals(TokenErrorCode.TOKEN_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void 토큰_만료됨() {
        // given
        String tokenValue = "expired-token";
        Token token = Token.builder()
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .expiredAt(LocalDateTime.now().minusMinutes(1)) // 만료된 상태
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertEquals(TokenErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void expiredAt이_null인_토큰_검증_실패() {
        // given
        String tokenValue = "pending-token";
        Token token = Token.builder()
                .token(tokenValue)
                .status(TokenStatus.ACTIVE)
                .expiredAt(null) // 대기열 통과하지 않음
                .build();

        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> tokenService.validateAndExtendToken(tokenValue));
        assertEquals(TokenErrorCode.TOKEN_NOT_PASSED_QUEUE, exception.getErrorCode());
    }

    @Test
    void expiredAt이_null인_상태에서_만료시간_연장_실패() {
        // given
        Token token = Token.builder()
                .token("pending-token")
                .status(TokenStatus.ACTIVE)
                .expiredAt(null) // 만료 시간이 설정되지 않음
                .build();

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> token.extendExpiration(5));
        assertEquals("Token has not passed the queue yet", exception.getMessage());
    }
}

