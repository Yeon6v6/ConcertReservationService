package kr.hhplus.be.server.api.concert.application.service;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.application.dto.response.ConcertSeatResult;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.exception.ConcertErrorCode;
import kr.hhplus.be.server.api.concert.exception.SeatErrorCode;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.api.reservation.presentation.dto.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private static final Logger logger = LoggerFactory.getLogger(ConcertService.class);

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    /**
     * 특정 콘서트의 예매 가능한 날짜 조회
     */
    public List<LocalDate> getAvailableDateList(Long concertId){
        return concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, false)
                .stream()
                .map(ConcertSchedule::getScheduleDate) // 콘서트 일정
                .distinct()// 중복 제거
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 예매 가능한 좌석 조회
     */
    public List<ConcertSeatResult> getAvailableSeatList(Long concertId, LocalDate scheduleDate) {
        return seatRepository.findAvailableSeatList(concertId, scheduleDate)
                .stream()
                .map(ConcertSeatResult::from)
                .toList();
    }

    /**
     * 좌석 예약
     */
    @Transactional
    public ConcertSeatResult reserveSeat(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new CustomException(SeatErrorCode.SEAT_NOT_FOUND));

        seat.reserve(); // 좌석 상태 변경
        Seat reservedSeat = seatRepository.save(seat);

        // 콘서트 일정 매진 여부 체크
        updateConcertSoldOutStatus(seat.getConcertId(), seat.getScheduleDate());

        return ConcertSeatResult.from(reservedSeat);
    }

    /**
     * 결제로 상태 업데이트
     */
    @Transactional
    public ConcertSeatResult payForSeat(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new CustomException(SeatErrorCode.SEAT_NOT_FOUND));

        if (seat.getStatus() != SeatStatus.RESERVED) {
            throw new CustomException(SeatErrorCode.SEAT_NOT_RESERVED);
        }

        seat.pay(); // 좌석 상태를 PAID로 변경
        Seat paidSeat = seatRepository.save(seat);

        // 콘서트 일정 매진 여부 체크
        updateConcertSoldOutStatus(seat.getConcertId(), seat.getScheduleDate());

        return ConcertSeatResult.from(paidSeat);
    }

    /**
     * 콘서트 매진 여부 체크
     */
    private void updateConcertSoldOutStatus(Long concertId, LocalDate scheduleDate) {
        if (seatRepository.countAvailableSeats(concertId, scheduleDate) == 0) {
            ConcertSchedule schedule = concertScheduleRepository.findSchedule(concertId, scheduleDate)
                    .orElseThrow(() -> new CustomException(ConcertErrorCode.CONCERT_NOT_FOUND));

            schedule.markSoldOut();
            concertScheduleRepository.save(schedule);
        }
    }
}
