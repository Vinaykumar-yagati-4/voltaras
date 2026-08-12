# ============================================================
# VOLTARAS Eureka Server — runtime image (Java 25)
# Uses the pre-built JAR produced by:
#   mvn clean package -DskipTests
# ============================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/eureka-server.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
