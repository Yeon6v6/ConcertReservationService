import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    scenarios: {
        one_time_run: {
            executor: 'shared-iterations',
            vus: 10,
            maxDuration: '5s',
            iterations: 10,
        },
    },
};

const BASE_URL = 'http://host.docker.internal:8080';

const users = new SharedArray('users', () => Array.from({ length: 1000 }, (_, i) => i + 1));
const concerts = new SharedArray('concerts', () => [101, 202, 303, 404, 505]);

export default function () {
    const userId = randomItem(users);

    // Step 1: 토큰 발급 요청
    let res = http.post(`${BASE_URL}/tokens/issue`, JSON.stringify({ userId }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (!check(res, { '토큰 발급 성공': (r) => r.status === 200 })) {
        console.error('토큰 발급 실패:', res.status, res.body);
        return;
    }

    const parsedTokenResp = JSON.parse(res.body);
    let { token, queuePosition, status, hasPassedQueue } = parsedTokenResp.data;
    console.log(`토큰 발급 성공: ${token} (userId: ${userId}), 대기열 순위: ${queuePosition}, 상태: ${status}`);

    // Step 2: 폴링을 통한 토큰 활성화 대기
    // 좌석 조회 API를 통해 토큰 ACTIVE 여부를 확인합니다.
    const maxPollingTime = 30; // 최대 30초 대기
    const pollingInterval = 2; // 2초 간격
    let elapsedTime = 0;
    let tokenActive = hasPassedQueue; // 이미 ACTIVE라면 바로 진행

    while (!tokenActive && elapsedTime < maxPollingTime) {
        let seatsRes = http.get(`${BASE_URL}/concerts/${concertId}/seats/available?scheduleDate=${scheduleDate}`, {
            headers: { 'QUEUE-TOKEN': token },
        });
        if (seatsRes.status === 200) {
            console.log(`대기열 통과 완료 (elapsedTime: ${elapsedTime}s)`);
            tokenActive = true;
            break;
        }
        sleep(pollingInterval);
        elapsedTime += pollingInterval;
    }

    if (!tokenActive) {
        console.error(`대기열 통과 실패: ${maxPollingTime}초 내에 ACTIVE 상태 전환 없음 (최종 대기열 순위: ${queuePosition})`);
        return;
    }

    sleep(1);

    // Step 3: 예약 가능한 날짜 조회
    const concertId = randomItem(concerts);
    res = http.get(`${BASE_URL}/concerts/${concertId}/dates/available`);
    if (!check(res, { '예약 가능한 날짜 조회 성공': (r) => r.status === 200 })) {
        console.error('예약 가능한 날짜 조회 실패:', res.status, res.body);
        return;
    }
    const parsedDates = JSON.parse(res.body);
    if (!parsedDates.availableDateList || parsedDates.availableDateList.length === 0) {
        console.error('예약 가능한 날짜 없음');
        return;
    }
    const scheduleDate = randomItem(parsedDates.availableDateList);
    console.log(`선택된 예약 날짜: ${scheduleDate}`);

    // Step 4: 예약 가능한 좌석 조회 (실제 예약을 위한 API 호출)
    res = http.get(`${BASE_URL}/concerts/${concertId}/seats/available?scheduleDate=${scheduleDate}`, {
        headers: { 'QUEUE-TOKEN': token },
    });
    if (!check(res, { '예약 가능한 좌석 조회 성공': (r) => r.status === 200 })) {
        console.error('예약 가능한 좌석 조회 실패:', res.status, res.body);
        return;
    }
    const parsedSeats = JSON.parse(res.body);
    if (!parsedSeats.availableSeatList || parsedSeats.availableSeatList.length === 0) {
        console.error("예약 가능한 좌석이 없음");
        return;
    }
    const selectedSeats = randomItem(parsedSeats.availableSeatList);
    console.log(`선택된 좌석 ID: ${selectedSeats}`);
    sleep(1);

    // Step 5: 좌석 예약 요청
    const reservationPayload = JSON.stringify({
        userId: userId,
        concertId: concertId,
        scheduleDate: scheduleDate,
        seatIds: [selectedSeats],
    });
    res = http.post(`${BASE_URL}/reservations/${concertId}/reserve-seats`, reservationPayload, {
        headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
    });
    if (!check(res, { '좌석 예약 성공': (r) => r.status === 200 || r.status === 201 })) {
        console.error('좌석 예약 실패:', res.status, res.body);
        return;
    }
    const parsedReservation = JSON.parse(res.body);
    if (!parsedReservation.reservationId) {
        console.error("좌석 예약 실패: 예약 ID 없음");
        return;
    }
    const reservationId = parsedReservation.reservationId;
    console.log(`예약 ID: ${reservationId}`);
    sleep(1);

    // Step 6: 결제 요청
    const paymentPayload = JSON.stringify({
        paymentMethod: "POINT",
    });
    res = http.post(`${BASE_URL}/reservations/${reservationId}/payment`, paymentPayload, {
        headers: { 'Content-Type': 'application/json', 'QUEUE-TOKEN': token },
    });
    if (!check(res, { '결제 처리 성공': (r) => r.status === 200 || r.status === 201 })) {
        console.error('결제 실패:', res.status, res.body);
        return;
    }
    const parsedPayment = JSON.parse(res.body);
    console.log(`결제 완료. 남은 잔액: ${parsedPayment.remainingBalance}`);
    sleep(1);

    // Step 7: 사용자 잔액 조회
    res = http.get(`${BASE_URL}/users/balance/${userId}`);
    if (!check(res, { '사용자 잔액 조회 성공': (r) => r.status === 200 })) {
        console.error('사용자 잔액 조회 실패:', res.status, res.body);
        return;
    }
    sleep(1);
}
