# VOLTARAS — Meter Reading Service

> **Service:** `meter-reading-service` · **Port:** `8083` · **Database:** `meter_db`
> **Base package:** `com.voltaras.meterreadingservice`
> **Framework:** Spring Boot 3.5.5 · Spring Cloud 2025.0.3 · Java 25 · Maven

---

## 1. Overview

The Meter Reading Service handles electricity meter reading submission,
history, and admin verification/rejection for the VOLTARAS platform.

**Security model — the service trusts only the API Gateway:**

| Trusted source | What it provides |
|---|---|
| `X-User-Id` header (injected by the Gateway) | The authenticated user id |
| `X-User-Email` header (injected by the Gateway) | The authenticated user email |
| `X-User-Role` header (injected by the Gateway) | The authenticated user role (`CONSUMER` / `ADMIN`) |

- The Gateway validates the JWT and **replaces** these headers — any
  client-supplied values are stripped first (`JwtAuthenticationFilter`).
- The service **never** parses JWT tokens and **never** accepts
  `authUserId` from the request body or URL.
- System-controlled fields (`unitsConsumed`, `status`, `verifiedBy`,
  `verifiedAt`, timestamps) are only ever set inside the service.

---

## 2. Project Structure

```
meter-reading-service/
├── pom.xml
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/voltaras/meterreadingservice/
    │   │   ├── MeterReadingServiceApplication.java
    │   │   ├── config/            CorsConfig.java
    │   │   ├── controller/        MeterReadingController.java
    │   │   ├── dto/request/       SubmitMeterReadingRequest.java
    │   │   │                      UpdateMeterReadingRequest.java
    │   │   │                      RejectMeterReadingRequest.java
    │   │   ├── dto/response/      MeterReadingResponse.java
    │   │   │                      ErrorResponse.java
    │   │   ├── entity/            MeterReading.java
    │   │   ├── enums/             MeterReadingStatus.java
    │   │   ├── exception/         ResourceNotFoundException.java
    │   │   │                      DuplicateResourceException.java
    │   │   │                      BadRequestException.java
    │   │   │                      ForbiddenOperationException.java
    │   │   │                      GlobalExceptionHandler.java
    │   │   ├── mapper/            MeterReadingMapper.java
    │   │   ├── repository/        MeterReadingRepository.java
    │   │   ├── service/           MeterReadingService.java
    │   │   ├── service/impl/      MeterReadingServiceImpl.java
    │   │   └── util/              package-info.java
    │   └── resources/             application.yml
    └── test/
        └── java/com/voltaras/meterreadingservice/
            ├── controller/        MeterReadingControllerTest.java
            └── service/           MeterReadingServiceImplTest.java
```

---

## 3. Business Rules

| # | Rule | Enforced at |
|---|---|---|
| 1 | `authUserId` comes only from `X-User-Id` | Controller |
| 2 | Client cannot provide `authUserId` (field absent from DTOs) | DTO design |
| 3 | `currentReading >= previousReading` | Service |
| 4 | `unitsConsumed = currentReading - previousReading` | Service |
| 5 | Client cannot provide `unitsConsumed` (field absent from DTOs) | DTO design |
| 6 | `billingMonth` between 1 and 12 | DTO validation |
| 7 | `billingYear` in 2000–2100 | DTO validation |
| 8 | No duplicate reading (user + meter + month + year) | Repository pre-check + DB unique constraint |
| 9 | New readings start as `SUBMITTED` | Service |
| 10 | Consumers see only their own readings | `findByIdAndAuthUserId` |
| 11 | Consumers update only their own `SUBMITTED` readings | `ensureEditable` |
| 12 | `VERIFIED` / `REJECTED` readings cannot be edited or deleted by consumers | `ensureEditable` |
| 13 | Only `ADMIN` can verify/reject | `requireAdminRole(role)` from `X-User-Role` |
| 14 | Verification sets `status=VERIFIED`, `verifiedBy`, `verifiedAt=now` | Service |
| 15 | Rejection sets `status=REJECTED`, mandatory `remarks`, `verifiedBy`, `verifiedAt=now` | Service |

> **Note:** the SUBMITTED-only guard (rule 12) applies to *consumer* edits.
> Admins may verify or re-reject a reading regardless of its current status
> — re-processing an already-finalized reading is intentionally allowed.

The unique constraint is declared at the database level:

```sql
uk_meter_reading_billing_period (auth_user_id, meter_number, billing_month, billing_year)
```

If the pre-check and a concurrent insert race each other, the constraint
violation is caught by `GlobalExceptionHandler` and returned as `409`.

---

## 4. API Gateway Route

Add this route to
`api-gateway/src/main/resources/application.yml` — inside the existing
`spring.cloud.gateway.server.webflux.routes` list, **after** the
`user-service` route (do not remove or replace any existing route):

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:

            - id: auth-service
              uri: lb://AUTH-SERVICE
              predicates:
                - Path=/api/auth/**

            - id: user-service
              uri: lb://USER-SERVICE
              predicates:
                - Path=/api/users/**

            # ▼▼▼ ADD THIS ROUTE ▼▼▼
            - id: meter-reading-service
              uri: lb://METER-READING-SERVICE
              predicates:
                - Path=/api/meter-readings/**
```

> `lb://METER-READING-SERVICE` resolves via Eureka to the instance
> registered by `spring.application.name: meter-reading-service`.
> The Gateway's `JwtAuthenticationFilter` already validates the JWT and
> injects `X-User-Id`, `X-User-Email`, `X-User-Role` for every request
> routed to this service — no gateway code changes are required.

---

## 5. Startup & Configuration

```bash
# 1. Start Eureka (port 8761)
cd eureka-server && mvn spring-boot:run

# 2. Start the meter reading service (needs MySQL + DB_USERNAME/DB_PASSWORD)
cd meter-reading-service
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password     # never hardcode
mvn spring-boot:run
```

Configuration is driven entirely by environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | *(required)* | MySQL password |

Ports: `8083` (direct service) — everything else goes through the
Gateway at `http://localhost:8080`.

---

## 6. MySQL Verification Queries

```sql
-- 1. Verify the service uses the right database
USE meter_db;

-- 2. Confirm the table was created by Hibernate ddl-auto=update
SHOW TABLES;

-- 3. Inspect the schema (columns, nullability, unique key)
DESCRIBE meter_readings;

-- 4. Verify the unique constraint exists
SHOW INDEX FROM meter_readings WHERE Non_unique = 0;

-- 5. Inspect the actual data
SELECT id, auth_user_id, meter_number, billing_month, billing_year,
       previous_reading, current_reading, units_consumed, reading_date,
       status, remarks, verified_by, verified_at, created_at, updated_at
FROM meter_readings;

-- 6. Confirm units_consumed is always current - previous
SELECT id, previous_reading, current_reading,
       (current_reading - previous_reading) AS expected_units,
       units_consumed
FROM meter_readings
WHERE (current_reading - previous_reading) <> units_consumed;

-- 7. Readings per status (for the admin dashboard)
SELECT status, COUNT(*) FROM meter_readings GROUP BY status;
```

---

## 7. Testing Checklist (PASS/FAIL)

| # | Check | Result |
|---|---|---|
| 1 | Service starts on port 8083 | ☐ PASS ☐ FAIL |
| 2 | Database connection to `meter_db` succeeds | ☐ PASS ☐ FAIL |
| 3 | Service registers with Eureka (`METER-READING-SERVICE` visible at http://localhost:8761) | ☐ PASS ☐ FAIL |
| 4 | API Gateway route `/api/meter-readings/**` works | ☐ PASS ☐ FAIL |
| 5 | JWT validation works (missing/invalid/expired token → 401 from Gateway) | ☐ PASS ☐ FAIL |
| 6 | `X-User-Id` forwarding works (reading owned by the right user) | ☐ PASS ☐ FAIL |
| 7 | `X-User-Role` forwarding works (consumer blocked from admin endpoints → 403) | ☐ PASS ☐ FAIL |
| 8 | Reading submission works (201 + status `SUBMITTED`) | ☐ PASS ☐ FAIL |
| 9 | Duplicate validation works (same meter + month + year → 409) | ☐ PASS ☐ FAIL |
| 10 | Units calculation works (`unitsConsumed = current - previous`) | ☐ PASS ☐ FAIL |
| 11 | Ownership validation works (foreign reading id → 404) | ☐ PASS ☐ FAIL |
| 12 | Admin verification works (status `VERIFIED`, `verifiedBy`/`verifiedAt` set) | ☐ PASS ☐ FAIL |
| 13 | Admin rejection works (status `REJECTED`, mandatory remarks) | ☐ PASS ☐ FAIL |
| 14 | Validation responses are clean (field-level `details`, no stack traces) | ☐ PASS ☐ FAIL |
| 15 | Database data is correct (queries in section 6) | ☐ PASS ☐ FAIL |

---

## 8. Git Workflow (package-by-package commits)

Commit each logical module separately. Never use `git add .` — always
add explicit paths.

```bash
# 1. Project configuration
git add meter-reading-service/pom.xml \
        meter-reading-service/.gitignore \
        meter-reading-service/src/main/resources/application.yml \
        meter-reading-service/src/main/java/com/voltaras/meterreadingservice/MeterReadingServiceApplication.java
git commit -m "feat(meter-reading): add project configuration for meter reading service"

# 2. Entity and enums
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/entity \
        meter-reading-service/src/main/java/com/voltaras/meterreadingservice/enums
git commit -m "feat(meter-reading): add meter reading entity and status enum"

# 3. Repository
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/repository
git commit -m "feat(meter-reading): add meter reading repository with derived queries"

# 4. Request and response DTOs
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/dto
git commit -m "feat(meter-reading): add request and response DTOs with validation"

# 5. Mapper
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/mapper
git commit -m "feat(meter-reading): add meter reading mapper"

# 6. Service interface
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/service/MeterReadingService.java
git commit -m "feat(meter-reading): add meter reading service interface"

# 7. Service implementation
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/service/impl
git commit -m "feat(meter-reading): implement meter reading business rules"

# 8. Controller
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/controller
git commit -m "feat(meter-reading): add consumer and admin meter reading endpoints"

# 9. Exception handling
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/exception
git commit -m "feat(meter-reading): add exception handling and error responses"

# 10. CORS config + util placeholder
git add meter-reading-service/src/main/java/com/voltaras/meterreadingservice/config \
        meter-reading-service/src/main/java/com/voltaras/meterreadingservice/util
git commit -m "chore(meter-reading): add CORS configuration"

# 11. Unit tests
git add meter-reading-service/src/test
git commit -m "test(meter-reading): add unit and web-layer tests"

# 12. API Gateway route (separate change to the gateway repo/module)
git add api-gateway/src/main/resources/application.yml
git commit -m "feat(gateway): add meter reading service route"

# 13. API testing documentation + Postman collection
git add docs/08_METER_READING_SERVICE.md \
        docs/09_METER_READING_POSTMAN_GUIDE.md \
        docs/postman/voltaras-meter-reading.postman_collection.json
git commit -m "docs(meter-reading): add API testing guide and Postman collection"
```

> Run `mvn clean package` inside `meter-reading-service` before committing
> to confirm the module compiles and all tests pass.

---

## 9. Daily Electricity Usage Tracking

Two consumer endpoints power the **Daily electricity tracking** screens in the
consumer portal (both routed through the Gateway at `/api/meter-readings/**`):

| Endpoint | Purpose |
|---|---|
| `GET /api/meter-readings/me/daily-usage` | Today's units, month-to-date units, estimated costs and the last 7 days of daily usage |
| `GET /api/meter-readings/me/usage-summary?days=7` | Same summary with a configurable look-back window (1–31 days) |

Both return `DailyUsageResponse` (`meterNumber`, `usageDate`, `previousReading`,
`latestReading`, `previousReadingAt`, `latestReadingAt`, `unitsConsumedToday`,
`estimatedPerUnitCost`, `estimatedTodayCost`, `monthUnitsSoFar`,
`estimatedMonthCost`, `hasReadings`, `hasReadingToday`, `dailyUsage[]`).

**Calculation rules** (all backend-side, from the consumer's real readings):

1. Readings used: `SUBMITTED` + `VERIFIED` (REJECTED excluded), for the
   consumer's most recently active meter.
2. Daily consumption = difference between consecutive recorded meter values
   (cumulative meter behaviour — a missing day reports 0 units and the next
   recorded reading carries the gap).
3. Today units = latest value recorded today − latest value recorded before
   today; `hasReadingToday` is `false` when no reading exists for today.
4. Month units so far = latest value in the current billing month − the last
   value recorded before the month start.
5. Costs are **estimates** mirroring `BillCalculator` (`TariffCalculator` in
   this service): progressive slabs ₹1.50/₹2.50/₹4.00/₹6.00, ₹100 fixed
   charge, 5% tax. `estimatedPerUnitCost` is the blended rate
   `energyCharge(monthUnits) / monthUnits`. The generated bill remains the
   source of truth.
6. No reading data is ever fabricated: days without a reading return zero
   units with `readingAt` null.
