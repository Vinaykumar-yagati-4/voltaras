# VOLTARAS — Organization Service · API Test Plan (Postman)

> **Service:** `organization-service` · **Port:** `8085` · **Database:** `organization_db` · **Base package:** `com.voltaras.organizationservice`
> **Gateway base URL:** `http://localhost:8080` — **all normal tests use the Gateway.**
> The direct-service URL `http://localhost:8085` is used only for the clearly-labelled
> direct debugging tests at the end.
> **Document:** `docs/12_ORGANIZATION_SERVICE_API_TEST_PLAN.md`

---

## 0. Before You Start

1. Start the stack in order: **Eureka (8761)** → **Auth (8081)** → **User (8082)** →
   **Meter Reading (8083)** → **Organization (8085)** → **API Gateway (8080)**.
2. Make sure `organization_db` exists in MySQL (or let Hibernate `ddl-auto=update`
   create it) and `DB_USERNAME` / `DB_PASSWORD` are exported.
3. Create the Postman collection from the endpoints in this document
   (Postman → **Import** → file, or build requests manually).

### Gateway route (implementation note — docs only, gateway is NOT modified here)

At implementation time, the API Gateway needs one new route covering both path
prefixes used by this service (member APIs and system-admin APIs):

```yaml
- id: organization-service
  uri: lb://ORGANIZATION-SERVICE
  predicates:
    - Path=/api/organizations/**, /api/admin/organizations/**
```

The Gateway's `JwtAuthenticationFilter` already validates the JWT and injects
`X-User-Id`, `X-User-Email`, `X-User-Role` — no gateway code changes required.

### Authentication flow

1. Login first with `POST http://localhost:8080/api/auth/login` (public route).
2. Store the token in the `accessToken` variable (consumer) and `adminAccessToken`
   variable (admin).
3. All subsequent requests use `Authorization: Bearer {{accessToken}}`.
4. **Never** add `X-User-Id` / `X-User-Email` / `X-User-Role` manually — the Gateway
   validates the JWT and injects these headers itself.

### Test accounts / roles

| Account | Platform role | Organization role (after setup) |
|---|---|---|
| `consumer-a` | `CONSUMER` | OWNER of "Sunrise Hostel" |
| `consumer-b` | `CONSUMER` | MEMBER / TENANT (via approved join request) |
| `consumer-c` | `CONSUMER` | Non-member (for authorization negatives) |
| `system-admin` | `ADMIN` | — (system admin) |

---

## 1. Positive Tests

### 1.1 Create organization — `POST /api/organizations`

| Item | Value |
|---|---|
| URL | `http://localhost:8080/api/organizations` |
| Auth | Bearer token (consumer) |
| Headers | `Content-Type: application/json` |
| Expected | `201 Created` |

```json
{
  "name": "Sunrise Hostel",
  "organizationCode": "SUNRISE-HST",
  "organizationType": "HOSTEL",
  "description": "Student hostel near campus",
  "email": "sunrise@example.com",
  "phone": "9876543210",
  "addressLine1": "12 College Road",
  "city": "Pune",
  "state": "Maharashtra",
  "country": "India",
  "postalCode": "411001"
}
```

**Expected response:** organization object with `id`, `status: "ACTIVE"`,
`createdByAuthUserId` = caller's id. Capture `jsonData.id` into `orgId`.

### 1.2 Get my organizations — `GET /api/organizations/me`

Expected `200 OK` with an array containing the caller's organizations and their
`membershipRole: "OWNER"`, `membershipStatus: "ACTIVE"`.

### 1.3 View organization — `GET /api/organizations/{{orgId}}`

Expected `200 OK` with organization details.

### 1.4 Update organization — `PUT /api/organizations/{{orgId}}`

```json
{ "name": "Sunrise Hostel (Renovated)", "description": "Renovated 2026" }
```

Expected `200 OK` with updated fields.

### 1.5 Create building — `POST /api/organizations/{{orgId}}/buildings`

```json
{ "name": "Main Hostel Building", "code": "MAIN", "address": "12 College Road, Pune" }
```

Expected `201 Created`. Capture `jsonData.id` into `buildingId`.

### 1.6 Create block — `POST /api/buildings/{{buildingId}}/blocks`

```json
{ "name": "Block A", "code": "A" }
```

Expected `201 Created`. Capture `jsonData.id` into `blockId`.

### 1.7 Create floor — `POST /api/blocks/{{blockId}}/floors`

```json
{ "floorNumber": 1, "name": "First Floor" }
```

Expected `201 Created`. Capture `jsonData.id` into `floorId`.

### 1.8 Create unit — `POST /api/floors/{{floorId}}/units`

```json
{ "unitNumber": "101", "unitName": "Deluxe Room", "unitType": "ROOM", "capacity": 2 }
```

Expected `201 Created`, `status: "AVAILABLE"`. Capture `jsonData.id` into `unitId`.

### 1.9 List structure — `GET /api/organizations/{{orgId}}/buildings` (and blocks/floors/units lists)

Expected `200 OK` arrays containing the created records.

### 1.10 Update unit status — `PATCH /api/units/{{unitId}}/status`

```json
{ "status": "OCCUPIED" }
```

Expected `200 OK` with `status: "OCCUPIED"`.

### 1.11 Join-request workflow (happy path)

1. `consumer-b` → `POST /api/organizations/{{orgId}}/join-requests`:

```json
{ "requestedRole": "TENANT", "requestMessage": "I would like to join the hostel" }
```

Expected `201 Created`, `status: "PENDING"`. Capture `jsonData.id` into `joinRequestId`.

2. `consumer-a` (OWNER) → `GET /api/organizations/{{orgId}}/join-requests` — `200` with the PENDING request.
3. `consumer-a` → `PATCH /api/organizations/{{orgId}}/join-requests/{{joinRequestId}}/approve` — `200 OK`, request `status: "APPROVED"`.
4. `consumer-b` → `GET /api/organizations/me` — shows the new membership with `membershipRole: "TENANT"`, `membershipStatus: "ACTIVE"`.

### 1.12 Deactivate organization — `PATCH /api/organizations/{{orgId}}/deactivate`

Expected `200 OK`, `status: "INACTIVE"`.

### 1.13 System admin — `PATCH /api/admin/organizations/{{orgId}}/activate`

Expected `200 OK`, `status: "ACTIVE"` (back from `INACTIVE`).

---

## 2. Negative Tests

| # | Test | Expected |
|---|---|---|
| N1 | Create org with blank `name` | `400 VALIDATION_ERROR` with field `name` |
| N2 | Create org with `organizationCode: "ab"` (too short) | `400 VALIDATION_ERROR` |
| N3 | Create org with `organizationType: "INDIVIDUAL"` | `400 VALIDATION_ERROR` (unknown enum) |
| N4 | Reject join request with blank `rejectionRemarks` | `400 VALIDATION_ERROR` |
| N5 | Approve a request that is already `REJECTED` | `400 BAD_REQUEST` |
| N6 | Create unit with `capacity: -1` | `400 VALIDATION_ERROR` |
| N7 | Malformed JSON body (`{ name: }`) | `400 MALFORMED_REQUEST` |
| N8 | Non-numeric id (`GET /api/organizations/abc`) | `400 INVALID_ARGUMENT` |
| N9 | Delete a building that has blocks | `400 BAD_REQUEST` (hierarchy) |
| N10 | Create a block under a building of another organization | `400 BAD_REQUEST` or `403 FORBIDDEN_OPERATION` |

## 3. Authentication-Header Tests

| # | Test | Expected |
|---|---|---|
| H1 | No `Authorization` header | `401` from Gateway (`{status, error: "UNAUTHORIZED", message}`) |
| H2 | `Authorization: Bearer invalid-token` | `401` from Gateway |
| H3 | Expired/tampered token | `401` from Gateway |
| H4 | Direct service call (8085) without `X-User-Id` | `400 MISSING_HEADER` |
| H5 | Direct service call (8085) without `X-User-Role` | `400 MISSING_HEADER` |
| H6 | Direct service call (8085) with fake `X-User-Id` / `X-User-Role` headers | Service trusts them only when the Gateway is bypassed — see §13 debug note |
| H7 | Via Gateway with client-supplied `X-User-Id: 999` | Gateway strips it; service sees the real id from the JWT |

## 4. Authorization Tests

| # | Test | Expected |
|---|---|---|
| A1 | `consumer-c` (non-member) views `GET /api/organizations/{{orgId}}` | `403 FORBIDDEN_OPERATION` |
| A2 | `consumer-b` (MEMBER/TENANT) calls `PUT /api/organizations/{{orgId}}` | `403 FORBIDDEN_OPERATION` |
| A3 | `consumer-b` approves a join request | `403 FORBIDDEN_OPERATION` |
| A4 | `consumer-b` creates a building | `403 FORBIDDEN_OPERATION` |
| A5 | `consumer-b` suspends a member | `403 FORBIDDEN_OPERATION` |
| A6 | OWNER assigns `ORGANIZATION_ADMIN` to `consumer-b` | `200 OK` |
| A7 | That `ORGANIZATION_ADMIN` (`consumer-b`) tries to assign `ORGANIZATION_ADMIN` to `consumer-c` | `400 BAD_REQUEST` (only OWNER may) |
| A8 | `MANAGER` updates an organization | `403 FORBIDDEN_OPERATION` |
| A9 | `MANAGER` manages a building | `200 OK` (structure allowed) |
| A10 | `consumer-c` calls any `/api/admin/organizations/**` endpoint | `403 FORBIDDEN_OPERATION` |
| A11 | Suspended member accesses structure lists | `403 FORBIDDEN_OPERATION` (treated as non-member) |

## 5. Ownership Tests

| # | Test | Expected |
|---|---|---|
| O1 | `consumer-b` views `GET /api/buildings/{{buildingId}}` of org they belong to | `200 OK` |
| O2 | `consumer-c` views `GET /api/buildings/{{buildingId}}` | `403 FORBIDDEN_OPERATION` |
| O3 | `consumer-a` updates a building of their org | `200 OK` |
| O4 | `consumer-c` updates a building of another org | `403 FORBIDDEN_OPERATION` |
| O5 | Member of org A views a unit of org B | `403 FORBIDDEN_OPERATION` |
| O6 | Caller requests `GET /api/organizations/{{foreignOrgId}}` where they are not a member | `403 FORBIDDEN_OPERATION` (existence not revealed) |

## 6. Duplicate Tests

| # | Test | Expected |
|---|---|---|
| D1 | Create second org with code `SUNRISE-HST` | `409 DUPLICATE_RESOURCE` |
| D2 | Create a second building with code `MAIN` in the same org | `409 DUPLICATE_RESOURCE` |
| D3 | Create a block with code `A` twice in the same building | `409 DUPLICATE_RESOURCE` |
| D4 | Create floor `1` twice in the same block | `409 DUPLICATE_RESOURCE` |
| D5 | Create unit `101` twice on the same floor | `409 DUPLICATE_RESOURCE` |
| D6 | Same code in a **different** organization/building/floor | `201 Created` (uniqueness is per-parent) |
| D7 | `consumer-b` submits a second join request while the first is PENDING | `409 DUPLICATE_RESOURCE` |
| D8 | OWNER tries to create a join request for their own org (already ACTIVE member) | `409 DUPLICATE_RESOURCE` |
| D9 | Approve a request whose user already has an ACTIVE membership | `409 DUPLICATE_RESOURCE` (membership duplicate) |

## 7. Join-Request Workflow Tests

| # | Test | Expected |
|---|---|---|
| J1 | Non-member creates request | `201`, `PENDING` |
| J2 | List requests filtered `?status=PENDING` | `200` filtered list |
| J3 | OWNER approves | `200`, request `APPROVED`, membership `ACTIVE` with requested role |
| J4 | OWNER rejects with remarks | `200`, request `REJECTED`, `reviewedByAuthUserId` + `reviewedAt` set |
| J5 | OWNER rejects without remarks | `400 VALIDATION_ERROR` |
| J6 | Requester cancels own PENDING request | `200`, request `CANCELLED` |
| J7 | Cancelled/rejected user submits a new request | `201`, new `PENDING` (re-request allowed) |
| J8 | OWNER / ORG_ADMIN cancel someone else's pending request | `200`, `CANCELLED` |
| J9 | Requester cancels a request that is already APPROVED | `400 BAD_REQUEST` (terminal) |
| J10 | Join request with `requestedRole: "ORGANIZATION_ADMIN"` | `400 VALIDATION_ERROR` (cannot request admin roles) |

## 8. Membership Tests

| # | Test | Expected |
|---|---|---|
| M1 | OWNER lists members (`GET /api/organizations/{{orgId}}/members`) | `200` paginated list |
| M2 | Member list pagination (`?page=0&size=5`) | `200` with `page`, `totalElements` |
| M3 | OWNER changes a member's role | `200`, `membershipRole` updated |
| M4 | OWNER assigns `ORGANIZATION_ADMIN` | `200` |
| M5 | ORG_ADMIN (non-OWNER) assigns `ORGANIZATION_ADMIN` | `400 BAD_REQUEST` |
| M6 | Role change to `OWNER` | `400 BAD_REQUEST` (ownership transfer out of scope) |
| M7 | OWNER suspends a member | `200`, status `SUSPENDED` |
| M8 | Suspending the OWNER | `400 BAD_REQUEST` (business rule, doc 10 FR-12) |
| M9 | OWNER removes a member (`DELETE .../members/{{membershipId}}`) | `200 OK` + `{ "message": "Resource deleted successfully" }`; row status `REMOVED` |
| M10 | OWNER removes themselves while org ACTIVE | `400 BAD_REQUEST` |
| M11 | MANAGER views members | `200` |
| M12 | MANAGER changes a member role | `403 FORBIDDEN_OPERATION` |

## 9. Structure Hierarchy Tests

| # | Test | Expected |
|---|---|---|
| S1 | Create building under inactive organization | `400 BAD_REQUEST` |
| S2 | Create block under `MAINTENANCE`/`INACTIVE` building | `400 BAD_REQUEST` |
| S3 | Delete a floor that still has units | `400 BAD_REQUEST` |
| S4 | Delete an empty unit | `200 OK` + `{ "message": "Resource deleted successfully" }` |
| S5 | Update unit status `AVAILABLE → OCCUPIED` | `200` |
| S6 | Invalid unit transition `OCCUPIED → INACTIVE` | `400 BAD_REQUEST` |
| S7 | `PATCH /api/units/{{unitId}}/status` with `{ "status": "BOOKED" }` | `400 VALIDATION_ERROR` |
| S8 | Create unit with `capacity: 0` | `201 Created` (0 allowed) |
| S9 | Access a floor of another organization's block | `403 FORBIDDEN_OPERATION` |
| S10 | GET `/api/buildings/999999` | `404 RESOURCE_NOT_FOUND` |

## 10. System-Admin Tests

| # | Test | Expected |
|---|---|---|
| SA1 | `GET /api/admin/organizations` | `200` paginated list |
| SA2 | `GET /api/admin/organizations?status=SUSPENDED&type=HOSTEL` | `200` filtered |
| SA3 | `GET /api/admin/organizations/{{orgId}}` | `200` (any organization) |
| SA4 | `PATCH /api/admin/organizations/{{orgId}}/suspend` | `200`, status `SUSPENDED` |
| SA5 | Restricted ops on a suspended org (e.g., create building) | `400 BAD_REQUEST` |
| SA6 | `PATCH /api/admin/organizations/{{orgId}}/activate` | `200`, status `ACTIVE` |
| SA7 | `PATCH /api/admin/organizations/999999/suspend` | `404 RESOURCE_NOT_FOUND` |
| SA8 | `CONSUMER` role calls any admin endpoint | `403 FORBIDDEN_OPERATION` |
| SA9 | Owner deactivate + admin suspend both tracked via `updated_at` | verify in DB (§12) |

## 11. Expected HTTP Status Codes

| Status | Used for |
|---|---|
| `200 OK` | Successful GET, PUT, PATCH, and DELETE (DELETE returns `{ "message": "Resource deleted successfully" }`) |
| `201 Created` | Successful POST (organization, join request, building, block, floor, unit) |
| `400 Bad Request` | Validation errors, malformed JSON, business-rule violations, missing headers (direct call) |
| `401 Unauthorized` | Missing/invalid/expired JWT (from Gateway) |
| `403 Forbidden` | Authenticated but not permitted (non-member, role too low) |
| `404 Not Found` | Organization/membership/join-request/structure/unit not found |
| `409 Conflict` | Duplicate org code, membership, PENDING join request, building/block/floor/unit code; DB constraint violation |
| `500 Internal Server Error` | Unexpected error (no stack trace exposed) |

## 12. MySQL Verification Queries

```sql
-- 1. Verify the service uses the right database
USE organization_db;

-- 2. Confirm tables were created
SHOW TABLES;
-- Expected: blocks, buildings, floors, organization_join_requests,
--           organization_memberships, organizations, units

-- 3. Inspect the organizations schema
DESCRIBE organizations;

-- 4. Verify unique constraints exist (organization code, memberships, structure codes)
SHOW INDEX FROM organizations WHERE Non_unique = 0;
SHOW INDEX FROM organization_memberships WHERE Non_unique = 0;
SHOW INDEX FROM organization_join_requests WHERE Non_unique = 0;
SHOW INDEX FROM buildings WHERE Non_unique = 0;
SHOW INDEX FROM blocks WHERE Non_unique = 0;
SHOW INDEX FROM floors WHERE Non_unique = 0;
SHOW INDEX FROM units WHERE Non_unique = 0;

-- 5. Organization + owner membership created atomically
SELECT o.id, o.name, o.organization_code, o.status,
       m.auth_user_id, m.membership_role, m.membership_status
FROM organizations o
JOIN organization_memberships m ON m.organization_id = o.id
WHERE o.organization_code = 'SUNRISE-HST';

-- 6. Join request approval created a membership
SELECT jr.id, jr.status, jr.requested_role, jr.reviewed_by_auth_user_id, jr.reviewed_at,
       m.membership_role, m.membership_status, m.joined_at
FROM organization_join_requests jr
LEFT JOIN organization_memberships m
       ON m.organization_id = jr.organization_id
      AND m.auth_user_id = jr.auth_user_id
WHERE jr.id = <joinRequestId>;

-- 7. No duplicate PENDING join requests per (org, user)
SELECT organization_id, auth_user_id, status, COUNT(*) AS cnt
FROM organization_join_requests
WHERE status = 'PENDING'
GROUP BY organization_id, auth_user_id, status
HAVING COUNT(*) > 1;

-- 8. Removed members are soft-deleted (status REMOVED, row still present)
SELECT id, organization_id, auth_user_id, membership_role, membership_status
FROM organization_memberships
WHERE membership_status = 'REMOVED';

-- 9. Structure hierarchy integrity (unit -> floor -> block -> building -> org)
SELECT u.unit_number, f.floor_number, b.code AS block_code, bg.code AS building_code,
       o.name AS organization_name
FROM units u
JOIN floors  f  ON f.id = u.floor_id
JOIN blocks  b  ON b.id = f.block_id
JOIN buildings bg ON bg.id = b.building_id
JOIN organizations o ON o.id = bg.organization_id
ORDER BY o.id, bg.code, b.code, f.floor_number, u.unit_number;

-- 10. Units with negative capacity (must return 0 rows)
SELECT id, unit_number, capacity FROM units WHERE capacity < 0;

-- 11. Organizations per status (for the admin dashboard)
SELECT status, COUNT(*) FROM organizations GROUP BY status;

-- 12. Members per role (for role management screens)
SELECT membership_role, COUNT(*) FROM organization_memberships
WHERE membership_status = 'ACTIVE'
GROUP BY membership_role;
```

## 13. Direct-Service Debugging Test (Port 8085 — Debug Only)

Use **only** when the Gateway path is unavailable. Because the Gateway is bypassed
you must **manually provide the trusted headers** exactly as the Gateway would:

| Item | Value |
|---|---|
| URL | `http://localhost:8085/api/organizations` |
| Headers | `X-User-Id: 1` · `X-User-Role: CONSUMER` · `Content-Type: application/json` |
| Expected | `201 Created` |

```json
{
  "name": "Debug Hostel",
  "organizationCode": "DEBUG-HST",
  "organizationType": "HOSTEL",
  "addressLine1": "1 Debug Lane",
  "city": "Test City"
}
```

> ⚠️ This bypasses JWT validation entirely — use it only for local debugging,
> never in production. All normal tests must go through `http://localhost:8080`.

## 14. Error Response Shapes

All service-level errors use the standard VOLTARAS envelope:

```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_RESOURCE",
    "message": "Organization code already exists: SUNRISE-HST"
  },
  "timestamp": "2026-08-02T14:20:00",
  "path": "/api/organizations"
}
```

Validation failures add field-level details:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for the request",
    "details": [
      { "field": "name", "message": "Organization name is required" },
      { "field": "organizationCode", "message": "must match [A-Za-z0-9_-]{4,20}" }
    ]
  },
  "timestamp": "2026-08-02T14:20:00",
  "path": "/api/organizations"
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

---

> **End of Organization Service API Test Plan**
> *Documentation only — no Postman collection file generated in this task.*
