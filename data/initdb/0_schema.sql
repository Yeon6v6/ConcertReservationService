SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS concert;
DROP TABLE IF EXISTS concert_schedule;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS seat;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS concert (
   id BIGINT AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS concert_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE
);
ALTER TABLE concert_schedule ADD CONSTRAINT uk_concert_schedule UNIQUE(concert_id, schedule_date);

CREATE TABLE IF NOT EXISTS seat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_number INT NOT NULL,
    concert_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    status ENUM('AVAILABLE', 'RESERVED', 'PAID') NOT NULL,
    price BIGINT
);
ALTER TABLE seat ADD CONSTRAINT uk_concert_seat UNIQUE(concert_id, schedule_date, seat_number);

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    seat_number INT NOT NULL,
    concert_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL,
    expired_at DATETIME NOT NULL,
    price BIGINT NOT NULL,
    paid_at DATETIME NULL
);
