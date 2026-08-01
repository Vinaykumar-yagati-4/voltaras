# VOLTARAS — Meter Reading Service · API Testing Guide (Postman)

> **Gateway base URL:** `http://localhost:8080` — **all normal tests use the Gateway.**
> The direct-service URL `http://localhost:8083` is used only for the
> clearly-labelled direct debugging test at the end.

---

## 0. Before You Start

1. Start the stack in order: **Eureka (8761)** → **Auth (8081)** →
   **User (8082)** → **Meter Reading (8083)** → **API Gateway (8080)**.
2. Make sure `meter_db` exists in MySQL and `DB_PASSWORD` is exported.
3. Import the collection:
   `docs/postman/voltaras-meter-reading.postman_collection.json`
   (Postman → **Import** → select the file).

### Authentication flow

1. Login first with `POST http://localhost:8080/api/auth/login` (this
   route is public — no JWT needed).
2. The collection's login test script automatically stores the token in
   the `accessToken` variable.
3. All subsequent requests use `Authorization: Bearer {{accessToken}}`.
4. **Never** add `X-User-Id`, `X-User-Email` or `X-User-Role` manually —
   the API Gateway validates the JWT and injects these headers itself.

### Roles

| Role | Purpose |
|---|---|
| `CONSUMER` | Submits and manages own readings |
| `ADMIN` | Verifies / rejects any reading, sees everything |

Login with a **consumer** account for the consumer tests and an
**admin** account for the admin tests. The JWT `role` claim decides
which headers the Gateway forwards. Collection variables:
`accessToken` (consumer) and `adminAccessToken` (admin).

---

## 1. Consumer Endpoints

### 1.1 Submit a reading — `POST /api/meter-readings`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings` |
| **Authorization** | Bearer token (consumer JWT) |
| **Headers** | `Content-Type: application/json` |
| **Body (JSON)** | See sample below |
| **Expected status** | `201 Created` |
| **Expected response** | The created reading with `id`, `status: "SUBMITTED"`, and `unitsConsumed` computed |

```json
{
  "meterNumber": "MTR-1001",
  "billingMonth": 7,
  "billingYear": 2026,
  "previousReading": 1250.500,
  "currentReading": 1425.750,
  "readingDate": "2026-07-31"
}
```

**Response:**

```json
{
  "id": 1,
  "authUserId": 2,
  "meterNumber": "MTR-1001",
  "billingMonth": 7,
  "billingYear": 2026,
  "previousReading": 1250.500,
  "currentReading": 1425.750,
  "unitsConsumed": 175.250,
  "readingDate": "2026-07-31",
  "status": "SUBMITTED",
  "createdAt": "2026-07-31T10:15:30",
  "updatedAt": "2026-07-31T10:15:30"
}
```

> The collection test script captures `jsonData.id` into the
> `readingId` variable automatically for the next requests.

**Failure cases:**
- Missing/invalid/expired JWT → `401` (blocked at the Gateway).
- `billingMonth: 0` or `13` → `400` `VALIDATION_ERROR`.
- `previousReading: -5` → `400` `VALIDATION_ERROR`.
- `currentReading < previousReading` → `400` `BAD_REQUEST`.
- Same meter + month + year again → `409` `DUPLICATE_RESOURCE`.

### 1.2 Get my readings — `GET /api/meter-readings/me`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/me` |
| **Authorization** | Bearer token (consumer JWT) |
| **Expected status** | `200 OK` |
| **Expected response** | JSON array of the caller's own readings, newest billing period first |

### 1.3 Get one of my readings — `GET /api/meter-readings/me/{{readingId}}`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/me/{{readingId}}` |
| **Authorization** | Bearer token (consumer JWT) |
| **Expected status** | `200 OK` |
| **Expected response** | The single reading object |

**Failure case:** another consumer's reading id → `404` `RESOURCE_NOT_FOUND`
(never reveals that the reading exists).

### 1.4 Update my submitted reading — `PUT /api/meter-readings/me/{{readingId}}`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/me/{{readingId}}` |
| **Authorization** | Bearer token (consumer JWT) |
| **Headers** | `Content-Type: application/json` |
| **Expected status** | `200 OK` |

```json
{
  "previousReading": 1425.750,
  "currentReading": 1600.000,
  "readingDate": "2026-08-01"
}
```

`unitsConsumed` is recalculated (`174.250`).

**Failure cases:**
- Reading is `VERIFIED`/`REJECTED` → `403` `FORBIDDEN_OPERATION`.
- `currentReading < previousReading` → `400` `BAD_REQUEST`.

### 1.5 Delete my submitted reading — `DELETE /api/meter-readings/me/{{readingId}}`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/me/{{readingId}}` |
| **Authorization** | Bearer token (consumer JWT) |
| **Expected status** | `200 OK` |
| **Expected response** | `{ "message": "Meter reading deleted successfully" }` |

**Failure cases:**
- Reading is `VERIFIED`/`REJECTED` → `403` `FORBIDDEN_OPERATION`.
- Unknown id → `404` `RESOURCE_NOT_FOUND`.

---

## 2. Admin Endpoints

### 2.1 Get all readings — `GET /api/meter-readings/admin`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/admin` |
| **Authorization** | Bearer token (**ADMIN** JWT) |
| **Expected status** | `200 OK` |

Optional query filter: `?status=SUBMITTED` / `VERIFIED` / `REJECTED`.

**Failure cases:**
- Consumer JWT → `403` `FORBIDDEN_OPERATION`.
- `?status=NOT_A_STATUS` → `400` `INVALID_ARGUMENT`.

### 2.2 Verify a reading — `PATCH /api/meter-readings/admin/{{readingId}}/verify`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/admin/{{readingId}}/verify` |
| **Authorization** | Bearer token (**ADMIN** JWT) |
| **Expected status** | `200 OK` |
| **Expected response** | `status: "VERIFIED"`, `verifiedBy` set to the admin's id, `verifiedAt` set |

**Failure cases:** consumer JWT → `403`; unknown id → `404`.

### 2.3 Reject a reading — `PATCH /api/meter-readings/admin/{{readingId}}/reject`

| Item | Value |
|---|---|
| **URL** | `http://localhost:8080/api/meter-readings/admin/{{readingId}}/reject` |
| **Authorization** | Bearer token (**ADMIN** JWT) |
| **Headers** | `Content-Type: application/json` |
| **Expected status** | `200 OK` |

```json
{
  "remarks": "Meter reading exceeds expected consumption threshold"
}
```

**Response:** `status: "REJECTED"`, `remarks`, `verifiedBy`, `verifiedAt`.

**Failure cases:**
- Blank/missing `remarks` → `400` `VALIDATION_ERROR`.
- Consumer JWT → `403`; unknown id → `404`.

---

## 3. Master Test-Case List (22)

| # | Test | Method & URL | Expected |
|---|---|---|---|
| 1 | Valid consumer submission | `POST /api/meter-readings` | `201` + id + `SUBMITTED` |
| 2 | Missing JWT | `POST /api/meter-readings` (no header) | `401` |
| 3 | Invalid JWT | `POST /api/meter-readings` (`Bearer invalid-token`) | `401` |
| 4 | Expired JWT | `POST /api/meter-readings` (tampered/expired token) | `401` |
| 5 | Missing required fields | `POST /api/meter-readings` (`{}`) | `400` + field details |
| 6 | Invalid billing month | `billingMonth: 13` | `400` `VALIDATION_ERROR` |
| 7 | Negative meter reading | `previousReading: -5` | `400` `VALIDATION_ERROR` |
| 8 | current < previous | `previousReading: 2000, currentReading: 1000` | `400` `BAD_REQUEST` |
| 9 | Duplicate reading | resubmit test #1 body | `409` `DUPLICATE_RESOURCE` |
| 10 | Get own readings | `GET /api/meter-readings/me` | `200` array |
| 11 | Get reading by valid ID | `GET /api/meter-readings/me/{{readingId}}` | `200` |
| 12 | Get another user's reading | `GET /api/meter-readings/me/{foreignId}` | `404` |
| 13 | Update submitted reading | `PUT /api/meter-readings/me/{{readingId}}` | `200` + recalculated units |
| 14 | Update verified reading | `PUT` on an admin-verified id | `403` |
| 15 | Delete submitted reading | `DELETE /api/meter-readings/me/{{readingId}}` | `200` |
| 16 | Delete verified reading | `DELETE` on an admin-verified id | `403` |
| 17 | Admin gets all readings | `GET /api/meter-readings/admin` | `200` |
| 18 | Admin filters by status | `GET ...?status=VERIFIED` | `200` filtered |
| 19 | Admin verifies reading | `PATCH .../{{readingId}}/verify` | `200` `VERIFIED` |
| 20 | Admin rejects reading | `PATCH .../{{readingId}}/reject` + remarks | `200` `REJECTED` |
| 21 | Consumer attempts admin endpoint | `GET /api/meter-readings/admin` (consumer JWT) | `403` |
| 22 | Resource not found | `GET /api/meter-readings/me/999999` | `404` |

> To execute #14/#16 you need a **verified** reading: run #1 with a
> consumer token, then #19 with the admin token on the same `readingId`,
> then run the negative `PUT`/`DELETE` with the consumer token.

---

## 4. Direct-Service Debugging Test (Port 8083 — Debug Only)

Use this **only** when the Gateway path is not available and you want to
debug the service in isolation. Because the Gateway is bypassed, you must
**manually provide the trusted headers** exactly as the Gateway would:

| Item | Value |
|---|---|
| **URL** | `http://localhost:8083/api/meter-readings` |
| **Headers** | `X-User-Id: 2` · `X-User-Role: CONSUMER` · `Content-Type: application/json` |
| **Expected status** | `201 Created` |

```json
{
  "meterNumber": "MTR-DEBUG-001",
  "billingMonth": 8,
  "billingYear": 2026,
  "previousReading": 1600.000,
  "currentReading": 1700.000,
  "readingDate": "2026-08-05"
}
```

> ⚠️ This bypasses JWT validation entirely — use it only for local
> debugging, never in production. All normal tests must go through
> `http://localhost:8080`.

---

## 5. Error Response Shape

All errors use the standard VOLTARAS envelope (same as auth/user services):

```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_RESOURCE",
    "message": "MeterReading already exists with meterNumber, billingMonth, billingYear: 'MTR-1001'"
  },
  "timestamp": "2026-07-31T10:20:00",
  "path": "/api/meter-readings"
}
```

Validation failures additionally include field-level details:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for the request",
    "details": [
      { "field": "billingMonth", "message": "Billing month must be between 1 and 12" },
      { "field": "meterNumber", "message": "Meter number is required" }
    ]
  },
  "timestamp": "2026-07-31T10:20:00",
  "path": "/api/meter-readings"
}
```

Gateway-level failures (`401`) come straight from the Gateway:

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired access token"
}
```
