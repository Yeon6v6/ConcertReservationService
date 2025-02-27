# Redis 및 캐싱을 적용한 성능 개선

## **1. 문제 정의**

### **1.1 성능 저하의 원인**

* **반복 조회** : 동일한 쿼리가 다수의 클라이언트로부터 반복적으로 호출
* **대량 데이터 처리** : 쿼리에서 대량의 데이터 필터링
* **DB 직접 접근** : 데이터베이스에 대한 직접 접근으로 인한 부하

### **1.2 대상 쿼리(**[**쿼리 성능 현황**](curquerystatus.md)**)**

1. `findByConcertIdAndIsSoldOut`: 특정 콘서트의 매진 여부 확인
2. `findSchedule`: 특정 콘서트 일정 정보를 조회
3. `findAvailableSeatList`: 특정 콘서트 일정의 예약 가능한 좌석 **목록**을 조회
4. `countAvailableSeats`: 특정 콘서트 일정의 예약 가능한 좌석 **개수**를 조회

***

## **2. 해결 방안 : 캐싱 및 Redis 활용**

### **2.1 캐싱 전략**

* **콘서트(좌석)  정보조회**
  * 읽기 많은 데이터 : 콘서트 정보
    * **`Read Through`** : 스케줄은 변경이 적고, 읽기 요청이 많으므로 적합
* **좌석 예약**
  * 읽기 많은 데이터 : 예약 가능한 좌석 상태
    * `C`**`ache Aside`**  : 좌석 상태는 자주 변경되므로, 데이터베이스에서 최신 상태를 가져오고 캐시를 갱신
  * 좌석 상태 업데이트 : 좌석 상태 변경
    * **`Write Through`** : 좌석 예약 시 캐시와 데이터베이스를 동시에 업데이트하여 일관성 보장

**(1) Read Through**

* 데이터 요청 시 캐시에 먼저 접근하며, 캐시에 데이터가 없으면 데이터베이스에서 조회 후 캐시에 저장
*   예시 코드

    ```java
    @Cacheable(value = "concertScheduleCache", key = "#concertId + '-' + #isSoldOut")
    public List<ConcertSchedule> findByConcertIdAndIsSoldOut(Long concertId, boolean isSoldOut) {
        return concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, isSoldOut);
    }
    ```

***

**(2) Cache Aside**

* 어플리케이션에서 직접 캐시를 관리하며, 캐시에 데이터가 없으면 DB에서 조회 후 캐시에 저장
*   예시 코드

    ```java
    public long countAvailableSeats(Long concertId, LocalDate scheduleDate) {
        String key = "availableSeats:" + concertId + ":" + scheduleDate;

        // 캐시 조회
        Long cachedCount = redisTemplate.opsForValue().get(key);
        if (cachedCount != null) {
            return cachedCount;
        }

        // 캐시에 데이터가 없으면 DB에서 조회 후 저장
        long count = seatRepository.countAvailableSeats(concertId, scheduleDate);
        redisTemplate.opsForValue().set(key, count, 1, TimeUnit.HOURS);

        return count;
    }
    ```

***

**(3) Write Through**

* 데이터를 데이터베이스에 쓰는 동시에 캐시에 저장
*   예시 코드

    ```java
    public void updateSeatStatus(Long seatId, String status) {
        seatRepository.updateSeatStatus(seatId, status);
        redisTemplate.opsForValue().set("seat:" + seatId, status);
    }
    ```

***



### **2.2 Redis를 활용한 캐시 최적화**

**(1) 캐시 TTL(Time-To-Live) 설정**

* 데이터 유효 기간을 설정하여 캐시된 데이터가 오래 사용되지 않도록 함
* 예: `countAvailableSeats` 결과를 1시간 동안 캐싱

```java
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
```

**(2) 캐시 갱신**

* 데이터 변경 시 캐시를 갱신하여 데이터 일관성 유지.

```java
jpublic void updateSeat(Long seatId, String status) {
    seatRepository.updateStatus(seatId, status);
    redisTemplate.delete("seat:" + seatId); // 캐시 삭제
}
```

