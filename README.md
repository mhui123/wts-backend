# WTS Backend (Spring Boot)

A minimal Spring Boot backend that fronts the Python Kiwoom adapter and fans out real-time quotes to the UI via STOMP WebSocket.

## Features
- WebFlux + WebClient: call Python FastAPI adapter (balance, orders, stocks)
- STOMP WebSocket: UI subscribes to `/topic/quotes` via `/ws`
- SSE→STOMP bridge: subscribes to Python SSE `/sse/quotes` and broadcasts to clients
- Basic security config: currently permitAll (tighten later)

## Project layout
- `src/main/java/com/wts/WtsApplication.java` – app entry
- `src/main/java/com/wts/config/WebSocketConfig.java` – STOMP broker config
- `src/main/java/com/wts/config/SecurityConfig.java` – security (permitAll)
- `src/main/java/com/wts/api/*` – REST endpoints (`/api/*`)
- `src/main/java/com/wts/infra/*` – Python adapter client + SSE bridge
- `src/main/java/com/wts/model/*` – DTOs
- `src/main/resources/application.yml` – settings

## Configure
Edit `src/main/resources/application.yml`:
```yaml
adapter:
  base-url: http://localhost:8000   # Python FastAPI base URL
quotes:
  codes: "005930,000660"           # optional: auto-subscribe on startup
```

## Run with Gradle (Windows, cmd)
Prereqs: Java 17+. If you have Gradle installed, we'll generate a wrapper; otherwise install Gradle or use an existing installation to run the wrapper task once.

- Start Python adapter (example): `uvicorn service:app --port 8000`
- In another terminal:
```cmd
cd d:\projects\toy\kwtool\wts-backend
gradle wrapper
gradlew.bat bootRun
```

Build a jar:
```cmd
gradlew.bat clean build -x test
java -jar build\libs\wts-backend-0.0.1-SNAPSHOT.jar
```

## Endpoints
- `GET /api/health` → proxy to Python `/health`
- `GET /api/account/balance`
- `POST /api/orders`
- WebSocket(STOMP): `ws://localhost:8080/ws`, subscribe to `/topic/quotes`

## Next steps
- Add persistence (orders/fills), idempotency, and risk checks
- Tighten security (JWT, CSRF for non-API, CORS rules)
- Map Python responses to typed DTOs and validation
 - If using JPA heavily, prefer Spring MVC over WebFlux or wrap blocking calls on boundedElastic

## 무엇을 만들었나 (파일 목록 및 역할)
- `src/main/java/com/wts/auth/KiwoomAuthController.java` – 인증 엔드포인트: `POST /auth/kiwoom` (요청 body: `{ "kiwoomToken": "..." }`)을 받아 키움 토큰 검증을 요청하고 내부 JWT를 반환합니다.
- `src/main/java/com/wts/auth/KiwoomAuthService.java` – 키움 검증 엔드포인트(`kiwoom.validate.url`)로 토큰을 전송하여 유효성을 확인합니다. 응답이 유효하면 `JwtUtil`로 내부 JWT를 생성하여 반환합니다.
- `src/main/java/com/wts/auth/JwtUtil.java` – 간단한 HS256 방식의 JWT 생성기(프로토타입용). 헤더/페이로드를 직접 조합하고 HMAC-SHA256으로 서명합니다.
- `src/main/resources/application.yml` – 다음 설정이 추가되었습니다:
  - `kiwoom.validate.url` : 키움(또는 검증서비스) 토큰 검증 엔드포인트 (예: `http://localhost:9000/validate`)
  - `app.jwt.secret` : 내부 JWT 서명용 비밀(예시값이 들어있음 — 운영 시 안전하게 관리 필요)
  - `app.jwt.exp-ms` : 내부 JWT 만료 시간(밀리초)
- `build.gradle` – JWT 관련 의존(deprecated/실험적 추가)을 포함하도록 수정했습니다. 현재 프로젝트는 내장 JWT 생성기를 사용하도록 구현되어 있습니다.

## 주의 및 권장 개선사항 (중요)
- 비밀 관리: `app.jwt.secret` 값을 소스나 리포지토리에 두지 마십시오. 운영환경에서는 환경변수 또는 Vault 같은 비밀관리 솔루션을 사용하세요.
- 서명 검증 우선: 키움이 서명된 토큰(공개키로 검증 가능한 JWT 등)을 제공하면 서버는 서명을 직접 검증하는 방식으로 구현하는 것이 가장 안전합니다. (검증 API 호출보다 더 신뢰할 수 있음)
- 검증 API 스펙: 현재 `KiwoomAuthService`는 `POST { "token": "..." }` 형태로 `kiwoom.validate.url`에 요청을 보내고, `{ "valid": true, "userId": "..." }`와 같은 응답을 기대합니다. 실제 키움 검증 엔드포인트의 스펙에 맞춰 요청/응답 파싱을 조정하세요.
- 라이브러리 사용: 프로덕션에서는 자체 JWT 구현 대신 검증된 라이브러리(e.g. jjwt, nimbus-jose-jwt)를 사용하세요. 키 길이, 알고리즘, 클레임 검증 등 보안 고려사항이 많습니다.
- 통신 보안: 클라이언트↔서버, 서버↔키움(검증서비스) 간 통신은 항상 TLS(HTTPS)로 보호하세요.
- 토큰 수명 및 리프레시: 내부 JWT 만료시간과 리프레시/폐기 정책(블랙리스트 등)을 설계하세요. 단기간 토큰과 리프레시 토큰 조합이 일반적입니다.
- 로깅 민감정보 주의: 원본 키움 토큰이나 민감 클레임은 로그에 남기지 마십시오.
- 테스트/모킹: 개발 환경에서 `kiwoom.validate.url`은 키움 대신 간단한 목 서버로 대체하여 테스트하세요(예: `{ "valid": true, "userId": "kiwoom-123" }`).

## 환경변수로 JWT 비밀 설정 (권장)

프로덕션에서는 `app.jwt.secret`을 리포지토리나 소스에 두지 않고 비밀관리(환경변수/시크릿스토어)를 통해 주입해야 합니다.

이 프로젝트는 다음 우선순위로 JWT 비밀을 읽습니다:
1. 환경변수 `APP_JWT_SECRET` (우선)
2. `application.yml`의 `app.jwt.secret`
3. 하드코딩된 기본값(개발용, 권장하지 않음)

아래는 몇 가지 실행/배포 환경에서의 설정 예시입니다.

- Windows (cmd.exe):
```cmd
set APP_JWT_SECRET=your-very-secret-value
cd D:\projects\toy\kwtool\wts-backend
gradlew.bat bootRun
```

- Windows PowerShell:
```powershell
$env:APP_JWT_SECRET = "your-very-secret-value"
cd D:\projects\toy\kwtool\wts-backend
.\gradlew.bat bootRun
```

- Linux / macOS (bash):
```bash
export APP_JWT_SECRET=your-very-secret-value
./gradlew bootRun
```

- Docker (환경변수 주입):
```bash
docker run -e APP_JWT_SECRET=your-very-secret-value -p 8080:8080 your-image:tag
```

- Kubernetes (Secret을 사용한 예시, 간단화):
1) Secret 생성:
```bash
kubectl create secret generic wts-secret --from-literal=APP_JWT_SECRET=your-very-secret-value
```
2) Deployment의 컨테이너에 envFrom으로 주입:
```yaml
envFrom:
  - secretRef:
      name: wts-secret
```

주의사항:
- 비밀 값은 가능한 한 긴(예: 32바이트 이상의 랜덤값) 문자열을 사용하고 안전하게 관리하세요.
- CI/CD 파이프라인에 비밀을 노출하지 않도록 주의하세요(시크릿 매니저 사용 권장).
- `application.yml`의 예시값은 개발 편의를 위한 것으로, 운영환경에서는 사용하지 마세요.

(이하 기존 README 내용 유지)
