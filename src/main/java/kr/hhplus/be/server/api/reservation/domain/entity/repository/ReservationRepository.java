package kr.hhplus.be.server.api.reservation.domain.entity.repository;

import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 특정 콘서트와 날짜에 해당하는 예약 리스트 조회
    List<Reservation> findByConcertIdAndScheduleDate(Long concertId, LocalDate scheduleDate);

    // 특정 사용자에 대한 모든 예약 조회
    List<Reservation> findByUserId(Long userId);

    // 특정 좌석에 대한 예약 조회
    @Query("SELECT r FROM Reservation r WHERE r.concertId = :concertId AND r.scheduleDate = :scheduleDate AND r.seatNumber = :seatNumber")
    Reservation findReservation(@Param("concertId") Long concertId,
                                @Param("scheduleDate") LocalDate scheduleDate,
                                @Param("seatNumber") int seatNumber);

    // 중복 예약 확인
    boolean existsByConcertIdAndScheduleDateAndSeatNumber(Long concertId, LocalDate scheduleDate, int seatNumber);
}
