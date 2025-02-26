package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.user.application.service.UserService;
import kr.hhplus.be.server.api.user.domain.entity.User;
import kr.hhplus.be.server.api.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BalanceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().id(1L).balance(5000L).build());
    }

    @Test
    void 잔액_충전_성공() {
        // When
        userService.chargeBalance(1L, 10000L);

        // Then
        User updatedBalance = userRepository.findById(1L).orElse(null);
        assertNotNull(updatedBalance);
        assertEquals(15000L, updatedBalance.getBalance());
    }
}

