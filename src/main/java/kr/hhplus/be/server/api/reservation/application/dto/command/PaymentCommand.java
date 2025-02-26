package kr.hhplus.be.server.api.reservation.application.dto.command;

public record PaymentCommand (
    Long reservationId,
    Long seatId,
    Long userId,
    Long paymentAmount,
    String paymentMethod
){

}


