# VOLTARAS User Service

## Purpose

The **User Service** manages consumer profile information for the VOLTARAS platform. It stores the rich profile details (full name, contact info, address, demographics) linked to an authenticated user from the Auth Service via `authUserId`.

## Port

- **Default port:** `8082`
- Configurable via `server.port` in `application.yml` or runtime argument `--server.port=<port>`.

## Features

- **Profile CRUD** — Create, read, update, and delete consumer profiles
- **One profile per user** — Enforced via a unique `authUserId` constraint and a `DuplicateResourceException`
- **Ownership scoping** — Every operation is scoped by `authUserId`, so a profile always belongs to its auth user
- **Validation** — Jakarta Bean Validation on all request DTOs
- **Eureka Client** — Registers with Eureka Server for service discovery
- **Actuator Endpoints** — Health checks and monitoring
- **Global Exception Handling** — Standardized JSON error responses

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/api/users/profile` | Create a new user profile |
| `GET`    | `/api/users/profile/{authUserId}` | Get the profile for an auth user |
| `PUT`    | `/api/users/profile/{authUserId}` | Update the profile for an auth user |
| `DELETE` | `/api/users/profile/{authUserId}` | Delete the profile for an auth user |

## Run Command

### Prerequisites

1. **MySQL** — Running on localhost:3306 with database `user_db` created
2. **Eureka Server** — Running on port 8761

### Using Maven (development)

```bash
cd user-service
mvn spring-boot:run
```

### Using the packaged JAR

```bash
mvn clean package -DskipTests
java -jar target/user-service.jar
```

## Verification Steps

1. **Ensure Eureka Server is running** on port 8761
2. **Create the database** in MySQL: `CREATE DATABASE IF NOT EXISTS user_db;`
3. **Start the service**: `cd user-service && mvn spring-boot:run`
4. **Check Eureka Dashboard** at `http://localhost:8761` — `USER-SERVICE` should appear
5. **Test Create Profile**:
   ```bash
   curl -X POST http://localhost:8082/api/users/profile \
     -H "Content-Type: application/json" \
     -d '{
       "authUserId": 1,
       "fullName": "John Doe",
       "phone": "9876543210",
       "address": "123 Main Street, Mumbai",
       "city": "Mumbai",
       "state": "Maharashtra",
       "country": "India",
       "postalCode": "400001",
       "dateOfBirth": "1995-05-10",
       "gender": "MALE"
     }'
   ```
6. **Test Get Profile**: `curl http://localhost:8082/api/users/profile/1`
7. **Test Update Profile** (full replacement — PUT semantics):
   ```bash
   curl -X PUT http://localhost:8082/api/users/profile/1 \
     -H "Content-Type: application/json" \
     -d '{
       "fullName": "John A. Doe",
       "phone": "9123456780",
       "address": "123 Main Street, Mumbai",
       "city": "Mumbai",
       "state": "Maharashtra",
       "country": "India",
       "postalCode": "400001",
       "dateOfBirth": "1995-05-10",
       "gender": "MALE"
     }'
   ```
8. **Test Delete Profile**: `curl -X DELETE http://localhost:8082/api/users/profile/1`
9. **Check health**: `curl http://localhost:8082/actuator/health`

## Dependencies

- **Spring Boot 3.5.5** — Application framework
- **Spring Cloud 2025.0.3** — Cloud dependencies
  - Netflix Eureka Client — Service discovery
- **Spring Data JPA** — Database access
- **Spring Validation** — Jakarta Bean Validation
- **MySQL Connector** — Database connectivity
- **Lombok 1.18.46** — Boilerplate code reduction
- **Spring Boot Actuator** — Health checks & monitoring
- **Java 25**

## Database Schema

### user_db

| Table | Description |
|-------|-------------|
| `user_profiles` | Consumer profile data (authUserId, name, address, contact, demographics) |

## Startup Order

```
1. MySQL Database
2. Eureka Server (port 8761)
3. Auth Service (port 8081)
4. User Service (port 8082)
5. API Gateway (port 8080)
```

## Notes

- The `authUserId` is an external reference to `auth_db.users.id` (managed by the Auth Service) — the User Service stores no credentials
- `ddl-auto: update` auto-creates/updates the `user_profiles` table on startup
- CORS is configured for `localhost:5173` (frontend dev server) and `localhost:8080` (API Gateway)
- Accessible through the API Gateway at `http://localhost:8080/api/users/**`
