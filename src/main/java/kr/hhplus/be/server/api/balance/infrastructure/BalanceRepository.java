package kr.hhplus.be.server.api.balance.infrastructure;

import kr.hhplus.be.server.api.balance.domain.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, String> {
    Optional<Balance> findByUserId(Long userId);
}
