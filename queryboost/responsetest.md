# 응답 속도 및 쿼리 실행 비교

## 0. **테스트 환경**

* **서버** : Spring Boot + JPA + MySQL + Redis
* **캐싱** : Spring Cache (`@Cacheable`), Redis (`RedisTemplate`)
* **테스트 도구**
  * Postman (API 응답 시간 측정)
  * `redis-cli` (Redis 캐싱 데이터 확인)
* **테스트 내용**
  * ✔️API 응답 시간이 캐싱 적용 전후로 얼마나 차이 나는지 비교\
    ✔️ MySQL에서 실제로 실행되는 쿼리 수가 줄어드는지 확인\
    ✔️ Redis에 데이터가 정상적으로 저장되는지 확인  (`redis-cli` 활용)
* **테스트 한 API**
  * 예약 가능한 날짜 조회 API
    * `GET /concert/{concertId}/available-dates`

***

## 1. **캐시 적용 전**

### **요약**

✅ **응답 속도**

* 첫 번째 요청: **290ms \~ 350ms**
* 두 번째 요청 이후: **10ms \~ 20ms** (Postman 자체 캐시 적용)

✅ **쿼리 발생 횟수 변화**

* 같은 요청을 반복해도 매번 DB에 접근(요청마다 **쿼리 발생**)
* `EXPLAIN`으로 조회해 보니 **FULL SCAN 발생**하는 경우 있음

### **요청 및 응답 속도 조회**

Postman으로 요청 할 경우 두번째 요청부터는 속도가 줄어듦&#x20;

_⇒ Postman 자체 캐시_ _때문에 속도가 줄어들게 된다.  Postman의  캐시 삭제해도 동일하지만, 웹페이지에서 캐시 및 쿠키를 삭제해가며 호출한 경우는 조회 속도가 빨라지진 않았다._

<figure><img src="../.gitbook/assets/image (4) (1).png" alt=""><figcaption><p>최초 요청(조회)</p></figcaption></figure>

<figure><img src="../.gitbook/assets/image (9).png" alt=""><figcaption><p>이후 4회 연속 조회 시</p></figcaption></figure>

Postman호출하여 속도가 줄어들더라도,  동일한 요청을 반복하지만 아래의 로그와 같이 매번 DB에 접근하여 조회하기 때문에 요청마다 쿼리가 발생

### **요청 및 응답 / 쿼리 실행 로그**

<figure><img src="../.gitbook/assets/image (6) (1).png" alt=""><figcaption><p>LOG 기록</p></figcaption></figure>

```log
[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738218300976
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
[LoggingFilter] 요청 처리 시간 : 208 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738218336322
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
[LoggingFilter] 요청 처리 시간 : 10 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738218342731
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
[LoggingFilter] 요청 처리 시간 : 10 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738218350289
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
[LoggingFilter] 요청 처리 시간 : 8 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738218359720
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
[LoggingFilter] 요청 처리 시간 : 9 ms
```

EXPLAIN으로 쿼리 실행계획을 확인해보니 FULL SCAN 발생하는 경우 존재

<figure><img src="../.gitbook/assets/image (11).png" alt=""><figcaption></figcaption></figure>

***

## 2. **캐시 적용 후**

### **요약**

✅ **응답 속도**

* 첫 번째 요청: **300ms \~ 550ms** (이때는 DB 접근)
* 두 번째 요청 이후: **5ms \~ 10ms** (캐시에서 바로 응답)

✅ **쿼리 발생 횟수 변화**

* 첫 번째 요청에서는 DB에 쿼리가 발생했지만, 이후 요청에서는 **쿼리가 발생하지 않음**

### **요청 및 응답 속도 조회**

<figure><img src="../.gitbook/assets/image (7) (1).png" alt=""><figcaption><p>최초 요청(조회)</p></figcaption></figure>

<figure><img src="../.gitbook/assets/image (8).png" alt=""><figcaption><p>이후 4회 연속 조회 시</p></figcaption></figure>

### **요청 및 응답 / 쿼리 실행 로그**

<figure><img src="../.gitbook/assets/image (16).png" alt=""><figcaption><p>최초 요청 시 LOG</p></figcaption></figure>

```
[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738220274310
CacheInterceptor - Computed cache key 'concerts:31' for operation Builder[public java.util.List kr.hhplus.be.server.api.concert.application.service.ConcertService.getAvailableDateList(java.lang.Long)] caches=[availableDates] | key=''concerts:' + #concertId' | keyGenerator='' | cacheManager='cacheManager' | cacheResolver='' | condition='' | unless='' | sync='false'
CacheInterceptor - No cache entry for key 'concerts:31' in cache(s) [availableDates]
Hibernate: select cs1_0.id,cs1_0.concert_id,cs1_0.is_sold_out,cs1_0.schedule_date from concert_schedule cs1_0 where cs1_0.concert_id=? and cs1_0.is_sold_out=?
CacheInterceptor - Creating cache entry for key 'concerts:31' in cache(s) [availableDates]
[LoggingFilter] 요청 처리 시간 : 251 ms
```

최초로 요청 할 경우 `Cahce Miss`인 상태이기 때문에 요청이 들어 온 경우 DB에서 조회 진행하여 쿼리 수행 로그가 기록됨

1. `No cache` :  캐시 저장소를 조회했으나 데이터가 없음
2. 따라서 기존  DB 조회 로직 실행
3. `Creating cache` : DB로부터 조회해온 데이터를 캐시에 저장

<figure><img src="../.gitbook/assets/image (35).png" alt=""><figcaption><p>최초 요청 이후 LOG</p></figcaption></figure>

```
[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738220604827
CacheInterceptor - Computed cache key 'concerts:31' for operation Builder[public java.util.List kr.hhplus.be.server.api.concert.application.service.ConcertService.getAvailableDateList(java.lang.Long)] caches=[availableDates] | key=''concerts:' + #concertId' | keyGenerator='' | cacheManager='cacheManager' | cacheResolver='' | condition='' | unless='' | sync='false'
CacheInterceptor - Cache entry for key 'concerts:31' found in cache(s) [availableDates]
[LoggingFilter] 요청 처리 시간 : 3 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738220605546
CacheInterceptor - Computed cache key 'concerts:31' for operation Builder[public java.util.List kr.hhplus.be.server.api.concert.application.service.ConcertService.getAvailableDateList(java.lang.Long)] caches=[availableDates] | key=''concerts:' + #concertId' | keyGenerator='' | cacheManager='cacheManager' | cacheResolver='' | condition='' | unless='' | sync='false'
CacheInterceptor - Cache entry for key 'concerts:31' found in cache(s) [availableDates]
[LoggingFilter] 요청 처리 시간 : 4 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738220607645
CacheInterceptor - Computed cache key 'concerts:31' for operation Builder[public java.util.List kr.hhplus.be.server.api.concert.application.service.ConcertService.getAvailableDateList(java.lang.Long)] caches=[availableDates] | key=''concerts:' + #concertId' | keyGenerator='' | cacheManager='cacheManager' | cacheResolver='' | condition='' | unless='' | sync='false'
CacheInterceptor - Cache entry for key 'concerts:31' found in cache(s) [availableDates]
[LoggingFilter] 요청 처리 시간 : 4 ms

[LoggingFilter] 요청 IP : 0:0:0:0:0:0:0:1
[LoggingFilter] 요청 시간 : 1738220609922
CacheInterceptor - Computed cache key 'concerts:31' for operation Builder[public java.util.List kr.hhplus.be.server.api.concert.application.service.ConcertService.getAvailableDateList(java.lang.Long)] caches=[availableDates] | key=''concerts:' + #concertId' | keyGenerator='' | cacheManager='cacheManager' | cacheResolver='' | condition='' | unless='' | sync='false'
CacheInterceptor - Cache entry for key 'concerts:31' found in cache(s) [availableDates]
[LoggingFilter] 요청 처리 시간 : 4 ms
```

`Cache entry` :  캐시 저장소를 조회하여 데이터 갖고오기

Cache에서 데이터를 갖고왔기 때문에, <mark style="color:red;">쿼리 수행 로그가 따로 기록되지 않는다</mark>.

***

## **3 Redis 캐싱 확인**

✅ **Redis에 데이터 저장 확인 (`redis-cli`)**

```bash
keys *
```

<figure><img src="../.gitbook/assets/image (13).png" alt=""><figcaption></figcaption></figure>

✅ **KEY에 저장된 데이터 확인**

```
get availableDates::concerts:31
```

<figure><img src="../.gitbook/assets/image (36).png" alt=""><figcaption></figcaption></figure>

✅ **Redis Cache TTL 확인**

```bash
ttl availableDates::concerts:31
```

<figure><img src="../.gitbook/assets/image (38).png" alt=""><figcaption></figcaption></figure>

TTL 값 정상 확인 됨
