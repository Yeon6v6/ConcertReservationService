# Redis를 사용한 캐싱 전략

### 캐시(Cache)란?

원본 저장소보다 빠르게 가져올 수 있는 임시 데이터 저장소

### 캐싱(Caching)이란?

캐시(Cache, 임시 데이터 저장소)에 접근해서 데이터를 빠르게 가져오는 방식

<figure><img src="../.gitbook/assets/image (23).png" alt=""><figcaption></figcaption></figure>

***

## 데이터를 캐싱할 때 사용하는 전략

### 1. Write-Through

* **설명** : 데이터를 데이터베이스에 저장하는 동시에 캐시에도 저장
* **활용 사례** : 읽기와 쓰기 작업이 균등하거나, 데이터 동기화가 중요한 경우
*   **예시 코드**

    ```java
    public void saveConcertSchedule(ConcertSchedule schedule) {
        concertScheduleRepository.save(schedule);
        redisTemplate.opsForValue().set("concertSchedule:" + schedule.getId(), schedule);
    }
    ```
* **특징**
  * 데이터베이스와 캐시가 항상 동기화됨
  * 쓰기 작업마다 캐시 업데이트

### 2. Write-Behind(Back)

* **설명** : 데이터를 캐시에 먼저 쓰고, 이후 비동기적으로 데이터베이스에 저장
* **활용 사례** : 데이터 일관성 요구가 낮고, 쓰기 연산 빈도가 매우 높은 경우
*   **예시 코드**

    ```java
    public void writeBackSchedule(ConcertSchedule schedule) {
        redisTemplate.opsForValue().set("concertSchedule:" + schedule.getId(), schedule);
        asyncDatabaseSave(schedule);
    }
    ```
* **특징**
  * 데이터베이스 부하를 줄일 수 있음
  * 데이터 손실 가능성을 고려해야 함

### 3. Read-Through

* **설명** : 데이터 요청 시 캐시에 먼저 접근하며, 캐시에 데이터가 없으면 데이터베이스에서 조회 후 캐시에 저장
* **활용 사례** : 읽기 비율이 매우 높고 데이터 변경이 빈번하지 않은 경우.
*   **예시 코드**

    ```java
    @Cacheable(value = "concertScheduleCache", key = "#concertId + '-' + #isSoldOut")
    public List<ConcertSchedule> findByConcertIdAndIsSoldOut(Long concertId, boolean isSoldOut) {
        return concertScheduleRepository.findByConcertIdAndIsSoldOut(concertId, isSoldOut);
    }
    ```
* **특징**
  * 데이터 로드와 캐싱이 통합되어 관리.
  * 캐시 미스가 발생하면 DB에서 가져와 자동으로 캐시에 저장.

### 4. ⭐Cache-Aside (Look Aside / Lazy Loading) <a href="#cache-aside" id="cache-aside"></a>

* **설명 :** 어플리케이션에서 직접 캐시를 관리하며, 캐시에 데이터가 없으면 DB에서 조회 후 캐시에 저장
* **활용 사례** : 데이터 변경이 빈번하지만, 변경된 데이터만 캐시를 무효화해야 하는 경우
*   **예시 코드**

    ```java
    public List<Seat> findAvailableSeats(Long concertId, LocalDate scheduleDate) {
        String key = "availableSeats:" + concertId + ":" + scheduleDate;
        List<Seat> seats = (List<Seat>) redisTemplate.opsForValue().get(key);
        if (seats == null) {
            seats = seatRepository.findAvailableSeatList(concertId, scheduleDate);
            redisTemplate.opsForValue().set(key, seats, 1, TimeUnit.HOURS);
        }
        return seats;
    }
    ```
* **특징**
  * 읽기 요청이 많고, 데이터 변경 시 캐시를 무효화하는 로직이 필요
  *   전달방식

      * 캐시에 데이터가 있을 경우 (= Cache Hint)

      <figure><img src="../.gitbook/assets/image (28).png" alt=""><figcaption><p>Cache Hint</p></figcaption></figure>

      * 캐시에 데이터가 없을 경우 (= Cache Miss)

      <figure><img src="../.gitbook/assets/image (27).png" alt=""><figcaption><p>Cache Miss</p></figcaption></figure>

### 5. (번외) Write Around

* **설명**&#x20;
  * 데이터를 캐시에 직접 쓰지 않고, 데이터베이스에만 저장하는 방식
  * 캐시에는 데이터가 존재하지 않으며, 해당 데이터가 나중에 조회될 때 처음으로 캐시에 적재됨
  * 데이터 **조회 시점에 캐싱**이 이루어지며, 데이터 쓰기 작업은 캐시와 무관하게 데이터베이스에만 수행
* **활용 사례** : 읽기 빈도가 낮거나, 일관성이 중요한 데이터 또는 쓰기 연산이 빈번한 데이터 (자주 쓰이지만 읽기 빈도는 낮은 경우)
* 특징
  *   전달방식

      <figure><img src="../.gitbook/assets/image (26).png" alt=""><figcaption></figcaption></figure>

