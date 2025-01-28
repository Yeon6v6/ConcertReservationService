---
icon: database
---

# ERD 설계

<figure><img src="../../.gitbook/assets/ERD%20(5).png" alt=""><figcaption><p>ERD</p></figcaption></figure>

{% stepper %}
{% step %}
### user

사용자의 기본 정보 관리

* `id` (bigint, PK)
* `balance` (decimal(10,2)) : 사용자의 현재 잔액
* `reg_date` (timestamp)
* `chg_date` (timestamp)
{% endstep %}

{% step %}
### token

사용자의 대기열 상태를 관리하는 테이블

* `token_id` (bigint, PK)
* `user_id` (bigint) : 대기열에 속한 사용자 ID.
* `status` (varchar) : 대기열 상태 (ex :  'WAITING', 'IN\_PROGRESS', 'COMPLETED' 등)
* `reg_date` (timestamp)
* `chg_date` (timestamp)
{% endstep %}

{% step %}
### reservation

사용자의 예약 정보 관리

* `id` (bigint, PK)
* `user_id` (bigint) : 예약을 한 사용자 ID
* `seat_id` (bigint) : 예약된 좌석 ID
* `seat_number` (int) : 좌석 번호
* `concert_id` (bigint) : 콘서트 ID
* `schedule_date` (date) : 예약 날짜
* `price` (decimal(10,2)) : 예약된 좌석의 가격.
* `status` (varchar) : 예약 상태 (예: 예약 완료, 취소 등)
* `expired_at` (timestamp) : 예약 만료 시간(등록시간으로부터 5분)
{% endstep %}

{% step %}
### seat

좌석 정보와 관련된 데이터를 저장

* `id` (bigint, PK)
* `seat_number` (int) : 좌석 번호
* `concert_id` (bigint) : 해당 좌석이 속한 콘서트 ID
* `schedule_id` (bigint) : 해당 좌석이 예약된 스케줄 ID
* `schedule_date` (date) : 해당 좌석이 속한  콘서트 날짜
* `status` (varchar) : 좌석 상태 (ex : 'RESERVED', 'AVAILABLE' 등)
{% endstep %}

{% step %}
### concert\_schedule

콘서트 일정 정보 저장

* `id` (bigint, PK)
* `concert_id` (bigint, FK) : 해당 일정이 속한 콘서트의 고유 ID (`concert` 테이블과 연결)
* `schedule_date` (date) : 콘서트 일정
* `is_soldout` (TINYINT(1)) : 해당 날짜에 예약 가능한 좌석이 있는지 여부\
  &#x20;                                                                 (0 - 없음, 1 - 있음)
{% endstep %}
{% endstepper %}
