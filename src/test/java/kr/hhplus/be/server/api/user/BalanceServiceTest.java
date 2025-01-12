package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.user.application.service.BalanceService;
import kr.hhplus.be.server.api.user.domain.entity.Balance;
import kr.hhplus.be.server.api.user.domain.repository.BalanceRepository;
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
    private BalanceRepository balanceRepository;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    void 잔액_충전_성공() {
        // Given
        Long userId = 1L;
        Balance balance = Balance.builder().userId(userId).balance(5000L).build();
        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        // When
        balanceService.chargeBalance(userId, 10000L);

        // Then
        assertEquals(15000L, balance.getBalance());
        verify(balanceRepository, times(1)).save(balance);
    }
}


