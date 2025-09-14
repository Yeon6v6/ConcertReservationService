# 콘서트 좌석 예약 시스템 🎫

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

* **Back** : Spring Boot, Java 11+
* **DB** : MySQL
* **통신 방식**: RESTful API

***

### 🚀  설정 및 실행

#### 1.  프로젝트 설정 정보

* JDK 17
* MySQL 8.0
* Git

#### 2. Docker Containers 실행

```bash
docker-compose up -d
```

부하 테스트를 위한 k6 / influxdb / grafana 추가 시 빌드 필요

<pre class="language-bash"><code class="lang-bash"><strong>docker-compose up --build
</strong><strong># k6 실행
</strong><strong>docker-compose up k6
</strong></code></pre>

