import http from "k6/http";
import { check } from "k6";

export function getAvailableSeats(concertId, scheduleDate) {
    const url = `http://host.docker.internal:8080/concerts/${concertId}/seats/available?scheduleDate=${scheduleDate}`;
    const response = http.get(url);

    check(response, {
        "Available seats retrieved": (r) => r.status === 200,
    });

    if (response.status === 200) {
        const seats = JSON.parse(response.body).data;
        if (seats && seats.length > 0) {
            return seats[Math.floor(Math.random() * seats.length)]; // 랜덤 좌석 반환
        }
    }

    console.error(`No seats available for concertId: ${concertId}`);
    return null;
}
