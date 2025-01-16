package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.PaymentResult;
import kr.hhplus.be.server.api.reservation.application.facade.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.reservation.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReservationIntegrationTest {

    @Autowired
    private ReservationFacade reservationFacade;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        seatRepository.deleteAll();
        concertScheduleRepository.deleteAll();
        reservationRepository.deleteAll();

        concertScheduleRepository.save(ConcertSchedule.builder()
                .concertId(1L)
                .scheduleDate(LocalDate.of(2025, 1, 10))
                .isSoldOut(false)
                .build());

        seatRepository.saveAll(List.of(
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(1).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(2).status(SeatStatus.AVAILABLE).build(),
                Seat.builder().concertId(1L).scheduleDate(LocalDate.of(2025, 1, 10)).seatNumber(20).status(SeatStatus.AVAILABLE).build()
        ));

        List<Seat> seats = seatRepository.findAll();
        System.out.println("Seats: " + seats);
    }

    @Test
    void 좌석_예약_성공() {
        // Given
        ReservationCommand reservationCommand = ReservationCommand.of(
                1L, 1L, 1, 1L, LocalDate.of(2025, 1, 10), 10000L
        );

        // When
        ReservationResult reservationResult = reservationFacade.reserveSeat(reservationCommand);

        // Then
        assertNotNull(reservationResult);
        assertEquals(1L, reservationResult.concertId());
        assertEquals(LocalDate.of(2025, 1, 10), reservationResult.scheduleDate());
        assertEquals(1, reservationResult.seatNumber());

        // 좌석 상태 확인
        Seat updatedSeat = seatRepository.findById(reservationResult.seatId()).orElse(null);
        assertNotNull(updatedSeat);
        assertEquals(SeatStatus.RESERVED, updatedSeat.getStatus());
    }

    @Test
    void 좌석_예약_중복_실패() {
        // Given
        ReservationCommand reservationCommand = ReservationCommand.of(
                1L, 1L, 1, 1L, LocalDate.of(2025, 1, 10), 10000L
        );

        reservationFacade.reserveSeat(reservationCommand);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> reservationFacade.reserveSeat(reservationCommand));
        assertEquals("이미 예약된 좌석입니다.", exception.getMessage());
    }

    @Test
    void 예약된_좌석_결제_성공() {
        // Given
        ReservationCommand reservationCommand = ReservationCommand.of(
                1L, 1L, 1, 1L, LocalDate.of(2025, 1, 10), 10000L
        );
        ReservationResult reservationResult = reservationFacade.reserveSeat(reservationCommand);

        PaymentCommand paymentCommand = new PaymentCommand(
                1L, reservationResult.reservationId(), 10000L, "CREDIT_CARD"
        );

        // When
        PaymentResult paymentResult = reservationFacade.payReservation(paymentCommand);

        // Then
        assertNotNull(paymentResult);
        assertEquals("PAID", paymentResult.seatStatus());
        assertEquals(10000L, paymentResult.seatPrice());
        assertEquals(10000L, paymentResult.paidAmount());

        // 결제된 좌석 상태 확인
        Seat paidSeat = seatRepository.findById(reservationResult.seatId()).orElse(null);
        assertNotNull(paidSeat);
        assertEquals(SeatStatus.PAID, paidSeat.getStatus());
    }
}