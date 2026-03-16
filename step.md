# 체험단 통합 플랫폼 - 단계별 진행 현황

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, H2 |
| Frontend | React, TypeScript, Vite, Tailwind CSS, shadcn/ui |
| 빌드 | 모노레포 (Gradle + 프론트엔드 통합) |
| 아키텍처 | DDD (domain / application / infrastructure / interfaces) |

---

## 전체 진행률: 47 / 59 Phase (79.7%)

```
[████████████████████████████████████████░░░░░░░░░░] 47/59
```

---

## Phase 1~6: 회원가입 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 프로젝트 초기 세팅 (모노레포, DDD 패키지 구조) | ✅ |
| 2 | Member 도메인 모델 (Email, PasswordHash, Nickname VO) | ✅ |
| 3 | 회원가입 Application 서비스 (BCrypt, 중복 검증) | ✅ |
| 4 | 회원가입 API + Security + GlobalExceptionHandler | ✅ |
| 5 | React 회원가입 화면 (실시간 유효성 검증) | ✅ |
| 6 | 통합 테스트 | ✅ |

> 프롬프트: `project-prompt/project.md`

---

## Phase 7~11: 로그인 / JWT ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 7 | JWT 인프라 + 인증 도메인 확장 | ✅ |
| 8 | 로그인 / 토큰 갱신 / 로그아웃 서비스 | ✅ |
| 9 | 인증 API + JWT 필터 | ✅ |
| 10 | React 로그인 + 인증 상태 관리 (AuthContext) | ✅ |
| 11 | 인증 기능 테스트 | ✅ |

> 프롬프트: `project-prompt/jwt.md`

---

## Phase 12~16: 캠페인 목록 조회 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 12 | Campaign 도메인 모델 (Category, Status) | ✅ |
| 13 | 캠페인 목록 조회 서비스 + 시드 데이터 60건 | ✅ |
| 14 | 캠페인 목록 API (검색/필터/정렬/페이징) | ✅ |
| 15 | React 메인 홈 (필터 바, 그리드, 페이지네이션) | ✅ |
| 16 | 테스트 | ✅ |

> 프롬프트: `project-prompt/campaign.md`

---

## Phase 17~21: 캠페인 상세 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 17 | Campaign 상세 필드 확장 (reward, mission, keywords 등) | ✅ |
| 18 | 캠페인 상세 조회 Application 서비스 | ✅ |
| 19 | 캠페인 상세 조회 API | ✅ |
| 20 | React 캠페인 상세 페이지 | ✅ |
| 21 | 테스트 | ✅ |

> 프롬프트: `project-prompt/campaign-detail.md`

---

## Phase 22~26: 마이페이지 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 22 | Member 도메인 프로필 관리 확장 | ✅ |
| 23 | 프로필 관리 Application 서비스 | ✅ |
| 24 | 마이페이지 API (프로필/비밀번호 변경/탈퇴) | ✅ |
| 25 | React 마이페이지 | ✅ |
| 26 | 테스트 | ✅ |

> 프롬프트: `project-prompt/mypage.md`

---

## Phase 27~31: 찜 / 북마크 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 27~31 | Bookmark 도메인, 찜 추가/취소/목록, React UI, 테스트 | ✅ |

> 프롬프트: 별도 파일 없음 (mypage.md 이후 진행)

---

## Phase 32~36: 크롤링 자동화 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 32 | 크롤링 인프라 (Jsoup, robots.txt, 딜레이) | ✅ |
| 33 | 사이트별 크롤러 (REVU, MBLE, GANGNAM) + Mock 모드 | ✅ |
| 34 | CrawlingOrchestrator + 스케줄링 | ✅ |
| 35 | 크롤링 수동 트리거 API + 로그 관리 | ✅ |
| 36 | 테스트 | ✅ |

> 프롬프트: `project-prompt/crawling.md`

---

## Phase 37~42: 관리자 기능 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 37 | Member 역할(Role) 확장 + RBAC 인프라 | ✅ |
| 38 | 관리자 회원 관리 API | ✅ |
| 39 | 관리자 캠페인 관리 API | ✅ |
| 40 | 관리자 대시보드 API | ✅ |
| 41 | 관리자 React 페이지 | ✅ |
| 42 | 테스트 | ✅ |

> 프롬프트: `project-prompt/admin.md`

---

## Phase 43~47: 크롤링 소스 동적 관리 ✅

| Phase | 내용 | 상태 |
|-------|------|------|
| 43 | CrawlingSource 도메인 모델 + SourceType enum→DB 전환 | ✅ |
| 44 | CrawlerRegistry + GenericCrawler + 크롤러 동적 매칭 | ✅ |
| 45 | 크롤링 소스 관리 API (등록/수정/토글/테스트) | ✅ |
| 46 | 관리자 크롤링 소스 관리 UI (탭, 모달, 동적 버튼) | ✅ |
| 47 | 테스트 (도메인/서비스/통합/프론트) | ✅ |

> 프롬프트: `project-prompt/crawling-detail.md`

---

## Phase 48~53: 알림 기능 ❌ 미진행

| Phase | 내용 | 상태 |
|-------|------|------|
| 48 | Notification 도메인 모델 설계 | ❌ |
| 49 | 알림 생성 이벤트 + 스케줄러 | ❌ |
| 50 | 알림 조회 / 읽음 / 설정 Application 서비스 | ❌ |
| 51 | 알림 API | ❌ |
| 52 | React 알림 UI | ❌ |
| 53 | 알림 기능 테스트 | ❌ |

> 프롬프트: `project-prompt/alram.md`

---

## Phase 54~59: AWS EC2 배포 ❌ 미진행

| Phase | 내용 | 상태 |
|-------|------|------|
| 54 | 운영 프로파일 + MySQL 전환 | ❌ |
| 55 | Docker 컨테이너화 | ❌ |
| 56 | AWS 인프라 구성 | ❌ |
| 57 | GitHub Actions CI/CD 파이프라인 | ❌ |
| 58 | 모니터링 + 운영 안정화 | ❌ |
| 59 | 배포 테스트 + 검증 | ❌ |

> 프롬프트: `project-prompt/aws.md`

---

## 테스트 현황

| 영역 | 테스트 수 | 도구 |
|------|----------|------|
| Backend | 215개 | JUnit 5, Mockito, AssertJ, H2 |
| Frontend | 68개 | Vitest, React Testing Library |
| **합계** | **283개** | 전부 통과 |

---

## 다음 작업

**Phase 48** (알림 기능 - Notification 도메인 모델 설계) 부터 진행
