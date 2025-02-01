-- 테이블 데이터 초기화
DELETE FROM user WHERE 1=1;
DELETE FROM token WHERE 1=1;
DELETE FROM concert WHERE 1=1;
/*DELETE FROM concert_schedule WHERE 1=1;
DELETE FROM seat WHERE 1=1;*/
-- 대용량 데이터 초기화를 위해 TRUNCATE로 변경
TRUNCATE TABLE concert_schedule;
TRUNCATE TABLE seat;
DELETE FROM reservation WHERE 1=1;

-- user 테이블 초기 데이터
INSERT INTO user (id, balance) VALUES
(1, 5000),
(2, 10000),
(3, 7500),
(4, 0),
(5, 2000);

-- token 테이블 초기 데이터
INSERT INTO token (id, token, user_id, status, expired_at, max_expired_at, created_at) VALUES
(1, 'token-1', 1, 'ACTIVE', '2025-01-15 23:59:59', '2025-01-20 23:59:59', '2025-01-01 10:00:00'),
(2, 'token-2', 2, 'EXPIRED', '2025-01-10 23:59:59', '2025-01-15 23:59:59', '2025-01-02 11:00:00'),
(3, 'token-3', 3, 'ACTIVE', '2025-01-25 23:59:59', '2025-01-30 23:59:59', '2025-01-03 12:00:00');

-- concert 테이블 초기 데이터
INSERT INTO concert (id) VALUES
                             (1),
                             (2);

-- Field 'is_sold_out' doesn't have a default value
-- concert_schedule 테이블 초기 데이터
/*INSERT INTO concert_schedule (id, concert_id, schedule_date, is_sold_out) VALUES
(1, 1, '2025-01-15', false),
(2, 1, '2025-01-16', false),
(3, 2, '2025-01-17', true);
*/
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

-- seat 테이블 초기 데이터
/*INSERT INTO seat (id, seat_number, concert_id, schedule_date, status, price) VALUES
-- 2025-01-15 공연 좌석
(1, 1, 1, '2025-01-15', 'AVAILABLE', 50000),
(2, 2, 1, '2025-01-15', 'AVAILABLE', 70000),
(3, 3, 1, '2025-01-15', 'RESERVED', 35000),
-- 2025-01-16 공연 좌석
(4, 1, 1, '2025-01-16', 'AVAILABLE', 62000),
(5, 2, 1, '2025-01-16', 'RESERVED', 150000),
-- 2025-01-17 공연 좌석
(6, 1, 2, '2025-01-17', 'RESERVED', 200000),
(7, 2, 2, '2025-01-17', 'RESERVED', 180000);
*/
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

-- reservation 테이블 초기 데이터
INSERT INTO reservation (id, user_id, seat_id, seat_number, concert_id, schedule_date, status, expired_at, price, paid_at) VALUES
(1, 1, 3, 3, 1, '2025-01-15', 'PENDING', '2025-01-20 23:59:59', 50000, NULL),
(2, 2, 5, 2, 1, '2025-01-16', 'PAID', '2025-01-20 23:59:59', 75000, '2025-01-10 12:00:00'),
(3, 3, 6, 1, 2, '2025-01-17', 'PENDING', '2025-01-25 23:59:59', 100000, NULL);