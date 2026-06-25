# 빌드 컨텍스트는 저장소 루트(.)이다. (docker-compose 에서 context: . 로 빌드)

# ---- Frontend build stage ----
FROM node:20-alpine AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
# 백엔드와 동일 출처(/api)로 호출
ENV VITE_API_BASE=/api
RUN npm run build
# 산출물: /fe/dist

# ---- Backend build stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
# 의존성 캐싱: 빌드 스크립트 먼저 복사
COPY backend/settings.gradle backend/build.gradle ./
RUN gradle dependencies --no-daemon || true
COPY backend/src ./src
# 빌드된 프론트엔드를 Spring Boot 정적 리소스로 포함
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN gradle bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN useradd -r -u 1001 spring && mkdir -p /app/uploads && chown -R spring:spring /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
