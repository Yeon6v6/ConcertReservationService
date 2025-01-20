package kr.hhplus.be.server.api.concert.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.concert.application.dto.response.ConcertSeatResult;
import kr.hhplus.be.server.api.concert.application.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
@Tag(name = "Conert", description =  "콘서트 예약 API - 정보 조회")
public class ConcertContoller {

    private final ConcertService concertService;

    /**
     * 예매 가능한 날짜 리스트 조회 API
     * @param concertId
     * @return 예매 가능한 날짜 리스트
     */
    @GetMapping("/{concertId}/dates/available")
    public ResponseEntity<?> getAvailableDates(@PathVariable Long concertId) {
        try {
            List<LocalDate> availableDates = concertService.getAvailableDateList(concertId);

            return ResponseEntity.ok(ApiResponse.success("예약 가능한 날짜 목록을 조회했습니다.", availableDates));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getErrorCode().getName(), e.getMessage()));
        }
    }

    /**
     * 특정 날짜의 예매 가능한 좌석 리스트 조회 API
     * @param concertId
     * @param scheduleDate
     * @return 예매 가능한 좌석 리스트
     */
    @GetMapping("/{concertId}/seats/available")
    public ResponseEntity<?> getAvailableSeats(@PathVariable Long concertId, @RequestParam LocalDate scheduleDate) {
        try {
            List<ConcertSeatResult> availableSeats = concertService.getAvailableSeatList(concertId, scheduleDate);

            return ResponseEntity.ok(ApiResponse.success("예약 가능한 좌석 정보를 조회했습니다.", availableSeats));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getErrorCode().getName(), e.getMessage()));
        }
    }
}
