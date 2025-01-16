package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.user.application.service.UserService;
import kr.hhplus.be.server.api.user.domain.entity.User;
import kr.hhplus.be.server.api.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 잔액_충전_성공() {
        // Given
        Long userId = 1L;
        User user = User.builder().id(userId).balance(5000L).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.chargeBalance(userId, 10000L);

        // Then
        assertEquals(15000L, user.getBalance());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void 충전_금액이_음수일_경우_예외() {
        // Given
        Long userId = 1L;
        Long initialBalance = 5000L;
        Long chargeAmount = -5000L;
        User user = User.builder().id(userId).balance(initialBalance).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(CustomException.class, () -> userService.chargeBalance(userId, chargeAmount));
        verify(userRepository, never()).save(any());
    }
}


