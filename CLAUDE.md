# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

체험단 통합 플랫폼 (Experience Platform) - 블로그 체험단 캠페인을 여러 사이트에서 크롤링하여 통합 제공하는 풀스택 웹 애플리케이션.

## Build & Run Commands

```bash
./gradlew bootRun          # 백엔드 실행 (local 프로필, H2 인메모리 DB)
./gradlew test             # 전체 테스트 실행
./gradlew bootJar          # JAR 빌드 (프론트엔드 포함)
```

프론트엔드 (frontend/ 디렉토리):
```bash
npm install && npm run dev   # Vite 개발서버 (localhost:5173)
npm run build                # 빌드 → src/main/resources/static 으로 복사
```

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.4.1, Spring Security (JWT), Spring Data JPA, H2
- **Frontend**: React, TypeScript, Vite, Tailwind CSS, shadcn/ui
- **Crawling**: Jsoup 1.17.2
- **Testing**: JUnit 5, Mockito, AssertJ

## Architecture

DDD 기반 레이어드 아키텍처. 두 개의 바운디드 컨텍스트: `member/`, `campaign/`.

각 컨텍스트 내부 레이어:
- **domain** — 엔티티, 값 객체(Email, Nickname, PasswordHash는 @Embeddable), 리포지토리 인터페이스, 도메인 예외
- **application** — 서비스, Command/Info DTO, 유즈케이스 오케스트레이션
- **infrastructure** — JPA 리포지토리 구현체, 외부 연동 (크롤링, JWT)
- **interfaces** — REST 컨트롤러, Request/Response DTO

리포지토리 패턴: `Domain Interface → JpaRepository 구현체 → SpringDataJpa 인터페이스` 3단 구조.

## Crawling Architecture

- `CampaignCrawler` 인터페이스를 각 사이트별로 구현 (30+ 크롤러)
- `CrawlerRegistry`가 Spring 컴포넌트 스캔으로 크롤러를 자동 등록하고 `crawlerType`으로 매핑
- `CrawlingOrchestrator`가 전체 크롤링 흐름 오케스트레이션
- `CrawlingSource`는 DB 엔티티로 관리 (동적 추가/수정 가능)
- 중복 방지: `(crawlingSource, originalId)` 유니크 제약 조건으로 upsert
- 크롤링 설정은 `application-local.yml`의 `crawling.*` 프로퍼티로 외부화

## Authentication

JWT 기반 stateless 인증. Access Token(30분) + Refresh Token(7일) 로테이션.
- `JwtTokenProvider` — 토큰 생성/검증
- `JwtAuthenticationFilter` — 요청별 인증 필터
- 공개 엔드포인트: 회원가입, 로그인, 토큰갱신, 캠페인 목록/상세

## Database

개발환경: H2 인메모리 (`jdbc:h2:mem:experiencedb`, DDL auto `create-drop`)
- 콘솔: `http://localhost:8080/h2-console`
- 프로덕션 마이그레이션 도구 미적용 (Flyway/Liquibase 필요)

## Key Conventions

- 생성자 주입만 사용 (필드 주입 금지)
- Application 레이어에서 Command(입력)/Info(출력) DTO 분리, Interfaces 레이어에서 Request/Response DTO 분리
- `CampaignSpecification`으로 동적 검색 쿼리 구성 (JPA Specification 패턴)
- Campaign-CrawlingSource는 ManyToOne LAZY 로딩
- 서비스에 `@Transactional` 적용, 조회는 `readOnly=true`
