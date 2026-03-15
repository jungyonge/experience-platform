당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, 배치/스케줄링, 크롤링 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 크롤링 자동화 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 + 마이페이지 + 찜 기능(Phase 1~31)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 32~36)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"크롤링 자동화 프로젝트 명세를 완벽히 이해했습니다. Phase 32 진행을 시작할까요?"**

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

Phase 1~31에서 다음 기능이 이미 구현되어 있습니다.

## 기존 구현 요약
- Campaign BC
  - 목록 조회
  - 상세 조회
  - 찜
  - `SourceType` enum: `REVU`, `MBLE`, `GANGNAM`
- Campaign 엔티티
  - `sourceType + originalId` 복합 Unique 제약
  - `update()` 메서드 존재
- `data.sql` 기반 시드 데이터 50건+
- Spring Security / JWT
- 관리자 UI는 아직 없음

---

# 기술 스택

## Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 Database
- Jsoup

## Frontend
- 이번 Phase에서는 프론트 UI 구현이 아니라 **관리 API 중심**
- 별도 React 화면은 구현하지 않음

---

# 아키텍처 원칙

1. 크롤링 기능은 **Campaign BC의 infrastructure 중심 기능**으로 설계합니다.
2. 단, 여러 크롤러를 조율하는 `CrawlingOrchestrator` 는 **application service** 성격으로 설계할 수 있습니다.
3. 사이트별 크롤러는 **Strategy 패턴**으로 분리합니다.
4. 새 사이트 추가 시 기존 오케스트레이터 수정 없이 **`CampaignCrawler` 구현체만 추가**하면 동작해야 합니다.
5. 크롤링 결과는 `Campaign` 도메인으로 upsert 됩니다.
6. 운영성 이력 데이터인 `CrawlingLog` 는 이번 프로젝트에서는 **campaign.infrastructure.crawling 내부 persistence 모델**로 관리합니다.
7. 외부 사이트 접근 실패, HTML 구조 변경, 일부 아이템 파싱 실패를 고려한 **방어적 구현**이 필요합니다.
8. 하나의 사이트 크롤링 실패가 전체 크롤링을 중단시키면 안 됩니다.
9. 분산 환경 락은 구현하지 않고, **단일 애플리케이션 인스턴스 기준 중복 실행 방지**만 구현합니다.
10. local 환경에서는 기본적으로:
   - 스케줄링 비활성화
   - mock 크롤러 활성화
11. 실제 크롤링과 mock 크롤링은 **설정으로 명시적으로 분기**합니다.
   - 실제 모드 실패 시 자동 mock fallback 하지 않습니다.

---

# 이번 범위에서 추가

- 크롤링 인프라 모듈
- 사이트별 크롤러 플러그인 구조
- 스케줄링 (`@Scheduled`)
- 크롤링 실행 로그
- 수동 크롤링 트리거 API
- Upsert 로직
- 시드 데이터와 크롤링 데이터 병행 전략

---

# 이번 범위에서 제외

구현하지 마세요.

- 관리자 UI
- 분산 크롤링 / 메시지 큐
- Selenium / Playwright / Headless 브라우저
- Proxy / IP 로테이션
- Redis 캐싱
- 크롤링 대상 사이트 DB 관리
- 관리자 RBAC
- 실제 운영용 anti-bot 회피 기법

---

# 크롤링 대상 사이트

실제 구현 시 각 사이트의 HTML 구조를 분석하여 크롤러를 작성합니다.

대상 사이트:
- `REVU` → `revu.net`
- `MBLE` → `mble.xyz`
- `GANGNAM` → `gangnammatzip.com`

주의:
- 사이트 접근이 불가능하거나 HTML 구조가 변경되었을 수 있습니다.
- 이를 고려해 **mock 모드**를 제공합니다.
- mock 모드는 `crawling.mock-enabled=true` 일 때만 사용합니다.

---

# 크롤링 정책

## 공통 정책
- 실행 주기: 1시간마다
- 중복 판단: `sourceType + originalId`
- 기존 데이터 존재 시: update
- 신규 데이터: insert
- 마감된 캠페인: 삭제하지 않고 `CLOSED` 로 갱신
- 요청 간격: 사이트당 1~2초 랜덤 딜레이
- 사이트별 최대 페이지: 5
- User-Agent: 커스텀 설정
- robots.txt 확인
- 사이트 하나 실패해도 다른 사이트는 계속 진행
- 일부 아이템 파싱 실패는 전체 실패가 아니라 partial 처리 가능

## 타임아웃
- 연결 타임아웃: 10초
- 읽기 타임아웃: 30초

## mock 정책
- local 기본값은 `mock-enabled=true`
- mock 모드에서는 외부 사이트를 호출하지 않고 더미 데이터를 생성합니다.
- 실제 모드(`mock-enabled=false`) 에서는 Jsoup 기반 실제 파싱만 수행합니다.

---

# 시드 데이터 병행 정책

- 기존 `data.sql` 은 유지 가능합니다.
- 시드 데이터는 `originalId` 를 `seed-xxx` 형식으로 구성하여 크롤링 데이터와 충돌하지 않게 합니다.
- 추후 완전 전환 시에는 `data.sql` 을 제거하거나 비울 수 있습니다.
- 이번 Phase에서는 **시드 + 크롤링 데이터 병행 허용**으로 설계합니다.

---

# 패키지 구조 기준

```text
campaign/
  application/
    crawling/
      CrawlingOrchestrator.java
  infrastructure/
    crawling/
      CampaignCrawler.java
      CrawledCampaign.java
      CrawlingResult.java
      CrawlingException.java
      CrawlingInProgressException.java
      CrawlingProperties.java
      JsoupClient.java
      RobotsTxtChecker.java
      CrawlingDelayHandler.java
      CategoryMapper.java
      CrawlingDateParser.java
      CrawlingNumberParser.java
      crawler/
        RevuCrawler.java
        MbleCrawler.java
        GangnamCrawler.java
      log/
        CrawlingLog.java
        CrawlingLogStatus.java
        CrawlingLogRepository.java
        CrawlingLogJpaRepository.java
        CrawlingLogSpringDataJpaRepository.java
      scheduler/
        CrawlingScheduler.java
  interfaces/
    crawling/
      CrawlingController.java
      dto/
        CrawlingExecuteResponse.java
        CrawlingResultResponse.java
        CrawlingLogListResponse.java
        CrawlingLogResponse.java
```

주의:
- `CrawlingLog` 는 운영성 persistence 모델이므로 `campaign.infrastructure.crawling.log` 아래에 둡니다.
- 기존 domain 계층에 억지로 넣지 마세요.

---

# Phase 32. 크롤링 인프라 기반 구축

## 목표
크롤링 모듈의 공통 기반 구조를 만듭니다.

## build.gradle 의존성 추가
- `org.jsoup:jsoup:1.17.2`

## CampaignCrawler
```java
public interface CampaignCrawler {
    SourceType getSourceType();
    List<CrawledCampaign> crawl();
}
```

규칙:
- 사이트별 크롤러는 Spring Bean 으로 등록
- 오케스트레이터는 `List<CampaignCrawler>` 로 자동 주입

## CrawledCampaign
크롤러가 HTML 에서 파싱한 **인프라 DTO** 입니다.

필드:
- `sourceType`
- `originalId`
- `title`
- `description`
- `detailContent`
- `thumbnailUrl`
- `originalUrl`
- `category`
- `status`
- `recruitCount`
- `applyStartDate`
- `applyEndDate`
- `announcementDate`
- `reward`
- `mission`
- `address`
- `keywords`

## CrawlingResult
개별 소스 실행 결과 DTO 입니다.

### 필드
- `sourceType`
- `status`
- `totalCrawled`
- `newCount`
- `updatedCount`
- `failedCount`
- `errorMessage`
- `durationMs`

주의:
- `boolean success` 대신 **상태 enum 기반**으로 관리합니다.

## Crawling 상태 enum
`CrawlingResult` 와 `CrawlingLog` 에서 공통적으로 사용할 수 있도록 아래 상태를 사용합니다.

- `SUCCESS`
- `FAILED`
- `PARTIAL`

필요하면 `CrawlingLogStatus` 하나로 통일해도 됩니다.

## CrawlingLog
운영성 이력 엔티티입니다.

### 필드
- `id`
- `sourceType`
- `status`
- `totalCrawled`
- `newCount`
- `updatedCount`
- `failedCount`
- `errorMessage`
- `durationMs`
- `executedAt`

### 저장소 구조
- `CrawlingLogRepository` : infrastructure 내부 추상화
- `CrawlingLogSpringDataJpaRepository extends JpaRepository<CrawlingLog, Long>`
- `CrawlingLogJpaRepository implements CrawlingLogRepository`

### 조회 메서드
`findRecent` 는 `Pageable` 기반으로 설계하세요.

예:
```java
List<CrawlingLog> findRecent(SourceType sourceType, Pageable pageable);
List<CrawlingLog> findRecent(Pageable pageable);
```

`limit int` 를 직접 repository 메서드에 박아 넣는 방식은 피하세요.

## JsoupClient
공통 HTTP 유틸입니다.

### 책임
- 공통 User-Agent 설정
- timeout 설정
- referrer 설정
- Jsoup 연결/파싱 수행

### 메서드 예시
```java
Document fetch(String url);
```

실패 시:
- `CrawlingException` 으로 감싸서 던집니다.

## RobotsTxtChecker
### 책임
- `robots.txt` 조회 및 허용 여부 판단
- 사이트별 결과 메모리 캐싱

### 정책
- `robots.txt` 조회 실패 시 이번 프로젝트에서는 **허용 + 경고 로그** 정책으로 갑니다.
- 보수적 차단으로 바꾸지 않습니다.

### 메서드 예시
```java
boolean isAllowed(String baseUrl, String path);
```

## CrawlingDelayHandler
- `delay()` 메서드 제공
- `delay-min-ms` ~ `delay-max-ms` 사이 랜덤 sleep
- 인터럽트 발생 시 현재 스레드 인터럽트 상태 복원

## CrawlingProperties
`@ConfigurationProperties(prefix = "crawling")`

### 필드
- `enabled`
- `mockEnabled`
- `scheduleCron`
- `maxPagesPerSite`
- `delayMinMs`
- `delayMaxMs`
- `connectionTimeoutMs`
- `readTimeoutMs`
- `userAgent`

## application-local.yml
```yaml
crawling:
  enabled: false
  mock-enabled: true
  schedule-cron: "0 0 * * * *"
  max-pages-per-site: 5
  delay-min-ms: 1000
  delay-max-ms: 2000
  connection-timeout-ms: 10000
  read-timeout-ms: 30000
  user-agent: "ExperiencePlatformBot/1.0 (+https://example.com/bot)"
```

## 산출물
- `CampaignCrawler.java`
- `CrawledCampaign.java`
- `CrawlingResult.java`
- `CrawlingException.java`
- `CrawlingProperties.java`
- `JsoupClient.java`
- `RobotsTxtChecker.java`
- `CrawlingDelayHandler.java`
- `CrawlingLog.java`
- `CrawlingLogStatus.java`
- `CrawlingLogRepository.java`
- `CrawlingLogSpringDataJpaRepository.java`
- `CrawlingLogJpaRepository.java`
- `application-local.yml` (수정)
- `build.gradle` (수정)

---

# Phase 33. 사이트별 크롤러 구현

## 목표
각 사이트별 크롤러와 파싱 유틸리티를 구현합니다.

## 구현 원칙
- 각 크롤러는 `CampaignCrawler` 구현체 + Spring `@Component`
- mock 모드와 실제 모드를 모두 지원
- 실제 모드 실패 시 자동 mock fallback 금지
- 개별 아이템 파싱 실패는 skip 하고 다음 아이템 계속 진행

## 공통 흐름
1. mock-enabled 확인
2. mock 모드면 더미 데이터 반환
3. 실제 모드면 robots.txt 확인
4. 차단되면 빈 결과 반환 + 로그
5. 목록 페이지 1부터 최대 5페이지 순회
6. HTML 파싱
7. 각 아이템에서 필드 추출
8. 일부 실패 아이템은 skip
9. 페이지 간 delay
10. 최종 리스트 반환

## 사이트별 크롤러
- `RevuCrawler`
- `MbleCrawler`
- `GangnamCrawler`

### 실제 사이트 파싱
- CSS selector 는 최대한 명확하게 작성
- selector 실패 시 전체 예외를 던지기보다 방어적으로 처리
- `originalId`, `title`, `originalUrl` 은 필수 필드로 간주
- 필수 필드가 없으면 해당 아이템 skip

## 카테고리 매핑
`CategoryMapper` 를 별도 유틸로 분리합니다.

규칙:
- 사이트별 카테고리 문자열 → `CampaignCategory`
- 매핑 실패 → `ETC`

## 날짜 파싱
`CrawlingDateParser`

지원 예시:
- `2026.03.25`
- `2026-03-25`
- `26/03/25`
- `3월 25일`
- `D-3`
- `마감 3일전`

규칙:
- 파싱 실패 시 예외 대신 `null`

## 숫자 파싱
`CrawlingNumberParser`

예시:
- `5명 모집` → `5`
- `인원 10` → `10`

규칙:
- 실패 시 `null`

## Mock 데이터 생성
- 각 소스별 10~20건 생성
- 다양한 카테고리 / 상태 / 날짜 분포
- `originalId` 는 소스별 고유하게 생성
- mock 데이터는 매 실행마다 조금 달라도 되지만, 테스트 가능성을 위해 너무 랜덤하게 만들지 말고 재현 가능한 패턴을 권장합니다.

## 설정 추가
`application-local.yml`
```yaml
crawling:
  mock-enabled: true
```

## 산출물
- `RevuCrawler.java`
- `MbleCrawler.java`
- `GangnamCrawler.java`
- `CategoryMapper.java`
- `CrawlingDateParser.java`
- `CrawlingNumberParser.java`
- `CrawlingProperties.java` (수정)
- `application-local.yml` (수정)

---

# Phase 34. 크롤링 오케스트레이터 및 스케줄링

## 목표
사이트별 크롤러를 조율하고, upsert 와 스케줄링을 구현합니다.

## CrawlingOrchestrator
위치는 `campaign.application.crawling`

### 책임
- 등록된 모든 크롤러 실행
- 특정 소스만 실행
- 결과를 Campaign 으로 upsert
- 실행 로그 저장
- 중복 실행 방지
- 크롤링 후 만료 캠페인 상태 정리

### 구조 예시
```java
@Service
public class CrawlingOrchestrator {

    private final List<CampaignCrawler> crawlers;
    private final CampaignRepository campaignRepository;
    private final CrawlingLogRepository crawlingLogRepository;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public List<CrawlingResult> executeAll() { ... }

    public CrawlingResult executeBySourceType(SourceType sourceType) { ... }
}
```

## 중복 실행 방지
- `AtomicBoolean isRunning`
- 실행 시작 시 `compareAndSet(false, true)`
- 실패/성공 관계없이 `finally` 에서 false 복구
- 이미 실행 중이면 `CrawlingInProgressException`

주의:
- 이 방식은 **단일 인스턴스 기준**입니다.
- 분산 락은 이번 범위에서 제외합니다.

## Upsert 로직
각 `CrawledCampaign` 에 대해:
1. `sourceType + originalId` 로 기존 Campaign 조회
2. 있으면 `campaign.update(...)`
3. 없으면 신규 생성
4. 저장
5. 동시성으로 인한 DB Unique 위반 발생 시 해당 아이템은 skip 가능

규칙:
- 한 소스 크롤링은 하나의 트랜잭션 경계로 둘 수 있습니다.
- 단, 개별 아이템 실패 때문에 전체 소스 크롤링을 롤백시키지 않도록 주의하세요.
- 구현 단순성을 위해 아이템별 예외를 catch 해서 카운팅 후 계속 진행하는 방식이 좋습니다.

## 마감 캠페인 상태 정리
- 기준 날짜를 명시적으로 전달받아 처리하세요.
- 예:
```java
List<Campaign> findExpiredRecruitingCampaigns(LocalDate today);
```
- `applyEndDate < today` 이고 `status == RECRUITING` 인 캠페인을 `CLOSED` 로 변경

## CampaignRepository 확장
- `findExpiredRecruitingCampaigns(LocalDate today)` 추가
- 기존 저장소 구조 유지

## CrawlingScheduler
위치는 `campaign.infrastructure.crawling.scheduler`

```java
@Component
@ConditionalOnProperty(name = "crawling.enabled", havingValue = "true")
public class CrawlingScheduler {

    private final CrawlingOrchestrator orchestrator;

    @Scheduled(cron = "${crawling.schedule-cron}")
    public void scheduledCrawl() {
        orchestrator.executeAll();
    }
}
```

## 프로파일 정책
### application-local.yml
```yaml
crawling:
  enabled: false
  mock-enabled: true
```

### application-dev.yml 예시
```yaml
crawling:
  enabled: true
  mock-enabled: false
```

## 산출물
- `CrawlingOrchestrator.java`
- `CrawlingInProgressException.java`
- `CrawlingScheduler.java`
- `CampaignRepository.java` (수정)
- 관련 JPA 구현체 (수정)
- `application-local.yml` (수정)

---

# Phase 35. 크롤링 수동 트리거 API

## 목표
수동 실행 및 실행 로그 조회용 관리 API를 구현합니다.

## 공통 규칙
- `/api/v1/admin/**`
- 현재는 **인증만 필요**
- 추후 ADMIN 권한 체크로 확장 예정

## API 1. 전체 크롤링 실행
- `POST /api/v1/admin/crawling/execute`

### 성공 응답
```json
{
  "results": [
    {
      "sourceType": "REVU",
      "sourceDisplayName": "레뷰",
      "status": "SUCCESS",
      "totalCrawled": 45,
      "newCount": 3,
      "updatedCount": 42,
      "failedCount": 0,
      "errorMessage": null,
      "durationMs": 12500
    }
  ],
  "totalDurationMs": 45000,
  "executedAt": "2026-03-15T10:00:00"
}
```

## API 2. 특정 소스 실행
- `POST /api/v1/admin/crawling/execute/{sourceType}`

### Path Variable
- `REVU`
- `MBLE`
- `GANGNAM`

### 성공 응답
```json
{
  "sourceType": "REVU",
  "sourceDisplayName": "레뷰",
  "status": "SUCCESS",
  "totalCrawled": 45,
  "newCount": 3,
  "updatedCount": 42,
  "failedCount": 0,
  "errorMessage": null,
  "durationMs": 12500,
  "executedAt": "2026-03-15T10:00:00"
}
```

## API 3. 실행 로그 조회
- `GET /api/v1/admin/crawling/logs`

### Query Parameters
- `sourceType`: optional
- `limit`: optional, default 20, max 100

### 성공 응답
```json
{
  "logs": [
    {
      "id": 1,
      "sourceType": "REVU",
      "sourceDisplayName": "레뷰",
      "status": "SUCCESS",
      "totalCrawled": 45,
      "newCount": 3,
      "updatedCount": 42,
      "failedCount": 0,
      "errorMessage": null,
      "durationMs": 12500,
      "executedAt": "2026-03-15T10:00:00"
    }
  ]
}
```

## 에러 응답
- 잘못된 `sourceType` → `400 INVALID_SOURCE_TYPE`
- 이미 실행 중 → `409 CRAWLING_IN_PROGRESS`
- 미인증 → `401 UNAUTHORIZED`

## DTO
- `CrawlingExecuteResponse.java`
- `CrawlingResultResponse.java`
- `CrawlingLogListResponse.java`
- `CrawlingLogResponse.java`

## Controller
- `CrawlingController.java`
- `@AuthenticationPrincipal` 또는 기존 principal 구조와 일관된 인증 방식 사용
- 현재는 인증만 확인하면 되므로 별도 ADMIN 체크 없음

## GlobalExceptionHandler 확장
- `CrawlingInProgressException` → `409`
- `MethodArgumentTypeMismatchException` 또는 enum 파싱 오류 → `400 INVALID_SOURCE_TYPE`

## SecurityConfig
- `/api/v1/admin/**` → authenticated

## 산출물
- `CrawlingController.java`
- `CrawlingExecuteResponse.java`
- `CrawlingResultResponse.java`
- `CrawlingLogListResponse.java`
- `CrawlingLogResponse.java`
- `CrawlingInProgressException.java`
- `GlobalExceptionHandler.java` (수정)
- `SecurityConfig.java` (수정)
- `CrawlingOrchestrator.java` (필요 시 수정)

---

# Phase 36. 크롤링 테스트

## 목표
크롤링 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 유틸리티 단위 테스트
- `CategoryMapper`
- `CrawlingDateParser`
- `CrawlingNumberParser`
- `RobotsTxtChecker`

검증:
- 정상 매핑/파싱
- 매핑 실패 시 기본값 / null
- robots 허용/차단
- robots 조회 실패 시 허용 정책

### 2. 개별 크롤러 테스트
외부 사이트 호출 없이 **HTML fixture 파일 기반**으로 테스트합니다.

리소스:
- `src/test/resources/crawling/revu_sample.html`
- `src/test/resources/crawling/mble_sample.html`
- `src/test/resources/crawling/gangnam_sample.html`

검증:
- HTML 파싱 → `CrawledCampaign` 리스트
- 필수 필드 존재
- 카테고리/상태 매핑
- 빈 HTML → 빈 리스트
- 일부 아이템 파싱 실패 → 나머지 정상 반환
- mock 모드 데이터 생성

### 3. CrawlingOrchestrator 테스트
- 전체 실행 성공
- 하나의 크롤러 실패 시 나머지 계속
- 신규 insert
- 기존 데이터 update
- unique 위반 skip
- 만료 캠페인 `RECRUITING -> CLOSED`
- 중복 실행 방지
- 실행 로그 저장

주의:
- 크롤러는 Mock 사용
- 실제 외부 사이트 호출 금지

### 4. API 통합 테스트
- `POST /api/v1/admin/crawling/execute`
- `POST /api/v1/admin/crawling/execute/{sourceType}`
- `GET /api/v1/admin/crawling/logs`

검증:
- 인증 성공/실패
- 잘못된 sourceType
- 정상 응답 구조
- 로그 필터링

### 5. 스케줄링 테스트
- `scheduledCrawl()` 직접 호출
- `@ConditionalOnProperty` 기반 빈 생성 여부 검증

## 테스트 설계 원칙
- 외부 네트워크 의존 제거
- fixture HTML 기반 파싱 테스트
- 오케스트레이터는 Mock crawler 기반
- flaky test 방지
- 랜덤 mock 데이터는 테스트 시 결정적 값 사용 가능하도록 제어

## 산출물
- `CategoryMapperTest.java`
- `CrawlingDateParserTest.java`
- `CrawlingNumberParserTest.java`
- `RobotsTxtCheckerTest.java`
- `RevuCrawlerTest.java`
- `MbleCrawlerTest.java`
- `GangnamCrawlerTest.java`
- `CrawlingOrchestratorTest.java`
- `CrawlingControllerIntegrationTest.java`
- `CrawlingSchedulerTest.java`
- HTML sample 파일 3개

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
| 32~36 | 크롤링 자동화 | Campaign (infrastructure/application) |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"크롤링 자동화 프로젝트 명세를 완벽히 이해했습니다. Phase 32 진행을 시작할까요?"**