package kr.hhplus.be.server.api.concert;

import kr.hhplus.be.server.api.common.exception.CustomException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        assertTrue(result.contains(LocalDate.of(2025, 1, 11)));
    }

    @Test
    void 예약_가능한_좌석_목록_조회_성공() {
        // Given
        Long concertId = 1L;
        LocalDate scheduleDate = LocalDate.of(2025, 1, 10);
        List<Seat> availableSeats = List.of(
                Seat.builder().id(1L).seatNumber(1).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().id(2L).seatNumber(2).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().id(3L).seatNumber(3).status(SeatStatus.AVAILABLE).build()
        );
        when(seatRepository.findAvailableSeatList(concertId, scheduleDate)).thenReturn(availableSeats);

        // When
        List<?> result = concertService.getAvailableSeatList(concertId, scheduleDate);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void 좌석_예약_성공() {
        // Given
        Long seatId = 1L;
        Seat seat = Seat.builder().id(seatId).seatNumber(1).status(SeatStatus.AVAILABLE).concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).build();
        when(seatRepository.findByIdWithLock(seatId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = concertService.reserveSeat(seatId);

        // Then
        assertNotNull(result);
        assertEquals(SeatStatus.RESERVED, result.status());
    }

    @Test
    void 예약된_좌석_결제_성공() {
        // Given
        Long seatId = 1L;
        Seat seat = Seat.builder().id(seatId).seatNumber(1).status(SeatStatus.RESERVED).concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).build();
        when(seatRepository.findByIdWithLock(seatId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = concertService.payForSeat(seatId);

        // Then
        assertNotNull(result);
        assertEquals(SeatStatus.PAID, result.status());
    }

    @Test
    void 좌석_예약_실패_이미_예약됨() {
        // Given
        Long seatId = 1L;
        Seat seat = Seat.builder().id(seatId).seatNumber(1).status(SeatStatus.RESERVED).concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).build();
        when(seatRepository.findByIdWithLock(seatId)).thenReturn(Optional.of(seat));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> concertService.reserveSeat(seatId));
        assertEquals("SEAT_ALREADY_RESERVED", exception.getErrorCode().getName());
    }
}

