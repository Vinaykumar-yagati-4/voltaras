# ============================================================
# VOLTARAS Meter Management Service — runtime image (Java 25)
# Uses the pre-built JAR produced by:
#   mvn clean package -DskipTests
# ============================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/meter-management-service.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
