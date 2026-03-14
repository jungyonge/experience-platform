좋습니다. 아래는 **로그인 / JWT 기능용 최종 마스터 프롬프트**입니다.
이대로 **복붙해서 AI에 넣으면 되는 형태**로 정리했습니다.

````markdown
당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, Spring Security, JWT, React 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 로그인 / JWT 인증 기능**을 구현합니다.

이 작업은 이미 구현된 **회원가입 기능(Phase 1~6)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **5개의 Phase(Phase 7~11)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

당신은 아래 전체 명세를 먼저 모두 숙지한 후, 반드시 아래 문장으로 시작해야 합니다.

**"로그인/JWT 프로젝트 명세를 완벽히 이해했습니다. Phase 7 진행을 시작할까요?"**

그 후 사용자에게서 **"진행해"** 라는 답변을 받으면, 그때 **해당 Phase의 코드만 생성**하세요.

각 Phase가 끝나면 반드시 다음 문장 형식으로 마무리하세요.

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

부분 코드, 생략 코드, 요약 코드, pseudo code는 사용하지 마세요.  
반드시 **실행 가능한 전체 코드** 기준으로 작성하세요.

이전 Phase에서 만든 파일을 수정해야 하면, 수정된 파일도 반드시 **전체 코드 기준**으로 다시 보여주세요.

파일 경로를 항상 명시하세요.

임의의 라이브러리는 추가하지 말고, 필요한 경우 추가 이유를 짧게 먼저 설명하세요.

---

# 공통 전제

회원가입 기능은 이미 구현되어 있습니다.

## 기존 구현 요약
- Member 도메인 (`Email`, `PasswordHash`, `Nickname`, `MemberStatus`)
- 회원가입 API (`POST /api/v1/members/signup`)
- Spring Security 기본 설정 (CSRF 비활성화, CORS 설정)
- React 회원가입 페이지
- 모노레포 구조
- base package: `com.example.experienceplatform`

---

# 기술 스택

## Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 Database
- JWT: `jjwt`

## Frontend
- React
- TypeScript
- Vite
- React Context + `useReducer`
- CSS Modules
- ex-design폴더 와 shad cn의 컴포넌트 활용
---

# 아키텍처 원칙

1. **실용적 DDD**를 적용합니다.
2. Bounded Context는 계속해서 **Member** 하나만 사용합니다.
3. Member는 Aggregate Root입니다.
4. Repository 인터페이스는 `domain`, 구현체는 `infrastructure`에 둡니다.
5. `interfaces` DTO와 `application` DTO는 철저히 분리합니다.
6. `PasswordHash`는 암호화된 문자열을 감싸는 값 객체 역할만 담당합니다.
7. 비밀번호 비교는 `Application Service`에서 `PasswordEncoder.matches(raw, encoded)`로 수행합니다.
8. RefreshToken은 이번 단계에서는 `Member`와 직접 JPA 연관관계를 맺지 않고, `memberId`만 보관합니다.
9. 트랜잭션은 Application Service에서 관리합니다.
10. Lombok은 **사용하지 않습니다**.

---

# 이번 범위에서 추가

- 로그인 API
- JWT (`Access Token` + `Refresh Token`) 발급 및 검증
- JWT 기반 인증 필터
- Refresh Token 저장 및 갱신
- React 로그인 페이지 및 인증 상태 관리
- 로그아웃
- 인증이 필요한 API 보호
- 내 정보 조회 API (`/api/v1/members/me`)

---

# 이번 범위에서 제외

구현하지 마세요.

- 소셜 로그인 (OAuth2)
- 이메일 인증
- 비밀번호 찾기 / 재설정
- Redis
- 관리자 권한 분리 (RBAC)
- Access Token 블랙리스트
- BFF(Backend for Frontend)

---

# 인증/보안 공통 원칙

## 1. JWT 정책
- Access Token 만료: 30분
- Refresh Token 만료: 7일
- Access Token 전달 방식: `Authorization: Bearer {token}`
- Refresh Token 전달 방식: Request Body(JSON)

## 2. 토큰 저장 전략
- Access Token: 프론트 메모리(Context state)에만 저장
- Refresh Token: 이번 프로젝트에서는 **학습 목적상 localStorage에 저장**
- 이것은 SPA라서 httpOnly 쿠키를 못 쓰기 때문이 아니라, **구현 단순화를 위한 선택**입니다
- 추후 실무에서는 httpOnly cookie + BFF 구조로 전환 가능하다는 점을 설명에 반영하세요

## 3. Refresh Token 저장 정책
- 이번 단계에서는 구현 단순화를 위해 Refresh Token **원문을 DB에 저장**
- 단, 실무에서는 해시 저장이 더 적절하다는 점을 주석 또는 설명에 명시하세요
- Member 1 : RefreshToken 1 관계
- 로그인 시 기존 Refresh Token이 있으면 새 것으로 교체
- Refresh Token 갱신 시 새 Refresh Token으로 교체 (RTR: Refresh Token Rotation)
- 로그아웃 시 Refresh Token 삭제
- 만료된 Refresh Token으로 갱신 요청 시 해당 Member의 Refresh Token 삭제

## 4. 로그아웃 정책
- 이번 단계의 로그아웃은 **서버에 저장된 Refresh Token 삭제만 수행**
- 이미 발급된 Access Token은 만료 시점까지 유효
- Access Token 블랙리스트는 구현하지 않음

## 5. 예외 처리 원칙
- 컨트롤러 내부 예외는 `GlobalExceptionHandler`에서 처리
- JWT 필터에서 발생하는 인증 예외는 `AuthenticationEntryPoint` 또는 `HandlerExceptionResolver`를 통해 공통 포맷으로 응답
- 필터 안에서 직접 JSON 문자열을 조립하는 방식은 최소화

## 6. expiresIn 규칙
- `application-local.yml`의 만료 시간 설정값은 **밀리초(ms)** 단위
- API 응답의 `expiresIn`은 **초(second)** 단위

## 7. JWT Secret 규칙
- HS256용 secret key는 최소 32바이트 이상
- 문자열 secret을 그대로 사용하지 말고, 적절히 `Key` 객체로 변환해서 사용

---

# 공통 에러 응답 포맷

## 단일 에러
```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "timestamp": "2026-03-14T10:00:00",
  "path": "/api/v1/auth/login"
}
````

## 다중 필드 검증 실패

```json
{
  "code": "VALIDATION_FAILED",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    {
      "field": "email",
      "message": "이메일은 필수입니다."
    }
  ],
  "timestamp": "2026-03-14T10:00:00",
  "path": "/api/v1/auth/login"
}
```

---

# 인증 관련 에러 응답 규칙

| 상황                   | HTTP 코드 | code                  | message                    |
| -------------------- | ------- | --------------------- | -------------------------- |
| 이메일/비밀번호 불일치         | 401     | AUTHENTICATION_FAILED | 이메일 또는 비밀번호가 올바르지 않습니다.    |
| Access Token 만료      | 401     | TOKEN_EXPIRED         | 액세스 토큰이 만료되었습니다.           |
| Access Token 유효하지 않음 | 401     | INVALID_TOKEN         | 유효하지 않은 토큰입니다.             |
| Refresh Token 만료/무효  | 401     | REFRESH_TOKEN_EXPIRED | 리프레시 토큰이 만료되었거나 유효하지 않습니다. |
| 인증 필요한 API에 토큰 없음    | 401     | UNAUTHORIZED          | 인증이 필요합니다.                 |
| 비활성/탈퇴 회원 로그인 시도     | 403     | ACCOUNT_DISABLED      | 비활성화되었거나 탈퇴한 계정입니다.        |

---

# Phase 7. JWT 인프라 및 인증 도메인 확장

## 목표

JWT 발급/검증 인프라와 RefreshToken 도메인 모델을 추가합니다.

## 요구사항

### RefreshToken 엔티티

* Member Bounded Context 내에 위치
* `Member`와 직접 연관관계를 맺지 않고 `memberId`만 보관
* 필드:

  * `id` (Long, PK)
  * `memberId` (Long, Unique)
  * `token` (String)
  * `expiryDate` (LocalDateTime)
  * `createdAt` (LocalDateTime)

### 도메인 규칙

* 하나의 Member에 하나의 RefreshToken만 존재
* `isExpired()` 메서드로 만료 여부 판단
* 만료된 토큰은 갱신 불가

### RefreshTokenRepository

* domain 레이어에 인터페이스
* infrastructure 레이어에 구현체
* 주요 메서드:

  * `Optional<RefreshToken> findByMemberId(Long memberId)`
  * `Optional<RefreshToken> findByToken(String token)`
  * `void deleteByMemberId(Long memberId)`
  * `RefreshToken save(RefreshToken refreshToken)`

### Repository 구현 구조

* `RefreshTokenRepository` : domain 인터페이스
* `RefreshTokenSpringDataJpaRepository extends JpaRepository<RefreshToken, Long>` : Spring Data JPA 인터페이스
* `RefreshTokenJpaRepository implements RefreshTokenRepository` : domain repository 구현체

### JwtTokenProvider

* infrastructure 레이어에 위치
* 책임:

  * Access Token 생성 (`subject: memberId`, `claims: email`)
  * Refresh Token 생성 (`UUID` 기반 opaque token)
  * Access Token에서 memberId 추출
  * Access Token 유효성 검증

### JwtProperties

* `@ConfigurationProperties(prefix = "jwt")`
* 필드:

  * `secret`
  * `accessTokenExpiry`
  * `refreshTokenExpiry`

### 설정 예시

```yaml
jwt:
  secret: ${JWT_SECRET:my-super-secret-key-for-local-development-only-32bytes}
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000
```

### build.gradle 의존성 추가

* `io.jsonwebtoken:jjwt-api`
* `io.jsonwebtoken:jjwt-impl` (`runtimeOnly`)
* `io.jsonwebtoken:jjwt-jackson` (`runtimeOnly`)

## 산출물

* `RefreshToken.java`
* `RefreshTokenRepository.java`
* `RefreshTokenSpringDataJpaRepository.java`
* `RefreshTokenJpaRepository.java`
* `JwtTokenProvider.java`
* `JwtProperties.java`
* `application-local.yml` (jwt 설정 추가)
* `build.gradle` (의존성 추가)

---

# Phase 8. 로그인 / 토큰 갱신 / 로그아웃 Application 서비스

## 목표

로그인, 토큰 갱신, 로그아웃 유스케이스를 구현합니다.

## 유스케이스 1: 로그인

* 입력: 이메일, 비밀번호
* 처리:

  1. 이메일로 Member 조회 (없으면 `AuthenticationFailedException`)
  2. 회원 상태 확인 (`ACTIVE`가 아니면 `AccountDisabledException`)
  3. 비밀번호 매칭 검증 (`PasswordEncoder.matches`)

     * 이메일 미존재와 비밀번호 불일치를 동일한 예외로 처리
  4. Access Token 생성
  5. Refresh Token 생성 및 DB 저장 (기존 토큰 있으면 교체)
* 출력: `AuthTokenInfo` (`accessToken`, `refreshToken`, `expiresIn`)

## 유스케이스 2: 토큰 갱신

* 입력: `refreshToken`
* 처리:

  1. DB에서 Refresh Token 조회 (없으면 `InvalidRefreshTokenException`)
  2. 만료 여부 확인

     * 만료 시 해당 Member의 Refresh Token 삭제 후 `RefreshTokenExpiredException`
  3. 해당 Member 조회
  4. 새 Access Token 생성
  5. 새 Refresh Token 생성 및 DB 교체 (Refresh Token Rotation)
* 출력: `AuthTokenInfo`

## 유스케이스 3: 로그아웃

* 입력: `memberId`
* 처리:

  1. 해당 Member의 Refresh Token 삭제
* 출력: 없음

## Application DTO

* `LoginCommand` (`email`, `password`)
* `RefreshTokenCommand` (`refreshToken`)
* `AuthTokenInfo` (`accessToken`, `refreshToken`, `expiresIn`)

## 커스텀 예외

* `AuthenticationFailedException`
* `AccountDisabledException`
* `InvalidRefreshTokenException`
* `RefreshTokenExpiredException`

## 설계 원칙

* `AuthService` 하나로 구현하거나 UseCase 인터페이스로 분리 가능
* 트랜잭션은 Application Service에서 관리
* `PasswordEncoder`는 생성자 주입
* 이메일 존재 여부와 비밀번호 불일치를 구분하지 않음
* Refresh Token Rotation은 1회 사용 원칙을 따르되, 동시 갱신 경쟁 상황은 이번 단계에서는 단순화하여 **마지막 저장 기준**으로 처리

## 산출물

* `LoginCommand.java`
* `RefreshTokenCommand.java`
* `AuthTokenInfo.java`
* `AuthService.java`
* `AuthenticationFailedException.java`
* `AccountDisabledException.java`
* `InvalidRefreshTokenException.java`
* `RefreshTokenExpiredException.java`

---

# Phase 9. 인증 API 및 JWT 필터

## 목표

인증 서비스를 REST API로 노출하고 JWT 인증 필터를 Spring Security에 통합합니다.

## API 스펙

### 로그인

* `POST /api/v1/auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

Response (`200 OK`)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 1800
}
```

### 토큰 갱신

* `POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

Response (`200 OK`)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1...(new)",
  "refreshToken": "661f9511-f30c-52e5-b827-557766551111",
  "expiresIn": 1800
}
```

### 로그아웃

* `POST /api/v1/auth/logout`
* `Authorization: Bearer {accessToken}` 필요

Response (`200 OK`)

```json
{
  "message": "로그아웃 되었습니다."
}
```

### 내 정보 조회

* `GET /api/v1/members/me`
* `Authorization: Bearer {accessToken}` 필요

Response (`200 OK`)

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "블로거킴"
}
```

## JWT 인증 필터

### JwtAuthenticationFilter

* `OncePerRequestFilter` 상속
* 처리 흐름:

  1. `Authorization` 헤더에서 Bearer 토큰 추출
  2. 토큰 없으면 필터 통과
  3. 토큰 유효성 검증
  4. 유효하면 memberId 추출
  5. `SecurityContextHolder`에 인증 정보 설정
* 만료 또는 유효하지 않은 토큰 예외는 `AuthenticationEntryPoint` 또는 `HandlerExceptionResolver`로 위임

### Security Principal

* JWT 인증 성공 시 `SecurityContext`에는 최소한 `memberId`를 포함하는 커스텀 Principal 객체 `AuthenticatedMember` 저장
* `/api/v1/members/me`에서는 이 Principal을 사용해 memberId를 읽고 회원 정보를 조회

### JwtAuthenticationEntryPoint

* `AuthenticationEntryPoint` 구현
* 인증되지 않은 요청이 보호된 API에 접근 시 `401` 응답 반환
* 공통 에러 포맷 사용

## Bean Validation

* `LoginRequest`: `@NotBlank` email, `@NotBlank` password
* `RefreshRequest`: `@NotBlank` refreshToken

## Security 설정

### permitAll

* `POST /api/v1/members/signup`
* `POST /api/v1/auth/login`
* `POST /api/v1/auth/refresh`
* `/h2-console/**`

### authenticated

* `POST /api/v1/auth/logout`
* `GET /api/v1/members/me`
* 그 외 `/api/**`

### 추가 설정

* 정적 리소스와 프론트엔드 라우팅 진입점은 접근 가능하도록 설정
* H2 콘솔 same-origin frame 허용
* CORS 허용 origin: `http://localhost:5173`
* 허용 메서드: `GET, POST, PUT, DELETE, OPTIONS`
* 허용 헤더: `Content-Type`, `Authorization`
* credentials 비활성화

## interfaces DTO

* `LoginRequest.java` → `LoginCommand`
* `LoginResponse.java` ← `AuthTokenInfo`
* `RefreshRequest.java` → `RefreshTokenCommand`
* `LogoutResponse.java`
* `MemberResponse.java`
* `AuthenticatedMember.java`

## GlobalExceptionHandler 확장

* `AuthenticationFailedException` → `401`
* `AccountDisabledException` → `403`
* `InvalidRefreshTokenException` → `401`
* `RefreshTokenExpiredException` → `401`

## 산출물

* `AuthController.java`
* `LoginRequest.java`
* `LoginResponse.java`
* `RefreshRequest.java`
* `LogoutResponse.java`
* `MemberResponse.java`
* `AuthenticatedMember.java`
* `JwtAuthenticationFilter.java`
* `JwtAuthenticationEntryPoint.java`
* `SecurityConfig.java` (수정)
* `GlobalExceptionHandler.java` (확장)
* `MemberController.java` (`/me` 엔드포인트 추가)

---

# Phase 10. React 로그인 및 인증 상태 관리

## 목표

React 로그인 페이지와 전역 인증 상태 관리를 구현합니다.

## 기술

* React + TypeScript + Vite
* 상태관리: React Context + `useReducer`
* 스타일링: CSS Modules

## 인증 토큰 저장 전략

* Access Token: 메모리(Context state)에만 저장
* Refresh Token: 이번 프로젝트에서는 학습 목적상 `localStorage`에 저장
* 추후 httpOnly cookie + BFF 구조로 전환 가능한 형태를 염두에 둠
* 앱 초기화 시 localStorage의 Refresh Token으로 자동 갱신 시도
* 자동 갱신 성공 후 `/api/v1/members/me` 호출로 사용자 정보 복원

## 화면 구성

### 로그인 페이지 (`/login`)

* 이메일 입력
* 비밀번호 입력
* 로그인 버튼
* 회원가입 링크
* 에러 메시지 표시
* 로딩 상태 표시

### 가입 완료 페이지 수정

* 기존 `/signup/complete` 페이지에 로그인 링크 추가

### 홈 화면 (`/`)

* 비로그인: 로그인/회원가입 링크
* 로그인: 닉네임 표시 + 로그아웃 버튼

### 보호된 라우트

* 인증이 필요한 페이지는 `ProtectedRoute`로 감싸기
* 미인증 시 `/login` 리다이렉트

## AuthState

```text
- isAuthenticated: boolean
- user: { id, email, nickname } | null
- accessToken: string | null
- isLoading: boolean
```

## AuthActions

```text
- login(email, password)
- logout()
- refreshToken()
```

## API 클라이언트

* `axios interceptor` 또는 `fetch wrapper` 사용 가능
* 요청 시 accessToken이 있으면 `Authorization: Bearer` 자동 추가
* `401 TOKEN_EXPIRED` 수신 시 자동으로 `/api/v1/auth/refresh` 호출
* 갱신 성공 시 원래 요청 재시도
* 갱신 실패 시 로그아웃 처리 후 `/login` 이동
* `/api/v1/auth/refresh` 요청 자체에는 자동 재시도 로직을 적용하지 않음
* 한 요청당 재시도는 1회만 허용
* 여러 요청이 동시에 401을 받을 경우 갱신 요청은 1회만 수행
* 갱신 진행 중인 동안 이후 요청은 대기 큐에 보관
* 갱신 성공 시 대기 중 요청들을 새 Access Token으로 재시도
* 갱신 실패 시 대기 중 요청도 모두 실패 처리 후 로그아웃

## 프로젝트 구조

```text
/frontend
  /src
    /pages
      LoginPage.tsx
      HomePage.tsx
      SignupCompletePage.tsx
    /components
      LoginForm.tsx
      ProtectedRoute.tsx
      Header.tsx
    /api
      authApi.ts
      apiClient.ts
      memberApi.ts
    /context
      AuthContext.tsx
      AuthProvider.tsx
    /hooks
      useAuth.ts
      useLoginForm.ts
    /types
      auth.ts
    /styles
      LoginForm.module.css
      HomePage.module.css
      Header.module.css
    App.tsx
```

## 산출물

* 위 구조의 모든 신규/수정 파일
* 로그인 폼 CSS Module
* API 클라이언트
* AuthContext / AuthProvider

---

# Phase 11. 인증 기능 테스트

## 목표

로그인/JWT 기능의 테스트 코드를 작성합니다.

## 테스트 범위

### 1. 도메인 단위 테스트

* `RefreshToken`

  * `isExpired()` 정상 동작
  * 만료되지 않은 토큰 확인

### 2. JwtTokenProvider 단위 테스트

* Access Token 생성 및 memberId 추출
* 만료된 토큰 검증 시 예외 발생
* 변조된 토큰 검증 시 예외 발생
* Refresh Token 생성 (UUID 형식 확인)

### 3. Application 서비스 테스트

* 로그인 성공
* 이메일 미존재 → `AuthenticationFailedException`
* 비밀번호 불일치 → `AuthenticationFailedException`
* 비활성 회원 → `AccountDisabledException`
* 토큰 갱신 성공 → 새로운 토큰 쌍 반환, 기존 Refresh Token 교체 확인
* 만료된 Refresh Token 갱신 → 예외 + 해당 멤버 토큰 삭제 확인
* 존재하지 않는 Refresh Token → `InvalidRefreshTokenException`
* 로그아웃 → Refresh Token 삭제 확인
* Repository는 Mock 사용
* 비밀번호 비교는 `PasswordEncoder.matches()` 기반으로 검증

### 4. API 통합 테스트

* `POST /api/v1/auth/login`

  * 정상 로그인 → `200 OK`
  * 이메일/비밀번호 불일치 → `401`
  * Bean Validation 실패 → `400`
* `POST /api/v1/auth/refresh`

  * 정상 갱신 → `200 OK`
  * 잘못된 Refresh Token → `401`
* `POST /api/v1/auth/logout`

  * 인증된 상태 → `200 OK`
  * 미인증 상태 → `401`
* `GET /api/v1/members/me`

  * 유효한 토큰 → `200 OK`
  * 토큰 없음 → `401`
  * 만료된 토큰 → `401` + `TOKEN_EXPIRED`
* permitAll 경로는 토큰 없이 접근 가능
* 보호된 경로는 토큰 필수

### 5. 프론트엔드 단위 테스트

* `useLoginForm`

  * 이메일/비밀번호 입력 상태 관리
  * 빈 값 제출 방어
* `useAuth`

  * login 후 인증 상태 변경
  * logout 후 상태 초기화

## 테스트 설계 원칙

* JWT/만료 테스트는 가능하면 `Clock` 주입 또는 시간 제어가 가능하도록 설계
* flaky test가 되지 않도록 시간 의존성을 줄일 것
* `/api/v1/auth/refresh` 자동 재시도 제외 로직도 프론트 테스트 가능하면 포함

## 테스트 도구

* Backend: JUnit 5, Mockito, AssertJ
* Frontend: Vitest, React Testing Library

## 산출물

* `RefreshTokenTest.java`
* `JwtTokenProviderTest.java`
* `AuthServiceTest.java`
* `AuthControllerIntegrationTest.java`
* `MemberControllerMeIntegrationTest.java`
* `useLoginForm.test.ts`
* `useAuth.test.ts`

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"로그인/JWT 프로젝트 명세를 완벽히 이해했습니다. Phase 7 진행을 시작할까요?"**

```

```
