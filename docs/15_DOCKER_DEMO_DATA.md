# VOLTARAS — Docker Demo Data (30 Demo Users)

Local demo data for the VOLTARAS microservice stack running in Docker Compose,
used for **Swagger/OpenAPI and API Gateway verification** (project policy: API
verification is done through Swagger/OpenAPI, not Postman).

The seeding setup creates **30 demo users** (1 ADMIN + 29 CONSUMER) plus
sample organizations, meters, meter readings, bills, payments and complaints —
all through the public APIs (via the API Gateway) with **no real personal data
and no hard-coded secrets**.

---

## 1. What gets seeded

| # | Dataset | Service / DB | Count |
|---|---|---|---|
| 1 | Demo users (auth accounts) | auth-service / `auth_db` | 30 (1 ADMIN + 29 CONSUMER) |
| 2 | User profiles | user-service / `user_db` | 30 |
| 3 | Demo organization + memberships | organization-service / `organization_db` | 1 org, 30 memberships |
| 4 | Meters (assigned to consumers) | meter-management-service / `meter_management_db` | 30 |
| 5 | Meter readings (verified) | meter-reading-service / `meter_db` | 30 monthly + 7 daily per consumer (240) |
| 6 | Bills | bill-service / `bill_db` | 30 |
| 7 | Wallet top-ups + bill payments | payment-service / `payment_db` | 30 payments |
| 8 | Complaints | complaint-service / `complaint_db` | 10 |

The 10 sample complaints use realistic electricity-service subjects (one per
consumer, in creation order):

| Consumer | Subject |
|---|---|
| soumya | Incorrect billing amount |
| anil | Meter reading mismatch |
| vinay | Payment not reflected |
| pavan | Unexpected usage increase |
| tarun | Meter display issue |
| bharath | Bill due-date clarification |
| satya | Wallet balance discrepancy |
| srivalli | Service interruption report |
| rekha | Frequent voltage fluctuation |
| uday | Meter not reporting readings |

All 10 are stored with the realistic subject only — no internal
`[voltaras-demo]` prefix is added to complaint subjects.

The two pre-existing Docker test accounts are **kept untouched**:

- `docker.consumer.test@gmail.com` (CONSUMER)
- `docker.admin.test@gmail.com` (ADMIN)

---

## 2. Files

| File | Purpose |
|---|---|
| `docker/seed/seed-docker-demo-data.ps1` | Primary seeder — Windows PowerShell |
| `docker/seed/seed-docker-demo-data.sh` | Optional seeder — bash / Git Bash / WSL |
| `docs/15_DOCKER_DEMO_DATA.md` | This document |

Neither script changes `docker-compose.yml`, any service code, or your `.env`.

---

## 3. Usage

Prerequisites:

1. The stack is running: `docker compose up -d --build` (see
   `docs/14_DOCKER_DEPLOYMENT.md`).
2. The repo root `.env` exists and contains `DB_USERNAME` / `DB_PASSWORD`
   (used only for the admin-role promotion and verification counts).
3. Python is available (used for JSON parsing by the bash seeder).

Run from the repository root:

```powershell
# Windows PowerShell
powershell -ExecutionPolicy Bypass -File docker/seed/seed-docker-demo-data.ps1

# bash / Git Bash / WSL
bash docker/seed/seed-docker-demo-data.sh
```

The script automatically:

1. Checks Docker containers are running and waits for the API Gateway
   (`/actuator/health`) — seeding never starts before JPA has created the
   tables (rule: no SQL before tables exist).
2. Creates/logs in the 30 demo users.
3. Promotes `sunny` to ADMIN (direct SQL on `auth_db` — the Auth Service has
   no admin-role API; this is the only SQL the seeder writes).
4. Creates the 30 user profiles.
5. Creates the demo organization and approves the 29 consumer memberships.
6. Creates 30 meters and assigns them to the consumers.
7. Submits 30 monthly meter readings and verifies them as admin, then
   submits **7 daily readings per consumer** (the last 7 days ending on the
   backend's current date) and verifies them as admin — this powers the
   consumer **Daily electricity tracking** screens (`/consumer` and
   `/consumer/readings`).
8. Generates 30 bills (admin).
9. Tops up wallets (only when the balance cannot cover the bill) and pays the
   30 bills using the `Idempotency-Key` header.
10. Creates 10 sample complaints with realistic electricity-service subjects
    (see “Sample complaint subjects” below).
11. Verifies: logins, complaint APIs through the gateway, and DB counts.

Optional environment variables (bash) / parameters (PowerShell):

| Setting | Default | Description |
|---|---|---|
| `GATEWAY_URL` | `http://localhost:8080` | API Gateway base URL |
| `METER_MGMT_URL` | `http://localhost:8089` | meter-management-service (no gateway route) |
| `MYSQL_CONTAINER` | `voltaras-mysql` | MySQL container name |
| `ENV_FILE` / `-EnvFile` | repo root `.env` | environment file with DB credentials |
| `DEMO_READING_DATE` / `-DemoReadingDate` | `2026-07-15` | fixed past demo billing month |

---

## 4. Design rules honoured

1. **No real personal data** — clean demo names, `*.demo@voltaras.local`
   emails, deterministic demo phones and Hyderabad/Telangana demo addresses.
2. **No hard-coded secrets** — DB credentials are read from the gitignored
   `.env` (or environment variables). The common demo password is a demo
   constant, documented here on purpose.
3. **Does not break Docker Compose** — no compose/config/code changes.
4. **Local/demo only** — never point `GATEWAY_URL` at a real environment.
5. **Idempotent** — running twice never duplicates users or data:
   - users: login-first (or 409 on register) → reuses existing accounts;
   - profiles: 409 on second create → skipped;
   - organization: looked up by unique `organization_code`;
   - memberships: pending join requests are approved, existing members are
     never re-added;
   - meters/readings/bills: unique keys (`meter_number`, meter+date, and
     consumer+meter+month+year) → 409 on re-run → existing rows reused;
     daily readings skip days that already exist and anchor the chain on
     the stored meter value, so a re-run on a later day continues from
     real data instead of duplicating or inventing readings;
   - payments: replayed safely via the `Idempotency-Key` header;
   - complaints: a consumer who already has a complaint is skipped (fixed
     10-consumer subset, one realistic subject per consumer).
6. **API-first** — seeding is done through the API Gateway wherever a route
   exists. Direct SQL is used only for the ADMIN role promotion and for the
   final verification counts.
7. **Existing test accounts are preserved** — the seeder only touches
   `*.demo@voltaras.local` users and `[voltaras-demo]` tagged data
   (complaint subjects are stored without the tag prefix).

---

## 5. Demo credentials

Common password for all demo users: **`Voltaras@123`**

| Role | Email |
|---|---|
| **ADMIN** | **`sunny.demo@voltaras.local`** |
| CONSUMER | `soumya.demo@voltaras.local` |
| CONSUMER | `anil.demo@voltaras.local` |
| CONSUMER | **`vinay.demo@voltaras.local`** |
| CONSUMER | `pavan.demo@voltaras.local` |
| CONSUMER | `tarun.demo@voltaras.local` |
| CONSUMER | `bharath.demo@voltaras.local` |
| CONSUMER | `satya.demo@voltaras.local` |
| CONSUMER | `srivalli.demo@voltaras.local` |
| CONSUMER | `rekha.demo@voltaras.local` |
| CONSUMER | `uday.demo@voltaras.local` |

All 30 demo emails follow `<name>.demo@voltaras.local`:

`soumya, anil, vinay, pavan, tarun, bharath, satya, srivalli, rekha, sunny`
(ADMIN), `uday, sunil, jash, nagesh, swaraj, kavya, rahul, sneha, kiran,
deepak, lavanya, rohit, meena, akhil, divya, manoj, priya, charan, harika,
naveen`

---

## 6. Verified counts (fresh seed run on this repository's stack)

| Dataset | Count |
|---|---|
| Demo users (`*.demo@voltaras.local`) | 30 |
| — ADMIN | 1 (`sunny`) |
| — CONSUMER | 29 |
| User profiles | 30 |
| Organizations (`VOLTARAS_DEMO`) | 1 |
| Organization memberships | 30 |
| Meters (`MTR-DEMO-*`) | 30 |
| Meter readings (verified) | 240 (30 monthly + 7 daily per consumer) |
| Bills | 30 |
| Payments | 30 |
| Complaints | 10 |

Re-running the seeder produces the **same counts** (verified).

---

## 7. Verification through the API Gateway

```bash
# 1. Login as admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"sunny.demo@voltaras.local","password":"Voltaras@123"}' \
  | python -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

# 2. Login as consumer
VTOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"vinay.demo@voltaras.local","password":"Voltaras@123"}' \
  | python -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

# 3. Consumer complaints
curl -H "Authorization: Bearer $VTOKEN" http://localhost:8080/api/complaints

# 4. Admin complaints
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/complaints

# 5. Database counts (container shell, app DB user)
docker exec -e MYSQL_PWD=<DB_PASSWORD> voltaras-mysql mysql -h127.0.0.1 -uroot \
  auth_db -e "SELECT COUNT(*) FROM users WHERE email LIKE '%.demo@voltaras.local';"
```

Swagger UIs (direct per service): `http://localhost:8081..8089/swagger-ui.html`
(auth 8081, user 8082, meter-reading 8083, bill 8084, organization 8085,
payment 8086, complaint 8087, notification 8088, meter-management 8089).

---

## 8. APIs used and skipped

**Used (through the API Gateway):**

| API | Purpose |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | user creation / login |
| `POST /api/users/profile` | user profiles |
| `POST /api/organizations`, `POST .../join-requests`, `PATCH .../approve` | org + memberships |
| `POST /api/meter-readings`, `PATCH /api/meter-readings/admin/{id}/verify` | monthly + daily readings + verification |
| `GET /api/meter-readings/me/daily-usage` | backend date lookup for the daily readings window |
| `POST /api/bills/admin` | bill generation |
| `POST /api/wallet/top-up`, `POST /api/bills/{billId}/payments` | wallets + payments |
| `POST /api/complaints`, `GET /api/complaints/categories` | complaints |

**Used (direct service call — no gateway route exists yet):**

| API | Why |
|---|---|
| `POST /api/meters/admin`, `PATCH /api/meters/admin/{id}/assign` (meter-management-service :8089) | The API Gateway has **no route for `/api/meters/**`**, so the seeder calls the service directly with `X-User-Id` / `X-User-Role: ADMIN` headers (the service trusts these headers, injected by the gateway in normal operation). Add an `api-gateway` route for `/api/meters/**` to seed through the gateway instead. |

**Skipped:**

| API | Why |
|---|---|
| `POST /api/recharges/orders` (Razorpay) | Requires a real Razorpay checkout/order flow; the demo uses the local `POST /api/wallet/top-up` test endpoint instead. |
| `POST /api/admin/notifications` | Notifications are already generated automatically by the complaint events (RabbitMQ) — no manual seeding needed. |
| Dashboard/analytics APIs | No dedicated Dashboard service exists in the stack. |
