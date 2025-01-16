package kr.hhplus.be.server.api.concert.domain.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query("SELECT s FROM Seat s WHERE s.concertId = :concertId AND s.scheduleDate = :scheduleDate AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatList(@Param("concertId") Long concertId, @Param("scheduleDate") LocalDate scheduleDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.concertId = :concertId AND s.scheduleDate = :scheduleDate AND s.status = 'AVAILABLE'")
    long countAvailableSeats(@Param("concertId") Long concertId, @Param("scheduleDate") LocalDate scheduleDate);
}
