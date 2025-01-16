package kr.hhplus.be.server.api.reservation.domain.repository;

import kr.hhplus.be.server.api.common.type.ReservationStatus;
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

    // 특정 좌석에 대한 예약 조회
    Reservation findBySeatId(@Param("seatId") Long seatId);

    // 좌석에 대한 중복 예약 확인
    boolean existsBySeatId(Long seatId);

}
