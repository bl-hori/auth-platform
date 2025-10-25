# Multi-stage Dockerfile for Auth Platform Backend
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /workspace/app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Grant execute permission for gradlew
RUN chmod +x gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src
COPY config config

# Build the application
RUN ./gradlew clean build -x test --no-daemon

# Extract the built JAR layers for better Docker layer caching
# Exclude the -plain.jar and use only the bootJar
RUN mkdir -p build/dependency && \
    cd build/dependency && \
    JAR_FILE=$(ls ../libs/*-SNAPSHOT.jar | grep -v plain) && \
    java -Djarmode=layertools -jar $JAR_FILE extract

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy dependencies from builder stage
COPY --from=builder --chown=spring:spring /workspace/app/build/dependency/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/app/build/dependency/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/app/build/dependency/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/app/build/dependency/application/ ./

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

# JVM options can be passed via JAVA_OPTS environment variable
# Example: docker run -e JAVA_OPTS="-Xmx512m -Xms256m" image-name
