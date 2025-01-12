package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.user.application.service.BalanceService;
import kr.hhplus.be.server.api.user.domain.entity.Balance;
import kr.hhplus.be.server.api.user.domain.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BalanceIntegrationTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceRepository balanceRepository;

    @BeforeEach
    void setUp() {
        balanceRepository.deleteAll();
        balanceRepository.save(Balance.builder().userId(1L).balance(5000L).build());
    }

    @Test
    void 잔액_충전_성공() {
        // When
        balanceService.chargeBalance(1L, 10000L);

        // Then
        Balance updatedBalance = balanceRepository.findByUserId(1L).orElse(null);
        assertNotNull(updatedBalance);
        assertEquals(15000L, updatedBalance.getBalance());
    }
}

