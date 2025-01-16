package kr.hhplus.be.server.api.reservation.application.dto.command;

public record PaymentCommand (
    Long userId,
    Long reservationId,
    Long paymentAmount,
    String paymentMethod
){

}


