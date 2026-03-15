당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 체험단 상세 페이지 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록 조회 기능(Phase 1~16)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 17~21)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"체험단 상세 페이지 프로젝트 명세를 완벽히 이해했습니다. Phase 17 진행을 시작할까요?"**

그 후 사용자가 **"진행해"** 라고 답변하면, 그때 **해당 Phase의 코드만 생성**하세요.

각 Phase가 끝나면 반드시 아래 문장 형식으로 마무리하세요.

**"Phase X 작업이 완료되었습니다. Phase X+1 진행을 시작할까요?"**

---

# 응답 형식 규칙

각 Phase를 진행할 때는 반드시 아래 순서를 지키세요.

1. 변경/생성 파일 목록
2. 전체 디렉토리 구조 또는 변경 포인트
3. 각 파일의 전체 코드
4. 핵심 구현 설명
5. 실행/검증 포인트
6. 다음 Phase 진행 여부 질문

부분 코드, 생략 코드, pseudo code는 사용하지 마세요.  
반드시 **실행 가능한 전체 코드** 기준으로 작성하세요.

이전 Phase에서 만든 파일을 수정해야 하면, 수정된 파일도 반드시 **전체 코드 기준으로 다시 보여주세요.**

파일 경로를 항상 명시하세요.

임의의 라이브러리는 추가하지 말고, 필요한 경우 추가 이유를 짧게 먼저 설명하세요.

Lombok은 사용하지 마세요.

---

# 공통 전제

Phase 1~16에서 다음 기능이 이미 구현되어 있습니다.

## 기존 구현 요약
- Member BC
  - 회원가입
  - 로그인
  - JWT 인증
- Campaign BC
  - 캠페인 목록 조회
  - 검색 / 필터 / 페이징 / 정렬
  - 시드 데이터
- React 메인 홈 = 캠페인 목록 페이지
- 공통 Header
  - 검색바
  - 로그인 / 회원가입 / 로그아웃
- 현재 캠페인 카드는 목록 화면에서 렌더링되고 있음

## 현재 카드 클릭 동작
- 기존에는 `originalUrl` 로 새 탭 이동
- 이번 Phase부터는 **플랫폼 내부 상세 페이지(`/campaigns/{id}`)** 로 이동하도록 변경

---

# 기술 스택

## Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 Database

## Frontend
- React
- TypeScript
- Vite
- react-router-dom
- CSS Modules
- ex-design 참고, shad cn 컴포넌트 활용

---

# 아키텍처 원칙

1. **실용적 DDD**를 적용합니다.
2. `Campaign` 은 `Campaign` Bounded Context 의 Aggregate Root 입니다.
3. Repository 인터페이스는 `domain`, 구현체는 `infrastructure` 에 둡니다.
4. `interfaces` DTO와 `application` DTO는 분리합니다.
5. 이번 프로젝트는 실용적 DDD를 적용하므로 domain 엔티티에 JPA 어노테이션 사용을 허용합니다.
6. 상세 조회는 읽기 전용 기능이므로 Application Service에는 `@Transactional(readOnly = true)` 를 적용합니다.
7. 캠페인 상세 조회 API는 **인증 불필요**입니다.
8. 기존 목록/필터 API와 충돌하지 않도록 상세 조회도 같은 `CampaignController` 안에서 일관되게 관리합니다.
9. 상세 설명은 이번 단계에서는 **HTML 렌더링을 지원하지 않고 plain text 기반**으로 처리합니다.
10. XSS 방어를 위해 프론트에서는 `dangerouslySetInnerHTML` 을 사용하지 않습니다.

---

# 이번 범위에서 변경/추가

- `Campaign` 도메인 확장
  - 상세 필드 추가
- 상세 조회 API
  - `GET /api/v1/campaigns/{id}`
- React 상세 페이지
  - `/campaigns/:id`
- 카드 클릭 동작 변경
  - 원본 사이트 새 탭 이동 → 플랫폼 상세 페이지 이동
- 상세 페이지 내 CTA 버튼 제공
  - 원본 사이트로 이동
- 시드 데이터 확장
  - 상세 필드 포함

---

# 이번 범위에서 제외

구현하지 마세요.

- 찜 / 북마크
- 댓글 / 리뷰
- 관련 캠페인 추천
- 조회수 카운팅
- SNS 공유
- HTML sanitization 라이브러리 도입
- WYSIWYG / Markdown 렌더링

---

# 공통 UX / 상세 정책

1. 상세 페이지 경로는 `/campaigns/:id` 입니다.
2. 비로그인도 상세 페이지를 볼 수 있습니다.
3. 목록에서 상세로 들어갔다가 뒤로 가면, **목록의 기존 검색 상태(URL query)** 가 유지되어야 합니다.
4. 목록 카드에서 상세 페이지로 이동할 때는 가능하면 현재 위치(`pathname + search`)를 `location.state.from` 으로 함께 전달하세요.
5. 상세 페이지의 "목록으로 돌아가기" 는 아래 우선순위를 따릅니다.
   1. `location.state.from` 이 있으면 해당 경로로 이동
   2. 브라우저 history가 있으면 `navigate(-1)`
   3. 그래도 불가능하면 `/`
6. 상세 설명(`detailContent`)은 plain text 로 취급합니다.
   - 줄바꿈은 보존해서 렌더링합니다.
   - HTML 태그가 포함되어 있어도 **텍스트 그대로** 보여주세요.
7. `keywords` 는 DB 에 쉼표 구분 문자열로 저장하지만, API/프론트에는 항상 `List<String>` / 배열로 노출합니다.
8. `keywords` 파싱 규칙:
   - 쉼표 기준 분리
   - 각 항목 `trim`
   - 빈 문자열 제거
   - 기존 순서 유지
9. nullable 필드는 값이 없으면 `null` 로 유지합니다.
10. 단, `keywords` 는 값이 없으면 **빈 배열**로 응답합니다.

---

# 상세 페이지 레이아웃 기준

- 공통 Header
- 목록으로 돌아가기 링크
- 상단 메타 정보 영역
  - 썸네일
  - 제목
  - 뱃지
  - 모집 인원
  - 신청 기간
  - 발표일
  - D-day
- 상세 정보 영역
  - 제공 내역
  - 미션
  - 방문 주소
  - 키워드
  - 상세 설명
- 안내 문구
- CTA 버튼
  - 원본 사이트에서 신청하기 / 원본 사이트 보기

---

# Phase 17. Campaign 도메인 상세 필드 확장

## 목표
기존 `Campaign` 도메인에 상세 페이지에 필요한 필드를 추가합니다.

## 추가 필드
기존 `Campaign` 엔티티에 아래 필드를 추가합니다.

- `detailContent`: `String`
  - 상세 설명
  - nullable
  - 최대 5000자
- `reward`: `String`
  - 제공 내역
  - nullable
  - 최대 500자
- `mission`: `String`
  - 미션 내용
  - nullable
  - 최대 500자
- `address`: `String`
  - 방문 주소
  - nullable
  - 최대 300자
- `keywords`: `String`
  - 쉼표 구분 저장
  - 예: `"강남맛집,이탈리안,파스타"`
  - nullable
  - 최대 500자

## 도메인 설계 원칙
- 새 필드는 모두 nullable 입니다.
  - 크롤링 소스별로 제공 정보가 다를 수 있기 때문입니다.
- `keywords` 는 이번 단계에서는 별도 테이블로 분리하지 않습니다.
- `detailContent` 는 이번 단계에서는 **plain text 저장 기준**으로 처리합니다.
  - HTML 렌더링은 지원하지 않습니다.
- JPA 매핑은 H2 호환성과 단순성을 고려해 `@Lob` 또는 동등하게 안전한 방식으로 처리하세요.
- DB 독립성을 고려해 `columnDefinition = "TEXT"` 같은 DB 종속 설정은 꼭 필요하지 않다면 피해주세요.

## Campaign 엔티티 수정 사항
- 기존 필드는 유지
- 새 필드 추가
- `getKeywordList()` 편의 메서드 추가
  - null → 빈 리스트
  - blank → 빈 리스트
  - 쉼표 분리 후 trim
  - 빈 항목 제거
  - 순서 유지
- 기존 `update()` 메서드를 확장
  - 새 필드도 갱신 가능하도록

## 시드 데이터 확장
- 기존 `data.sql` 의 50건+ 데이터에 새 필드를 추가합니다.
- 최소 20건 이상은 상세 필드를 풍부하게 작성하세요.
- 나머지는 일부 필드만 있거나 null 인 경우도 포함하세요.
- `detailContent` 값은 SQL 문자열이 깨지지 않도록 안전하게 작성하세요.
  - 여러 문장 텍스트 위주
  - 줄바꿈이 필요하면 안전한 문자열 형식 사용

## 산출물
- `Campaign.java` (수정)
- `data.sql` (수정)

---

# Phase 18. 캠페인 상세 조회 Application 서비스

## 목표
확장된 `Campaign` 도메인을 기반으로 상세 조회 유스케이스를 구현합니다.

## 유스케이스: 캠페인 상세 조회

### 입력
- `campaignId`: `Long`

### 처리
1. Campaign 을 조회합니다.
2. 없으면 `CampaignNotFoundException` 을 던집니다.
3. 조회 결과를 `CampaignDetail` DTO 로 변환합니다.

## 출력 DTO: `CampaignDetail`
필드는 아래와 같습니다.

- `id`
- `sourceType`
- `sourceDisplayName`
- `title`
- `description`
- `detailContent`
- `thumbnailUrl`
- `originalUrl`
- `category`
- `categoryDisplayName`
- `status`
- `statusDisplayName`
- `recruitCount`
- `applyStartDate`
- `applyEndDate`
- `announcementDate`
- `reward`
- `mission`
- `address`
- `keywords`: `List<String>`
- `createdAt`
- `updatedAt`

## 커스텀 예외
- `CampaignNotFoundException`
  - 존재하지 않는 캠페인 ID

## 설계 원칙
- 기존 `CampaignService` 에 `getDetail(Long id)` 메서드를 추가해도 되고,
  별도 UseCase 인터페이스를 사용해도 됩니다.
- `@Transactional(readOnly = true)` 적용
- `CampaignSummary` 와 `CampaignDetail` 은 반드시 분리합니다.
- `keywords` 는 도메인의 `getKeywordList()` 를 사용해 변환합니다.
- `keywords` 는 DTO 에서 절대 null 이 되지 않도록 하세요. 빈 경우 `[]`

## 산출물
- `CampaignDetail.java`
- `CampaignService.java` (수정)
- `CampaignNotFoundException.java`

---

# Phase 19. 캠페인 상세 조회 API

## 목표
상세 조회 서비스를 REST API 로 노출합니다.

## API 스펙
- `GET /api/v1/campaigns/{id}`
- 인증 불필요 (`permitAll`)

## Path Variable
- `id`: `Long`

## 성공 응답 예시
```json
{
  "id": 1,
  "sourceType": "REVU",
  "sourceDisplayName": "레뷰",
  "title": "[서울 강남] 이탈리안 레스토랑 체험단 모집",
  "description": "역삼역 근처 이탈리안 레스토랑에서 2인 식사를 제공합니다.",
  "detailContent": "역삼역 3번출구 도보 5분 거리에 위치한 프리미엄 이탈리안 레스토랑입니다.\n정통 나폴리 피자와 수제 파스타를 전문으로 합니다.",
  "thumbnailUrl": "https://placehold.co/600x400",
  "originalUrl": "https://revu.net/campaign/12345",
  "category": "FOOD",
  "categoryDisplayName": "맛집",
  "status": "RECRUITING",
  "statusDisplayName": "모집중",
  "recruitCount": 5,
  "applyStartDate": "2026-03-10",
  "applyEndDate": "2026-03-25",
  "announcementDate": "2026-03-27",
  "reward": "2인 식사권 (5만원 상당)",
  "mission": "블로그 리뷰 작성 (사진 10장 이상)",
  "address": "서울 강남구 역삼동 123-45",
  "keywords": ["강남맛집", "이탈리안", "파스타"],
  "createdAt": "2026-03-14T10:00:00",
  "updatedAt": "2026-03-14T10:00:00"
}
```

## 응답 규칙
- `keywords` 는 항상 배열로 응답합니다.
- 상세 필드가 없으면 해당 필드는 `null` 로 응답합니다.
- 날짜/시간은 ISO 8601 형식으로 직렬화합니다.

## 에러 응답 규칙
- 존재하지 않는 캠페인 ID
  - `404 Not Found`
  - `CAMPAIGN_NOT_FOUND`
- ID 형식 오류
  - `400 Bad Request`
  - `INVALID_PARAMETER`

## interfaces DTO
- `CampaignDetailResponse.java`
  - `CampaignDetailResponse.from(CampaignDetail)` 정적 팩토리 사용

## GlobalExceptionHandler 확장
- `CampaignNotFoundException` → `404`
- `MethodArgumentTypeMismatchException` → `400`

## Spring Security 변경
다음 GET API 만 permitAll 로 유지/추가하세요.

- `GET /api/v1/campaigns`
- `GET /api/v1/campaigns/filters`
- `GET /api/v1/campaigns/{id}`

주의:
- 추후 보호가 필요한 `/api/v1/campaigns/**` API 가 생길 수 있으므로,
  이번 단계에서는 무조건 `/api/v1/campaigns/**` 전체를 permitAll 로 풀지 마세요.

## 산출물
- `CampaignDetailResponse.java`
- `CampaignController.java` (수정)
- `GlobalExceptionHandler.java` (수정)
- `SecurityConfig.java` (수정)

---

# Phase 20. React 캠페인 상세 페이지

## 목표
상세 조회 API 를 호출하는 상세 페이지를 구현하고, 카드 클릭 동작을 내부 라우팅으로 변경합니다.

## 기술
- React + TypeScript + Vite
- react-router-dom
- CSS Modules

## 라우팅
- `/` → 캠페인 목록
- `/campaigns/:id` → 캠페인 상세
- `/login` → 로그인
- `/signup` → 회원가입
- `/signup/complete` → 가입 완료

## 카드 클릭 동작 변경
- 변경 전: `originalUrl` 새 탭 이동
- 변경 후: `/campaigns/{id}` 내부 라우팅 이동
- `Link` 또는 동등한 라우팅 방식 사용
- 가능하면 현재 목록 위치를 `state.from` 으로 함께 전달하세요.
  - 예: 현재 목록 URL query 포함

## 상세 페이지 구성

### 1. 뒤로가기 네비게이션
- "← 목록으로 돌아가기"
- 우선순위:
  1. `location.state.from`
  2. `navigate(-1)`
  3. `/`

### 2. 상단 메타 정보
- 썸네일
- 제목
- 뱃지
  - 소스명
  - 카테고리
  - 상태
- 모집 인원
- 신청 기간
- 발표일
- 모집중이고 마감일이 있으면 D-day 표시

### 3. 상세 정보 영역
- `reward` 가 있으면 표시
- `mission` 이 있으면 표시
- `address` 가 있으면 표시
- `keywords` 가 있으면 칩 형태로 표시
- `detailContent` 가 있으면 표시
- null 인 값은 해당 섹션 자체를 숨깁니다.
- `detailContent` 는 텍스트 렌더링만 허용
  - `dangerouslySetInnerHTML` 금지
  - CSS `white-space: pre-line` 또는 동등한 방식으로 줄바꿈 보존

### 4. 안내 문구
고정 텍스트:
- "신청은 원본 사이트에서 진행됩니다."
- "본 플랫폼은 정보 제공 목적이며, 실제 체험단 운영은 해당 사이트에서 관리합니다."

### 5. CTA 버튼
- 모집중: `"원본 사이트에서 신청하기"`
- 모집마감 / 종료 / 발표완료: `"원본 사이트 보기"`
- 스타일은 상태에 따라 다르게 보일 수 있지만,
  링크는 항상 `originalUrl` 로 동작합니다.
- 새 탭 이동 시 반드시:
  - `target="_blank"`
  - `rel="noopener noreferrer"`

### 6. 상태 처리
- 로딩: 상세 전용 스켈레톤 UI
- 404 또는 잘못된 id 형식: not found 상태
- 네트워크 에러: 재시도 가능 상태

## 잘못된 id 파라미터 처리
- `useParams()` 로 받은 `id` 가 숫자 형식이 아니면,
  프론트에서는 API 호출 전에 바로 not found 상태로 처리해도 됩니다.
- 단, 백엔드 API 는 여전히 `400 INVALID_PARAMETER` 를 처리해야 합니다.

## 반응형 디자인
- 데스크톱: 2컬럼
  - 좌측 썸네일
  - 우측 메타 정보
- 모바일: 1컬럼
  - 썸네일 상단
  - 메타 정보 하단
- CTA 버튼은 모바일에서 하단 sticky 형태 허용

## 프로젝트 구조
```text
/frontend
  /src
    /pages
      CampaignDetailPage.tsx
    /components
      CampaignCard.tsx
      CampaignDetailHeader.tsx
      CampaignDetailBody.tsx
      KeywordChip.tsx
      StatusBadge.tsx
      DetailSkeleton.tsx
      NotFoundState.tsx
    /api
      campaignApi.ts
    /hooks
      useCampaignDetail.ts
    /types
      campaign.ts
    /utils
      dateUtils.ts
    /styles
      CampaignDetailPage.module.css
      CampaignDetailHeader.module.css
      CampaignDetailBody.module.css
      StatusBadge.module.css
      KeywordChip.module.css
    App.tsx
```

## useCampaignDetail 훅 책임
- route param 으로 받은 id 기반 조회
- 로딩 상태 관리
- 404 상태 관리
- 네트워크 에러 상태 관리
- retry 함수 제공

반환 예시:
```typescript
function useCampaignDetail(id: string | undefined) {
  return {
    campaign,
    isLoading,
    isNotFound,
    error,
    retry,
  };
}
```

## dateUtils 설계
아래 유틸리티를 제공합니다.

- `getDdayText(endDate: string | null): string | null`
  - 미래 → `D-3`
  - 오늘 → `D-Day`
  - 과거 → `마감`
  - null → `null`
- `formatDate(date: string | null): string | null`
  - `2026.03.10` 형식
- `formatPeriod(start: string | null, end: string | null): string | null`
  - 둘 다 있으면 `2026.03.10 ~ 2026.03.25`
  - 하나만 있으면 있는 값 기준으로 자연스럽게 표시하거나 `null`

## 추가 요구사항
- 썸네일이 없거나 로드 실패 시 기본 placeholder 처리
- 목록 카드도 접근성을 고려해 `Link` 기반으로 구현
- 상세 페이지 뱃지/키워드 컴포넌트는 재사용 가능하게 분리
- `campaign.ts` 타입 정의는 목록용 / 상세용을 명확히 구분

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의
- `dateUtils.ts`

---

# Phase 21. 캠페인 상세 테스트

## 목표
캠페인 상세 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 도메인 단위 테스트
- `Campaign`
  - 새 필드 포함 생성 확인
  - `getKeywordList()` 테스트
    - `"강남맛집,이탈리안,파스타"` → `["강남맛집", "이탈리안", "파스타"]`
    - null → 빈 리스트
    - 빈 문자열 → 빈 리스트
    - `" 강남맛집 ,  이탈리안 , "` → trim + 빈 값 제거 확인
  - `update()` 확장 필드 갱신 확인

### 2. Application 서비스 테스트
- 정상 상세 조회
- 존재하지 않는 ID → `CampaignNotFoundException`
- nullable 필드가 null 인 데이터 매핑
- `keywords` → 배열 변환 확인
- Repository Mock 사용

### 3. API 통합 테스트
- `GET /api/v1/campaigns/{id}`
  - 정상 조회 → `200`
  - 존재하지 않는 ID → `404` + `CAMPAIGN_NOT_FOUND`
  - 문자열 ID → `400` + `INVALID_PARAMETER`
  - nullable 필드 null 직렬화 확인
  - `keywords` 빈 경우 빈 배열 또는 일관된 응답 구조 확인
  - 인증 없이 접근 가능
- H2 + 시드 데이터 기반

### 4. 프론트엔드 단위 테스트
- `useCampaignDetail`
  - 정상 데이터 로드
  - 404 응답 → `isNotFound = true`
  - 네트워크 에러 → `error`
  - retry 동작
  - 잘못된 id 형식 처리
- `dateUtils`
  - D-day 계산
  - 날짜 포맷
  - 기간 포맷

## 테스트 설계 원칙
- 훅 테스트에서는 라우터 환경을 포함하세요.
- 404 와 일반 네트워크 에러를 분리 검증하세요.
- `dateUtils` 는 오늘 / 미래 / 과거 케이스를 모두 검증하세요.

## 테스트 도구
- Backend: JUnit 5, Mockito, AssertJ
- Frontend: Vitest, React Testing Library

## 산출물
- `CampaignDetailTest.java`
- `CampaignServiceDetailTest.java`
- `CampaignDetailControllerIntegrationTest.java`
- `useCampaignDetail.test.ts`
- `dateUtils.test.ts`

---

# 전체 Phase 누적 현황

| Phase | 기능 | BC |
|-------|------|----|
| 1~6 | 회원가입 | Member |
| 7~11 | 로그인 / JWT | Member |
| 12~16 | 캠페인 목록 조회 | Campaign |
| 17~21 | 캠페인 상세 페이지 | Campaign |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"체험단 상세 페이지 프로젝트 명세를 완벽히 이해했습니다. Phase 17 진행을 시작할까요?"**