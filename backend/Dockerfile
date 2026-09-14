# syntax=docker/dockerfile:1
# 운영 배포용 이미지. GitHub Actions 에서 buildx 로 linux/amd64 빌드 (EC2 t3 계열, x86_64).

# ── 빌드 스테이지 ──
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 의존성 레이어 캐싱 — 빌드 스크립트만 먼저 복사해 리졸브 (소스만 바뀌면 이 레이어 재사용)
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --version

COPY src src
# 테스트는 CI(ci.yml)에서 이미 돌았으므로 이미지 빌드에서는 스킵
RUN ./gradlew bootJar --no-daemon -x test

# ── 런타임 스테이지 ──
FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home --home-dir /app --shell /usr/sbin/nologin bandive
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R bandive:bandive /app
USER bandive

EXPOSE 8080
# 컨테이너 메모리 제한에 맞춰 힙을 자동 산정 (고정 -Xmx 대신)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
