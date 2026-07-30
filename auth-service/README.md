# VOLTARAS Auth Service

## Purpose

The **Auth Service** handles all authentication and authorization operations for the VOLTARAS platform. It is responsible for user registration, login, JWT token generation and validation, and password management.

## Port

- **Default port:** `8081`
- Configurable via `server.port` in `application.yml` or runtime argument `--server.port=<port>`.

## Features

- **User Registration** — Create a new consumer account with email, password, and profile details
- **User Login** — Authenticate with email/password and receive a JWT token
- **JWT Token Generation** — Stateless authentication tokens with 24-hour expiry
- **Password Management** — Change password with current password verification
- **Role-Based Access** — CONSUMER and ADMIN roles for authorization
- **Eureka Client** — Registers with Eureka Server for service discovery
- **Actuator Endpoints** — Health checks and monitoring
- **Global Exception Handling** — Standardized error responses

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | No | Register a new consumer account |
| `POST` | `/api/auth/login` | No | Authenticate and receive JWT token |
| `POST` | `/api/auth/change-password` | Yes | Change current user's password |
| `GET`  | `/api/auth/me` | Yes | Get current authenticated user's details |
| `POST` | `/api/auth/logout` | Yes | Logout (client-side token removal) |

## Run Command

### Prerequisites

1. **MySQL** — Running on localhost:3306 with database `auth_db` created
2. **Eureka Server** — Running on port 8761

### Using Maven (development)

```bash
cd auth-service
mvn spring-boot:run
```

### Using the packaged JAR

```bash
mvn clean package -DskipTests
java -jar target/auth-service.jar
```

## Verification Steps

1. **Ensure Eureka Server is running** on port 8761
2. **Create the database** in MySQL: `CREATE DATABASE IF NOT EXISTS auth_db;`
3. **Start the service**: `cd auth-service && mvn spring-boot:run`
4. **Check Eureka Dashboard** at `http://localhost:8761` — `AUTH-SERVICE` should appear
5. **Test Registration**:
   ```bash
   curl -X POST http://localhost:8081/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "fullName": "John Doe",
       "email": "john@example.com",
       "phone": "9876543210",
       "address": "123 Main Street, Mumbai",
       "password": "Secure123!",
       "confirmPassword": "Secure123!"
     }'
   ```
6. **Test Login**:
   ```bash
   curl -X POST http://localhost:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "john@example.com", "password": "Secure123!"}'
   ```
7. **Test Get Current User** (use the token from login):
   ```bash
   curl -X GET http://localhost:8081/api/auth/me \
     -H "Authorization: Bearer <your-token>"
   ```
8. **Check health**: `curl http://localhost:8081/actuator/health`

## Dependencies

- **Spring Boot 3.5.5** — Application framework
- **Spring Cloud 2025.0.3** — Cloud dependencies
  - Netflix Eureka Client — Service discovery
- **Spring Data JPA** — Database access
- **Spring Security** — Authentication & authorization
- **Spring Validation** — Jakarta Bean Validation
- **MySQL Connector** — Database connectivity
- **JJWT 0.12.6** — JWT token generation & validation
- **Lombok 1.18.46** — Boilerplate code reduction
- **Spring Boot Actuator** — Health checks & monitoring
- **Java 25**

## Database Schema

### auth_db

| Table | Description |
|-------|-------------|
| `users` | User credentials (email, password_hash, is_active) |
| `roles` | Role definitions (CONSUMER, ADMIN) |
| `user_roles` | Junction table linking users to roles |

## Startup Order

```
1. MySQL Database
2. Eureka Server (port 8761)
3. Auth Service (port 8081)
4. Other services...
```

## Notes

- Passwords are hashed using BCrypt before storage
- JWT tokens expire after 24 hours (configurable)
- The service seeds CONSUMER and ADMIN roles automatically on startup
- All protected endpoints require `Authorization: Bearer <token>` header
- CORS is configured for `localhost:5173` (frontend dev server) and `localhost:8080` (API Gateway)
