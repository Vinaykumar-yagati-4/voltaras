# VOLTARAS — Project Context

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 2 — Project Context
> **Document:** `docs/01_PROJECT_CONTEXT.md`

---

## 1. Project Overview

**VOLTARAS** is a full-stack web application that enables electricity consumers to track daily energy consumption, view computed bills, make payments, raise complaints, and analyze their usage patterns — while providing administrators with tools to manage users, configure tariff slabs, monitor readings, generate bills, track payments, resolve complaints, send notifications, and access system reports.

The platform follows a **Layered Architecture** with a **Spring Boot** backend serving **REST APIs** and a modern frontend (React). Authentication is handled via **JWT tokens**, data is persisted in **MySQL**, and the entire system is designed following **SOLID principles** and industry-standard design patterns.

---

## 2. Technology Stack

### Backend

| Technology | Version / Specification | Purpose |
|---|---|---|
| Java | 17 (LTS) | Primary programming language |
| Spring Boot | 3.x | Application framework |
| Spring Web | — | REST API development |
| Spring Data JPA | — | Database access & ORM |
| Spring Security | — | Authentication & authorization |
| Hibernate | — | JPA implementation / ORM |
| MySQL Connector | — | Database connectivity |
| JWT (jjwt) | 0.12.x | Token-based authentication |
| Lombok | Latest | Boilerplate code reduction |
| MapStruct | Latest | DTO mapping |
| Bean Validation (javax.validation) | — | Request validation |
| Springdoc OpenAPI (Swagger) | 2.x | API documentation |
| Logback / SLF4J | — | Logging |
| JUnit 5 + Mockito | — | Unit & integration testing |
| Maven Surefire/Failsafe | — | Test execution |

### Frontend

| Technology | Version / Specification | Purpose |
|---|---|---|
| React | 18.x | UI library |
| TypeScript | 5.x | Type-safe JavaScript |
| React Router | 6.x | Client-side routing |
| Axios | Latest | HTTP client for API calls |
| Redux Toolkit / Context API | Latest | State management |
| Chart.js / Recharts | Latest | Analytics charts & graphs |
| Tailwind CSS / Material UI | Latest | Styling & UI components |
| React Hook Form | Latest | Form handling & validation |
| React Hot Toast / Sonner | Latest | Toast notifications |
| Vite | 5.x | Build tool & dev server |

### Database

| Technology | Specification | Purpose |
|---|---|---|
| MySQL | 8.x | Relational database |
| Flyway | Latest | Database migration management |

### Authentication

| Technology | Purpose |
|---|---|
| JWT (JSON Web Tokens) | Stateless authentication |
| Spring Security Filter Chain | Request-level security enforcement |
| BCrypt | Password hashing |

### API Documentation

| Technology | Purpose |
|---|---|
| Springdoc OpenAPI (Swagger UI) | Auto-generated interactive API docs |
| Swagger Annotations | Manual API endpoint documentation |

### Build Tool

| Technology | Purpose |
|---|---|
| Maven | Backend build, dependency management, packaging |
| Vite | Frontend build and dev server |

### Version Control

| Technology | Purpose |
|---|---|
| Git | Source code management |
| GitHub / GitLab / Bitbucket | Remote repository hosting |

### Containerization

| Technology | Purpose |
|---|---|
| Docker | Application containerization |
| Docker Compose | Multi-container orchestration (app + db) |

### Deployment

| Environment | Strategy |
|---|---|
| Development | Localhost with Docker Compose |
| Testing | Cloud VM / Staging server |
| Production | Cloud VM / VPS with CI/CD pipeline |

---

## 3. Coding Standards

The project follows **TrainingMug ADF Coding Standards** built on established software engineering principles.

### SOLID Principles

| Principle | Implementation |
|---|---|
| **S** — Single Responsibility | Each class has exactly one reason to change; one focused responsibility. |
| **O** — Open/Closed | Classes are open for extension, closed for modification; use interfaces and polymorphism. |
| **L** — Liskov Substitution | Subtypes are substitutable for their base types without altering correctness. |
| **I** — Interface Segregation | Many small, specific interfaces over one large, general-purpose interface. |
| **D** — Dependency Inversion | High-level modules do not depend on low-level modules; both depend on abstractions. |

### Layered Architecture

```
Controller Layer   →  Handles HTTP requests/responses
       ↓
Service Layer      →  Business logic & orchestration
       ↓
Repository Layer   →  Database access (Spring Data JPA)
       ↓
Entity Layer       →  JPA entity mapping to database tables
```

### DTO Pattern

- Data Transfer Objects are used for all API request/response payloads.
- Entities are never exposed directly to the client.
- MapStruct is used for entity-to-DTO and DTO-to-entity mapping.
- Separate DTOs for requests (inbound) and responses (outbound).

### Repository Pattern

- Each entity has a corresponding repository interface extending `JpaRepository`.
- Custom queries use `@Query` annotations or derived query methods.
- Repositories are injected into services via constructor injection.

### Constructor Injection

- Dependencies are injected through constructors (not field injection).
- Classes have a single constructor annotated with `@RequiredArgsConstructor` (Lombok) or explicit constructor.
- No `@Autowired` on fields.

### Validation

- Incoming request DTOs are validated using `jakarta.validation` annotations:
  - `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@Positive`, etc.
- Custom validators are created for complex validation logic.
- Service-layer validation is performed before persistence operations.

### Global Exception Handling

- A centralized `@RestControllerAdvice` class handles all exceptions.
- Custom exception classes extend `RuntimeException` (e.g., `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`).
- Consistent error response structure with status code, message, timestamp, and details.

### Swagger Documentation

- All REST controllers are annotated with `@Tag` and `@Operation`.
- Request/Response DTOs use `@Schema` annotations for field descriptions.
- Swagger UI is accessible at `/swagger-ui.html` in non-production environments.

### REST Naming Standards

- Use **nouns**, not verbs, for resource endpoints.
- Use **plural nouns** for collection resources.
- HTTP methods map to CRUD operations:
  - `GET` — Retrieve resource(s)
  - `POST` — Create resource
  - `PUT` — Update resource (full)
  - `PATCH` — Update resource (partial)
  - `DELETE` — Remove resource
- Use **camelCase** for query parameters.
- Use **kebab-case** for path variables when multi-word (e.g., `/api/tariff-slabs`).

### Meaningful Logging

- Use SLF4J + Logback for logging.
- Log levels:
  - `ERROR` — Runtime exceptions, failures
  - `WARN` — Unexpected situations that are not errors
  - `INFO` — Business-relevant actions (user registration, bill generation, payment)
  - `DEBUG` — Detailed diagnostic information (development only)
  - `TRACE` — Fine-grained execution flow
- Include correlation IDs for request tracing where applicable.

---

## 4. Project Folder Structure

```
VOLTARAS/
├── docs/                                  # Project documentation
│   ├── 01_PROJECT_CONTEXT.md
│   ├── 02_REQUIREMENTS.md
│   ├── 03_SYSTEM_DESIGN.md
│   └── ...
│
├── backend/                               # Spring Boot backend
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/voltaras/
│   │   │   │   ├── VoltarasApplication.java          # Main entry point
│   │   │   │   │
│   │   │   │   ├── config/                           # Configuration classes
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── CorsConfig.java
│   │   │   │   │   ├── SwaggerConfig.java
│   │   │   │   │   └── WebConfig.java
│   │   │   │   │
│   │   │   │   ├── controller/                       # REST controllers
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── ReadingController.java
│   │   │   │   │   ├── BillController.java
│   │   │   │   │   ├── PaymentController.java
│   │   │   │   │   ├── ComplaintController.java
│   │   │   │   │   ├── NotificationController.java
│   │   │   │   │   ├── TariffSlabController.java
│   │   │   │   │   ├── DashboardController.java
│   │   │   │   │   └── ReportController.java
│   │   │   │   │
│   │   │   │   ├── dto/                              # Data Transfer Objects
│   │   │   │   │   ├── request/                      # Inbound DTOs
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   ├── ReadingRequest.java
│   │   │   │   │   │   ├── BillRequest.java
│   │   │   │   │   │   ├── PaymentRequest.java
│   │   │   │   │   │   ├── ComplaintRequest.java
│   │   │   │   │   │   ├── TariffSlabRequest.java
│   │   │   │   │   │   └── NotificationRequest.java
│   │   │   │   │   └── response/                     # Outbound DTOs
│   │   │   │   │       ├── AuthResponse.java
│   │   │   │   │       ├── UserResponse.java
│   │   │   │   │       ├── ReadingResponse.java
│   │   │   │   │       ├── BillResponse.java
│   │   │   │   │       ├── PaymentResponse.java
│   │   │   │   │       ├── ComplaintResponse.java
│   │   │   │   │       ├── NotificationResponse.java
│   │   │   │   │       ├── TariffSlabResponse.java
│   │   │   │   │       ├── DashboardResponse.java
│   │   │   │   │       ├── ReportResponse.java
│   │   │   │   │       └── ErrorResponse.java
│   │   │   │   │
│   │   │   │   ├── entity/                           # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Role.java
│   │   │   │   │   ├── MeterReading.java
│   │   │   │   │   ├── Bill.java
│   │   │   │   │   ├── Payment.java
│   │   │   │   │   ├── Complaint.java
│   │   │   │   │   ├── Notification.java
│   │   │   │   │   └── TariffSlab.java
│   │   │   │   │
│   │   │   │   ├── enums/                            # Enumerations
│   │   │   │   │   ├── UserRole.java
│   │   │   │   │   ├── BillStatus.java
│   │   │   │   │   ├── PaymentStatus.java
│   │   │   │   │   ├── ComplaintStatus.java
│   │   │   │   │   └── NotificationType.java
│   │   │   │   │
│   │   │   │   ├── exception/                        # Custom exceptions & handler
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── BadRequestException.java
│   │   │   │   │   ├── UnauthorizedException.java
│   │   │   │   │   ├── DuplicateResourceException.java
│   │   │   │   │   └── BusinessException.java
│   │   │   │   │
│   │   │   │   ├── mapper/                           # MapStruct mappers
│   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   ├── ReadingMapper.java
│   │   │   │   │   ├── BillMapper.java
│   │   │   │   │   ├── PaymentMapper.java
│   │   │   │   │   ├── ComplaintMapper.java
│   │   │   │   │   ├── NotificationMapper.java
│   │   │   │   │   └── TariffSlabMapper.java
│   │   │   │   │
│   │   │   │   ├── repository/                       # Spring Data JPA repositories
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── RoleRepository.java
│   │   │   │   │   ├── MeterReadingRepository.java
│   │   │   │   │   ├── BillRepository.java
│   │   │   │   │   ├── PaymentRepository.java
│   │   │   │   │   ├── ComplaintRepository.java
│   │   │   │   │   ├── NotificationRepository.java
│   │   │   │   │   └── TariffSlabRepository.java
│   │   │   │   │
│   │   │   │   ├── security/                         # JWT & security utilities
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   │   └── CustomUserDetails.java
│   │   │   │   │
│   │   │   │   ├── service/                          # Business logic layer
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── ReadingService.java
│   │   │   │   │   ├── BillService.java
│   │   │   │   │   ├── PaymentService.java
│   │   │   │   │   ├── ComplaintService.java
│   │   │   │   │   ├── NotificationService.java
│   │   │   │   │   ├── TariffSlabService.java
│   │   │   │   │   ├── DashboardService.java
│   │   │   │   │   └── ReportService.java
│   │   │   │   │
│   │   │   │   ├── service/impl/                     # Service implementations
│   │   │   │   │   ├── AuthServiceImpl.java
│   │   │   │   │   ├── UserServiceImpl.java
│   │   │   │   │   ├── ReadingServiceImpl.java
│   │   │   │   │   ├── BillServiceImpl.java
│   │   │   │   │   ├── PaymentServiceImpl.java
│   │   │   │   │   ├── ComplaintServiceImpl.java
│   │   │   │   │   ├── NotificationServiceImpl.java
│   │   │   │   │   ├── TariffSlabServiceImpl.java
│   │   │   │   │   ├── DashboardServiceImpl.java
│   │   │   │   │   └── ReportServiceImpl.java
│   │   │   │   │
│   │   │   │   └── util/                             # Utility classes
│   │   │   │       ├── BillCalculator.java
│   │   │   │       └── DateUtils.java
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml                   # Main configuration
│   │   │       ├── application-dev.yml               # Dev profile
│   │   │       ├── application-test.yml              # Test profile
│   │   │       ├── application-prod.yml              # Production profile
│   │   │       └── db/migration/                     # Flyway migration scripts
│   │   │           ├── V1__init_schema.sql
│   │   │           └── ...
│   │   │
│   │   └── test/java/com/voltaras/                   # Unit & integration tests
│   │       ├── controller/
│   │       ├── service/
│   │       └── repository/
│   │
│   └── .mvn/                                         # Maven wrapper
│
├── frontend/                                # React frontend
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── tailwind.config.js
│   ├── index.html
│   ├── public/
│   │   └── assets/
│   │       ├── images/
│   │       └── icons/
│   └── src/
│       ├── main.tsx                        # Entry point
│       ├── App.tsx                         # Root component with routing
│       ├── api/                            # API service layer (Axios)
│       │   ├── axiosInstance.ts
│       │   ├── authApi.ts
│       │   ├── userApi.ts
│       │   ├── readingApi.ts
│       │   ├── billApi.ts
│       │   ├── paymentApi.ts
│       │   ├── complaintApi.ts
│       │   ├── notificationApi.ts
│       │   └── dashboardApi.ts
│       ├── components/                     # Reusable UI components
│       │   ├── common/
│       │   │   ├── Header.tsx
│       │   │   ├── Sidebar.tsx
│       │   │   ├── Footer.tsx
│       │   │   ├── Loader.tsx
│       │   │   ├── Pagination.tsx
│       │   │   └── Modal.tsx
│       │   ├── charts/
│       │   │   ├── ConsumptionChart.tsx
│       │   │   ├── CostBreakdownChart.tsx
│       │   │   └── ComparisonChart.tsx
│       │   └── layout/
│       │       ├── AdminLayout.tsx
│       │       └── ConsumerLayout.tsx
│       ├── pages/                          # Page components
│       │   ├── auth/
│       │   │   ├── LoginPage.tsx
│       │   │   ├── RegisterPage.tsx
│       │   │   └── ForgotPasswordPage.tsx
│       │   ├── consumer/
│       │   │   ├── DashboardPage.tsx
│       │   │   ├── SubmitReadingPage.tsx
│       │   │   ├── MyBillsPage.tsx
│       │   │   ├── BillDetailPage.tsx
│       │   │   ├── MakePaymentPage.tsx
│       │   │   ├── PaymentHistoryPage.tsx
│       │   │   ├── ComplaintsPage.tsx
│       │   │   ├── RaiseComplaintPage.tsx
│       │   │   ├── NotificationsPage.tsx
│       │   │   ├── ProfilePage.tsx
│       │   │   └── EnergyAnalyticsPage.tsx
│       │   ├── admin/
│       │   │   ├── AdminDashboardPage.tsx
│       │   │   ├── ManageUsersPage.tsx
│       │   │   ├── ManageTariffsPage.tsx
│       │   │   ├── MonitorReadingsPage.tsx
│       │   │   ├── GenerateBillPage.tsx
│       │   │   ├── MonitorPaymentsPage.tsx
│       │   │   ├── ManageComplaintsPage.tsx
│       │   │   ├── SendNotificationsPage.tsx
│       │   │   └── ReportsPage.tsx
│       │   └── common/
│       │       ├── NotFoundPage.tsx
│       │       └── UnauthorizedPage.tsx
│       ├── hooks/                          # Custom React hooks
│       │   ├── useAuth.ts
│       │   ├── useBill.ts
│       │   └── useNotification.ts
│       ├── store/                          # State management
│       │   ├── authSlice.ts
│       │   └── index.ts
│       ├── routes/                         # Route configuration
│       │   ├── AppRoutes.tsx
│       │   ├── ProtectedRoute.tsx
│       │   └── AdminRoute.tsx
│       ├── types/                          # TypeScript type definitions
│       │   ├── user.ts
│       │   ├── bill.ts
│       │   ├── reading.ts
│       │   ├── payment.ts
│       │   ├── complaint.ts
│       │   ├── notification.ts
│       │   ├── tariff.ts
│       │   └── dashboard.ts
│       └── utils/                          # Utility functions
│           ├── formatters.ts
│           └── validators.ts
│
├── docker/                                 # Docker configuration
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── docker-compose.yml
│
├── postman/                                # Postman API collection
│   └── VOLTARAS_API.postman_collection.json
│
├── .github/                                # GitHub workflows
│   └── workflows/
│       ├── ci.yml
│       └── cd.yml
│
├── .gitignore
├── README.md
└── VOLTARAS_Phase1_ProjectUnderstanding.md
```

---

## 5. Architecture Style

### Why Spring Boot?

| Reason | Explanation |
|---|---|
| **Rapid Development** | Auto-configuration, embedded server, starter dependencies reduce boilerplate. |
| **Production-Ready** | Built-in health checks, metrics, logging, and externalized configuration. |
| **Mature Ecosystem** | Vast community, extensive documentation, and seamless integration with JPA, Security, and other modules. |
| **Java 17 LTS** | Long-term support, modern language features (records, sealed classes, pattern matching). |
| **Microservice-Ready** | While this project starts monolithic, Spring Boot's modular design allows easy extraction into microservices later. |

### Why Layered Architecture?

| Reason | Explanation |
|---|---|
| **Separation of Concerns** | Each layer has a distinct responsibility (controller → service → repository → entity). |
| **Testability** | Layers can be mocked and tested independently. |
| **Maintainability** | Changes in one layer (e.g., replacing JPA with JDBC) don't affect other layers. |
| **Team Scalability** | Multiple developers can work on different layers simultaneously with minimal conflicts. |
| **Industry Standard** | Well-understood pattern; new developers can be onboarded quickly. |

### Why REST APIs?

| Reason | Explanation |
|---|---|
| **Stateless Communication** | Each request contains all necessary information; no server-side session state. |
| **Frontend Agnostic** | Any frontend (React, Angular, mobile) can consume the same APIs. |
| **Scalability** | Statelessness enables horizontal scaling of backend instances. |
| **Standardized** | REST is the industry standard for web APIs; tools and documentation are mature. |
| **Cacheable** | HTTP caching mechanisms can be leveraged for performance. |

### Why JWT Authentication?

| Reason | Explanation |
|---|---|
| **Stateless** | No server-side session storage; JWT contains all user information. |
| **Scalable** | Perfect for horizontally scaled deployments. |
| **Self-Contained** | Token carries user identity, roles, and expiration — no database lookup on every request. |
| **Cross-Platform** | Works seamlessly with web, mobile, and third-party clients. |
| **Secure** | Signed tokens prevent tampering; expiration limits token lifespan. |

### Why MySQL?

| Reason | Explanation |
|---|---|
| **Relational Data** | Electricity billing has clear relational structure: users → readings → bills → payments. |
| **ACID Compliance** | Ensures data integrity for financial transactions (bills, payments). |
| **Mature & Stable** | Battle-tested in production across countless enterprises. |
| **Excellent Tooling** | Rich ecosystem of migration tools (Flyway), ORM support (Hibernate), and management tools. |
| **Cost-Effective** | Open-source with no licensing costs. |
| **Strong Community** | Extensive documentation, tutorials, and community support. |

---

## 6. Git Branching Strategy

### Branch Structure

```
main
  └── develop
       ├── feature/backend-auth
       ├── feature/frontend-login
       ├── feature/backend-reading
       ├── feature/frontend-reading
       └── ...
```

### Branch Rules

| Branch | Purpose | Base Branch | Merge Into |
|---|---|---|---|
| `main` | Production-ready code | — | — |
| `develop` | Integration branch for features | `main` | `main` |
| `feature/*` | Individual feature development | `develop` | `develop` |

### Workflow

1. Create a `feature/*` branch from `develop`.
2. Work on the feature, committing regularly.
3. Open a Pull Request to merge `feature/*` into `develop`.
4. Code review and CI checks must pass before merge.
5. After release testing, merge `develop` into `main`.

### Commit Message Conventions

Follow the **Conventional Commits** format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**

| Type | Usage |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation changes |
| `style` | Code formatting (no logic change) |
| `refactor` | Code restructuring (no feature/bugfix) |
| `test` | Adding or modifying tests |
| `chore` | Build, CI, tooling changes |
| `perf` | Performance improvements |

**Examples:**

```
feat(auth): add JWT token generation and validation
fix(bill): correct slab calculation for multi-slab billing
docs(api): update Swagger documentation for payment endpoints
refactor(reading): extract meter reading validation logic
test(service): add unit tests for BillService
chore(deps): upgrade Spring Boot to 3.2.0
```

---

## 7. Deployment Strategy

### Development Environment

| Aspect | Detail |
|---|---|
| **Purpose** | Local development & testing |
| **Location** | Developer's local machine |
| **Backend** | `mvn spring-boot:run` (port 8080) |
| **Frontend** | `npm run dev` (Vite, port 5173) |
| **Database** | MySQL via Docker Compose or local install |
| **Profiles** | `spring.profiles.active=dev` |
| **CORS** | Allow localhost origins |

### Testing / Staging Environment

| Aspect | Detail |
|---|---|
| **Purpose** | Integration testing, QA, UAT |
| **Location** | Cloud VM (AWS EC2 / DigitalOcean / similar) |
| **Backend** | Docker container (port 8080) |
| **Frontend** | Docker container (port 80/443) |
| **Database** | Docker container (MySQL 8.x) |
| **Orchestration** | Docker Compose |
| **Profiles** | `spring.profiles.active=test` |
| **CI/CD** | GitHub Actions build + deploy on `develop` push |

### Production Environment

| Aspect | Detail |
|---|---|
| **Purpose** | Live user-facing system |
| **Location** | Cloud VM / VPS |
| **Backend** | Docker container (port 8080, behind reverse proxy) |
| **Frontend** | Docker container (served via Nginx on port 443) |
| **Database** | Managed MySQL (AWS RDS / similar) |
| **Reverse Proxy** | Nginx or Apache (SSL termination, load balancing) |
| **SSL** | Let's Encrypt via Certbot |
| **Profiles** | `spring.profiles.active=prod` |
| **CI/CD** | GitHub Actions build → push to registry → deploy on `main` merge |
| **Backup** | Automated database backups (daily) |

---

## 8. Naming Conventions

### Packages

| Rule | Example |
|---|---|
| All lowercase | `com.voltaras.service` |
| Reverse domain prefix | `com.voltaras` |
| No underscores or hyphens | `com.voltaras.controller` (not `com.voltaras.controller_api`) |

### Classes

| Rule | Example |
|---|---|
| PascalCase | `UserService`, `BillController` |
| Noun or noun phrase | `MeterReading`, `PaymentRequest` |
| Descriptive names | Avoid single-letter or overly abbreviated names |

### Interfaces

| Rule | Example |
|---|---|
| PascalCase | `UserService`, `ReadingRepository` |
| No "I" prefix | Use `UserService` (not `IUserService`) |
| Service interfaces define contracts | `UserService`, `UserServiceImpl` for implementation |

### Methods

| Rule | Example |
|---|---|
| camelCase | `getUserById()`, `createReading()` |
| Verb or verb phrase | `save()`, `findAll()` |
| CRUD naming convention | `findById()`, `save()`, `deleteById()` |
| Boolean methods use `is`/`has`/`can` prefix | `isActive()`, `hasPendingBills()` |

### Variables

| Rule | Example |
|---|---|
| camelCase | `userName`, `totalAmount`, `meterReading` |
| Meaningful names | `totalBillAmount` (not `tba`) |
| Avoid single-letter names (except loop counters) | Use `index` over `i` in non-loop contexts |

### Constants

| Rule | Example |
|---|---|
| UPPER_SNAKE_CASE | `MAX_READING_VALUE`, `DEFAULT_PAGE_SIZE` |
| `static final` modifiers | `private static final int MAX_LOGIN_ATTEMPTS = 5;` |
| Placed in enclosing class or dedicated constants class | `BillConstants.java` |

### REST APIs

| Rule | Example |
|---|---|
| Plural nouns for resources | `/api/users`, `/api/bills` |
| Lowercase, kebab-case for multi-word resources | `/api/tariff-slabs`, `/api/meter-readings` |
| Use nouns, not verbs | `/api/users` (not `/api/getUsers`) |
| Nest related resources | `/api/users/{userId}/readings` |
| Query parameters for filtering | `/api/bills?status=PENDING&month=2026-01` |
| Version prefix (if versioning needed) | `/api/v1/users` |

**Standard REST Endpoint Examples:**

| HTTP | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | User login |
| GET | `/api/users/{id}` | Get user profile |
| PUT | `/api/users/{id}` | Update user profile |
| POST | `/api/readings` | Submit meter reading |
| GET | `/api/readings?userId={id}` | Get user readings |
| GET | `/api/bills?userId={id}` | Get user bills |
| POST | `/api/payments` | Make payment |
| GET | `/api/complaints?userId={id}` | Get user complaints |
| POST | `/api/complaints` | Raise a complaint |
| GET | `/api/admin/users` | Admin: list all users |
| POST | `/api/admin/tariff-slabs` | Admin: create tariff slab |
| POST | `/api/admin/notifications` | Admin: send notification |

---

> **End of Phase 2 — Deliverable**
> *`docs/01_PROJECT_CONTEXT.md` has been generated.*
> *Pending approval to proceed to Phase 3.*
