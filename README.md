# SAN-Backend

**Scrap-And-Notify (SAN)** 서비스의 백엔드 서버입니다.
Spring Boot 기반의 REST API를 제공합니다.

## 기술 스택

- **Framework:** Spring Boot 3.5, Spring Security, Spring Data JPA/Redis
- **Language:** Java 21
- **Database:** PostgreSQL (pgvector), Valkey (Redis 호환)
- **Build:** Gradle
- **API 문서:** Springdoc OpenAPI (Swagger)

## 시작하기

```bash
# 환경 변수 설정
cp .env.example .env

# 빌드
chmod +x gradlew
./gradlew build -x test

# 실행
./gradlew bootRun
```

## Docker

```bash
# 환경 변수 설정
cp .env.example .env

# 컨테이너 빌드 및 실행 (PostgreSQL + Valkey + Backend)
docker compose up --build

# 헬스체크
curl http://localhost:8080/actuator/health
```

Docker Compose는 다음 서비스를 포함합니다:

| 서비스 | 이미지 | 포트 | 설명 |
|--------|--------|------|------|
| `db` | `pgvector/pgvector:pg15` | 5432 | PostgreSQL + pgvector |
| `redis` | `valkey/valkey:8-alpine` | 6379 | Valkey (Redis 호환) |
| `backend` | 로컬 빌드 | 8080 | Spring Boot API 서버 |

## CI/CD 파이프라인

GitHub Actions 기반으로 세 가지 워크플로우가 동작합니다.

### 1. CI/CD Pipeline (`.github/workflows/ci-cd.yml`)

| 트리거 | 대상 브랜치 |
|--------|-------------|
| `push`, `pull_request` | `main`, `develop` |

**Build 단계 (모든 push/PR):**

```
Checkout → JDK 21 설치(Temurin) → Gradle 빌드(테스트 제외)
```

**Deploy 단계 (main push만):**

```
SSH 접속 → git fetch & reset → .env 복사(/var/www/san/.env.be) → docker compose up --build → 헬스체크(최대 60초 대기)
```

배포 시 헬스체크는 5초 간격으로 최대 12회 재시도하며, 실패 시 `docker compose logs`를 출력합니다.

### 2. PR Mattermost 알림 (`.github/workflows/pr-mattermost.yml`)

PR이 열리거나, 업데이트되거나, 머지/클로즈될 때 Mattermost 웹훅으로 알림을 전송합니다.

### 3. GitLab 동기화 (`.github/workflows/sync-to-gitlab.yml`)

`main` 브랜치에 push 시 GitLab의 `back/default` 브랜치로 자동 동기화됩니다. `workflow_dispatch`로 수동 실행도 가능합니다.

### 필요한 GitHub Secrets

| Secret | 용도 |
|--------|------|
| `DEPLOY_HOST` | 배포 서버 호스트 |
| `DEPLOY_USER` | 배포 서버 SSH 사용자 |
| `DEPLOY_SSH_KEY` | 배포 서버 SSH 키 |
| `MATTERMOST_WEBHOOK_URL` | Mattermost 알림 웹훅 URL |
| `GITLAB_TOKEN` | GitLab 동기화용 토큰 |

## 로컬 테스트

### 인프라만 Docker로 실행

DB와 Redis만 Docker로 띄우고 백엔드는 IDE에서 직접 실행하는 방식입니다.

```bash
# PostgreSQL + Valkey만 실행
docker compose up db redis -d

# 백엔드 직접 실행
cp .env.example .env
./gradlew bootRun
```

### 전체 Docker로 실행

```bash
cp .env.example .env
docker compose up --build

# 헬스체크
curl http://localhost:8080/actuator/health
```

### 빌드 & 테스트

```bash
# 빌드 (테스트 제외)
./gradlew build -x test

# 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.san.app.SomeTest"
```

### 환경 변수 (.env)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_PASSWORD` | PostgreSQL 비밀번호 | - |
| `JWT_SECRET` | JWT 서명 키 | - |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `AUTH_LOGIN_MAX_FAIL_COUNT` | 로그인 실패 허용 횟수 | `5` |
| `AUTH_LOGIN_FAIL_WINDOW_SECONDS` | 실패 카운트 윈도우(초) | `300` |
| `AUTH_LOGIN_LOCK_DURATION_SECONDS` | 계정 잠금 시간(초) | `900` |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID | - |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth Client Secret | - |
| `GITHUB_REDIRECT_URI` | GitHub OAuth 콜백 URI | `http://localhost:8080/api/auth/github/callback` |
| `GITHUB_SCOPE` | GitHub OAuth 스코프 | `read:user,repo` |
| `GITHUB_SUCCESS_REDIRECT_URI` | OAuth 성공 리다이렉트 | `http://localhost:5173/auth/github/success` |
| `GITHUB_FAILURE_REDIRECT_URI` | OAuth 실패 리다이렉트 | `http://localhost:5173/auth/github/failure` |
| `GITHUB_TOKEN_ENCRYPTION_SECRET` | GitHub 토큰 암호화 키 (Base64) | - |

### API 문서

서버 실행 후 Swagger UI에서 API를 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui/index.html
```
