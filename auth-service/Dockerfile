# ============================================================
# VOLTARAS Auth Service — Production Dockerfile
# Multi-stage build: Maven build stage + slim JRE runtime stage
# ============================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Cache dependencies first (only the POM is copied, so layers are reused
# unless the POM itself changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and package (tests skipped in the image build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a non-root user for production safety
RUN groupadd -r voltaras && useradd -r -g voltaras voltaras

# Copy the executable JAR from the build stage
COPY --from=build /app/target/auth-service.jar app.jar

# Run as non-root
USER voltaras

# Service port
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
