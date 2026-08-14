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
| GET | `/api/complaints` | own complaints, Spring `Page` (`content[]`, `totalElements`, `totalPages`, `number`, `size`); filters `status`, `priority`, `categoryId`; default sort `createdAt DESC` |
| POST | `/api/complaints` | `CreateComplaintRequest` — `categoryId` (required, active), `subject` (required, 10-200 chars), `description` (required, 20-5000 chars); 201 + `ComplaintDetailResponse` |
| GET | `/api/complaints/{id}` | own complaint detail — `ComplaintDetailResponse` includes `comments[]` (`CommentResponse`) and `statusHistory[]` (`{fromStatus, toStatus, changedBy, changedAt}`) |
| GET | `/api/complaints/ticket/{ticketNumber}` | lookup by ticket `CMP-YYYYMMDD-NNNN` |
| PUT | `/api/complaints/{id}` | edit `subject` + `description` while OPEN (`UpdateComplaintRequest`) |
| POST | `/api/complaints/{id}/comments` | `AddComplaintCommentRequest` — `commentText` (1-1000 chars); 201; not allowed on CLOSED |
| GET | `/api/complaints/categories` | active categories list (`CategoryResponse`: `id`, `name`, `description`) for the form dropdown |
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

## 5. Consumer portal services (verified live, August 2026)

### Bills (bill-service)
| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/bills/me` | `BillSummaryResponse[]` | **plain array, NOT a Spring Page** — client-side pagination/filtering required |
| GET | `/api/bills/me/{billId}` | `BillResponse` | full detail incl. itemized `energyCharge`, `fixedCharge`, `taxAmount`, `lateFee`, `totalAmount`, `amountPaid`, `outstandingAmount` |
| GET | `/api/bills/me/outstanding` | `BillSummaryResponse[]` | still-payable bills (UNPAID / PARTIALLY_PAID / FAILED, not CANCELLED) |
| GET | `/api/bills/me/filter` | `BillSummaryResponse[]` | `month` + `year` params only |

`BillResponse` includes `remarks` (may contain internal seed markers — the UI never renders them)
and `paidAt`. Bill lifecycle statuses: `GENERATED|PENDING|PAID|OVERDUE|CANCELLED`.

### Wallet + payments + recharges (payment-service)
| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/wallet/me` | `WalletResponse` | `balance`, `currency: INR` |
| GET | `/api/payments` | Spring `Page<PaymentResponse>` | server-paginated (`page`, `size`), sorted `createdAt DESC` |
| GET | `/api/payments/{paymentId}` | `PaymentResponse` | own payment only |
| GET | `/api/recharges/me` | `RechargeTransactionResponse[]` | **plain array** |
| POST | `/api/bills/{billId}/payments` | `PaymentResponse` | wallet-funded bill payment; requires `Idempotency-Key` header + `PayBillRequest {amount, currency: INR, organizationId}`; `400 INSUFFICIENT_WALLET_BALANCE` possible |
| POST | `/api/recharges/orders` | `RechargeOrderResponse` | **NOT wired in the Phase 2 UI** — returns a Razorpay `orderId` + `razorpayKeyId` for a checkout integration; the portal displays recharge history only (no checkout, no keys handled in the browser) |
| POST | `/api/wallet/top-up` | `WalletResponse` | local test-only endpoint — **not exposed in the UI** |

`PaymentResponse` includes `paymentReference`, `transactionType` (`RECHARGE|BILL_PAYMENT|REFUND`),
`paymentMethod` (`UPI|CARD|WALLET`), `status` (`CREATED|PENDING|SUCCESS|FAILED|CANCELLED|REFUNDED`),
and `idempotencyKey` (internal marker — the UI never renders it).

### Meter readings (meter-reading-service)
| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/meter-readings/me` | `MeterReadingResponse[]` | **plain array** of the consumer's readings; `status` `SUBMITTED|VERIFIED|REJECTED` |
| POST | `/api/meter-readings` | `MeterReadingResponse` | submit a reading (not part of the Phase 2 portal screens) |

`remarks` may contain internal seed markers — the readings UI never renders them.

### Notifications (notification-service)
| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/notifications` | `NotificationResponse[]` | **plain array**, newest first |
| GET | `/api/notifications/unread` | `NotificationResponse[]` | unread only |
| GET | `/api/notifications/count/unread` | `UnreadNotificationCountResponse` | `{authUserId, unreadCount}` |
| PATCH | `/api/notifications/{id}/read` | `NotificationResponse` | own notification only |
| PATCH | `/api/notifications/read-all` | `204 No Content` | marks every unread notification READ |

`NotificationResponse` includes `type` (`BILL_GENERATED|PAYMENT_SUCCESS|RECHARGE_SUCCESS|
COMPLAINT_STATUS_UPDATED|MANUAL`), `channel`, `status` (`UNREAD|READ|FAILED`).

### Organizations (organization-service, consumer view)
| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/organizations/me` | `OrganizationMembership[]` | **plain array** of the user's memberships (`organizationId`, `organizationName`, `membershipRole`, `membershipStatus`) |

Used to resolve `organizationId` for the wallet bill-payment flow.

## 6. BLOCKERS (Gateway gaps / deferred integrations — do not bypass)

1. **`/api/meters/**` (meter-management-service) has no Gateway route.**
   The service exists on port 8089 with `GET /api/meters`, `GET /api/meters/{id}`, and
   `/api/meters/admin/**` CRUD, but the Gateway's route table has no entry for it, so
   browser calls to `http://localhost:8080/api/meters/**` fail (no matching route).
   The Phase 2 portal therefore has **no `/consumer/meters` screen**; consumption is shown
   from the routed `/api/meter-readings/**` data. Adding the meter screen requires adding
   the Gateway route first.
2. **Wallet recharge checkout (Razorpay) is deferred.**
   `POST /api/recharges/orders` is a real endpoint, but completing a recharge requires
   the Razorpay Checkout integration (`orderId` + public `razorpayKeyId` from the response).
   Phase 2 shows recharge history only; no checkout SDK or keys are loaded in the browser.
   A future phase should add the sandbox checkout and keep keys out of the bundle.

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
