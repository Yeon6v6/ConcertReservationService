package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
import kr.hhplus.be.server.api.reservation.application.service.ReservationService;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.reservation.domain.entity.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Reservation 관련 테스트 클래스
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void 좌석예약_생성_테스트() {
        // given: 예약 요청과 좌석 정보
        ReservationServiceRequest serviceRequest = ReservationServiceRequest.builder().build();
        Seat seat = Seat.builder().seatNumber(1).status(SeatStatus.AVAILABLE).build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .seatId(seat.getId())
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when: 예약 생성
        var result = reservationService.createReservation(serviceRequest, seat);

        // then: 예약 확인
        assertThat(result).isNotNull();
    }

    @Test
    void 결제후_예약상태_변경_테스트() {
        // given: 결제 요청과 상태
        PaymentServiceRequest paymentRequest = PaymentServiceRequest.builder().build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .seatId(Seat.builder().seatNumber(1).status(SeatStatus.RESERVED).build().getId())
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(java.util.Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when: 예약 결제
        var updatedReservation = reservationService.payReservation(paymentRequest, ReservationStatus.PAID);

        // then: 상태 확인
        assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.PAID);
    }
}
