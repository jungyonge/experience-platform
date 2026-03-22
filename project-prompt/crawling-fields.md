# 체험단 사이트별 수집 가능 필드 현황

## 1. CrawledCampaign 스키마 (18개 필드)

| # | 필드명 | 타입 | 설명 |
|---|--------|------|------|
| 1 | sourceCode | String | 크롤링 소스 코드 (메타데이터) |
| 2 | originalId | String | 원본 사이트 캠페인 ID |
| 3 | title | String | 캠페인 제목 |
| 4 | description | String | 캠페인 설명/요약 |
| 5 | detailContent | String | 상세 콘텐츠 (HTML) |
| 6 | thumbnailUrl | String | 썸네일 이미지 URL |
| 7 | originalUrl | String | 원본 캠페인 URL |
| 8 | category | CampaignCategory | 카테고리 (FOOD/BEAUTY/TRAVEL/LIFE/DIGITAL/CULTURE/ETC) |
| 9 | status | CampaignStatus | 상태 (RECRUITING/CLOSED) |
| 10 | recruitCount | Integer | 모집 인원 |
| 11 | applyStartDate | LocalDate | 신청 시작일 |
| 12 | applyEndDate | LocalDate | 신청 마감일 |
| 13 | announcementDate | LocalDate | 발표일 |
| 14 | reward | String | 제공 내역 |
| 15 | mission | String | 미션 (리뷰 유형) |
| 16 | address | String | 방문 주소/지역 |
| 17 | keywords | String | 키워드 태그 |
| 18 | currentApplicants | Integer | 현재 신청자 수 |

---

## 2. 사이트별 전체 필드 수집 매트릭스 (활성 34개)

> 전체 등록 소스 44개, 활성 소스 34개
> 비활성 10개: REVU, ODIYA, REVIEW_EXPEDITION, WEREVIEW, ALLJAM, CHEHUMDAN, CLOUDREVIEW, LINKTUBE, RINGBLE, STORYNMEDIA

범례: O=수집, X=null, △=상세페이지 enrichment로 조건부 수집

컬럼 번호는 위 스키마의 필드 번호에 대응:
- ①sourceCode ②originalId ③title ④description ⑤detailContent ⑥thumbnailUrl ⑦originalUrl ⑧category ⑨status
- ⑩recruitCount ⑪applyStartDate ⑫applyEndDate ⑬announcementDate ⑭reward ⑮mission ⑯address ⑰keywords ⑱currentApplicants

> **상세페이지 enrichment**: 활성 크롤러에 `DetailPageEnricher`가 적용됨. `parseDetailPage`가 상세 페이지 HTML에서 meta description, 신청자 수, 주소, 사이트별 추가 필드를 추출. 단, REVE는 enricher 미사용 (API에서 전체 데이터 직접 수집).
> **⑯ address 추출**: `DetailPageEnricher.extractAddress(doc)` 5단계 fallback 전략 (라벨 기반 → Tanzsoft 패턴 → CSS 클래스 → 한국 주소 정규식 → 지도 링크). 일부 크롤러는 API 상세 호출로 직접 주소 수집.

### API 기반 크롤러 (JSON/REST) — 10개

| 사이트 | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | ⑨ | ⑩ | ⑪ | ⑫ | ⑬ | ⑭ | ⑮ | ⑯ | ⑰ | ⑱ | 합계 |
|--------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:---:|
| REVE | O | O | O | O | O | O | O | O | O | O | O | O | O | O | O | O | O | X | 17 |
| OHMYBLOG | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| WHOGIUP | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| WEU | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | O | 14 |
| STYLEC | O | O | O | △ | X | O | O | O | O | O | O | O | X | O | O | △ | O | X | 14 |
| FINEADPLE | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | X | 13 |
| FOURBLOG | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| POPOMON | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| CHVU | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| DINNERQUEEN | O | O | O | △ | X | O | O | O | O | △ | X | O | X | △ | O | △ | O | △ | 14 |

### Tanzsoft 계열 크롤러 (동일 JSON 구조) — 4개

| 사이트 | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | ⑨ | ⑩ | ⑪ | ⑫ | ⑬ | ⑭ | ⑮ | ⑯ | ⑰ | ⑱ | 합계 |
|--------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:---:|
| DAILYVIEW | O | O | O | O | △ | O | O | O | O | O | X | O | △ | O | O | △ | O | △ | 16 |
| BLOGLAB | O | O | O | O | △ | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| PAVLOVU | O | O | O | O | △ | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| YOGITG | O | O | O | O | △ | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |

### SSR HTML 파싱 크롤러 (Jsoup) — 19개

| 사이트 | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | ⑨ | ⑩ | ⑪ | ⑫ | ⑬ | ⑭ | ⑮ | ⑯ | ⑰ | ⑱ | 합계 |
|--------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:---:|
| GANGNAM | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | X | △ | 13 |
| SEOULOUBA | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| REVIEWPLACE | O | O | O | △ | X | O | O | O | O | O | X | O | △ | O | O | △ | O | △ | 15 |
| CHERIVU | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| CHERRYCOOK | O | O | O | △ | X | O | O | O | O | O | X | O | X | △ | △ | △ | O | △ | 14 |
| TQUEENS | O | O | O | △ | X | O | O | O | O | △ | X | O | X | △ | O | △ | O | △ | 14 |
| TBLE | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| PLAYVIEW | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| HUKI | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | △ | △ | O | △ | 14 |
| ASSAVIEW | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| BEAUTY_QUEEN | O | O | O | △ | X | O | O | O | O | △ | X | O | X | △ | O | △ | O | △ | 14 |
| BLOGDEX | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |
| DDOK | O | O | O | △ | X | O | O | O | O | O | X | O | X | △ | △ | O | O | △ | 14 |
| GABOJA | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | △ | 14 |
| GUGUDAS | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | △ | O | X | 14 |
| MRBLOG | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | △ | 14 |
| OPENREVIEW | O | O | O | △ | X | O | O | O | O | O | X | O | △ | O | O | △ | O | △ | 15 |
| REAL_REVIEW | O | O | O | △ | X | O | O | O | O | O | X | △ | X | △ | △ | △ | O | △ | 14 |
| COMETOPLAY | O | O | O | △ | X | O | O | O | O | O | X | O | X | △ | O | △ | O | △ | 15 |

### Next.js SSR 크롤러 — 1개

| 사이트 | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | ⑨ | ⑩ | ⑪ | ⑫ | ⑬ | ⑭ | ⑮ | ⑯ | ⑰ | ⑱ | 합계 |
|--------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:---:|
| REVIEWNOTE | O | O | O | △ | X | O | O | O | O | O | X | O | X | O | O | O | O | X | 14 |

### 비활성 크롤러 (8개, data.sql active=false)

| 사이트 | 상태 | 비고 |
|--------|------|------|
| REVU | 비활성 | AngularJS SPA, Jsoup 파싱 불가. REVE API 크롤러가 대체. |
| ODIYA | 비활성 | DNS 해석 실패, 사이트 다운 지속. |
| REVIEW_EXPEDITION | 비활성 | 사이트 폐쇄 (DNS 조회 실패). |
| WEREVIEW | 비활성 | 숏폼(릴스/틱톡) 전문, 블로그 체험단 아님. |
| ALLJAM | 비활성 | 주소 미제공. |
| CHEHUMDAN | 비활성 | 주소 마스킹 (선정자만 확인 가능). |
| CLOUDREVIEW | 비활성 | 주소 미제공. |
| LINKTUBE | 비활성 | 주소 미제공. |
| RINGBLE | 비활성 | 비활성화. |
| STORYNMEDIA | 비활성 | 사이트 변경 (체험단 플랫폼 아님). |

---

## 3. 필드별 수집 커버리지 요약 (활성 34개 기준)

| # | 필드 | 한국어명 | O | △ | X | 수집합계 | 비율 | 미수집 사이트 |
|---|------|----------|:-:|:-:|:-:|:-------:|:----:|---------------|
| 1 | sourceCode | 소스코드 | 34 | 0 | 0 | 34 | 100% | - |
| 2 | originalId | 원본ID | 34 | 0 | 0 | 34 | 100% | - |
| 3 | title | 제목 | 34 | 0 | 0 | 34 | 100% | - |
| 4 | description | 설명 | 4 | 30 | 0 | 34 | 100% | - |
| 5 | detailContent | 상세콘텐츠 | 1 | 4 | 29 | 5 | 15% | REVE/Tanzsoft 외 전부 |
| 6 | thumbnailUrl | 썸네일URL | 34 | 0 | 0 | 34 | 100% | - |
| 7 | originalUrl | 원본URL | 34 | 0 | 0 | 34 | 100% | - |
| 8 | category | 카테고리 | 34 | 0 | 0 | 34 | 100% | - |
| 9 | status | 상태 | 34 | 0 | 0 | 34 | 100% | - |
| 10 | recruitCount | 모집인원 | 31 | 3 | 0 | 34 | 100% | - |
| 11 | applyStartDate | 신청시작일 | 2 | 0 | 32 | 2 | 6% | REVE, STYLEC 외 전부 |
| 12 | applyEndDate | 신청마감일 | 31 | 2 | 1 | 33 | 97% | - |
| 13 | announcementDate | 발표일 | 1 | 2 | 31 | 3 | 9% | REVE/DAILYVIEW/OPENREVIEW 외 전부 |
| 14 | reward | 제공내역 | 25 | 9 | 0 | 34 | 100% | - |
| 15 | mission | 미션 | 29 | 5 | 0 | 34 | 100% | - |
| 16 | address | 주소 | 13 | 21 | 0 | 34 | 100% | - |
| 17 | keywords | 키워드 | 33 | 0 | 1 | 33 | 97% | GANGNAM |
| 18 | currentApplicants | 신청자수 | 1 | 21 | 12 | 22 | 65% | 아래 참고 |

> △ = 상세페이지 enrichment로 조건부 수집. `detail-fetch-enabled: true` 시에만 동작.

### ⑯ address(주소) 추출 방식 상세

| 방식 | 사이트 수 | 사이트 | 추출 로직 |
|------|:---------:|--------|-----------|
| O (API 직접) | 9 | REVE, WEU, BLOGDEX, MRBLOG, CHERIVU, CHVU, OHMYBLOG, WHOGIUP, POPOMON | API 필드에서 직접 추출 또는 상세 API 호출 |
| O (리스트/AJAX 직접) | 5 | REVIEWNOTE, DDOK, FOURBLOG, GUGUDAS, GANGNAM | 리스트 데이터 또는 AJAX 상세에서 주소 추출 |
| △ (커스텀 parseDetailPage) | 7 | SEOULOUBA, PLAYVIEW, GABOJA, OPENREVIEW, COMETOPLAY, DAILYVIEW, BLOGLAB, PAVLOVU, YOGITG | 상세페이지에서 "주소/위치" 라벨 자체 탐색 |
| △ (enricher) | 13 | 나머지 | `DetailPageEnricher.extractAddress(doc)` 5단계 fallback |

> `DetailPageEnricher.extractAddress()` 5단계 전략:
> - A: 라벨("주소","위치","업체주소","방문주소","매장주소","장소") 기반 형제/인라인 추출
> - B: Tanzsoft `ul.basic_form li` 패턴
> - C: CSS 클래스 기반 (`.address`, `[class*=address]`, `[data-address]`)
> - D: 상세 콘텐츠 영역에서 한국 주소 정규식 패턴 매칭
> - E: 네이버/카카오 지도 링크 주변 텍스트

### 수집률 등급 분류

| 등급 | 기준 | 해당 필드 (개수) |
|------|------|-----------------|
| 공통 (100%) | 34/34 | sourceCode, originalId, title, description, thumbnailUrl, originalUrl, category, status, recruitCount, reward, mission, address (12개) |
| 준공통 (97%) | 33/34 | applyEndDate, keywords (2개) |
| 선택 (65%) | 22/34 | currentApplicants (1개) |
| 희귀 (6-15%) | 2-5/34 | detailContent, announcementDate, applyStartDate (3개) |

### currentApplicants(신청자수) 미수집 사이트 (12개)

REVE, OHMYBLOG, WHOGIUP, STYLEC, FINEADPLE, FOURBLOG, POPOMON, CHVU, CHERIVU, BLOGDEX, GUGUDAS, REVIEWNOTE

---

## 4. 사이트별 수집 필드 수 랭킹

| 순위 | 사이트 | 수집 필드 수 | 유형 |
|:----:|--------|:----------:|------|
| 1 | REVE | 17/18 | API |
| 2 | DAILYVIEW | 16/18 | Tanzsoft |
| 3 | OPENREVIEW | 15/18 | SSR |
| 3 | REVIEWPLACE | 15/18 | SSR |
| 3 | COMETOPLAY | 15/18 | SSR |
| 6 | WEU | 14/18 | API |
| 6 | OHMYBLOG | 14/18 | API |
| 6 | WHOGIUP | 14/18 | API |
| 6 | STYLEC | 14/18 | API |
| 6 | FOURBLOG | 14/18 | API |
| 6 | POPOMON | 14/18 | API |
| 6 | CHVU | 14/18 | API |
| 6 | DINNERQUEEN | 14/18 | API |
| 6 | BLOGLAB | 14/18 | Tanzsoft |
| 6 | PAVLOVU | 14/18 | Tanzsoft |
| 6 | YOGITG | 14/18 | Tanzsoft |
| 6 | SEOULOUBA | 14/18 | SSR |
| 6 | CHERIVU | 14/18 | SSR |
| 6 | CHERRYCOOK | 14/18 | SSR |
| 6 | TQUEENS | 14/18 | SSR |
| 6 | TBLE | 14/18 | SSR |
| 6 | PLAYVIEW | 14/18 | SSR |
| 6 | HUKI | 14/18 | SSR |
| 6 | ASSAVIEW | 14/18 | SSR |
| 6 | BEAUTY_QUEEN | 14/18 | SSR |
| 6 | BLOGDEX | 14/18 | SSR |
| 6 | DDOK | 14/18 | SSR |
| 6 | GABOJA | 14/18 | SSR |
| 6 | GUGUDAS | 14/18 | SSR |
| 6 | MRBLOG | 14/18 | SSR |
| 6 | REAL_REVIEW | 14/18 | SSR |
| 6 | REVIEWNOTE | 14/18 | Next.js |
| 33 | GANGNAM | 13/18 | SSR |
| 34 | FINEADPLE | 13/18 | API |

> 평균 수집 필드: 14.1/18 (78%)
> REVE가 17개 필드로 가장 많이 수집 (currentApplicants만 미수집).

---

## 5. 미수집 원본 데이터 (CrawledCampaign에 없는 필드)

각 사이트의 API/HTML 응답에서 확인되지만 현재 CrawledCampaign 스키마에 매핑되지 않는 데이터.

### 5-1. 다수 사이트에서 공통 제공하지만 미매핑

| 원본 필드 | 제공 사이트 | 설명 |
|-----------|-------------|------|
| SNS/채널 유형 (snsType, channel, media) | STYLEC, FINEADPLE, REVE, REVIEWNOTE | instagram, youtube, blog, tiktok, clip 등 |
| 캠페인 타입 (campaignType, type) | WEU, POPOMON, REVE, REVIEWPLACE | 방문형/배송형/구매형/기자단 등 |
| 지역/시도 (region, city, sido) | REVIEWNOTE, REVE, SEOULOUBA, MRBLOG | 시/도 단위 지역 정보 |

### 5-2. 개별 사이트에서만 제공

| 사이트 | 원본 필드 | 설명 |
|--------|-----------|------|
| REVE | venue.addressFirst/addressLast | 상세 주소 (분리형) |
| REVE | localTag | 지역 태그 |
| REVE | posting period | 리뷰 작성 기간 |
| REVE | contentImage | 상세 이미지 URL |
| OHMYBLOG | companyName | 광고주/업체명 |
| OHMYBLOG | typeText | 캠페인 유형 텍스트 |
| WHOGIUP | ended (boolean) | 마감 여부 플래그 |
| WHOGIUP | product | 제공 상품명 (→ reward로 매핑 중) |
| POPOMON | C_state | 캠페인 상태 코드 (ONGOING 등) |
| POPOMON | ctType, recruitType | 세부 캠페인/모집 유형 |
| POPOMON | C_address_full | 상세 주소 (상세 API에서 수집) |
| FOURBLOG | REMAINDATE | D-day 카운트다운 값 |
| FOURBLOG | REVIEWER_BENEFIT | 리뷰어 혜택 상세 |
| COMETOPLAY | it_description | 해시태그 기반 설명 |
| STYLEC | caName, wrType | 카테고리명, 리뷰 유형 |
| CHERIVU | lat, lon | 위경도 좌표 |
| REVIEWNOTE | lat, lng | 위경도 좌표 |

---

## 6. 크롤링 방식별 데이터 풍부도 비교

| 크롤링 방식 | 사이트 수 | 평균 수집 필드 | 특징 |
|-------------|-----------|---------------|------|
| REST API (JSON) | 10 | 14.0/18 | 구조화된 데이터, REVE만 17/18 수집. 상세 API로 주소 추출 (CHVU, OHMYBLOG, WHOGIUP, POPOMON) |
| Tanzsoft JSON | 4 | 14.5/18 | 동일 플랫폼 기반, enrichment로 detailContent/address/announcementDate 추가 |
| SSR HTML (Jsoup) | 19 | 14.0/18 | 가장 많은 사이트, enrichment로 description/reward/address/currentApplicants 추가 |
| Next.js SSR | 1 | 14.0/18 | dehydrated state 파싱 + 상세 API로 주소 추출 (쿠키 인증) |

---

## 7. 사이트별 최대 수집 건수

1회 크롤링 시 사이트별로 최대 몇 개의 캠페인을 수집할 수 있는지 정리한다.

**글로벌 설정**: `max-pages-per-site: 10` (`application-local.yml:29`)

### A. 페이지네이션 크롤러 — 21개

| 사이트명 | 페이지당 건수 | 최대 건수 공식 | 예상 최대 건수 |
|----------|:------------:|---------------|:------------:|
| REVE | 35 | 35 x 10 | ~350 |
| GANGNAM | 가변(~28) | ~28 x 10 | ~280 |
| GUGUDAS | 20 | 20 x 10 x 2 | 400 |
| WEU | 20 | 20 x 10 | 200 |
| CHVU | 20 | 20 x 10 | 200 |
| FINEADPLE | 20 | 20 x 10 | 200 |
| STYLEC | 20 | 20 x 10 | 200 |
| OPENREVIEW | 20 | 20 x 10 | 200 |
| POPOMON | 12 | 12 x 10 | 120 |
| CHERIVU | 가변 | 가변 x 10 | ~100 |
| SEOULOUBA | 가변 | 가변 x 10 | ~100 |
| DINNERQUEEN | 가변 | 가변 x 10 | ~100 |
| OHMYBLOG | 가변 | 가변 x 10 | ~100 |
| REAL_REVIEW | 가변 | 가변 x 10 | ~100 |
| TQUEENS | 가변 | 가변 x 10 | ~80 |
| BEAUTY_QUEEN | 가변 | 가변 x 10 | ~80 |
| BLOGDEX | 가변 | 가변 x 10 | ~80 |
| DAILYVIEW | 가변 | 가변 x 10 | ~80 |
| BLOGLAB | 가변 | 가변 x 10 | ~80 |
| PAVLOVU | 가변 | 가변 x 10 | ~80 |
| YOGITG | 가변 | 가변 x 10 | ~80 |

### B. 단일 페이지 크롤러 — 6개

| 사이트명 | 비고 | 예상 최대 건수 |
|----------|------|:------------:|
| REVIEWPLACE | 홈페이지 전체 아이템 | ~20-30 |
| TBLE | 단일 리스트 페이지 | ~20-30 |
| PLAYVIEW | 단일 리스트 페이지 | ~20-30 |
| CHERRYCOOK | /mission 페이지 | ~10-20 |
| DDOK | /campaign 페이지 | ~10-15 |
| MRBLOG | 메인 페이지 | ~10-15 |
| ASSAVIEW | campaign_list 페이지 | ~20-30 |

### C. 고정 다중 페이지/섹션 크롤러 — 5개

| 사이트명 | 수집 방식 | 예상 최대 건수 |
|----------|----------|:------------:|
| FOURBLOG | 단일 API, limit=300 | ~300 |
| HUKI | 4개 섹션 (product/local/review/news) | ~120 |
| WHOGIUP | 2개 타입 (region/product) | ~40-100 |
| GABOJA | 4개 섹션 (pick/new/closing/hit) | ~40-60 |
| REVIEWNOTE | 4개 정렬 (new/popular/premium/nearEnd), 중복제거 | ~30-60 |

### 전체 합산 예상치

| 유형 | 사이트 수 | 예상 합계 건수 |
|------|:---------:|:------------:|
| A. 페이지네이션 | 21 | ~2,800-3,500 |
| B. 단일 페이지 | 7 | ~100-170 |
| C. 다중 섹션 | 5 | ~530-640 |
| **합계** | **34** | **~3,430-4,310** |

> 실제 수집 건수는 사이트의 활성 캠페인 수에 따라 예상 최대보다 적을 수 있다.
> 중복 방지(`crawlingSource + originalId` 유니크 제약)로 DB에는 고유 캠페인만 저장된다.
