import http from "k6/http";
import { check } from "k6";

export function issueToken(userId) {
    const url = "http://host.docker.internal:8080/tokens/issue"; // 토큰 발급 API
    const payload = JSON.stringify({ userId });
    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const response = http.post(url, payload, params);

    check(response, {
        "Token issued successfully": (r) => r.status === 200,
    });

    if (response.status === 200) {
        return response.headers["Authorization"]; // 발급된 토큰 반환
    } else {
        console.error(`Failed to issue token for userId: ${userId}`);
        return null;
    }
}