package kr.hhplus.be.server.api.reservation.application.event;

import kr.hhplus.be.server.api.common.kafka.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConcertSeatPaidEvent implements DomainEvent {
    Long reservationId; 
    Long userId;
    Long seatId;
    Long finalPrice;

    @Override
    public String getTopic() {
        return "seatPaid-topic";
    }

    @Override
    public String getKey() {
        return String.valueOf(reservationId);
    }
}
