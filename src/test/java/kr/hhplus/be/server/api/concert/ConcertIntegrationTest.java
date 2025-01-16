package kr.hhplus.be.server.api.concert;

import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.application.service.ConcertService;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ConcertIntegrationTest {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @BeforeEach
    void setUp() {
        concertScheduleRepository.deleteAll();
        seatRepository.deleteAll();

        concertScheduleRepository.saveAll(List.of(
                ConcertSchedule.builder()
                        .concertId(1L)
                        .scheduleDate(LocalDate.of(2025, 1, 10))
                        .isSoldOut(false)
                        .build(),
                ConcertSchedule.builder()
                        .concertId(1L)
                        .scheduleDate(LocalDate.of(2025, 1, 11))
                        .isSoldOut(false)
                        .build()
        ));
        seatRepository.saveAll(List.of(
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(1).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(2).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(3).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(4).status(SeatStatus.RESERVED).build()
        ));
    }

    @Test
    void 예약_가능한_날짜_조회_성공() {
        // When
        List<LocalDate> result = concertService.getAvailableDateList(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void 예약_가능한_좌석_목록_조회_성공() {
        // When
        List<Integer> result = concertService.getAvailableSeatList(1L, LocalDate.of(2025, 1, 10));

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(1));
    }
}


