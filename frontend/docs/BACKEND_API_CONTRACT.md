# VOLTARAS Backend API Contract (Frontend Reference)

> Source of truth: backend controllers/DTOs (this repository) cross-checked against
> live `/v3/api-docs` from the running Docker stack (August 2026) and the API Gateway
> route table (`api-gateway/src/main/resources/application.yml`).
>
> **All frontend traffic must go through the API Gateway at `http://localhost:8080`.**
> Do not call service ports (8081-8089) directly from the browser.

## 1. Gateway routing (api-gateway, port 8080)

| Gateway route prefix | Target service | Port | Notes |
|---|---|---|---|
| `/api/auth/**` | auth-service | 8081 | includes `/api/auth/internal/users/**` |
| `/api/users/**` | user-service | 8082 | profile CRUD |
| `/api/meter-readings/**` | meter-reading-service | 8083 | readings, `/me` and `/admin` |
| `/api/bills/**` | bill-service | 8084 | bills, `/me` and `/admin` |
| `/api/organizations/**`, `/api/buildings/**`, `/api/blocks/**`, `/api/floors/**`, `/api/units/**`, `/api/admin/organizations/**` | organization-service | 8085 | orgs + structure |
| `/api/payments/**`, `/api/recharges/**`, `/api/wallet/**`, `/api/wallets/**`, `/api/bills/*/payments/**` | payment-service | 8086 | wallet, payments, recharges |
| `/api/complaints/**`, `/api/admin/complaints/**` | complaint-service | 8087 | consumer + admin complaint APIs |
| `/api/notifications/**` | notification-service | 8088 | notifications |
| — (NO route) | **meter-management-service** | 8089 | **BLOCKER**: `/api/meters/**` is not routed through the Gateway |

### Gateway security behaviour
- The Gateway is a global filter (`JwtAuthenticationFilter`). Every non-public request
  must carry `Authorization: Bearer <accessToken>`.
- On success the Gateway **replaces** `X-User-Id`, `X-User-Email`, `X-User-Role` headers
  with values decoded from the JWT. Services trust these headers, never body fields.
- Role values are exactly `ADMIN` and `CONSUMER` (uppercase, from the JWT).
- **Public (no Bearer required):**
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh-token`
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`
  - `POST /api/payments/webhooks/**`
  - `GET /api/auth/internal/users/**` (service-to-service; not browser-facing)
  - `/actuator/**`
- Missing/invalid token → `401` with body `{"status":401,"error":"UNAUTHORIZED","message":"..."}`.
- CORS: `http://localhost:*` / `http://127.0.0.1:*` allowed for all methods/headers;
  `Authorization` is exposed; credentials not required (JWT in header).

## 2. Auth endpoints (POST /api/auth/..., auth-service)

### POST /api/auth/register — public, 201
Request body (all required, validated):
```json
{
  "fullName": "Jane Doe",          // required, 2-100 chars
  "email": "jane@example.com",     // required, valid email, max 255
  "phone": "9876543210",           // required, exactly 10 digits
  "password": "Voltaras@123",      // required, min 8 chars, 1 upper, 1 lower, 1 digit
  "confirmPassword": "Voltaras@123",
  "address": "Flat 101, ..."       // required, max 500 chars
}
```
- Every new account is created with role **CONSUMER** (there is no role field on the form).
- `409 Conflict` when email or phone already exists; `400` on validation failure.

### POST /api/auth/login — public, 200
Request: `{ "email": "...", "password": "..." }` (both required).
Response `AuthResponse`:
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshTokenExpiresIn": 604800,
  "userId": 5,
  "role": "CONSUMER",        // exact: ADMIN | CONSUMER
  "email": "vinay.demo@voltaras.local",
  "fullName": "Vinay ...",
  "message": null
}
```
- `401` on bad credentials with ErrorResponse body (see section 8).

### POST /api/auth/refresh-token — public, 200
Request: `{ "refreshToken": "<jwt>" }` → `RefreshTokenResponse` (new access + refresh token).

### POST /api/auth/forgot-password — public, 200
Request: `{ "email": "..." }`.
Response: `{ "message": "If an account exists for that email, password reset instructions have been sent." }`
— identical for existing/unknown emails. `400` invalid email; `429` rate limited.
**Note:** the reset link is delivered by email (Mailpit in the demo stack) and contains a
one-time token. The demo flow therefore cannot complete in the browser without reading the
inbox; the page is implemented as "request sent" confirmation.

### POST /api/auth/reset-password — public, 200
Request: `{ "token": "...", "newPassword": "...", "confirmNewPassword": "..." }`
(newPassword same rule as register). Token one-time, expires in 15 minutes.

### POST /api/auth/change-password — Bearer, 200
Request `ChangePasswordRequest` (currentPassword/newPassword/confirmNewPassword). Not used in Phase 1 screens.

### GET /api/auth/profile — Bearer, 200
Response `UserInfoResponse`: `{ userId, email, role, isActive, lastLoginAt, createdAt }`.
**Note:** does NOT return fullName/phone/address.

### POST /api/auth/logout — Bearer, 200
Revokes server-side refresh session. Client must clear stored tokens.

## 3. User profile (user-service)

Base `/api/users/profile`, all Bearer + `X-User-Id` from Gateway.

| Method | Path | Body / notes |
|---|---|---|
| POST | `/api/users/profile` | `CreateUserProfileRequest` (see below), 201, `409` if exists |
| GET | `/api/users/profile` | 200 or `404` when profile missing |
| PUT | `/api/users/profile` | `UpdateUserProfileRequest` |
| DELETE | `/api/users/profile` | 200 `{message}` |

`CreateUserProfileRequest` fields (all optional except `fullName`):
`fullName` (2-100), `phone` (10 digits), `address` (max 500), `city`, `state`, `country` (max 100),
`postalCode` (`^[A-Za-z0-9\- ]{3,10}$`), `profileImage` (max 1000), `dateOfBirth` (past date),
`gender` (`MALE|FEMALE|OTHER`).

> Registration does **not** auto-create a profile. Registration signs the user in only;
> profile creation is handled separately by the user-service.

## 4. Complaints (complaint-service, both consumer + admin)

### Consumer
| Method | Path | Notes |
|---|---|---|
| GET | `/api/complaints` | own complaints, Spring `Page` (`content[]`, `totalElements`, ...); filters `status`, `priority`, `categoryId`; default sort `createdAt DESC` |
| POST | `/api/complaints` | `CreateComplaintRequest` — `categoryId` (required, active), `subject` (required), `description` (optional/required per DTO) |
| GET | `/api/complaints/{id}` | own complaint detail |
| GET | `/api/complaints/ticket/{ticketNumber}` | lookup by ticket `CMP-YYYYMMDD-NNNN` |
| PUT | `/api/complaints/{id}` | edit while OPEN |
| POST | `/api/complaints/{id}/comments` | comment while not CLOSED |
| GET | `/api/complaints/categories` | active categories list (form dropdown) |
| GET | `/api/complaints/internal/count` | status counts, ADMIN only |

### Admin
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/complaints` | all complaints, `Page`; filters `status`, `priority`, `categoryId`, `consumerId`, `assignedTo`, `fromDate`, `toDate` |
| GET | `/api/admin/complaints/{id}` | any complaint detail |
| GET | `/api/admin/complaints/ticket/{ticketNumber}` | by ticket |
| PATCH | `/api/admin/complaints/{id}/status` | OPEN → IN_PROGRESS → RESOLVED → CLOSED |
| PUT | `/api/admin/complaints/{id}/assign` | assign to admin (OPEN/IN_PROGRESS) |
| POST | `/api/admin/complaints/{id}/comments` | admin comment |

Role enforcement: `X-User-Role` must be exactly `ADMIN` for `/api/admin/**` and
`/api/complaints/internal/count`; `403` otherwise.

## 5. Other services (available through Gateway, not used by Phase 1 screens)

- **meter-reading** `/api/meter-readings/**` — submit/list readings, admin verify/reject.
- **bill** `/api/bills/**` — `/me`, `/me/outstanding`, `/me/filter`, admin generate/update.
- **payment** `/api/wallet/me`, `/api/wallet/top-up`, `/api/recharges/orders`, `/api/payments/**` — wallet + Razorpay flow.
- **organization** `/api/organizations/**`, `/api/admin/organizations/**`, `/api/buildings|blocks|floors|units/**`.
- **notification** `/api/notifications/**` — list, unread count, mark read.

## 6. BLOCKERS (Gateway gaps — do not bypass)

1. **`/api/meters/**` (meter-management-service) has no Gateway route.**
   The service exists on port 8089 with `GET /api/meters`, `GET /api/meters/{id}`, and
   `/api/meters/admin/**` CRUD, but the Gateway's route table has no entry for it, so
   browser calls to `http://localhost:8080/api/meters/**` fail (no matching route).
   Phase 1 does not render meter data; a meter dashboard requires adding the route first.

## 7. Error response shape (consistent across services)

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request",
    "details": [ { "field": "phone", "message": "Phone number must be exactly 10 digits" } ]
  },
  "timestamp": "2026-08-13T10:00:00",
  "path": "/api/auth/register"
}
```
Gateway-level 401 has a different shape: `{ "status": 401, "error": "UNAUTHORIZED", "message": "..." }`.

## 8. Frontend obligations derived from the contract

- Store `accessToken` + `refreshToken`; attach `Authorization: Bearer <accessToken>` via a single Axios interceptor.
- Read role only from the **login response** (`AuthResponse.role`) and from the JWT payload (`role` claim) — never from user input.
- Redirect by role: `ADMIN` → admin shell, `CONSUMER` → consumer shell.
- Register form fields exactly: `fullName`, `email`, `phone`, `password`, `confirmPassword`, `address` with the validations above.
- Normalize both error shapes (ErrorResponse + gateway 401) into one client-side `ApiError` with field errors for forms.
- Local demo credentials (Docker only): admin `sunny.demo@voltaras.local`, consumer `vinay.demo@voltaras.local`, password `Voltaras@123`.
