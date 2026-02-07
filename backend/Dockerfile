# Stage 1: Build stage with Java 25 and Gradle
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Install Gradle
RUN apk add --no-cache gradle

# Copy Gradle wrapper and build files
COPY gradlew ./
COPY gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts ./
COPY settings.gradle.kts ./

# Copy source code
COPY src ./src

# Build application using gradle wrapper
RUN chmod +x ./gradlew && \
    ./gradlew build --no-daemon

# Stage 2: Runtime stage with minimal JRE
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Install curl for health checks and wait utilities
RUN apk add --no-cache curl postgresql-client

# Copy the built application from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Expose port for Spring Boot application
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with startup wait
ENTRYPOINT ["java", "-jar", "app.jar"]
