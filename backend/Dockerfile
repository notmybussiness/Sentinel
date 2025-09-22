# Multi-stage build for Spring Boot
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build the application
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# Copy built jar
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown spring:spring app.jar
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]