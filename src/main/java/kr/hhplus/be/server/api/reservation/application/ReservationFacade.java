package kr.hhplus.be.server.api.reservation.application;

import kr.hhplus.be.server.api.user.application.service.BalanceService;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.concert.application.service.ConcertService;
import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
import kr.hhplus.be.server.api.reservation.application.service.ReservationService;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.reservation.exception.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationFacade {
    private final ReservationService reservationService;
    private final ConcertService concertService;
    private final BalanceService balanceService;

    /**
     * 좌석 예약
     * @param ReservationServiceReq
     * @return
     */
    @Transactional
    public Reservation reserveSeat(ReservationServiceRequest ReservationServiceReq) {
        // 좌석 상태 확인 및 예약
        Seat seat = concertService.reserveSeat(
                ReservationServiceReq.getConcertId(),
                ReservationServiceReq.getScheduleDate(),
                ReservationServiceReq.getSeatNumber()
        );

        // 예약 생성
        return reservationService.createReservation(ReservationServiceReq, seat);
    }

    /**
     * 예약된 좌석 결제
     * @param paymentServiceReq
     * @return
     */
    @Transactional
    public Reservation payReservation(PaymentServiceRequest paymentServiceReq) {
        Long actualAmount = balanceService.processPayment(paymentServiceReq.getUserId(), paymentServiceReq.getPrice());
        reservationService.updatePaymentStatus(paymentServiceReq.getReservationId(), ReservationStatus.PAID, actualAmount);


        // 예약 상태 업데이트 및 결제 기록 저장
        return reservationService.payReservation(paymentServiceReq, ReservationStatus.PAID);
    }
}
