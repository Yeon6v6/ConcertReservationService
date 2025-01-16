package kr.hhplus.be.server.api.user.domain.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.user.exception.BalanceErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column
    private Long balance; // 확장 될 경우 BigDecimal 사용

    /**
     * 잔액 충전
     */
    public void charge(Long amount) {
        this.balance += amount;
    }

    /**
     * 잔액 차감
     */
    public void deduct(Long amount) {
        if (this.balance < amount) {
            throw new CustomException(BalanceErrorCode.BALANCE_INSUFFICIENT);
        }
        this.balance -= amount;
    }

}