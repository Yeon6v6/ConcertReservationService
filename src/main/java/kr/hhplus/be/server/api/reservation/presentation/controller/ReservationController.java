package kr.hhplus.be.server.api.reservation.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.reservation.application.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.dto.PaymentServiceRequest;
import kr.hhplus.be.server.api.reservation.application.dto.ReservationServiceRequest;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import kr.hhplus.be.server.api.reservation.presentation.dto.PaymentRequest;
import kr.hhplus.be.server.api.reservation.presentation.dto.PaymentResponse;
import kr.hhplus.be.server.api.reservation.presentation.dto.ReservationRequest;
import kr.hhplus.be.server.api.reservation.presentation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reservations")
@Tag(name = "Reservation", description =  "콘서트 예약 API - 예약 생성 및 결제")
public class ReservationController {

    private final ReservationFacade reservationFacade;

    /**
     * 좌석 예약 요청 API
     * @param concertId
     * @param reservationRequest
     * @return
     */
    @PostMapping("/{concertId}/reserve-seats")
    public ResponseEntity<?> reserveSeat(@PathVariable Long concertId, @RequestBody ReservationRequest reservationRequest) {
        try {
            // Presentation DTO -> 서비스 계층 요청 객체 변환
            ReservationServiceRequest serviceRequest = reservationRequest.toServiceDTO(concertId);

            // 비즈니스 로직 호출
            Reservation reservation = reservationFacade.reserveSeat(serviceRequest);

            ReservationResponse reservationResponse = ReservationResponse.fromEntity(reservation);

            return ResponseEntity.ok(ApiResponse.success("좌석이 임시로 예약되었습니다.", reservationResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getErrorCode().getName(), e.getMessage()));
        }
    }

    /**
     * 예약한 좌석 결제 API
     * @param reservationId
     * @param paymentRequest
     * @return
     */
    @PostMapping("/{reservationId}/payment")
    public ResponseEntity<?> payment(@PathVariable Long reservationId, @RequestBody PaymentRequest paymentRequest) {
        try {
            // Presentation DTO -> 서비스 계층 요청 객체 변환
            PaymentServiceRequest serviceRequest = paymentRequest.toServiceDTO(reservationId);

            // 비즈니스 로직 호출
            Reservation updatedReservation = reservationFacade.payReservation(serviceRequest);

            PaymentResponse paymentResponse = PaymentResponse.fromEntity(updatedReservation);

            return ResponseEntity.ok(ApiResponse.success("결제가 성공적으로 처리되었습니다.", paymentResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getErrorCode().getName(), e.getMessage()));
        }
    }

}
