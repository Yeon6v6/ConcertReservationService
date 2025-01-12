package kr.hhplus.be.server.api.reservation.application;

import kr.hhplus.be.server.api.balance.application.service.BalanceService;
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
        // 예약 조회
        Reservation reservation = reservationService.findById(paymentServiceReq.getReservationId());

        // 예약 상태 확인
        if (!reservation.getStatus().equals(ReservationStatus.PENDING)) {
            throw new CustomException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 잔액 확인
        Long userBalance = balanceService.getBalance(reservation.getUserId());
        Long initialPrice = paymentServiceReq.getPrice();
        Long chargeAmount = initialPrice - userBalance;
        Long actualAmount = initialPrice;

        if (chargeAmount > 0) {
            // 부족한 잔액만 충전
            balanceService.chargeBalance(reservation.getUserId(), chargeAmount);
            actualAmount = initialPrice - chargeAmount; // 실제 결제 금액 업데이트
        }

        // 잔액 차감 (실제 결제 금액 사용)
        balanceService.deductBalance(reservation.getUserId(), actualAmount);

        // 새로운 PaymentServiceRequest 객체 생성 (실제 결제 금액 반영)
        PaymentServiceRequest updatedPaymentServiceReq = PaymentServiceRequest.builder()
                .userId(paymentServiceReq.getUserId())
                .reservationId(paymentServiceReq.getReservationId())
                .seatId(paymentServiceReq.getSeatId())
                .price(actualAmount) // 실제 결제 금액
                .paymentMethod(paymentServiceReq.getPaymentMethod())
                .build();

        // 예약 상태 업데이트 및 결제 기록 저장
        return reservationService.payReservation(updatedPaymentServiceReq, ReservationStatus.PAID);
    }
}
