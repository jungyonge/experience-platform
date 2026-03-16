# 체험단 통합 플랫폼

체험단 정보를 모아서 보여주는 플랫폼입니다.

## 기술 스택

- **Backend**: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, H2
- **Frontend**: React + TypeScript + Vite
- **빌드**: 모노레포 (Gradle 내 프론트엔드 빌드 통합)

## 프로젝트 구조

```
├── src/main/java/com/example/experienceplatform/
│   ├── ExperiencePlatformApplication.java
│   └── member/
│       ├── domain/          # 엔티티, VO, 리포지토리 인터페이스
│       ├── application/     # 유스케이스, Command/Info DTO
│       ├── infrastructure/  # JPA 리포지토리 구현체
│       └── interfaces/      # REST Controller, Request/Response DTO
├── src/main/resources/
│   ├── application.yml
│   └── application-local.yml
└── frontend/                # React + Vite + TypeScript
```

## 실행 방법

### 백엔드 (개발 모드)
```bash
./gradlew bootRun
```

### 프론트엔드 (개발 모드)
```bash
cd frontend
npm install
npm run dev
```

### 통합 빌드
```bash
./gradlew bootJar
```
프론트엔드 빌드 결과가 Spring Boot의 static 리소스로 자동 복사됩니다.

### H2 콘솔
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:experiencedb`
- Username: `sa`
