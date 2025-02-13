# 현재 API 분석(ReservationFacade)

## **개요**

ReservationFacade는 하나의 애플리케이션 내에서 콘서트 좌석 예약과 결제 처리 프로세스를 단일 진입점에서 관리

<mark style="background-color:yellow;">**모놀리식(Monolithic) 아키텍처**</mark> 기반으로 운영되며, 모든 예약 및 결제 프로세스가 **하나의 애플리케이션 내부에서 동작**하는 구조

> 이 방식에서는 **하나의 데이터베이스**와 **단일 트랜잭션 범위** 내에서 좌석 예약, 결제, 사용자 정보(잔액) 갱신 등이 진행된다.

* **주요 기능**
  * **좌석 예약** : 좌석의 예약 가능 여부를 확인한 후 예약 정보를 생성
  * **결제 처리** : 예약 정보를 조회, 결제 진행, 좌석 상태와 예약 상태 업데이트, 그리고 관련 토큰 만료 처리
* **기술적 특징**
  * **트랜잭션 관리** : 각 메서드는 Spring의 `@Transactional`을 통해 단일 트랜잭션으로 실행되어, 문제가 발생할 경우 전체 작업이 롤백
  * **동시성 제어** : `@RedisLock` 어노테이션을 활용하여 동일 좌석에 대한 동시 접근을 막아 데이터 무결성을 보장

***

## **로직 분석**

### A. **좌석 예약 프로세스** (reserveSeat)

* **동작 흐름**
  1. **좌석 상태 확인**
     * Concert Service의 `reserveSeat`를 호출하여, 지정된 좌석의 예약 가능 여부를 확인
     * 좌석이 이미 예약되어 있으면, `CustomException`과 함께 `SeatErrorCode.SEAT_ALREADY_RESERVED` 예외를 발생시켜 예약 프로세스를 중단
  2. **예약 정보 생성**
     * 예약이 가능하면 Reservation Service의 `createReservation`을 호출하여 예약 데이터를 생성 후 반환
* **특징**
  * 전체 로직이 하나의 트랜잭션으로 처리되며, Redis 기반 락으로 동일 좌석에 대한 동시 요청을 제어함
    * `@RedisLock(prefix = "seat:", key = "#reservationCmd.seatId")`

### B. **결제 처리 프로세스** (payReservation)

* **동작 흐름**
  1. **예약 조회 및 검증**
     * **예약 정보 조회**
       * `reservationService.findById(paymentCmd.reservationId())`를 호출하여 해당 예약 정보를 조회
     * **유효성 체크**
       * 조회된 예약 객체가 없으면 `SeatErrorCode.SEAT_NOT_RESERVED` 오류를 발생
       * 예약 객체의 `validate()` 메서드를 통해 결제 진행 전 금액, 상태 등 비즈니스 조건을 확인
  2. **결제 처리**
     * 예약된 좌석의 가격(seatPrice)을 조회한 후, 아래의 메소드를호출하여 사용자의 결제를 진행
       * `userService.processPayment(paymentCmd.userId(),paymentCmd.paymentAmount())`
     * 해당 호출을 통해 사용자의 결제 처리가 완료되고, 남은 잔액(remainingBalance)이 반환
  3. **좌석 및 예약 상태 업데이트**
     * Concert Service를 호출하여 좌석 상태를 결제 완료 상태로 변경
     * Reservation Service를 통해 예약 상태를 갱신
  4. **토큰 만료 처리**
     * Token Service를 통해 해당 사용자의 토큰을 조회하고, 만료 처리함
  5. **최종 결과 반환**
     * PaymentResult 객체에 결제 관련 정보를 담아 반환
* **특징**:
  * 여러 도메인(예약, 결제, 좌석, 사용자, 토큰)이 하나의 트랜잭션 내에서 호출되며, Redis 락으로 동시 결제 요청을 제어

### **트랜잭션 분석**

* **트랜잭션 단위**: `processPayment()`는 `@Transactional`을 통해 **예약 조회 → 잔액 차감 → 결제 내역 저장 → 예약 확정**을 **하나의 트랜잭션 내에서 처리**함.
* **주요 트랜잭션 범위**
  1. **예약 정보 조회** (`ReservationRepository`)
  2. **사용자 잔액 차감** (`UserRepository`)
  3. **결제 정보 저장** (`ReservationRepository`) \
     &#xNAN;_⇒ 예약 기록이 결제 기록을 대체한다_
  4. **예약 확정 처리** (`ReservationRepository`)
* **트랜잭션 실패 시** : 중간 과정에서 오류 발생 시 **잔액 차감 및 결제 정보 저장 롤백**



