package kr.hhplus.be.server.api.concert.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AvailableSeatListResponse {
    private List<Integer> availableSeatList;
}
