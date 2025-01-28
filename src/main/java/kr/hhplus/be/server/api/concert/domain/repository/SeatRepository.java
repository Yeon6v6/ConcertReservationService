package kr.hhplus.be.server.api.concert.domain.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query("SELECT s FROM Seat s WHERE s.concertId = :concertId AND s.scheduleDate = :scheduleDate AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatList(@Param("concertId") Long concertId, @Param("scheduleDate") LocalDate scheduleDate);

    /*@Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")}) //동시성이 많을 수 있으므로 락 대기시간 설정
    Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);*/

    Optional<Seat> findById(@Param("seatId") Long Id); // 비관적 락 제거

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.concertId = :concertId AND s.scheduleDate = :scheduleDate AND s.status = 'AVAILABLE'")
    long countAvailableSeats(@Param("concertId") Long concertId, @Param("scheduleDate") LocalDate scheduleDate);
}
