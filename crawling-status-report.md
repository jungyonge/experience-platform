# 크롤링 수집 현황 보고서

**점검 일시**: 2026-03-21 09:25 ~ 10:00 (KST)
**설정**: `max-pages-per-site: 10`, `connection-timeout-ms: 10000`, `read-timeout-ms: 30000`
**프로필**: local (H2 인메모리 DB)

## 요약

| 항목 | 수치 |
|------|------|
| 전체 등록 소스 | 46개 |
| 크롤링 완료 소스 | 15개 |
| 크롤링 실패 소스 | 2개 (MBLE, SEOULOUBA) |
| 처리 중 행(hang) | 1개 (TOJOBCN에서 멈춤) |
| 미처리 소스 | 28개 (TOJOBCN 행으로 인해 후속 소스 미실행) |
| 총 수집 캠페인 | 2,035건 (시드 60건 포함) |
| DB 저장 실패 (Value too long) | 94건 (GANGNAM ADDRESS 컬럼 초과) |
| 총 소요 시간 | 약 35분 (15개 소스 처리, TOJOBCN에서 중단) |

## 사이트별 크롤링 결과

### 크롤링 완료된 소스 (15개)

| # | 소스코드 | 이름 | 크롤링 건수 | DB 저장 건수 | 소요시간 | 비고 |
|---|----------|------|------------|-------------|---------|------|
| 1 | REVU | 레뷰 | - | 20 | - | 시드 데이터, 별도 완료 로그 없음 |
| 2 | GANGNAM | 강남맛집 | - | 206 | ~3분 | 94건 ADDRESS 컬럼 길이 초과로 skip |
| 3 | REVE | 레뷰(REVE) | 350 | 350 | ~3분30초 | JSON API 기반, 정상 |
| 4 | REVIEWNOTE | 리뷰노트 | 0 | 0 | ~7초 | Next.js SSR - 파싱 실패 추정 |
| 5 | REVIEWPLACE | 리뷰플레이스 | 20 | 20 | ~12초 | 정상 |
| 6 | DINNERQUEEN | 디너의여왕 | 300 | 30 | ~6분20초 | POST API 기반, 대량 수집 |
| 7 | RINGBLE | 링블 | 108 | 102 | ~2분30초 | 정상 |
| 8 | WEU | 위유체험단 | 0 | 0 | ~1초 | 수집 대상 캠페인 없음 |
| 9 | ALLJAM | 잠자리체험단 | 211 | 211 | ~3분35초 | 여행 특화, 정상 |
| 10 | CHERIVU | 체리뷰 | 180 | 180 | ~1분50초 | 정상 |
| 11 | CHERRYCOOK | 체리쿡 | 24 | 24 | ~14초 | 정상 |
| 12 | CHEHUMDAN | 체험단닷컴 | 88 | 48 | ~1분24초 | 일부 중복/저장 실패 |
| 13 | CHVU | 체험뷰 | 14 | 14 | ~8초 | 정상 |
| 14 | CLOUDREVIEW | 클라우드리뷰 | 780 | 780 | ~7분17초 | 최다 수집, 정상 |
| 15 | TQUEENS | 택배의여왕 | 300 | 30 | ~6분20초 | 정상 |

### 크롤링 실패 소스 (2개)

| 소스코드 | 이름 | 에러 내용 |
|----------|------|-----------|
| MBLE | 미블 | 페이지 조회 실패 (Connect timed out) - 사이트 접속불가 |
| SEOULOUBA | 서울오빠 | 첫 페이지 크롤링 실패 (페이지 조회 실패) - 크롤링 URL 문제 |

### 처리 중 행(hang) 발생 (1개)

| 소스코드 | 이름 | 상태 |
|----------|------|------|
| TOJOBCN | 투잡커넥트 | 크롤링 시작 후 응답 없이 무한 대기 (행 발생) |

> TOJOBCN 크롤러에서 행이 발생하여 이후 28개 소스가 미처리됨. 사이트 자체는 HTTP 200 응답하나, 크롤링 과정에서 특정 요청이 타임아웃 없이 블로킹된 것으로 추정.

### 미처리 소스 (28개)

TOJOBCN 행으로 인해 아래 소스들은 크롤링이 실행되지 않음:

| # | 소스코드 | 이름 |
|---|----------|------|
| 1 | TBLE | 티블 |
| 2 | PAVLOVU | 파블로체험단 |
| 3 | FINEADPLE | 파인앳플 |
| 4 | FOURBLOG | 포블로그 |
| 5 | POPOMON | 포포몬 |
| 6 | PLAYVIEW | 플레이뷰 |
| 7 | WHOGIUP | 후기업 |
| 8 | HUKI | 후키 |
| 9 | ASSAVIEW | 아싸뷰 |
| 10 | BEAUTY_QUEEN | 뷰티의여왕 |
| 11 | BLOGDEX | 블덱스 |
| 12 | BLOGLAB | 블로그랩 |
| 13 | COMETOPLAY | 놀러와체험단 |
| 14 | DAILYVIEW | 데일리뷰 |
| 15 | DDOK | 똑똑체험단 |
| 16 | GABOJA | 가보자체험단 |
| 17 | GUGUDAS | 구구다스 |
| 18 | LINKTUBE | 링크튜브 |
| 19 | MIBLE | 미블(mrblog) |
| 20 | ODIYA | 어디야 |
| 21 | OHMYBLOG | 오마이블로그 |
| 22 | OPENREVIEW | 오픈리뷰 |
| 23 | REAL_REVIEW | 리얼리뷰 |
| 24 | REVIEW_EXPEDITION | 리뷰원정대 |
| 25 | STORYNMEDIA | 스토리앤미디어 |
| 26 | STYLEC | 스타일씨 |
| 27 | WEREVIEW | 위리뷰 |
| 28 | YOGITG | 요깃지 |

## 사이트 URL 접속 검증 결과

### 정상 접속 (42개)

| 소스코드 | 이름 | URL | HTTP 상태 |
|----------|------|-----|-----------|
| REVU | 레뷰 | https://www.revu.net | 200 |
| GANGNAM | 강남맛집 | https://xn--939au0g4vj8sq.net | 200 |
| REVE | 레뷰(REVE) | https://api.weble.net | 200 |
| REVIEWNOTE | 리뷰노트 | https://reviewnote.co.kr | 200 |
| SEOULOUBA | 서울오빠 | https://seoulouba.co.kr | 200 |
| REVIEWPLACE | 리뷰플레이스 | https://reviewplace.co.kr | 200 |
| DINNERQUEEN | 디너의여왕 | https://dinnerqueen.net | 200 |
| RINGBLE | 링블 | https://ringble.co.kr | 200 |
| WEU | 위유체험단 | https://weu.kr | 200 |
| ALLJAM | 잠자리체험단 | https://www.alljam.co.kr | 200 |
| CHERIVU | 체리뷰 | https://cherivu.co.kr | 200 |
| CHERRYCOOK | 체리쿡 | https://cherry-cook.com | 200 |
| CHEHUMDAN | 체험단닷컴 | https://chehumdan.com | 200 |
| CHVU | 체험뷰 | https://chvu.co.kr | 200 |
| CLOUDREVIEW | 클라우드리뷰 | https://www.cloudreview.co.kr | 200 |
| TQUEENS | 택배의여왕 | https://tqueens.net | 200 |
| TOJOBCN | 투잡커넥트 | https://www.tojobcn.com | 200 |
| PAVLOVU | 파블로체험단 | https://pavlovu.com | 200 |
| FINEADPLE | 파인앳플 | https://www.fineadple.com | 200 |
| FOURBLOG | 포블로그 | https://4blog.net | 200 |
| POPOMON | 포포몬 | https://popomon.com | 200 |
| PLAYVIEW | 플레이뷰 | https://playview.co.kr | 200 |
| WHOGIUP | 후기업 | https://www.whogiup.com | 200 |
| HUKI | 후키 | https://www.huki.co.kr | 200 |
| ASSAVIEW | 아싸뷰 | https://assaview.co.kr | 200 |
| BEAUTY_QUEEN | 뷰티의여왕 | https://bqueens.net | 200 |
| BLOGDEX | 블덱스 | https://blogdexreview.space | 200 |
| BLOGLAB | 블로그랩 | https://bloglab.kr | 200 |
| COMETOPLAY | 놀러와체험단 | https://cometoplay.kr | 200 |
| DAILYVIEW | 데일리뷰 | https://dailyview.kr | 200 |
| DDOK | 똑똑체험단 | https://ddok.co.kr | 200 |
| GABOJA | 가보자체험단 | https://xn--o39a04kpnjo4k9hgflp.com | 200 |
| GUGUDAS | 구구다스 | https://99das.com | 200 |
| LINKTUBE | 링크튜브 | https://linktube.me | 200 |
| MIBLE | 미블(mrblog) | https://mrblog.net | 200 |
| OHMYBLOG | 오마이블로그 | https://ohmyblog.co.kr | 200 |
| OPENREVIEW | 오픈리뷰 | https://openreview.kr | 200 |
| REAL_REVIEW | 리얼리뷰 | https://real-review.kr | 200 |
| STORYNMEDIA | 스토리앤미디어 | https://storyn.co.kr | 200 |
| STYLEC | 스타일씨 | https://www.stylec.co.kr | 200 |
| WEREVIEW | 위리뷰 | https://www.wereview.fun | 200 |
| YOGITG | 요깃지 | https://yogitg.co.kr | 200 |

### 접속 문제 사이트 (4개)

| 소스코드 | 이름 | URL | HTTP 상태 | 문제 |
|----------|------|-----|-----------|------|
| MBLE | 미블 | https://www.mble.xyz | 000 (접속불가) | DNS 해석 실패 또는 서버 다운. 크롤링도 실패함 |
| ODIYA | 어디야 | https://odiya.kr | 000 (접속불가) | DNS 해석 실패 또는 서버 다운. 크롤링 미처리 |
| REVIEW_EXPEDITION | 리뷰원정대 | https://reviewexpedition.co.kr | 000 (접속불가) | 사이트 폐쇄 추정. 크롤링 미처리 |
| TBLE | 티블 | https://tble.kr | 403 (Forbidden) | 봇 차단 또는 접근 제한. 크롤링 미처리 |

## 주요 이슈 및 개선 권고

### 1. TOJOBCN 크롤러 행(hang) 문제 (심각)
- **현상**: TOJOBCN 크롤링 시작 후 응답 없이 무한 대기, 이후 28개 소스 미처리
- **원인 추정**: Jsoup 연결이 내부적으로 블로킹, `read-timeout-ms` 설정이 특정 케이스에서 동작하지 않음
- **권고**: 크롤러별 실행 타임아웃 (ExecutorService + Future.get(timeout)) 추가 필요

### 2. ADDRESS 컬럼 길이 초과 (중간)
- **현상**: GANGNAM 크롤링 시 94건의 `Value too long for column "ADDRESS VARCHAR(300)"` 에러
- **원인**: 강남맛집 사이트의 주소 데이터에 크롤링 지침 텍스트가 포함되어 300자 초과
- **권고**: ADDRESS 컬럼을 `@Column(length = 1000)` 또는 `@Lob`으로 변경, 또는 크롤러에서 길이 제한 처리

### 3. 접속불가 사이트 비활성화 권고
- MBLE (`www.mble.xyz`): 사이트 다운 - `active = false` 설정 권고
- ODIYA (`odiya.kr`): 사이트 다운 - `active = false` 설정 권고
- REVIEW_EXPEDITION (`reviewexpedition.co.kr`): 사이트 폐쇄 추정 - `active = false` 설정 권고
- TBLE (`tble.kr`): 403 Forbidden - 봇 차단 가능성, User-Agent 변경 또는 `active = false` 설정 권고

### 4. 스케줄 충돌
- **현상**: 매시 정각 cron 트리거 시 초기 크롤링이 아직 진행 중이어서 "크롤링이 이미 실행 중입니다" 에러
- **권고**: `schedule-cron` 간격 확대 (현재 1시간 → 2~3시간) 또는 초기 크롤링 완료 후 스케줄 시작

### 5. SEOULOUBA 크롤링 URL 확인 필요
- **현상**: HTTP 200 응답하지만 크롤링 실패 (`/campaign/` 경로 페이지 조회 실패)
- **원인 추정**: 사이트 구조 변경으로 크롤링 대상 URL이 더 이상 유효하지 않음
- **권고**: SEOULOUBA 사이트의 캠페인 목록 URL 재확인 및 크롤러 수정

### 6. REVIEWNOTE, WEU 0건 수집
- REVIEWNOTE: Next.js SSR 기반으로 Jsoup에서 동적 콘텐츠 파싱 불가 추정
- WEU: 현재 모집 중인 캠페인이 없거나, 사이트 구조 변경
