import { issueToken } from "./api/issueToken.js";
import { reserveSeat } from "./api/seatReservation.js";
import { getAvailableSeats } from "./api/getSeats.js";

export const options = {
    scenarios: {
        generate_new_vus: {
            executor: 'constant-arrival-rate',  // VU가 한 번 실행 후 종료
            rate: 50, // 초당 50개의 VU 생성
            timeUnit: '1s',
            duration: '10s',
            preAllocatedVUs: 50,
        },
    },
};

export default function () {
    const userId = __VU;  // VU 번호를 userId로 사용
    // 토큰 발급
    const token = issueToken(userId);
    // 좌석 조회
    const seatInfo = getAvailableSeats(1, "2025-01-01");
    // 좌석 예약
    if (token && seatInfo) {
        reserveSeat(token, 1, seatInfo.seatId, 1000, "2025-01-01");
    }
}