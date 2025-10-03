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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {
    private static final String CACHE_AVAILABLE_DATES = "availableDates";
    private static final String CACHE_AVAILABLE_SEATS = "availableSeats";

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;
    private final CacheManager cacheManager;

    /**
     * 특정 콘서트의 예약 가능한 날짜 조회
     */
    @Cacheable(value = CACHE_AVAILABLE_DATES, key = "#concertId", cacheManager = "cacheManager")
    public List<LocalDate> getAvailableDateList(Long concertId){
        return concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, false)
                .stream()
                .map(ConcertSchedule::getScheduleDate) // 콘서트 일정
                .distinct()// 중복 제거
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 예약 가능한 좌석 조회
     */
    @Cacheable(value = CACHE_AVAILABLE_SEATS, key = "#concertId + ':' + #scheduleDate", cacheManager = "cacheManager")
    public List<ConcertSeatResult> getAvailableSeatList(Long concertId, LocalDate scheduleDate) {
        return seatRepository.findAvailableSeatList(concertId, scheduleDate)
                .stream()
                .map(ConcertSeatResult::from)
                .collect(Collectors.toList());
    }

    /**
     * 좌석 예약
     */
    public ConcertSeatResult reserveSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(SeatErrorCode.SEAT_NOT_FOUND));

        seat.reserve(); // 좌석 상태 변경
        Seat savedSeat = seatRepository.save(seat);

        updateSeatRelatedData(seat.getConcertId(), seat.getScheduleDate());

        return ConcertSeatResult.from(savedSeat);
    }

    /**
     * 결제로 상태 업데이트 (Facade에서 트랜잭션 관리)
     */
    public ConcertSeatResult payForSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(SeatErrorCode.SEAT_NOT_FOUND));

        if (seat.getStatus() != SeatStatus.RESERVED) {
            throw new CustomException(SeatErrorCode.SEAT_NOT_RESERVED);
        }

        seat.pay(); // 좌석 상태를 PAID로 변경
        Seat savedSeat = seatRepository.save(seat);

        updateSeatRelatedData(seat.getConcertId(), seat.getScheduleDate());

        return ConcertSeatResult.from(savedSeat);
    }

    /**
     * 좌석 변경 시 관련 데이터 업데이트 (매진 상태 체크 + 캐시 무효화)
     */
    private void updateSeatRelatedData(Long concertId, LocalDate scheduleDate) {
        // 매진 여부 체크
        ConcertSchedule schedule = concertScheduleRepository.findSchedule(concertId, scheduleDate)
                .orElseThrow(() -> new CustomException(ConcertErrorCode.CONCERT_NOT_FOUND));

        if (seatRepository.countAvailableSeats(concertId, scheduleDate) == 0) {
            schedule.markSoldOut();
            concertScheduleRepository.save(schedule);
        }

        // 캐시 무효화
        String cacheKey = concertId + ":" + scheduleDate;
        if (cacheManager.getCache(CACHE_AVAILABLE_SEATS) != null) {
            cacheManager.getCache(CACHE_AVAILABLE_SEATS).evict(cacheKey);
        }
    }
}