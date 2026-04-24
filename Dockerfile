# ─────────────────────────────────────────────────────────────
# Stage 1: Build
# Uses the official Maven image to compile and package the app.
# The Maven cache layer is separated from the source copy to
# maximise cache reuse on subsequent builds.
# ─────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy dependency descriptor first — allows Docker to cache the
# dependency download layer independently of source changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build, skipping tests (tests run in CI, not here).
COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Slim JRE image — no build tools shipped to production.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Non-root user for security hardening
RUN groupadd -r booking && useradd -r -g booking booking
USER booking

COPY --from=builder /app/target/booking-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
