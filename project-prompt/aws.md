당신은 15년 차 시니어 DevOps 엔지니어이자, Spring Boot, Docker, Nginx, AWS EC2/RDS, GitHub Actions, 운영 배포 아키텍처 전문가입니다.

지금부터 아래 요구사항에 따라 **체험단 통합 플랫폼의 AWS EC2 배포 환경**을 구축합니다.

이 작업은 이미 구현된 **회원가입 + 로그인/JWT + 캠페인 목록/상세 + 마이페이지 + 찜 + 크롤링 + 관리자 + 알림 기능(Phase 1~53)** 을 기반으로 이어서 진행합니다.

---

# 매우 중요한 진행 규칙

이 프로젝트는 총 **6개의 Phase(Phase 54~59)** 로 나뉘어 있습니다.

절대 한 번에 모든 코드를 작성하지 마세요.

먼저 아래 전체 명세를 모두 숙지한 뒤, 반드시 아래 문장으로만 응답하세요.

**"AWS EC2 배포 프로젝트 명세를 완벽히 이해했습니다. Phase 54 진행을 시작할까요?"**

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

## 기존 환경 요약
- 모노레포
  - Backend: Spring Boot
  - Frontend: React + Vite
- 현재 Gradle 빌드 시 프론트 정적 파일이 Spring Boot jar 내부에 포함되는 구조
- local 개발은 H2 기반
- 운영 배포는 아직 미구축 상태

## 이번 배포 방향
- 단일 EC2 배포
- Docker 기반
- 운영 DB는 MySQL 8.x
- 단일 인스턴스 다운타임 허용
- 운영 안정성을 위한 최소 수준의 Actuator / 로그 / 헬스체크 / 백업 포함

---

# 기술 스택

## Runtime / Infra
- Docker
- Docker Compose
- Nginx
- AWS EC2
- AWS RDS MySQL 8.x
- GitHub Actions

## Backend
- Java 17
- Spring Boot 3.x
- Flyway
- MySQL Connector/J

## Frontend
- React
- TypeScript
- Vite

---

# 아키텍처 원칙

1. 배포 단위는 **Spring Boot 단일 애플리케이션 컨테이너**입니다.
   - 프론트 정적 파일은 Spring Boot jar 내부에 포함됩니다.
   - Nginx는 reverse proxy 역할만 수행합니다.
2. 운영 DB는 **RDS MySQL** 을 사용합니다.
3. 로컬 개발용 Docker Compose는 **MySQL + App** 을 함께 띄웁니다.
4. 운영용 Docker Compose는 **App + Nginx (+ Certbot)** 구조로 가고, DB는 외부 RDS를 사용합니다.
5. 설정은 `local / dev / prod` 프로파일로 분리합니다.
6. local은 개발 편의성, dev는 Docker/MySQL 검증, prod는 실제 운영 환경을 의미합니다.
7. H2는 local만 사용하고, dev/prod는 MySQL + Flyway를 사용합니다.
8. 운영에서는 `ddl-auto=create/update` 를 사용하지 않고, **Flyway + ddl-auto=validate** 로 고정합니다.
9. 비밀값은 코드/리포지토리에 넣지 않고 **환경변수** 로 주입합니다.
10. `/actuator/health` 는 헬스체크용으로 공개 가능하지만, 그 외 actuator 엔드포인트는 보호합니다.
11. 단일 EC2 환경이므로 Blue-Green, 무중단 배포, 오토스케일링은 이번 범위에서 제외합니다.

---

# 이번 범위에서 추가

- 운영 프로파일 구성
- H2 → MySQL 전환
- Flyway 마이그레이션
- Docker 컨테이너화
- Docker Compose
- Nginx reverse proxy
- HTTPS(선택)
- AWS EC2 / RDS 수동 구성 가이드
- GitHub Actions CI/CD
- 기본 모니터링 / 헬스체크 / 운영 스크립트
- 배포 검증 문서

---

# 이번 범위에서 제외

구현하지 마세요.

- ECS / EKS
- Terraform / CloudFormation
- CloudFront CDN
- Auto Scaling
- ALB / NLB
- 무중단 배포
- Datadog / New Relic 같은 외부 APM
- Secrets Manager / SSM Parameter Store 연동
- Kubernetes
- CloudWatch 대시보드 구성

---

# 배포 아키텍처

## 운영 구조
```text
[Client]
   ↓
[Nginx on EC2]
   ↓
[Spring Boot App Container]
   ↓
[AWS RDS MySQL]
```

## 트래픽 흐름
- 80 → 443 리다이렉트
- 443 → Nginx
- Nginx → app:8080 reverse proxy
- app → RDS MySQL

## 정적 파일 처리
- React 정적 파일은 별도 Nginx 루트에서 직접 서빙하지 않고,
- Spring Boot jar 내부에 포함된 결과물을 app이 응답합니다.
- Nginx는 reverse proxy 역할에 집중합니다.

---

# Phase 54. 운영 프로파일 + MySQL 전환

## 목표
프로파일을 정리하고, dev/prod 환경에서 MySQL + Flyway 기반으로 전환합니다.

## 프로파일 전략

| 프로파일 | 용도 | DB | Flyway | 크롤링 | 알림 |
|------|------|------|------|------|------|
| local | 로컬 개발 | H2 | 비활성 | mock/비활성 | 비활성 |
| dev | Docker 검증용 개발 서버 | MySQL | 활성 | mock/활성 | 활성 |
| prod | 운영 서버 | MySQL(RDS) | 활성 | 실제/활성 | 활성 |

## application.yml (공통)
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jpa:
    open-in-view: false
    properties:
      hibernate:
        default_batch_fetch_size: 100

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

## application-local.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:experiencedb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    defer-datasource-initialization: true
  flyway:
    enabled: false

jwt:
  secret: local-dev-secret-key-minimum-32-bytes-long
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

crawling:
  enabled: false
  mock-enabled: true

notification:
  enabled: false
```

## application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:experiencedb}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:dev_user}
    password: ${DB_PASSWORD:dev_password}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: true
    baseline-on-migrate: true

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

crawling:
  enabled: true
  mock-enabled: true
  schedule-cron: "0 0 * * * *"

notification:
  enabled: true
  deadline-cron: "0 0 9 * * *"
  cleanup-cron: "0 0 3 * * *"
```

## application-prod.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME:experiencedb}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: true
    baseline-on-migrate: true

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

crawling:
  enabled: true
  mock-enabled: false
  schedule-cron: "0 0 * * * *"

notification:
  enabled: true
  deadline-cron: "0 0 9 * * *"
  cleanup-cron: "0 0 3 * * *"

logging:
  level:
    root: INFO
    com.example.experienceplatform: INFO
```

## Flyway 전략
- local: H2 + `ddl-auto=create-drop`, Flyway 비활성
- dev/prod: Flyway 활성 + `ddl-auto=validate`

## Flyway 디렉토리
```text
src/main/resources/db/migration/
  V1__create_member_tables.sql
  V2__create_campaign_tables.sql
  V3__create_crawling_source_table.sql
  V4__create_bookmark_table.sql
  V5__create_refresh_token_table.sql
  V6__create_notification_tables.sql
  V7__create_crawling_log_table.sql
  V8__insert_initial_data.sql
```

## 마이그레이션 파일 원칙
- DDL / 초기 데이터 삽입 포함
- local용 `data.sql`과 dev/prod용 Flyway 초기 데이터 책임을 구분
- 초기 관리자 계정, 초기 크롤링 소스는 dev/prod에서는 Flyway 또는 별도 initializer 중 하나로 일관되게 처리
- 둘 다 동시에 중복 생성하지 않게 합니다

## build.gradle 추가 의존성
```groovy
runtimeOnly 'com.mysql:mysql-connector-j'

implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

## 환경변수 목록
- `SPRING_PROFILES_ACTIVE`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

## 산출물
- `application.yml`
- `application-local.yml`
- `application-dev.yml`
- `application-prod.yml`
- `build.gradle` (수정)
- `db/migration/V1__...sql` ~ `V8__...sql`
- `.env.example`

---

# Phase 55. Docker 컨테이너화

## 목표
애플리케이션을 Docker 이미지로 만들고, 로컬/운영 compose를 분리합니다.

## Dockerfile 원칙
- 멀티스테이지 빌드
- 프론트 빌드와 백엔드 빌드를 분리
- 실행 이미지는 경량 JRE 사용
- 비root 사용자로 실행

## Dockerfile
주의:
- 기존 Gradle 프론트 통합 빌드가 있더라도, Docker 빌드에서는 **프론트 dist를 먼저 빌드한 뒤 backend builder에서 jar에 포함**하는 방식으로 단순화해도 됩니다.
- 이때 `src/main/resources/static`으로 복사하거나, 프로젝트 구조에 맞는 포함 방식 중 하나를 일관되게 선택하세요.

예시 방향:
1. `node:20-alpine` 에서 frontend build
2. `gradle + jdk17` 에서 backend bootJar
3. `eclipse-temurin:17-jre-alpine` 에서 app.jar 실행

## .dockerignore
- `.git`
- `.gradle`
- `build`
- `frontend/node_modules`
- `frontend/dist`
- `.env*`
- 로컬 캐시/문서 파일 등

## docker-compose.yml (로컬 dev)
### 서비스
- `db`
  - mysql:8.0
- `app`
  - build from Dockerfile
  - profile: dev
  - DB_HOST=db
- 필요 시 healthcheck 포함

주의:
- 로컬 compose는 Nginx 없이 앱과 DB만 구성해도 됩니다.
- 로컬 목적은 Docker + MySQL 검증입니다.

## docker-compose.prod.yml (운영)
### 서비스
- `app`
- `nginx`
- `certbot` (도메인/HTTPS 사용하는 경우)

주의:
- 운영 compose에는 MySQL 컨테이너를 넣지 않습니다.
- RDS를 외부로 사용합니다.

## Nginx 구성
### 원칙
- app:8080 reverse proxy
- `/api/` 와 SPA 요청 모두 app 으로 전달
- health check용 `/actuator/health` 접근 가능
- 보안 헤더 추가
- gzip 사용
- HTTPS 설정 파일과 HTTP-only 설정 파일 둘 다 제공

## 환경변수 치환 주의
Nginx config에서 `${DOMAIN_NAME}` 같은 변수는 자동 치환되지 않습니다.  
따라서 아래 둘 중 하나로 구현합니다.

1. 템플릿 파일(`default.conf.template`) + `envsubst`
2. 도메인 문자열을 직접 하드코딩한 실제 conf 파일

이번 Phase에서는 **템플릿 + envsubst 방식**을 추천합니다.

## 운영 compose 로깅
- json-file driver
- `max-size`, `max-file`

## 헬스체크
- app 컨테이너는 `/actuator/health`
- runtime image 안에 healthcheck용 `curl` 또는 `wget`가 존재하도록 구성

## 산출물
- `Dockerfile`
- `.dockerignore`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `nginx/nginx.conf`
- `nginx/conf.d/default.conf.template`
- `nginx/conf.d/default-http-only.conf`
- `.env.example` (수정)

---

# Phase 56. AWS 인프라 구성

## 목표
AWS 수동 구성 가이드와 EC2 초기 설정 스크립트를 작성합니다.

## 인프라 구성
### EC2
- AMI: Amazon Linux 2023
- Type: t3.small
- Storage: 20GB gp3
- Elastic IP 사용 권장

### RDS
- MySQL 8.0
- db.t3.micro
- Public access: No
- EC2와 같은 VPC
- RDS SG는 EC2 SG에서만 3306 허용

## Security Group
### EC2 SG
- 22: 내 IP
- 80: 0.0.0.0/0
- 443: 0.0.0.0/0

### RDS SG
- 3306: EC2 SG

## EC2 초기화 스크립트
### 목표
- OS 업데이트
- Docker 설치
- Docker Compose 사용 가능 상태
- git 설치
- 타임존 설정
- swap 설정
- 배포 디렉토리 생성

주의:
- Amazon Linux 2023 기준으로 `docker compose` 사용 방식을 우선합니다.
- legacy `docker-compose` standalone 바이너리를 설치하더라도, 최종 문서와 스크립트에서 어떤 명령을 사용할지 일관되게 통일하세요.
- 권장: `docker compose`

## 배포 디렉토리
```text
/home/ec2-user/app/
  docker-compose.prod.yml
  .env
  nginx/
  certbot/
  scripts/
```

## 운영 스크립트
- `ec2-init.sh`
- `deploy.sh`
- `ssl-init.sh`
- `backup-db.sh`

## SSL 초기 발급
- 도메인이 준비된 경우만 HTTPS 활성화
- 도메인이 없으면 HTTP-only Nginx 설정으로 먼저 시작 가능

## AWS 문서
- EC2 생성 절차
- RDS 생성 절차
- SG 연결 절차
- SSH 접속 후 파일 배치
- `.env` 작성
- compose 실행
- health check 확인

## 산출물
- `scripts/ec2-init.sh`
- `scripts/deploy.sh`
- `scripts/ssl-init.sh`
- `scripts/backup-db.sh`
- `AWS_SETUP_GUIDE.md`
- `.env.example` (수정)

---

# Phase 57. GitHub Actions CI/CD 파이프라인

## 목표
main push 시 자동 배포, PR 시 테스트만 수행하는 CI/CD를 구성합니다.

## workflow 분리
### 1. ci.yml
- `pull_request` 대상
- frontend install/test
- backend test

### 2. deploy.yml
- `push` to `main`
- build
- test
- docker image build/push
- EC2 ssh deploy

## 빌드 순서 원칙
- 테스트를 먼저 수행
- 그 다음 jar build / docker build
- 배포 전 healthcheck 검증

## Docker Registry
- 기본은 Docker Hub 기준
- 대안으로 GHCR 안내 가능
- 하나의 기준으로 먼저 작성하고, 문서에서 대안 소개 정도만 허용

## GitHub Secrets
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `EC2_HOST`
- `EC2_SSH_KEY`

필요 시:
- `EC2_PORT`
- `EC2_USER`
- `DOMAIN_NAME`

## deploy 방식
- EC2 SSH 접속
- `/home/ec2-user/app`
- `docker compose pull`
- `docker compose up -d`
- health check loop
- 오래된 이미지 정리

## 주의
- `docker compose` 와 `docker-compose` 를 혼용하지 말고 하나로 통일
- SSH action 사용 시 script를 너무 길게 넣지 말고, 가능하면 서버의 `deploy.sh`를 호출하는 방식도 허용

## 산출물
- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`
- `CICD_GUIDE.md`

---

# Phase 58. 모니터링 + 운영 안정화

## 목표
운영에 필요한 최소한의 헬스체크, 로깅, 백업, 재시작 체계를 구성합니다.

## Spring Boot Actuator
### 공통 원칙
- `/actuator/health` 는 헬스체크용
- 나머지 actuator는 보호

### application-prod.yml 확장
- `health`
- `info`
- 필요 시 `metrics`
- 과도한 노출은 피함

## SecurityConfig
- `/actuator/health` → permitAll
- `/actuator/**` → ADMIN 또는 내부 접근만 허용

## Nginx와 Actuator 관계
- health check 용도로 `/actuator/health` 는 Nginx를 통해 접근 가능
- 나머지 actuator 엔드포인트는 Nginx에서 막거나, 앱 Security에서 보호하는 방식 중 하나를 일관되게 선택

권장:
- `/actuator/health` 만 외부 health check 허용
- 그 외는 앱 Security에서 ADMIN 보호

## 커스텀 HealthIndicator
- 마지막 크롤링 성공 시각 기반 health 보조 지표
- 너무 오래 성공 이력이 없으면 DOWN까지는 아니더라도 상태 메시지에 반영 가능

## 로깅
### 원칙
- Docker 환경에서는 stdout 우선
- 필요 시 prod에서 rolling file 추가
- 과한 이중 로깅은 피함

### logback-spring.xml
- prod profile 전용 appender 구성 가능
- rolling policy 포함 가능

## 운영 스크립트
- `monitor.sh`
  - 헬스체크
  - 실패 시 재시작 시도
- `backup-db.sh`
  - mysqldump
  - 30일 이상 백업 삭제

## Crontab 예시
- monitor: 5분마다
- backup: 주 1회 새벽 3시

## 운영 문서
- 로그 보는 법
- 컨테이너 재시작
- health 확인
- backup 확인
- 장애 시 점검 순서

## 산출물
- `logback-spring.xml`
- `CrawlingHealthIndicator.java`
- `AppStartupNotifier.java`
- `scripts/monitor.sh`
- `scripts/backup-db.sh`
- `application-prod.yml` (수정)
- `SecurityConfig.java` (수정)
- `docker-compose.prod.yml` (수정)
- `OPERATION_GUIDE.md`

---

# Phase 59. 배포 테스트 + 검증

## 목표
배포 환경을 실제로 검증할 수 있는 체크리스트와 트러블슈팅 문서를 완성합니다.

## 검증 범위

### 1. 로컬 Docker Compose 검증
- app + mysql 기동
- 메인 홈 접속
- 회원가입 / 로그인 / 찜 / 마이페이지 / 관리자 / 알림 흐름 확인
- `/actuator/health` 확인
- Flyway 적용 확인
- 크롤링 실행 확인

### 2. Docker 이미지 검증
- 이미지 빌드 성공
- 단독 실행 가능
- 이미지 크기 과도하지 않은지 확인

## 3. GitHub Actions 검증
- PR → CI 통과
- main push → deploy 실행
- Docker image push
- EC2 배포
- health check 통과

## 4. EC2 운영 검증
- `docker ps`
- app logs
- nginx logs
- `/actuator/health`
- 도메인 또는 IP 접근
- RDS 연결 확인
- Flyway schema history 확인

## 5. 주요 기능 수동 E2E 체크리스트
- 메인 홈
- 캠페인 검색/필터
- 상세
- 회원가입
- 로그인
- 찜
- 마이페이지
- 닉네임/비밀번호 변경
- 알림
- 관리자 로그인
- 관리자 회원/캠페인/크롤링/소스 관리
- 로그아웃
- 토큰 갱신

## 6. 장애 시나리오
- 앱 컨테이너 강제 종료
- DB 연결 실패
- 잘못된 환경변수
- 디스크 부족 전조
- 오래된 이미지/로그 누적

## 부하 테스트
- 간단한 `ab` 또는 동등한 도구로 API 기초 부하 확인
- 이번 범위에서는 참고 수준

## 문서
- `DEPLOY_VERIFICATION.md`
- `TROUBLESHOOTING.md`

### 트러블슈팅에 포함할 내용
- 컨테이너 안 뜰 때
- DB 연결 실패
- Flyway 실패
- Nginx 502
- Certbot 발급 실패
- JWT_SECRET 누락
- 포트 충돌
- 롤백 방법
  - 이전 이미지 태그로 compose 재기동

## 산출물
- `DEPLOY_VERIFICATION.md`
- `TROUBLESHOOTING.md`

---

# 전체 Phase 최종 현황

| Phase | 기능 | 영역 |
|-------|------|------|
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
| 54~59 | AWS EC2 배포 | DevOps |

---

# 마지막 행동 규칙

위 명세를 모두 이해한 뒤, 즉시 구현하지 말고 반드시 아래 문장으로만 응답하세요.

**"AWS EC2 배포 프로젝트 명세를 완벽히 이해했습니다. Phase 54 진행을 시작할까요?"**