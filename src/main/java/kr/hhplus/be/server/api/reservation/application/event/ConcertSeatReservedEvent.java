package kr.hhplus.be.server.api.reservation.application.event;

import kr.hhplus.be.server.api.common.kafka.event.DomainEvent;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConcertSeatReservedEvent implements DomainEvent {

    private Long reservationId;
    private Integer seatNumber;

    @Override
    public String getTopic() {
        return "seatReserved-topic";
    }

    @Override
    public String getKey() {
        return String.valueOf(reservationId);
    }
}

