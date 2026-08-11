# VOLTARAS Meter Management Service

## Purpose

The **Meter Management Service** is the **source of truth for physical
electricity meters** in the VOLTARAS platform. It stores meter details,
ownership, assignment and status.

It deliberately does **not** store monthly consumption values — meter
readings belong to the **Meter Reading Service**. This service only answers
"which meter is installed where, and who owns it".

The service is responsible for:

- Storing meter master data in MySQL (`meter_management_db`)
- Registering new meters (ADMIN only)
- Assigning meters to consumers (`authUserId`) and organizations
- Tracking meter lifecycle status (`ACTIVE`, `INACTIVE`, `FAULTY`,
  `REPLACED`, `REMOVED`)
- Soft-deleting meters by changing status to `REMOVED` (no hard deletes)
- Serving user APIs scoped to the authenticated `X-User-Id`
- Enforcing authorization through the identity headers injected by the
  API Gateway (`X-User-Id`, `X-User-Role`)

## Port

- **Default port:** `8089`
- Configurable via `server.port` in `application.yml` or runtime argument
  `--server.port=<port>`.

## Architecture

```
API Gateway (8080) ── JWT validation, injects X-User-Id / X-User-Role
        │
        ▼
Meter Management Service (8089) ── MySQL meter_management_db
        │
        ├── GET  /api/meters            (user, own meters only)
        ├── GET  /api/meters/{id}       (user, own meter only)
        └── /api/meters/admin/*         (ADMIN: create, list, get, update,
                                          assign, status, soft delete)
```

Meter Reading Service stores `meter_readings` (consumption values);
Meter Management Service stores `meters` (the meter master records).
The two services share the `meterNumber` identifier.

## Database

- **Name:** `meter_management_db` (MySQL), created with
  `CREATE DATABASE meter_management_db;`
- Schema is managed with `spring.jpa.hibernate.ddl-auto=update` like the
  other services.
- Table: `meters` with a unique constraint on `meter_number`.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/api/meters` | User | Get my meters (newest first) |
| `GET`  | `/api/meters/{id}` | User | Get one of my meters (owner only) |
| `POST` | `/api/meters/admin` | ADMIN | Create a new meter |
| `GET`  | `/api/meters/admin` | ADMIN | Get all meters (filters: `status`, `authUserId`, `organizationId`, `meterNumber`) |
| `GET`  | `/api/meters/admin/{id}` | ADMIN | Get meter by ID |
| `PUT`  | `/api/meters/admin/{id}` | ADMIN | Update meter details |
| `PATCH`| `/api/meters/admin/{id}/assign` | ADMIN | Assign meter to a user |
| `PATCH`| `/api/meters/admin/{id}/status` | ADMIN | Update meter status |
| `DELETE` | `/api/meters/admin/{id}` | ADMIN | Soft delete (status → `REMOVED`) |

## Security Rules

- Authentication is performed by the API Gateway (JWT). The Gateway injects
  `X-User-Id` and `X-User-Role` headers; this service never trusts
  client-supplied user IDs.
- User APIs scope every read to the authenticated `X-User-Id`: fetching a
  meter owned by another user returns `404 RESOURCE_NOT_FOUND`.
- Admin APIs require `X-User-Role = ADMIN` (or `ROLE_ADMIN`, consistent with
  the other VOLTARAS services); any other role receives
  `403 ACCESS_DENIED`.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `METER_MANAGEMENT_DB_USERNAME` | `root` | MySQL username |
| `METER_MANAGEMENT_DB_PASSWORD` | *(required)* | MySQL password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka server URL |
| `JPA_SHOW_SQL` | `true` | Show generated SQL in the logs |

## Run Command

```bash
# 1. Start MySQL and Eureka, then create the database
CREATE DATABASE meter_management_db;

# 2. Start the service
cd meter-management-service
mvn spring-boot:run
```

## Swagger

- Direct: `http://localhost:8089/swagger-ui.html`
- One Swagger UI, two selectable servers:
  - `http://localhost:8089` — direct service call, no JWT required
  - `http://localhost:8080` — API Gateway (JWT required; authorize with
    the `bearerAuth` button in Swagger, then the Gateway injects
    `X-User-Id` / `X-User-Role` from the token claims)
- API calls can be executed through the Gateway route `/api/meters/**`.

## Test Command

```bash
cd meter-management-service
mvn clean test
```

Tests run against an in-memory H2 database (MySQL mode) with Eureka disabled.
