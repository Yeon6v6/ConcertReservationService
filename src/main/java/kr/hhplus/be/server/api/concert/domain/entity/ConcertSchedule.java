package kr.hhplus.be.server.api.concert.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name="concert_schedule")
public class ConcertSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long concertId; //논리적 연결(물리적 연결 제외)
    private LocalDate scheduleDate; // 콘서트(스케줄) 날짜

    @Builder.Default
    private boolean isSoldOut = false; // 매진 여부

    // 매진 여부 확인
    public boolean isSeatSoldOut() {
        return this.isSoldOut;
    }

    // 매진 상태로 변경
    public ConcertSchedule markSoldOut() {
        return this.toBuilder().isSoldOut(true).build();
    }
    // 매진 상태 해제 (예약 취소 발생 시)
    public ConcertSchedule releaseSoldOut() {
        return this.toBuilder().isSoldOut(false).build();
    }
}
