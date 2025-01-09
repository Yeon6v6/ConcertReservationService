package kr.hhplus.be.server.api.concert.application.dto;

/**
 * 예매 가능한 좌석 데이터(Model)
 */
public class AvailableSeat {
    private int seatNumber;

    public AvailableSeat(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
}