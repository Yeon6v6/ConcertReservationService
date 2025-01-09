package kr.hhplus.be.server.api.balance.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    public void addAmount(Long chargeAmount) {
        this.balance += chargeAmount;
    }

    public void deductAmount(Long deductAmount) {
        this.balance -= deductAmount;
    }

}