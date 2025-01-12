package kr.hhplus.be.server.api.reservation.presentation.dto;

import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import lombok.*;

import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long reservationId;
    private Long price;
    private Long balance;

    /**
     * Reservation Entity => PaymentReponse로 변환
     */
    public static PaymentResponse fromEntity(Reservation reservation) {
        return PaymentResponse.builder()
                .reservationId(reservation.getId())
                .price(reservation.getPrice())
                .build();
    }
}
