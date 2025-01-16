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
        assertTrue(result.contains(LocalDate.of(2025, 1, 10)));
        assertTrue(result.contains(LocalDate.of(2025, 1, 11)));
    }

    @Test
    void 예약_가능한_좌석_목록_조회_성공() {
        // When
        List<?> result = concertService.getAvailableSeatList(1L, LocalDate.of(2025, 1, 10));

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // AVAILABLE 상태의 좌석은 3개
    }

    @Test
    void 좌석_예약_성공() {
        // Given
        Long seatId = seatRepository.findAvailableSeatList(1L, LocalDate.of(2025, 1, 10))
                .stream()
                .filter(seat -> seat.getSeatNumber() == 1)
                .findFirst()
                .map(Seat::getId)
                .orElseThrow(() -> new IllegalStateException("Seat not found"));

        // When
        var result = concertService.reserveSeat(seatId);

        // Then
        assertNotNull(result);
        assertEquals(SeatStatus.RESERVED, result.status());
    }

    @Test
    void 예약된_좌석_결제_성공() {
        // Given
        Long seatId = seatRepository.findAvailableSeatList(1L, LocalDate.of(2025, 1, 10))
                .stream()
                .filter(seat -> seat.getSeatNumber() == 4)
                .findFirst()
                .map(Seat::getId)
                .orElseThrow(() -> new IllegalStateException("Seat not found"));

        // When
        var result = concertService.payForSeat(seatId);

        // Then
        assertNotNull(result);
        assertEquals(SeatStatus.PAID, result.status());
    }
}


