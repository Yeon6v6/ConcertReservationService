package kr.hhplus.be.server.api.reservation;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.application.facade.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.service.ReservationService;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.reservation.domain.factory.ReservationFactory;
import kr.hhplus.be.server.api.reservation.domain.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Reservation 관련 테스트 클래스
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationFactory reservationFactory;

    @InjectMocks
    private ReservationService reservationService;

    @InjectMocks
    private ReservationFacade reservationFacade;

    @Test
    void 좌석예약_생성_테스트() {
        // Given
        ReservationCommand reservationCommand = ReservationCommand.of(
                1L, 1L, 1, 1L, LocalDate.of(2025, 1, 10), 10000L
        );

        Reservation reservation = Reservation.create(
                reservationCommand.userId(),
                reservationCommand.seatId(),
                reservationCommand.seatNumber(),
                reservationCommand.concertId(),
                reservationCommand.scheduleDate(),
                reservationCommand.price()
        );

        when(reservationFactory.createReservation(reservationCommand)).thenReturn(reservation);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // When
        ReservationResult result = reservationService.createReservation(reservationCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.seatNumber()).isEqualTo(1);
        assertThat(result.scheduleDate()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    void 결제후_예약상태_변경_테스트() {
        // Given
        PaymentCommand paymentCommand = new PaymentCommand(
                1L, 1L, 10000L, "CREDIT_CARD"
        );

        Reservation reservation = Reservation.builder()
                .id(1L)
                .seatId(1L)
                .status(ReservationStatus.PENDING)
                .price(10000L)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(reservationRepository.findById(paymentCommand.reservationId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationFacade.payReservation(paymentCommand);

        // Then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(reservation.getPrice()).isEqualTo(10000L);
        assertThat(reservation.getPaidAt()).isNotNull();
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void 결제_실패_테스트_만료된_예약() {
        // Given
        PaymentCommand paymentCommand = new PaymentCommand(
                1L, 1L, 10000L, "CREDIT_CARD"
        );

        Reservation expiredReservation = Reservation.builder()
                .id(1L)
                .seatId(1L)
                .status(ReservationStatus.PENDING)
                .price(10000L)
                .expiredAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(reservationRepository.findById(paymentCommand.reservationId())).thenReturn(Optional.of(expiredReservation));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> reservationFacade.payReservation(paymentCommand));
        assertThat(exception.getMessage()).isEqualTo("예약이 만료되었습니다.");
        verify(reservationRepository, never()).save(any());
    }
}
