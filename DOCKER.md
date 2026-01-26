# WTS Backend Docker 실행 가이드

## 빠른 시작

### 1. Docker Compose로 전체 스택 실행
```powershell
# 프로젝트 빌드 및 Docker Compose 실행
./scripts/run-docker.bat
```

### 2. 상태 확인
```powershell
# 헬스체크 및 연결 상태 확인
./scripts/check-docker-health.bat
```

## 상세 설정

### 환경변수 설정
Docker Compose 실행 전에 `.env` 파일을 생성하여 환경변수를 설정할 수 있습니다:

```env
# .env 파일 예시
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KIWOOM_ENCRYPTION_SECRET=your-encryption-secret
KIWOOM_ENCRYPTION_SALT=your-encryption-salt
```

### 개별 서비스 관리

#### MySQL 단독 실행
```powershell
docker run -d --name wts-mysql \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=stockdb \
  -p 3546:3306 \
  mysql:8.0
```

#### WTS Backend 단독 실행
```powershell
# MySQL이 실행된 후
docker run -d --name wts-backend-container \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3546/stockdb \
  -p 9789:9789 \
  wts-backend:latest
```

## 포트 매핑

| 서비스 | 컨테이너 포트 | 호스트 포트 | 설명 |
|--------|---------------|-------------|------|
| WTS Backend | 9789 | 9789 | Spring Boot 애플리케이션 |
| MySQL | 3306 | 3546 | 데이터베이스 |
| Redis | 6379 | 6379 | 캐시 및 세션 스토어 |

## 트러블슈팅

### MySQL 연결 실패
```powershell
# 컨테이너 간 네트워크 연결 확인
docker network inspect wts_wts-network

# MySQL 컨테이너 직접 접속
docker exec -it wts-mysql mysql -u root -p1234
```

### Backend 시작 실패
```powershell
# 상세 로그 확인
docker-compose logs wts-backend

# 컨테이너 내부 접근
docker exec -it wts-backend-container bash
```

### 포트 충돌
기존에 실행 중인 서비스가 있다면 포트를 변경하세요:
```yaml
# docker-compose.yml
services:
  wts-backend:
    ports:
      - "9790:9789"  # 9789 대신 9790 사용
```

## API 확인

서비스가 정상 실행되면 다음 URL에서 확인할 수 있습니다:

- **헬스체크**: http://localhost:9789/actuator/health
- **API 문서**: http://localhost:9789/swagger-ui.html (설정된 경우)
- **H2 콘솔**: http://localhost:9789/h2-console (개발 모드 시)

## 데이터 영속화

MySQL 데이터는 Docker 볼륨에 저장되어 컨테이너 재시작 후에도 유지됩니다:
```powershell
# 볼륨 확인
docker volume ls | findstr wts

# 볼륨 삭제 (데이터 초기화)
docker volume rm wts_mysql_data
```

## 환경별 설정

### 개발 환경 (로컬)
```powershell
# H2 DB 사용
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Docker 환경
```powershell
# MySQL + Redis 사용
docker-compose up -d
```

### 운영 환경
환경변수를 통한 외부 설정 주입:
```yaml
environment:
  - SPRING_DATASOURCE_URL=${DB_URL}
  - SPRING_DATASOURCE_USERNAME=${DB_USER}
  - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
```
