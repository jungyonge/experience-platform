당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JPA, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 마이페이지 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 기능(Phase 1~21)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 22~26)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"마이페이지 프로젝트 명세를 완벽히 이해했습니다. Phase 22 진행을 시작할까요?"**

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

Phase 1~21에서 다음 기능이 이미 구현되어 있습니다.

## 기존 구현 요약
- Member BC
  - 회원가입
  - 로그인
  - JWT 인증
  - `GET /api/v1/members/me`
- Campaign BC
  - 목록 조회
  - 상세 조회
- React 인증 상태 관리
  - `AuthContext`
  - `AuthProvider`
  - `ProtectedRoute`
- 공통 Header
  - 로고
  - 검색바
  - 로그인 / 회원가입 또는 닉네임 / 로그아웃

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
2. 마이페이지 기능은 기존 `Member` Bounded Context 를 확장합니다.
3. `Member` 는 `Member` Bounded Context 의 Aggregate Root 입니다.
4. Repository 인터페이스는 `domain`, 구현체는 `infrastructure` 에 둡니다.
5. `interfaces` DTO와 `application` DTO는 분리합니다.
6. 비밀번호 비교는 도메인이 아니라 **Application Service 에서 `PasswordEncoder.matches(raw, encoded)`** 로 수행합니다.
7. 비밀번호 변경 / 탈퇴는 모두 **현재 비밀번호 확인**을 거칩니다.
8. 회원 탈퇴는 하드 삭제가 아니라 **소프트 삭제**입니다.
9. 탈퇴 시 Refresh Token 을 삭제하여 **즉시 재발급 불가 상태**로 만듭니다.
10. 기존 Access Token 블랙리스트는 구현하지 않으므로, 탈퇴 성공 후 프론트는 즉시 로그아웃 처리해야 합니다.
11. 마이페이지 API는 모두 인증 필요입니다.
12. Header 의 닉네임 영역은 드롭다운 메뉴로 확장합니다.
13. 프론트의 validation regex 는 기존 회원가입/로그인 Phase 에서 사용한 정책을 재사용하고, 중복 정의를 피하세요.

---

# 이번 범위에서 추가

- 마이페이지 (`/mypage`)
  - 프로필 조회
  - 닉네임 수정
  - 비밀번호 변경
  - 회원 탈퇴
- Member 도메인 확장
  - 닉네임 변경
  - 비밀번호 변경
  - 회원 탈퇴
- 프로필 관리 API
- Header 에 마이페이지 진입점 추가
- Toast 알림
- 회원 탈퇴 모달

---

# 이번 범위에서 제외

구현하지 마세요.

- 찜 / 북마크 목록
- 프로필 이미지 업로드
- 이메일 변경
- 활동 내역 / 히스토리
- Access Token 블랙리스트
- 계정 복구 기능

---

# 공통 UX / 정책

## 비밀번호 변경 정책
- 현재 비밀번호를 반드시 입력해야 합니다.
- 새 비밀번호는 기존 회원가입 정책과 동일합니다.
  - 최소 8자 이상
  - 영문 포함
  - 숫자 또는 특수문자 포함
- 현재 비밀번호와 새 비밀번호가 동일하면 거부합니다.
- 새 비밀번호와 새 비밀번호 확인은 일치해야 합니다.

## 회원 탈퇴 정책
- 현재 비밀번호 입력으로 본인 확인
- 탈퇴 확인 모달 제공
- 탈퇴 처리 시 회원 상태를 `WITHDRAWN` 으로 변경
- Refresh Token 삭제
- 프론트는 성공 직후 Access Token / Refresh Token / AuthContext 상태를 모두 정리하고 로그아웃 처리
- 탈퇴 후 로그인 시도는 기존 인증 로직에 의해 `ACCOUNT_DISABLED` 또는 동등한 예외로 거부됩니다.

## 닉네임 수정 정책
- 기존 닉네임 정책과 동일
  - 한글 / 영문 / 숫자만 허용
  - 2~20자
- 중복 검사는 Application Service + DB Unique 로 이중 보장
- 현재 닉네임과 동일한 값이면 **변경 없이 성공 처리**
- 닉네임 변경 성공 시 Header 와 AuthContext 에 즉시 반영

## 마이페이지 접근 정책
- `/mypage` 는 `ProtectedRoute`
- 인증된 사용자만 접근 가능
- JWT subject 의 memberId 와 실제 수정 대상이 항상 동일해야 하며, 외부 memberId 파라미터를 받지 않습니다.

---

# Phase 22. Member 도메인 프로필 관리 확장

## 목표
기존 `Member` 도메인에 프로필 수정, 비밀번호 변경, 회원 탈퇴 관련 행위를 추가합니다.

## Member 엔티티 확장

### updatedAt 추가
- `Member` 엔티티에 `updatedAt` 필드를 추가합니다.  
  이미 존재하면 재사용하세요.
- `createdAt`, `updatedAt` 은 가능하면 `@PrePersist`, `@PreUpdate` 또는 동등한 방식으로 자동 관리하세요.
- 도메인 메서드는 비즈니스 상태 변경에 집중하고, timestamp 관리는 persistence lifecycle 에 맡겨도 됩니다.

### 닉네임 변경
```java
public void changeNickname(Nickname newNickname) {
    this.nickname = newNickname;
}
```

규칙:
- 닉네임 유일성 검증은 Application Service 에서 수행
- 현재와 동일 닉네임 여부 판단도 Application Service 에서 수행

### 비밀번호 변경
```java
public void changePassword(PasswordHash newPasswordHash) {
    this.passwordHash = newPasswordHash;
}
```

규칙:
- 현재 비밀번호 검증은 Application Service 에서 수행
- 도메인에는 이미 검증된 새 비밀번호 해시만 전달

### 회원 탈퇴
```java
public void withdraw() {
    if (this.status == MemberStatus.WITHDRAWN) {
        throw new AlreadyWithdrawnException();
    }
    this.status = MemberStatus.WITHDRAWN;
}
```

규칙:
- 이미 탈퇴한 회원의 재탈퇴 방어
- 소프트 삭제만 수행
- 하드 삭제는 하지 않음

## MemberStatus 확장
- `MemberStatus` 에 `displayName` 이 없으면 이번 Phase 에 추가하세요.
- 예시:
  - `ACTIVE` → `활성`
  - `INACTIVE` → `비활성`
  - `WITHDRAWN` → `탈퇴`

## MemberRepository 확장
기존 인터페이스에 아래 메서드가 없으면 추가하세요.

- `Optional<Member> findById(Long id)`
- `boolean existsByNickname(Nickname nickname)`

주의:
- 기존 저장소 구조를 불필요하게 전면 변경하지 마세요.
- 이미 Spring Data 인터페이스 + 구현체가 분리되어 있다면 그 구조를 유지하면서 필요한 메서드만 추가하세요.

## 커스텀 예외
- `MemberNotFoundException`
- `AlreadyWithdrawnException`
- `SamePasswordException`
- `CurrentPasswordMismatchException`

## 산출물
- `Member.java` (수정)
- `MemberStatus.java` (필요 시 수정)
- `MemberRepository.java` (수정)
- 관련 JPA 저장소 구현체 (수정)
- `MemberNotFoundException.java`
- `AlreadyWithdrawnException.java`
- `SamePasswordException.java`
- `CurrentPasswordMismatchException.java`

---

# Phase 23. 프로필 관리 Application 서비스

## 목표
프로필 조회, 닉네임 수정, 비밀번호 변경, 회원 탈퇴 유스케이스를 구현합니다.

## 서비스 설계
- 추천: 기존 회원가입 로직은 `MemberService`, 프로필 관리는 **`ProfileService`** 로 분리
- 트랜잭션은 Application Service 에서 관리
- 읽기 전용 조회에는 `@Transactional(readOnly = true)` 적용
- 쓰기 작업에는 일반 `@Transactional` 적용

## 유스케이스 1. 프로필 조회
### 입력
- `memberId: Long`

### 처리
1. Member 조회
2. 없으면 `MemberNotFoundException`
3. `MemberProfile` DTO 로 변환

### 출력: `MemberProfile`
- `id`
- `email`
- `nickname`
- `status`
- `statusDisplayName`
- `createdAt`

## 유스케이스 2. 닉네임 수정
### 입력
- `ChangeNicknameCommand`
  - `memberId`
  - `newNickname`

### 처리
1. Member 조회
2. 없으면 `MemberNotFoundException`
3. 현재 닉네임과 동일하면 저장 없이 성공 반환
4. 새 닉네임 중복 검사
5. `member.changeNickname(newNickname)`
6. 저장
7. 동시성 상황을 대비해 DB Unique 위반이 발생하면 `DuplicateNicknameException` 으로 변환

### 출력
- `MemberProfile`

## 유스케이스 3. 비밀번호 변경
### 입력
- `ChangePasswordCommand`
  - `memberId`
  - `currentPassword`
  - `newPassword`
  - `newPasswordConfirm`

### 처리
1. Member 조회
2. 없으면 `MemberNotFoundException`
3. 새 비밀번호와 확인 비밀번호 일치 검증
4. 새 비밀번호 정책 검증
5. 현재 비밀번호 매칭 검증
   - `PasswordEncoder.matches(currentPassword, member.getPasswordHash().getValue())`
   - 도메인에서 `matches()` 메서드를 호출하지 않음
6. 현재 비밀번호와 새 비밀번호 동일 여부 검사
   - 보통 `PasswordEncoder.matches(newPassword, member.getPasswordHash().getValue())` 로 확인 가능
7. 새 비밀번호 암호화
8. `member.changePassword(newPasswordHash)`
9. 저장

### 출력
- 없음 (`void`)

## 유스케이스 4. 회원 탈퇴
### 입력
- `WithdrawMemberCommand`
  - `memberId`
  - `currentPassword`

### 처리
1. Member 조회
2. 없으면 `MemberNotFoundException`
3. 현재 비밀번호 검증
4. `member.withdraw()`
5. 저장
6. 해당 Member 의 Refresh Token 삭제

### 출력
- 없음 (`void`)

## Application DTO
- `MemberProfile.java`
- `ChangeNicknameCommand.java`
- `ChangePasswordCommand.java`
- `WithdrawMemberCommand.java`

## 설계 원칙
- 모든 수정/삭제 유스케이스는 인증된 사용자 본인만 수행
- 현재 비밀번호 확인은 Application Service 책임
- 닉네임 중복은 사전 검사 + DB unique fallback
- 탈퇴 후 추가 변경을 막기 위해 도메인/서비스에서 현재 상태를 반영해 동작하도록 구현

## 산출물
- `MemberProfile.java`
- `ChangeNicknameCommand.java`
- `ChangePasswordCommand.java`
- `WithdrawMemberCommand.java`
- `ProfileService.java`

---

# Phase 24. 마이페이지 API

## 목표
프로필 관리 서비스를 REST API 로 노출합니다.

## 공통 규칙
- 모든 API 는 인증 필요
- JWT 에서 인증된 `memberId` 를 추출하여 사용
- 외부에서 memberId 를 request 로 받지 않습니다.

## 인증 정보 추출 방식
- 기존 Phase 9 에서 사용한 `AuthenticatedMember` principal 구조와 **일관되게** 사용하세요.
- 이번 Phase 에서는 새 ArgumentResolver 를 추가하지 말고, 우선 **`@AuthenticationPrincipal AuthenticatedMember`** 방식으로 통일합니다.

## API 1. 프로필 조회
- `GET /api/v1/members/me/profile`

### 성공 응답
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "블로거킴",
  "status": "ACTIVE",
  "statusDisplayName": "활성",
  "createdAt": "2026-03-14T10:00:00"
}
```

## API 2. 닉네임 수정
- `PATCH /api/v1/members/me/nickname`

### 요청
```json
{
  "nickname": "새닉네임"
}
```

### 성공 응답
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "새닉네임",
  "status": "ACTIVE",
  "statusDisplayName": "활성",
  "createdAt": "2026-03-14T10:00:00"
}
```

## API 3. 비밀번호 변경
- `PATCH /api/v1/members/me/password`

### 요청
```json
{
  "currentPassword": "oldPassword123!",
  "newPassword": "newPassword456!",
  "newPasswordConfirm": "newPassword456!"
}
```

### 성공 응답
```json
{
  "message": "비밀번호가 변경되었습니다."
}
```

## API 4. 회원 탈퇴
- `DELETE /api/v1/members/me`

주의:
- 이번 프로젝트에서는 `DELETE` 요청에 JSON body 를 허용합니다.
- axios/fetch 에서 request body 전달이 가능하도록 구현하세요.

### 요청
```json
{
  "currentPassword": "password123!"
}
```

### 성공 응답
```json
{
  "message": "회원 탈퇴가 완료되었습니다."
}
```

## 에러 응답 규칙

| 상황 | HTTP 코드 | code |
|------|-----------|------|
| 회원 미존재 | 404 | MEMBER_NOT_FOUND |
| 닉네임 중복 | 409 | DUPLICATE_NICKNAME |
| 닉네임 형식 오류 | 400 | VALIDATION_FAILED |
| 현재 비밀번호 불일치 | 400 | CURRENT_PASSWORD_MISMATCH |
| 새 비밀번호 정책 위반 | 400 | INVALID_PASSWORD |
| 새 비밀번호 확인 불일치 | 400 | PASSWORD_MISMATCH |
| 현재/새 비밀번호 동일 | 400 | SAME_PASSWORD |
| 이미 탈퇴한 회원 | 400 | ALREADY_WITHDRAWN |
| 미인증 | 401 | UNAUTHORIZED |

## Bean Validation
### ChangeNicknameRequest
- `@NotBlank`
- 기존 닉네임 정책 regex 재사용

### ChangePasswordRequest
- `@NotBlank` x 3
- 새 비밀번호에 대해 기존 비밀번호 정책 regex 재사용

### WithdrawRequest
- `@NotBlank currentPassword`

## interfaces DTO
- `MemberProfileResponse.java`
- `ChangeNicknameRequest.java`
- `ChangePasswordRequest.java`
- `WithdrawRequest.java`
- `MessageResponse.java`

## Controller 설계
- `ProfileController.java` 로 분리 구현
- 기존 `MemberController` 는 유지
- 인증 정보는 `@AuthenticationPrincipal AuthenticatedMember` 로 받음

## GlobalExceptionHandler 확장
- `MemberNotFoundException` → `404`
- `CurrentPasswordMismatchException` → `400`
- `SamePasswordException` → `400`
- `AlreadyWithdrawnException` → `400`

## Spring Security
- `/api/v1/members/me/profile`
- `/api/v1/members/me/nickname`
- `/api/v1/members/me/password`
- `/api/v1/members/me`

위 경로는 모두 인증 필요입니다.  
기존 설정에서 이미 보호된다면, 의도가 명확히 드러나도록 확인 또는 명시적 설정을 해주세요.

## 산출물
- `ProfileController.java`
- `MemberProfileResponse.java`
- `ChangeNicknameRequest.java`
- `ChangePasswordRequest.java`
- `WithdrawRequest.java`
- `MessageResponse.java`
- `GlobalExceptionHandler.java` (수정)
- `SecurityConfig.java` (확인/수정)

---

# Phase 25. React 마이페이지

## 목표
마이페이지 화면을 구현하고, Header 에 마이페이지 진입점을 추가합니다.

## 기술
- React + TypeScript + Vite
- CSS Modules
- 기존 AuthContext 재사용

## 라우팅
- `/mypage`
- 인증 필요 (`ProtectedRoute`)

## Header 변경
### 로그인 상태일 때
기존 단순 텍스트 대신 닉네임 드롭다운 메뉴로 변경합니다.

#### 구성
- `{닉네임}님 ▾`
- 드롭다운 메뉴:
  - `마이페이지`
  - `로그아웃`

#### 동작
- 닉네임 영역 클릭 시 열림 / 닫힘
- 메뉴 바깥 클릭 시 닫힘
- 메뉴에서 `마이페이지` 클릭 시 `/mypage`
- 메뉴에서 `로그아웃` 클릭 시 기존 로그아웃 로직 수행

## 마이페이지 구성

### 1. 프로필 정보 섹션
- 이메일
  - 표시만
  - 수정 불가
  - 시각적으로 비활성/잠금 느낌 표현
- 닉네임
  - 표시 + `[수정]`
  - 수정 클릭 시 인라인 편집 모드
  - 입력 필드 + 저장 / 취소
  - 클라이언트 검증은 기존 regex 상수 재사용
  - 저장 성공 시:
    - 표시 모드 복귀
    - 성공 토스트
    - **AuthContext 의 user.nickname 갱신**
  - 저장 실패 시:
    - 서버 에러 메시지 표시
- 가입일
  - 표시만

### 2. 비밀번호 변경 섹션
- 아코디언 형태
- 펼치면 아래 입력 노출
  - 현재 비밀번호
  - 새 비밀번호
  - 새 비밀번호 확인
- 새 비밀번호 정책 안내 표시
- 클라이언트 검증:
  - 정책 위반
  - 새 비밀번호 확인 불일치
- 성공 시:
  - 입력값 초기화
  - 성공 토스트
- 실패 시:
  - 서버 에러 표시

### 3. 계정 관리 섹션 (Danger Zone)
- 시각적으로 위험 영역 구분
- 안내 텍스트:
  - "탈퇴 시 계정이 비활성화되며 로그인할 수 없습니다."
  - "탈퇴 후에는 복구할 수 없습니다."
- `[회원 탈퇴]` 버튼
- 클릭 시 확인 모달 표시
  - 현재 비밀번호 입력
  - 취소 / 탈퇴하기
- 성공 시:
  - AuthContext 로그아웃
  - localStorage refresh token 제거
  - 홈(`/`) 이동
  - 탈퇴 완료 알림 표시

### 4. Toast 알림
- 성공 / 실패 알림 표시
- 화면 상단 또는 고정 위치
- 3초 후 자동 사라짐
- 별도 라이브러리 없이 직접 구현

## AuthContext 변경
- `updateUser(partial)` 액션 추가
- 닉네임 변경 성공 시 즉시 Header 반영
- 탈퇴 성공 시 기존 `logout()` 재사용 가능하도록 구조 정리

## 프로젝트 구조
```text
/frontend
  /src
    /pages
      MyPage.tsx
    /components
      Header.tsx
      UserDropdown.tsx
      ProfileSection.tsx
      NicknameEditor.tsx
      PasswordChangeSection.tsx
      AccountDangerSection.tsx
      WithdrawModal.tsx
      Toast.tsx
    /api
      profileApi.ts
    /hooks
      useProfile.ts
      usePasswordChange.ts
      useWithdraw.ts
      useToast.ts
    /context
      ToastContext.tsx
      ToastProvider.tsx
    /types
      profile.ts
    /styles
      MyPage.module.css
      UserDropdown.module.css
      ProfileSection.module.css
      NicknameEditor.module.css
      PasswordChangeSection.module.css
      AccountDangerSection.module.css
      WithdrawModal.module.css
      Toast.module.css
    App.tsx
```

## useProfile 훅 책임
- 프로필 조회
- 닉네임 수정
- 성공 시 AuthContext.user.nickname 갱신
- 로딩 / 에러 상태 관리

예시:
```typescript
function useProfile() {
  return {
    profile,
    isLoading,
    error,
    changeNickname,
    isChangingNickname,
    nicknameError,
  };
}
```

## usePasswordChange 훅 책임
- 폼 상태 관리
- 클라이언트 검증
- 제출 / 초기화
- 서버 에러 상태 관리

## useWithdraw 훅 책임
- 모달 열기 / 닫기
- 현재 비밀번호 상태
- 제출 상태
- 성공 시 상위 컴포넌트에서 logout + navigate 수행 가능하도록 결과 반환

## useToast 훅 / ToastContext
- 토스트 추가 / 제거
- 자동 닫힘 관리
- 전역 재사용 가능 구조

## 추가 요구사항
- 마이페이지는 페이지 최초 진입 시 프로필 1회 로드
- 비밀번호 변경 섹션은 기본 접힘 상태
- 드롭다운 / 모달은 Escape 키 또는 바깥 클릭 처리까지 고려하면 좋습니다.
- 기존 `apiClient` 인증 헤더 로직과 충돌하지 않도록 구현
- UI 문구는 과하지 않고 명확하게

## 산출물
- 위 구조의 모든 신규/수정 파일
- CSS Module 파일들
- API 연동 코드
- 타입 정의
- Toast 시스템

---

# Phase 26. 마이페이지 테스트

## 목표
마이페이지 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 도메인 단위 테스트
- `Member`
  - `changeNickname()` → 닉네임 변경
  - `changePassword()` → 비밀번호 해시 변경
  - `withdraw()` → 상태 `WITHDRAWN`
  - `withdraw()` 재호출 → `AlreadyWithdrawnException`
- `updatedAt` 자동 갱신은 lifecycle 정책에 맞게 검증 가능한 범위에서 확인

### 2. Application 서비스 테스트
#### 프로필 조회
- 정상 조회
- 존재하지 않는 ID → `MemberNotFoundException`

#### 닉네임 수정
- 정상 변경
- 동일 닉네임 → 변경 없이 성공
- 닉네임 중복 → `DuplicateNicknameException`
- DB Unique 위반 fallback → `DuplicateNicknameException`

#### 비밀번호 변경
- 정상 변경
- 새 비밀번호 확인 불일치 → `PasswordMismatchException`
- 비밀번호 정책 위반 → `InvalidPasswordException`
- 현재 비밀번호 불일치 → `CurrentPasswordMismatchException`
- 현재/새 비밀번호 동일 → `SamePasswordException`

#### 회원 탈퇴
- 정상 탈퇴 → 상태 변경 + Refresh Token 삭제
- 현재 비밀번호 불일치 → `CurrentPasswordMismatchException`
- 이미 탈퇴 → `AlreadyWithdrawnException`

Repository, PasswordEncoder, RefreshTokenRepository 는 Mock 사용

### 3. API 통합 테스트
- `GET /api/v1/members/me/profile`
  - 인증 + 정상 조회 → `200`
  - 미인증 → `401`
- `PATCH /api/v1/members/me/nickname`
  - 정상 변경 → `200`
  - 닉네임 중복 → `409`
  - 형식 오류 → `400`
  - 미인증 → `401`
- `PATCH /api/v1/members/me/password`
  - 정상 변경 → `200`
  - 현재 비밀번호 불일치 → `400`
  - 동일 비밀번호 → `400`
  - 미인증 → `401`
- `DELETE /api/v1/members/me`
  - 정상 탈퇴 → `200`
  - 현재 비밀번호 불일치 → `400`
  - 미인증 → `401`
  - 탈퇴 후 재로그인 → `403 ACCOUNT_DISABLED`

### 4. 프론트엔드 단위 테스트
- `useProfile`
  - 프로필 로드
  - 닉네임 변경 성공 / 실패
  - AuthContext nickname 갱신
- `usePasswordChange`
  - 폼 상태 관리
  - 클라이언트 검증
  - 제출 후 초기화
- `useWithdraw`
  - 모달 열기 / 닫기
  - 제출 동작
  - 성공 시 후속 처리 트리거 확인

## 테스트 설계 원칙
- 인증이 필요한 훅/컴포넌트 테스트에서는 필요한 provider 를 함께 감싸세요.
- 탈퇴 후 재로그인 불가 시나리오는 로그인 API 와 연계된 통합 테스트 형태로 검증하세요.
- 토스트 시스템은 필요하면 context 단위 또는 hook 단위로 검증 가능합니다.

## 테스트 도구
- Backend: JUnit 5, Mockito, AssertJ
- Frontend: Vitest, React Testing Library

## 산출물
- `MemberDomainExtensionTest.java`
- `ProfileServiceTest.java`
- `ProfileControllerIntegrationTest.java`
- `useProfile.test.ts`
- `usePasswordChange.test.ts`
- `useWithdraw.test.ts`

---

# 전체 Phase 누적 현황

| Phase | 기능 | BC |
|-------|------|----|
| 1~6 | 회원가입 | Member |
| 7~11 | 로그인 / JWT | Member |
| 12~16 | 캠페인 목록 조회 | Campaign |
| 17~21 | 캠페인 상세 페이지 | Campaign |
| 22~26 | 마이페이지 | Member |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"마이페이지 프로젝트 명세를 완벽히 이해했습니다. Phase 22 진행을 시작할까요?"**