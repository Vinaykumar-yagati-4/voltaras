# VOLTARAS Organization Service

## Service Name

`organization-service`

## Purpose

The Organization Service introduces **optional organization membership** to the
VOLTARAS platform. It manages:

- Organizations (hostel, institution, apartment, commercial)
- Organization memberships and organization-level roles
- Join requests (request, approve, reject, cancel)
- The physical structure hierarchy: Buildings → Blocks → Floors → Units

Organization membership is **strictly optional** — public registration, login,
and personal utility features continue to work for every user through the
Auth Service and User Service without any organization.

Authentication identity comes from the API Gateway headers `X-User-Id` and
`X-User-Role`. This service **does not parse or validate JWTs**.

## Tech Stack

- Java 25
- Spring Boot 3.5.5
- Spring Cloud 2025.0.3 (Eureka Client)
- Spring Data JPA · Jakarta Validation · MySQL
- Lombok · MapStruct · Spring Boot Actuator
- Spring Security (customized at implementation time)

## Port

`8085`

## Database

`organization_db` (MySQL)

> Set `DB_USERNAME` and `DB_PASSWORD` environment variables. No credentials
> are hardcoded.

## Base Package

`com.voltaras.organizationservice`

## Project Structure

```
organization-service/
├── pom.xml
├── Dockerfile
├── README.md
├── .gitignore
└── src/main/
    ├── java/com/voltaras/organizationservice/
    │   ├── OrganizationServiceApplication.java
    │   ├── config/
    │   ├── constant/
    │   ├── controller/
    │   ├── dto/request/
    │   ├── dto/response/
    │   ├── entity/
    │   ├── enums/
    │   ├── exception/
    │   ├── mapper/
    │   ├── repository/
    │   ├── security/
    │   ├── service/impl/
    │   └── util/
    └── resources/
        └── application.yml
```

## Run Command

```bash
# Local development (requires Eureka at http://localhost:8761 and MySQL)
cd organization-service
DB_USERNAME=root DB_PASSWORD=yourpassword mvn spring-boot:run
```

## Build Command

```bash
cd organization-service
mvn clean package -DskipTests
# Output: target/organization-service.jar
```

## Docker Commands

```bash
# Build the image
docker build -t voltaras/organization-service:0.0.1 .

# Run the container (Eureka + MySQL must be reachable)
docker run -p 8085:8085 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=yourpassword \
  -e EUREKA_SERVER_URL=http://host.docker.internal:8761/eureka/ \
  voltaras/organization-service:0.0.1
```

> The image runs as a non-root user and exposes port `8085`.

## Health Check

```bash
curl http://localhost:8085/actuator/health
```

## Notes

- Skeleton only — no business logic, entities, repositories, controllers,
  DTOs, or tests yet.
- Every empty package contains a `package-info.java`.
- Postman (not Swagger) is used for API documentation/testing (see
  `docs/12_ORGANIZATION_SERVICE_API_TEST_PLAN.md`).
