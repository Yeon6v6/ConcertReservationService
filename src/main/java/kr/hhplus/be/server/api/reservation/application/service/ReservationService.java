package kr.hhplus.be.server.api.reservation.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.exception.SeatErrorCode;
import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.reservation.domain.entity.repository.ReservationRepository;
import kr.hhplus.be.server.api.reservation.exception.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * 좌석 예약
     */
    @Transactional
    public Reservation createReservation(ReservationServiceRequest serviceRequest, Seat seat) {

        // 중복 예약 확인
        boolean isDuplicate = reservationRepository.existsByConcertIdAndScheduleDateAndSeatNumber(
                serviceRequest.getConcertId(),
                serviceRequest.getScheduleDate(),
                seat.getSeatNumber()
        );
        if (isDuplicate) {
            throw new CustomException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
        }

        // 예약 생성(validation은 concert에서 좌석 찾을 때 진행)
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(5);
        Reservation reservation = Reservation.builder()
                .concertId(serviceRequest.getConcertId())
                .userId(serviceRequest.getUserId())
                .scheduleDate(serviceRequest.getScheduleDate())
                .seatId(serviceRequest.getSeatId())
                .seatNumber(seat.getSeatNumber())
                .status(ReservationStatus.PENDING)
                .price(seat.getPrice())
                .expiredAt(expiredAt)
                .build();

        // 예약 저장
        return reservationRepository.save(reservation);
    }

    /**
     * 좌석 결제 및 상태 변경
     */
    @Transactional
    public Reservation payReservation(PaymentServiceRequest paymentServiceReq, ReservationStatus status) {
        Reservation reservation = findById(paymentServiceReq.getReservationId());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new CustomException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        Reservation updateReservation = reservation.toBuilder()
                    .id(paymentServiceReq.getSeatId())
                    .status(status)
                    .price(paymentServiceReq.getPrice())
                    .paidAt(LocalDateTime.now())
                    .build();

        return reservationRepository.save(updateReservation);
    }
}
