# ============================================================
# BillingSystem Pro — Dockerfile for Railway / Render / Docker
# ============================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY .mvn/ .mvn/
COPY mvnw.cmd pom.xml ./

# Copy source
COPY src/ src/

# Build the JAR (skip tests for faster builds)
RUN chmod +x mvnw.cmd 2>/dev/null || true
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q && \
    mv target/BillingSystemWeb-*.jar target/app.jar

# Stage 2: Run (lightweight JRE image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S billing && adduser -S billing -G billing

# Copy the built JAR
COPY --from=builder /app/target/app.jar app.jar

# Set ownership
RUN chown billing:billing app.jar
USER billing

# Railway injects PORT env var; Spring Boot reads it via server.port=${PORT:8080}
EXPOSE 8080

# Start with prod profile
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar", \
  "--spring.profiles.active=prod"]
