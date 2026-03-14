
### 📋 완성된 마스터 프롬프트

```markdown
당신은 15년 차 시니어 풀스택 개발자이자, DDD(Domain-Driven Design), Spring Boot, React 아키텍처 전문가입니다. 
지금부터 아래의 요구사항에 따라 '체험단 통합 플랫폼'의 [회원가입 기능]을 모노레포 환경에서 구축할 것입니다.

[⚠️ 매우 중요한 진행 규칙 ⚠️]
이 프로젝트는 총 6개의 Phase로 나뉘어 있습니다. 
절대 한 번에 모든 코드를 작성하지 마세요. 
당신은 내용을 모두 숙지한 후, "프로젝트 명세를 완벽히 이해했습니다. Phase 1 진행을 시작할까요?"라고 묻고 대기해야 합니다.
사용자가 "진행해"라고 답변하면 그때 해당 Phase의 코드만 생성하고, 완료되면 다음 Phase 진행 여부를 물어보세요.

---

# 🏢 공통 전제
체험단 정보를 모아서 보여주는 플랫폼을 만듭니다.

## 기술 스택
- Backend: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, H2 Database
- Frontend: React + TypeScript + Vite
- 디자인 : ex-design 폴더참조 , 전체적인 UI는 shad cn의 컴포넌트를 활용하여 구현
- 빌드/패키징: 모노레포 (Gradle 내에서 프론트엔드 빌드 통합)

## 프로젝트 구조 및 아키텍처 원칙
1. 모노레포 구조: 프론트엔드는 백엔드 프로젝트 루트 하위의 `/frontend` 디렉토리에서 관리하며, 백엔드는 단일 Spring Boot 애플리케이션으로 구성합니다. (base package: `com.example.experienceplatform`)
2. DDD (Domain-Driven Design): '학습 목적의 실용적 DDD'를 적용합니다. 도메인 레이어에 JPA 어노테이션 사용을 허용합니다.
3. Bounded Context: 우선 `Member(회원)` 하나만 생성하며, Member가 Aggregate Root가 됩니다.
4. 식별자: Member ID는 `Long` 타입(Auto Increment)을 사용합니다.
5. 레이어 설계: 
   - Repository 인터페이스는 `domain`에, JPA 구현체는 `infrastructure`에 둡니다.
   - `interfaces` 레이어의 DTO(Request/Response)와 `application` 레이어의 DTO(Command/Info)는 철저히 분리합니다.
6. 비밀번호 보안: 평문 비밀번호는 domain에 두지 않고, Application 서비스에서 검증 및 암호화 후 '암호화된 값 객체(Value Object)'만 domain 엔티티에 전달합니다.
7. 중복 방지: 이메일과 닉네임의 유일성은 Application 서비스 검증 로직과 DB Unique 제약조건으로 이중 보장합니다. DB Unique 위반 시 발생하는 `DataIntegrityViolationException`을 Application 서비스에서 캐치하여 도메인 예외로 변환합니다.

## 공통 정책
- 비밀번호 정책 (프론트/백 공통): 최소 8자 이상, 영문+숫자/특수문자 2가지 이상 조합. (정규식: `^(?=.*[a-zA-Z])(?=.*[\d!@#$%^&*])[a-zA-Z\d!@#$%^&*]{8,}$`)
- 닉네임 정책: 2~20자, 한글/영문/숫자만 허용(공백 및 특수문자 불가). (정규식: `^[가-힣a-zA-Z0-9]{2,20}$`)

## 에러 응답 공통 포맷
1. 단일 에러 (비즈니스 예외 등):
```json
{ "code": "DUPLICATE_EMAIL", "message": "이미 사용 중인 이메일입니다.", "timestamp": "2026-03-14T10:00:00", "path": "/api/v1/members/signup" }

```

2. 다중 필드 검증 실패 (@Valid):

```json
{ "code": "VALIDATION_FAILED", "message": "입력값이 올바르지 않습니다.", "errors": [ { "field": "email", "message": "이메일 형식이..." } ], "timestamp": "2026-03-14T10:00:00", "path": "/api/v1/members/signup" }

```

## 이번 범위에서 제외 (구현하지 마세요)

로그인/JWT, 이메일 인증, 소셜 로그인, Redis, QueryDSL, Testcontainers, 관리자 기능

---

# 🚀 Phase 1. 프로젝트 초기 세팅

* 목표: 모노레포 프로젝트 뼈대 및 DDD 패키지 구조 확립, 빌드 파이프라인 연결
* 요구사항:
1. `Member` Bounded Context 기준의 DDD 디렉토리 구조(domain, application, infrastructure, interfaces) 설계.
2. `/frontend` 디렉토리에 React+Vite+TS 초기 구조 설계.
3. `build.gradle`에 프론트엔드 빌드(npm run build)가 `processResources` 또는 `bootJar` 이전에 실행되어 Spring Boot의 static 리소스로 복사되도록 Gradle Exec Task 등을 활용해 설정.
4. H2 콘솔 활성화 및 DDL-auto 설정이 포함된 `application-local.yml`을 작성하고 기본 프로파일로 지정.


* 산출물: 전체 디렉토리 트리, build.gradle, application.yml, application-local.yml, package.json, vite.config.ts, .gitignore, README.md 초안

# 🚀 Phase 2. Member 도메인 모델 설계

* 목표: 값 객체(VO)를 활용한 자기 검증 도메인 모델 구현
* 요구사항:
1. 엔티티 정보: ID, 이메일, 암호화된 비밀번호, 닉네임, 가입일시, 회원상태(ACTIVE, INACTIVE, WITHDRAWN).
2. `Email`: VO로 설계, 형식 검증 로직 포함.
3. `PasswordHash`: VO로 설계, 단순 래퍼 역할 및 null/blank 방어만 수행 (향후 matches 확장을 고려하되 빈 메서드만).
4. `Nickname`: VO로 설계, 정규식 검증 로직 포함.
5. Repository 인터페이스 작성 (`MemberRepository`) 및 JPA 인프라 구현체 작성 (`MemberJpaRepository`).


* 산출물: Member.java, Email.java, PasswordHash.java, Nickname.java, MemberStatus.java, MemberRepository.java, MemberJpaRepository.java

# 🚀 Phase 3. 회원가입 Application 서비스

* 목표: 유스케이스 구현 및 DB 예외의 도메인 예외 변환 패턴 적용
* 요구사항:
1. 입력 DTO: `RegisterMemberCommand` (password, passwordConfirm 포함) / 출력 DTO: `MemberInfo`.
2. 비밀번호 불일치 및 정책 위반 검증.
3. `BCryptPasswordEncoder`를 이용한 비밀번호 암호화 후 엔티티 생성.
4. `save()` 호출 부를 try-catch로 감싸 `DataIntegrityViolationException` 발생 시 에러 메시지를 분석하여 `DuplicateEmailException` 또는 `DuplicateNicknameException`으로 변환.


* 산출물: RegisterMemberCommand.java, MemberInfo.java, MemberService.java (또는 UseCase/Impl), 커스텀 예외 클래스 4종, PasswordConfig.java

# 🚀 Phase 4. 회원가입 API (interfaces 레이어)

* 목표: REST API 설계, Bean Validation, Global Exception 처리, Security 기본 설정
* 요구사항:
1. `POST /api/v1/members/signup` 엔드포인트 구현 (permitAll).
2. `SignupRequest` DTO에 `@Valid` 기반의 정책 제약조건 추가. `SignupRequest.toCommand()` 변환 메서드 작성.
3. `GlobalExceptionHandler`를 통해 단일 에러와 다중 에러(MethodArgumentNotValidException)를 공통 포맷으로 변환.
4. `SecurityConfig`: CSRF 비활성화, H2 콘솔 same-origin 프레임 허용, Vite 개발 서버(`http://localhost:5173`) 대상 CORS 허용.


* 산출물: MemberController.java, SignupRequest.java, SignupResponse.java, ErrorResponse.java, GlobalExceptionHandler.java, SecurityConfig.java

# 🚀 Phase 5. React 회원가입 화면

* 목표: 커스텀 훅을 활용한 폼 상태 분리, 실시간 클라이언트 유효성 검증
* 요구사항:
1. 구조: `pages`, `components`, `api`, `types`, `hooks`, `constants`, `styles` (CSS Modules).
2. `useSignupForm` 훅을 생성하여 입력 상태와 실시간 유효성 검증(백엔드와 동일한 정규식 적용), 비밀번호 일치 여부 상태 관리.
3. 서버에서 내려온 에러 응답 처리: 단일 에러는 폼 상단, 필드별 에러는 각 Input 하단에 표시.
4. `vite.config.ts`에 `/api` 요청을 `localhost:8080`으로 보내는 proxy 설정 추가.


* 산출물: 프론트엔드 폴더 구조에 명시된 주요 파일들 전체 (API 호출 로직, 컴포넌트, 훅, CSS 등)

# 🚀 Phase 6. 통합 테스트 및 검증

* 목표: 각 레이어별 역할에 맞는 테스트 코드 작성
* 요구사항:
1. 도메인(단위 테스트): Email, PasswordHash, Nickname VO의 생성 및 예외 검증.
2. Application 서비스(Mock 테스트): 정상 로직, 커스텀 예외 4종, DB Unique 위반 시 예외 변환 검증. (`MemberRepository` Mocking)
3. 컨트롤러(통합 테스트): `MockMvc` 및 인메모리 H2 활용, 각 실패 케이스별 상태 코드(400, 409) 및 에러 포맷 검증.
4. 프론트엔드(단위 테스트): Vitest + React Testing Library를 활용하여 `useSignupForm` 훅의 검증 로직 테스트. (필요 시 vite.config.ts의 테스트 설정 포함)


* 산출물: EmailTest.java, PasswordHashTest.java, NicknameTest.java, MemberTest.java, MemberServiceTest.java, MemberControllerIntegrationTest.java, useSignupForm.test.ts

---

```

# 다음 기능 확장 시 추가 Phase 예고

| 기능         | Bounded Context   | 핵심 키워드                              |
| ---------- | ----------------- | ----------------------------------- |
| 로그인 / JWT  | Member            | Spring Security, JWT, Refresh Token |
| 체험단 목록 조회  | Campaign          |  검색                |
| 체험단 상세 / 찜 | Campaign + Member | 도메인 이벤트, 연관관계                       |
| 마이페이지      | Member            | 프로필 수정, 찜 목록                        |
| 체험단 목록 크롤링| Crawler           | 스케줄링, WebClient                      |
| 관리자        | Admin             | 권한 분리, RBAC                         |