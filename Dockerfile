# Eclipse Temurin Java 17 사용 (OpenJDK 공식 후속)
FROM eclipse-temurin:17-jdk

# 작업 디렉토리 설정
WORKDIR /app

# 시간대 설정
ENV TZ=Asia/Seoul

# 필요한 패키지 설치 (헬스체크 및 디버깅용)
RUN apt-get update && \
    apt-get install -y curl netcat-traditional && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성 (보안 강화)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# JAR 파일 복사 (Gradle은 build/libs 디렉토리에 생성)
COPY build/libs/*.jar app.jar

# 파일 소유권 변경
RUN chown appuser:appuser app.jar

# 포트 노출 (application.yml의 server.port 설정에 맞춤: 9789)
EXPOSE 9789

# 헬스체크 추가 (더 안정적인 설정)
HEALTHCHECK --interval=45s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:9789/actuator/health || exit 1

# 애플리케이션 사용자로 전환
USER appuser

# JVM 옵션 설정 및 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:+UseContainerSupport", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
