# ============================================================
# VOLTARAS API Gateway — runtime image (Java 25)
# Uses the pre-built JAR produced by:
#   mvn clean package -DskipTests
# ============================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/api-gateway.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
