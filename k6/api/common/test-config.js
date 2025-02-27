export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 100 },
                { duration: '30s', target: 100 },
                { duration: '10s', target: 300 },
                { duration: '30s', target: 300 },
                { duration: '10s', target: 0 },
            ],
        }
    },
    thresholds: {
        http_req_duration: ['p(95) < 2000'], // 95% 요청이 2초 이내
        http_req_failed: ['rate < 0.01'], // 에러율 1% 이하 유지
    },
};

export const BASE_URL = 'http://host.docker.internal:8080';