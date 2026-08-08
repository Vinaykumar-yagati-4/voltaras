# 13. Payment Service

## Service Purpose

The **Payment Service** (`payment-service`, package `com.voltaras.paymentservice`)
implements the VOLTARAS **prepaid wallet** model:

1. Users **recharge** their wallet with **UPI** or **CARD** through the
   **Razorpay payment gateway in sandbox/test mode**.
2. Razorpay confirms the payment via a **signature-protected webhook**, which
   credits the wallet on success.
3. Users **pay electricity bills from the wallet balance**; the wallet is
   debited and the Bill Service is notified (`PAID`/`PARTIALLY_PAID`).

No card numbers, CVVs, UPI PINs or bank credentials are ever accepted or stored.

## Architecture

- **Port:** `8086`
- **Database:** `payment_db` (MySQL, database-per-service convention)
- **Eureka:** registers as `PAYMENT-SERVICE`
- **Gateway routes:** `/api/payments/**`, `/api/bills/*/payments/**`,
  `/api/recharges/**`, `/api/wallet/**`, `/api/wallets/**` (the
  bill-payment path is declared before the `/api/bills/**` bill-service route)

The service follows the layered structure used by all VOLTARAS services:
`config`, `controller`, `dto.request`, `dto.response`, `entity`, `enums`,
`exception`, `mapper`, `repository`, `security`, `service`, `service.impl`,
`client`, `client.impl`, `provider`, `provider.impl`, `util`.

Inter-service communication:

- `AuthServiceClient` → Auth Service (8081): **Spring Cloud OpenFeign** client
  resolved via Eureka (`@FeignClient(name = "auth-service")`). Calls
  `GET /api/auth/internal/users/{userId}` and forwards the original Bearer
  token (Feign request interceptor in `FeignConfig`). The Auth Service
  verifies the token and returns the profile (userId, email, fullName, role,
  active). This is the only way the Payment Service reads user data — the
  `auth_db.users` table is never accessed directly.
- `RazorpayGatewayClient` → Razorpay API (sandbox): `POST /v1/orders` with
  Basic auth; amounts in **paise**; webhook signature verification with
  HMAC-SHA256.
- `BillServiceClient` → Bill Service (8084): validates bill existence,
  ownership and payability (`GET /api/bills/me/{id}` or admin variant), and
  notifies the new payment status (`PATCH /api/bills/admin/{id}/payment-status`
  with `PAID` or `PARTIALLY_PAID` and the cumulative amount paid).
- `OrganizationServiceClient` → Organization Service (8085): authorizes
  organization access via the existing `GET /api/organizations/{id}` endpoint,
  which enforces ACTIVE membership (any role) or system ADMIN.

**No distributed transactions.** If the Bill Service notification fails after
the wallet debit, the exception aborts the transaction and both the debit and
the payment record are rolled back (the caller receives
`502 UPSTREAM_SERVICE_ERROR` and may retry with the same idempotency key).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/recharges/orders` | Create a Razorpay recharge order (`Idempotency-Key` required; UPI or CARD) |
| `GET` | `/api/recharges/me` | List my recharge history |
| `GET` | `/api/wallet/me` | Get my wallet balance (wallet created lazily) |
| `POST` | `/api/bills/{billId}/payments` | Pay a bill from the wallet (`Idempotency-Key` required) |
| `GET` | `/api/payments/{paymentId}` | Get payment by ID |
| `GET` | `/api/payments/reference/{paymentReference}` | Get payment by reference |
| `GET` | `/api/bills/{billId}/payments` | List payments for a bill |
| `GET` | `/api/payments` | List requester payments (paginated; ADMIN may filter by `organizationId`) |
| `POST` | `/api/payments/webhooks/razorpay` | Razorpay callback (public; protected by `X-Razorpay-Signature`) |

## Security Rules

- JWT authentication is performed by the API Gateway, which injects
  `X-User-Id` and `X-User-Role` and forwards the `Authorization` header.
  Business endpoints require these headers (`400 MISSING_HEADER` when absent).
- Before `POST /api/recharges/orders`, `GET /api/recharges/me`,
  `GET /api/wallet/me` and `POST /api/bills/{billId}/payments` the user is
  verified with the **Auth Service** via the `AuthServiceClient` Feign client:
  user does not exist → `404 USER_NOT_FOUND`; user deactivated →
  `403 USER_INACTIVE`; token rejected or user ID/role/email does not match the
  gateway headers → `401 UNAUTHORIZED_USER`. The auth database is never read
  directly.
- Recharges and bill payments additionally require an **ACTIVE membership**
  in the organization (validated via Organization Service).
- A bill can be paid only by its owner (validated via Bill Service) and only
  while it is `UNPAID` or `PARTIALLY_PAID`.
- Client-supplied organization IDs are always validated; never trusted as-is.
- The webhook endpoint is the only public API. Its HMAC-SHA256 signature
  (over the raw body) is verified with `RAZORPAY_WEBHOOK_SECRET`; invalid
  signatures return `403 FORBIDDEN_OPERATION`.
- System ADMIN (`X-User-Role: ADMIN` or `ROLE_ADMIN`) may read any payment.
- The payment read endpoints (`GET /api/payments/**`, `GET /api/bills/{id}/payments`)
  are not individually verified with the Auth Service — they are protected by the
  bill/ownership checks and the gateway JWT. Only the four user-facing endpoints
  listed above run the Auth Service verification.
- Edge case: a user deleted from `auth_db` whose JWT is still valid is rejected by
  the Auth Service with 401, which the Payment Service maps to
  `UNAUTHORIZED_USER` (not `USER_NOT_FOUND`).

## Status Transitions

| From | To |
|------|----|
| CREATED | PENDING, SUCCESS, FAILED, CANCELLED |
| PENDING | SUCCESS, FAILED, CANCELLED |
| SUCCESS | REFUNDED |
| FAILED / CANCELLED / REFUNDED | (terminal) |

Invalid transitions return `400 BAD_REQUEST` via `InvalidStateException`.
A failed recharge can be retried by creating a **new order** with a new
idempotency key.

## Idempotency Behavior

- `Idempotency-Key` header is required on recharge orders and bill payments;
  identical replays return the original result; different payloads with the
  same key return `409 IDEMPOTENCY_CONFLICT`.
- Unique constraints: wallet per user; recharge reference / Razorpay order ID /
  idempotency key; payment reference / idempotency key.
- Under a concurrent race with the same key, one request wins and the loser
  receives `409 DATA_CONSTRAINT_VIOLATION` from the unique constraint.
- Razorpay webhook callbacks are idempotent: replayed captured/failed events
  are no-ops and never credit the wallet twice.
- The wallet is credited only for `payment.captured` / `order.paid` events and
  only when the webhook amount matches the order amount.

## Wallet Concurrency

`Wallet` reads and updates inside credit/debit use a **pessimistic write lock**
(`SELECT ... FOR UPDATE`) so concurrent recharges and bill payments against the
same wallet are serialized. The wallet is created lazily on first access with a
zero balance. Debit below the balance raises
`400 INSUFFICIENT_WALLET_BALANCE`.

## Environment Variables

| Variable | Default |
|----------|---------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `payment_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / required |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` |
| `BILL_SERVICE_URL` | `http://localhost:8084` |
| `ORGANIZATION_SERVICE_URL` | `http://localhost:8085` |
| `RAZORPAY_BASE_URL` | `https://api.razorpay.com` |
| `RAZORPAY_KEY_ID` | required (`rzp_test_...`) |
| `RAZORPAY_KEY_SECRET` | required |
| `RAZORPAY_WEBHOOK_SECRET` | required |
| `JPA_DDL_AUTO` | `update` |

## Startup Commands

```bash
# Create the database once
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS payment_db;"

# Export Razorpay sandbox credentials (rzp_test_...)
export RAZORPAY_KEY_ID=rzp_test_xxxx
export RAZORPAY_KEY_SECRET=xxxx
export RAZORPAY_WEBHOOK_SECRET=yyyy

# Start Eureka, Auth, Bill, Organization, API Gateway, then:
cd payment-service
mvn spring-boot:run
```

## Swagger URL

- Direct: `http://localhost:8086/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8086/v3/api-docs`
- API calls run through the Gateway at `http://localhost:8080`.

## Test Command

```bash
cd payment-service
mvn clean test
```

## Razorpay Sandbox Warning

The Razorpay integration is **sandbox/test mode only**. Use `rzp_test_...`
credentials and a matching webhook secret from the Razorpay test dashboard.
Never use live credentials, never store card numbers, CVVs or UPI PINs, and
never treat `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` as config values
that can be committed.
