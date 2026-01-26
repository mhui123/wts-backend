@echo off
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

REM 5. Docker 이미지 강제 재빌드 (캐시 없이)
echo.
echo 5. Docker 이미지 재빌드...
docker build --no-cache -t wts-backend:latest .
if errorlevel 1 (
    echo [오류] Docker 이미지 빌드 실패!
    pause
    exit /b 1
)

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