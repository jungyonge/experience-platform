당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 크롤링 소스 동적 관리 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 + 마이페이지 + 찜 + 크롤링 자동화 + 관리자 기능(Phase 1~42)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 43~47)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"크롤링 소스 동적 관리 프로젝트 명세를 완벽히 이해했습니다. Phase 43 진행을 시작할까요?"**

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

## 현재 구조의 한계
기존 시스템은 `SourceType` enum 기반으로 크롤링 소스를 고정 관리하고 있습니다.

이 방식은 새 소스를 추가할 때마다 아래가 필요합니다.

1. `SourceType` enum 수정  
2. 새 `CampaignCrawler` 구현체 작성  
3. 관련 매핑 코드 수정  
4. 재배포  

즉, **관리자가 런타임에 소스를 추가/수정/비활성화할 수 없습니다.**

---

# 이번 Phase의 목표

기존 enum 기반 구조를 **DB 기반 `CrawlingSource` 참조 구조**로 전환하여,  
관리자가 관리자 화면에서 크롤링 소스를 동적으로 관리할 수 있도록 구현합니다.

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

---

# 아키텍처 원칙

1. `SourceType` enum은 제거하고, `Campaign BC` 내부의 **`CrawlingSource` 엔티티**로 대체합니다.
2. `Campaign`은 `CrawlingSource`를 **`@ManyToOne`** 으로 참조합니다.
3. 외부 API의 호환성을 위해 응답 필드명은 가능하면 유지합니다.
   - 예: 외부 응답에는 계속 `sourceType`, `sourceDisplayName` 사용
   - 단, 내부 구현은 `CrawlingSource.code`, `CrawlingSource.name`을 사용
4. 크롤러 구현체는 enum이 아니라 **`crawlerType` 문자열**로 매칭합니다.
5. `CrawlingSource.code`는 불변 식별자입니다.
6. 비활성화된 소스는:
   - 스케줄 크롤링 대상에서 제외
   - 필터 옵션에서 제외
   - 기존 캠페인 데이터는 유지
7. 이번 Phase에서는 **소스 삭제 API를 만들지 않습니다.**
   - 이유: 기존 `Campaign`, `CrawlingLog`와의 참조/이력 보존 문제 때문
   - 운영은 `active` 토글로 관리합니다.
8. `CrawlingLog`는 `CrawlingSource` FK를 직접 참조하지 않고,
   - `sourceCode`
   - `sourceName`
   - `crawlerType`
   의 **스냅샷 문자열**을 저장합니다.
9. 현재 프로젝트는 H2/local 기반 학습용 구조이므로, enum→DB 전환은 **실운영 DB in-place migration**이 아니라 **스키마 재생성 + 초기 데이터 재적재** 방식으로 처리합니다.
10. 기존 공개 API의 쿼리 파라미터는 내부 FK가 아니라 **source code 문자열 기반**으로 유지합니다.
    - 예: `sourceTypes=REVU,MBLE`

---

# 이번 범위에서 추가

- `CrawlingSource` 엔티티
- `Campaign.sourceType(enum)` → `Campaign.crawlingSource(FK)` 전환
- 크롤러 동적 매칭
- 관리자용 크롤링 소스 CRUD
- 필터 옵션 API의 source 목록 동적화
- 관리자 UI에서 소스 추가/수정/활성화/비활성화/테스트 실행
- 기존 크롤링 실행 버튼의 동적 생성

---

# 이번 범위에서 제외

구현하지 마세요.

- 크롤러 Java 코드 자체를 관리자 화면에서 작성/편집
- CSS selector를 DB에 저장하는 완전 동적 크롤러
- 소스 삭제 API
- 소스별 개별 cron 스케줄 설정
- 실운영용 DB migration 도구(Flyway/Liquibase) 도입
- 다중 인스턴스 환경의 분산 락

---

# 전환 전략

## Before
- `Campaign.sourceType = SourceType.REVU`
- `CampaignCrawler.getSourceType()`
- 필터 옵션 → `SourceType.values()`

## After
- `Campaign.crawlingSource = CrawlingSource(...)`
- `CampaignCrawler.getCrawlerType()`
- 필터 옵션 → `CrawlingSourceRepository.findAllActiveOrderByDisplayOrder()`

## 외부 API 호환성 유지 원칙
기존 프론트와 API 계약을 최대한 깨지 않기 위해,
외부 응답에는 아래 필드명을 유지합니다.

- `sourceType` → 내부적으로는 `crawlingSource.code`
- `sourceDisplayName` → 내부적으로는 `crawlingSource.name`

즉, **필드명은 유지하되 데이터 출처만 enum → DB로 변경**합니다.

---

# Phase 43. CrawlingSource 도메인 모델 + 마이그레이션

## 목표
기존 `SourceType` enum을 DB 엔티티 `CrawlingSource`로 전환하고, `Campaign`의 참조 방식을 변경합니다.

## CrawlingSource 엔티티

위치: `campaign/domain`

### 필드
- `id`: `Long` (PK)
- `code`: `String`
  - 영문 대문자/숫자/언더스코어
  - UNIQUE
  - 생성 후 변경 불가
  - 최대 30자
- `name`: `String`
  - 한글명
  - 최대 50자
- `baseUrl`: `String`
  - 최대 300자
- `listUrlPattern`: `String`
  - nullable
  - 최대 500자
- `description`: `String`
  - nullable
  - 최대 500자
- `crawlerType`: `String`
  - 크롤러 구현체 식별자
  - 최대 50자
- `active`: `boolean`
  - 기본 true
- `displayOrder`: `int`
  - 기본 0
- `createdAt`
- `updatedAt`

## 도메인 규칙
- `code`는 불변
- 정규식:
  - `^[A-Z0-9_]{2,30}$`
- `active=false`인 소스는 필터 옵션/스케줄 크롤링에서 제외
- `displayOrder ASC` 기준으로 정렬
- `crawlerType`은 `CampaignCrawler.getCrawlerType()`과 매칭

## 도메인 메서드
```java
public void update(String name, String baseUrl, String listUrlPattern,
                   String description, String crawlerType, int displayOrder) { ... }

public void activate() { ... }

public void deactivate() { ... }
```

## CrawlingSourceRepository
```java
public interface CrawlingSourceRepository {
    CrawlingSource save(CrawlingSource source);
    Optional<CrawlingSource> findById(Long id);
    Optional<CrawlingSource> findByCode(String code);
    List<CrawlingSource> findAllActiveOrderByDisplayOrder();
    List<CrawlingSource> findAllOrderByDisplayOrder();
    boolean existsByCode(String code);
}
```

## Campaign 엔티티 변경
### Before
```java
@Enumerated(EnumType.STRING)
private SourceType sourceType;
```

### After
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "crawling_source_id", nullable = false)
private CrawlingSource crawlingSource;
```

## Campaign 복합 Unique 변경
- 기존: `source_type + original_id`
- 변경: `crawling_source_id + original_id`

## Campaign 편의 메서드
```java
public String getSourceCode() { return crawlingSource.getCode(); }
public String getSourceName() { return crawlingSource.getName(); }
```

## SourceType 제거
- `SourceType.java` 삭제
- 기존 enum 참조를 모두 아래 기준으로 변경합니다.
  - 내부 검색/조건: `Set<String> sourceCodes`
  - entity 참조: `CrawlingSource`
  - 크롤링 결과 DTO: `sourceCode`
  - 외부 응답: `sourceType` 필드명 유지

## CrawlingLog 변경
기존 `SourceType` 또는 FK 참조 대신, 아래 스냅샷 필드로 저장합니다.

- `sourceCode`
- `sourceName`
- `crawlerType`

이유:
- 소스명 수정/비활성화 후에도 과거 로그를 그대로 유지하기 위함

## 마이그레이션 전략
이번 프로젝트는 H2/local 기반이므로 **in-place migration을 하지 않습니다.**

대신:
1. 스키마 재생성
2. `crawling_source` 초기 데이터 삽입
3. `campaign` 시드 데이터가 `crawling_source_id`를 참조하도록 `data.sql` 수정

## 초기 CrawlingSource 시드 데이터
- `REVU`
- `MBLE`
- `GANGNAM`

삽입 시 `display_order`도 함께 설정합니다.

## 영향 범위
아래 영역의 기존 코드가 모두 수정 대상입니다.

### Domain
- `Campaign.java`
- `CampaignSearchCondition.java`
- `CampaignRepository.java`
- `CrawlingLog.java`
- `SourceType.java` 삭제

### Application
- `CampaignSummary.java`
- `CampaignDetail.java`
- `CampaignSearchCommand.java`
- `CampaignService.java`
- `BookmarkedCampaignSummary.java`
- `AdminCampaignService.java`
- 관련 command/info DTO들

### Infrastructure
- `CampaignSpecification.java`
- `CampaignJpaRepository.java`
- `CrawledCampaign.java`
- `CampaignCrawler.java`
- 각 크롤러 구현체
- `CategoryMapper.java`
- 크롤링 오케스트레이터 관련 매칭 코드

### Interfaces
- `CampaignController.java`
- `CampaignSearchRequest.java`
- `CampaignItemResponse.java`
- `CampaignDetailResponse.java`
- `FilterOptionResponse.java`
- `AdminCampaignController.java`
- 관련 request/response DTO

## 산출물
- `CrawlingSource.java`
- `CrawlingSourceRepository.java`
- `CrawlingSourceSpringDataJpaRepository.java`
- `CrawlingSourceJpaRepository.java`
- `SourceType.java` 삭제
- `Campaign.java` (수정)
- `CampaignSearchCondition.java` (수정)
- `CampaignSpecification.java` (수정)
- `CrawlingLog.java` (수정)
- `data.sql` (수정)
- 위 영향 범위에 해당하는 모든 파일 수정

---

# Phase 44. 크롤러 동적 매칭 + 오케스트레이터 수정

## 목표
DB 기반 `CrawlingSource`와 크롤러 구현체를 동적으로 매칭합니다.

## CampaignCrawler 인터페이스 변경
```java
public interface CampaignCrawler {
    String getCrawlerType();
    List<CrawledCampaign> crawl(CrawlingSource source);
}
```

주의:
- `getSourceType()` 삭제
- `crawl()`은 `CrawlingSource`를 직접 받음
- 크롤러 내부 하드코딩 URL 대신 `source.getBaseUrl()`, `source.getListUrlPattern()` 사용

## CrawledCampaign 변경
- `SourceType sourceType` → `String sourceCode`

## CrawlerRegistry
```java
@Component
public class CrawlerRegistry {
    public Optional<CampaignCrawler> findByCrawlerType(String crawlerType) { ... }
    public List<String> getAvailableCrawlerTypes() { ... }
}
```

## GenericCrawler
전용 크롤러가 없는 신규 소스를 위한 범용 크롤러를 추가합니다.

### 규칙
- `getCrawlerType()` → `"GENERIC"`
- `crawl(CrawlingSource source)` 에서 `listUrlPattern` 활용
- 기본적인 링크/제목/이미지 정도만 추출 시도
- 완전 정밀 파싱보다는 fallback 성격

## 기존 크롤러 수정
- `RevuCrawler`
- `MbleCrawler`
- `GangnamCrawler`

각 크롤러는:
- `getCrawlerType()` 문자열 반환
- `crawl(CrawlingSource source)` 사용
- URL 하드코딩 제거

## CrawlingOrchestrator 수정
### executeAll()
- `sourceRepository.findAllActiveOrderByDisplayOrder()` 로 활성 소스 조회
- 각 소스에 대해 crawlerType 매칭
- 크롤러 없으면 FAILED 로그 저장 후 다음 진행

### executeBySourceCode(String sourceCode)
- `findByCode(sourceCode)`
- 없으면 `CrawlingSourceNotFoundException`

### Upsert 기준
- `findByCrawlingSourceAndOriginalId(source, originalId)`

## 수동 실행 API 변경
- 기존 `/execute/{sourceType}` → `/execute/{sourceCode}`
- 외부적으로는 REVU 같은 문자열이 그대로 들어가므로 호환성은 유지됨

## 커스텀 예외
- `CrawlingSourceNotFoundException`

## 산출물
- `CrawlerRegistry.java`
- `GenericCrawler.java`
- `CampaignCrawler.java` (수정)
- `CrawledCampaign.java` (수정)
- `CrawlingOrchestrator.java` (수정)
- `RevuCrawler.java` (수정)
- `MbleCrawler.java` (수정)
- `GangnamCrawler.java` (수정)
- `CrawlingController.java` (수정)
- `CampaignRepository.java` (수정)
- `CrawlingSourceNotFoundException.java`

---

# Phase 45. 크롤링 소스 관리 API

## 목표
관리자가 크롤링 소스를 CRUD(실질적으로는 생성/수정/활성화토글/조회) 할 수 있는 API를 구현합니다.

## 공통 규칙
- 모든 API는 ADMIN 전용
- **삭제 API는 제공하지 않음**
- code는 등록 시만 입력 가능, 수정 불가

## API 1. 소스 목록 조회
- `GET /api/v1/admin/crawling/sources`

### 응답
- `sources`
  - `id`
  - `code`
  - `name`
  - `baseUrl`
  - `listUrlPattern`
  - `description`
  - `crawlerType`
  - `active`
  - `displayOrder`
  - `campaignCount`
  - `lastCrawledAt`
  - `createdAt`
- `availableCrawlerTypes`

## API 2. 소스 등록
- `POST /api/v1/admin/crawling/sources`

### Bean Validation
- code: `@Pattern(^[A-Z0-9_]{2,30}$)`
- name: required
- baseUrl: required
- crawlerType: required

### 예외
- 중복 code → `409 DUPLICATE_SOURCE_CODE`

## API 3. 소스 수정
- `PUT /api/v1/admin/crawling/sources/{id}`

### 규칙
- code 수정 불가
- 수정 대상:
  - name
  - baseUrl
  - listUrlPattern
  - description
  - crawlerType
  - displayOrder

## API 4. 활성/비활성 토글
- `PATCH /api/v1/admin/crawling/sources/{id}/toggle-active`

### 규칙
- 기존 캠페인 데이터는 삭제하지 않음
- 비활성화된 소스는:
  - 필터 옵션에서 제외
  - 스케줄/전체 실행 대상에서 제외
- 단, 해당 소스의 기존 캠페인은 목록/상세에 계속 표시됨

## API 5. 소스별 크롤링 테스트
- `POST /api/v1/admin/crawling/sources/{id}/test`

### 규칙
- 1페이지 또는 소량만 테스트
- DB 저장 금지
- 파싱 결과 미리보기만 반환
- 매칭 크롤러 없으면 `400 CRAWLER_NOT_FOUND`

## CrawlingSourceService
기능:
- 목록 조회
- 등록
- 수정
- 활성/비활성 토글
- Dry Run 테스트
- campaignCount / lastCrawledAt 조합 조회

## 필터 옵션 API 변경
기존 `GET /api/v1/campaigns/filters` 의 `sourceTypes`는
`CrawlingSourceRepository.findAllActiveOrderByDisplayOrder()` 결과를 사용합니다.

## 산출물
- `CrawlingSourceService.java`
- `CrawlingSourceCreateCommand.java`
- `CrawlingSourceUpdateCommand.java`
- `CrawlingSourceInfo.java`
- `CrawlingTestResult.java`
- `AdminCrawlingSourceController.java`
- `CrawlingSourceCreateRequest.java`
- `CrawlingSourceUpdateRequest.java`
- `CrawlingSourceListResponse.java`
- `CrawlingSourceResponse.java`
- `CrawlingTestResponse.java`
- `DuplicateSourceCodeException.java`
- `CrawlerNotFoundException.java`
- `CampaignController.java` (수정)
- `FilterOptionResponse.java` (수정)
- `GlobalExceptionHandler.java` (수정)

---

# Phase 46. 관리자 크롤링 소스 관리 UI

## 목표
관리자 화면에서 크롤링 소스를 동적으로 관리할 수 있게 합니다.

## 페이지 구조
기존 `/admin/crawling` 을 탭 기반으로 확장합니다.

### 탭
- `execution`
- `sources`

### URL
- `/admin/crawling?tab=execution`
- `/admin/crawling?tab=sources`

## 소스 관리 탭 기능

### 1. 목록 테이블
컬럼:
- ID
- 코드
- 이름
- URL
- 크롤러
- 캠페인 수
- 마지막 크롤링
- 상태
- 액션

액션:
- 수정
- 테스트
- 활성/비활성

## 2. 소스 추가 모달
입력:
- 코드
- 이름
- Base URL
- 목록 URL 패턴
- 설명
- 크롤러 선택
- 표시 순서

코드 필드는 등록 시에만 편집 가능

## 3. 소스 수정 모달
- 등록 모달 재사용
- code는 read-only

## 4. 크롤링 테스트 결과 모달
- 성공/실패 건수
- 파싱된 캠페인 미리보기
- “DB에는 저장되지 않습니다” 안내

## 5. 활성/비활성 토글
- 확인 모달
- 비활성화 시 영향 안내 문구 표시

## 실행 탭 변경
기존 소스별 하드코딩 버튼을 제거하고,
**활성 소스 목록을 기반으로 동적 버튼 생성**으로 변경합니다.

예:
- 전체 실행
- 활성 소스별 실행 버튼

## 메인 홈 연동
기존 메인 홈의 필터 소스 칩은 필터 옵션 API를 호출하므로,
활성 소스 추가/비활성화 후 자동 반영되어야 합니다.

## 프로젝트 구조
```text
/frontend
  /src
    /pages
      /admin
        AdminCrawlingPage.tsx
    /components
      /admin
        CrawlingExecutionTab.tsx
        CrawlingSourceTab.tsx
        CrawlingSourceFormModal.tsx
        CrawlingTestResultModal.tsx
    /api
      adminApi.ts
    /hooks
      /admin
        useAdminCrawling.ts
        useAdminCrawlingSources.ts
    /types
      admin.ts
    /styles
      /admin
        CrawlingSourceTab.module.css
        CrawlingSourceForm.module.css
        CrawlingTestResult.module.css
```

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의

---

# Phase 47. 크롤링 소스 동적 관리 테스트

## 목표
enum → DB 전환 이후의 회귀 테스트와 신규 기능 테스트를 작성합니다.

## 테스트 범위

### 1. 도메인 단위 테스트
- `CrawlingSource`
  - 생성
  - activate/deactivate
  - update
  - code 불변
- `Campaign`
  - `crawlingSource` 연관관계
  - `getSourceCode()`
  - `getSourceName()`

## 2. Registry 테스트
- crawlerType 조회
- 미등록 type → empty

## 3. Orchestrator 테스트
- 활성 소스만 실행
- 비활성 소스 skip
- 매칭 크롤러 없음 → FAILED 처리
- sourceCode 기준 실행
- upsert 기준 변경 확인

## 4. Application 서비스 테스트
### CrawlingSourceService
- 등록
- 중복 code
- 수정
- 토글
- Dry Run
- 크롤러 미존재

### CampaignService 회귀
- 필터 옵션에 활성 소스만 포함
- 비활성 소스의 캠페인도 목록에 표시

## 5. API 통합 테스트
- 소스 관리 API 전체
- USER 접근 → 403
- 필터 옵션 API 동적 소스 반환 확인
- 비활성화 후 필터 옵션에서 제외 확인
- 기존 캠페인 목록/상세 응답의 source 정보 정상 표시 확인
- 기존 크롤링 실행 API가 활성 소스 기준으로 동작하는지 확인

## 6. 마이그레이션 검증
- `data.sql` 기준 기존 캠페인이 `crawling_source_id`로 정상 연결되는지 검증

## 7. 프론트 테스트
- `useAdminCrawlingSources`
  - 목록 로드
  - 등록/수정
  - 토글
  - 테스트 실행

## 산출물
- `CrawlingSourceTest.java`
- `CampaignSourceMigrationTest.java`
- `CrawlerRegistryTest.java`
- `CrawlingOrchestratorSourceTest.java`
- `CrawlingSourceServiceTest.java`
- `CampaignServiceSourceTest.java`
- `AdminCrawlingSourceControllerIntegrationTest.java`
- `CampaignFilterOptionIntegrationTest.java`
- `CrawlingExecuteSourceIntegrationTest.java`
- `useAdminCrawlingSources.test.ts`

---

# 전체 Phase 누적 현황

| Phase | 기능 | BC |
|-------|------|----|
| 1~6 | 회원가입 | Member |
| 7~11 | 로그인 / JWT | Member |
| 12~16 | 캠페인 목록 조회 | Campaign |
| 17~21 | 캠페인 상세 | Campaign |
| 22~26 | 마이페이지 | Member |
| 27~31 | 찜 / 북마크 | Campaign |
| 32~36 | 크롤링 자동화 | Campaign |
| 37~42 | 관리자 기능 | Admin |
| 43~47 | 크롤링 소스 동적 관리 | Campaign + Admin |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.
ㅔ
**"크롤링 소스 동적 관리 프로젝트 명세를 완벽히 이해했습니다. Phase 43 진행을 시작할까요?"**