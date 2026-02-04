@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion


echo =================================
echo Docker Compose WTS Backend 실행
echo =================================


REM 1. .env 파일 존재 확인
echo.
echo 1. 환경변수 파일 확인...
if not exist ".env" (
    echo [경고] .env 파일이 없습니다!
    echo .env.example을 참고하여 .env 파일을 생성하세요.
    pause
)

REM 2. 기존 컨테이너 정리
echo.
echo 2. 기존 컨테이너 강제 정리...
docker-compose down --volumes --remove-orphans

REM 3. Gradle Clean 빌드 (완전히 새로 빌드)
echo.
echo 3. Gradle Clean 빌드 수행...
call gradlew.bat clean bootJar
if errorlevel 1 (
    echo [오류] Gradle 빌드 실패!
    pause
    exit /b 1
)

REM 4. JAR 파일 존재 확인
echo.
echo 4. JAR 파일 확인...
if not exist "build\libs\wts-backend-0.0.1-SNAPSHOT.jar" (
    echo [오류] JAR 파일이 생성되지 않았습니다!
    pause
    exit /b 1
)
echo [확인] JAR 파일 생성 완료

REM 5. Docker 이미지 재빌드
echo.
echo 5. Docker 이미지 재빌드...
docker build -t wts-backend:latest .
if errorlevel 1 (
    echo [오류] Docker 이미지 빌드 실패!
    pause
    exit /b 1
)

REM --no-cache는 매번 전체 레이어를 새로 빌드하여 시간 소요가 크므로 docker build 에서 제거함.

REM 6. Docker Compose 실행
echo.
echo 6. Docker Compose 실행...
docker-compose up -d
if errorlevel 1 (
    echo [오류] Docker Compose 실행 실패!
    pause
    exit /b 1
)

REM 7. 백엔드 컨테이너 시작 대기
echo.
echo 7. 백엔드 컨테이너 시작 대기 (30초)...
timeout /t 30 /nobreak >nul

REM 7-1. Python 컨테이너 연결 테스트
echo.
echo 7-1. Python 컨테이너 연결 테스트...

REM exec 대상 컨테이너가 실제로 존재하는지 먼저 확인 (없으면 docker exec가 즉시 실패)
docker ps --format "{{.Names}}" | findstr /R /C:"^wts-backend-container$" >nul
set "BACKEND_CONTAINER_EXISTS=!errorlevel!"

if not "!BACKEND_CONTAINER_EXISTS!"=="0" (
    echo [경고] 백엔드 컨테이너(wts-backend-container)를 찾지 못했습니다. docker ps로 이름을 확인하세요.
) else (
    REM curl 응답(JSON 등)이 배치 파서에 섞이지 않도록 stdout/stderr를 완전히 버리고, errorlevel을 즉시 캡처
    docker exec wts-backend-container curl -s http://wts-python-app:19789/health 1>nul 2>nul
    set "PY_HEALTH_EXIT=!errorlevel!"

    if "!PY_HEALTH_EXIT!"=="0" (
        echo [확인] Python 컨테이너 연결 성공!
    ) else (
        echo [경고] Python 컨테이너 연결 실패!
        echo - Python 컨테이너가 실행 중인지 확인하세요.
        echo - 같은 네트워크(wts-backend_wts-network)에 있는지 확인하세요.
    )
)

REM 8. 로그 확인
echo.
echo 8. 백엔드 로그 확인...
docker-compose logs --tail=50 wts-backend

echo.
echo =================================
echo 로그를 계속 보려면 아무 키나 누르세요
echo =================================
pause

docker-compose logs -f wts-backend

endlocal