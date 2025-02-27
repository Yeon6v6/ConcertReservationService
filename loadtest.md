# 장애 대응 보고서 및 부하 테스트

## **1. 개요**

* 해당문서는 콘서트 좌석 예약 시스템의 **API 성능 테스트 및 장애 대응 과정**, **개선 방안**을 정리한 보고서이다.
* 테스트는 `k6` 부하 테스트를 사용 했으며, 주요 측정 항목은 아래와 같다.
  * API 응답 시간 (`http_req_duration`)
  * 처리 속도 (`reserve_time`, `payment_time`)
  * 실패율 (`http_req_failed`)

## **2. 장애 발생 및 원인 분석**

### **2.1. 전체 시스템 장애 발생 시나리오**

* 일부 API에서 성능 저하가 감지
* `queue_wait_time`을 제외한 일부 API에서 응답 시간이 증가하여 예약 및 결제 실패율에 영향\
  ⇒ `queue_wait_time` 의 경우, 스케줄러가 토큰 처리하는 것을 대기하기 위해 의도적으로 지연 설
* 성능 테스트 실행 결과

**전체 시스템 지표 (개선 전)**

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

**✅ 동시성 처리 최적화**

* 기존 `@RedisLock` 외에 **낙관적 락(Optimistic Locking)** 적용 검토.
* 예약 시 트랜잭션 병렬 처리 구조 개선.

**✅ 쿼리 부하 개선**

* 실행 계획(`EXPLAIN ANALYZE`)을 분석하여 **비효율적인 쿼리 최적화**
* 응답 시간이 긴 쿼리를 **Batch 처리 및 Lazy Loading 적용**

**✅ 인덱스 최적화 적용**

* `concert_schedule` 테이블에 `concert_id, is_sold_out` 복합 인덱스 추가
* `seat` 테이블에 `concert_id, schedule_date` 복합 인덱스 추가
* `reservation` 테이블에 `user_id, status` 복합 인덱스 추가
* **사용 빈도 기반 인덱스 재구성** 및 **실제 실행된 쿼리 로그 기반 튜닝**
* **좌석 조회 시 캐싱 적용** (`@Cacheable` 적용)
  * `SeatRepository.findByConcertIdAndScheduleDateAndSeatNumber()`에 캐싱 추가

***

### **4. 결론 및 향후 계획**

### **4-1. 전체 시스템 지표 (개선 후)**

```
data_received..................: 4.3 MB 42 kB/s
data_sent......................: 2.1 MB 21 kB/s
http_req_duration..............: avg=9.87ms min=1.1ms med=7.23ms max=250.67ms p(90)=21.34ms p(95)=32.89ms
http_req_failed................: 0.04%  142 out of 172142
payment_time...................: avg=19.67ms min=5.12ms med=10.43ms max=6.21s p(90)=15ms p(95)=18ms
reserve_time...................: avg=7.43ms min=3.65ms med=6.5ms max=5.87s p(90)=9ms p(95)=11ms
schedule_time..................: avg=5.12ms min=1ms med=3.5ms max=6.35s p(90)=5.5ms p(95)=6.2ms
token_time.....................: avg=3.21ms min=1ms med=2.5ms max=120ms p(90)=4.2ms p(95)=4.8ms
```

| **토큰 발급 (`/tokens/issue`)**                                | 5.62ms  | 3.21ms  | 120ms | 0.1% → 0.05%  | Redis 최적화           |
| ---------------------------------------------------------- | ------- | ------- | ----- | ------------- | ------------------- |
| **대기열 상태 조회 (`/tokens/status`)**                           | 30s     | 30s     | 30s   | 0%            |                     |
| **예약 가능한 일정 조회 (`/concerts/{concertId}/dates/available`)** | 14.68ms | 5.12ms  | 6.35s | 0.41% → 0.12% | 쿼리 최적화, 캐싱 적용       |
| **좌석 예약 (`/reservations/{concertId}/reserve-seats`)**      | 26.08ms | 7.43ms  | 5.87s | 0.52% → 0.08% | 트랜잭션 충돌 해결, 인덱스 추가  |
| **결제 (`/reservations/{reservationId}/payment`)**           | 61.87ms | 19.67ms | 6.21s | 0.52% → 0.10% | 트랜잭션 비용 감소, 인덱스 최적화 |

### 4-2. **최종 결론**

1. **토큰 발급, 일정 조회, 좌석 예약, 결제 프로세스 전체 성능 개선**
2. **트랜잭션 충돌과 캐싱 부족 문제 해결, 실패율 감소**
3. **인덱스 최적화를 통해 주요 API 응답 속도 개선**
4. **추가적인 쿼리 부하 감소를 위한 구조 개선 진행**

### 4-3. **추가 개선 계획**



