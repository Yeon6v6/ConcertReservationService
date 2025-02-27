import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { options, BASE_URL } from '../common/test-config.js';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export { options };

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
    const tokenRes = http.post(
        `${BASE_URL}/tokens/issue`,
        JSON.stringify({ userId }),
        {
            tags: { name: 'POST /tokens/issue' },
            headers: { 'Content-Type': 'application/json' },
        }
    );
    tokenTime.add(new Date() - start);

    if (tokenRes.status !== 200 || !tokenRes.body) {
        return;
    }

    let parsedTokenResp;
    try {
        parsedTokenResp = JSON.parse(tokenRes.body);
    } catch (e) {
        return;
    }

    if (!parsedTokenResp.data) {
        return;
    }

    let { id, token, queuePosition, status, hasPassedQueue } = parsedTokenResp.data;

    // Step 2: 대기열 통과 체크
    const maxPollingTime = 30;
    const pollingInterval = 2;
    let elapsedTime = 0;
    let tokenActive = hasPassedQueue;

    while (!tokenActive && elapsedTime < maxPollingTime) {
        let queueCheck = http.get(
            `${BASE_URL}/tokens/status?tokenId=${id}`,
            { tags: { name: 'GET /tokens/status' } }
        );
        if (queueCheck.status === 200) {
            let responseBody = JSON.parse(queueCheck.body);
            if (responseBody.status === "ACTIVE") {
                tokenActive = true;
                break;
            }
        }
        sleep(pollingInterval);
        elapsedTime += pollingInterval;
    }

    queueWaitTime.add(elapsedTime);

    // Step 3: 예약 가능한 일정 조회
    start = new Date();
    const scheduleRes = http.get(
        `${BASE_URL}/concerts/${concertId}/dates/available`,
        { tags: { name: 'GET /concerts/:id/dates/available' } }
    );
    scheduleTime.add(new Date() - start);

    if (scheduleRes.status !== 200 || !scheduleRes.body) {
        return;
    }

    const parsedDates = JSON.parse(scheduleRes.body);
    if (!parsedDates.data || parsedDates.data.length === 0) {
        return;
    }

    const scheduleDate = randomItem(parsedDates.data);

    // Step 4: 예약 가능한 좌석 조회
    const lockedSeats = new Set();

    const selectedSeat = findAvailableSeat(concertId, scheduleDate, token, lockedSeats);
    if (!selectedSeat) {
        return;
    }

    // Step 5: 좌석 예약 요청
    const reservationId = (() => {
        const reservationPayload = JSON.stringify({
            userId: userId,
            date: scheduleDate,
            seatId: selectedSeat.id,
            seatNo: selectedSeat.seatNumber
        });

        start = new Date();
        const reservationUrl = `${BASE_URL}/reservations/${concertId}/reserve-seats`;
        const reservedSeatRes = http.post(
            reservationUrl,
            reservationPayload,
            {
                tags: { name: 'POST /reservations/:id/reserve-seats' },
                headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
            }
        );
        reserveTime.add(new Date() - start);

        if (reservedSeatRes.status === 200) {
            const parsedReservation = JSON.parse(reservedSeatRes.body);
            return parsedReservation.data.reservationId;
        }
        lockedSeats.add(selectedSeat); // 해당 좌석을 잠긴 좌석 목록에 추가
    })();

    sleep(3); // 예약 성공 후 서버가 상태를 업데이트할 시간을 주기 위해 3초 대기

    // Step 6: 결제 요청
    const paymentPayload = JSON.stringify({
        userId: userId,
        seatId: selectedSeat.id,
        paymentInfo: {
            amount: 50000,
            method: "CREDIT_CARD"
        }
    });

    const paymentUrl = `${BASE_URL}/reservations/${reservationId}/payment`;

    start = new Date();
    const paymentResponse = http.post(
        paymentUrl,
        paymentPayload,
        {
            tags: { name: 'POST /reservations/:id/payment' },
            headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
        }
    );
    paymentTime.add(new Date() - start);

    if (paymentResponse.status !== 200) {
        return;
    }

    const parsedPayment = JSON.parse(paymentResponse.body);

    sleep(1);
}

// 예약 가능한 좌석 찾는 함수
function findAvailableSeat(concertId, scheduleDate, token, lockedSeats) {
    let retryCount = 0;
    const maxRetries = 5; // 최대 5회 재시도
    let seat = null;

    while (!seat && retryCount < maxRetries) {
        let res = http.get(
            `${BASE_URL}/concerts/${concertId}/seats/available?scheduleDate=${scheduleDate}`,
            {
                tags: { name: 'GET /concerts/:id/seats/available' },
                headers: { 'QUEUE-TOKEN': token },
            }
        );

        if (res.status !== 200 || !res.body) {
            return null;
        }

        const parsedSeats = JSON.parse(res.body);
        if (!parsedSeats.data || parsedSeats.data.length === 0) {
            retryCount++;
            sleep(1);
            continue;
        }

        const availableSeats = parsedSeats.data.filter(seat => !lockedSeats.has(seat.id));
        if (availableSeats.length === 0) {
            retryCount++;
            sleep(1);
            continue;
        }

        seat = randomItem(availableSeats);
    }
    return seat;
}
