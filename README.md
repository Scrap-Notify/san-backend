# SAN-Backend

**Scrap-And-Notify (SAN)** 서비스의 백엔드 서버입니다.

## 로컬 테스트

### 사전 준비

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치 및 실행
- `act` 설치 (Windows)
  ```powershell
  winget install nektos.act
  ```
  > macOS: `brew install act` / Linux: [공식 설치 가이드](https://nektosact.com/installation/index.html)

### Docker 실행

```bash
# 컨테이너 빌드 및 실행
docker compose up -d --build

# 종료
docker compose down

# 로그 확인
docker compose logs -f

# 헬스체크
curl http://localhost:8080/health
```

### CI 로컬 테스트 (act)

```bash
# build job만 실행
act -j build --container-architecture linux/amd64
```
