# VOLTARAS Payment Service

## Purpose

The **Payment Service** implements the VOLTARAS **prepaid wallet** flow:

1. Users recharge their wallet with **UPI** or **CARD** through the
   **Razorpay payment gateway in sandbox/test mode** (`POST /api/recharges/orders`).
2. Razorpay confirms the payment via a **signature-protected webhook**
   (`POST /api/payments/webhooks/razorpay`), which credits the wallet on success.
3. Bills are paid **from the wallet balance** (`POST /api/bills/{billId}/payments`),
   which debits the wallet and notifies the **Bill Service** (`PAID`/`PARTIALLY_PAID`).

No card numbers, CVV values, UPI PINs or bank credentials are ever accepted or
stored. All monetary values use `BigDecimal` (scale 2); amounts are converted
to **paise** only at the Razorpay API boundary.

The service is responsible for:

- Creating Razorpay orders for wallet recharges (idempotent via `Idempotency-Key`)
- Verifying Razorpay webhook signatures (HMAC-SHA256 over the raw body) and
  crediting the wallet exactly once per successful payment
- Paying bills from the wallet with idempotency, sufficient-balance checks
  (`INSUFFICIENT_WALLET_BALANCE`) and Bill Service notification
- Enforcing payment status transitions (CREATED → PENDING/SUCCESS/FAILED/CANCELLED → REFUNDED)
- Authorizing every request through the identity headers injected by the API Gateway

## Port

- **Default port:** `8086`
- Configurable via `server.port` in `application.yml` or runtime argument `--server.port=<port>`.

## Architecture

```
API Gateway (8080) ── JWT validation, injects X-User-Id / X-User-Role, forwards Authorization
        │
        ▼
Payment Service (8086) ── MySQL payment_db
        │
        ├── AuthServiceClient (Feign)  ──► Auth Service (8081)
        │        └── GET /api/auth/internal/users/{userId} (verifies user
        │            exists, active, role/email; forwards the Bearer token)
        │
        ├── RazorpayGatewayClient (interface)
        │        └── RestRazorpayGatewayClient (sandbox mode, HMAC webhook verification)
        │
        ├── WalletService            (lazy wallet, pessimistic-lock credit/debit)
        ├── RechargeService          (Razorpay orders + webhook processing)
        ├── PaymentService           (wallet-funded bill payments)
        │
        ├── BillServiceClient  ──► Bill Service (8084)
        │        └── RestBillServiceClient (validates bills, notifies payment status)
        │
        └── OrganizationServiceClient ──► Organization Service (8085)
                 └── RestOrganizationServiceClient (validates org access)
```

Inter-service communication: the Auth Service is called through a
**Spring Cloud OpenFeign** client resolved via Eureka
(`AuthServiceClient`, name `auth-service`); Bill and Organization services are
called through small isolated HTTP clients. There are **no distributed
transactions**: a Bill Service notification failure rolls back the wallet
debit and the payment record.

## Database

- **Name:** `payment_db` (MySQL), created with `CREATE DATABASE IF NOT EXISTS payment_db;`
- Schema is managed with `spring.jpa.hibernate.ddl-auto=update` like the other services.
- Tables: `wallets`, `recharge_transactions`, `payments`.
- Unique constraints: wallet per user; recharge reference / order ID / idempotency
  key; payment reference / idempotency key. Indexes on bill/organization/user/status/created_at.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/recharges/orders` | Yes | Create a Razorpay recharge order (UPI/CARD, requires `Idempotency-Key`) |
| `GET`  | `/api/recharges/me` | Yes | List my recharge history |
| `GET`  | `/api/wallet/me` | Yes | Get my wallet balance |
| `POST` | `/api/bills/{billId}/payments` | Yes | Pay a bill from the wallet (requires `Idempotency-Key`) |
| `GET`  | `/api/payments/{paymentId}` | Yes | Get a payment by ID |
| `GET`  | `/api/payments/reference/{paymentReference}` | Yes | Get a payment by reference |
| `GET`  | `/api/bills/{billId}/payments` | Yes | List payments for a bill |
| `GET`  | `/api/payments` | Yes | List requester-accessible payments (paginated) |
| `POST` | `/api/payments/webhooks/razorpay` | Signature | Razorpay callback (X-Razorpay-Signature) |

## Security Rules

- Authentication is performed by the API Gateway (JWT). The Gateway injects
  `X-User-Id` and `X-User-Role` headers and forwards the `Authorization`
  header; this service never trusts client-supplied user/organization IDs
  without validation.
- Before recharge order creation, recharge history, wallet reads and bill
  payments, the user is verified with the **Auth Service** through the
  `AuthServiceClient` Feign client (`GET /api/auth/internal/users/{userId}`):
  missing user → `404 USER_NOT_FOUND`, inactive user → `403 USER_INACTIVE`,
  identity mismatch (ID/role/email vs. gateway headers or rejected token) →
  `401 UNAUTHORIZED_USER`. The auth database is never read directly.
- Recharges and bill payments require an **ACTIVE membership** in the
  organization (validated through Organization Service).
- A bill can be paid only by its owner (validated through Bill Service) and
  only while it is `UNPAID` or `PARTIALLY_PAID`.
- System ADMINs (`X-User-Role: ADMIN` or `ROLE_ADMIN`) may read any payment.
- The Razorpay webhook is public (no JWT) but protected by the
  `X-Razorpay-Signature` header, verified as HMAC-SHA256 over the raw body with
  `RAZORPAY_WEBHOOK_SECRET`.

## Payment Status Transitions

```
CREATED ──► PENDING ──► SUCCESS ──► REFUNDED
   │  │        │  │
   │  │        │  └────► FAILED
   │  │        └───────► CANCELLED
   │  └────────► SUCCESS / FAILED / CANCELLED
   └──────────► (webhook completes from CREATED too)

FAILED, CANCELLED, REFUNDED are terminal. A failed recharge can be retried by
creating a new order with a new idempotency key.
```

## Idempotency Behavior

- The `Idempotency-Key` header is **required** on recharge orders and bill payments.
- Replaying the same key with the same payload returns the **original result**
  (no duplicate order or payment; the wallet is never debited/credited twice).
- Reusing the key with a **different payload** returns `409 IDEMPOTENCY_CONFLICT`.
- Unique database constraints on `idempotency_key` protect against concurrent duplicates.
- Razorpay webhook callbacks are idempotent: replaying a captured/failed event
  is a no-op and never credits the wallet twice.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `payment_db` | MySQL database name |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | *(required)* | MySQL password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka server URL |
| `BILL_SERVICE_URL` | `http://localhost:8084` | Bill Service base URL |
| `ORGANIZATION_SERVICE_URL` | `http://localhost:8085` | Organization Service base URL |
| `RAZORPAY_BASE_URL` | `https://api.razorpay.com` | Razorpay API base URL (sandbox) |
| `RAZORPAY_KEY_ID` | *(required)* | Razorpay key ID (rzp_test_...) |
| `RAZORPAY_KEY_SECRET` | *(required)* | Razorpay key secret |
| `RAZORPAY_WEBHOOK_SECRET` | *(required)* | Razorpay webhook secret for signature verification |
| `JPA_DDL_AUTO` | `update` | Hibernate DDL mode |

## Run Command

```bash
# 1. Start MySQL and create the database
CREATE DATABASE IF NOT EXISTS payment_db;

# 2. Export Razorpay sandbox credentials
export RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxx
export RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxx
export RAZORPAY_WEBHOOK_SECRET=yyyyyyyyyyyyyyyy

# 3. Start Eureka (8761), Auth (8081), Bill (8084), Organization (8085), API Gateway (8080)

# 4. Start the service
cd payment-service
mvn spring-boot:run
```

## Swagger

- Direct: `http://localhost:8086/swagger-ui.html`
- API calls run through the Gateway routes `/api/payments/**`,
  `/api/bills/*/payments/**`, `/api/recharges/**`, `/api/wallet/**` and
  `/api/wallets/**`.

## Test Command

```bash
cd payment-service
mvn clean test
```

## Razorpay Sandbox Warning

This service talks to Razorpay in **sandbox/test mode only**. The
`RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` must be `rzp_test_...` test
credentials and `RAZORPAY_WEBHOOK_SECRET` must match the webhook secret
configured in the Razorpay dashboard test settings. Never point this service
at live credentials, and never store card numbers, CVVs or UPI PINs.
