package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.api.reservation.application.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.reservation.domain.entity.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        ReservationServiceRequest reservationRequest = ReservationServiceRequest.builder()
                .concertId(1L)
                .userId(999L)
                .seatId(1L)
                .scheduleDate(LocalDate.of(2025, 1, 10))
                .seatNumber(1)
                .build();

        // When
        Reservation reservation = reservationFacade.reserveSeat(reservationRequest);

        // Then
        assertNotNull(reservation);
        assertEquals(1L, reservation.getConcertId());
        assertEquals(LocalDate.of(2025, 1, 10), reservation.getScheduleDate());
        assertEquals(1, reservation.getSeatNumber());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        // 좌석 상태 확인
        Seat updatedSeat = seatRepository.findById(reservation.getSeatId()).orElse(null);
        assertNotNull(updatedSeat);
        assertEquals(SeatStatus.RESERVED, updatedSeat.getStatus());
    }

    @Test
    void 좌석_예약_중복_실패() {
        // Given : 좌석 예약 요청 생성
        ReservationServiceRequest reservationRequest = ReservationServiceRequest.builder()
                .concertId(1L)
                .userId(999L)
                .scheduleDate(LocalDate.of(2025, 1, 10))
                .seatNumber(1)
                .build();

        // Given : 좌석 예약
        reservationFacade.reserveSeat(reservationRequest);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reservationFacade.reserveSeat(reservationRequest);
        });
        assertEquals("이미 예약된 좌석입니다.", exception.getMessage());
    }

    @Test
    void 예약된_좌석_결제_성공() {
        // Given: 좌석 예약 요청 생성
        ReservationServiceRequest reservationRequest = ReservationServiceRequest.builder()
                .concertId(1L)
                .userId(999L)
                .seatId(1L)
                .scheduleDate(LocalDate.of(2025, 1, 10))
                .seatNumber(1)
                .build();

        Reservation reservation = reservationFacade.reserveSeat(reservationRequest);

        PaymentServiceRequest paymentRequest = PaymentServiceRequest.builder()
                .reservationId(reservation.getId())
                .userId(1L)
                .seatId(1L)
                .price(10000L)
                .build();

        // When: 좌석 결제 요청
        Reservation paidReservation = reservationFacade.payReservation(paymentRequest);

        // Then: 결제 상태 확인
        assertNotNull(paidReservation);
        assertEquals(ReservationStatus.PAID, paidReservation.getStatus());
        assertEquals(10000L, paidReservation.getPrice());

        // 결제된 좌석 상태 확인
        Seat paidSeat = seatRepository.findById(paidReservation.getSeatId()).orElse(null);
        assertNotNull(paidSeat);
        assertEquals(SeatStatus.RESERVED, paidSeat.getStatus());
    }
}