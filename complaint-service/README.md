# VOLTARAS Complaint Service

Complaint management for the VOLTARAS platform. Consumers raise complaints,
track their status and add comments; admins review the full complaint queue,
assign complaints, drive the lifecycle and add resolution comments.

- **Port:** `8087`
- **Database:** `complaint_db` (MySQL, `ddl-auto: update`)
- **Eureka:** registers as `complaint-service` (`lb://COMPLAINT-SERVICE`)
- **API Gateway:** `http://localhost:8080/api/complaints/**` and
  `http://localhost:8080/api/admin/complaints/**`
- **Swagger UI:** `http://localhost:8087/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8087/v3/api-docs`

## Authentication

Authentication is handled by the API Gateway (JWT validation). After
validating the token the Gateway injects `X-User-Id` and `X-User-Role`
headers; this service authorizes in the service layer only:

- The gateway role format is exactly `ADMIN` / `CONSUMER` (the Auth Service
  issues the JWT `role` claim as the RoleType enum name).
- Admin operations require `X-User-Role = ADMIN`; the `ROLE_ADMIN` spelling
  is never produced by the VOLTARAS gateway and is not accepted.
- Consumer operations are scoped to the complaint owner; the owner ID stored
  on the complaint is the gateway `X-User-Id` (Auth Service user ID).

The direct port is for local development and must not be exposed publicly in
production.

## Complaint Lifecycle

```
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
```

- `CLOSED` is terminal; complaints are never cancelled or deleted.
- Invalid, same-status and terminal transitions return
  `400 BUSINESS_RULE_VIOLATION`.
- Every transition writes a `complaint_status_history` row and publishes a
  `ComplaintStatusChangedEvent` to RabbitMQ.

## RabbitMQ

The service publishes complaint status-changed events to the existing
Notification Service topology (exchange `voltaras.notification.exchange`,
routing key `notification.complaint.status`, consumed on
`voltaras.complaint.status.queue`). Events are serialized with
`Jackson2JsonMessageConverter` and carry the logical type ID
`ComplaintStatusChangedEvent` so the Notification Service can deserialize
them into its own event class. Publishing is best-effort: broker failures
are logged and never roll back the complaint update.

## Main APIs

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/complaints` | CONSUMER | Raise a complaint (ticket number `CMP-YYYYMMDD-NNNN`) |
| GET | `/api/complaints` | CONSUMER | Own complaints (page/size/sort + status/priority/category filters) |
| GET | `/api/complaints/{id}` | CONSUMER | Own complaint with comments and history |
| GET | `/api/complaints/ticket/{ticketNumber}` | CONSUMER | Own complaint by ticket number |
| PUT | `/api/complaints/{id}` | CONSUMER | Edit own complaint while OPEN |
| POST | `/api/complaints/{id}/comments` | CONSUMER | Add a comment to own complaint |
| GET | `/api/complaints/categories` | ANY | Active categories |
| GET | `/api/complaints/internal/count` | ADMIN (internal) | Per-status counts for the admin dashboard |
| GET | `/api/admin/complaints` | ADMIN | All complaints with filters |
| GET | `/api/admin/complaints/{id}` | ADMIN | Complaint details |
| GET | `/api/admin/complaints/ticket/{ticketNumber}` | ADMIN | Complaint by ticket number |
| PATCH | `/api/admin/complaints/{id}/status` | ADMIN | Status transition |
| PUT | `/api/admin/complaints/{id}/assign` | ADMIN | Assign to an admin |
| POST | `/api/admin/complaints/{id}/comments` | ADMIN | Resolution comment |

## Run

```bash
mvn spring-boot:run
```

Requires MySQL (`complaint_db`) and optionally RabbitMQ (for notifications).
Environment variables: `DB_PASSWORD`, `DB_USERNAME` (default `root`),
`DB_HOST`/`DB_PORT`/`DB_NAME`, `EUREKA_SERVER_URL`,
`RABBITMQ_HOST`/`RABBITMQ_PORT`/`RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD`.

## Test

```bash
mvn clean test
```

Unit and web-layer tests run against an in-memory H2 database; Eureka and
RabbitMQ are not required.
