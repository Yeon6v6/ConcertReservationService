package kr.hhplus.be.server.api.concert.domain.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.exception.SeatErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Desc : 특정 콘서트의 특정 일정에서 좌석 정보를 나타낸다.
 */
@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int seatNumber; // 좌석 번호

    private Long concertId; // 해당 좌석이 속한 콘서트
    private LocalDate scheduleDate; // 해당 좌석이 속한 일정

    @Enumerated(EnumType.STRING)
    private SeatStatus status; // 좌석 상태

    private Long price; // 좌석 금액ㅇ

    /**
     * 좌석 예약
     * : 상태가 이미 RESERVED인 경우 예외 발생
     */
    public void reserve(){
        if (this.status == SeatStatus.RESERVED) {
            throw new CustomException(SeatErrorCode.SEAT_ALREADY_RESERVED);
        }
        this.status = SeatStatus.RESERVED;
    }

    /**
     * 좌석 예약 해제
     * :
     */
    public void cancel(){
        if (this.status == SeatStatus.RESERVED) {
            this.status = SeatStatus.AVAILABLE;
        }
    }
}