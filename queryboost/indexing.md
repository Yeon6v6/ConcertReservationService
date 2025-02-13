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

## 1. **기본 기능 인덱싱 적용**

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

### **b. (단일)좌석 조회**

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
  * 1️⃣**날짜 필터링 속도 개선**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **사용자 기준 데이터 조회 최적화**
    * `WHERE concert_id = ? AND schedule_date = ? AND status = ?`
    *   concert\_id, schedule\_date, status는 예약 가능한 좌석 조회 시 항상 사용하는 필터 조건이고,

        해당 조건들이 인덱스 없이 검색 될 경우 Full Table Scan이 발생한다

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

### **b. 특정 (인기) 콘서트 좌석 현황을 한 페이지에서 모두 조회**

* **인덱스가 필요한 이유**
  * 1️⃣콘섵, 날짜별 좌석 조회 최적화
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **OREDR BY 정렬 속도 개선**
    *

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

### **c. 특정 사용자의 예약 및 결제 내역 통계 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**예약(reservation)과 사용자(user) 조인 성능 개선**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **사용자별 예약 내역 조회 최적화**
    * `WHERE concert_id = ? AND schedule_date = ? AND status = ?`
    *   concert\_id, schedule\_date, status는 예약 가능한 좌석 조회 시 항상 사용하는 필터 조건이고,

        해당 조건들이 인덱스 없이 검색 될 경우 Full Table Scan이 발생한다

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

### **e. 좌석 예약 및 취소 이력 조회**

* **인덱스가 필요한 이유**
  * 1️⃣**취소된 예약에 대한 필터링 속도 개선**
    * 예약 가능한 좌석 조회는 가장 자주 호쵤되는 쿼리 중 하나이며, 매번 전체 테이블을 스캔하면 성능 저하가 발생될 수 있다&#x20;
    * 추가적으로 동시 예약 요청이 많은 콘서트 시스템의 경우, 데이터 양이 많아질수록 응답 시간이 길어지게 된다
  * 2️⃣ **날짜 정렬 성능 최적화**
    * `WHERE concert_id = ? AND schedule_date = ? AND status = ?`
    *   concert\_id, schedule\_date, status는 예약 가능한 좌석 조회 시 항상 사용하는 필터 조건이고,

        해당 조건들이 인덱스 없이 검색 될 경우 Full Table Scan이 발생한다

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
