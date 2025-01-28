# 쿼리 성능 현황

## 사전 데이터 세팅

1\) 콘서트 스케줄 1,000,000건 추가

```sql
-- ConcertSchedule 데이터 삽입 (1,000,000건)
INSERT INTO concert_schedule (concert_id, schedule_date, is_sold_out)
SELECT
    FLOOR(RAND() * 100 + 1) AS concert_id, -- 랜덤 콘서트 ID
    DATE_ADD('2025-01-01', INTERVAL FLOOR(RAND() * 365) DAY) AS schedule_date, -- 랜덤 일정
    RAND() > 0.5 AS is_sold_out -- 랜덤 매진 여부
FROM
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS tmp1,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS tmp2,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS tmp3,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS tmp4,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS tmp5;
```

2\) 각 콘서트마다 좌석 50개씩 추가(예약 랜덤 상태)

```sql
-- Seat 데이터 삽입 (스케줄 당 좌석 50개)
INSERT INTO seat (seat_number, concert_id, schedule_date, status, price)
SELECT
    seat_number, -- 좌석 번호 (1~50)
    cs.concert_id, -- 스케줄의 콘서트 ID
    cs.schedule_date, -- 스케줄 날짜
    CASE
        WHEN RAND() > 0.8 THEN 'RESERVED'
        ELSE 'AVAILABLE'
        END AS status,
    FLOOR(RAND() * 100000) AS price
FROM concert_schedule cs
         CROSS JOIN (
    SELECT @seat_num := @seat_num + 1 AS seat_number
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS a,
        (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) AS b,
        (SELECT @seat_num := 0) AS init
) seat_gen
WHERE seat_number <= 50;
```

***

## 쿼리 지연이 발생할 수 있는 로직

### <mark style="background-color:yellow;">**ConcertScheduleRepository**</mark>

* **`findByConcertIdAndIsSoldOut`**
  * 특정 콘서트 ID와 매진 여부를 기준으로 데이터를 조회
  * 데이터 양이 많으면 풀 테이블 스캔 가능성 有

#### 실행 계획

```sql
EXPLAIN 
SELECT * FROM concert_schedule 
WHERE concert_id = 1 AND is_sold_out = 1;
```

<figure><img src="../.gitbook/assets/image (29).png" alt=""><figcaption><p>결과 값</p></figcaption></figure>

* [x] type : ALL \
  &#xNAN;_⇒ 테이블 풀 스캔_
* [x] key : NULL \
  &#xNAN;_⇒ 인덱스 사용 안함_
* [x] Extra : Using where \
  &#xNAN;_⇒ 조건절로만 필터링 진행_



* **`findSchedule`**:
  * `concertId`와 `scheduleDate`를 조건으로 조회
    * 적절한 인덱스가 없으면 성능 저하

#### 실행 계획

```sql
EXPLAIN 
SELECT * FROM concert_schedule 
WHERE concert_id = 1 AND schedule_date = '2025-01-01';
```

<figure><img src="../.gitbook/assets/image (31).png" alt=""><figcaption><p>결과 값</p></figcaption></figure>

* [x] type : ALL \
  &#xNAN;_⇒ 테이블 풀 스캔_
* [x] key : NULL \
  &#xNAN;_⇒ 인덱스 사용 안함_
* [x] Extra : Using where \
  &#xNAN;_⇒ 조건절로만 필터링 진행_



### <mark style="background-color:yellow;">**SeatRepository**</mark>

* **`findAvailableSeatList`**:
  * 좌석 상태(`status = 'AVAILABLE'`), 콘서트 ID, 일정 기준으로 조회
  * 데이터가 많으면 풀 테이블 스캔 가능

#### 실행 계획

```sql
EXPLAIN SELECT * 
FROM seat 
WHERE concert_id = 1 
  AND schedule_date = '2025-01-01' 
  AND status = 'AVAILABLE';
```

<figure><img src="../.gitbook/assets/image (33).png" alt=""><figcaption><p>결과 값</p></figcaption></figure>

* [x] type : ALL \
  &#xNAN;_⇒ 테이블 풀 스캔_
* [x] rows : 156114\
  &#xNAN;_⇒ 조회에 사용된 행의 수_

- **`countAvailableSeats`**:
  * `COUNT` 연산은 대량 데이터에서 성능이 저하될 가능성이 높음
  * 대량의 조건 필터링 후 집계 연산

#### 실행 계획

```sql
EXPLAIN SELECT COUNT(*) 
FROM seat 
WHERE concert_id = 1 
  AND schedule_date = '2025-01-01' 
  AND status = 'AVAILABLE';
```

<figure><img src="../.gitbook/assets/image (34).png" alt=""><figcaption><p>결과 값</p></figcaption></figure>

* [x] type : ALL \
  &#xNAN;_⇒ 테이블 풀 스캔_
* [x] Extra : Using where \
  &#xNAN;_⇒ 조건절로만 필터링 진행_

