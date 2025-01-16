package kr.hhplus.be.server.api.concert.application.service;

import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.exception.ConcertErrorCode;
import kr.hhplus.be.server.api.concert.exception.SeatErrorCode;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    /**
     * 특정 콘서트의 예매 가능한 날짜 조회
     */
    public List<LocalDate> getAvailableDateList(Long concertId){
        return concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, false)
                .stream()
                .map(ConcertSchedule::getScheduleDate) // 콘서트 일정
                .distinct()                       // 중복 제거
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 예매 가능한 좌석 조회
     */
    public List<Integer> getAvailableSeatList(Long concertId, LocalDate scheduleDate) {
        return seatRepository.findAvailableSeatList(concertId, scheduleDate)
                .stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());
    }

    /**
     * 좌석 예약
     */
    @Transactional
    public Seat reserveSeat(Long concertId, LocalDate scheduleDate, int seatNumber) {
        // 매진 여부 확인
        Optional<Boolean> result  = concertScheduleRepository.isScheduleSoldOut(concertId, scheduleDate);
        boolean isSoldOut = result.orElse(false); // 기본값으로 false 처리
        if (isSoldOut) {
            throw new CustomException(ConcertErrorCode.CONCERT_FULL);
        }

        // 좌석 상태 확인
        Seat seat = seatRepository.findSeat(concertId, scheduleDate, seatNumber)
                .orElseThrow(() -> new CustomException(SeatErrorCode.SEAT_NOT_FOUND));

        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new CustomException(SeatErrorCode.SEAT_ALREADY_RESERVED);
        }

        // 좌석 상태 변경(예약됨)
        Seat updatedSeat = seat.toBuilder()
                .status(SeatStatus.RESERVED)
                .build();
        seatRepository.save(updatedSeat);

        // 콘서트 매진 상태 업데이트 확인
        checkAndMarkSoldOut(concertId, scheduleDate);

        return updatedSeat;
    }

    public void checkAndMarkSoldOut(Long concertId, LocalDate scheduleDate) {
        // 남은 예약 가능한 좌석 개수 확인
        long availableSeats = seatRepository.countAvailableSeats(concertId, scheduleDate);

        // 모든 좌석이 예약된 경우 매진 처리
        if (availableSeats == 0) {
            ConcertSchedule concertSchedule = concertScheduleRepository.findSchedule(concertId, scheduleDate)
                    .orElseThrow(() -> new CustomException(ConcertErrorCode.CONCERT_NOT_FOUND));

            ConcertSchedule updatedSchedule = concertSchedule.toBuilder()
                    .isSoldOut(true) // 매진 처리
                    .build();

            concertScheduleRepository.save(updatedSchedule);
        }
    }
}
