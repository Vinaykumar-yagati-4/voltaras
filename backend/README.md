# VOLTARAS Backend — Package Structure

This document describes the purpose of each package within the Spring Boot backend.

```
src/main/java/com/voltaras/backend/
├── VoltarasApplication.java   # Application entry point
├── config/                    # Configuration classes
├── constant/                  # Constants and enumerations
├── controller/                # REST API controllers
├── dto/
│   ├── request/               # Incoming request DTOs
│   └── response/              # Outgoing response DTOs
├── entity/                    # JPA entity classes
├── exception/                 # Custom exceptions and error handling
├── mapper/                    # Object mapping / conversion utilities
├── repository/                # Spring Data JPA repositories
├── security/                  # Authentication & authorization
├── service/                   # Business logic layer
└── util/                      # General-purpose utility classes
```

---

## Package Details

| Package | Purpose |
|---|---|
| `config` | Spring configuration classes (`@Configuration`) — e.g., CORS, web, security, and bean definitions. |
| `constant` | Application-wide constants, enums, and static values used across multiple packages. |
| `controller` | REST endpoint handlers (`@RestController`) that receive HTTP requests, delegate to services, and return responses. |
| `dto/request` | Data Transfer Objects for incoming payloads (e.g., `@RequestBody` bindings, query parameters). |
| `dto/response` | Data Transfer Objects for outgoing payloads sent back to API clients. |
| `entity` | JPA entity classes (`@Entity`) mapped to database tables. |
| `exception` | Custom exception classes and a global exception handler (`@RestControllerAdvice`) for consistent error responses. |
| `mapper` | Mapping/conversion utilities (e.g., MapStruct interfaces or manual `ModelMapper` helpers) to transform entities ↔ DTOs. |
| `repository` | Spring Data JPA repository interfaces (`@Repository` / `JpaRepository`) providing CRUD and query methods. |
| `security` | JWT authentication filters, security configuration, and user details services. |
| `service` | Business service classes (`@Service`) containing core application logic. |
| `util` | Generic utility/helper classes reused across the application. |

---

## Conventions

- **Package-by-layer** approach — classes are grouped by architectural role rather than by feature.
- Each package should contain an `index` or `package-info.java` if additional documentation is needed (optional).
- Zero-argument constructors, Lombok, and dependency injection (constructor injection) are preferred throughout.
