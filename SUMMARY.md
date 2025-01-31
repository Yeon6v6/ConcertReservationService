# Table of contents

* [콘서트 예약 시스템 🎫](README.md)

## 요구사항 분석자료 <a href="#diagram" id="diagram"></a>

* [플로우 차트](diagram/flow.md)
* [시퀀스 다이어그램](diagram/sequence-diagram.md)

## 설계자료 <a href="#design" id="design"></a>

* [ERD 설계](design/design.md)
* [API 명세](https://cheese-2.gitbook.io/hh_crs_api)

## Swagger

* [메인화면](swagger/api.md)
* [Token](swagger/token.md)
* [Concert](swagger/concert.md)
* [Reservation](swagger/reservation.md)
* [UserBalance](swagger/userbalance.md)

## 동시성 처리 <a href="#concurrency" id="concurrency"></a>

* [동시성 이슈 및 제어 방식 분석](concurrency/handling.md)

## 쿼리 성능 개선 <a href="#queryboost" id="queryboost"></a>

* [Redis를 사용한 캐싱 전략](queryboost/rediscache.md)
* [캐시 스탬피드(Cache Stampede) 현상](queryboost/cachestampede.md)
* [쿼리 성능 현황](queryboost/curquerystatus.md)
* [Redis 및 캐싱을 적용한 성능 개선](queryboost/rediscacheboost.md)
* [응답 속도 및 쿼리 실행 비교](queryboost/responsetest.md)
