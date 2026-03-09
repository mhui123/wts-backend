# WTS Backend

키움 증권 API를 연동한 실시간 주식 거래 시스템(WTS)의 Spring Boot 백엔드 서버입니다.

---

## 기술 스택

| 구분 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Build Tool | Gradle |
| DB | MySQL 8.0 / H2 (개발용) |
| Cache / Pub-Sub | Redis 7 (Lettuce) |
| 인증 | Google OAuth2 + JWT (jjwt 0.11.5) |
| 실시간 통신 | WebSocket (STOMP), Redis Pub/Sub |
| 외부 연동 | WebClient (Spring WebFlux) |
| 보안 | Spring Security, BCrypt, AES 암호화 |
| 기타 | Spring Data JPA, Querydsl, Lombok, Spring AOP, Spring Actuator |

---

## 주요 기능

### 인증 / 사용자 관리
- **Google OAuth2 로그인**: Google 계정으로 소셜 로그인, JWT 발급
- **이메일/비밀번호 로그인**: 자체 회원가입 및 로그인
- **게스트 모드**: 로그인 없이 대시보드 조회 등 제한적 기능 이용 가능
- JWT를 HttpOnly 쿠키로 전달하여 XSS 방어

### 대시보드 / 포트폴리오
- 사용자별 포트폴리오 현황 조회 (`/api/dash/getDashSummary`)
- 개별 종목 상세 정보 조회 (배당 이력 포함)
- 포트폴리오 아이템 최신화 (거래 이력 기반 자동 집계)
- 오실레이터 정보 조회

### 거래 이력 (Trade History)
- 거래 내역 페이징 조회 (날짜, 종목명, 거래 유형 필터링)
- NDJSON 형식 거래 내역 파일 업로드 및 파싱
- 키움, 토스 증권 등 다양한 브로커 유형 지원 (`BrokerType`)
- 국내/해외 주식 및 통화(`Currency`) 구분 처리

### 현금흐름 (Cashflow)
- 월별 현금흐름(입출금, 배당 등) 집계 및 조회
- KRW / USD 통화별 분리 집계
- 현금흐름 상세 내역 조회

### 키움 API 연동
- Python FastAPI 어댑터를 통한 키움 REST API 호출
- 계좌 잔고 조회, 관심종목 관리 (Watch Group / Watch List)
- API 키 AES 암호화 저장 (`KiwoomKeyService`)
- 권한 레벨별 API 접근 제어 (`KiwoomPermissionService`)
- 모든 API 호출에 대한 감사 로그 기록 (`KiwoomAuditService`)
- 키움 토큰 관리 (`KiwoomTokenManager`)
- 종목 코드/마켓 동기화

### 실시간 시세
- Python 서버가 Redis `realtime_price_data` 채널에 시세 Publish
- `RedisSubscriberService`가 수신 후 WebSocket STOMP로 브로드캐스트
- 클라이언트는 `/topic/quotes` 구독으로 실시간 데이터 수신

### 스케줄러
- 매일 새벽 2시: 종목 심볼-티커 매핑 테이블 자동 동기화 (`PortfolioScheduler`)

---

## 아키텍처 구조

```
[프론트엔드 (React/Vite)]
        │  REST API / WebSocket (STOMP)
        ▼
[WTS Backend (Spring Boot :9789)]
  ├── Spring Security (JWT + OAuth2)
  ├── REST Controllers
  │     ├── /api/account    - 계정 관리
  │     ├── /api/guest      - 게스트 모드
  │     ├── /api/dash       - 대시보드 / 포트폴리오
  │     ├── /api/th         - 거래 이력
  │     ├── /api/kiwoom     - 키움 API 프록시
  │     └── /api/kiwoom/auth - 키움 인증
  ├── WebSocket Broker (/ws → /topic/quotes)
  └── Redis Subscriber (realtime_price_data)
        │
        ├──▶ [MySQL 8.0 :3546]   - 거래 이력, 포트폴리오, 사용자 등 영속 데이터
        ├──▶ [Redis :6379]       - 실시간 시세 Pub/Sub
        └──▶ [Python 어댑터 :8000/:19987] - 키움 API 실제 호출
```

---

## 패키지 구조

```
com.wts
├── api/
│   ├── dto/          # OrderRequest, StockInfo, PythonRequestDto 등
│   ├── entity/       # Order (JPA 엔티티)
│   ├── repository/   # OrderRepository
│   ├── service/
│   │   ├── PythonServerService   # Python 어댑터 WebClient 통신
│   │   └── RedisSubscriberService # Redis → WebSocket 브릿지
│   └── web/
│       ├── PythonController
│       └── TestController
├── auth/
│   ├── controller/
│   │   ├── AccountController   # 로그인, 회원가입, 내 정보
│   │   └── GuestController     # 게스트 로그인, 대시보드
│   ├── dto/                    # JwtResponse, RegisterRequest
│   ├── jpa/
│   │   ├── entity/User
│   │   └── repository/UserRepository
│   ├── security/
│   │   ├── CustomOAuth2UserService
│   │   ├── JwtAuthenticationFilter
│   │   └── OAuth2AuthenticationSuccessHandler
│   ├── service/
│   │   ├── AccountService      # 계정 비즈니스 로직
│   │   └── GuestService        # 게스트 사용자 처리
│   └── JwtUtil
├── config/
│   ├── SecurityConfig          # Spring Security 필터 체인
│   ├── WebSocketConfig         # STOMP /ws 엔드포인트
│   ├── WebClientConfig         # Python 어댑터용 WebClient Bean
│   ├── WebConfig               # MVC 설정
│   ├── SpaWebConfig            # SPA 정적 리소스 라우팅
│   ├── RedisConfig             # Redis 연결 및 리스너 설정
│   └── QuerydslConfig          # Querydsl JPAQueryFactory
├── kiwoom/
│   ├── KiwoomApiController     # /api/kiwoom/** REST API
│   ├── KiwoomAuthController    # 키움 인증 관련 API
│   ├── dto/                    # KeyDto, WatchListDto, StockDto 등
│   ├── entity/                 # KiwoomApiKey, KiwoomToken, UserWatchGroup 등
│   ├── interceptor/            # KiwoomPermissionInterceptor
│   ├── repository/             # 키움 관련 JPA Repository
│   └── service/
│       ├── KiwoomApiService    # 키움 API 비즈니스 로직
│       ├── KiwoomAuditService  # API 호출 감사 로그
│       ├── KiwoomKeyService    # API 키 암호화/복호화
│       ├── KiwoomPermissionService # 권한 레벨 검증
│       ├── KiwoomPublicService # 인증 불필요 공개 서비스
│       └── KiwoomTokenManager  # 키움 토큰 수명 관리
├── model/                      # Money, Quantity, TradeHistoryVO 등 도메인 모델
├── scheduler/
│   └── PortfolioScheduler      # 주기적 포트폴리오 동기화
├── summary/
│   ├── adapter/                # TradeHistoryNdjsonAdapter
│   ├── controller/
│   │   ├── DashboardController
│   │   └── TradeHistoryController
│   ├── domain/
│   │   ├── cashflow/
│   │   ├── portfolio/          # Portfolio, PortfolioItem
│   │   └── service/            # CashflowDomainService, TradeHistoryDomainService
│   ├── dto/                    # DashboardSummaryDto, CashflowDto, TradeHistoryDto 등
│   ├── enums/                  # BrokerType, Currency, FlowType, FlowCategory 등
│   ├── jpa/
│   │   ├── entity/             # TradeHistory, CashflowEntity, PortfolioItemEntity 등
│   │   └── repository/         # JPA Repository + Specification
│   ├── mapper/                 # DashboardAssembler
│   └── service/
│       ├── CashflowService
│       ├── DashboardService
│       └── TradeHistoryService
└── util/
    ├── MapCaster               # Map 타입 변환 유틸
    ├── PortfolioCalculator     # 포트폴리오 수익률 계산
    └── UtilsForRequest         # 요청 관련 유틸
```

---

## 환경 변수 설정

애플리케이션 실행 전 아래 환경 변수를 설정해야 합니다.

| 환경 변수 | 설명 | 예시 |
|-----------|------|------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 | `GOCSPX-...` |
| `KIWOOM_ENCRYPTION_SECRET` | 키움 API 키 암호화 비밀키 (최소 32자) | `your-32-character-secret-key!!` |
| `KIWOOM_ENCRYPTION_SALT` | 암호화 Salt (32자리 HEX) | `0123456789abcdef0123456789abcdef` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 (기본값: localhost) | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 (기본값: 6379) | `6379` |
| `EXTERNAL_PYTHON_SERVER_BASE_URL` | Python 어댑터 URL | `http://localhost:8000` |
| `EXTERNAL_PYTHON_SERVER_PY32_URL` | Python 32비트 어댑터 URL | `http://localhost:19987` |

---

## 로컬 개발 환경 실행

### 사전 준비
- Java 17+
- MySQL 8.0 (포트 3546, DB명: `stockdb`)
- Redis 7+ (포트 6379)
- Python FastAPI 어댑터 서버 실행 중

### 실행 방법

```powershell
# 환경 변수 설정 (PowerShell)
$env:GOOGLE_CLIENT_ID = "your-client-id"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret"
$env:KIWOOM_ENCRYPTION_SECRET = "your-32-character-secret-key!!"
$env:KIWOOM_ENCRYPTION_SALT = "0123456789abcdef0123456789abcdef"

# 애플리케이션 실행
./gradlew bootRun
```

서버 기동 후 `http://localhost:9789` 에서 프론트엔드 정적 리소스 및 API를 제공합니다.

---

## Docker 실행

```powershell
# 이미지 빌드
docker build -t wts-backend:latest .

# .env 파일 생성 후 실행
docker-compose up -d
```

`docker-compose.yml`은 Redis 컨테이너를 포함합니다.  
MySQL과 Python 서버는 호스트(`host.docker.internal`) 또는 외부 네트워크(`wts-backend_wts-network`)를 통해 연결합니다.

---

## 주요 API 엔드포인트

### 인증
| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/account/login` | 이메일/비밀번호 로그인, JWT 쿠키 발급 |
| `POST` | `/api/account/register` | 회원가입 |
| `GET`  | `/api/account/getMyInfo` | 내 계정 정보 조회 |
| `POST` | `/api/guest/login` | 게스트 로그인, JWT 발급 |
| `GET`  | `/login/oauth2/code/google` | Google OAuth2 콜백 (자동 처리) |

### 대시보드 / 포트폴리오
| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/dash/getDashSummary` | 포트폴리오 대시보드 요약 |
| `GET` | `/api/dash/syncLatestPortfolioItems` | 포트폴리오 최신화 |
| `GET` | `/api/dash/getStockDetailInfo` | 개별 종목 상세 (배당 포함) |
| `GET` | `/api/dash/getOcilatorInfo` | 오실레이터 정보 |

### 거래 이력
| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/th/getTradesHistoryRenew` | 거래 이력 페이징 조회 |

### 키움 API
| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/kiwoom/account/balance` | 계좌 잔고 조회 |
| `*` | `/api/kiwoom/**` | 키움 API 프록시 (권한 레벨 필요) |

### WebSocket
| Endpoint | 설명 |
|----------|------|
| `ws://host/ws` | STOMP 연결 엔드포인트 |
| `/topic/quotes` | 실시간 시세 구독 주소 |

---

## 보안 정책

- `/`, `/login`, `/assets/**` 등 정적 리소스: **인증 불필요**
- `/api/guest/**`: 게스트 접근 허용
- `/api/**`: **JWT 인증 필요**
- `/api/kiwoom/**`: **키움 권한 레벨** 추가 검증 (`@PreAuthorize`)
- CORS: `http://localhost:5173`, `http://localhost:19789` 허용 (개발 환경)
- JWT는 HttpOnly + Secure + SameSite=None 쿠키로 전달
- 키움 API 키: AES 암호화 후 DB 저장

---

## 테스트

```powershell
./gradlew test
```

- 테스트 리포트: `build/reports/tests/test/index.html`
- 단위 테스트: JUnit 5 + Mockito
- MockWebServer를 이용한 WebClient 통합 테스트 지원

