# ============================================================
# BillingSystem Pro — Dockerfile (Railway / Render / Docker)
# ============================================================

# Stage 1: Build with Maven (uses official Maven+JDK image)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml first (cache dependency downloads)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q
RUN mv target/BillingSystemWeb-*.jar target/app.jar

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user
RUN groupadd -r billing && useradd -r -g billing billing

# Copy the built JAR
COPY --from=builder /app/target/app.jar app.jar
RUN chown billing:billing app.jar

USER billing

# Render/Railway inject PORT automatically
EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
