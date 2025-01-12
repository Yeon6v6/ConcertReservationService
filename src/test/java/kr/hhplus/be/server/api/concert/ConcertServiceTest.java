package kr.hhplus.be.server.api.concert;

import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.application.service.ConcertService;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    ConcertScheduleRepository concertScheduleRepository;

    @Mock
    SeatRepository seatRepository;

    @InjectMocks
    private ConcertService concertService;

    @Test
    void 예약_가능한_날짜_조회_성공() {
        // Given
        Long concertId = 1L;
        List<ConcertSchedule> schedules = List.of(
                ConcertSchedule.builder()
                        .concertId(concertId)
                        .scheduleDate(LocalDate.of(2025, 1, 10))
                        .isSoldOut(false)
                        .build(),
                ConcertSchedule.builder()
                        .concertId(concertId)
                        .scheduleDate(LocalDate.of(2025, 1, 11))
                        .isSoldOut(false)
                        .build()
        );
        when(concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, false)).thenReturn(schedules);

        // When
        List<LocalDate> result = concertService.getAvailableDateList(concertId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(LocalDate.of(2025, 1, 10)));
    }

    @Test
    void 예약_가능한_좌석_목록_조회_성공() {
        // Given
        Long concertId = 1L;
        LocalDate scheduleDate = LocalDate.of(2025, 1, 10);
        List<Seat> availableSeats = List.of(
                Seat.builder().seatNumber(1).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().seatNumber(2).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().seatNumber(3).status(SeatStatus.AVAILABLE).build()
        );
        when(seatRepository.findAvailableSeatList(concertId, scheduleDate)).thenReturn(availableSeats);

        // When
        List<Integer> result = concertService.getAvailableSeatList(concertId, scheduleDate);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
    }
}

