import http from "k6/http";
import { check } from "k6";

export function reserveSeat(token, concertId, seatId, amount, date) {
    const url = `http://host.docker.internal:8080/reservations/${concertId}/reserve-seats`;
    const payload = JSON.stringify({
        seatId: seatId,
        amount: amount,
        date: date,
    });
    const params = {
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`, // 발급된 토큰 사용
        },
    };

    const response = http.post(url, payload, params);

    check(response, {
        "Reservation successful or seat already reserved": (r) =>
            r.status === 200 || r.status === 409, // 예약 성공 또는 충돌
    });

    if (response.status !== 200 && response.status !== 409) {
        console.error(`Failed to reserve seatId: ${seatId}`);
    }
}
