당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 알림 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 + 마이페이지 + 찜 + 크롤링 자동화 + 관리자 기능 + 크롤링 소스 동적 관리(Phase 1~47)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **6개의 Phase(Phase 48~53)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"알림 기능 프로젝트 명세를 완벽히 이해했습니다. Phase 48 진행을 시작할까요?"**

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

## 기존 구현 요약
- Member BC
  - 회원가입
  - 로그인 / JWT
  - 마이페이지
  - RBAC
- Campaign BC
  - 목록 / 상세
  - 찜
  - 크롤링 자동화
  - 크롤링 소스 동적 관리
- Admin BC
  - 회원 / 캠페인 / 크롤링 관리
  - 대시보드
- React
  - 메인 홈
  - 상세
  - 마이페이지
  - 관리자 페이지
- 클라이언트에는 이미 간단한 Toast 시스템이 존재

---

# 기술 스택

## Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 Database
- Spring Events
- Spring Scheduling
- Spring Async

## Frontend
- React
- TypeScript
- Vite
- react-router-dom
- CSS Modules

---

# 아키텍처 원칙

1. **Notification Bounded Context** 를 신규 생성합니다.
2. Notification BC 는 Member / Campaign BC 와 **직접 엔티티 연관관계를 맺지 않고**, `memberId`, `campaignId`, `sourceCode` 같은 값 참조를 사용합니다.
3. BC 간 연동은 **Spring Event** 기반으로 처리합니다.
4. 실시간 WebSocket 은 이번 범위에서 제외하고, **폴링 기반 인앱 알림**으로 구현합니다.
5. 알림은 모두 **서비스 내부(in-app)** 저장형 알림입니다.
6. 알림 중복 방지는 `campaignId + type` 수준이 아니라 **`referenceKey` 기반**으로 구현합니다.
7. 알림 생성은 크롤링 / 회원가입 트랜잭션과 강하게 묶지 않고, **`@TransactionalEventListener(AFTER_COMMIT)`** 로 커밋 후 처리합니다.
8. 대량 알림 생성은 `@Async("notificationExecutor")` 를 사용해 크롤링/배치 본 처리에 미치는 영향을 줄입니다.
9. 오래된 알림은 30일 이후 자동 삭제합니다.
10. 프론트에서는 로그인 사용자에게만 알림 벨과 폴링을 활성화합니다.

---

# 이번 범위에서 추가

- Notification BC
- 인앱 알림
  - 마감 임박
  - 오늘 마감
  - 상태 변경
  - 신규 캠페인
- 알림 설정
- 알림 생성 이벤트 리스너
- 마감 알림 스케줄러
- 알림 정리 스케줄러
- 알림 목록 / 읽음 / 전체 읽음 API
- Header 벨 아이콘 + 알림 드롭다운
- 전체 알림 페이지
- 마이페이지 알림 설정 탭

---

# 이번 범위에서 제외

구현하지 마세요.

- 이메일 알림
- 푸시 알림
- WebSocket / SSE 실시간 알림
- 관리자 공지사항 알림
- SMS 알림
- 브라우저 Notification API
- 알림 템플릿 관리자 UI

---

# 알림 종류

| 알림 유형 | 트리거 | 예시 | 대상 |
|------|------|------|------|
| `DEADLINE_APPROACHING` | 스케줄러 | `[캠페인명] 마감이 3일 남았습니다.` | 해당 캠페인 찜한 회원 |
| `DEADLINE_TODAY` | 스케줄러 | `[캠페인명] 오늘 마감입니다!` | 해당 캠페인 찜한 회원 |
| `STATUS_CHANGED` | 크롤링 후 이벤트 | `[캠페인명] 모집이 마감되었습니다.` | 해당 캠페인 찜한 회원 |
| `NEW_CAMPAIGN` | 크롤링 후 이벤트 | `[소스명]에 새 맛집 체험단이 등록되었습니다.` | 관심 카테고리 설정 회원 |

---

# 알림 정책

## 마감 알림
- `DEADLINE_APPROACHING`: 기본 D-3
- `DEADLINE_TODAY`: 마감일 당일

## 보관 정책
- 30일 이후 자동 삭제

## 중복 방지 정책
중복 방지는 **referenceKey** 로 처리합니다.

### Notification.referenceKey 예시
- `DEADLINE_APPROACHING:{campaignId}:{applyEndDate}`
- `DEADLINE_TODAY:{campaignId}:{applyEndDate}`
- `STATUS_CHANGED:{campaignId}:{newStatus}`
- `NEW_CAMPAIGN:{campaignId}`

즉:
- 같은 캠페인의 D-3 알림은 1회
- 같은 캠페인의 D-Day 알림은 1회
- 같은 캠페인의 `CLOSED`, `ANNOUNCED`, `COMPLETED` 상태 변경 알림은 각각 별도 가능
- 같은 신규 캠페인 알림은 회원당 1회

## 폴링 정책
- 미읽음 카운트는 60초마다 폴링
- 탭이 숨김 상태면 중지
- 탭이 다시 활성화되면 즉시 1회 갱신 후 재시작

## 클릭 이동 정책
- `campaignId` 가 있으면 클릭 시 `/campaigns/{campaignId}` 이동
- 이동과 동시에 읽음 처리
- 이미 삭제된 캠페인이라면 상세 페이지에서 404 처리되도록 허용
- `campaignId` 가 없으면 읽음만 처리하고 이동하지 않아도 됨

---

# 알림함 UI 기준

## Header
- 로그인 상태일 때만 벨 아이콘 표시
- 미읽음 카운트 뱃지 표시
- 0이면 숨김

## 드롭다운
- 최근 5건 표시
- [모두 읽음]
- [전체 알림 보기]

## 전체 알림 페이지
- `/notifications`
- 인증 필요
- 유형 필터
- 페이지네이션
- 모두 읽음 버튼

## 마이페이지 알림 설정
- 기존 마이페이지 탭에 `알림 설정` 추가
- URL 예: `/mypage?tab=notification-settings`

---

# Phase 48. Notification 도메인 모델 설계

## 목표
새로운 Notification BC 를 만들고 알림 도메인 모델을 설계합니다.

## 패키지 구조
```text
com.example.experienceplatform
  ├── member/
  ├── campaign/
  ├── admin/
  └── notification/
      ├── domain/
      ├── application/
      ├── infrastructure/
      └── interfaces/
```

## Notification 엔티티

### 필드
- `id`: `Long`
- `memberId`: `Long`
- `type`: `NotificationType`
- `title`: `String`
- `message`: `String`
- `campaignId`: `Long` nullable
- `sourceCode`: `String` nullable
- `referenceKey`: `String`
- `isRead`: `boolean`
- `readAt`: `LocalDateTime` nullable
- `createdAt`: `LocalDateTime`

## 필드 규칙
- `title`: 최대 100자
- `message`: 최대 500자
- `referenceKey`: 최대 200자 정도의 문자열이면 충분
- `referenceKey` 는 `memberId + referenceKey` 조합으로 unique 하게 관리 가능하도록 설계
  - JPA unique constraint 또는 exists 체크 + fallback 방식 가능
- `campaignId` 는 nullable
- `sourceCode` 는 nullable

## 인덱스
- `(member_id, is_read, created_at)` 복합 인덱스
- `created_at` 인덱스
- `(member_id, reference_key)` unique 또는 동등한 중복 방지 전략

## NotificationType
```java
public enum NotificationType {
    DEADLINE_APPROACHING("마감 임박", "찜한 캠페인의 마감이 임박했습니다."),
    DEADLINE_TODAY("오늘 마감", "찜한 캠페인이 오늘 마감됩니다."),
    STATUS_CHANGED("상태 변경", "찜한 캠페인의 상태가 변경되었습니다."),
    NEW_CAMPAIGN("신규 캠페인", "관심 카테고리에 새 캠페인이 등록되었습니다.");

    private final String displayName;
    private final String defaultMessage;
}
```

## Notification 도메인 메서드
```java
public void markAsRead() {
    if (!this.isRead) {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
```

## NotificationRepository
```java
public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> saveAll(List<Notification> notifications);

    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

    Page<Notification> findByMemberId(Long memberId, Pageable pageable);
    Page<Notification> findByMemberIdAndTypeIn(Long memberId, List<NotificationType> types, Pageable pageable);

    long countByMemberIdAndIsReadFalse(Long memberId);

    int markAllAsReadByMemberId(Long memberId);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);

    boolean existsByMemberIdAndReferenceKey(Long memberId, String referenceKey);
}
```

주의:
- 단일 type 검색보다 `typeIn` 이 더 유용합니다.
- 이유: UI의 “마감임박” 필터는 `DEADLINE_APPROACHING + DEADLINE_TODAY` 를 함께 포함해야 하기 때문입니다.

## NotificationPreference 엔티티

### 필드
- `id`
- `memberId`
- `deadlineAlert`
- `statusChangeAlert`
- `newCampaignAlert`
- `preferredCategories`
- `createdAt`
- `updatedAt`

### 규칙
- `memberId` unique
- 기본값:
  - `deadlineAlert = true`
  - `statusChangeAlert = true`
  - `newCampaignAlert = false`
- `preferredCategories` 는 캠페인 카테고리 code 를 쉼표 구분 문자열로 저장
  - 예: `FOOD,BEAUTY,TRAVEL`

### 편의 메서드
```java
public List<String> getPreferredCategoryList() { ... }
public void update(boolean deadlineAlert, boolean statusChangeAlert,
                   boolean newCampaignAlert, String preferredCategories) { ... }
```

### preferredCategories 파싱 규칙
- 쉼표 분리
- trim
- 빈 값 제거
- 중복 제거
- 순서 유지

## NotificationPreferenceRepository
```java
public interface NotificationPreferenceRepository {
    NotificationPreference save(NotificationPreference preference);
    Optional<NotificationPreference> findByMemberId(Long memberId);

    List<NotificationPreference> findByMemberIdIn(Collection<Long> memberIds);

    List<NotificationPreference> findByNewCampaignAlertTrue();
}
```

주의:
- `preferredCategoriesContaining(...)` 같은 문자열 contains 쿼리는 사용하지 않습니다.
- 카테고리 포함 여부는 애플리케이션에서 `getPreferredCategoryList()` 기반으로 정확히 비교합니다.

## 산출물
- `Notification.java`
- `NotificationType.java`
- `NotificationRepository.java`
- `NotificationSpringDataJpaRepository.java`
- `NotificationJpaRepository.java`
- `NotificationPreference.java`
- `NotificationPreferenceRepository.java`
- `NotificationPreferenceSpringDataJpaRepository.java`
- `NotificationPreferenceJpaRepository.java`

---

# Phase 49. 알림 생성 이벤트 + 스케줄러

## 목표
Spring Event와 스케줄러를 이용해 알림을 자동 생성합니다.

## 이벤트 1. 캠페인 상태 변경
Campaign BC 에서 발행:
```java
public class CampaignStatusChangedEvent {
    private Long campaignId;
    private String campaignTitle;
    private String sourceCode;
    private String previousStatus;
    private String newStatus;
}
```

### 발행 조건
- 기존 캠페인 status 가 실제로 변경된 경우만 발행
- 사용자에게 의미 있는 상태 변화만 알림 대상으로 처리
  - 권장: `CLOSED`, `ANNOUNCED`, `COMPLETED`

## 이벤트 2. 신규 캠페인 등록
Campaign BC 에서 발행:
```java
public class NewCampaignCreatedEvent {
    private Long campaignId;
    private String campaignTitle;
    private String sourceCode;
    private String sourceName;
    private String category;
}
```

## 이벤트 3. 회원가입 완료
Member BC 에서 발행:
```java
public class MemberRegisteredEvent {
    private Long memberId;
}
```

## Notification BC 리스너

### CampaignNotificationListener
```java
@Component
public class CampaignNotificationListener {

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(CampaignStatusChangedEvent event) { ... }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewCampaign(NewCampaignCreatedEvent event) { ... }
}
```

### 상태 변경 알림 로직
1. 해당 캠페인을 찜한 회원 ID 목록 조회
2. 회원별 NotificationPreference 조회
3. `statusChangeAlert=true` 회원만 대상
4. `referenceKey = STATUS_CHANGED:{campaignId}:{newStatus}`
5. 이미 존재하면 skip
6. Notification 일괄 생성

### 신규 캠페인 알림 로직
1. `newCampaignAlert=true` 인 설정 조회
2. `preferredCategories` 에 이벤트 category 가 포함된 회원만 대상
3. `referenceKey = NEW_CAMPAIGN:{campaignId}`
4. Notification 일괄 생성

## MemberNotificationListener
회원가입 직후 기본 설정 자동 생성:
```java
@Component
public class MemberNotificationListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRegistered(MemberRegisteredEvent event) { ... }
}
```

## DeadlineNotificationScheduler
```java
@Component
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true")
public class DeadlineNotificationScheduler {
    @Scheduled(cron = "${notification.deadline-cron}")
    public void sendDeadlineAlerts() { ... }
}
```

### 로직
#### D-3
- `applyEndDate == today + deadlineDaysBefore`
- `status == RECRUITING`
- 찜한 회원 대상
- `deadlineAlert = true`
- `referenceKey = DEADLINE_APPROACHING:{campaignId}:{applyEndDate}`

#### D-Day
- `applyEndDate == today`
- `status == RECRUITING`
- 찜한 회원 대상
- `deadlineAlert = true`
- `referenceKey = DEADLINE_TODAY:{campaignId}:{applyEndDate}`

## NotificationCleanupScheduler
```java
@Component
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true")
public class NotificationCleanupScheduler {
    @Scheduled(cron = "${notification.cleanup-cron}")
    public void cleanupOldNotifications() { ... }
}
```

### 로직
- `now - 30 days` 이전 알림 삭제

## Async 설정
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() { ... }
}
```

## NotificationProperties
```yaml
notification:
  enabled: true
  deadline-cron: "0 0 9 * * *"
  cleanup-cron: "0 0 3 * * *"
  deadline-days-before: 3
```

## 다른 BC Repository 확장
### BookmarkRepository
- `List<Long> findMemberIdsByCampaignId(Long campaignId)`

### CampaignRepository
- `List<Campaign> findRecruitingByApplyEndDate(LocalDate date)`

## RegisterMemberService 수정
- 회원가입 완료 후 `MemberRegisteredEvent` 발행

## CrawlingOrchestrator 수정
- 상태 변경 시 `CampaignStatusChangedEvent`
- 신규 등록 시 `NewCampaignCreatedEvent`

## 산출물
- `CampaignStatusChangedEvent.java`
- `NewCampaignCreatedEvent.java`
- `MemberRegisteredEvent.java`
- `CampaignNotificationListener.java`
- `MemberNotificationListener.java`
- `DeadlineNotificationScheduler.java`
- `NotificationCleanupScheduler.java`
- `NotificationProperties.java`
- `AsyncConfig.java`
- `CrawlingOrchestrator.java` (수정)
- `RegisterMemberService.java` 또는 관련 회원가입 서비스 (수정)
- `BookmarkRepository.java` (수정)
- `CampaignRepository.java` (수정)
- `application-local.yml` (수정)

---

# Phase 50. 알림 조회 / 읽음 / 설정 Application 서비스

## 목표
알림 조회, 읽음 처리, 알림 설정 유스케이스를 구현합니다.

## 유스케이스 1. 미읽음 카운트
- 입력: `memberId`
- 출력: `UnreadCountInfo`

## 유스케이스 2. 알림 목록 조회
- 입력: `NotificationSearchCommand`
  - `memberId`
  - `typeGroup` nullable
  - `page`
  - `size`

## typeGroup 규칙
UI 필터에 맞춰 아래 그룹을 사용합니다.

- `ALL`
- `DEADLINE`
  - `DEADLINE_APPROACHING`
  - `DEADLINE_TODAY`
- `STATUS_CHANGED`
- `NEW_CAMPAIGN`

즉, API는 개별 enum type 대신 **그룹 필터**를 지원합니다.

## 출력 DTO
### NotificationInfo
- `id`
- `type`
- `typeDisplayName`
- `title`
- `message`
- `campaignId`
- `sourceCode`
- `isRead`
- `createdAt`
- `timeAgo`

### NotificationListInfo
- `notifications`
- `totalCount`
- `totalPages`
- `currentPage`
- `hasNext`
- `unreadCount`

## 유스케이스 3. 개별 읽음 처리
- 입력: `memberId`, `notificationId`
- 본인 알림인지 확인 후 `markAsRead()`

## 유스케이스 4. 전체 읽음 처리
- 입력: `memberId`
- 해당 회원 알림 전체 읽음 처리

## 유스케이스 5. 알림 설정 조회
- 입력: `memberId`

### NotificationPreferenceInfo
- `deadlineAlert`
- `statusChangeAlert`
- `newCampaignAlert`
- `preferredCategories`
- `availableCategories`

## 유스케이스 6. 알림 설정 변경
- 입력: `UpdatePreferenceCommand`
  - `memberId`
  - `deadlineAlert`
  - `statusChangeAlert`
  - `newCampaignAlert`
  - `preferredCategories`

### 설정 규칙
- `preferredCategories` 는 카테고리 code 목록
- `newCampaignAlert=false` 여도 categories 값은 저장 유지 가능
- 단, UI에서는 `newCampaignAlert=false` 이면 categories 선택 영역 비활성화

## 상대 시간 유틸
```java
public class TimeAgoUtil {
    public static String format(LocalDateTime dateTime) { ... }
}
```

### 규칙
- 1분 미만 → `방금 전`
- N분 전
- N시간 전
- N일 전
- 7일 이상 → `yyyy.MM.dd`

## 커스텀 예외
- `NotificationNotFoundException`
- `NotificationAccessDeniedException`

## 산출물
- `NotificationSearchCommand.java`
- `NotificationTypeGroup.java`
- `NotificationInfo.java`
- `NotificationListInfo.java`
- `UnreadCountInfo.java`
- `NotificationPreferenceInfo.java`
- `UpdatePreferenceCommand.java`
- `NotificationService.java`
- `NotificationPreferenceService.java`
- `TimeAgoUtil.java`
- `NotificationNotFoundException.java`
- `NotificationAccessDeniedException.java`

---

# Phase 51. 알림 API

## 목표
알림 관련 서비스를 REST API 로 노출합니다.

## 공통 규칙
- `/api/v1/notifications/**`
- 모두 인증 필요

## API 1. 미읽음 카운트
- `GET /api/v1/notifications/unread-count`

응답:
```json
{
  "count": 3
}
```

## API 2. 알림 목록
- `GET /api/v1/notifications`

### Query Parameters
- `typeGroup`
  - `ALL`, `DEADLINE`, `STATUS_CHANGED`, `NEW_CAMPAIGN`
- `page`
- `size`

응답:
```json
{
  "notifications": [
    {
      "id": 1,
      "type": "DEADLINE_APPROACHING",
      "typeDisplayName": "마감 임박",
      "title": "[레뷰] 강남 맛집 체험단",
      "message": "마감이 3일 남았습니다.",
      "campaignId": 42,
      "sourceCode": "REVU",
      "isRead": false,
      "createdAt": "2026-03-15T09:00:00",
      "timeAgo": "2시간 전"
    }
  ],
  "totalCount": 15,
  "totalPages": 1,
  "currentPage": 0,
  "hasNext": false,
  "unreadCount": 3
}
```

## API 3. 개별 읽음 처리
- `PATCH /api/v1/notifications/{id}/read`

## API 4. 전체 읽음 처리
- `PATCH /api/v1/notifications/read-all`

## API 5. 알림 설정 조회
- `GET /api/v1/notifications/preferences`

## API 6. 알림 설정 변경
- `PUT /api/v1/notifications/preferences`

## 에러 응답
- 알림 미존재 → `404 NOTIFICATION_NOT_FOUND`
- 본인 알림 아님 → `403 NOTIFICATION_ACCESS_DENIED`
- 미인증 → `401 UNAUTHORIZED`

## DTO
- `NotificationListResponse.java`
- `NotificationItemResponse.java`
- `UnreadCountResponse.java`
- `NotificationPreferenceResponse.java`
- `UpdatePreferenceRequest.java`
- `MessageResponse.java` 재사용 가능

## SecurityConfig
- `/api/v1/notifications/**` → authenticated

## 산출물
- `NotificationController.java`
- `NotificationPreferenceController.java` 또는 통합 컨트롤러
- `NotificationListResponse.java`
- `NotificationItemResponse.java`
- `UnreadCountResponse.java`
- `NotificationPreferenceResponse.java`
- `UpdatePreferenceRequest.java`
- `GlobalExceptionHandler.java` (수정)
- `SecurityConfig.java` (수정)

---

# Phase 52. React 알림 UI

## 목표
헤더 알림 벨, 드롭다운, 전체 알림 페이지, 알림 설정 UI를 구현합니다.

## 1. Header 알림 벨
- 로그인 상태일 때만 표시
- 미읽음 카운트 표시
- 0이면 숨김

## useUnreadCount
### 책임
- 60초 폴링
- 탭 숨김 상태면 중지
- 탭 복귀 시 즉시 refresh
- 읽음 처리 후 즉시 카운트 반영 가능

## 2. 알림 드롭다운
- 벨 클릭 시 열림
- 최근 알림 5건 표시
- 드롭다운은 **기존 목록 API를 `page=0,size=5`로 호출**해서 사용
- [모두 읽음]
- [전체 알림 보기]
- 바깥 클릭 시 닫힘

## 드롭다운 알림 아이템
- 읽음/미읽음 점 표시
- 소스 느낌이 드는 제목 표시
- 본문 1줄 말줄임
- 상대 시간 표시
- 클릭 시:
  1. 읽음 처리
  2. `campaignId` 있으면 `/campaigns/{campaignId}` 이동

## 3. 전체 알림 페이지
- `/notifications`
- `ProtectedRoute`
- 필터:
  - 전체
  - 마감임박
  - 상태변경
  - 신규캠페인
- 페이지네이션
- 모두 읽음 버튼
- 빈 상태 표시

## 4. 마이페이지 알림 설정 탭
- `/mypage?tab=notification-settings`
- 토글:
  - 마감 임박 알림
  - 상태 변경 알림
  - 신규 캠페인 알림
- 관심 카테고리 다중 선택
- `newCampaignAlert=false` 이면 카테고리 선택 비활성화
- 저장 시 토스트

## 프로젝트 구조
```text
/frontend
  /src
    /pages
      NotificationsPage.tsx
      MyPage.tsx
    /components
      Header.tsx
      NotificationBell.tsx
      NotificationDropdown.tsx
      NotificationItem.tsx
      NotificationList.tsx
      NotificationPreferenceForm.tsx
    /api
      notificationApi.ts
    /hooks
      useUnreadCount.ts
      useNotifications.ts
      useNotificationPreference.ts
    /types
      notification.ts
    /styles
      NotificationBell.module.css
      NotificationDropdown.module.css
      NotificationItem.module.css
      NotificationsPage.module.css
      NotificationPreference.module.css
    App.tsx
```

## useNotifications 훅
- 목록 로드
- 필터 변경
- 페이지 변경
- 개별 읽음
- 전체 읽음
- unreadCount 동기화

## useNotificationPreference 훅
- 설정 조회
- 설정 저장
- loading / error / success 상태 관리

## 추가 요구사항
- `document.visibilitychange` 활용
- 드롭다운과 UserDropdown 이 동시에 열리지 않도록 적절히 제어하면 좋음
- 알림 클릭 후 상세로 이동 시 드롭다운 닫힘
- 전체 알림 페이지에서 읽음 상태는 즉시 반영
- 벨 아이콘은 관리자/일반 사용자 모두 로그인 상태면 표시

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의

---

# Phase 53. 알림 기능 테스트

## 목표
이벤트, 스케줄러, 서비스, API, 프론트 훅까지 전체 알림 흐름을 테스트합니다.

## 테스트 범위

### 1. 도메인 단위 테스트
- `Notification`
  - 생성
  - `markAsRead()`
  - 재호출 시 readAt 유지
- `NotificationPreference`
  - 기본값
  - `getPreferredCategoryList()`
- `TimeAgoUtil`
  - 방금 전 / 분 / 시간 / 일 / 7일 이상 날짜

## 2. 이벤트 리스너 테스트
- `CampaignNotificationListener`
  - 상태 변경 알림 생성
  - 설정 OFF 제외
  - referenceKey 중복 방지
  - 신규 캠페인 알림 생성
- `MemberNotificationListener`
  - 회원가입 후 기본 설정 생성

## 3. 스케줄러 테스트
- D-3 → `DEADLINE_APPROACHING`
- D-Day → `DEADLINE_TODAY`
- 찜 안 한 회원 제외
- `deadlineAlert=false` 제외
- 중복 생성 방지
- 30일 지난 알림 삭제

## 4. Application 서비스 테스트
- 미읽음 카운트
- 목록 조회
- 그룹 필터 조회
- 개별 읽음
- 전체 읽음
- 본인 아닌 알림 접근 거부
- 설정 조회/수정

## 5. API 통합 테스트
- unread-count
- 목록
- read
- read-all
- preferences 조회/수정
- 미인증 401
- 본인 아닌 알림 403

## 6. 이벤트 통합 테스트
- 크롤링 → 상태 변경 이벤트 → 알림 생성
- 회원가입 → 이벤트 → NotificationPreference 생성

## 7. 프론트 테스트
- `useUnreadCount`
  - 폴링
  - visibilitychange
- `useNotifications`
  - 목록 / 필터 / 읽음
- `useNotificationPreference`
  - 조회 / 저장

## 테스트 도구
- Backend: JUnit 5, Mockito, AssertJ
- Frontend: Vitest, React Testing Library

## 산출물
- `NotificationTest.java`
- `NotificationPreferenceTest.java`
- `TimeAgoUtilTest.java`
- `CampaignNotificationListenerTest.java`
- `MemberNotificationListenerTest.java`
- `DeadlineNotificationSchedulerTest.java`
- `NotificationCleanupSchedulerTest.java`
- `NotificationServiceTest.java`
- `NotificationPreferenceServiceTest.java`
- `NotificationControllerIntegrationTest.java`
- `NotificationEventIntegrationTest.java`
- `useUnreadCount.test.ts`
- `useNotifications.test.ts`
- `useNotificationPreference.test.ts`

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
| 48~53 | 알림 | Notification |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"알림 기능 프로젝트 명세를 완벽히 이해했습니다. Phase 48 진행을 시작할까요?"**dmd 