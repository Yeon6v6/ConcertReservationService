package kr.hhplus.be.server.api.reservation.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.PaymentResult;
import kr.hhplus.be.server.api.reservation.application.facade.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
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
     */
    @PostMapping("/{concertId}/reserve-seats")
    public ResponseEntity<?> reserveSeat(@PathVariable Long concertId, @RequestBody ReservationRequest reservationRequest) {
        try {
            // Presentation DTO => Service DTO 변환
            ReservationCommand reservationCommand = reservationRequest.toCommand(concertId);

            // 비즈니스 로직 호출
            ReservationResult reservationResult = reservationFacade.reserveSeat(reservationCommand);

            // ReservationResult => Presentation DTO 변환
            ReservationResponse reservationResponse = ReservationResponse.fromResult(reservationResult);

            return ApiResponse.success("좌석이 임시로 예약되었습니다.", reservationResponse);
        } catch (CustomException e) {
            return ApiResponse.error(e.getErrorCode().getName(), e.getMessage());
        }
    }

    /**
     * 예약한 좌석 결제 API
     * @param reservationId
     * @param paymentRequest
     */
    @PostMapping("/{reservationId}/payment")
    public ResponseEntity<?> payment(@PathVariable Long reservationId, @RequestBody PaymentRequest paymentRequest) {
        try {
            // Presentation DTO -> 서비스 계층 요청 객체 변환
            PaymentCommand serviceRequest = paymentRequest.toCommand(reservationId);

            // 비즈니스 로직 호출
            PaymentResult paymentResult = reservationFacade.payReservation(serviceRequest);

            // PaymentResult => Presentation DTO 변환
            PaymentResponse paymentResponse = PaymentResponse.fromResult(paymentResult);

            return ApiResponse.success("결제가 성공적으로 처리되었습니다.", paymentResponse);
        } catch (CustomException e) {
            return ApiResponse.error(e.getErrorCode().getName(), e.getMessage());
        }
    }
}
