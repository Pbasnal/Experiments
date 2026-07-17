# syntax=docker/dockerfile:1

# --- Stage 1: build React reader + Spring Boot JAR ---
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY .mvn/settings.xml .mvn/settings.xml
COPY pom.xml .
COPY reader reader
COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -s .mvn/settings.xml -B -DskipTests package \
    && cp target/amarkatha-*.jar /workspace/app.jar

# --- Stage 2: runtime ---
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl webp \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system amarkatha \
    && useradd --system --gid amarkatha --home-dir /app --shell /usr/sbin/nologin amarkatha

COPY --from=build /workspace/app.jar /app/app.jar
COPY docker/entrypoint.sh /entrypoint.sh

RUN mkdir -p /data/media \
    && chown amarkatha:amarkatha /data/media \
    && chmod +x /entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["/entrypoint.sh"]
