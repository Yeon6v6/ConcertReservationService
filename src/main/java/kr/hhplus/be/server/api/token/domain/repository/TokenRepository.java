package kr.hhplus.be.server.api.token.domain.repository;

import kr.hhplus.be.server.api.common.type.TokenStatus;
import kr.hhplus.be.server.api.token.domain.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    List<Token> findByStatusAndCreatedAtBefore(TokenStatus status, LocalDateTime time);

    @Query("SELECT t FROM Token t WHERE t.status = :status AND t.lastRequestAt < :time")
    List<Token> findByStatusAndLastRequestAtBefore(
            @Param("status") TokenStatus status,
            @Param("time") LocalDateTime time
    );

    Token findFirstByStatusOrderByIdAsc(TokenStatus status);

    Optional<Token> findByToken(String tokenValue);

    List<Token> findAllByStatus(TokenStatus tokenStatus);
}
