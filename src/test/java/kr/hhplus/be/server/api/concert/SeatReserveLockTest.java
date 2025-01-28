package kr.hhplus.be.server.api.concert;

import kr.hhplus.be.server.api.common.lock.RedisLockManager;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.application.dto.response.ConcertSeatResult;
import kr.hhplus.be.server.api.concert.application.service.ConcertService;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SeatReserveLockTest {
    private static final String SEAT_LOCK_PREFIX = "lock:seat:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ConcertService concertService;

    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    ConcertScheduleRepository concertScheduleRepository;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(SEAT_LOCK_PREFIX+"*"); //전체 Lock 삭제

        seatRepository.deleteAll();
        seatRepository.flush();

        Seat sampleSeat = Seat.builder().
                seatNumber(1)
                .concertId(1L)
                .scheduleDate(LocalDate.of(2025, 1, 1))
                .status(SeatStatus.AVAILABLE)
                .price(50000L)
                .build();

        seatRepository.saveAndFlush(sampleSeat);

        concertScheduleRepository.saveAll(List.of(
                ConcertSchedule.builder()
                        .concertId(1L)
                        .scheduleDate(LocalDate.of(2025, 1, 1))
                        .isSoldOut(false)
                        .build(),
                ConcertSchedule.builder()
                        .concertId(1L)
                        .scheduleDate(LocalDate.of(2025, 1, 2))
                        .isSoldOut(false)
                        .build()
        ));
    }

    @Test
    void 레디스_심플락을_이용한_좌석_예약_동시성_테스트() throws InterruptedException {
//        Long seatId = 101L;
        Long seatId = seatRepository.findAll().get(0).getId();
        String lockKey = SEAT_LOCK_PREFIX + seatId;

        int threadCount = 10;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for(int i = 0; i < threadCount; i++){
            executorService.execute(() -> {
                try {
                    ConcertSeatResult concertSeatResult = concertService.reserveSeat(seatId);
                    assertThat(concertSeatResult).isNotNull();
                    assertThat(concertSeatResult.id()).isEqualTo(seatId);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        // 모든 스레드 작업 대기
        countDownLatch.await();
        executorService.shutdown();

        // 좌석이 한 번만 예약되었는지 검증
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
}