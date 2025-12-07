# =============================================================================
# Aura Server Dockerfile
# Multi-stage build for Kotlin/Ktor application
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle files first for better layer caching
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy version catalog
COPY gradle/libs.versions.toml gradle/

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application
RUN gradle buildFatJar --no-daemon

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install necessary packages
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=Asia/Seoul

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create directories for storage
RUN mkdir -p /app/storage/audio && \
    chown -R appuser:appgroup /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*-all.jar /app/app.jar

# Change ownership
RUN chown appuser:appgroup /app/app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Environment variables (to be overridden at runtime)
ENV DB_URL=""
ENV DB_DRIVER="org.postgresql.Driver"
ENV DB_USER=""
ENV DB_PASSWORD=""
ENV DB_MAX_POOL_SIZE="10"
ENV REDIS=""
ENV GEMINI_API_KEY=""
ENV TTS_API_KEY=""
ENV VISION_API_KEY=""
ENV JWT_SECRET=""
ENV AUDIO_STORAGE_PATH="/app/storage/audio"

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
