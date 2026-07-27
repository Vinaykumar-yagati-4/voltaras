# VOLTARAS — Architecture Design

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 4 — Architecture Design
> **Document:** `docs/03_ARCHITECTURE.md`

---

## 1. Architecture Overview

VOLTARAS adopts a **Microservices Architecture** to enable independent development, deployment, and scaling of distinct business capabilities. Each microservice owns its domain logic, data store, and API surface, communicating with others via REST over HTTP.

An **API Gateway** acts as the single entry point for all client requests, routing them to the appropriate backend service. A **Service Registry (Eureka)** provides dynamic service discovery so that services can locate each other without hardcoded addresses.

**Key architectural decisions:**

| Decision | Rationale |
|---|---|
| **Microservices over Monolith** | Each domain (auth, user, meter, billing, payment, notification) has independent scaling needs, release cycles, and team ownership boundaries. Microservices prevent a single point of failure and allow targeted optimization. |
| **REST over gRPC/Message Queue** | REST is simpler to implement, debug, and document. For the current scale (hundreds to low thousands of consumers), synchronous REST suffices. A message queue can be introduced later for async workflows (e.g., bill generation triggers notification). |
| **Database-per-Service** | Each service owns its MySQL database, ensuring loose coupling. No service directly accesses another service's database — all cross-service data flows through API calls. |
| **JWT for inter-service auth** | The JWT issued at login carries user identity and roles. Services validate the token locally (no central session store) to authorize intra-service requests. |

---

## 2. High-Level Architecture

```
                         ┌─────────────────────────┐
                         │     React Frontend       │
                         │   (React + TypeScript)   │
                         └───────────┬─────────────┘
                                     │ HTTPS
                                     ▼
                         ┌─────────────────────────┐
                         │     API Gateway          │
                         │   (Spring Cloud Gateway) │
                         └───┬───┬───┬───┬───┬───┬─┘
                             │   │   │   │   │   │
              ┌──────────────┘   │   │   │   │   └──────────────┐
              ▼                  ▼   ▼   ▼   ▼                  ▼
     ┌──────────────┐    ┌─────────────────────────────────┐   ┌──────────────────┐
     │ Eureka Server │    │         Auth Service           │   │  Notification    │
     │ (Registry)    │    │   (Login, Register, JWT)       │   │  Service         │
     └──────────────┘    └────────────┬────────────────────┘   │  (Notifications) │
                                      │                        └──────────────────┘
                                      ▼
     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                         User Service                                        │
     │              (Profile, Admin User Management)                               │
     │                   ┌──────────────────────┐                                  │
     │                   │    MySQL (User DB)    │                                  │
     │                   └──────────────────────┘                                  │
     └─────────────────────────────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                         Meter Service                                       │
     │              (Meter Reading Submission, History, Validation)                │
     │                   ┌──────────────────────┐                                  │
     │                   │   MySQL (Meter DB)    │                                  │
     │                   └──────────────────────┘                                  │
     └─────────────────────────────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                         Billing Service                                     │
     │              (Bill Calculation, Tariff Slabs, Daily/Monthly Bills)          │
     │                   ┌──────────────────────┐                                  │
     │                   │  MySQL (Billing DB)   │                                  │
     │                   └──────────────────────┘                                  │
     └─────────────────────────────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                         Payment Service                                     │
     │              (Payment Recording, History, Status Updates)                   │
     │                   ┌──────────────────────┐                                  │
     │                   │  MySQL (Payment DB)   │                                  │
     │                   └──────────────────────┘                                  │
     └─────────────────────────────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                     Notification Service                                    │
     │              (In-App Notifications, Broadcast, Targeted)                    │
     │                   ┌──────────────────────────┐                              │
     │                   │  MySQL (Notification DB)  │                              │
     │                   └──────────────────────────┘                              │
     └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Microservice Architecture Diagram (Text-Based)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                    │
│   ┌──────────────┐                                                                │
│   │   Frontend   │                                                                │
│   │   (React)    │                                                                │
│   └──────┬───────┘                                                                │
│          │  HTTPS                                                                │
│          ▼                                                                        │
│   ┌──────────────────────────────────────────────────────────────────────────┐   │
│   │                     API Gateway (Spring Cloud Gateway)                  │   │
│   │  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────┐ │   │
│   │  │ /api/   │ │ /api/   │ │ /api/    │ │ /api/    │ │ /api/    │ │/api/│ │   │
│   │  │ auth/** │ │ users/**│ │readings/**││ bills/** │ │payments/**││notif│ │   │
│   │  └────┬────┘ └────┬────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──┬──┘ │   │
│   └───────┼───────────┼───────────┼────────────┼────────────┼───────────┼─────┘   │
│           │           │           │            │            │           │         │
│           ▼           ▼           ▼            ▼            ▼           ▼         │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│   │  Auth    │ │  User    │ │  Meter   │ │  Billing  │ │ Payment  │ │Notificat │ │
│   │ Service  │ │ Service  │ │ Service  │ │ Service   │ │ Service  │ │ Service  │ │
│   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
│        │            │            │            │            │            │       │
│   ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐      │
│   │ MySQL   │ │ MySQL   │ │ MySQL   │ │ MySQL   │ │ MySQL   │ │ MySQL   │      │
│   │ Auth DB │ │ User DB  │ │ Meter DB│ │ Billing │ │Payment  │ │Notific  │      │
│   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘      │
│                                                                                    │
│   ┌──────────────────────────────────────────────────────────────────────────┐   │
│   │                     Eureka Service Registry                               │   │
│   │                   (Service Discovery & Health Checks)                     │   │
│   └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### Service-to-Service Communication Matrix

| From | To | Purpose | Method |
|---|---|---|---|
| API Gateway | Auth Service | Login, register, token validation | REST |
| API Gateway | User Service | Profile CRUD, admin user management | REST |
| API Gateway | Meter Service | Submit reading, view history | REST |
| API Gateway | Billing Service | Generate bills, view bills | REST |
| API Gateway | Payment Service | Record payment, view history | REST |
| API Gateway | Notification Service | View/send notifications | REST |
| Billing Service | Meter Service | Fetch daily readings for bill calculation | REST (internal) |
| Billing Service | User Service | Fetch consumer details for billing | REST (internal) |
| Payment Service | Billing Service | Update bill status after payment | REST (internal) |
| Auth Service | User Service | Register user profile after signup | REST (internal) |
| Notification Service | User Service | Fetch consumer details for targeted notification | REST (internal) |

---

## 4. Responsibilities of Every Microservice

### 4.1 API Gateway

| Responsibility | Details |
|---|---|
| **Single Entry Point** | All client requests arrive at the gateway and are routed to the appropriate microservice. |
| **Authentication Enforcement** | Validates JWT tokens on all protected routes before forwarding requests. |
| **Route Configuration** | Routes requests based on path prefix (e.g., `/api/auth/**` → Auth Service). |
| **Load Balancing** | Distributes requests across multiple instances of a service (when horizontally scaled). |
| **CORS Management** | Centralized CORS policy for frontend origin(s). |
| **Request/Response Transformation** | Strips sensitive headers, adds correlation IDs for tracing. |
| **Rate Limiting (Future)** | Protects backend services from abuse. |

**Technology:** Spring Cloud Gateway

---

### 4.2 Eureka Service Registry

| Responsibility | Details |
|---|---|
| **Service Registration** | Each microservice registers itself with Eureka on startup. |
| **Health Monitoring** | Periodically checks heartbeat of registered services; deregisters unhealthy instances. |
| **Service Discovery** | API Gateway and services query Eureka to find the network location of other services. |
| **Instance Management** | Maintains a real-time map of all running service instances and their metadata. |

**Technology:** Netflix Eureka (Spring Cloud)

---

### 4.3 Auth Service

| Responsibility | Details |
|---|---|
| **User Registration** | Accepts registration requests, validates input, hashes password, creates user via User Service, stores credentials in Auth DB. |
| **User Login** | Validates email and password against stored credentials, generates and returns a JWT token upon successful authentication. |
| **Token Validation** | Provides an internal endpoint for other services/gateway to validate JWT tokens and retrieve user details. |
| **Password Management** | Supports password change with current password verification. |
| **Role Assignment** | Assigns default CONSUMER role on registration; ADMIN role is pre-seeded or assigned by another admin. |

**Owns:** Auth DB (users table for credentials, roles table)

---

### 4.4 User Service

| Responsibility | Details |
|---|---|
| **Profile Management** | CRUD operations on user profile (name, phone, address). |
| **Consumer Number** | Auto-generates a unique consumer account number during registration. |
| **Admin User Management** | Provides endpoints for admin to list all users, search, activate/deactivate accounts. |
| **User Lookup** | Provides internal REST endpoints for other services to fetch user details by ID or email. |

**Owns:** User DB (profile details, status, account number)

---

### 4.5 Meter Service

| Responsibility | Details |
|---|---|
| **Reading Submission** | Accepts daily meter reading with value and date; validates value is greater than last reading and no duplicate date. |
| **Units Calculation** | Computes units consumed by subtracting current reading from previous reading. |
| **Reading History** | Returns paginated, sorted reading history for a consumer. |
| **Anomaly Detection** | Flags readings that exceed configured thresholds as "suspicious." |
| **Admin Reading View** | Provides admin endpoints to view all readings, filter by consumer/date, and flag readings. |
| **Internal API** | Exposes endpoints for Billing Service to fetch readings for a date range. |

**Owns:** Meter DB (meter readings, consumer meter mapping)

---

### 4.6 Billing Service

| Responsibility | Details |
|---|---|
| **Tariff Slab Management** | CRUD for tariff slabs (unit range, rate per unit). Admin-only endpoints. |
| **Daily Bill Computation** | Automatically computes a daily bill when a reading is submitted (via internal trigger or scheduled check). Applies slab rates. |
| **Monthly Bill Generation** | Aggregates daily consumption for a month, applies slab rates, adds fixed charges, generates a monthly bill. Triggered by admin. |
| **Bill History** | Returns bills (daily/monthly) for a consumer with full breakdown. |
| **Internal API** | Provides endpoint for Payment Service to update bill payment status. |

**Owns:** Billing DB (tariff slabs, daily bills, monthly bills)

---

### 4.7 Payment Service

| Responsibility | Details |
|---|---|
| **Payment Recording** | Records payment against an unpaid bill with amount, method, transaction reference, and timestamp. |
| **Bill Status Update** | After recording payment, calls Billing Service to mark the bill as PAID. |
| **Payment History** | Returns paginated payment history for a consumer. |
| **Duplicate Prevention** | Prevents multiple payments against the same bill. |
| **Admin Monitoring** | Provides endpoints for admin to view all payments and filter by date/consumer/status. |

**Owns:** Payment DB (payment transactions)

---

### 4.8 Notification Service

| Responsibility | Details |
|---|---|
| **Notification Creation** | Creates in-app notifications triggered by events (bill generated, payment confirmed, complaint status changed). |
| **Admin Notifications** | Allows admin to send broadcast (all consumers) or targeted (specific consumer) notifications. |
| **Notification Retrieval** | Returns paginated notifications for a consumer, with read/unread status. |
| **Mark as Read** | Marks individual or all notifications as read. |
| **Internal API** | Provides endpoint for other services (Billing, Payment, Complaint) to trigger notifications programmatically. |

**Owns:** Notification DB (notifications, user notification mappings)

---

## 5. Service Communication

### 5.1 Gateway Routing

The API Gateway (Spring Cloud Gateway) routes incoming requests to the correct microservice based on the request path prefix:

| Route Prefix | Target Service |
|---|---|
| `/api/auth/**` | Auth Service |
| `/api/users/**` | User Service |
| `/api/readings/**` | Meter Service |
| `/api/bills/**` | Billing Service |
| `/api/payments/**` | Payment Service |
| `/api/notifications/**` | Notification Service |
| `/api/complaints/**` | (Handled within User Service or dedicated service in V2) |

**Routing flow:**

```
Client → /api/bills/123 → Gateway → discovers Billing-Service via Eureka → http://billing-service:8083/api/bills/123
```

### 5.2 Eureka Discovery

- Each microservice registers with Eureka at startup with its service name (e.g., `AUTH-SERVICE`, `BILLING-SERVICE`).
- The API Gateway and all services use Spring Cloud LoadBalancer with Eureka to resolve service names to actual instances.
- Eureka performs a health check every 30 seconds (configurable). Instances that fail 3 consecutive checks are removed from the registry.
- When a new instance starts, it registers automatically and begins receiving traffic from the gateway.

**Service names:**

| Service | Eureka Service ID | Default Port |
|---|---|---|
| Eureka Server | `eureka-server` | 8761 |
| API Gateway | `api-gateway` | 8080 |
| Auth Service | `auth-service` | 8081 |
| User Service | `user-service` | 8082 |
| Meter Service | `meter-service` | 8083 |
| Billing Service | `billing-service` | 8084 |
| Payment Service | `payment-service` | 8085 |
| Notification Service | `notification-service` | 8086 |

### 5.3 REST Communication

- All inter-service communication uses synchronous REST over HTTP.
- Services use `WebClient` (Spring WebFlux) or `RestTemplate` with `@LoadBalanced` to call other services via their Eureka service names.
- JSON is the serialization format for all request/response bodies.
- Internal service calls include the original JWT token in the `Authorization` header for propagation (token relay).
- A correlation ID (`X-Correlation-Id`) is passed through all service calls for request tracing.

**Example internal call (Billing Service → Meter Service):**

```
GET http://meter-service/api/internal/readings?consumerId=101&from=2026-07-01&to=2026-07-31
Authorization: Bearer <service-account-jwt or relayed-user-jwt>
X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000
```

### 5.4 JWT Flow

```
┌──────────┐          ┌─────────────┐          ┌─────────────┐
│  Client   │          │ API Gateway  │          │ Auth Service │
└─────┬─────┘          └──────┬──────┘          └──────┬──────┘
      │                       │                        │
      │  1. POST /api/auth/login                       │
      │  (email + password)   │                        │
      ├──────────────────────►│                        │
      │                       │  2. Route to Auth      │
      │                       ├──────────────────────►│
      │                       │                        │
      │                       │  3. Validate credentials│
      │                       │  Generate JWT          │
      │                       │◄──────────────────────┤
      │                       │                        │
      │  4. JWT Response      │                        │
      │◄──────────────────────┤                        │
      │                       │                        │
      │  5. GET /api/bills                          │
      │  (Authorization: Bearer <JWT>)                 │
      ├──────────────────────►│                        │
      │                       │  6. Gateway validates  │
      │                       │     JWT (offline - no  │
      │                       │     call to Auth Svc)  │
      │                       │                        │
      │                       │  7. Route to Billing   │
      │                       │     (forward JWT)      │
      │                       ├──────────────────────► │
      │                       │                        │
      │                       │  8. Billing Service    │
      │                       │     validates JWT      │
      │                       │     (extract userId,   │
      │                       │      role from claims) │
      │                       │◄────────────────────── │
      │  9. Bills Response    │                        │
      │◄──────────────────────┤                        │
      │                       │                        │
```

**JWT Token Structure (Claims):**

```json
{
  "sub": "user@example.com",
  "userId": 101,
  "role": "CONSUMER",
  "iat": 1722000000,
  "exp": 1722086400
}
```

**Key points:**
- Gateway validates the JWT signature and expiration on every request (offline, using the public key/secret).
- Gateway extracts the `role` claim to enforce route-level authorization.
- The JWT is forwarded to downstream services in the `Authorization` header.
- Each service can independently parse and validate the JWT to get user identity — no need to call Auth Service on every request.

---

## 6. Layered Architecture Inside Every Service

Each microservice follows a consistent layered architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CONTROLLER LAYER                                    │
│  Handles HTTP request/response, input validation (basic), delegates to      │
│  service layer. Returns DTOs as JSON responses.                             │
│                                                                             │
│  ┌──────────────────────────────────────────────┐                          │
│  │  ReadingController.java                      │                          │
│  │  @RestController                             │                          │
│  │  @RequestMapping("/api/readings")            │                          │
│  │  - submitReading(ReadingRequest) → ReadingResponse                      │
│  │  - getReadingHistory(userId, page) → Page<ReadingResponse>              │
│  └──────────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                                      │
│  Contains business logic, validation rules, orchestration, and calls to     │
│  repository layer and/or other services via WebClient/RestTemplate.         │
│  Implements service interface (contract).                                   │
│                                                                             │
│  ┌──────────────────────────────────────────────┐                          │
│  │  ReadingService (interface)                   │                          │
│  │  ReadingServiceImpl.java                      │                          │
│  │  - submitReading(userId, request)             │                          │
│  │    → validate reading > previous              │                          │
│  │    → check no duplicate date                  │                          │
│  │    → calculate units consumed                 │                          │
│  │    → save to repository                       │                          │
│  │    → trigger daily bill computation           │                          │
│  │  - getReadingHistory(userId, pageable)        │                          │
│  └──────────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REPOSITORY LAYER                                    │
│  Data access layer using Spring Data JPA. Each entity has a repository      │
│  interface extending JpaRepository. Custom queries use @Query or derived    │
│  method names.                                                              │
│                                                                             │
│  ┌──────────────────────────────────────────────┐                          │
│  │  MeterReadingRepository.java                  │                          │
│  │  extends JpaRepository<MeterReading, Long>   │                          │
│  │  - findByUserIdOrderByReadingDateDesc()       │                          │
│  │  - findTopByUserIdOrderByReadingDateDesc()    │                          │
│  └──────────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ENTITY LAYER                                       │
│  JPA entities mapping to database tables. Not exposed outside the service.  │
│                                                                             │
│  ┌──────────────────────────────────────────────┐                          │
│  │  MeterReading.java                            │                          │
│  │  @Entity @Table(name = "meter_readings")      │                          │
│  │  - id, userId, readingDate, meterValue,       │                          │
│  │    unitsConsumed, status, createdAt           │                          │
│  └──────────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SUPPORTING LAYERS                                       │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐ │
│  │ DTO Layer    │  │ Config Layer │  │ Security Layer │  │ Exception    │ │
│  │ Request/     │  │ - CorsConfig│  │ - JwtToken    │  │ Layer        │ │
│  │ Response DTO │  │ - Swagger   │  │   Provider    │  │ Global       │ │
│  │ + MapStruct  │  │ - AppConfig │  │ - JwtFilter  │  │ Exception    │ │
│  │ Mappers      │  │             │  │ - Security   │  │ Handler      │ │
│  │              │  │             │  │   Config      │  │ + Custom     │ │
│  │              │  │             │  │               │  │   Exceptions │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐                                        │
│  │ Validation   │  │ Utility      │                                        │
│  │ Layer        │  │ Layer        │                                        │
│  │ Jakarta      │  │ - DateUtils  │                                        │
│  │ Validation   │  │ - Calculator │                                        │
│  │ Annotations  │  │ - Constants  │                                        │
│  │ + Custom     │  │              │                                        │
│  │ Validators   │  │              │                                        │
│  └──────────────┘  └──────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Summary of Layers

| Layer | Responsibility | Technologies |
|---|---|---|
| **Controller** | HTTP request handling, response serialization, basic validation | Spring Web (`@RestController`) |
| **Service** | Business logic, orchestration, validation | Spring (`@Service`), custom logic |
| **Repository** | Database access, queries | Spring Data JPA (`JpaRepository`) |
| **Entity** | ORM mapping to database tables | Jakarta Persistence (`@Entity`) |
| **DTO** | Request/response data carriers | POJOs, MapStruct mappers |
| **Config** | Service configuration (CORS, Swagger, beans) | Spring `@Configuration` |
| **Security** | JWT validation, role-based access control | Spring Security, custom filters |
| **Exception** | Centralized error handling, custom exceptions | `@RestControllerAdvice` |
| **Validation** | Input validation on DTOs | Jakarta Validation (`@Valid`) |
| **Utility** | Shared helper classes, constants, calculators | Custom utility classes |

---

## 7. Authentication Flow

### 7.1 Registration Flow

```
Client                    API Gateway               Auth Service              User Service
  │                           │                         │                        │
  │  POST /api/auth/register  │                         │                        │
  │  {name, email, phone,    │                         │                        │
  │   address, password}     │                         │                        │
  ├──────────────────────────►│                         │                        │
  │                           │  Route to Auth Service  │                        │
  │                           ├────────────────────────►│                        │
  │                           │                         │  1. Validate input     │
  │                           │                         │  2. Check email unique │
  │                           │                         │  3. Hash password      │
  │                           │                         │   (BCrypt)            │
  │                           │                         │  4. POST /internal/   │
  │                           │                         │     users (create     │
  │                           │                         │     profile)          ├──────►│
  │                           │                         │                        │        │
  │                           │                         │◄───────────────────────┤        │
  │                           │                         │  5. Store credentials  │        │
  │                           │                         │     in Auth DB         │        │
  │                           │                         │  6. Return success     │        │
  │                           │◄────────────────────────┤                        │        │
  │◄──────────────────────────┤                         │                        │        │
  │  {message: "Registration                         │                        │
  │   successful"}           │                         │                        │
```

**Steps:**
1. Client sends registration data to gateway.
2. Gateway routes to Auth Service.
3. Auth Service validates input (email format, password strength, required fields).
4. Auth Service checks email uniqueness in Auth DB.
5. Auth Service hashes password with BCrypt.
6. Auth Service calls **User Service** (internal REST) to create user profile (name, phone, address, account number).
7. User Service generates a unique consumer account number and stores profile in User DB.
8. Auth Service stores the credential mapping (email + hashed password + role + user ID from User Service) in Auth DB.
9. Success response returned to client.

### 7.2 Login Flow

```
Client                    API Gateway               Auth Service
  │                           │                         │
  │  POST /api/auth/login     │                         │
  │  {email, password}        │                         │
  ├──────────────────────────►│                         │
  │                           │  Route to Auth Service  │
  │                           ├────────────────────────►│
  │                           │                         │  1. Find user by email
  │                           │                         │  2. Verify BCrypt hash
  │                           │                         │  3. Check account active
  │                           │                         │  4. Generate JWT:
  │                           │                         │     sub=email,
  │                           │                         │     userId=..., role=...
  │                           │                         │     iat, exp (24h)
  │                           │◄────────────────────────┤
  │◄──────────────────────────┤                         │
  │  {token: "<JWT>",         │                         │
  │   userId: 101,            │                         │
  │   role: "CONSUMER",       │                         │
  │   expiresIn: 86400}       │                         │
```

**Steps:**
1. Client sends login credentials to gateway.
2. Gateway routes to Auth Service.
3. Auth Service looks up user by email in Auth DB.
4. Auth Service verifies password against BCrypt hash.
5. Auth Service checks account is active (via User Service or cached status).
6. Auth Service generates JWT with claims: `sub` (email), `userId`, `role`, `iat`, `exp` (24 hours).
7. JWT token and user metadata returned to client.
8. Client stores token (localStorage/sessionStorage) and includes it in `Authorization: Bearer <token>` header for subsequent requests.

### 7.3 JWT Token Flow

**Token Generation (Auth Service):**

| Claim | Value | Purpose |
|---|---|---|
| `sub` | User email | Subject identifier |
| `userId` | Numeric user ID | Consumer/service identification |
| `role` | `CONSUMER` or `ADMIN` | Authorization |
| `iat` | Issued at timestamp | Token creation time |
| `exp` | Expiration timestamp | Token expiry (24 hours from `iat`) |

**Token Validation (API Gateway & Services):**

1. Gateway intercepts every request (except `/api/auth/**`).
2. Extracts `Authorization` header and parses the JWT.
3. Validates:
   - Signature integrity (HMAC-SHA256 with shared secret or RSA public key).
   - Token not expired (`exp` claim).
   - Required claims present (`sub`, `userId`, `role`).
4. If valid, forwards the request (including JWT) to the downstream service.
5. If invalid/expired, returns `401 Unauthorized`.

### 7.4 Gateway Validation

| Route Pattern | Auth Required | Role Required |
|---|---|---|
| `POST /api/auth/register` | ❌ No | — |
| `POST /api/auth/login` | ❌ No | — |
| `/api/users/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/readings/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/bills/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/payments/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/notifications/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/complaints/**` | ✅ Yes | CONSUMER or ADMIN |
| `/api/admin/**` | ✅ Yes | ADMIN only |

**Gateway Filter Chain:**

```
1. Request arrives → 2. Extract path
   → If public path (/auth/**) → skip auth → forward
   → If protected path → extract JWT → validate signature & expiry
     → If invalid → 401
     → If valid → extract role from claims
       → If admin path & role != ADMIN → 403
       → Add userId & role to forwarded headers → forward
```

### 7.5 Role-Based Authorization

**Consumer Permissions (enforced at service level):**
- Can access own profile only (service checks `userId` in JWT matches requested `userId`).
- Can submit readings for own consumer ID only.
- Can view own bills and payment history only.
- Can raise complaints as self only.
- Cannot access admin endpoints.

**Admin Permissions:**
- Can access any consumer's profile, readings, bills, payments.
- Can generate bills, manage tariff slabs.
- Can view, update status, and resolve any complaint.
- Can send broadcast and targeted notifications.
- Can access admin-only dashboard and reports.

**Enforcement:**
- **Gateway level:** Route-based role check (admin routes require `ADMIN` role in JWT).
- **Service level:** Method-level authorization using `@PreAuthorize` (Spring Security) and manual checks (e.g., comparing JWT `userId` to resource owner ID).

### 7.6 Refresh Token (Future Enhancement)

**Planned for a future iteration:**
- Short-lived access token (15 minutes) + long-lived refresh token (7 days).
- Refresh token stored securely (httpOnly cookie or secure storage).
- `/api/auth/refresh` endpoint accepts refresh token and returns a new access token.
- Refresh token rotation: each refresh invalidates the previous refresh token.

---

## 8. Deployment Architecture

### 8.1 Developer Machine

```
┌────────────────────────────────────────────────────────────┐
│                    Developer Laptop                         │
│                                                             │
│  ┌────────────────┐   ┌─────────────────────────────────┐  │
│  │  IDE (VS Code/  │   │        Terminal / Docker        │  │
│  │  IntelliJ)      │   │                                 │  │
│  │  - Backend code │   │  docker-compose up (MySQL per   │  │
│  │  - Frontend code│   │  service via separate containers)│  │
│  └────────────────┘   └─────────────────────────────────┘  │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Service Startup Order:                                │ │
│  │                                                         │ │
│  │  1. Eureka Server   (mvn spring-boot:run -pl eureka)   │ │
│  │  2. Config Server   (if centralized config needed)     │ │
│  │  3. Auth Service    (mvn spring-boot:run -pl auth)     │ │
│  │  4. User Service    (mvn spring-boot:run -pl user)     │ │
│  │  5. Meter Service   (mvn spring-boot:run -pl meter)    │ │
│  │  6. Billing Service (mvn spring-boot:run -pl billing)  │ │
│  │  7. Payment Service (mvn spring-boot:run -pl payment)  │ │
│  │  8. Notification Svc(mvn spring-boot:run -pl notify)   │ │
│  │  9. API Gateway     (mvn spring-boot:run -pl gateway)  │ │
│  │  10. Frontend       (cd frontend && npm run dev)       │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**Development Setup Details:**

| Component | Run Command | URL |
|---|---|---|
| Eureka Server | `mvn spring-boot:run -pl eureka-server` | http://localhost:8761 |
| API Gateway | `mvn spring-boot:run -pl api-gateway` | http://localhost:8080 |
| Auth Service | `mvn spring-boot:run -pl auth-service` | http://localhost:8081 |
| User Service | `mvn spring-boot:run -pl user-service` | http://localhost:8082 |
| Meter Service | `mvn spring-boot:run -pl meter-service` | http://localhost:8083 |
| Billing Service | `mvn spring-boot:run -pl billing-service` | http://localhost:8084 |
| Payment Service | `mvn spring-boot:run -pl payment-service` | http://localhost:8085 |
| Notification Service | `mvn spring-boot:run -pl notification-service` | http://localhost:8086 |
| Frontend | `npm run dev` | http://localhost:5173 |
| MySQL (all services) | Docker Compose (multiple containers) | localhost:3307–3312 |

**MySQL Docker Compose (development):**

| Service | Database Name | Container Port | Host Port |
|---|---|---|---|
| Auth Service | `voltaras_auth` | 3306 | 3307 |
| User Service | `voltaras_users` | 3306 | 3308 |
| Meter Service | `voltaras_meter` | 3306 | 3309 |
| Billing Service | `voltaras_billing` | 3306 | 3310 |
| Payment Service | `voltaras_payment` | 3306 | 3311 |
| Notification Service | `voltaras_notification` | 3306 | 3312 |

### 8.2 Docker Deployment (Future)

```
┌────────────────────────────────────────────────────────────────┐
│                   Docker Host (Testing / Staging)               │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Docker Compose (docker-compose.yml)                      │  │
│  │                                                           │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │  │
│  │  │ eureka-server │  │  api-gateway  │  │  auth-service  │ │  │
│  │  │ :8761         │  │  :8080        │  │  :8081         │ │  │
│  │  └──────────────┘  └──────────────┘  └───────┬────────┘ │  │
│  │                                               │          │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌───────▼────────┐ │  │
│  │  │ user-service  │  │ meter-service│  │ billing-service│ │  │
│  │  │ :8082         │  │ :8083        │  │ :8084          │ │  │
│  │  └───────┬───────┘  └──────┬───────┘  └───────┬────────┘ │  │
│  │          │                 │                   │          │  │
│  │  ┌───────▼───────┐  ┌─────▼────────┐  ┌──────▼─────────┐ │  │
│  │  │payment-service│  │notification  │  │ MySQL Containers│ │  │
│  │  │ :8085         │  │ -service     │  │ (6 DBs, each   │ │  │
│  │  └───────────────┘  │ :8086        │  │  mapped to own │ │  │
│  │                      └──────────────┘  │  container)    │ │  │
│  │                                         └────────────────┘ │  │
│  │                                                           │  │
│  │  ┌──────────────────────────────────────────────────────┐ │  │
│  │  │  Frontend (Nginx, :80)                               │ │  │
│  │  │  Reverse proxy → http://api-gateway:8080             │ │  │
│  │  └──────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### 8.3 Cloud Deployment (Future)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Cloud Provider (AWS / DigitalOcean / VPS)    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Load Balancer (Port 443 — HTTPS)                           │    │
│  │  SSL Termination (Let's Encrypt)                            │    │
│  └──────────────────────────┬──────────────────────────────────┘    │
│                             │                                       │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  API Gateway (1+ instances, horizontally scalable)          │    │
│  │  - JWT validation                                           │    │
│  │  - Rate limiting (Future)                                   │    │
│  └───┬───┬───┬───┬───┬───┬─────────────────────────────────────┘    │
│      │   │   │   │   │   │                                        │
│      ▼   ▼   ▼   ▼   ▼   ▼                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Eureka Server (1+ instances for HA)                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │
│  │ Auth │ │ User │ │Meter │ │ Bill │ │ Pay  │ │ Notif│          │
│  │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │          │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘          │
│     │        │        │        │        │        │               │
│     ▼        ▼        ▼        ▼        ▼        ▼               │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │
│  │MySQL │ │MySQL │ │MySQL │ │MySQL │ │MySQL │ │MySQL │          │
│  │Auth  │ │User  │ │Meter │ │Bill  │ │Pay   │ │Notif │          │
│  │(RDS) │ │(RDS) │ │(RDS) │ │(RDS) │ │(RDS) │ │(RDS) │          │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘          │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Frontend (Nginx, CDN for static assets)                     │    │
│  │  - Static build served via Nginx or cloud object storage    │    │
│  │  - API calls proxied to Load Balancer → API Gateway          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  CI/CD Pipeline (GitHub Actions)                             │    │
│  │  - On push to develop: build → test → deploy to staging     │    │
│  │  - On push to main: build → test → deploy to production     │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 9. Design Principles

### SOLID Principles

| Principle | How VOLTARAS Applies It |
|---|---|
| **S — Single Responsibility** | Each microservice has one domain responsibility. Within a service, each class has one reason to change. Controllers handle HTTP, services handle business logic, repositories handle data access. |
| **O — Open/Closed** | Services are open for extension via new features added through new endpoints or service methods, but closed for modification of existing, tested behavior. Interfaces allow swapping implementations. |
| **L — Liskov Substitution** | Service interfaces guarantee that any implementation is substitutable. Repository interfaces ensure any JPA implementation works without altering business logic. |
| **I — Interface Segregation** | Each service defines focused interfaces. A `ReadingService` does not expose payment methods. Small, role-specific DTOs are created rather than massive general-purpose objects. |
| **D — Dependency Inversion** | High-level service implementations depend on interfaces (e.g., `ReadingService` interface), not concrete classes. The service layer does not instantiate repositories directly — they are injected. |

### REST Principles

| Principle | Application in VOLTARAS |
|---|---|
| **Statelessness** | Each request contains all necessary information (JWT token in header, all required parameters). No server-side session state. |
| **Resource-Based** | URLs represent resources (`/api/users`, `/api/readings`, `/api/bills`), not actions. HTTP methods define operations. |
| **Uniform Interface** | Consistent naming: `GET` for read, `POST` for create, `PUT` for full update, `DELETE` for removal. Response formats are uniform across all services. |
| **HATEOAS (Future)** | Links in responses to enable client navigation without hardcoded URLs. |
| **Stateless Caching** | Cache-Control headers on GET responses where appropriate (e.g., tariff slabs, which change infrequently). |

### Repository Pattern

| Aspect | Implementation |
|---|---|
| **Purpose** | Abstracts data access logic behind an interface. Business logic never works with raw `EntityManager` or JDBC. |
| **Interface** | Each entity has a repository interface extending `JpaRepository<T, ID>`. |
| **Custom Queries** | Business-specific queries use `@Query` annotations or derived method names (e.g., `findByUserIdAndReadingDateBetween()`). |
| **Transactions** | Repository methods inherit transactional behavior. Service layer defines transaction boundaries with `@Transactional`. |
| **Testability** | Repositories can be mocked in unit tests using Mockito. |

### DTO Pattern

| Aspect | Implementation |
|---|---|
| **Separation** | Entities are never exposed via API. Request and response DTOs are used at all API boundaries. |
| **Request DTOs** | Contain only fields needed for input. Annotated with Jakarta Validation constraints. |
| **Response DTOs** | Contain only fields meant for the client. No sensitive data (passwords, internal IDs where unnecessary). |
| **Mapping** | MapStruct generates type-safe mapping code between entities and DTOs at compile time. |
| **Per-Service DTOs** | Each microservice defines its own DTOs. Cross-service calls may use shared DTOs or internal DTOs. |

### Constructor Injection

| Aspect | Implementation |
|---|---|
| **Rule** | All dependencies are injected through constructors, not field injection (`@Autowired` on fields is prohibited). |
| **Mechanism** | Either explicit constructor or Lombok's `@RequiredArgsConstructor` for `final` fields. |
| **Benefits** | Makes dependencies explicit, enables final fields (immutability), simplifies testing (no reflection needed), prevents null dependencies at construction time. |

**Example pattern:**

```java
@Service
public class ReadingServiceImpl implements ReadingService {

    private final MeterReadingRepository readingRepository;
    private final ReadingMapper readingMapper;

    // Constructor injection (explicit or via Lombok)
    public ReadingServiceImpl(MeterReadingRepository readingRepository,
                              ReadingMapper readingMapper) {
        this.readingRepository = readingRepository;
        this.readingMapper = readingMapper;
    }

    // Business methods...
}
```

### Layered Architecture

| Layer | Communication Rules |
|---|---|
| **Controller** | Calls Service (interface). Never calls Repository directly. Never uses Entity classes. |
| **Service** | Calls Repository. May call other Services (via interface). Uses Entity and DTO classes. |
| **Repository** | Only accesses Entity classes. Returns Entity objects to Service layer. |
| **DTO** | Used at Controller boundary only. Service converts between Entity and DTO via Mapper. |

**Cross-layer dependency rule:** A layer may only depend on the layer directly below it (Controller → Service → Repository). No skipping layers.

### Loose Coupling

| Strategy | How Achieved |
|---|---|
| **Microservices** | Each service is independently deployable with its own database. No direct database access across services. All cross-service communication is via REST API contracts. |
| **Interface-based Design** | Services depend on interfaces, not implementations. Swapping an implementation (e.g., different payment processor) does not affect callers. |
| **Eventual Async (Future)** | For non-critical workflows (e.g., sending notification after bill generation), an event-driven approach with message queues will decouple services further. |
| **API Gateway** | Clients are decoupled from backend service topology. Service location, scaling, and routing changes are transparent to the frontend. |

### Single Responsibility

| Level | Application |
|---|---|
| **Microservice** | Each service owns exactly one business domain (Auth, User, Meter, Billing, Payment, Notification). No service handles responsibilities outside its domain. |
| **Class** | Each class has one clear purpose. Controller only handles HTTP concerns. Service only handles business logic. Repository only handles data access. Entity only represents the data model. Exception handler only handles errors. |
| **Method** | Each method does one thing. For example, `submitReading()` validates input, calls repository, and triggers bill computation — but each of these steps is delegated to private helper methods or injected collaborators. |

---

> **End of Phase 4 — Deliverable**
> *`docs/03_ARCHITECTURE.md` has been generated.*
> *Pending approval to proceed to Phase 5.*
