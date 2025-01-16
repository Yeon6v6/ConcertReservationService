package kr.hhplus.be.server.api.reservation.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.application.factory.ReservationResultFactory;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.reservation.domain.factory.ReservationFactory;
import kr.hhplus.be.server.api.reservation.domain.repository.ReservationRepository;
import kr.hhplus.be.server.api.reservation.exception.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationFactory reservationFactory;
    private final ReservationResultFactory reservationResultFactory;


    /**
     * 예약 ID로 예약 정보 조회
     */
    public Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * 예약 생성(좌석 예약)
     */
    @Transactional
    public ReservationResult createReservation(ReservationCommand reservationCommand) {
        boolean isExistReservation = reservationRepository.existsBySeatId(reservationCommand.seatId());
        if(isExistReservation){
            throw new CustomException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
        }

        // 예약(Reservation 객체) 생성
        Reservation reservation = reservationFactory.createReservation(reservationCommand);

        Reservation savedReservation = reservationRepository.save(reservation);

        return reservationResultFactory.createResult(savedReservation);
    }
    
    /**
     * 예약 정보 업데이트
     */
    public void updateReservation(Reservation reservation) {
        reservationRepository.save(reservation);
    }
}
