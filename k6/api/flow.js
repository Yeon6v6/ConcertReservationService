import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        normal_flow: {
            executor: 'constant-vus',
            vus: 200,
            duration: '5m',
        },
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const userId = Math.floor(Math.random() * 1000) + 1;

    // Step 1: 대기열 토큰 발급
    let res = http.post(`${BASE_URL}/api/v1/queue/users/${userId}`);
    check(res, { 'Queue token received': (r) => r.status === 200 });
    const token = JSON.parse(res.body).token;
    sleep(1);

    // Step 2: 콘서트 조회 (예약 가능한 날짜/정보)
    res = http.get(`${BASE_URL}/api/v1/concerts`, {
        headers: { 'QUEUE-TOKEN': token },
    });
    check(res, { 'Concerts queried': (r) => r.status === 200 });
    let concerts = JSON.parse(res.body);
    if (concerts.length === 0) return;
    const concert = concerts[0];  // 첫 번째 콘서트 선택
    sleep(1);

    // Step 3: 해당 콘서트의 일정 조회
    res = http.get(`${BASE_URL}/api/v1/concerts/${concert.id}/schedules`, {
        headers: { 'QUEUE-TOKEN': token },
    });
    check(res, { 'Schedules queried': (r) => r.status === 200 });
    let schedules = JSON.parse(res.body);
    if (schedules.length === 0) return;
    const schedule = schedules[0];
    sleep(1);

    // Step 4: 해당 일정의 좌석 조회
    res = http.get(`${BASE_URL}/api/v1/concerts/${concert.id}/schedules/${schedule.id}/seats`, {
        headers: { 'QUEUE-TOKEN': token },
    });
    check(res, { 'Seats queried': (r) => r.status === 200 });
    let seats = JSON.parse(res.body);
    if (seats.length === 0) return;
    const selectedSeats = seats.slice(0, 2).map(seat => seat.id); // 1~2개 좌석 선택
    sleep(1);

    // Step 5: 좌석 예약 요청
    const reservationPayload = JSON.stringify({
        userId: userId,
        concertId: concert.id,
        scheduleId: schedule.id,
        seatIds: selectedSeats,
    });
    res = http.post(`${BASE_URL}/api/v1/reservations`, reservationPayload, {
        headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
    });
    check(res, { 'Reservation successful': (r) => r.status === 200 || r.status === 201 });
    let reservationIds = JSON.parse(res.body).map(r => r.id);
    sleep(1);

    // Step 6: 포인트 잔액 조회 (또는 충전 후 조회)
    res = http.get(`${BASE_URL}/api/v1/balance/users/${userId}`);
    check(res, { 'Balance queried': (r) => r.status === 200 });
    sleep(1);

    // Step 7: 결제 요청
    const paymentPayload = JSON.stringify({
        reservationIds: reservationIds,
    });
    res = http.post(`${BASE_URL}/api/v1/payment/payments/users/${userId}`, paymentPayload, {
        headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
    });
    check(res, { 'Payment completed': (r) => r.status === 200 || r.status === 201 });
    sleep(1);
}
