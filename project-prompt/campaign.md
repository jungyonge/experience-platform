당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 체험단 목록 조회 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT 기능(Phase 1~11)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 12~16)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"체험단 목록 조회 프로젝트 명세를 완벽히 이해했습니다. Phase 12 진행을 시작할까요?"**

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

Phase 1~11에서 다음 기능이 이미 구현되어 있습니다.

## 기존 구현 요약
- Member Bounded Context
  - 회원가입
  - 로그인
  - JWT 인증
- React 인증 상태 관리
  - `AuthContext`
  - `AuthProvider`
  - `ProtectedRoute`
- Spring Security + JWT 필터
- 기존 홈페이지(`/`)는 간단한 인사 화면이었음
  - 이번 Phase에서 **캠페인 목록 메인 홈 화면**으로 교체

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
- ex-design폴더 와 shad cn의 컴포넌트 활용

---

# 아키텍처 원칙

1. **실용적 DDD**를 적용합니다.
2. Bounded Context는 기존 `Member`에 더해 신규 `Campaign`를 추가합니다.
3. `Campaign`은 `Campaign` Bounded Context의 Aggregate Root입니다.
4. Repository 인터페이스는 `domain`, 구현체는 `infrastructure`에 둡니다.
5. `interfaces` DTO와 `application` DTO는 분리합니다.
6. 이번 프로젝트는 실용적 DDD를 적용하므로:
   - domain 엔티티에 JPA 어노테이션 사용을 허용합니다.
   - domain repository 시그니처에서 `Page` / `Pageable` 사용도 허용합니다.
7. QueryDSL은 사용하지 않고, **검색은 JPA Specification만 사용**합니다.
8. 검색은 읽기 전용 기능이므로 Application Service에는 `@Transactional(readOnly = true)`를 적용합니다.
9. 캠페인 목록 조회 API는 **비로그인도 사용 가능**해야 합니다.
10. 기존 JWT / Security 구조는 유지하되, 체험단 목록 조회 관련 GET API만 `permitAll`에 추가합니다.

---

# 이번 범위에서 추가

- **Campaign Bounded Context** 신규 생성
- 체험단 목록 조회 API
  - 검색
  - 필터
  - 정렬
  - 페이징
- 필터 옵션 API
- 메인 홈페이지(`/`)를 체험단 목록 페이지로 교체
- 공통 Header 확장
- 개발용 시드 데이터(`data.sql`)

---

# 이번 범위에서 제외

구현하지 마세요.

- 크롤링 / 스크래핑
- 체험단 상세 페이지
- 찜 / 북마크
- 관리자 캠페인 CRUD
- Elasticsearch
- Redis
- QueryDSL

---

# 데이터 유입 전략

## 이번 Phase
- H2 시드 데이터(`data.sql`)로 캠페인 데이터를 50건 이상 삽입합니다.
- 다양한 소스와 카테고리를 포함합니다.
- 이번 목록 화면의 주 사용 상태는 `RECRUITING`, `CLOSED` 입니다.
- `ANNOUNCED`, `COMPLETED` 는 **추후 확장용 enum 값으로는 유지**하되, 이번 시드 데이터에는 넣지 않거나 최소화하여 목록 UX를 단순하게 유지합니다.

## 추후 Phase
- 크롤링 모듈을 추가하여 실제 데이터 수집 자동화를 구현합니다.

## 도메인 확장 고려
- 추후 크롤링 확장을 고려하여 아래 필드를 미리 포함합니다.
  - `sourceType`
  - `originalId`
  - `originalUrl`

---

# 공통 UX / 검색 정책

1. 메인 홈페이지(`/`)는 체험단 목록 페이지입니다.
2. 비로그인도 전체 조회 기능을 사용할 수 있습니다.
3. 카드 클릭 시 체험단 상세 페이지가 아니라 **원본 사이트(`originalUrl`)를 새 탭으로 엽니다.**
4. 소스 필터와 카테고리 필터는 **복수 선택**입니다.
5. 상태 필터는 **단일 선택**입니다.
6. 검색은 **캠페인 제목 기준 contains 검색**입니다.
   - `lower(title) like lower(%keyword%)` 형태의 **대소문자 무시 검색**으로 구현합니다.
7. `keyword`는 trim 후 빈 문자열이면 검색 조건에서 제외합니다.
8. URL = 검색 상태를 유지합니다.
9. `page`는 **API와 URL 모두 0-based** 로 통일합니다.
   - UI에 표시되는 페이지 번호만 1-based로 보여주세요.
10. 필터/검색/정렬이 바뀌면 `page`는 0으로 초기화합니다.
11. 검색어 입력은 300ms 디바운스를 적용합니다.

---

# 메인 홈페이지 레이아웃 기준

- 상단 공통 Header
  - 좌측: 로고/플랫폼명
  - 중앙: 검색바
  - 우측:
    - 비로그인: 로그인 / 회원가입
    - 로그인: `{닉네임}님` + 로그아웃

- 필터 영역
  - 소스
  - 카테고리
  - 상태
  - 정렬

- 캠페인 카드 그리드
- 페이지네이션
- 총 건수 표시

## Header 검색바 동작 규칙
- Header는 모든 페이지에서 공통으로 표시합니다.
- Home(`/`)에서는 검색어가 URL 상태와 동기화됩니다.
- Home이 아닌 다른 페이지에서 Header 검색을 실행하면,
  - `/`로 이동하면서 `keyword` 쿼리 파라미터를 붙여 캠페인 목록 검색 결과를 보여주세요.

---

# Phase 12. Campaign 도메인 모델 설계

## 목표
새로운 Bounded Context인 `Campaign` 을 추가하고, 체험단 캠페인 도메인을 설계합니다.

## 패키지 구조

```text
com.example.experienceplatform
  ├── member/
  │   ├── domain/
  │   ├── application/
  │   ├── infrastructure/
  │   └── interfaces/
  └── campaign/
      ├── domain/
      ├── application/
      ├── infrastructure/
      └── interfaces/
```

## Campaign 엔티티

`Campaign`은 Aggregate Root 입니다.

### 필드
- `id`: `Long` (PK, auto increment)
- `sourceType`: `SourceType` (NOT NULL)
- `originalId`: `String` (NOT NULL)
- `title`: `String` (NOT NULL, 최대 200자)
- `description`: `String` (nullable, 최대 1000자)
- `thumbnailUrl`: `String` (nullable)
- `originalUrl`: `String` (NOT NULL)
- `category`: `CampaignCategory` (NOT NULL)
- `status`: `CampaignStatus` (NOT NULL)
- `recruitCount`: `Integer` (nullable)
- `applyStartDate`: `LocalDate` (nullable)
- `applyEndDate`: `LocalDate` (nullable)
- `announcementDate`: `LocalDate` (nullable)
- `createdAt`: `LocalDateTime` (NOT NULL)
- `updatedAt`: `LocalDateTime` (NOT NULL)

## 도메인 규칙
- `sourceType + originalId` 조합은 시스템 내에서 유일해야 합니다.
- DB Unique 제약조건 이름은 명시적으로 지정해주세요.
  - 예: `uq_campaign_source_original_id`
- `createdAt`, `updatedAt`은 엔티티 생명주기 콜백(`@PrePersist`, `@PreUpdate`) 또는 동등한 방식으로 자동 관리해주세요.
- `Campaign`에는 크롤링 재수집 시 외부 데이터를 반영하는 `update(...)` 메서드를 둡니다.
- `title`, `originalUrl`, `sourceType`, `originalId`, `category`, `status`는 필수값입니다.

## SourceType
아래 enum으로 설계하고, 각 값은 `displayName` 을 가집니다.

- `REVU` → `레뷰`
- `MBLE` → `미블`
- `GANGNAM` → `강남맛집`

## CampaignCategory
각 값은 `displayName` 을 가집니다.

- `FOOD` → `맛집`
- `BEAUTY` → `뷰티`
- `TRAVEL` → `여행/숙박`
- `LIFE` → `생활/가전`
- `DIGITAL` → `IT/디지털`
- `CULTURE` → `문화/도서`
- `ETC` → `기타`

## CampaignStatus
각 값은 `displayName` 을 가집니다.

- `RECRUITING` → `모집중`
- `CLOSED` → `모집마감`
- `ANNOUNCED` → `발표완료`
- `COMPLETED` → `종료`

주의:
- 이번 메인 목록 화면에서는 주로 `RECRUITING`, `CLOSED`만 사용합니다.
- `ANNOUNCED`, `COMPLETED`는 추후 상세/아카이브 확장용입니다.

## CampaignSearchCondition
검색 조건용 도메인 객체를 만듭니다.

### 필드
- `keyword`: `String`
- `sourceTypes`: `Set<SourceType>`
- `categories`: `Set<CampaignCategory>`
- `status`: `CampaignStatus`

## CampaignRepository
domain 레이어 인터페이스로 설계합니다.

```java
public interface CampaignRepository {
    Campaign save(Campaign campaign);
    Optional<Campaign> findById(Long id);
    Optional<Campaign> findBySourceTypeAndOriginalId(SourceType sourceType, String originalId);
    Page<Campaign> searchByCondition(CampaignSearchCondition condition, Pageable pageable);
}
```

## Repository 구현 구조
아래 3단계 구조를 따르세요.

- `CampaignRepository` : domain 인터페이스
- `CampaignSpringDataJpaRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign>` : Spring Data JPA 인터페이스
- `CampaignJpaRepository implements CampaignRepository` : domain repository 구현체

## CampaignSpecification
- infrastructure 레이어에 위치
- JPA Specification으로 동적 조건을 조합합니다.
- 규칙:
  - `keyword` → `title contains` (case-insensitive)
  - `sourceTypes` → `IN`
  - `categories` → `IN`
  - `status` → `=`
  - null / blank / empty 조건은 무시합니다.

## 산출물
- `Campaign.java`
- `SourceType.java`
- `CampaignCategory.java`
- `CampaignStatus.java`
- `CampaignSearchCondition.java`
- `CampaignRepository.java`
- `CampaignSpringDataJpaRepository.java`
- `CampaignJpaRepository.java`
- `CampaignSpecification.java`

---

# Phase 13. 캠페인 목록 조회 Application 서비스 + 시드 데이터

## 목표
캠페인 목록 조회 유스케이스를 구현하고 개발용 시드 데이터를 추가합니다.

## 유스케이스: 캠페인 목록 조회

### 입력: `CampaignSearchCommand`
- `keyword`: `String` nullable
- `sourceTypes`: `Set<String>` nullable
- `categories`: `Set<String>` nullable
- `status`: `String` nullable
- `page`: `int`
- `size`: `int`
- `sort`: `String`

### 처리
1. 문자열 필터를 enum 으로 변환합니다.
2. 잘못된 enum 값은 예외를 던지지 말고 **무시**합니다.
3. `CampaignSearchCondition` 을 생성합니다.
4. 페이징 + 정렬 조건을 생성합니다.
5. Repository 검색을 호출합니다.
6. 결과를 `CampaignSummary` 목록으로 변환합니다.

## 출력: `CampaignListInfo`
- `campaigns`: `List<CampaignSummary>`
- `totalCount`: `long`
- `totalPages`: `int`
- `currentPage`: `int`
- `hasNext`: `boolean`

## CampaignSummary
- `id`
- `sourceType`
- `sourceDisplayName`
- `title`
- `thumbnailUrl`
- `originalUrl`
- `category`
- `categoryDisplayName`
- `status`
- `statusDisplayName`
- `recruitCount`
- `applyEndDate`

프론트엔드가 enum → 한글명 변환을 하지 않도록, displayName 필드를 포함하세요.

## 정렬 옵션
- `latest` → `createdAt DESC`
- `deadline` → `applyEndDate ASC`
- `popular` → `recruitCount DESC`

정렬 규칙:
- `deadline`, `popular` 정렬은 가능하면 null 값을 뒤로 보내세요.
- Spring Data / JPA 환경에서 null handling 지원이 제한되면, 동작이 최대한 일관되도록 구현하세요.
- 시드 데이터는 `applyEndDate`, `recruitCount` 값을 대부분 채워 정렬 테스트가 안정적으로 동작하도록 만드세요.

## 설계 원칙
- 읽기 전용 유스케이스이므로 `@Transactional(readOnly = true)` 적용
- 잘못된 enum 값은 관대하게 무시
- 정렬 값은 API 레이어에서 먼저 검증하므로 Application Service는 유효한 sort 값을 받는다고 가정 가능
- `keyword`는 trim 후 빈 문자열이면 null 로 간주

## 시드 데이터 (`data.sql`)
- 50건 이상 작성하세요. 가능하면 60건 정도로 구성하세요.
- 다양한 소스와 카테고리를 고르게 포함하세요.
- 이번 목록 화면 기준으로 상태는 주로 `RECRUITING`, `CLOSED`를 사용하세요.
- `thumbnailUrl`은 placeholder URL을 사용하세요.
  - URL 인코딩 이슈를 피하기 위해 query text는 ASCII 위주로 구성해도 됩니다.
- `originalUrl`은 `#` 가 아니라, 실제 사이트처럼 보이는 더미 URL 형식으로 작성하세요.
  - 예:
    - `https://revu.net/campaign/1001`
    - `https://mble.xyz/campaign/2001`
- `createdAt`, `updatedAt` 값도 함께 넣어주세요.
- `originalId`는 sourceType 내에서 중복되지 않도록 작성하세요.

## 설정 추가
`application-local.yml`에 아래를 포함하세요.

- `spring.jpa.defer-datasource-initialization: true`

필요하면 기존 로컬 설정과 충돌하지 않도록 함께 정리하세요.

## 산출물
- `CampaignSearchCommand.java`
- `CampaignSummary.java`
- `CampaignListInfo.java`
- `CampaignService.java`
  - 또는 `SearchCampaignUseCase` + `SearchCampaignService`
- `data.sql`
- `application-local.yml` (수정)

---

# Phase 14. 캠페인 목록 조회 API

## 목표
캠페인 조회 서비스를 REST API로 노출합니다.

## API 1. 캠페인 목록 조회
- `GET /api/v1/campaigns`
- 인증 불필요 (`permitAll`)

## Query Parameters
- `keyword`: `String`
- `sourceTypes`: `String` (콤마 구분)
- `categories`: `String` (콤마 구분)
- `status`: `String`
- `page`: `int`, 기본값 `0`
- `size`: `int`, 기본값 `12`, 최대 `50`
- `sort`: `String`, 기본값 `latest`

## 요청 예시
```text
GET /api/v1/campaigns?keyword=맛집&sourceTypes=REVU,MBLE&categories=FOOD&status=RECRUITING&page=0&size=12&sort=latest
```

## 성공 응답
```json
{
  "campaigns": [
    {
      "id": 1,
      "sourceType": "REVU",
      "sourceDisplayName": "레뷰",
      "title": "[서울 강남] 이탈리안 레스토랑 체험단 모집",
      "thumbnailUrl": "https://placehold.co/300x200?text=Campaign+1",
      "originalUrl": "https://revu.net/campaign/1001",
      "category": "FOOD",
      "categoryDisplayName": "맛집",
      "status": "RECRUITING",
      "statusDisplayName": "모집중",
      "recruitCount": 5,
      "applyEndDate": "2026-03-25"
    }
  ],
  "totalCount": 127,
  "totalPages": 11,
  "currentPage": 0,
  "hasNext": true
}
```

## 에러 처리 규칙
- 잘못된 `sort` 값 → `400`, code: `INVALID_SORT_VALUE`
- `size > 50` → `400`, code: `INVALID_PAGE_SIZE`
- `page < 0` → `400`, code: `INVALID_PAGE_NUMBER`
- 잘못된 `sourceType`, `category`, `status` 값 → **무시**, 에러 아님

## CampaignSearchRequest
- Query Parameter 를 받는 interfaces DTO 입니다.
- `@ModelAttribute` 로 바인딩하세요.
- `toCommand()` 메서드에서:
  - `sourceTypes`, `categories` 의 콤마 구분 문자열을 `Set<String>` 으로 변환
  - trim 처리
  - 빈 값 제거
- `page`, `size`, `sort` 검증을 포함하세요.
- `size` 최대값은 50
- `page` 최소값은 0
- `sort` 허용값은 `latest`, `deadline`, `popular`

## 응답 DTO
- `CampaignListResponse.java`
- `CampaignItemResponse.java`

## API 2. 필터 옵션 조회
- `GET /api/v1/campaigns/filters`
- 인증 불필요 (`permitAll`)

## 필터 옵션 응답 규칙
필터 옵션은 백엔드 enum 기준으로 내려주세요.

단, 이번 목록 화면에서는 상태 옵션을 아래 2개만 내려주세요.
- `RECRUITING`
- `CLOSED`

즉, `ANNOUNCED`, `COMPLETED` 는 enum 에는 존재하지만, 현재 메인 목록 필터 옵션 응답에는 포함하지 않습니다.

## 필터 옵션 응답 예시
```json
{
  "sourceTypes": [
    { "code": "REVU", "name": "레뷰" },
    { "code": "MBLE", "name": "미블" },
    { "code": "GANGNAM", "name": "강남맛집" }
  ],
  "categories": [
    { "code": "FOOD", "name": "맛집" },
    { "code": "BEAUTY", "name": "뷰티" },
    { "code": "TRAVEL", "name": "여행/숙박" },
    { "code": "LIFE", "name": "생활/가전" },
    { "code": "DIGITAL", "name": "IT/디지털" },
    { "code": "CULTURE", "name": "문화/도서" },
    { "code": "ETC", "name": "기타" }
  ],
  "statuses": [
    { "code": "RECRUITING", "name": "모집중" },
    { "code": "CLOSED", "name": "모집마감" }
  ],
  "sortOptions": [
    { "code": "latest", "name": "최신순" },
    { "code": "deadline", "name": "마감임박순" },
    { "code": "popular", "name": "모집인원순" }
  ]
}
```

## Spring Security 변경
아래 경로를 `permitAll`에 추가하세요.

- `GET /api/v1/campaigns`
- `GET /api/v1/campaigns/filters`

기존 로그인/JWT 설정은 유지하세요.

## 산출물
- `CampaignController.java`
- `CampaignSearchRequest.java`
- `CampaignListResponse.java`
- `CampaignItemResponse.java`
- `FilterOptionResponse.java`
- `SecurityConfig.java` (수정)
- `GlobalExceptionHandler.java` (필요 시 확장)

---

# Phase 15. React 메인 홈페이지 (캠페인 목록)

## 목표
기존 홈페이지(`/`)를 캠페인 목록 메인 화면으로 교체합니다.

## 기술
- React + TypeScript + Vite
- `useState`, `useEffect`
- `useSearchParams`
- 기존 `apiClient` 사용
- CSS Modules

## 핵심 UX 원칙
- **URL = 검색 상태**
- 비로그인도 전체 조회 가능
- 브라우저 뒤로가기/앞으로가기로 이전 검색 상태 복원 가능
- URL 공유 시 동일한 검색 결과 표시

## Header
기존 `Header.tsx` 를 확장합니다.

### 구성
- 좌측: 로고/플랫폼명
- 중앙: 검색바
- 우측:
  - 비로그인 → 로그인 / 회원가입
  - 로그인 → `{닉네임}님` + 로그아웃

### 동작 규칙
- Header 는 모든 페이지에 공통 배치
- Home(`/`)에서는 `keyword`가 URL 과 동기화
- Home 이 아닌 페이지에서 검색 실행 시 `/`로 이동하면서 `keyword` 쿼리를 붙입니다.

## 메인 페이지 구성

### 1. 필터 영역
- 소스 필터
  - 칩/토글 버튼
  - 복수 선택
  - "전체" 클릭 시 초기화
- 카테고리 필터
  - 칩/토글 버튼
  - 복수 선택
- 상태 필터
  - 단일 선택
  - 전체 / 모집중 / 모집마감
- 정렬
  - 드롭다운
  - 최신순 / 마감임박순 / 모집인원순
- 필터 변경 시 자동 검색
- 필터 변경 시 `page` 는 0으로 초기화
- 필터 옵션은 `/api/v1/campaigns/filters` 에서 가져옵니다.

### 2. 캠페인 카드 그리드
- 반응형:
  - 모바일: 1열
  - 태블릿: 2열
  - 데스크톱: 3~4열
- 카드 구성:
  - 썸네일
  - 소스 뱃지
  - 제목
  - 카테고리 / 상태
  - 마감일 / 모집인원
- 제목은 2줄 초과 시 말줄임
- 모집중은 강조 스타일
- 모집마감은 회색 스타일
- 카드 클릭은 `onClick` 만 사용하지 말고, 접근성을 위해 anchor 태그 또는 동등한 구조를 사용하세요.
- 새 탭 이동 시 `target="_blank"` + `rel="noopener noreferrer"` 를 사용하세요.
- 썸네일이 없거나 로드 실패 시 기본 placeholder 처리를 포함하세요.

### 3. 페이지네이션
- 총 건수 표시
- 이전/다음 버튼
- 최대 5개 페이지 번호 표시
- 페이지 변경 시 스크롤 상단 이동
- UI 라벨은 1-based 로 표시하되, 내부 상태와 API 요청은 0-based 를 유지하세요.

### 4. 상태 처리
- 로딩: 스켈레톤 UI 또는 로딩 스피너
- 빈 결과: 안내 문구 + 필터 초기화 버튼
- 에러: 안내 문구 + 재시도 버튼

## 프로젝트 구조
```text
/frontend
  /src
    /pages
      HomePage.tsx
      LoginPage.tsx
      SignupPage.tsx
      SignupCompletePage.tsx
    /components
      Header.tsx
      CampaignCard.tsx
      CampaignGrid.tsx
      FilterBar.tsx
      FilterChip.tsx
      Pagination.tsx
      EmptyState.tsx
      LoadingSkeleton.tsx
    /api
      campaignApi.ts
    /hooks
      useCampaignSearch.ts
      useCampaignFilters.ts
    /types
      campaign.ts
    /constants
      campaign.ts
    /styles
      HomePage.module.css
      Header.module.css
      CampaignCard.module.css
      CampaignGrid.module.css
      FilterBar.module.css
      Pagination.module.css
    App.tsx
```

## useCampaignSearch 훅 책임
- URL 의 `searchParams` 에서 초기 상태 복원
- 검색/필터/정렬/페이지 변경 시 URL 업데이트
- URL 변경에 따라 API 호출
- 검색어 입력은 300ms 디바운스
- API 응답 상태 관리
  - loading
  - error
  - empty
- `resetFilters()` 제공

## URL 파라미터 규칙
- `keyword`: 문자열
- `sourceTypes`: 콤마 구분 (`REVU,MBLE`)
- `categories`: 콤마 구분 (`FOOD,BEAUTY`)
- `status`: 단일 문자열
- `page`: 0-based 정수
- `size`: 기본 12
- `sort`: `latest | deadline | popular`

URL 예시:
```text
/?keyword=맛집&sourceTypes=REVU,MBLE&categories=FOOD&status=RECRUITING&page=0&size=12&sort=latest
```

## useCampaignFilters 훅 책임
- 페이지 최초 로드시 1회 호출
- 필터 옵션 캐시
- 불필요한 재호출 방지

## 추가 요구사항
- 검색어 디바운스 300ms 적용
- CSS 는 깔끔하고 현대적인 디자인
- 카드에 미세한 그림자와 hover 효과
- App.tsx 레벨에서 Header 공통 배치
- 비로그인 상태에서도 campaign API 호출이 정상 동작해야 함
- 기존 auth 흐름과 충돌하지 않도록 `apiClient` 의 인증 헤더 부착은 선택적으로 동작해야 함

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의

---

# Phase 16. 캠페인 목록 테스트

## 목표
캠페인 목록 조회 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 도메인 단위 테스트
- `Campaign`
  - 생성 시 `createdAt`, `updatedAt` 자동 설정 확인
  - `update()` 메서드 필드 갱신 확인
- `SourceType`
  - `displayName` 매핑
- `CampaignCategory`
  - `displayName` 매핑
- `CampaignSearchCondition`
  - 빈 조건 생성 가능
  - 각 필드 설정 확인

### 2. JPA Specification 테스트
- `@DataJpaTest`
- `CampaignSpecification` 동적 쿼리 테스트
  - 키워드 검색
  - 소스 필터
  - 카테고리 필터
  - 상태 필터
  - 복합 조건
  - 조건 없을 때 전체 조회
  - 페이징 동작
  - 정렬 동작

### 3. Application 서비스 테스트
- 기본 조건 목록 조회
- 키워드 검색
- 필터 조합 검색
- 잘못된 enum 값 무시
- 페이징/정렬 전달 확인
- Repository Mock 사용

### 4. API 통합 테스트
- `GET /api/v1/campaigns`
  - 기본 조회 → `200`
  - 키워드 검색
  - 소스/카테고리/상태 필터 조합
  - 잘못된 sort 값 → `400`
  - size 초과 → `400`
  - 인증 없이 접근 가능
- `GET /api/v1/campaigns/filters`
  - 전체 옵션 반환
  - 인증 없이 접근 가능
- H2 + 시드 데이터 기반

### 5. 프론트엔드 단위 테스트
- `useCampaignSearch`
  - 초기 상태
  - 필터 토글
  - 페이지 변경
  - URL 동기화
  - 검색어 디바운스
- `useCampaignFilters`
  - 필터 옵션 로드
  - 캐시 동작 확인

## 테스트 설계 원칙
- 훅 테스트에서는 `MemoryRouter` 또는 동등한 라우터 환경을 사용하세요.
- 검색어 디바운스 테스트는 fake timer 를 사용해 안정적으로 검증하세요.
- URL 기반 상태 훅은 searchParams 변경에 따른 재호출까지 검증하세요.

## 테스트 도구
- Backend: JUnit 5, Mockito, AssertJ
- Frontend: Vitest, React Testing Library

## 산출물
- `CampaignTest.java`
- `CampaignSpecificationTest.java`
- `CampaignServiceTest.java`
- `CampaignControllerIntegrationTest.java`
- `useCampaignSearch.test.ts`
- `useCampaignFilters.test.ts`

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"체험단 목록 조회 프로젝트 명세를 완벽히 이해했습니다. Phase 12 진행을 시작할까요?"**