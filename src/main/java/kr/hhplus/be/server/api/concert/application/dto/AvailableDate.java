package kr.hhplus.be.server.api.concert.application.dto;

import java.time.LocalDateTime;

/**
 * 예매 가능한 날짜 데이터(Model)
 */
public class AvailableDate {
    private LocalDateTime scheduleDate;

    public AvailableDate(LocalDateTime scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public LocalDateTime getScheduleDate() {
        return scheduleDate;
    }
}
