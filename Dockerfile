# ============================================================
# Stage 1: Build  (Gradle로 소스 컴파일 → bootJar 생성)
# ============================================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle wrapper 및 설정 파일을 먼저 복사 (의존성 레이어 캐시 활용)
COPY gradlew gradlew.bat* ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# 의존성만 먼저 다운로드 (소스 미변경 시 캐시 재사용)
RUN ./gradlew dependencies --no-daemon -q

# 소스 코드 복사 후 JAR 빌드 (테스트 제외)
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ============================================================
# Stage 2: Runtime  (JRE 경량 이미지)
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Seoul

# curl (헬스체크용)
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 애플리케이션 전용 사용자 생성 (보안)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 빌드 스테이지에서 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appuser app.jar

# 포트 노출 (application.yml server.port: 9789)
EXPOSE 9789

# 헬스체크
HEALTHCHECK --interval=45s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:9789/actuator/health || exit 1

USER appuser

ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:+UseContainerSupport", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

#