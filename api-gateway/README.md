# VOLTARAS API Gateway

## Purpose

API Gateway acts as the **single entry point** for all client requests into the VOLTARAS microservices ecosystem. It uses Spring Cloud Gateway to route requests to the appropriate backend services dynamically via Eureka service discovery.

## Port

- **Default port:** `8080`
- Configurable via `server.port` in `application.yml` or runtime argument `--server.port=<port>`.

## Features

- **Dynamic routing** — Routes requests to registered microservices via Eureka discovery (no routes configured yet).
- **Eureka client** — Registers itself with Eureka Server and fetches the service registry.
- **Health checks** — Actuator endpoints for monitoring and health verification.

## Run Command

### Using Maven (development)

```bash
# From the project root (voltaras/)
cd api-gateway
mvn spring-boot:run
```

### Using Maven with profile (optional)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Using the packaged JAR

```bash
mvn clean package -DskipTests
java -jar target/api-gateway.jar
```

## Verification Steps

1. **Ensure Eureka Server is running** — Start Eureka Server on port `8761` first.
2. **Start the gateway** — Run `mvn spring-boot:run` from the `api-gateway/` directory.
3. **Check console output** — Look for the banner and log line:
   ```
   Started ApiGatewayApplication in X.XXX seconds (process running for X.XXX)
   ```
4. **Verify Eureka registration** — Open the Eureka Dashboard at `http://localhost:8761` and confirm `API-GATEWAY` appears in the registered instances list.
5. **Check health endpoint** — Visit `http://localhost:8080/actuator/health` — should return:
   ```json
   {
     "status": "UP"
   }
   ```
6. **Check info endpoint** — Visit `http://localhost:8080/actuator/info` — should return basic application metadata.
7. **Stop the gateway** — Press `Ctrl+C` to stop gracefully.

## Startup Order

API Gateway must be started **after** Eureka Server and **before** any other microservice that needs to be routed through it.

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
- **Spring Cloud 2025.0.3**
  - Spring Cloud Gateway — reactive API gateway
  - Netflix Eureka Client — service discovery
- **Spring Boot Actuator** — health checks and monitoring
- **Java 25**

## Notes

- No routes are configured yet. Routes will be added in subsequent steps as backend services are developed.
- No JWT filter or security configuration is included in this initial setup.
- This module contains **no business logic** — it only routes requests to downstream services.
- The gateway registers itself with Eureka (`register-with-eureka: true`) and fetches the registry (`fetch-registry: true`) to enable dynamic service discovery for routing.
