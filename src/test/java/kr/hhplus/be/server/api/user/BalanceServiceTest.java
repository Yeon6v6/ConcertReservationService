package kr.hhplus.be.server.api.user;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BalanceServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 잔액_충전_성공() {
        // Given
        Long userId = 1L;
        User balance = User.builder().userId(userId).balance(5000L).build();
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        // When
        userService.chargeBalance(userId, 10000L);

        // Then
        assertEquals(15000L, balance.getBalance());
        verify(userRepository, times(1)).save(balance);
    }
}


