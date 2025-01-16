package kr.hhplus.be.server.api.concert.domain.repository;

import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {
    List<ConcertSchedule> findByConcertIdAndIsSoldOut(Long concertId, boolean isSoldOut);

    @Query("SELECT cs FROM ConcertSchedule cs WHERE cs.concertId = :concertId AND cs.scheduleDate = :scheduleDate")
    Optional<ConcertSchedule> findSchedule(Long concertId, LocalDate scheduleDate);

    @Query("SELECT cs.isSoldOut FROM ConcertSchedule cs WHERE cs.concertId = :concertId AND cs.scheduleDate = :scheduleDate")
    Boolean isScheduleSoldOut(@Param("seatId") Long seatId);

}
