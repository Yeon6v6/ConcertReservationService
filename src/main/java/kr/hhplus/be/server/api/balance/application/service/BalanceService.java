package kr.hhplus.be.server.api.balance.application.service;

import kr.hhplus.be.server.api.balance.domain.entity.Balance;
import kr.hhplus.be.server.api.balance.exception.BalanceErrorCode;
import kr.hhplus.be.server.api.balance.infrastructure.BalanceRepository;
import kr.hhplus.be.server.api.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    /**
     * 사용자 잔액을 조회
     * - 사용자가 존재하지 않을 경우 기본값 0을 반환
     */
    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        // 사용자 잔액 조회
        return balanceRepository.findByUserId(userId)
                // 잔액 정보가 없으면 0 반환
                .map(Balance::getBalance)
                .orElse(0L);
    }
    
    /**
     * 사용자 잔액을 충전 후, 현재 잔액 반환
     * @param amount 충전 금액
     */
    @Transactional
    public Long chargeBalance(Long userId, Long amount) {
        // 사용자 잔액 조회
        Balance balance = balanceRepository.findByUserId(userId)
                // 잔액 정보가 없으면 새로운 Balance 객체 생성
                .orElse(Balance.builder()
                        .userId(userId)
                        .balance(0L) // 초기 잔액 0으로 설정
                        .build());

        // 잔액 충전
        balance.addAmount(amount);

        // 변경된 잔액 정보 저장
        balanceRepository.save(balance);

        // 충전 후의 현재 잔액 반환
        return balance.getBalance();
    }

    /**
     * 사용자 잔액 차감
     * @param amount 차감 금액
     */
    @Transactional
    public void deductBalance(Long userId, Long amount) {
        // 사용자 잔액 조회
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(BalanceErrorCode.BALANCE_NOT_FOUND));

        // 잔액 부족 여부 확인
        if (balance.getBalance().compareTo(amount) < 0) {
            throw new CustomException(BalanceErrorCode.BALANCE_INSUFFICIENT);
        }
        // 잔액 차감
        balance.deductAmount(amount);

        // 차감 된 잔액 정보 저장
        balanceRepository.save(balance);
    }
}