package kr.hhplus.be.server.api.user;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.user.application.service.UserService;
import kr.hhplus.be.server.api.user.domain.entity.User;
import kr.hhplus.be.server.api.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder().id(1L).balance(5000L).build());
        userRepository.save(User.builder().id(2L).balance(10000L).build());
    }

    @Test
    void 잔액_충전_성공() {
        // Given
        Long userId = 1L;
        Long chargeAmount = 5000L;

        // When
        userService.chargeBalance(userId, chargeAmount);

        // Then
        User updatedUser = userRepository.findById(userId).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(10000L, updatedUser.getBalance());
    }

    @Test
    void 여러_사용자_잔액_충전_성공() {
        // Given
        Long user1Id = 1L;
        Long user2Id = 2L;

        // When
        userService.chargeBalance(user1Id, 2000L);
        userService.chargeBalance(user2Id, 3000L);

        // Then
        User user1 = userRepository.findById(user1Id).orElse(null);
        User user2 = userRepository.findById(user2Id).orElse(null);

        assertNotNull(user1);
        assertNotNull(user2);
        assertEquals(7000L, user1.getBalance());
        assertEquals(13000L, user2.getBalance());
    }

    @Test
    void 존재하지_않는_사용자_잔액_충전_실패() {
        // Given
        Long invalidUserId = 99L;

        // When & Then
        assertThrows(CustomException.class, () -> userService.chargeBalance(invalidUserId, 5000L));
    }
}

