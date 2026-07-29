# VOLTARAS Eureka Server

## Purpose

Eureka Server acts as the **Service Registry** for all VOLTARAS microservices. Every microservice (Auth, User, Meter, Billing, Payment, Notification) registers itself with Eureka on startup. The API Gateway and microservices query Eureka to discover the network location of other services dynamically — without hardcoded addresses.

## Port

- **Default port:** `8761`
- Configurable via `server.port` in `application.yml` or runtime argument `--server.port=<port>`.

## Run Command

### Using Maven (development)

```bash
# From the project root (voltaras/)
cd eureka-server
mvn spring-boot:run
```

### Using Maven with profile (optional)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Using the packaged JAR

```bash
mvn clean package -DskipTests
java -jar target/eureka-server.jar
```

## Dashboard URL

Once the server is running, open the Eureka Dashboard in a browser:

```
http://localhost:8761
```

The dashboard displays:
- **Status** — "System Status" section with current memory, uptime, and health.
- **Instances** — "Instances currently registered with Eureka" list (initially empty until other services start).
- **General Info** — Environment, data center, and total available memory.

## Expected Verification Steps

1. **Start the server** — Run `mvn spring-boot:run` from the `eureka-server/` directory.
2. **Check console output** — Look for the banner and log line:
   ```
   Started EurekaServerApplication in X.XXX seconds (process running for X.XXX)
   ```
3. **Open dashboard** — Navigate to `http://localhost:8761`.
4. **Verify no self-registration** — The dashboard should show **zero** registered instances (Eureka Server is configured not to register itself).
5. **Check health endpoint** — Visit `http://localhost:8761/actuator/health` — should return:
   ```json
   {
     "status": "UP"
   }
   ```
6. **Stop the server** — Press `Ctrl+C` to stop gracefully.

## Startup Order

Eureka Server must be started **before** any other VOLTARAS microservice or the API Gateway.

```
1. Eureka Server    (port 8761)
2. API Gateway      (port 8080)
3. Auth Service     (port 8081)
4. User Service     (port 8082)
5. Meter Service    (port 8083)
6. Billing Service  (port 8084)
7. Payment Service  (port 8085)
8. Notification Svc (port 8086)
```

## Dependencies

- **Spring Boot 3.5.5**
- **Spring Cloud 2025.0.3** (Netflix Eureka Server)
- **Spring Boot Actuator** — health checks and monitoring
- **Java 25**

## Notes

- This module is **independent** from all business modules. It contains no business logic, no database, and no API endpoints beyond Eureka and Actuator.
- Self-registration is disabled (`register-with-eureka: false`) because the Eureka Server itself does not need to discover itself.
- Registry fetching is disabled (`fetch-registry: false`) as the Eureka Server does not need to cache its own registry.
- Self-preservation mode is enabled by default to protect against network partitions.
