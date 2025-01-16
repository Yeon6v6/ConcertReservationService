package kr.hhplus.be.server.api.user.application.service;

import kr.hhplus.be.server.api.user.application.dto.response.UserBalanceResult;
import kr.hhplus.be.server.api.user.domain.entity.User;
import kr.hhplus.be.server.api.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 잔액을 조회
     * - 사용자가 존재하지 않을 경우 기본값 0을 반환
     */
    public UserBalanceResult getBalance(Long userId) {
        // 사용자 잔액 조회
        return userRepository.findById(userId)
                // 잔액 정보가 없으면 생성 후 반환
                .map(UserBalanceResult::from) // 엔티티 => record
                .orElseGet(() -> {
                    User newBalance = User.builder()
                            .balance(0L)
                            .build();
                    userRepository.save(newBalance);
                    return UserBalanceResult.from(newBalance); // 저장 후 변환
                });
    }

    /**
     * 결제 처리
     * - 잔액 확인 후 부족 시 충전
     * - 결제 가능 금액 반환
     */
    public Long processPayment(Long userId, Long totalAmount) {
        User user = userRepository.findById(userId)
                .orElseGet(() -> {
                    User newBalance = User.builder()
                            .balance(0L)
                            .build();
                    userRepository.save(newBalance);
                    return newBalance;
                });

        // 부족한 금액 계산
        Long insufficientAmount = totalAmount - user.getBalance();
        if (insufficientAmount > 0) {
            user.chargeBalance(insufficientAmount);
        }

        // 잔액에서 결제 금액 차감
        user.deductBalance(totalAmount);
        userRepository.save(user);

        return totalAmount;
    }

    /**
     * 사용자 잔액을 충전 후, 현재 잔액 반환
     *
     * @param amount 충전 금액
     */
    @Transactional
    public Long chargeBalance(Long userId, Long amount) {
        // 사용자 잔액 조회
        User user = userRepository.findById(userId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .balance(0L)
                            .build();
                    userRepository.save(newUser);
                    return newUser;
                });

        // 잔액 충전
        user.chargeBalance(amount);

        // 변경된 잔액 정보 저장
        userRepository.save(user);

        // 충전 후의 현재 잔액 반환
        return user.getBalance();
    }
}
