# 콘서트 좌석 예약 시스템 🎫

***

### 🗂️ 프로젝트 소개 [(GitHub Repository)](https://github.com/Yeon6v6/ConcertReservationService)

콘서트 좌석 예약 과정에서 발생할 수 있는 **동시성 문제**와 **트래픽 집중**을 해결하기 위해 설계한 백엔드 시스템입니다.\
대기열 관리, 좌석 선점, 예약/결제 처리까지 안정적이고 일관성 있는 서비스를 제공하는 것을 목표로 했습니다.

***

### 📌 주요 기능

1. **좌석 예약 관리 모듈**
   * 사용자가 예약 가능한 날짜와 좌석 정보를 조회
   * 좌석 예약 요청 및 임시 배정을 통해 결제 전까지 자리를 확보
   * 예약 취소 및 임시 배정 해제 처리
2. **대기열 관리 모듈**
   * 유저의 대기열 상태를 확인하고 토큰을 발급
   * API 호출 시 대기열 검증을 통해 서비스 접근 제어
3. **잔액 및 결제 모듈**
   * 사용자 잔액 충전 및 조회
   * 좌석 결제 처리 및 결제 내역 생성
   * 결제 완료 시 좌석 소유권 확정 및 대기열 토큰 만료 처리

***

### 🛠 기술 스택

* **Back** : Spring Boot, Java 17+, MySQL
* **통신 방식**: RESTful API
* **Messaging**: Kafka
* **Cache & Lock**: Redis
* **Test**: JUnit, K6
* **Monitoring**: Prometheus, Grafana
* **Infra**: Docker Compose

***

### 🎯 사용 기술 및 성과

* Redis 기반 대기열 시스템을 구현하여 순간적인 트래픽에도 안정적 처리
* 좌석 예약 시 분산락을 적용해 동시성 문제 방지
* Redis 캐시 + 인덱스 최적화로 DB 부하 감소 및 조회 성능 향상
* K6 부하 테스트로 성능 검증, Prometheus + Grafana로 모니터링 환경 구축

***

### 🚀  설정 및 실행

#### Docker Containers 실행

```bash
docker-compose up -d
```

부하 테스트를 위한 k6 / influxdb / grafana 추가 시 빌드 필요

<pre class="language-bash"><code class="lang-bash"><strong>docker-compose up --build
</strong><strong># k6 실행
</strong><strong>docker-compose up k6
</strong></code></pre>

