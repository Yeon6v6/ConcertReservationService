package kr.hhplus.be.server.api.user.application.service;

import kr.hhplus.be.server.api.user.domain.entity.Balance;
import kr.hhplus.be.server.api.user.exception.BalanceErrorCode;
import kr.hhplus.be.server.api.user.domain.repository.BalanceRepository;
import kr.hhplus.be.server.api.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    /**
     * 사용자 잔액을 조회
     * - 사용자가 존재하지 않을 경우 기본값 0을 반환
     */
    public Balance getBalance(Long userId) {
        // 사용자 잔액 조회
        return balanceRepository.findByUserId(userId)
                // 잔액 정보가 없으면 생성 후 반환
                .orElseGet(() -> {
                    Balance newBalance = Balance.builder()
                            .userId(userId)
                            .balance(0L)
                            .build();
                    balanceRepository.save(newBalance);
                    return newBalance;
                });
    }

    /**
     * 결제 처리
     * - 잔액 확인 후 부족 시 충전
     * - 결제 가능 금액 반환
     */
    public Long processPayment(Long userId, Long totalAmount) {
        Balance balance = getBalance(userId);

        // 부족한 금액 계산
        Long insufficientAmount = totalAmount - balance.getBalance();
        if (insufficientAmount > 0) {
            balance.charge(insufficientAmount);
        }

        // 잔액에서 결제 금액 차감
        balance.deduct(totalAmount);
        balanceRepository.save(balance);

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
        Balance balance = getBalance(userId);

        // 잔액 충전
        balance.charge(amount);

        // 변경된 잔액 정보 저장
        balanceRepository.save(balance);

        // 충전 후의 현재 잔액 반환
        return balance.getBalance();
    }
}
