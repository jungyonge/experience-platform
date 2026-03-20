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

## 2. 공통 필드 — 모든 사이트에서 수집 (6개)

| 필드 | 한국어명 | 커버리지 | 비고 |
|------|----------|----------|------|
| originalId | 원본ID | 46/46 (100%) | 사이트 고유 ID 또는 URL 기반 생성 |
| title | 제목 | 46/46 (100%) | 캠페인 제목 |
| originalUrl | 원본URL | 46/46 (100%) | 상세 페이지 링크 |
| category | 카테고리 | 46/46 (100%) | CategoryMapper로 제목 기반 분류 |
| status | 상태 | 46/46 (100%) | RECRUITING/CLOSED 판별 |
| keywords | 키워드 | 46/46 (100%) | 사이트명+태그 조합 |

---

## 3. 준공통 필드 — 80% 이상 사이트에서 수집 (5개)

| 필드 | 한국어명 | 커버리지 | 미수집 사이트 |
|------|----------|----------|---------------|
| thumbnailUrl | 썸네일URL | 45/46 (98%) | TOJOBCN |
| description | 설명 | △44/46 (96%) | TOJOBCN, REVIEW_EXPEDITION |
| mission | 미션 | △46/46 (100%) | — (상세페이지에서 추가 추출 시도) |
| applyEndDate | 신청마감일 | △43/46 (93%) | ALLJAM, TOJOBCN, LINKTUBE |
| recruitCount | 모집인원 | △43/46 (93%) | TOJOBCN, RINGBLE, LINKTUBE |

> △ = 상세페이지 enrichment로 조건부 수집. `DetailPageEnricher`가 활성화된 경우 상세 페이지에서 추가 추출 시도.
> recruitCount는 일부 사이트에서 조건부 수집 (값이 0이면 null 처리): REVIEWPLACE, WEU, CHVU, FINEADPLE, FOURBLOG, POPOMON, WHOGIUP, GABOJA, MIBLE, ODIYA, OPENREVIEW, COMETOPLAY, STYLEC
> description은 리스트에서 7개 사이트 수집 + 상세페이지 meta[name=description] / meta[property=og:description]에서 37개 사이트 추가 추출

---

## 4. 선택 필드 — 50~80% 사이트에서 수집 (2개)

| 필드 | 한국어명 | 커버리지 | 수집 사이트 |
|------|----------|----------|-------------|
| reward | 제공내역 | △39/46 (85%) | 리스트 29개 + 상세페이지 enrichment △10개 (REVU, MBLE, DINNERQUEEN, BEAUTY_QUEEN, TQUEENS, CHERRYCOOK, CLOUDREVIEW, DDOK, REAL_REVIEW, STORYNMEDIA) |
| currentApplicants | 현재신청자수 | △37/46 (80%) | 상세페이지 "신청 N" 패턴 추출. Archetype B 크롤러 37개 (API/SPA 7개 제외) |

미수집 reward 사이트 (7개): RINGBLE, WEU, TOJOBCN, COMETOPLAY, LINKTUBE, ODIYA, WEREVIEW

---

## 5. 희귀 필드 — 30% 미만 사이트에서 수집 (4개)

| 필드 | 한국어명 | 커버리지 | 수집 사이트 |
|------|----------|----------|-------------|
| address | 주소 | △8/46 (17%) | 리스트 5개 (REVE, REVIEWNOTE, BLOGDEX, GUGUDAS, MIBLE) + 상세페이지 △3개 (GANGNAM, SEOULOUBA, PLAYVIEW) |
| applyStartDate | 신청시작일 | 3/46 (7%) | REVE, STYLEC, WEREVIEW |
| detailContent | 상세콘텐츠 | 1/46 (2%) | REVE (contentImage를 HTML img 태그로 래핑) |
| announcementDate | 발표일 | 1/46 (2%) | REVE |

---

## 6. 사이트별 수집 필드 매트릭스 (46개)

범례: O=수집, X=null, △=상세페이지 enrichment로 조건부 수집

공통 6개 필드(originalId(원본ID), title(제목), originalUrl(원본URL), category(카테고리), status(상태), keywords(키워드))는 모든 사이트에서 O이므로 생략.

> **상세페이지 enrichment**: 전체 44개 활성 크롤러에 `DetailPageEnricher`가 적용됨. `parseDetailPage`가 상세 페이지 HTML에서 meta description, 신청자 수, 사이트별 추가 필드를 추출.

### 6-1. API 기반 크롤러 (JSON/REST)

| 사이트 | 설명 | 상세콘텐츠 | 썸네일 | 모집인원 | 신청시작일 | 신청마감일 | 발표일 | 제공내역 | 미션 | 주소 | 신청자수 |
|--------|:----:|:--------:|:-----:|:-------:|:--------:|:--------:|:-----:|:-------:|:----:|:----:|:-------:|
| REVE | O | O | O | O | O | O | O | O | O | O | O |
| OHMYBLOG | △ | X | O | O | X | O | X | O | O | X | X |
| WHOGIUP | △ | X | O | △ | X | O | X | O | O | X | △ |
| WEU | △ | X | O | △ | X | O | X | X | O | X | △ |
| STYLEC | △ | X | O | △ | O | O | X | O | O | X | △ |
| FINEADPLE | △ | X | O | △ | X | O | X | O | O | X | △ |
| FOURBLOG | △ | X | O | △ | X | O | X | O | O | X | △ |
| POPOMON | △ | X | O | △ | X | O | X | O | O | X | △ |
| CHVU | △ | X | O | △ | X | O | X | O | O | X | △ |
| DINNERQUEEN | △ | X | O | △ | X | O | X | △ | O | X | △ |

### 6-2. Tanzsoft 계열 크롤러 (동일 JSON 구조)

| 사이트 | 설명 | 상세콘텐츠 | 썸네일 | 모집인원 | 신청시작일 | 신청마감일 | 발표일 | 제공내역 | 미션 | 주소 | 신청자수 |
|--------|:----:|:--------:|:-----:|:-------:|:--------:|:--------:|:-----:|:-------:|:----:|:----:|:-------:|
| DAILYVIEW | O | X | O | O | X | O | X | O | O | X | △ |
| BLOGLAB | O | X | O | O | X | O | X | O | O | X | △ |
| PAVLOVU | O | X | O | O | X | O | X | O | O | X | △ |
| YOGITG | O | X | O | O | X | O | X | O | O | X | △ |

### 6-3. SSR HTML 파싱 크롤러 (Jsoup)

| 사이트 | 설명 | 상세콘텐츠 | 썸네일 | 모집인원 | 신청시작일 | 신청마감일 | 발표일 | 제공내역 | 미션 | 주소 | 신청자수 |
|--------|:----:|:--------:|:-----:|:-------:|:--------:|:--------:|:-----:|:-------:|:----:|:----:|:-------:|
| REVU | △ | X | O | O | X | O | X | △ | △ | X | △ |
| MBLE | △ | X | O | O | X | O | X | △ | O | X | △ |
| GANGNAM | △ | X | O | O | X | O | X | O | O | △ | △ |
| SEOULOUBA | △ | X | O | O | X | O | X | O | O | △ | △ |
| REVIEWPLACE | △ | X | O | △ | X | O | △ | O | O | X | △ |
| ALLJAM | △ | X | O | O | X | X | X | O | O | X | X |
| CHERIVU | △ | X | O | O | X | O | X | O | O | X | X |
| CHERRYCOOK | △ | X | O | O | X | O | X | △ | △ | X | △ |
| CHEHUMDAN | △ | X | O | O | X | △ | X | O | △ | X | △ |
| CLOUDREVIEW | △ | X | O | O | X | O | X | △ | O | X | △ |
| TQUEENS | △ | X | O | △ | X | O | X | △ | O | X | △ |
| TBLE | △ | X | O | O | X | O | X | O | △ | X | △ |
| PLAYVIEW | △ | X | O | O | X | O | X | O | O | △ | △ |
| HUKI | △ | X | O | O | X | O | X | O | △ | X | △ |
| ASSAVIEW | △ | X | O | O | X | O | X | O | △ | X | △ |
| BEAUTY_QUEEN | △ | X | O | △ | X | O | X | △ | O | X | △ |
| BLOGDEX | △ | X | O | O | X | O | X | O | O | O | X |
| DDOK | △ | X | O | O | X | O | X | △ | △ | X | △ |
| GABOJA | △ | X | O | △ | X | O | X | O | O | X | △ |
| GUGUDAS | △ | X | O | O | X | O | X | O | O | O | X |
| MIBLE | △ | X | O | △ | X | O | X | O | O | O | △ |
| ODIYA | △ | X | O | △ | X | O | X | X | O | X | △ |
| OPENREVIEW | △ | X | O | △ | X | O | X | O | O | X | △ |
| REAL_REVIEW | △ | X | O | O | X | △ | X | △ | △ | X | △ |
| RINGBLE | △ | X | O | X | X | O | X | X | O | X | △ |
| COMETOPLAY | △ | X | O | △ | X | O | X | X | O | X | △ |
| LINKTUBE | △ | X | O | X | X | X | X | X | O | X | △ |
| TOJOBCN | X | X | X | X | X | X | X | X | O | X | X |
| STORYNMEDIA | △ | X | O | △ | X | △ | X | △ | △ | X | △ |

### 6-4. Next.js SSR / SPA 크롤러

| 사이트 | 설명 | 상세콘텐츠 | 썸네일 | 모집인원 | 신청시작일 | 신청마감일 | 발표일 | 제공내역 | 미션 | 주소 | 신청자수 |
|--------|:----:|:--------:|:-----:|:-------:|:--------:|:--------:|:-----:|:-------:|:----:|:----:|:-------:|
| REVIEWNOTE | △ | X | O | O | X | O | X | O | O | O | X |
| WEREVIEW | △ | X | O | O | O | O | X | X | O | X | X |

### 6-5. 오프라인/비활성 크롤러

| 사이트 | 상태 | 비고 |
|--------|------|------|
| REVIEW_EXPEDITION | 사이트 오프라인 | DNS 해석 실패, 빈 리스트 반환 |

---

## 7. 미수집 원본 데이터 (CrawledCampaign에 없는 필드)

각 사이트의 API/HTML 응답에서 확인되지만 현재 CrawledCampaign 스키마에 매핑되지 않는 데이터.

### 7-1. 다수 사이트에서 공통 제공하지만 미매핑

| 원본 필드 | 제공 사이트 | 설명 |
|-----------|-------------|------|
| SNS/채널 유형 (snsType, channel, media) | STYLEC, FINEADPLE, LINKTUBE, REVE, REVIEWNOTE | instagram, youtube, blog, tiktok, clip 등 |
| 캠페인 타입 (campaignType, type) | WEU, POPOMON, REVE, REVIEWPLACE | 방문형/배송형/구매형/기자단 등 |
| 지역/시도 (region, city, sido) | REVIEWNOTE, REVE, SEOULOUBA, MIBLE | 시/도 단위 지역 정보 |

> 신청자 수(currentApplicants)는 CrawledCampaign 스키마에 추가되어 상세페이지 enrichment로 수집 중 (△37/46 사이트).

### 7-2. 개별 사이트에서만 제공

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
| WEREVIEW | platform type | 숏폼 영상 플랫폼 (숏폼 전용) |
| POPOMON | C_state | 캠페인 상태 코드 (ONGOING 등) |
| POPOMON | ctType, recruitType | 세부 캠페인/모집 유형 |
| FOURBLOG | REMAINDATE | D-day 카운트다운 값 |
| FOURBLOG | REVIEWER_BENEFIT | 리뷰어 혜택 상세 |
| COMETOPLAY | it_description | 해시태그 기반 설명 |
| STYLEC | caName, wrType | 카테고리명, 리뷰 유형 |

---

## 8. 크롤링 방식별 데이터 풍부도 비교

| 크롤링 방식 | 사이트 수 | 평균 수집 필드 | 특징 |
|-------------|-----------|---------------|------|
| REST API (JSON) | 10 | 12.5/18 | 구조화된 데이터 + 상세페이지 enrichment |
| Tanzsoft JSON | 4 | 13/18 | 동일 플랫폼 기반, description + enrichment |
| SSR HTML (Jsoup) | 29 | 12.0/18 | 가장 많은 사이트, enrichment로 description/currentApplicants 추가 |
| Next.js SSR/SPA | 2 | 11.5/18 | dehydrated state 파싱 + og:description enrichment |

### 방식별 필드 특성

**API 기반 (JSON)** — REVE가 유일하게 전체 18개 필드 수집. 상세페이지 enrichment로 description(△), currentApplicants(△) 추가.

**Tanzsoft 계열** — DAILYVIEW, BLOGLAB, PAVLOVU, YOGITG 4개 사이트가 동일 JSON 구조. 기존 description + enrichment로 currentApplicants 추가.

**SSR HTML (Jsoup)** — 29개로 가장 큰 그룹. enrichment 적용으로 description(meta 태그), currentApplicants("신청 N" 패턴), 사이트별 추가 필드(reward, mission, address) 수집 가능. detailContent, applyStartDate, announcementDate는 거의 수집 불가.

**Next.js/SPA** — REVIEWNOTE는 address 수집 가능. WEREVIEW는 applyStartDate 수집 가능하나 숏폼 특화로 reward 미제공. 두 사이트 모두 og:description enrichment 적용.

---

## 9. 필드 수집률 요약

| 필드 | 한국어명 | 수집 사이트 수 | 비율 | 등급 |
|------|----------|---------------|------|------|
| originalId | 원본ID | 46 | 100% | 공통 |
| title | 제목 | 46 | 100% | 공통 |
| originalUrl | 원본URL | 46 | 100% | 공통 |
| category | 카테고리 | 46 | 100% | 공통 |
| status | 상태 | 46 | 100% | 공통 |
| keywords | 키워드 | 46 | 100% | 공통 |
| thumbnailUrl | 썸네일URL | 45 | 98% | 준공통 |
| description | 설명 | △44 | 96% | 준공통 |
| mission | 미션 | △46 | 100% | 준공통 |
| applyEndDate | 신청마감일 | △43 | 93% | 준공통 |
| recruitCount | 모집인원 | △43 | 93% | 준공통 |
| reward | 제공내역 | △39 | 85% | 선택 |
| currentApplicants | 현재신청자수 | △37 | 80% | 선택 |
| address | 주소 | △8 | 17% | 희귀 |
| applyStartDate | 신청시작일 | 3 | 7% | 희귀 |
| detailContent | 상세콘텐츠 | 1 | 2% | 희귀 |
| announcementDate | 발표일 | 1 | 2% | 희귀 |

> △ = 상세페이지 enrichment로 조건부 수집 포함. `detail-fetch-enabled: true` 시에만 동작.
> REVE(레뷰 API)가 유일하게 18개 필드 전체 수집 가능한 사이트.
> TOJOBCN(투잡커넥트)이 가장 적은 필드 수집 (7개: 공통 6개 + mission).

---

## 10. 사이트별 최대 수집 건수

1회 크롤링 시 사이트별로 최대 몇 개의 캠페인을 수집할 수 있는지 정리한다.

**글로벌 설정**: `max-pages-per-site: 5` (`application-local.yml:29`)

### A. 페이지네이션 크롤러 (maxPagesPerSite=5 적용) — 25개

페이지 번호를 증가시키며 `maxPagesPerSite`까지 반복하는 크롤러.

| 사이트명 | 페이지당 건수 | 섹션 수 | 최대 건수 공식 | 예상 최대 건수 |
|----------|:------------:|:------:|---------------|:------------:|
| REVU | 35 | 1 | 35 × 5 | ~175 |
| REVE | 35 | 1 | 35 × 5 | ~175 |
| GANGNAM | 가변(~28) | 1 | ~28 × 5 | ~140 |
| GUGUDAS | 20 | 2 | 20 × 5 × 2 | 200 |
| WEU | 20 | 1 | 20 × 5 | 100 |
| CHVU | 20 | 1 | 20 × 5 | 100 |
| FINEADPLE | 20 | 1 | 20 × 5 | 100 |
| STYLEC | 20 | 1 | 20 × 5 | 100 |
| OPENREVIEW | 20 | 1 | 20 × 5 | 100 |
| POPOMON | 12 | 1 | 12 × 5 | 60 |
| ALLJAM | 가변 | 1 | 가변 × 5 | ~50 |
| CHERIVU | 가변 | 1 | 가변 × 5 | ~50 |
| MBLE | 가변 | 1 | 가변 × 5 | ~50 |
| SEOULOUBA | 가변 | 1 | 가변 × 5 | ~50 |
| DINNERQUEEN | 가변 | 1 | 가변 × 5 | ~50 |
| OHMYBLOG | 가변 | 1 | 가변 × 5 | ~50 |
| REALREVIEW | 가변 | 1 | 가변 × 5 | ~50 |
| TQUEENS | 가변 | 1 | 가변 × 5 | ~40 |
| BEAUTYQUEEN | 가변 | 1 | 가변 × 5 | ~40 |
| BLOGDEX | 가변 | 1 | 가변 × 5 | ~40 |
| TOJOBCN | 가변 | 1 | 가변 × 5 | ~40 |
| DAILYVIEW | 가변 | 1 | 가변 × 5 | ~40 |
| BLOGLAB | 가변 | 1 | 가변 × 5 | ~40 |
| PAVLOVU | 가변 | 1 | 가변 × 5 | ~40 |
| YOGITG | 가변 | 1 | 가변 × 5 | ~40 |

### B. 단일 페이지 크롤러 (페이지네이션 없음) — 12개

한 번의 HTTP 요청으로 리스트 페이지를 가져와 파싱하는 크롤러.

| 사이트명 | 비고 | 예상 최대 건수 |
|----------|------|:------------:|
| REVIEWPLACE | 홈페이지 전체 아이템 | ~20-30 |
| TBLE | 단일 리스트 페이지 | ~20-30 |
| PLAYVIEW | 단일 리스트 페이지 | ~20-30 |
| CHERRYCOOK | /mission 페이지 | ~10-20 |
| CHEHUMDAN | all_campaign.html | ~10-20 |
| CLOUDREVIEW | /campaign/blog 페이지 | ~10-20 |
| DDOK | /campaign 페이지 | ~10-15 |
| LINKTUBE | /product 페이지 | ~10-15 |
| MIBLE | 메인 페이지 | ~10-15 |
| ODIYA | 카테고리 829 페이지 | ~10-15 |
| STORYNMEDIA | 메인 페이지 | ~10-15 |
| WEREVIEW | Next.js SSR 단일 페이지 | ~10-15 |

### C. 고정 다중 페이지/섹션 크롤러 — 6개

여러 섹션·카테고리·정렬을 순회하거나 고정 파라미터로 대량 요청하는 크롤러.

| 사이트명 | 수집 방식 | 예상 최대 건수 |
|----------|----------|:------------:|
| FOURBLOG | 단일 API, limit=300 | ~300 |
| HUKI | 4개 섹션 (product/local/review/news) | ~120 |
| WHOGIUP | 2개 타입 (region/product) | ~40-100 |
| GABOJA | 4개 섹션 (pick/new/closing/hit) | ~40-60 |
| REVIEWNOTE | 4개 정렬 (new/popular/premium/nearEnd), 중복제거 | ~30-60 |
| RINGBLE | 2개 페이지 (메인+마감) | ~20-40 |

### D. 비활성 — 1개

| 사이트명 | 상태 |
|----------|------|
| REVIEW_EXPEDITION | DNS 실패, 빈 리스트 반환 |

### 전체 합산 예상치

| 유형 | 사이트 수 | 예상 합계 건수 |
|------|:---------:|:------------:|
| A. 페이지네이션 | 25 | ~1,800-2,000 |
| B. 단일 페이지 | 12 | ~150-250 |
| C. 다중 섹션 | 6 | ~550-680 |
| D. 비활성 | 1 | 0 |
| **합계** | **44** | **~2,500-2,930** |

> 실제 수집 건수는 사이트의 활성 캠페인 수에 따라 예상 최대보다 적을 수 있다.
> 중복 방지(`crawlingSource + originalId` 유니크 제약)로 DB에는 고유 캠페인만 저장된다.
