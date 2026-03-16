# 체험단 통합 플랫폼 - 프로젝트 진행 현황

## 기술 스택

- **Backend**: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, H2 Database
- **Frontend**: React + TypeScript + Vite + Tailwind CSS + shadcn/ui
- **빌드**: 모노레포 (Gradle 내에서 프론트엔드 빌드 통합)
- **아키텍처**: DDD (Domain-Driven Design), Layered Architecture (domain / application / infrastructure / interfaces)

---

## 전체 Phase 현황 (총 59 Phase)

### 완료된 작업

| Phase | 기능 | Bounded Context | 프롬프트 파일 | 상태 |
|-------|------|----------------|-------------|------|
| 1~6 | 회원가입 | Member | `project.md` | ✅ 완료 |
| 7~11 | 로그인 / JWT 인증 | Member | `jwt.md` | ✅ 완료 |
| 12~16 | 캠페인 목록 조회 | Campaign | `campaign.md` | ✅ 완료 |
| 17~21 | 캠페인 상세 페이지 | Campaign | `campaign-detail.md` | ✅ 완료 |
| 22~26 | 마이페이지 | Member | `mypage.md` | ✅ 완료 |
| 27~31 | 찜 / 북마크 | Campaign | *(별도 파일 없음)* | ✅ 완료 |
| 32~36 | 크롤링 자동화 | Campaign | `crawling.md` | ✅ 완료 |
| 37~42 | 관리자 기능 (RBAC, 회원/캠페인/대시보드 관리) | Admin | `admin.md` | ✅ 완료 |
| 43~47 | 크롤링 소스 동적 관리 (enum→DB 전환) | Campaign + Admin | `crawling-detail.md` | ✅ 완료 |

### 미완료 작업

| Phase | 기능 | Bounded Context | 프롬프트 파일 | 상태 |
|-------|------|----------------|-------------|------|
| 48~53 | 알림 기능 | Notification | `alram.md` | ❌ 미진행 |
| 54~59 | AWS EC2 배포 | DevOps | `aws.md` | ❌ 미진행 |

---

## 완료된 기능 상세

### Phase 1~6: 회원가입 (`project.md`)
- 모노레포 프로젝트 초기 세팅 (DDD 패키지 구조, Gradle 빌드 통합)
- Member 도메인 모델 (Email, PasswordHash, Nickname VO)
- 회원가입 Application 서비스 (BCrypt 암호화, 중복 검증)
- REST API (`POST /api/v1/members/signup`)
- React 회원가입 화면 (실시간 유효성 검증)
- 통합 테스트

### Phase 7~11: 로그인 / JWT (`jwt.md`)
- JWT 인프라 (Access Token / Refresh Token)
- 로그인/토큰 갱신/로그아웃 서비스
- JWT 인증 필터 + Spring Security 설정
- React 로그인 화면 + 인증 상태 관리 (AuthContext)
- 인증 기능 테스트

### Phase 12~16: 캠페인 목록 조회 (`campaign.md`)
- Campaign 도메인 모델 (SourceType, Category, Status)
- 검색/필터/정렬/페이징 기능
- REST API (`GET /api/v1/campaigns`, `GET /api/v1/campaigns/filters`)
- React 메인 홈페이지 (필터 바, 캠페인 그리드, 페이지네이션)
- 시드 데이터 60건
- 테스트

### Phase 17~21: 캠페인 상세 (`campaign-detail.md`)
- Campaign 도메인 상세 필드 확장 (detailContent, reward, mission, address, keywords)
- 캠페인 상세 조회 API (`GET /api/v1/campaigns/{id}`)
- React 상세 페이지
- 테스트

### Phase 22~26: 마이페이지 (`mypage.md`)
- 프로필 조회/수정 (닉네임 변경)
- 비밀번호 변경
- 회원 탈퇴
- React 마이페이지 (프로필, 비밀번호 변경, 계정 삭제)
- 테스트

### Phase 27~31: 찜 / 북마크
- Bookmark 도메인 (Member ↔ Campaign 연관관계)
- 찜 추가/취소/목록 조회
- React 찜 기능 (하트 아이콘, 마이페이지 찜 목록)
- 테스트

### Phase 32~36: 크롤링 자동화 (`crawling.md`)
- 크롤링 인프라 (Jsoup, robots.txt 준수, 딜레이)
- 사이트별 크롤러 (REVU, MBLE, GANGNAM) + Mock 모드
- CrawlingOrchestrator (upsert, 만료 캠페인 자동 CLOSED)
- 스케줄러 + 수동 트리거 API
- 크롤링 로그 관리
- 테스트

### Phase 37~42: 관리자 기능 (`admin.md`)
- Member 역할(Role) 확장 + RBAC
- 관리자 회원 관리 API
- 관리자 캠페인 관리 API
- 관리자 대시보드 API
- 관리자 React 페이지
- 테스트

### Phase 43~47: 크롤링 소스 동적 관리 (`crawling-detail.md`)
- `SourceType` enum → `CrawlingSource` DB 엔티티 전환
- `Campaign.sourceType` → `Campaign.crawlingSource` (ManyToOne FK)
- CrawlerRegistry (crawlerType 기반 동적 매칭)
- GenericCrawler (범용 fallback 크롤러)
- CrawlingLog 스냅샷 방식 (sourceCode, sourceName, crawlerType)
- 관리자 크롤링 소스 CRUD API (등록/수정/토글/테스트)
- React 관리자 소스 관리 UI (탭 기반, 모달, 동적 실행 버튼)
- 필터 옵션 API 동적화 (DB 기반 활성 소스)
- 테스트 (도메인/서비스/통합/프론트)

---

## 미완료 기능 상세

### Phase 48~53: 알림 기능 (`alram.md`)
- **Phase 48**: Notification 도메인 모델 설계
- **Phase 49**: 알림 생성 이벤트 + 스케줄러
- **Phase 50**: 알림 조회 / 읽음 / 설정 Application 서비스
- **Phase 51**: 알림 API
- **Phase 52**: React 알림 UI
- **Phase 53**: 알림 기능 테스트

### Phase 54~59: AWS EC2 배포 (`aws.md`)
- **Phase 54**: 운영 프로파일 + MySQL 전환
- **Phase 55**: Docker 컨테이너화
- **Phase 56**: AWS 인프라 구성
- **Phase 57**: GitHub Actions CI/CD 파이프라인
- **Phase 58**: 모니터링 + 운영 안정화
- **Phase 59**: 배포 테스트 + 검증

---

## 테스트 현황

- **백엔드**: 215개 테스트 (JUnit 5 + Mockito + AssertJ + H2)
- **프론트엔드**: 68개 테스트 (Vitest + React Testing Library)
- **전체**: 283개 테스트 통과
