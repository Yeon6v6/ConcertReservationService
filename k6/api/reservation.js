import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    scenarios: {
        ticket_reservation: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '10s', target: 50 },  // 10초 동안 50명 증가
                { duration: '20s', target: 100 }, // 20초 동안 100명 증가
                { duration: '10s', target: 0 },   // 10초 동안 종료
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95% 요청이 2초 이내
        http_req_failed: ['rate<0.01'], // 에러율 1% 이하 유지
    },
};

/*export const options = {
    scenarios: {
        ticket_reservation: {
            executor: 'constant-arrival-rate',
            rate: 10,
            timeUnit: '1s',
            duration: '10s',
            preAllocatedVUs: 10,
        },
    },
};*/

const BASE_URL = 'http://host.docker.internal:8080';

const users = new SharedArray('users', () => Array.from({ length: 1000 }, (_, i) => i + 1));
const concerts = new SharedArray('concerts', () => [101, 202, 303, 404, 505]);

const tokenTime = new Trend('token_time', true);
const queueWaitTime = new Trend('queue_wait_time');
const scheduleTime = new Trend('schedule_time');
const seatTime = new Trend('seat_time');
const reserveTime = new Trend('reserve_time');
const paymentTime = new Trend('payment_time');

export default function () {
    const userId = randomItem(users);
    const concertId = randomItem(concerts);

    // Step 1: 토큰 발급 요청
    let start = new Date();
    const tokenRes = http.post(`${BASE_URL}/tokens/issue`, JSON.stringify({ userId }), {
        headers: { 'Content-Type': 'application/json' },
    });
    tokenTime.add(new Date() - start);

    if (tokenRes.status !== 200 || !tokenRes.body) {
        console.error(`토큰 발급 실패: ${tokenRes.status}, ${tokenRes.body}`);
        return;
    }

    let parsedTokenResp;
    try {
        parsedTokenResp = JSON.parse(tokenRes.body);
    } catch (e) {
        console.error(`JSON 파싱 오류: ${tokenRes.body}`);
        return;
    }

    if (!parsedTokenResp.data) {
        console.error(`토큰 발급 API 응답 데이터 없음: ${tokenRes.body}`);
        return;
    }

    let { id, token, queuePosition, status, hasPassedQueue } = parsedTokenResp.data;
    console.log(`토큰 발급 성공: ${token} (userId: ${userId}), 대기열 순위: ${queuePosition}`);

    // Step 2: 대기열 통과 체크
    const maxPollingTime = 30;
    const pollingInterval = 2;
    let elapsedTime = 0;
    let tokenActive = hasPassedQueue;

    while (!tokenActive && elapsedTime < maxPollingTime) {
        let queueCheck = http.get(`${BASE_URL}/tokens/status?tokenId=${id}`);
        if (queueCheck.status === 200) {
            let responseBody = JSON.parse(queueCheck.body);
            if (responseBody.status === "ACTIVE") {
                tokenActive = true;
                console.log(`대기열 통과 완료: ${elapsedTime}s`);
                break;
            }
        }

        sleep(pollingInterval);
        elapsedTime += pollingInterval;
    }

    queueWaitTime.add(elapsedTime);

    // Step 3: 예약 가능한 일정 조회
    start = new Date();
    const scheduleRes = http.get(`${BASE_URL}/concerts/${concertId}/dates/available`);
    scheduleTime.add(new Date() - start);

    if (scheduleRes.status !== 200 || !scheduleRes.body) {
        console.error('예약 가능한 날짜 조회 실패:', scheduleRes.status, scheduleRes.body);
        return;
    }

    const parsedDates = JSON.parse(scheduleRes.body);
    if (!parsedDates.data || parsedDates.data.length === 0) {
        console.error('예약 가능한 날짜 없음');
        return;
    }

    const scheduleDate = randomItem(parsedDates.data);
    // console.log(`선택된 예약 날짜: ${scheduleDate}`);

    // Step 4: 예약 가능한 좌석 조회
    const lockedSeats = new Set();

    const selectedSeat = findAvailableSeat(concertId, scheduleDate, token, lockedSeats);
    if (!selectedSeat) {
        console.error("예약 가능한 좌석을 찾지 못했습니다. 예약 요청을 중단합니다.");
        return;
    }

    // Step 5: 좌석 예약 요청 및 "SEAT_ALREADY_RESERVED" 자동 처리
    const reservationId = (() => {
        const reservationPayload = JSON.stringify({
            userId: userId,
            date: scheduleDate,
            seatId: selectedSeat.id,
            seatNo: selectedSeat.seatNumber
        });

        console.log("좌석 예약 요청 : ", reservationPayload);

        // 좌석 선점
        start = new Date();
        const reservationUrl = `${BASE_URL}/reservations/${concertId}/reserve-seats`;
        const reservedSeatRes = http.post(reservationUrl, reservationPayload, {
            headers: {'Content-Type': 'application/json', 'QUEUE-TOKEN': token},
        });
        reserveTime.add(new Date() - start);

        if (reservedSeatRes.status === 200) {
            console.log("--------------------")
            const parsedReservation = JSON.parse(reservedSeatRes.body);
            console.log(parsedReservation.data)
            // console.log(`좌석 예약 성공! 예약 ID: ${reservationId} / 좌석 ID : ${selectedSeatId}`);
            console.log("--------------------")
            return parsedReservation.data.reservationId;
        }
    })();

    if (!reservationId) {
        console.error(`좌석 예약 실패: ${selectedSeat.id}`);
        lockedSeats.add(selectedSeat); // 해당 좌석을 잠긴 좌석 목록에 추가
    }

    sleep(3); // 예약 성공 후 서버가 상태를 업데이트할 시간을 주기 위해 3초 대기

    // Step 6: 결제 요청
    const paymentPayload = JSON.stringify({
        userId: userId,
        seatId: selectedSeat.id,
        paymentInfo: {
            amount: 50000,  // 결제 금액
            method: "CREDIT_CARD"  // 결제 방법
        }
    });

    console.log("결제 요청 Payload:", paymentPayload);

    const paymentUrl = `${BASE_URL}/reservations/${reservationId}/payment`;

    start = new Date();
    const paymentResponse = http.post(paymentUrl, paymentPayload, {
        headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
    });
    paymentTime.add(new Date() - start);

// 결제 성공/실패 처리
    if (paymentResponse.status !== 200) {
        console.error('결제 실패:', paymentResponse.status, paymentResponse.body);
        return;
    }

    const parsedPayment = JSON.parse(paymentResponse.body);
    console.log(`결제 완료. 결제된 금액: ${parsedPayment.paidAmount}`);

    sleep(1);
}

// 예약 가능한 좌석 찾는 함수
function findAvailableSeat(concertId, scheduleDate, token, lockedSeats) {
    let retryCount = 0;
    const maxRetries = 5; // 최대 5회 재시도
    let seat = null;

    while (!seat && retryCount < maxRetries) {
        let res = http.get(`${BASE_URL}/concerts/${concertId}/seats/available?scheduleDate=${scheduleDate}`, {
            headers: { 'QUEUE-TOKEN': token },
        });

        if (res.status !== 200 || !res.body) {
            console.error('예약 가능한 좌석 조회 실패:', res.status, res.body);
            return null;
        }

        const parsedSeats = JSON.parse(res.body);
        if (!parsedSeats.data || parsedSeats.data.length === 0) {
            console.error("예약 가능한 좌석 없음, 재시도 중...");
            retryCount++;
            sleep(1);
            continue;
        }

        // 이미 예약된 좌석을 lockedSeats에 추가하여 필터링
        const availableSeats = parsedSeats.data.filter(seat => !lockedSeats.has(seat.id));
        if (availableSeats.length === 0) {
            console.error("예약 가능한 좌석 없음. 추가 재시도 필요.");
            retryCount++;
            sleep(1);
            continue;
        }

        seat = randomItem(availableSeats);
    }
    return seat;
}
