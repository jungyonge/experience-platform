# 크롤링 수집 현황 보고서

**점검 일시**: 2026-03-22 17:44 ~ 18:10 (KST)
**설정**: `max-pages-per-site: 10`, `connection-timeout-ms: 10000`, `read-timeout-ms: 30000`, `parallel-threads: 5`
**프로필**: local (H2 인메모리 DB)

## 요약

| 항목 | 수치 |
|------|------|
| 전체 등록 소스 | 44개 |
| 크롤링 완료 소스 | 44개 (전체 성공) |
| 크롤링 실패 소스 | 0개 |
| 데이터 수집 소스 | 35개 (1건 이상 수집) |
| 0건 수집 소스 | 9개 |
| 총 수집 캠페인 | 6,547건 (크롤링 건수 합계) |
| DB 저장 캠페인 | 4,700건 (신규 저장 기준) |
| DB 저장 실패 | 0건 |
| 총 소요 시간 | 약 26분 |

### 이전 보고서(3/21) 대비 변경사항

| 항목 | 3/21 | 3/22 | 비고 |
|------|------|------|------|
| 등록 소스 | 46개 | 44개 | TOJOBCN 삭제, MBLE→MIBLE 통합 |
| 크롤링 완료 | 15개 | 44개 | 전체 소스 크롤링 성공 |
| 크롤링 실패 | 2개 | 0개 | MBLE 접속불가 해결 (URL 변경) |
| 행(hang) 발생 | 1개 (TOJOBCN) | 0개 | TOJOBCN 크롤러 삭제로 해결 |
| 미처리 소스 | 28개 | 0개 | 전체 처리 완료 |
| 총 수집 캠페인 | 2,035건 | 4,700건 | +131% 증가 |
| ADDRESS 저장 실패 | 94건 | 0건 | 해결됨 |

## 사이트별 크롤링 결과

### 데이터 수집 성공 소스 (35개)

| # | 소스코드 | 이름 | 크롤링 건수 | 신규 | 갱신 | 실패 | 소요시간 | 비고 |
|---|----------|------|-----------|------|------|------|---------|------|
| 1 | CLOUDREVIEW | 클라우드리뷰 | 791 | 791 | 0 | 0 | ~7분27초 | 최다 수집, 정상 |
| 2 | PAVLOVU | 파블로체험단 | 581 | 2 | 579 | 0 | ~6분34초 | 대부분 기존 데이터 갱신 |
| 3 | DAILYVIEW | 데일리뷰 | 541 | 530 | 11 | 0 | ~5분4초 | 신규 대량 수집 |
| 4 | REVE | 레뷰(REVE) | 350 | 350 | 0 | 0 | ~18초 | JSON API 기반, 빠른 수집 |
| 5 | DINNERQUEEN | 디너의여왕 | 300 | 30 | 270 | 0 | ~6분40초 | POST API 기반 |
| 6 | TQUEENS | 택배의여왕 | 300 | 30 | 270 | 0 | ~6분32초 | 정상 |
| 7 | BEAUTY_QUEEN | 뷰티의여왕 | 300 | 30 | 270 | 0 | ~6분20초 | 정상 |
| 8 | GANGNAM | 강남맛집 | 280 | 186 | 94 | 0 | ~3분2초 | ADDRESS 저장 실패 해결됨 |
| 9 | PLAYVIEW | 플레이뷰 | 261 | 261 | 0 | 0 | ~2분25초 | 정상 |
| 10 | TBLE | 티블 | 258 | 248 | 10 | 0 | ~2분19초 | 403 응답이지만 크롤링 성공 |
| 11 | REAL_REVIEW | 리얼리뷰 | 240 | 240 | 0 | 0 | ~5분10초 | 정상 |
| 12 | ALLJAM | 잠자리체험단 | 211 | 211 | 0 | 0 | ~3분32초 | 여행 특화, 정상 |
| 13 | BLOGDEX | 블덱스 | 200 | 199 | 1 | 0 | ~5분54초 | 정상 |
| 14 | OHMYBLOG | 오마이블로그 | 200 | 200 | 0 | 0 | ~2분5초 | 정상 |
| 15 | OPENREVIEW | 오픈리뷰 | 200 | 200 | 0 | 0 | ~2분21초 | 정상 |
| 16 | STYLEC | 스타일씨 | 200 | 200 | 0 | 0 | ~2분6초 | 정상 |
| 17 | CHERIVU | 체리뷰 | 180 | 180 | 0 | 0 | ~1분49초 | 정상 |
| 18 | GUGUDAS | 구구다스 | 128 | 127 | 1 | 0 | ~1분40초 | 정상 |
| 19 | POPOMON | 포포몬 | 120 | 21 | 99 | 0 | ~1분35초 | 대부분 기존 데이터 갱신 |
| 20 | RINGBLE | 링블 | 108 | 103 | 5 | 0 | ~2분26초 | 정상 |
| 21 | FINEADPLE | 파인앳플 | 100 | 100 | 0 | 0 | ~1분21초 | 정상 |
| 22 | CHEHUMDAN | 체험단닷컴 | 88 | 48 | 40 | 0 | ~1분28초 | 정상 |
| 23 | HUKI | 후키 | 63 | 63 | 0 | 0 | ~48초 | 정상 |
| 24 | GABOJA | 가보자체험단 | 60 | 45 | 15 | 0 | ~59초 | 정상 |
| 25 | LINKTUBE | 링크튜브 | 50 | 50 | 0 | 0 | ~1분7초 | 정상 |
| 26 | BLOGLAB | 블로그랩 | 36 | 36 | 0 | 0 | ~55초 | 정상 |
| 27 | MIBLE | 미블 | 30 | 29 | 1 | 0 | ~18초 | URL mrblog.net으로 변경 후 정상 |
| 28 | YOGITG | 요깃지 | 26 | 26 | 0 | 0 | ~15초 | 정상 |
| 29 | CHERRYCOOK | 체리쿡 | 24 | 24 | 0 | 0 | ~14초 | 정상 |
| 30 | DDOK | 똑똑체험단 | 24 | 24 | 0 | 0 | ~27초 | 정상 |
| 31 | COMETOPLAY | 놀러와체험단 | 20 | 20 | 0 | 0 | ~11초 | 정상 |
| 32 | REVIEWPLACE | 리뷰플레이스 | 20 | 20 | 0 | 0 | ~12초 | 정상 |
| 33 | ASSAVIEW | 아싸뷰 | 20 | 20 | 0 | 0 | ~13초 | 정상 |
| 34 | CHVU | 체험뷰 | 14 | 14 | 0 | 0 | ~7초 | 정상 |
| 35 | STORYNMEDIA | 스토리앤미디어 | 2 | 2 | 0 | 0 | ~3초 | 수집 대상 적음 |

### 0건 수집 소스 (9개)

| # | 소스코드 | 이름 | 소요시간 | 사이트 접속 | 원인 분석 |
|---|----------|------|---------|-----------|----------|
| 1 | REVU | 레뷰 | 0.2초 | 200 | 시드 데이터(20건) 보유, 별도 크롤링 로직 미동작 추정 |
| 2 | REVIEWNOTE | 리뷰노트 | 6.8초 | 200 | Next.js SSR 기반 - Jsoup으로 동적 콘텐츠 파싱 불가 |
| 3 | SEOULOUBA | 서울오빠 | 0.1초 | 200 | listUrlPattern 미설정, 크롤링 URL 문제 |
| 4 | WEU | 위유체험단 | 0.1초 | 200 | 현재 모집 중인 캠페인 없거나 사이트 구조 변경 |
| 5 | FOURBLOG | 포블로그 | 1초 | 200 | 수집 대상 캠페인 없음 |
| 6 | WHOGIUP | 후기업 | 0.5초 | 200 | 수집 대상 캠페인 없거나 파싱 실패 |
| 7 | ODIYA | 어디야 | 0.1초 | 000 (접속불가) | DNS 해석 실패, 사이트 다운 |
| 8 | REVIEW_EXPEDITION | 리뷰원정대 | 0초 | 000 (접속불가) | 사이트 폐쇄 추정 |
| 9 | WEREVIEW | 위리뷰 | 0.3초 | 200 | 수집 대상 캠페인 없거나 파싱 실패 |

### 삭제된 소스 (이전 대비)

| 소스코드 | 이름 | 삭제 사유 |
|----------|------|----------|
| TOJOBCN | 투잡커넥트 | 크롤링 행(hang) 발생으로 삭제 (130e835 커밋) |
| MBLE | 미블 | MIBLE로 통합 (URL: mble.xyz → mrblog.net) |

## 사이트 URL 접속 검증 결과

### 정상 접속 (41개)

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
| MIBLE | 미블 | https://mrblog.net | 200 |
| OHMYBLOG | 오마이블로그 | https://ohmyblog.co.kr | 200 |
| OPENREVIEW | 오픈리뷰 | https://openreview.kr | 200 |
| REAL_REVIEW | 리얼리뷰 | https://real-review.kr | 200 |
| STORYNMEDIA | 스토리앤미디어 | https://storyn.co.kr | 200 |
| STYLEC | 스타일씨 | https://www.stylec.co.kr | 200 |
| WEREVIEW | 위리뷰 | https://www.wereview.fun | 200 |
| YOGITG | 요깃지 | https://yogitg.co.kr | 200 |

### 접속 문제 사이트 (3개)

| 소스코드 | 이름 | URL | HTTP 상태 | 문제 | 3/21 대비 |
|----------|------|-----|-----------|------|----------|
| TBLE | 티블 | https://tble.kr | 403 (Forbidden) | 봇 차단 가능성, 그러나 크롤러는 258건 정상 수집 | 동일 |
| ODIYA | 어디야 | https://odiya.kr | 000 (접속불가) | DNS 해석 실패, 사이트 다운 | 동일 |
| REVIEW_EXPEDITION | 리뷰원정대 | https://reviewexpedition.co.kr | 000 (접속불가) | 사이트 폐쇄 추정 | 동일 |

> **참고**: 이전 보고서에서 접속불가였던 MBLE(mble.xyz)은 MIBLE(mrblog.net)로 URL 변경 후 정상 접속 및 크롤링 성공.

## 주요 이슈 및 개선 권고

### 해결된 이슈 (이전 보고서 대비)

| 이슈 | 이전 상태 | 현재 상태 | 해결 방법 |
|------|----------|----------|----------|
| TOJOBCN 행(hang) | 심각 - 28개 소스 블로킹 | 해결 | 크롤러 삭제 (130e835 커밋) |
| ADDRESS 컬럼 초과 | 94건 저장 실패 | 해결 | GANGNAM 280건 중 0건 실패 |
| MBLE 접속불가 | 사이트 다운 | 해결 | URL을 mrblog.net으로 변경 |

### 잔존 이슈

#### 1. 접속불가 사이트 비활성화 권고 (낮음)
- **ODIYA** (`odiya.kr`): 사이트 다운 지속 - `active = false` 설정 권고
- **REVIEW_EXPEDITION** (`reviewexpedition.co.kr`): 사이트 폐쇄 추정 - `active = false` 설정 권고

#### 2. 0건 수집 소스 점검 필요 (중간)
- **SEOULOUBA**: HTTP 200이나 0건 수집 - `listUrlPattern` 미설정, 크롤링 URL 재확인 필요
- **REVIEWNOTE**: Next.js SSR 기반 - Jsoup으로 동적 콘텐츠 파싱 불가, Selenium/Playwright 도입 또는 비활성화 고려
- **REVU**: 시드 데이터 20건만 보유, 크롤링 로직 미동작 - 크롤러 점검 필요
- **WEU, FOURBLOG, WHOGIUP, WEREVIEW**: 수집 대상이 실제로 없는지 사이트 직접 확인 필요

#### 3. 크롤러별 실행 타임아웃 미적용 (중간)
- TOJOBCN 삭제로 현재는 문제없으나, CompletableFuture.join()에 타임아웃 없음
- 향후 유사 행 발생 시 동일 문제 재발 가능
- **권고**: `Future.get(timeout)` 또는 `CompletableFuture.orTimeout()` 적용

#### 4. 스케줄 충돌 가능성 (낮음)
- 전체 크롤링 소요 ~26분, 스케줄 간격 1시간 → 현재는 충돌 없음
- 소스 추가 시 소요시간 증가 가능, 모니터링 필요

#### 5. TBLE 403 응답 (낮음)
- curl로 403이지만 크롤러에서는 258건 정상 수집
- User-Agent 또는 크롤러 내부 헤더 설정으로 우회되는 것으로 추정
- 현재 동작에는 문제없으나 사이트 정책 변경 시 차단 가능성 있음
