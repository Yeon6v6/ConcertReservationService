-- 테이블 데이터 초기화
DELETE FROM user;
DELETE FROM token;
DELETE FROM concert;
DELETE FROM concert_schedule;
DELETE FROM seat;
DELETE FROM reservation;

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
INSERT INTO concert_schedule (id, concert_id, schedule_date, is_sold_out) VALUES
(1, 1, '2025-01-15', false),
(2, 1, '2025-01-16', false),
(3, 2, '2025-01-17', true);

-- seat 테이블 초기 데이터
INSERT INTO seat (id, seat_number, concert_id, schedule_date, status, price) VALUES
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

-- reservation 테이블 초기 데이터
INSERT INTO reservation (id, user_id, seat_id, seat_number, concert_id, schedule_date, status, expired_at, price, paid_at) VALUES
(1, 1, 3, 3, 1, '2025-01-15', 'PENDING', '2025-01-20 23:59:59', 50000, NULL),
(2, 2, 5, 2, 1, '2025-01-16', 'PAID', '2025-01-20 23:59:59', 75000, '2025-01-10 12:00:00'),
(3, 3, 6, 1, 2, '2025-01-17', 'PENDING', '2025-01-25 23:59:59', 100000, NULL);