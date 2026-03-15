당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 관리자 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 + 마이페이지 + 찜 + 크롤링 자동화 기능(Phase 1~36)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **6개의 Phase(Phase 37~42)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"관리자 기능 프로젝트 명세를 완벽히 이해했습니다. Phase 37 진행을 시작할까요?"**

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

Phase 1~36에서 다음 기능이 이미 구현되어 있습니다.

## 기존 구현 요약
- Member BC
  - 회원가입
  - 로그인 / JWT
  - 마이페이지
- Campaign BC
  - 목록 조회
  - 상세 조회
  - 찜
  - 크롤링 자동화
- `/api/v1/admin/crawling/**`
  - 현재는 인증만 체크하거나, 아직 ADMIN 권한 분리가 완전하지 않을 수 있음
- `MemberStatus`
  - `ACTIVE`
  - `INACTIVE`
  - `WITHDRAWN`
- `MemberRole`
  - 아직 없음
  - 이번 Phase에서 추가

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

1. **실용적 DDD**를 적용합니다.
2. `MemberRole` 은 **Member BC 확장**으로 구현합니다.
3. `Admin BC` 는 자체 Aggregate Root 를 새로 만들지 않고, **기존 Member / Campaign / Bookmark / CrawlingLog 데이터를 관리하는 application/interfaces 중심 BC** 로 설계합니다.
4. 즉, Admin BC 는 기존 BC 의 repository 와 domain behavior 를 활용하는 **관리 orchestration 계층**입니다.
5. `interfaces` DTO 와 `application` DTO 를 분리합니다.
6. 역할 기반 권한은 **RBAC(Role-Based Access Control)** 로 구현합니다.
7. 권한 수준은 이번 Phase 에서 **`USER`, `ADMIN` 2단계만** 사용합니다.
8. `/api/v1/admin/**` 는 **ADMIN 전용**입니다.
9. JWT Access Token 에 `role` claim 을 포함하고, SecurityContext 에 `GrantedAuthority` 를 부여합니다.
10. 비로그인 사용자는 `/api/v1/admin/**` 접근 시 `401`, 로그인했지만 USER 역할이면 `403` 이어야 합니다.
11. 관리자 UI 는 일반 사용자 UI 와 분리된 `/admin/*` 라우트 체계로 구현합니다.
12. 프론트 권한 체크는 **AuthContext 의 `user.role` 기반**으로 수행하며, 단순 JWT 문자열 파싱에만 의존하지 않습니다.
13. 초기 관리자 계정은 local 환경에서 자동 생성 가능하도록 구성합니다.

---

# 이번 범위에서 추가

- **Admin Bounded Context** 신규 생성
- **RBAC**
  - `MemberRole`
  - JWT role claim
  - Spring Security role-based authorization
- 관리자 전용 API
  - 회원 관리
  - 캠페인 관리
  - 크롤링 관리
  - 대시보드 통계
- 관리자 React 페이지
  - `/admin`
  - `/admin/members`
  - `/admin/campaigns`
  - `/admin/crawling`
- 초기 관리자 계정 생성

---

# 이번 범위에서 제외

구현하지 마세요.

- Permission 레벨의 세분화 권한
- Audit Log / 감사 로그
- 관리자 2FA
- 크롤링 소스 동적 관리
- 관리자용 실시간 웹소켓 대시보드
- 조직/팀 단위 권한
- 멀티 테넌시

---

# RBAC 설계

## MemberRole
`Member` 엔티티에 역할 필드를 추가합니다.

### enum
- `USER` → `일반 사용자`
- `ADMIN` → `관리자`

## 역할 규칙
- 기본 회원가입 시 role 은 `USER`
- 초기 관리자 계정만 `ADMIN`
- 관리자만 `/api/v1/admin/**` 접근 가능
- 관리자도 일반 API 와 마이페이지 사용 가능

## 권한 매트릭스

| 경로 | 비로그인 | USER | ADMIN |
|------|---------|------|-------|
| `GET /api/v1/campaigns/**` | 허용 | 허용 | 허용 |
| `POST /api/v1/members/signup` | 허용 | - | - |
| `POST /api/v1/auth/login` | 허용 | - | - |
| `POST /api/v1/auth/refresh` | 허용 | - | - |
| `GET /api/v1/members/me/**` | 거부 | 허용 | 허용 |
| `POST /api/v1/campaigns/*/bookmark` | 거부 | 허용 | 허용 |
| `/api/v1/admin/**` | 401 | 403 | 허용 |

주의:
- `/api/v1/auth/logout` 는 인증 필요입니다.
- USER 가 `/api/v1/admin/**` 접근 시 `401` 이 아니라 `403 ACCESS_DENIED` 여야 합니다.

---

# 관리자 페이지 레이아웃 기준

- 상단 관리자 전용 헤더
  - 로고
  - "관리자"
  - 관리자 닉네임
  - 로그아웃
- 좌측 사이드바
  - 대시보드
  - 회원 관리
  - 캠페인 관리
  - 크롤링 관리
  - 구분선
  - 홈으로 돌아가기
- 우측 콘텐츠 영역
- Footer 는 단순 문구 수준으로 유지 가능

---

# Phase 37. Member 도메인 역할(Role) 확장 + RBAC 인프라

## 목표
Member BC 에 역할 개념을 추가하고, Spring Security 에 RBAC 를 적용합니다.

## MemberRole
`member/domain/MemberRole.java`

```java
public enum MemberRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String displayName;
}
```

## Member 엔티티 수정
- `role` 필드 추가
- `@Enumerated(EnumType.STRING)`
- `nullable = false`
- 기본값은 `USER`

예시:
```java
@Column(nullable = false)
@Enumerated(EnumType.STRING)
private MemberRole role = MemberRole.USER;

public void changeRole(MemberRole newRole) {
    this.role = newRole;
}
```

주의:
- 자기 자신의 역할 변경 금지는 **Application Service 에서** 방어합니다.
- 도메인 메서드는 상태 변경 자체만 담당합니다.

## JWT 확장
`JwtTokenProvider` 수정

### Access Token claims
- `sub`: memberId
- `email`
- `role`

예:
```json
{
  "sub": "1",
  "email": "admin@experienceplatform.com",
  "role": "ADMIN"
}
```

### 필요한 메서드
- `String getRole(String token)` 또는 동등 기능
- 기존 memberId 추출 로직 유지

## JwtAuthenticationFilter 수정
- role claim 추출
- `SimpleGrantedAuthority("ROLE_ADMIN")` 또는 `ROLE_USER` 부여
- 기존 principal 구조(`AuthenticatedMember`) 에 `role` 필드 포함

## SecurityConfig 수정
- 기존 permitAll 유지
- `/api/v1/admin/**` → `hasRole("ADMIN")`
- 그 외 authenticated 정책은 기존 구조 유지

## AccessDeniedHandler
`AdminAccessDeniedHandler` 추가

### 역할
- 인증은 되었으나 권한 없는 경우 `403`
- 공통 에러 포맷 반환

예시:
```json
{
  "code": "ACCESS_DENIED",
  "message": "관리자 권한이 필요합니다.",
  "timestamp": "2026-03-15T10:00:00",
  "path": "/api/v1/admin/..."
}
```

주의:
- 미인증은 기존 `AuthenticationEntryPoint` 가 `401` 처리
- 인증됨 + 권한 없음은 `AccessDeniedHandler` 가 `403` 처리

## 초기 관리자 계정
추천 방식: `ApplicationRunner`

### 정책
- `local` 프로파일에서만 실행
- 이미 존재하면 생성하지 않음
- `PasswordEncoder` 로 암호화 후 저장

### 초기 계정
- email: `admin@experienceplatform.com`
- password: `Admin1234!`
- nickname: `관리자`
- role: `ADMIN`
- status: `ACTIVE`

## 기존 API 응답 확장
아래 응답/DTO 에 `role`, `roleDisplayName` 추가가 필요합니다.

- `GET /api/v1/members/me`
- `MemberProfile`
- `MemberProfileResponse`
- 필요 시 `MemberResponse`
- AuthContext user 타입

주의:
- role 정보는 로그인 직후와 새로고침 복원 흐름에서도 유지되어야 합니다.

## 프론트 AuthContext 수정
- `user.role`
- `user.roleDisplayName` 또는 최소 `role`
- `isAdmin` 편의 속성 추가

## 산출물
- `MemberRole.java`
- `Member.java` (수정)
- `AuthenticatedMember.java` (수정)
- `JwtTokenProvider.java` (수정)
- `JwtAuthenticationFilter.java` (수정)
- `SecurityConfig.java` (수정)
- `AdminAccessDeniedHandler.java`
- `AdminInitializer.java`
- `MemberProfile.java` (수정)
- `MemberProfileResponse.java` (수정)
- `MemberResponse.java` 또는 관련 DTO (필요 시 수정)
- 프론트 `AuthContext` 관련 코드 (수정)
- 프론트 타입 정의 (수정)

---

# Phase 38. 관리자 회원 관리 API

## 목표
관리자가 회원 목록 조회, 상세 조회, 상태 변경, 역할 변경을 수행할 수 있는 API 를 구현합니다.

## Admin BC 구조
```text
admin/
  application/
    member/
      AdminMemberService.java
      AdminMemberSearchCommand.java
      AdminMemberSearchCondition.java
      AdminMemberInfo.java
      AdminMemberDetailInfo.java
  interfaces/
    member/
      AdminMemberController.java
      AdminMemberSearchRequest.java
      AdminMemberListResponse.java
      AdminMemberDetailResponse.java
      ChangeMemberStatusRequest.java
      ChangeMemberRoleRequest.java
```

주의:
- Admin BC 는 **자체 Member 도메인을 새로 만들지 않습니다.**
- 기존 `MemberRepository`, `BookmarkRepository`, `RefreshTokenRepository` 등을 활용합니다.

## API 1. 회원 목록 조회
- `GET /api/v1/admin/members`
- ADMIN 전용

### Query Parameters
- `keyword`
  - 이메일 / 닉네임 검색
- `status`
  - `ACTIVE`, `INACTIVE`, `WITHDRAWN`
- `role`
  - `USER`, `ADMIN`
- `page`
  - 기본 0
- `size`
  - 기본 20, 최대 100
- `sort`
  - `latest`, `oldest`

### 응답
- `members`
- `totalCount`
- `totalPages`
- `currentPage`
- `hasNext`

각 항목:
- `id`
- `email`
- `nickname`
- `role`
- `roleDisplayName`
- `status`
- `statusDisplayName`
- `createdAt`

## API 2. 회원 상세 조회
- `GET /api/v1/admin/members/{memberId}`
- ADMIN 전용

### 응답
- `id`
- `email`
- `nickname`
- `role`
- `roleDisplayName`
- `status`
- `statusDisplayName`
- `createdAt`
- `updatedAt`
- `bookmarkCount`

## API 3. 회원 상태 변경
- `PATCH /api/v1/admin/members/{memberId}/status`
- ADMIN 전용

### 요청
```json
{
  "status": "INACTIVE"
}
```

### 비즈니스 규칙
- 자기 자신의 상태는 변경 불가
- `WITHDRAWN -> ACTIVE` 복구 허용
- 상태 변경 성공 시 해당 회원 Refresh Token 삭제
  - 강제 로그아웃 효과

## API 4. 회원 역할 변경
- `PATCH /api/v1/admin/members/{memberId}/role`
- ADMIN 전용

### 요청
```json
{
  "role": "ADMIN"
}
```

### 비즈니스 규칙
- 자기 자신의 역할은 변경 불가
- 역할 변경 성공 시 Refresh Token 삭제
  - 새 JWT 에 변경된 role 반영 유도

## 검색 구현
`MemberRepository` 확장

### searchByCondition
- keyword → `email LIKE OR nickname LIKE`
- status 필터
- role 필터
- JPA Specification 사용

## 커스텀 예외
- `CannotModifySelfException`

## 산출물
- `AdminMemberService.java`
- `AdminMemberSearchCommand.java`
- `AdminMemberSearchCondition.java`
- `AdminMemberInfo.java`
- `AdminMemberDetailInfo.java`
- `AdminMemberController.java`
- `AdminMemberSearchRequest.java`
- `AdminMemberListResponse.java`
- `AdminMemberDetailResponse.java`
- `ChangeMemberStatusRequest.java`
- `ChangeMemberRoleRequest.java`
- `CannotModifySelfException.java`
- `MemberSpecification.java` 또는 기존 Member 검색용 Specification (수정/추가)
- `MemberRepository.java` (수정)
- 관련 JPA 구현체 (수정)
- `GlobalExceptionHandler.java` (수정)

---

# Phase 39. 관리자 캠페인 관리 API

## 목표
관리자가 캠페인을 직접 관리하는 CRUD API 를 구현합니다.

## Admin BC 구조
```text
admin/
  application/
    campaign/
      AdminCampaignService.java
      AdminCampaignCreateCommand.java
      AdminCampaignUpdateCommand.java
      AdminCampaignInfo.java
  interfaces/
    campaign/
      AdminCampaignController.java
      AdminCampaignCreateRequest.java
      AdminCampaignUpdateRequest.java
      AdminCampaignListResponse.java
      AdminCampaignItemResponse.java
```

## API 1. 관리자 캠페인 목록 조회
- `GET /api/v1/admin/campaigns`
- ADMIN 전용

### 특징
- 공개 목록 API 와 달리 **모든 상태 조회 가능**
- 관리자 메타 정보 포함
  - `originalId`
  - `bookmarkCount`
  - `createdAt`
  - `updatedAt`

### Query Parameters
- `keyword`
- `sourceTypes`
- `categories`
- `status`
- `page`
- `size`
- `sort`

## API 2. 캠페인 수동 등록
- `POST /api/v1/admin/campaigns`
- ADMIN 전용

### 요청 필드
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

### 규칙
- `sourceType + originalId` 는 유일
- 수동 등록 데이터도 기존 Campaign 엔티티에 저장
- 수동 등록은 `originalId` 를 예: `manual-001` 같은 식으로 사용할 수 있음

## API 3. 캠페인 수정
- `PUT /api/v1/admin/campaigns/{id}`
- ADMIN 전용
- 전체 필드 수정 방식으로 구현

## API 4. 캠페인 삭제
- `DELETE /api/v1/admin/campaigns/{id}`
- ADMIN 전용

### 삭제 정책
- **하드 삭제**
- 해당 캠페인의 Bookmark 도 함께 삭제
- Cascade 로 안전하게 처리 가능하면 사용
- 그렇지 않으면 서비스에서 명시적으로 bookmark 삭제 후 campaign 삭제

## 에러 처리
- 캠페인 없음 → `404 CAMPAIGN_NOT_FOUND`
- `sourceType + originalId` 중복 → `409 DUPLICATE_CAMPAIGN`
- validation 실패 → `400 VALIDATION_FAILED`
- 권한 없음 → `403 ACCESS_DENIED`

## Repository 확장
- `CampaignRepository`
  - 관리자 검색용 메서드 추가 가능
  - `deleteById(Long id)` 확인/추가
- `BookmarkRepository`
  - `deleteByCampaignId(Long campaignId)` 확인/추가

## 산출물
- `AdminCampaignService.java`
- `AdminCampaignCreateCommand.java`
- `AdminCampaignUpdateCommand.java`
- `AdminCampaignInfo.java`
- `AdminCampaignController.java`
- `AdminCampaignCreateRequest.java`
- `AdminCampaignUpdateRequest.java`
- `AdminCampaignListResponse.java`
- `AdminCampaignItemResponse.java`
- `CampaignRepository.java` (수정)
- `BookmarkRepository.java` (수정)
- `GlobalExceptionHandler.java` (수정)

---

# Phase 40. 관리자 대시보드 API

## 목표
관리자 대시보드에 필요한 통계 요약 API 를 구현합니다.

## API
- `GET /api/v1/admin/dashboard`
- ADMIN 전용

## 응답 구조
### members
- `total`
- `active`
- `inactive`
- `withdrawn`
- `todaySignup`

### campaigns
- `total`
- `recruiting`
- `closed`
- `announced`
- `completed`
- `bySource`
  - `sourceType`
  - `sourceDisplayName`
  - `count`

### bookmarks
- `total`
- `todayCount`

### crawling
- `lastExecution`
- `recentResults`
  - 최근 실행 로그 3건 정도

### meta
- `generatedAt`

## 서비스
- `AdminDashboardService`
- `@Transactional(readOnly = true)`

## Repository 확장
### MemberRepository
- `countByStatus(MemberStatus)`
- `countByCreatedAtAfter(LocalDateTime)`

### CampaignRepository
- `countByStatus(CampaignStatus)`
- `countBySourceType()` 또는 projection 기반 집계

### BookmarkRepository
- `count()`
- `countByCreatedAtAfter(LocalDateTime)`

### CrawlingLogRepository
- 최근 로그 조회
- 예: `findTop3ByOrderByExecutedAtDesc()` 또는 pageable 기반 최근 조회

## DTO
- `DashboardInfo.java`
- `DashboardResponse.java`

## 산출물
- `AdminDashboardService.java`
- `DashboardInfo.java`
- `AdminDashboardController.java`
- `DashboardResponse.java`
- 관련 Repository 수정

---

# Phase 41. 관리자 React 페이지

## 목표
관리자 전용 React 페이지를 구현합니다.

## 라우팅
- `/admin`
- `/admin/members`
- `/admin/campaigns`
- `/admin/campaigns/new`
- `/admin/campaigns/:id/edit`
- `/admin/crawling`

모두 ADMIN 전용입니다.

## AdminRoute
- 비로그인 → `/login`
- 로그인했지만 USER → `/` 이동 + 권한 없음 알림
- ADMIN → 렌더링

## 관리자 레이아웃
`AdminLayout`
- 좌측 사이드바
- 우측 콘텐츠
- 상단 관리자 헤더
- 홈으로 돌아가기 링크 제공

## Header 변경
로그인 사용자가 ADMIN 이면 드롭다운 메뉴에 아래를 추가합니다.
- `관리자 페이지`
- `마이페이지`
- `로그아웃`

USER 는 기존처럼:
- `마이페이지`
- `로그아웃`

## 1. 대시보드 페이지
- 통계 카드
- 소스별 분포 시각화
- 최근 크롤링 결과 테이블
- 수동 새로고침 버튼

차트는 라이브러리 없이 CSS 기반 단순 바 차트로 구현하세요.

## 2. 회원 관리 페이지
- 검색
- 상태 필터
- 역할 필터
- 테이블
- 상태 변경 액션
- 역할 변경 액션
- 확인 모달
- 자기 자신은 액션 비활성화
- 페이지네이션

## 3. 캠페인 관리 페이지
- 관리자 전용 검색/필터
- 테이블
- 등록 버튼
- 수정 버튼
- 삭제 버튼
- 삭제 확인 모달

## 4. 캠페인 등록/수정 폼
- 별도 페이지로 구현
- 생성/수정 공용 폼 훅 가능
- validation 은 백엔드 규칙과 일치
- 성공 시 목록으로 이동 + 토스트

## 5. 크롤링 관리 페이지
- 전체 실행 버튼
- 소스별 실행 버튼
- 실행 중 표시
- 실행 결과 표시
- 최근 로그 테이블

## 프론트 구조
```text
/frontend
  /src
    /pages
      /admin
        AdminDashboardPage.tsx
        AdminMemberPage.tsx
        AdminCampaignPage.tsx
        AdminCampaignFormPage.tsx
        AdminCrawlingPage.tsx
    /components
      /admin
        AdminLayout.tsx
        AdminSidebar.tsx
        AdminRoute.tsx
        StatCard.tsx
        SimpleBarChart.tsx
        DataTable.tsx
        ConfirmModal.tsx
        MemberStatusBadge.tsx
        MemberRoleBadge.tsx
      Header.tsx
      UserDropdown.tsx
    /api
      adminApi.ts
    /hooks
      /admin
        useAdminDashboard.ts
        useAdminMembers.ts
        useAdminCampaigns.ts
        useAdminCampaignForm.ts
        useAdminCrawling.ts
    /types
      admin.ts
    /styles
      /admin
        AdminLayout.module.css
        AdminSidebar.module.css
        AdminDashboard.module.css
        AdminMember.module.css
        AdminCampaign.module.css
        AdminCrawling.module.css
        DataTable.module.css
        StatCard.module.css
    App.tsx
```

## 공통 컴포넌트
- `DataTable`
- `ConfirmModal`
- `StatCard`

## AuthContext 연계
- `isAdmin` 활용
- role 반영이 로그인 직후 / 새로고침 후 모두 유지되어야 함

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의

---

# Phase 42. 관리자 기능 테스트

## 목표
관리자 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 도메인 테스트
- `MemberRole`
  - displayName 매핑
- `Member.changeRole()`
  - 정상 변경
  - 기본 role USER 확인

### 2. JWT + Security 테스트
- role claim 포함 토큰 생성/추출
- ADMIN → `/api/v1/admin/**` 접근 허용
- USER → `/api/v1/admin/**` 접근 시 `403 ACCESS_DENIED`
- 미인증 → `/api/v1/admin/**` 접근 시 `401`

## 3. Application 서비스 테스트
### AdminMemberService
- 회원 검색
- 상태 변경
- 역할 변경
- 자기 자신 수정 방지
- RefreshToken 삭제 확인

### AdminCampaignService
- 등록
- 수정
- 삭제
- bookmark 연쇄 삭제
- 중복 캠페인 방지

### AdminDashboardService
- 통계 조합 확인

## 4. API 통합 테스트
### 회원 관리
- 목록 조회
- 상태 변경
- 역할 변경
- 자기 자신 수정 금지

### 캠페인 관리
- 등록 / 수정 / 삭제
- 미존재 → 404
- bookmark 연쇄 삭제 확인

### 대시보드
- 응답 구조 확인

### 크롤링 관리
- 기존 `/api/v1/admin/crawling/**` 에 대해 ADMIN/USER/미인증 권한 테스트

## 5. 프론트 테스트
- `AdminRoute`
- `useAdminMembers`
- `useAdminCampaignForm`

## 테스트 도구
- Backend: JUnit 5, Mockito, AssertJ
- Frontend: Vitest, React Testing Library

## 산출물
- `MemberRoleTest.java`
- `JwtTokenProviderRoleTest.java`
- `SecurityRbacIntegrationTest.java`
- `AdminMemberServiceTest.java`
- `AdminCampaignServiceTest.java`
- `AdminDashboardServiceTest.java`
- `AdminMemberControllerIntegrationTest.java`
- `AdminCampaignControllerIntegrationTest.java`
- `AdminDashboardControllerIntegrationTest.java`
- `AdminRoute.test.tsx`
- `useAdminMembers.test.ts`
- `useAdminCampaignForm.test.ts`

---

# 전체 Phase 누적 현황

| Phase | 기능 | BC |
|-------|------|----|
| 1~6 | 회원가입 | Member |
| 7~11 | 로그인 / JWT | Member |
| 12~16 | 캠페인 목록 조회 | Campaign |
| 17~21 | 캠페인 상세 페이지 | Campaign |
| 22~26 | 마이페이지 | Member |
| 27~31 | 찜 / 북마크 | Campaign |
| 32~36 | 크롤링 자동화 | Campaign |
| 37~42 | 관리자 기능 | Admin + Member 확장 |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"관리자 기능 프로젝트 명세를 완벽히 이해했습니다. Phase 37 진행을 시작할까요?"**