# =============================================================================
# MULTI-STAGE DOCKERFILE
#
# Why multi-stage?
# Stage 1 (builder): has the full JDK + Maven — needed to compile the code.
#   Result: a fat JAR (~80MB with all dependencies).
# Stage 2 (runtime): only the JRE (no Maven, no JDK sources, no compiler).
#   Result: a slim image (~200MB vs ~500MB with full JDK).
#
# Interview: "Why not just use one stage?"
# The build tools (Maven, JDK) are not needed at runtime. They bloat the image
# and increase the attack surface. Multi-stage keeps the final image lean and secure.
# =============================================================================

# ── STAGE 1: BUILD ──────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first and download dependencies.
# Docker layer caching: if pom.xml hasn't changed, this layer is cached.
# Only re-downloaded when dependencies change — not on every code change.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Now copy source code and build.
# -DskipTests: we run tests in CI, not in the Docker build.
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ── STAGE 2: RUNTIME ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Security best practice: never run as root inside a container.
# Create a non-root user and group, switch to it.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy ONLY the built JAR from the builder stage. Nothing else.
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Document which port the app listens on (informational — Compose still maps ports).
EXPOSE 8080

# JVM tuning for containers:
# -XX:+UseContainerSupport        : respect Docker memory limits (not host RAM)
# -XX:MaxRAMPercentage=75.0       : use at most 75% of container memory for heap
# -Djava.security.egd=...urandom  : faster startup (avoids /dev/random blocking)
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
