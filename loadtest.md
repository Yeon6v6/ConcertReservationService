# 장애 대응 보고서 및 부하 테스트

## **1. 개요**

* **문서 목적**
  * 해당문서는 콘서트 좌석 예약 시스템의 **API 성능 테스트 및 장애 대응 과정**, **개선 방안**을 정리한 보고서이다.
* **시스템 개요**
  * **대기열**(Queue) 처리로 30초의 인위적 지연이 존재하지만, 이는 병목 요소가 아닌 의도된 대기임을 전제한다.
* **성능 테스트 개요**
  * **테스트 대상 API**
    1. `POST /tokens/issue` (토큰 발급)
    2. `GET /tokens/status` (대기열 상태 조회)
    3. `GET /concerts/:id/dates/available` (일정 조회)
    4. `GET /concerts/:id/seats/available` (좌석 조회)
    5. `POST /reservations/:id/reserve-seats` (좌석 예약)
    6. `POST /reservations/:id/payment` (결제)
  * **테스트 시나리오**
    1. 사용자별 **토큰 발급**
    2. **대기열 30초**(인위적 지연)
    3. 콘서트 일정 및 좌석 조회
    4. **좌석 예약** 및 **결제** 요청
    5. 각 API 단계별 응답 시간, 에러율 측정
  * **테스트 환경**
    * **도구**: k6 스크립트(`integration-test.js`), Grafana 대시보드
    * **가상 유저(VU)**: 최대 300
    * **목표 지표**: 응답 시간(AVG, P95, P99), 에러율, 처리량(RPS), 커스텀 메트릭(`reserve_time`, `payment_time`, 등)

```javascript
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
```

***

## **2. 장애 발생 및 원인 분석**

### **2.1. 전체 시스템 장애 발생 시나리오**

1. 일부 API에서 성능 저하가 감지
2. 원인 분석
   1. 동시성 처리 미흡
      * 기존 Redis Lock만으로는 다수의 동시 요청이 들어올 때 락 경합이 발생하여 응답 지연 및 실패가 증가
      * ReservationFacade의 예약/결제 로직에서 락 적용은 있었으나, 추가적인 낙관적 락 도입이 필요
   2. 쿼리 부하 및 인덱스 부재
      * 예약/결제 관련 DB 쿼리에서 인덱스가 없던 부분이 있어, 풀 스캔 및 비효율적 쿼리 실행이 원인
   3. 캐시 전략
      * 좌석 조회 및 일정 조회에서 캐시 적용이 미흡하여 동일 데이터에 대해 반복적으로 DB 접근이 발생
   4. 토큰 관리 방식 문제
      * 기존에 토큰 관리를 DB에서 수행하던 방식은 응답 지연 및 동시성 문제를 야기

> **전체 시스템 지표 (개선 전)**

```
data_received..................: 3.6 MB 33 kB/s
data_sent......................: 1.8 MB 16 kB/s
http_req_duration..............: avg=10.56ms min=1.68ms med=2.46ms max=7.65s p(90)=4.59ms p(95)=6.05ms
http_req_failed................: 0.41%  55 out of 13227
payment_time...................: avg=61.87ms min=7.87ms med=30ms max=7.45s p(90)=37ms p(95)=43ms
reserve_time...................: avg=26.08ms min=7.89ms med=27ms max=7.46s p(90)=35.5ms p(95)=38ms
schedule_time..................: avg=14.68ms min=1ms med=2ms max=7.65s p(90)=3ms p(95)=4ms
token_time.....................: avg=5.62ms min=1ms med=5ms max=166ms p(90)=7ms p(95)=8ms
```

***

## **3. 장애 해결 과정**

### **3.1. 전체 시스템 개선 조치**

**✔️ 토큰 관리 개선**

* 기존 DB에서 관리되던 토큰을 Redis로 전환하여, 빠른 조회 및 상태 변경이 가능하도록 개선
* RedisTokenRepository, RedisTokenQueueRepository, TokenService 등을 활용해 Redis 기반 토큰 관리 체계를 구축
* ([참고: 응답 테스트 문서](https://cheese-2.gitbook.io/hh_crs_doc/queryboost/responsetest))

**✔️ 동시성 처리 최적화**

* 기존 Redis Lock 외에 **낙관적 락(Optimistic Locking)** 도입을 검토 및 적용하여, 동시 예약/결제 요청 시 락 경합을 최소화
* ReservationFacade의 예약 및 결제 로직에서 락 적용을 강화

**✔️ 쿼리 부하 개선**

* DB 쿼리 실행 계획(EXPLAIN ANALYZE)을 분석하여 비효율적인 쿼리를 최적화하고, Batch 처리 및 Lazy Loading 기법을 도입
* 예약/결제 관련 쿼리에서 불필요한 풀 스캔을 방지

**✔️ 인덱스 최적화**

* 기존 인덱스가 없던 부분에 대해 인덱스를 추가하여 쿼리 성능을 개선
  * **concert\_schedule** 테이블: `concert_id, is_sold_out` 복합 인덱스 추가
  * **seat** 테이블: `concert_id, schedule_date` 복합 인덱스 추가
  * **reservation** 테이블: `user_id, status` 복합 인덱스 추가
* ([참고: 인덱싱 개선 문서](https://cheese-2.gitbook.io/hh_crs_doc/queryboost/indexing))

**✔️ 캐시 적용 강화**

* ConcertService에서 좌석 조회 시 @Cacheable을 활용하여 동일 데이터에 대한 반복 조회를 방지
* Redis를 통한 토큰 관리와 함께 캐시 전략을 보완하여, 전체 시스템 응답 속도를 향상

***

## **4. 성능 테스트 시나리오 및 결과**

### **4-1. 성능 테스트 개요**

* **테스트 대상 API**
  1. `POST /tokens/issue` (토큰 발급)
  2. `GET /tokens/status` (대기열 상태 조회)
  3. `GET /concerts/:id/dates/available` (일정 조회)
  4. `GET /concerts/:id/seats/available` (좌석 조회)
  5. `POST /reservations/:id/reserve-seats` (좌석 예약)
  6. `POST /reservations/:id/payment` (결제)
* **테스트 시나리오**
  1. 사용자별 토큰 발급 (POST /tokens/issue)
  2. 의도적 30초 대기(대기열 처리 – 분석 대상에서 제외)
  3. 예약 가능한 일정 및 좌석 조회
  4. 좌석 예약 요청 (POST /reservations/:id/reserve-seats)
  5. 결제 요청 (POST /reservations/:id/payment)
* **테스트 환경**
  * **도구**: k6 스크립트(`integration-test.js`), Grafana 대시보드
  * **가상 유저(VU)**: 최대 300
  * **목표 지표**: 응답 시간(AVG, P95, P99), 에러율, 처리량(RPS), 커스텀 메트릭(`reserve_time`, `payment_time`, 등)

{% hint style="success" %}
**테스트 시나리오**
{% endhint %}

```javascript
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

```

### **4-1. 전체 시스템 지표 (개선 후)**

```
data_received..................: 4.4 MB   (41 kB/s)
data_sent......................: 1.5 MB   (14 kB/s)
http_req_blocked...............: avg=830.12µs    min=1.5µs         med=7.03µs   max=7.76s   p(90)=11.6µs   p(95)=87.16µs
http_req_connecting............: avg=816.17µs    min=0s            med=0s       max=7.76s   p(90)=0s       p(95)=0s
✓ http_req_duration..............: avg=285.73ms    min=-19761814ns   med=2.61ms   max=13.69s  p(90)=116.21ms p(95)=1.24s
  { expected_response:true }...: avg=291.25ms    min=-19761814ns   med=2.61ms   max=13.69s  p(90)=133.06ms p(95)=1.27s
✗ http_req_failed................: 2.47%  (270 out of 10911)
http_req_receiving.............: avg=850.22µs    min=-22408572ns   med=110.05µs max=7.61s   p(90)=300.4µs  p(95)=413.29µs
http_req_sending...............: avg=38.69µs     min=4.15µs        med=35.15µs  max=810.6µs p(90)=60.33µs  p(95)=85.59µs
http_req_tls_handshaking.......: avg=0s          min=0s            med=0s       max=0s      p(90)=0s       p(95)=0s
http_req_waiting...............: avg=284.84ms    min=0s            med=2.45ms   max=13.69s  p(90)=115.4ms  p(95)=1.23s
http_reqs......................: 10911  (101.13/s)
iteration_duration.............: avg=35.68s      min=2.04ms        med=37.21s   max=49.3s   p(90)=47.87s   p(95)=48.4s
iterations.....................: 525    (4.87/s)
payment_time...................: avg=1935.29ms   min=-7982         med=950      max=13620   p(90)=6048.9ms  p(95)=6251.05ms
queue_wait_time................: avg=30          min=30            med=30       max=30      p(90)=30       p(95)=30
reserve_time...................: avg=1708.90ms   min=-7942         med=890.5    max=13697   p(90)=5941.4ms  p(95)=6245.2ms
schedule_time..................: avg=3.39ms      min=-8096         med=3        max=7620    p(90)=4        p(95)=5
token_time.....................: avg=31.88ms     min=-8116000000ns med=6ms      max=7.8s    p(90)=9ms      p(95)=11ms
vus............................: 10   (min=9, max=300)
vus_max........................: 300  (min=300, max=300)
```

{% hint style="info" %}
#### **HTTP REQUEST 분석 내용**
{% endhint %}

<figure><img src=".gitbook/assets/image (1).png" alt=""><figcaption><p>Grafana Dashboard 의 HTTP REQUEST</p></figcaption></figure>

1. `/reservations/:id/payment` <mark style="color:yellow;">(500)</mark>
   1. Count=1로 500 에러가 1건 발생
   2. 단발성 에러(테스트 중 특정 케이스)일 가능성이 높음
2. `/reservations/:id/payment`<mark style="color:green;">(200)</mark>
   1. 다수의 정상 결제가 발생(예: COUNT=100+).&#x20;
   2. AVG는 수 ms수십 ms 정도로 보이지만, MAX가 최대 7초까지 치솟고, P95/P99 구간에서 37초대가 관측됨
   3. 일부 요청에서 병목이 발생했을 가능성이 크므로 모니터링 필요
3. &#x20;`/reservations/:id/reserve-seats` <mark style="color:green;">(200)</mark>
   1. 비교적 요청 수(Count)가 적은 편이지만, MIN/AVG/MAX 모두 수 ms 내외로 매우 빠른 응답
   2. 좌석 예약 로직이 빠르게 처리되는 케이스가 많음
4. `concerts/:id/dates/available`, `/concerts/:id/seats/available`
   1. AVG가 ms 단위로 빠르지만, 간헐적으로 MAX가 수 초(최대 6s)까지 올라가는 경우가 존재
   2. 캐시 미스나 DB 일시 부하, 네트워크 지연 등 특정 상황에서 지연이 발생했을 가능성이 있음
5. `/tokens/issue`, `/tokens/status`
   1. 주로 AVG가 수 ms\~수십 ms 수준으로 안정적
   2. 토큰 발급/상태 조회 로직이 Redis를 통한 캐싱/조회로 빠르게 동작하고 있음을 시사
6. `/users/status`
   1. 호출 빈도가 상대적으로 낮고, 평균 응답 시간이 4.5ms, 최대 9ms 수준
   2. 사용자 정보 조회가 크게 병목을 일으키지 않는 것으로 보임

{% hint style="info" %}
#### **핵심 지표 분석 내용**
{% endhint %}

<figure><img src=".gitbook/assets/image (2).png" alt=""><figcaption></figcaption></figure>

1. **Peak RPS**: 약 **208** RPS로, 트래픽이 몰리는 구간에서 초당 200건 이상의 요청
2. **Max Response Time**: **13.7초**
3. **Error Rate**: 약 **2.47%** (HTTP Failures: **270**건)
4. **VU Max**: 동시 접속자 **300**
5. **Average Response Time**: **504.01ms**
6. **Total Requests**: 약 **10910**건 처리

> 대부분 요청은 평균 504ms 내로 처리되지만, 일부 요청이 최대 13.7초까지 지연됨

* **결제(payment) API**: 대다수 요청은 `수십 ms ~ 수백 ms`이지만, 일부 요청이 7초 이상 지연되어 P95/P99 구간이 높음
* **예약(reserve-seats) API**: 응답 시간이 매우 짧고 안정적
* **일정/좌석 조회**: 평균은 ms대이지만, 캐시 미스나 DB 부하 시 수 초 단위로 지연되는 사례 존재
* **토큰 발급/조회**: Redis 전환 후 대체로 빠르고 안정적

***

## **5. 종합 평가 및 결론**&#x20;

### **5-1. 전체 시스템 성능 요약**

* **안정성**: 테스트 결과 HTTP 실패율(http\_req\_failed)은 약 2.47%로, 성공률은 약 97.5% (개선 필요)
* **확장성**: 최대 300 VU(동시 사용자)에서 안정적으로 작동하였으며, 시나리오 테스트 기준으로는 초당 약 101건의 요청을 처리할 수 있음 (Queue 대기 시간 고려 필요)
* **응답성**:  대부분의 API는 평균 285.73ms의 응답 시간을 보이나, 일부 요청에서는 최대 13.69초까지 지연된 사례 조회
* **일관성**: 평균 응답 시간 대비 P95가 1.24초 등 일부 극단적 지연 사례가 존재하나, 전반적으로는 일관된 성능을 유지

### **5-2. 최종 결론**

* 토큰 발급, 일정 조회, 좌석 예약, 결제 프로세스 전체 성능 개선
* 트랜잭션 충돌과 캐싱 부족 문제 해결, 실패율 감소
* 인덱스 최적화를 통해 주요 API 응답 속도 개선

### **5-3. 개선 영역**

* **응답 시간 일관성**:
  * 일부 API에서 95번째 백분위 응답 시간이 평균보다 크게 높아 개선할 필요
* **에러 처리**:
  * 현재 성공률은 약 97.5%로, 에러 원인 분석 필요
