package kr.hhplus.be.server.api.reservation;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.service.ReservationService;
import kr.hhplus.be.server.api.reservation.domain.entity.Reservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Test
    void 동시에_같은_좌석_예약시_하나의_예약만_성공() throws InterruptedException {
        Long seatId = 1L;
        LocalDate reservationDate = LocalDate.now();

        // 5명의 사용자 생성
        ReservationCommand[] commands = {
                new ReservationCommand(100L, seatId, 10, 1L, reservationDate, 100L),
                new ReservationCommand(101L, seatId, 10, 1L, reservationDate, 100L),
                new ReservationCommand(102L, seatId, 10, 1L, reservationDate, 100L),
                new ReservationCommand(103L, seatId, 10, 1L, reservationDate, 100L),
                new ReservationCommand(104L, seatId, 10, 1L, reservationDate, 100L)
        };

        CountDownLatch latch = new CountDownLatch(5);

        // 각 사용자 작업 생성
        for (int i = 0; i < 5; i++) {
            int index = i;
            new Thread(() -> {
                try {
                    reservationService.createReservation(commands[index]);
                    System.out.println("사용자 " + (index + 1) + "좌석 예약 성공");
                } catch (Exception e) {
                    System.err.println("사용자 " + (index + 1) + " 예약 실패 : " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 모든 스레드 대기
        latch.await();

        // 결과 확인
        List<Reservation> reservations = reservationService.findAllReservationsBySeatId(seatId);
        assertEquals(1, reservations.size(), "동시에 같은 좌석을 예약했을 때 하나의 예약만 성공");
    }
}
