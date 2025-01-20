package kr.hhplus.be.server.api.reservation.domain.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 특정 콘서트와 날짜에 해당하는 예약 리스트 조회
    List<Reservation> findByConcertIdAndScheduleDate(Long concertId, LocalDate scheduleDate);

    // 특정 좌석에 대한 예약 조회(Lock은 Seat가 가져감)
    @Query("SELECT r FROM Reservation r WHERE r.seatId = :seatId")
    Reservation findBySeatIdWithLock(@Param("seatId") Long seatId);

    @Query("SELECT r FROM Reservation r WHERE r.seatId = :seatId")
    List<Reservation> findBySeatId(Long seatId);
}
