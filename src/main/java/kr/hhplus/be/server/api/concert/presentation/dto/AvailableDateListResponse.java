package kr.hhplus.be.server.api.concert.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 예약 가능한 날짜 목록 응답 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class AvailableDateListResponse {

    private Long concertId;
    private List<String> availableDateList;

}
