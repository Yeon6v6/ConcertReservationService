package kr.hhplus.be.server.api.reservation.domain.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.api.common.type.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long seatId;
    private int seatNumber; // 좌석 번호

    private Long concertId; // 콘서트 ID
    private LocalDate scheduleDate; // 스케줄 날짜

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // 예약 상태
    private LocalDateTime expiredAt; // (좌석)예약 만료일

    private Long price; // (예약좌석)결제 금액
    private LocalDateTime paidAt; // 결제일
}
