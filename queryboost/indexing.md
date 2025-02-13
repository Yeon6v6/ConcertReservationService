# 인덱싱을 통한 성능 개선

## 0. **테스트 환경**

* **서버** : Spring Boot + JPA + MySQL
* **테스트 도구**
  * MySQL CLI (쿼리 성능 확인)
* **테스트 내용**
  * ✔️쿼리 실행 시간 비교
  * ✔️더미 데이터로 인덱스 추가 전후 쿼리 성능 테스트
* **테스트 한 API**
  * 예약 가능한 좌석 조회 API
  * 동시 예약 요청 처리 API
* **테스트 데이터 수** \
  ⇒ (더 많은 데이를 넣을 경우 시간이 너무 오래걸림..)
  * user = 1,005
  * concert = 1,000
  * concert\_schedule = 3,990
  * seat = 119,700
  * reservation = 1,000

## 1. **기본 기능 쿼리에 대한 인덱스 생성**

### **a. 예약 가능한 좌석 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**잦은 조회와 데이터 양 증가**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **WHERE 조건의 다중 컬럼 사용**
    * `WHERE concert_id = ? AND schedule_date = ? AND status = ?`
    *   concert\_id, schedule\_date, status는 예약 가능한 좌석 조회 시 항상 사용하는 필터 조건이고,

        해당 조건들이 인덱스 없이 검색 될 경우 Full Table Scan이 발생한다
  * **3️⃣콘서트 상태 값 처리 시에도 사용**
    * 콘서트 SoldOut 처리 시에도 그룹핑하여 사용하기 때문에 다른 쿼리에 비해 효율이 좋다

#### <mark style="background-color:red;">**인덱스 생성**</mark>

> CREATE INDEX idx\_seat\_concert\_schedule\_status ON seat (concert\_id, schedule\_date, status);

```bash
mysql> CREATE INDEX idx_seat_concert_schedule_status ON seat (concert_id, schedule_date, status);
Query OK, 0 rows affected (1.20 sec)
Records: 0  Duplicates: 0  Warnings: 0
```

* **인덱스 적용 이유**
  * **☑️Cardinality(선택도) 고려**
    * concert\_id와 schedule\_date는 다양한 값을 가지며, 선택도가 높기 때문에 다른 컬럼에  비해인덱스 효율 좋다
      * `concert_id` ⇒ 많은 값이 존재 (선택도가 높다)
      * `schedule_date` ⇒ 날짜별로 정렬될 수 있음 (검색 속도가 빠름)
    * 반면 `status`는 `AVAILABLE`, `RESERVED`등 값이 적어 선택도가 낮지만, \
      `concert_id`와 `schedule_date`로 대부분의 필터링이 이러우진 이후에 `status`로 추가 필터링하므로 효율적일 수 있다
    * 따라서 **Cardinality**가 높은 `concert_id`와 `schedule_date`를 먼저 배치하고, 이후 `status`를 마지막에 배치
* **💡잘못된 인덱스 설계 예시**&#x20;
  * `CREATE INDEX idx_seat_status_schedule ON seat (status, schedule_date);`&#x20;
  * 비효율적인 이유
    * `status`는 값이 AVAILABLE, RESERVED 등 몇 개 안 됨 → 선택도가 낮음&#x20;
    * `schedule_date`는 많이 사용되지만, 첫 번째 컬럼이 status일경우 인덱스를 제대로 활용 못 함

#### <mark style="background-color:yellow;">**인덱스 적용 전**</mark>&#x20;

> **EXPLAIN** SELECT \* FROM seat WHERE concert\_id = 351 AND schedule\_date = '2025-04-08' AND status = 'AVAILABLE';

```bash
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
|  1 | SIMPLE      | seat  | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 119100 |     0.33 | Using where |
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
```

> **EXPLAIN** **ANALYZE** SELECT \* FROM seat WHERE concert\_id = 351 AND schedule\_date = '2025-04-08' AND status = 'AVAILABLE';

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Index lookup on seat using idx_seat_concert_schedule_status (concert_id=351, schedule_date=DATE'2025-04-08', status='AVAILABLE')
  , with index condition:
    (seat.`status` = 'AVAILABLE')
    (cost=12014 rows=119100)ws=1)
|   (actual time=0.906..32.4 rows=119280 loops=1)
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.05 sec)
```

* Table Scan 발생 ⇒ 전체 테이블 탐색하면서 데이터를 찾는다
* 데이터가 많아질수록 O(n) 시간 소요
* cost=12014 rows=119100 ⇒ 쿼리가 너무 많은 데이터를 훑어본다 (낮은 효율성)

#### <mark style="background-color:yellow;">**인덱스 적용 후**</mark>

> **EXPLAIN** SELECT \* FROM seat WHERE concert\_id = 351 AND schedule\_date = '2025-04-08' AND status = 'AVAILABLE';

```bash
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
| id | select_type | table | partitions | type | possible_keys                    | key                              | key_len | ref               | rows | filtered | Extra                 |
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
|  1 | SIMPLE      | seat  | NULL       | ref  | idx_seat_concert_schedule_status | idx_seat_concert_schedule_status | 12      | const,const,const |    1 |   100.00 | Using index condition |
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
```

> **EXPLAIN** **ANALYZE** SELECT \* FROM seat WHERE concert\_id = 351 AND schedule\_date = '2025-04-08' AND status = 'AVAILABLE';

<pre class="language-bash"><code class="lang-bash">+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Index lookup on seat using idx_seat_concert_schedule_status (concert_id=351, schedule_date=DATE'2025-04-08', status='AVAILABLE')
  , with index condition: 
<strong>  (seat.`status` = 'AVAILABLE')  
</strong>  (cost=0.35 rows=1) 
| (actual time=0.0236..0.0236 rows=0 loops=1)
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
</code></pre>

#### <mark style="background-color:blue;">**인덱스 추가 전후 차이**</mark>

<table><thead><tr><th width="257">구분</th><th>인덱스 추가 전</th><th>인덱스 추가 후</th></tr></thead><tbody><tr><td>실행 계획</td><td>Full Table Scan</td><td>Index Scan</td></tr><tr><td>쿼리 실행 시간 (actual time)</td><td>32.4ms</td><td>0.02ms</td></tr><tr><td>행 스캔   (rows)</td><td>119,280</td><td>1</td></tr></tbody></table>

***

### **b. (단일) 좌석 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**id는 자주 사용하는 기본키 기반 조회 컬럼**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있음
    * 추가적으로 인기있는 콘서트에서, 인기있는 좌석의 경우에는 seat 테이블에 데이터가 많을 경우 모든 좌석 데이터를 순차적으로 조회(Full Table Scan)해야하기 때문에 성능 저하가 발생
  * 2️⃣ **특정 좌석이 예약되었는지 확인 할 때도 사용(임시 예약, 결제 요청 시)**
    * MySQL의 **`FOR UPDATE`**&#xB97C; 사용하여, 행 수준 잠금(Row Lock)을 사용 할 경우 인덱스가 없다면 전체 테이블을 스캔하여 조건에 맞는 행을 찾고 잠그기 때문에 Full Table Scan이 발생

❗MySql 에서 기본키는 자동으로 클러스터형 인덱스(Clustered Index)가 생성되므로, 별도의 인덱스를 생성 할 필요가 없다. (혹시나.. id가 기본키가 아니라면..ㅎㅎ.. 생성 필요)



## **2. 기본 기능 외 지연 발생할 수 있는 쿼리**&#x20;

### **a. 사용자가 최근 30일간 결제한 모든 내역 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**잦은 조회와 데이터 양 증가**
    * 사용자별 결제 내역은 자주 조회되는 쿼리 중 하나이며, 예약 테이블(reservation)이 커질수록 성능 저하 발생 가능&#x20;
    * 최근 30일 데이터를 `paid_at`을 기준으로 필터링하는데, 인덱스가 없으면 Full Table Scan이 발생할 수 있다
  * 2️⃣ **사용자 기준 데이터 조회 최적화**
    * `WHERE user_id = ? AND paid_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)`
    * user\_id와 paid\_at을 조합한 필터링이므로, 복합 인덱스가 없으면 Full Scan 발생 가능
  * **3️⃣정렬 및 필터링 최적화**
    * 결제 내역을 paid\_at 기준으로 정렬할 가능성이 높으므로, 해당 인덱스가 정렬 성능까지 개선 가능

#### <mark style="background-color:red;">**인덱스 생성**</mark>

> CREATE INDEX idx\_reservation\_user\_paid ON reservation (user\_id, paid\_at);

```bash
mysql> CREATE INDEX idx_reservation_user_paid ON reservation (user_id, paid_at);
Query OK, 0 rows affected (0.07 sec)
Records: 0  Duplicates: 0  Warnings: 0
```

* **인덱스 적용 이유**
  * **☑️Cardinality(선택도)**
    * `user_id` ⇒ 사용자의 예약 내역을 빠르게 찾기 위해 필요
    * `paid_at` ⇒ 날짜 범위를 기준으로 필터링을 최적화하기 위해 필요

#### <mark style="background-color:yellow;">**인덱스 적용 전**</mark>&#x20;

> **EXPLAIN** \
> SELECT r.id, r.price, r.paid\_at, s.seat\_number, c.id AS concert\_id \
> FROM reservation r \
> &#x20; JOIN seat s ON r.seat\_id = s.id \
> &#x20; JOIN concert c ON s.concert\_id = c.id \
> WHERE r.user\_id = 1001 AND r.paid\_at >= DATE\_SUB(NOW(), INTERVAL 30 DAY);

```bash
+----+-------------+-------+------------+--------+---------------+---------+---------+---------------------+------+----------+-------------+
| id | select_type | table | partitions | type   | possible_keys | key     | key_len | ref                 | rows | filtered | Extra       |
+----+-------------+-------+------------+--------+---------------+---------+---------+---------------------+------+----------+-------------+
|  1 | SIMPLE      | r     | NULL       | ALL    | NULL          | NULL    | NULL    | NULL                | 1000 |     3.33 | Using where |
|  1 | SIMPLE      | s     | NULL       | eq_ref | PRIMARY       | PRIMARY | 8       | hhplus.r.seat_id    |    1 |   100.00 | NULL        |
|  1 | SIMPLE      | c     | NULL       | eq_ref | PRIMARY       | PRIMARY | 8       | hhplus.s.concert_id |    1 |   100.00 | Using index |
+----+-------------+-------+------------+--------+---------------+---------+---------+---------------------+------+----------+-------------+
```

> **EXPLAIN** **ANALYZE** \
> SELECT r.id, r.price, r.paid\_at, s.seat\_number, c.id AS concert\_id \
> FROM reservation r \
> &#x20; JOIN seat s ON r.seat\_id = s.id \
> &#x20; JOIN concert c ON s.concert\_id = c.id \
> WHERE r.user\_id = 1001 AND r.paid\_at >= DATE\_SUB(NOW(), INTERVAL 30 DAY);

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Nested loop inner join  (cost=118 rows=33.3) (actual time=0.46..0.46 rows=0 loops=1)
    -> Nested loop inner join  (cost=107 rows=33.3) (actual time=0.459..0.459 rows=0 loops=1)
        -> Filter: ((r.user_id = 1001) and (r.paid_at >= <cache>((now() - interval 30 day))))  (cost=95.1 rows=33.3) (actual time=0.458..0.458 rows=0 loops=1)
            -> Table scan on r  (cost=95.1 rows=1000) (actual time=0.0549..0.392 rows=1000 loops=1)
        -> Single-row index lookup on s using PRIMARY (id=r.seat_id)  (cost=0.253 rows=1) (never executed)
    -> Single-row covering index lookup on c using PRIMARY (id=s.concert_id)  (cost=0.253 rows=1) (never executed)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.01 sec)
```

* Table Scan (r 테이블) 발생 ⇒ 전체 테이블 탐색하면서 데이터를 찾는다
* 데이터가 많아질수록 O(n) 시간 소요

#### <mark style="background-color:yellow;">**인덱스 적용 후**</mark>

> **EXPLAIN** \
> SELECT r.id, r.price, r.paid\_at, s.seat\_number, c.id AS concert\_id \
> FROM reservation r \
> &#x20; JOIN seat s ON r.seat\_id = s.id \
> &#x20; JOIN concert c ON s.concert\_id = c.id \
> WHERE r.user\_id = 1001 AND r.paid\_at >= DATE\_SUB(NOW(), INTERVAL 30 DAY);

```bash
+----+-------------+-------+------------+--------+---------------------------+---------------------------+---------+---------------------+------+----------+-----------------------+    
| id | select_type | table | partitions | type   | possible_keys             | key                       | key_len | ref                 | rows | filtered | Extra                 |    
+----+-------------+-------+------------+--------+---------------------------+---------------------------+---------+---------------------+------+----------+-----------------------+    
|  1 | SIMPLE      | r     | NULL       | range  | idx_reservation_user_paid | idx_reservation_user_paid | 14      | NULL                |    1 |   100.00 | Using index condition |    
|  1 | SIMPLE      | s     | NULL       | eq_ref | PRIMARY                   | PRIMARY                   | 8       | hhplus.r.seat_id    |    1 |   100.00 | NULL                  |    
|  1 | SIMPLE      | c     | NULL       | eq_ref | PRIMARY                   | PRIMARY                   | 8       | hhplus.s.concert_id |    1 |   100.00 | Using index           |    
+----+-------------+-------+------------+--------+---------------------------+---------------------------+---------+---------------------+------+----------+-----------------------+ 
```

> **EXPLAIN** **ANALYZE** \
> SELECT r.id, r.price, r.paid\_at, s.seat\_number, c.id AS concert\_id \
> FROM reservation r \
> &#x20; JOIN seat s ON r.seat\_id = s.id \
> &#x20; JOIN concert c ON s.concert\_id = c.id \
> WHERE r.user\_id = 1001 AND r.paid\_at >= DATE\_SUB(NOW(), INTERVAL 30 DAY);

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Nested loop inner join  (cost=1.41 rows=1) (actual time=0.0194..0.0194 rows=0 loops=1)
    -> Nested loop inner join  (cost=1.06 rows=1) (actual time=0.0187..0.0187 rows=0 loops=1)
        -> Index range scan on r using idx_reservation_user_paid over (user_id = 1001 AND '2025-01-14 16:05:11' <= paid_at), with index condition: ((r.user_id = 1001) and (r.paid_at >= <cache>((now() - interval 30 day))))  (cost=0.71 rows=1) (actual time=0.0181..0.0181 rows=0 loops=1)
        -> Single-row index lookup on s using PRIMARY (id=r.seat_id)  (cost=0.35 rows=1) (never executed)
    -> Single-row covering index lookup on c using PRIMARY (id=s.concert_id)  (cost=0.35 rows=1) (never executed)
 |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
```

#### <mark style="background-color:blue;">**인덱스 추가 전후 차이**</mark>

<table><thead><tr><th width="257">구분</th><th>인덱스 추가 전</th><th>인덱스 추가 후</th></tr></thead><tbody><tr><td>실행 계획</td><td>Full Table Scan</td><td>Index Scan</td></tr><tr><td>쿼리 실행 시간 (actual time)</td><td>0.46ms</td><td>0.0194ms</td></tr><tr><td>행 스캔   (rows)</td><td>1,000</td><td>1</td></tr></tbody></table>

***

### **b. 특정 (인기) 콘서트 좌석 현황을 한 페이지에서 모두 조회**

* **인덱스가 필요한 이유**
  * 1️⃣조회 데이터 양 증가
    * 인기 콘서트의 좌석 정보를 한 번에 불러와야 하며, 좌석(seat) 테이블이 커질수록 성능 저하 발생 가능&#x20;
    * 좌석 상태(status)에 따라 필터링하는 경우도 고려 대상이 된다
  * 2️⃣ **WHERE 조건의 다중 컬럼 사용**
    * `WHERE concert_id = ? AND schedule_date BETWEEN ? AND ?`
    * concert\_id와 schedule\_date를 동시에 조회하므로, 복합 인덱스를 활용해야 성능 최적화 가능

#### <mark style="background-color:red;">**인덱스 생성**</mark>

> CREATE INDEX idx\_seat\_concert\_schedule\_status ON seat (concert\_id, schedule\_date, status);

```bash
mysql> CREATE INDEX idx_seat_concert_schedule_status ON seat (concert_id, schedule_date, status);
Query OK, 0 rows affected (1.20 sec)
Records: 0  Duplicates: 0  Warnings: 0
```

* **인덱스 적용 이유**
  * **☑️Cardinality(선택도) 고려**
    * `concertId` → 콘서트별 좌석을 필터링하는데 필수적&#x20;
    * `scheduleDate` → 콘서트 일정별 좌석을 조회하는 데 필요
    * `status` → AVAILABLE 또는 RESERVED 등 상태별 필터링 시 속도를 최적화
* ❗ 해당 인덱스는 이미 1-a에서 적용되어있다

#### <mark style="background-color:yellow;">**인덱스 적용 전**</mark>&#x20;

> **EXPLAIN** SELECT s.id, s.seat\_number, s.status, s.price FROM seat s WHERE s.concertId = 351 AND s.scheduleDate BETWEEN '2025-03-01' AND '2025-03-07' ORDER BY s.scheduleDate;

```bash
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-----------------------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra                       |
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-----------------------------+
|  1 | SIMPLE      | s     | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 119700 |     1.11 | Using where; Using filesort |
+----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-----------------------------+
```

> **EXPLAIN** **ANALYZE** SELECT s.id, s.seat\_number, s.status, s.price FROM seat s WHERE s.concertId = 351 AND s.scheduleDate BETWEEN '2025-03-01' AND '2025-03-07' ORDER BY s.scheduleDate;

<pre class="language-bash"><code class="lang-bash">+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Sort: s.schedule_date  (cost=12074 rows=119700) (actual time=40..40 rows=30 loops=1)
    -> Filter: ((s.concert_id = 351) and (s.schedule_date between '2025-03-01' and '2025-03-07'))  (cost=12074 rows=119700) (actual time=14.6..40 rows=30 loops=1)
        -> Table scan on s  (cost=12074 rows=119700) (actual time=0.962..33.4 rows=119700 loops=1)
<strong> |
</strong>+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.05 sec)
</code></pre>

#### <mark style="background-color:yellow;">**인덱스 적용 후**</mark>

> **EXPLAIN** SELECT s.id, s.seat\_number, s.status, s.price FROM seat s WHERE s.concertId = 351 AND s.scheduleDate BETWEEN '2025-03-01' AND '2025-03-07' ORDER BY s.scheduleDate;

```bash
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
| id | select_type | table | partitions | type | possible_keys                    | key                              | key_len | ref               | rows | filtered | Extra                 |
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
|  1 | SIMPLE      | seat  | NULL       | ref  | idx_seat_concert_schedule_status | idx_seat_concert_schedule_status | 12      | const,const,const |    1 |   100.00 | Using index condition |
+----+-------------+-------+------------+------+----------------------------------+----------------------------------+---------+-------------------+------+----------+-----------------------+
```

> **EXPLAIN** **ANALYZE** SELECT s.id, s.seat\_number, s.status, s.price FROM seat s WHERE s.concertId = 351 AND s.scheduleDate BETWEEN '2025-03-01' AND '2025-03-07' ORDER BY s.scheduleDate;

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Index range scan on s using idx_seat_concert_schedule_status over 
    (concert_id = 351 AND '2025-03-01' <= schedule_date <= '2025-03-07'), 
    with index condition: ((s.concert_id = 351) and (s.schedule_date between '2025-03-01' and '2025-03-07'))  
    (cost=13.8 rows=30) 
    (actual time=0.154..0.161 rows=30 loops=1)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
```

#### <mark style="background-color:blue;">**인덱스 추가 전후 차이**</mark>

<table><thead><tr><th width="257">구분</th><th>인덱스 추가 전</th><th>인덱스 추가 후</th></tr></thead><tbody><tr><td>실행 계획</td><td>Full Table Scan</td><td>Index Range Scan</td></tr><tr><td>쿼리 실행 시간 (actual time)</td><td>40ms</td><td>0.161ms</td></tr><tr><td>행 스캔   (rows)</td><td>119,700</td><td>30</td></tr></tbody></table>

***

### **c. 특정 사용자의 예약 및 결제 내역 통계 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**자주 실행되는 통계 쿼리**
    * 특정 사용자의 예약 내역을 통계`(COUNT(*), SUM(price))`로 조회하는 경우 성능 저하 발생 가능
    * 예약 테이블이 커질수록 필터링과 집계 연산이 느려질 수 있다
  * 2️⃣ **WHERE 조건의 다중 컬럼 사용**&#x20;
    * `WHERE user_id = ? AND paid_at IS NOT NULL`&#x20;
    * 특정 사용자 + 결제 완료된 예약을 필터링해야 하므로, 복합 인덱스가 필요하
  * **3️⃣집계 연산 성능 최적화**
    * `GROUP BY user_id`를 수행할 경우, 인덱스를 활용하면 불필요한 테이블 스캔을 줄일 수 있다

#### <mark style="background-color:red;">**인덱스 생성**</mark>

> CREATE INDEX idx\_reservation\_user\_paid\_status ON reservation (user\_id, paid\_at, status);

```bash
mysql> CREATE INDEX idx_seat_concert_schedule_status ON seat (concert_id, schedule_date, status);
Query OK, 0 rows affected (1.20 sec)
Records: 0  Duplicates: 0  Warnings: 0
```

* **인덱스 적용 이유**
  * **☑️Cardinality(선택도)**
    * `user_id` → 특정 사용자의 예약 데이터를 빠르게 찾기 위해 필요&#x20;
    * `paid_at` → 결제된 예약 내역을 필터링하는 데 최적화&#x20;
    * `status` → 결제 완료(PAID), 예약 취소(CANCELLED) 등 상태별 필터링 가능

#### <mark style="background-color:yellow;">**인덱스 적용 전**</mark>&#x20;

> **EXPLAIN** SELECT r.user\_id, COUNT(r.id) AS total\_reservations, SUM(r.price) AS total\_spent FROM reservation r WHERE r.user\_id = 1001 AND r.paid\_at IS NOT NULL GROUP BY r.user\_id;

```bash
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra       |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------------+
|  1 | SIMPLE      | r     | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 1000 |     9.00 | Using where |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------------+
```

> **EXPLAIN** **ANALYZE** SELECT r.user\_id, COUNT(r.id) AS total\_reservations, SUM(r.price) AS total\_spent FROM reservation r WHERE r.user\_id = 1001 AND r.paid\_at IS NOT NULL GROUP BY r.user\_id;

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Group aggregate: count(r.id), sum(r.price)  (cost=111 rows=31.6) (actual time=0.403..0.403 rows=0 loops=1)
    -> Filter: ((r.user_id = 1001) and (r.paid_at is not null))  (cost=102 rows=90) (actual time=0.401..0.401 rows=0 loops=1)
        -> Table scan on r  (cost=102 rows=1000) (actual time=0.0559..0.322 rows=1000 loops=1)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.05 sec)
```

#### <mark style="background-color:yellow;">**인덱스 적용 후**</mark>

> **EXPLAIN** SELECT r.user\_id, COUNT(r.id) AS total\_reservations, SUM(r.price) AS total\_spent FROM reservation r WHERE r.user\_id = 1001 AND r.paid\_at IS NOT NULL GROUP BY r.user\_id;

```bash
+----+-------------+-------+------------+-------+----------------------------------+----------------------------------+---------+------+------+----------+-----------------------+
| id | select_type | table | partitions | type  | possible_keys                    | key                              | key_len | ref  | rows | filtered | Extra                 |      
+----+-------------+-------+------------+-------+----------------------------------+----------------------------------+---------+------+------+----------+-----------------------+      
|  1 | SIMPLE      | r     | NULL       | range | idx_reservation_user_paid_status | idx_reservation_user_paid_status | 14      | NULL |    1 |   100.00 | Using index condition |      
+----+-------------+-------+------------+-------+----------------------------------+----------------------------------+---------+------+------+----------+-----------------------+  
```

> **EXPLAIN** **ANALYZE** SELECT r.user\_id, COUNT(r.id) AS total\_reservations, SUM(r.price) AS total\_spent FROM reservation r WHERE r.user\_id = 1001 AND r.paid\_at IS NOT NULL GROUP BY r.user\_id;

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Group aggregate: count(r.id), sum(r.price)  (cost=0.81 rows=1) (actual time=0.0183..0.0183 rows=0 loops=1)
    -> Index range scan on r using idx_reservation_user_paid_status over (user_id = 1001 AND NULL < paid_at), with index condition: ((r.user_id = 1001) and (r.paid_at is not null))  
        (cost=0.71 rows=1) 
        (actual time=0.0172..0.0172 rows=0 loops=1)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
```

#### <mark style="background-color:blue;">**인덱스 추가 전후 차이**</mark>

<table><thead><tr><th width="257">구분</th><th>인덱스 추가 전</th><th>인덱스 추가 후</th></tr></thead><tbody><tr><td>실행 계획</td><td>Full Table Scan</td><td>Index Range Scan</td></tr><tr><td>쿼리 실행 시간 (actual time)</td><td>0.403ms</td><td>0.0183ms</td></tr><tr><td>행 스캔   (rows)</td><td>1,000</td><td>1</td></tr></tbody></table>

***

### **d. 가장 많이 예약된 콘서트 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**콘서트 별 예약 건수 집계 속도 개선**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **대량 데이터 그룹화에 대한 죄적화**
    * `WHERE concert_id = ? AND schedule_date = ? AND status = ?`
    *   concert\_id, schedule\_date, status는 예약 가능한 좌석 조회 시 항상 사용하는 필터 조건이고,

        해당 조건들이 인덱스 없이 검색 될 경우 Full Table Scan이 발생한다
  * **3️⃣콘서트 상태 값 처리 시에도 사용**
    * 콘서트 SoldOut 처리 시에도 그룹핑하여 사용하기 때문에 다른 쿼리에 비해 효율이 좋음

#### <mark style="background-color:red;">**인덱스 생성**</mark>

> CREATE INDEX idx\_reservation\_concert ON reservation (concert\_id);

```bash
mysql> CREATE INDEX idx_reservation_concert ON reservation (concert_id);
Query OK, 0 rows affected (0.07 sec)
Records: 0  Duplicates: 0  Warnings: 0
```

* **인덱스 적용 이유**
  * **☑️예약 테이블이 커질수록 성능 저하 가능**
    * 콘서트별 예약 건수를 집계(COUNT(\*))하여 가장 인기 있는 콘서트를 찾는 쿼리이다.&#x20;
    * 예약 데이터가 많아질수록 GROUP BY concertId 연산이 부담될 수 있음
  * **☑️WHERE 조건이 없지만 GROUP BY 최적화 필요**
    * concertId를 기준으로 집계해야 하므로, 이 컬럼에 대한 인덱스가 있으면 성능 개선 가능

#### <mark style="background-color:yellow;">**인덱스 적용 전**</mark>&#x20;

> **EXPLAIN** SELECT r.concertId, COUNT(r.id) AS total\_reservations FROM reservation r GROUP BY r.concertId ORDER BY total\_reservations DESC LIMIT 10;

```bash
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra                           |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
|  1 | SIMPLE      | r     | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 1000 |   100.00 | Using temporary; Using filesort |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
```

> **EXPLAIN** **ANALYZE** SELECT r.concertId, COUNT(r.id) AS total\_reservations FROM reservation r GROUP BY r.concertId ORDER BY total\_reservations DESC LIMIT 10;

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|| -> Limit: 10 row(s)  (actual time=0.472..0.473 rows=8 loops=1)
    -> Sort: total_reservations DESC, limit input to 10 row(s) per chunk  (actual time=0.472..0.472 rows=8 loops=1)
        -> Table scan on <temporary>  (actual time=0.456..0.457 rows=8 loops=1)
            -> Aggregate using temporary table  (actual time=0.455..0.455 rows=8 loops=1)
                -> Table scan on r  (cost=102 rows=1000) (actual time=0.0329..0.237 rows=1000 loops=1)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
```

#### <mark style="background-color:yellow;">**인덱스 적용 후**</mark>

> **EXPLAIN** SELECT r.concertId, COUNT(r.id) AS total\_reservations FROM reservation r GROUP BY r.concertId ORDER BY total\_reservations DESC LIMIT 10;

```bash
+----+-------------+-------+------------+-------+-------------------------+-------------------------+---------+------+------+----------+----------------------------------------------+ 
| id | select_type | table | partitions | type  | possible_keys           | key                     | key_len | ref  | rows | filtered | Extra                                        | 
+----+-------------+-------+------------+-------+-------------------------+-------------------------+---------+------+------+----------+----------------------------------------------+ 
|  1 | SIMPLE      | r     | NULL       | index | idx_reservation_concert | idx_reservation_concert | 8       | NULL | 1000 |   100.00 | Using index; Using temporary; Using filesort | 
+----+-------------+-------+------------+-------+-------------------------+-------------------------+---------+------+------+----------+----------------------------------------------+ 
```

> **EXPLAIN** **ANALYZE** SELECT r.concertId, COUNT(r.id) AS total\_reservations FROM reservation r GROUP BY r.concertId ORDER BY total\_reservations DESC LIMIT 10;

```bash
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| EXPLAIN                                                                                                                                                                                         |
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| -> Limit: 10 row(s)  (actual time=0.295..0.296 rows=8 loops=1)
    -> Sort: total_reservations DESC, limit input to 10 row(s) per chunk  (actual time=0.294..0.295 rows=8 loops=1)
        -> Stream results  (cost=202 rows=8) (actual time=0.08..0.281 rows=8 loops=1)
            -> Group aggregate: count(r.id)  (cost=202 rows=8) (actual time=0.0775..0.276 rows=8 loops=1)
                -> Covering index scan on r using idx_reservation_concert  (cost=102 rows=1000) (actual time=0.0342..0.215 rows=1000 loops=1)
|
+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```

#### <mark style="background-color:blue;">**인덱스 추가 전후 차이**</mark>

<table><thead><tr><th width="257">구분</th><th>인덱스 추가 전</th><th>인덱스 추가 후</th></tr></thead><tbody><tr><td>실행 계획</td><td>Temporary Table 사용 + Full Table Scan</td><td>Covering Index Scan</td></tr><tr><td>쿼리 실행 시간 (actual time)</td><td>0.472ms</td><td>0.296ms</td></tr><tr><td>행 스캔   (rows)</td><td>1,000</td><td>1000 (Covering Index)</td></tr></tbody></table>

***
