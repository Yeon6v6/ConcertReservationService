package kr.hhplus.be.server.api.user.application.service;

import kr.hhplus.be.server.api.common.lock.util.RedisPublisher;
import kr.hhplus.be.server.api.user.application.dto.response.UserBalanceResult;
import kr.hhplus.be.server.api.user.domain.entity.User;
import kr.hhplus.be.server.api.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    /**
     * 사용자 잔액을 조회
     * - 사용자가 존재하지 않을 경우 기본값 0을 반환
     */
    public UserBalanceResult getBalance(Long userId) {
        User user = findOrCreateUser(userId);
        log.info("[UserService] 잔액 조회 성공 >> User ID: {}, Balance: {}", userId, user.getBalance());
        return UserBalanceResult.from(user);
    }

    /**
     * 결제 처리
     * - 잔액 확인 후 부족 시 충전
     * - 결제 가능 금액 반환
     */
    public Long processPayment(Long userId, Long totalAmount) {
        log.info("[UserService] 결제 처리 시작 >> User ID: {}, Total Amount: {}", userId, totalAmount);

        User user = findOrCreateUser(userId);

        // 부족한 금액 계산
        Long insufficientAmount = totalAmount - user.getBalance();
        if (insufficientAmount > 0) {
            user.chargeBalance(insufficientAmount);
            log.info("[UserService] 부족한 금액 충전 >> User ID: {}, Charged Amount: {}", userId, insufficientAmount);
        }

        // 잔액에서 결제 금액 차감
        user.deductBalance(totalAmount);
        userRepository.save(user);

        log.info("[UserService] 결제 처리 완료 >> User ID: {}, Total Amount: {}", userId, totalAmount);
        return totalAmount;
    }

    /**
     * 사용자 잔액을 충전 후, 현재 잔액 반환
     */
    @Transactional
    public Long chargeBalance(Long userId, Long amount) {
        log.info("[UserService] 잔액 충전 시작 >> User ID: {}, Amount: {}", userId, amount);

        User user = findOrCreateUser(userId);
        user.chargeBalance(amount);
        userRepository.save(user);

        log.info("[UserService] 잔액 충전 완료 >> User ID: {}, Current Balance: {}", userId, user.getBalance());
        return user.getBalance();
    }

    /**
     * 사용자 잔액 감소 후, 현재 잔액 반환
     */
    @Transactional
    public Long deductBalance(Long userId, Long amount) {
        log.info("[UserService] 잔액 차감 시작 >> User ID: {}, Amount: {}", userId, amount);

        User user = findOrCreateUser(userId);
        user.deductBalance(amount);
        userRepository.save(user);

        log.info("[UserService] 잔액 차감 완료 >> User ID: {}, Current Balance: {}", userId, user.getBalance());
        return user.getBalance();
    }

    /**
     * 사용자 조회 또는 생성 (공통 로직)
     */
    private User findOrCreateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> {
                    User newUser = User.builder().balance(0L).build();
                    userRepository.save(newUser);
                    log.warn("[UserService] 새로운 사용자 생성 >> User ID: {}", userId);
                    return newUser;
                });
    }
}
