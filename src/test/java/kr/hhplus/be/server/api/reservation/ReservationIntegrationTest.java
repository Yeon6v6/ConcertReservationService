package kr.hhplus.be.server.api.reservation;

import com.jayway.jsonpath.JsonPath;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.type.SeatStatus;
import kr.hhplus.be.server.api.concert.domain.entity.ConcertSchedule;
import kr.hhplus.be.server.api.concert.domain.entity.Seat;
import kr.hhplus.be.server.api.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.api.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.api.reservation.application.dto.command.ReservationCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.PaymentResult;
import kr.hhplus.be.server.api.reservation.application.facade.ReservationFacade;
import kr.hhplus.be.server.api.reservation.application.dto.command.PaymentCommand;
import kr.hhplus.be.server.api.reservation.application.dto.result.ReservationResult;
import kr.hhplus.be.server.api.reservation.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String BASE_URL;

    @BeforeEach
    public void setUp() {
        this.BASE_URL = "http://localhost:" + port;
    }

    @Test
    public void testReservationProcess() {
        // 1) 토큰 발급
        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(BASE_URL + "/tokens/issue", "{\"userId\":\"testuser\"}", String.class);
        assertNotNull(tokenResponse.getBody());
        String token = extractJsonValue(tokenResponse.getBody(), "token");

        // 2) 토큰 활성화 (없음, 토큰 발급 후 바로 사용)

        // 3) 예약 가능한 날짜 조회
        ResponseEntity<String> datesResponse = restTemplate.getForEntity(BASE_URL + "/concerts/1/dates/available", String.class);
        String availableDate = extractJsonArrayValue(datesResponse.getBody(), 0);

        // 4) 예약 가능한 좌석 조회
        ResponseEntity<String> seatsResponse = restTemplate.getForEntity(BASE_URL + "/concerts/1/seats/available?scheduleDate=" + availableDate, String.class);
        String seatId = extractJsonArrayValue(seatsResponse.getBody(), 0);

        // 5) 좌석 예약 요청
        ResponseEntity<String> reservationResponse = restTemplate.postForEntity(BASE_URL + "/reservations/1/reserve-seats", "{\"date\":\"" + availableDate + "\", \"seatId\":\"" + seatId + "\"}", String.class);
        String reservationId = extractJsonValue(reservationResponse.getBody(), "reservationId");

        // 6) 결제 요청
        ResponseEntity<String> paymentResponse = restTemplate.postForEntity(BASE_URL + "/reservations/" + reservationId + "/payment", "{\"paymentMethod\":\"creditcard\"}", String.class);
        assertTrue(extractJsonValue(paymentResponse.getBody(), "paymentStatus").equals("SUCCESS"));

        // 7) 대기열 토큰 만료 (테스트 환경에서 강제 만료 시뮬레이션 필요)
    }

    private String extractJsonValue(String json, String key) {
        return json.substring(json.indexOf(key) + key.length() + 3).split("\"")[0];
    }

    private String extractJsonArrayValue(String json, int index) {
        return json.substring(json.indexOf("[") + 2).split("\"")[index];
    }
}
