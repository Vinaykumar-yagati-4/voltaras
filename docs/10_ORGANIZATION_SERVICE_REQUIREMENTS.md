# VOLTARAS — Organization Service Requirements

> **Service:** `organization-service` · **Port:** `8085` · **Database:** `organization_db`
> **Base package:** `com.voltaras.organizationservice`
> **Framework:** Spring Boot 3.5.5 · Spring Cloud 2025.0.3 · Java 25 · Spring Data JPA · Jakarta Validation · MySQL · Lombok · Eureka Client · Actuator · JUnit & Mockito · Maven
> **Document:** `docs/10_ORGANIZATION_SERVICE_REQUIREMENTS.md`

---

## 1. Purpose

The Organization Service introduces **optional organization membership** to VOLTARAS. It lets any authenticated user create a utility-managing organization (hostel, institution, apartment complex, or commercial entity), request to join one, and manage the organization's members and physical structure (buildings → blocks → floors → units).

The service exists so that VOLTARAS can be used by groups of people sharing a metered connection or a managed property (a hostel warden managing rooms, an institution managing classrooms/labs, an apartment tower managing flats, a commercial complex managing offices/shops), without forcing any user to belong to an organization.

Organization membership is **strictly optional** — the public platform (register, login, personal meter readings, bills, payments) continues to work for every user independently through the existing Auth Service and User Service.

## 2. Core Vision & Relationship to the Auth Service

VOLTARAS is a publicly accessible utility-management platform. The Organization Service must **never** become a gatekeeper:

| Rule | Detail |
|---|---|
| Register normally | Any person can register via Auth Service without any code |
| Login normally | Any person can log in via Auth Service without any code |
| Use personal features immediately | Meter readings, bills, payments work for every user with no organization |
| Organization membership optional | Creating/joining an organization is a separate, opt-in step |
| No secret codes | No hostel/institution/organization code is required for registration or login |
| One common auth system | Only the existing Auth Service authenticates users; no separate auth for hostels, institutions, apartments, or commercial organizations |
| No `INDIVIDUAL` organization type | Independent users already use VOLTARAS through Auth Service + User Service without an organization |

**Identity trust model** — the service trusts only the API Gateway. The Gateway validates the JWT, strips any client-supplied identity headers, and injects trusted headers (same model as `meter-reading-service`):

| Trusted header | Value | Notes |
|---|---|---|
| `X-User-Id` | authenticated auth-user id | Never parsed from the body or URL |
| `X-User-Email` | authenticated email | Informational |
| `X-User-Role` | `CONSUMER` or `ADMIN` | `ADMIN` = system admin |

The service never parses or validates JWT tokens and never accepts `authUserId` from the request body.

## 3. Scope

### 3.1 In Scope

1. Organizations (create, view, update, deactivate)
2. Organization memberships (view, change role, suspend, remove)
3. Organization join requests (create, list, approve, reject, cancel)
4. Organization-level roles (`OWNER`, `ORGANIZATION_ADMIN`, `MANAGER`, `MEMBER`, `TENANT`, `STUDENT`, `STAFF`)
5. Physical structure management — buildings, blocks, floors, units
6. Organization activation, deactivation, and system-admin suspension/activation
7. Optional membership for all users (no registration/login impact)

### 3.2 Out of Scope (see §15)

Ownership transfer, public organization search/listing, unit-to-meter assignment, organization-level billing, and type-specific dashboards are future work.

## 4. Supported Organization Types

All organization types use the same generic, reusable structures. There are **no separate entity models** per type.

| OrganizationType | Typical hierarchy | Role emphasis |
|---|---|---|
| `HOSTEL` | Organization → Building → Block → Floor → Room | TENANT / MANAGER |
| `INSTITUTION` | Organization → Building → Block or Department → Floor → Classroom or Lab | STUDENT / STAFF / MANAGER |
| `APARTMENT` | Organization → Building or Tower → Block → Floor → Flat | MEMBER / TENANT / MANAGER |
| `COMMERCIAL` | Organization → Building → Wing or Block → Floor → Office or Shop | MEMBER / MANAGER |

The four types differ only in **labeling** — the tables and APIs are identical.

## 5. User Journeys

### J1 — Independent consumer (no organization)
1. Register + login via Auth Service (no code required).
2. Submit meter readings, view bills, pay — all personal features work immediately.
3. Organization Service is never involved.

### J2 — Create an organization
1. Authenticated user calls `POST /api/organizations` (identity from `X-User-Id`).
2. Organization created with `status=ACTIVE`.
3. Creator automatically becomes `OWNER` with `ACTIVE` membership.
4. Creator adds buildings → blocks → floors → units and invites members.

### J3 — Join an organization
1. Non-member discovers an organization id (via UI or admin).
2. Calls `POST /api/organizations/{organizationId}/join-requests` with an optional requested role.
3. Request starts as `PENDING`.
4. OWNER / ORGANIZATION_ADMIN approves → membership created (`ACTIVE`, requested role).
5. Member can now see organization data and (if permitted) manage structure.

### J4 — Join request rejected or cancelled
1. OWNER / ORGANIZATION_ADMIN rejects with mandatory remarks → status `REJECTED`, reviewer id + time recorded.
2. OR the requester cancels their own pending request → status `CANCELLED`.
3. The user may submit a fresh request later.

### J5 — Member management
1. OWNER / ORGANIZATION_ADMIN view members.
2. Change a member's role (only OWNER can assign/remove `ORGANIZATION_ADMIN`).
3. Suspend or remove a member. OWNER cannot remove themselves while the organization is `ACTIVE`.

### J6 — Structure management
1. OWNER / ORGANIZATION_ADMIN / MANAGER create buildings, blocks, floors, and units.
2. Units are tracked with type, capacity, and status (`AVAILABLE`, `OCCUPIED`, `INACTIVE`, `MAINTENANCE`).

### J7 — System admin intervention
1. System `ADMIN` (auth role) lists all organizations, views any organization, suspends or activates it.

## 6. Functional Requirements

| # | Requirement | Key rule |
|---|---|---|
| FR-1 | Create organization | Any authenticated user; identity from `X-User-Id`; `organization_code` unique; `name` not blank; default status `ACTIVE`; creator becomes `OWNER` + `ACTIVE` member — all in one transaction |
| FR-2 | View own organizations | `GET /api/organizations/me` returns the caller's organizations with their membership role/status |
| FR-3 | View organization | ACTIVE members of the organization and system `ADMIN` only |
| FR-4 | Update organization | `OWNER` / `ORGANIZATION_ADMIN` only; organization must be `ACTIVE` |
| FR-5 | Deactivate organization | `OWNER` or system `ADMIN`; transitions `ACTIVE` → `INACTIVE`; restricted operations rejected while inactive |
| FR-6 | Request to join | Any authenticated non-member; one `PENDING` request per (organization, user); ACTIVE members cannot request |
| FR-7 | Approve join request | `OWNER` / `ORGANIZATION_ADMIN`; on approval the request becomes `APPROVED` **and** an `ACTIVE` membership with the requested role is created atomically |
| FR-8 | Reject join request | `OWNER` / `ORGANIZATION_ADMIN`; `rejectionRemarks` mandatory; reviewer id + time stored |
| FR-9 | Cancel join request | Requester (own pending request) or `OWNER` / `ORGANIZATION_ADMIN` |
| FR-10 | View members | `OWNER` / `ORGANIZATION_ADMIN` / `MANAGER`; paginated |
| FR-11 | Change member role | `OWNER` / `ORGANIZATION_ADMIN`; only `OWNER` may assign/remove `ORGANIZATION_ADMIN`; `OWNER` membership role cannot be changed |
| FR-12 | Suspend member | `OWNER` / `ORGANIZATION_ADMIN`; `ACTIVE` → `SUSPENDED`; cannot suspend the `OWNER` |
| FR-13 | Remove member | `OWNER` / `ORGANIZATION_ADMIN`; soft delete to status `REMOVED`; `OWNER` cannot remove themselves while the organization is `ACTIVE` |
| FR-14 | Manage buildings | `OWNER` / `ORGANIZATION_ADMIN` / `MANAGER`; code unique within an organization; child of `ACTIVE` organization |
| FR-15 | Manage blocks | Same roles; code unique within a building; parent building must be `ACTIVE` |
| FR-16 | Manage floors | Same roles; floor number unique within a block; parent block must be `ACTIVE` |
| FR-17 | Manage units | Same roles; unit number unique within a floor; capacity ≥ 0; parent floor must be `ACTIVE` |
| FR-18 | Update unit status | Same roles; allowed transitions per §9.8 |
| FR-19 | System admin view | `GET /api/admin/organizations` paginated; `GET /api/admin/organizations/{id}` any organization |
| FR-20 | System admin suspend/activate | `PATCH .../suspend` → `SUSPENDED`; `PATCH .../activate` → `ACTIVE` (from `INACTIVE` or `SUSPENDED`) |

All ownership, membership, role, and status checks live in the **service layer** (never in controllers).

## 7. Non-Functional Requirements

| # | Requirement | Detail |
|---|---|---|
| NFR-1 | Transaction safety | Join-request approval (membership + request status), organization creation (org + OWNER membership), and deactivation flows are `@Transactional`; failures roll back fully |
| NFR-2 | Database indexing | Unique constraints and lookup indexes per §11 (docs/11) for org code, membership (org, user), join requests, and structure codes |
| NFR-3 | Pagination | Member lists and system-admin organization lists support `page`, `size`, `sort` (defaults `page=0`, `size=10`, sort `createdAt,desc`) |
| NFR-4 | Audit timestamps | `created_at` / `updated_at` on every table; `joined_at`, `reviewed_at` for lifecycle events |
| NFR-5 | Logging without sensitive data | SLF4J INFO on business actions; log ids and codes, never passwords, tokens, or personal details; ERROR for unexpected failures without leaking stack traces to clients |
| NFR-6 | API response consistency | Single error envelope (§13) and consistent response DTOs; entities never exposed directly (DTO + mapper pattern) |
| NFR-7 | Ownership validation | Every nested resource lookup validates the full ancestry (unit → floor → block → building → organization) and the caller's membership before any operation |
| NFR-8 | Maintainability | Layered architecture (controller → service interface → impl → repository → entity), constructor injection, Lombok, mapper classes, centralized `GlobalExceptionHandler` |
| NFR-9 | Future scalability | Stateless service, Eureka-registered, database-per-service (`organization_db`), indexes sized for large member/unit counts |
| NFR-10 | Future Meter Management Service | Units will become assignable targets for meters; keep `unit_number`/`unit_type`/`capacity` stable and expose unit ids in responses |
| NFR-11 | Future Billing & Tariff integration | Organizations/units may own tariff/billing contexts; organization id must remain stable and unique |
| NFR-12 | Future prepaid & postpaid support | Design unit status and membership lifecycle so a metered unit can later carry a billing mode without schema rewrites |
| NFR-13 | Future type-specific dashboards | OrganizationType is stored as an enum column so dashboards can filter by `HOSTEL`, `INSTITUTION`, `APARTMENT`, `COMMERCIAL` |

## 8. Roles and Permissions

### 8.1 Membership roles (organization-level)

| Role | Typical holder | Structure mgmt | Member mgmt | Join-request review | Org update | Org deactivate |
|---|---|---|---|---|---|---|
| `OWNER` | Creator / top authority | ✅ | ✅ (incl. assign/remove `ORGANIZATION_ADMIN`) | ✅ | ✅ | ✅ |
| `ORGANIZATION_ADMIN` | Deputy administrator | ✅ | ✅ (cannot assign/remove `ORGANIZATION_ADMIN`) | ✅ | ✅ | ❌ |
| `MANAGER` | Building/floor manager | ✅ | view members only | ❌ | ❌ | ❌ |
| `MEMBER` / `TENANT` / `STUDENT` / `STAFF` | Regular participants | ❌ | ❌ | ❌ | ❌ | ❌ |

### 8.2 Platform roles (from `X-User-Role`)

| Platform role | Meaning for this service |
|---|---|
| `CONSUMER` | Any authenticated user; may create organizations, join, and use personal features |
| `ADMIN` | System admin; may list/view/suspend/activate any organization via `/api/admin/organizations/**` |

## 9. Enum Design

### 9.1 OrganizationType
`HOSTEL`, `INSTITUTION`, `APARTMENT`, `COMMERCIAL` — no `INDIVIDUAL`.

### 9.2 OrganizationStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Normal operation; new organizations start here |
| `INACTIVE` | Deactivated by OWNER or system admin; restricted operations rejected |
| `SUSPENDED` | System-admin action; same restrictions as inactive |

### 9.3 MembershipRole
`OWNER`, `ORGANIZATION_ADMIN`, `MANAGER`, `MEMBER`, `TENANT`, `STUDENT`, `STAFF`.

### 9.4 MembershipStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Full membership; default on creation/approval |
| `SUSPENDED` | Temporarily disabled by an administrator |
| `REMOVED` | Terminal soft-delete state |

### 9.5 JoinRequestStatus
`PENDING` → `APPROVED` / `REJECTED` / `CANCELLED` (terminal states).

### 9.6 StructureStatus (buildings, blocks, floors)
`ACTIVE`, `INACTIVE`, `MAINTENANCE`.

### 9.7 UnitType
`ROOM`, `FLAT`, `CLASSROOM`, `LAB`, `OFFICE`, `SHOP`, `OTHER`.

### 9.8 UnitStatus
`AVAILABLE`, `OCCUPIED`, `INACTIVE`, `MAINTENANCE`.

Allowed transitions: `AVAILABLE ⇄ OCCUPIED`, `AVAILABLE ⇄ INACTIVE`, `AVAILABLE ⇄ MAINTENANCE`, `OCCUPIED → MAINTENANCE`. Any other transition → `400 BAD_REQUEST`.

## 10. API List

Base paths: `/api/organizations/**` (member APIs) and `/api/admin/organizations/**` (system admin APIs). All require authentication (Gateway). **Postman, not Swagger.**

### 10.1 Organization APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/organizations` | any authenticated user | Create organization (creator becomes OWNER) |
| `GET` | `/api/organizations/me` | any authenticated user | My organizations + membership info |
| `GET` | `/api/organizations/{organizationId}` | ACTIVE members, system admin | View organization |
| `PUT` | `/api/organizations/{organizationId}` | OWNER, ORGANIZATION_ADMIN | Update organization |
| `PATCH` | `/api/organizations/{organizationId}/deactivate` | OWNER, system admin | Deactivate → `INACTIVE` |

### 10.2 Join-request APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/organizations/{organizationId}/join-requests` | any authenticated non-member | Request to join |
| `GET` | `/api/organizations/{organizationId}/join-requests` | OWNER, ORGANIZATION_ADMIN | List requests (optional `?status=`) |
| `PATCH` | `/api/organizations/{organizationId}/join-requests/{requestId}/approve` | OWNER, ORGANIZATION_ADMIN | Approve (creates membership) |
| `PATCH` | `/api/organizations/{organizationId}/join-requests/{requestId}/reject` | OWNER, ORGANIZATION_ADMIN | Reject (remarks mandatory) |
| `PATCH` | `/api/organizations/{organizationId}/join-requests/{requestId}/cancel` | requester, OWNER, ORGANIZATION_ADMIN | Cancel own/pending request |

### 10.3 Membership APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `GET` | `/api/organizations/{organizationId}/members` | OWNER, ORGANIZATION_ADMIN, MANAGER | Paginated member list |
| `PATCH` | `/api/organizations/{organizationId}/members/{membershipId}/role` | OWNER, ORGANIZATION_ADMIN | Change role (OWNER-only for ORGANIZATION_ADMIN) |
| `PATCH` | `/api/organizations/{organizationId}/members/{membershipId}/suspend` | OWNER, ORGANIZATION_ADMIN | Suspend member |
| `DELETE` | `/api/organizations/{organizationId}/members/{membershipId}` | OWNER, ORGANIZATION_ADMIN | Remove member (status → `REMOVED`) → `200 OK` + `{ "message": ... }` |

### 10.4 Building APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/organizations/{organizationId}/buildings` | OWNER, ORGANIZATION_ADMIN, MANAGER | Create building |
| `GET` | `/api/organizations/{organizationId}/buildings` | ACTIVE members | List buildings |
| `GET` | `/api/buildings/{buildingId}` | ACTIVE members of owning org | View building |
| `PUT` | `/api/buildings/{buildingId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Update building |
| `DELETE` | `/api/buildings/{buildingId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Delete building (no children) → `200 OK` + `{ "message": ... }` |

### 10.5 Block APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/buildings/{buildingId}/blocks` | OWNER, ORGANIZATION_ADMIN, MANAGER | Create block |
| `GET` | `/api/buildings/{buildingId}/blocks` | ACTIVE members of owning org | List blocks |
| `GET` | `/api/blocks/{blockId}` | ACTIVE members of owning org | View block |
| `PUT` | `/api/blocks/{blockId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Update block |
| `DELETE` | `/api/blocks/{blockId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Delete block (no children) → `200 OK` + `{ "message": ... }` |

### 10.6 Floor APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/blocks/{blockId}/floors` | OWNER, ORGANIZATION_ADMIN, MANAGER | Create floor |
| `GET` | `/api/blocks/{blockId}/floors` | ACTIVE members of owning org | List floors |
| `GET` | `/api/floors/{floorId}` | ACTIVE members of owning org | View floor |
| `PUT` | `/api/floors/{floorId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Update floor |
| `DELETE` | `/api/floors/{floorId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Delete floor (no children) → `200 OK` + `{ "message": ... }` |

### 10.7 Unit APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `POST` | `/api/floors/{floorId}/units` | OWNER, ORGANIZATION_ADMIN, MANAGER | Create unit |
| `GET` | `/api/floors/{floorId}/units` | ACTIVE members of owning org | List units |
| `GET` | `/api/units/{unitId}` | ACTIVE members of owning org | View unit |
| `PUT` | `/api/units/{unitId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Update unit |
| `PATCH` | `/api/units/{unitId}/status` | OWNER, ORGANIZATION_ADMIN, MANAGER | Change unit status |
| `DELETE` | `/api/units/{unitId}` | OWNER, ORGANIZATION_ADMIN, MANAGER | Delete unit → `200 OK` + `{ "message": ... }` |

### 10.8 System Admin APIs

| Method | Path | Allowed | Description |
|---|---|---|---|
| `GET` | `/api/admin/organizations` | system `ADMIN` | Paginated list, optional `?status=&type=` filters |
| `GET` | `/api/admin/organizations/{organizationId}` | system `ADMIN` | View any organization |
| `PATCH` | `/api/admin/organizations/{organizationId}/suspend` | system `ADMIN` | `ACTIVE` → `SUSPENDED` |
| `PATCH` | `/api/admin/organizations/{organizationId}/activate` | system `ADMIN` | `INACTIVE`/`SUSPENDED` → `ACTIVE` |

## 11. Authorization Rules

1. **All APIs require authenticated gateway headers.** Missing/invalid/expired JWT → `401` at the Gateway (before reaching the service).
2. Any authenticated user can create an organization.
3. Any authenticated non-member can request membership (one `PENDING` request at a time).
4. Only `OWNER` and `ORGANIZATION_ADMIN` can approve or reject join requests.
5. Only `OWNER` can assign or remove the `ORGANIZATION_ADMIN` role.
6. `OWNER` cannot remove themselves while the organization is `ACTIVE`.
7. Organization members must not access unauthorized organizations — membership + role checks in the service layer.
8. System `ADMIN` can view, suspend, and activate any organization.
9. Inactive or suspended organizations reject restricted operations (`400 BAD_REQUEST`).
10. Never trust organization ids without membership and permission checks — every nested structure call validates the full parent chain.
11. Suspended members are treated as non-members for restricted operations.

## 12. Validation Rules & Error Cases

### 12.1 Field-level validation (Jakarta Validation on request DTOs)

| DTO | Rules |
|---|---|
| `CreateOrganizationRequest` | `name` @NotBlank (≤100); `organizationCode` @NotBlank @Pattern `[A-Za-z0-9_-]{4,20}`; `organizationType` @NotNull; `email` @Email (optional); `addressLine1` @NotBlank (≤255); `description` optional ≤500; `phone` optional ≤20; `city`/`state`/`country` optional ≤100; `postalCode` optional ≤20 |
| `UpdateOrganizationRequest` | Same optional fields; `name` @NotBlank if present. `organizationCode` is **immutable after creation** — never accepted on update |
| `CreateJoinRequestRequest` | `requestedRole` optional (default `MEMBER`; must not be `OWNER`/`ORGANIZATION_ADMIN`); `requestMessage` ≤500 |
| `RejectJoinRequestRequest` | `rejectionRemarks` @NotBlank (≤500) |
| `ChangeMembershipRoleRequest` | `membershipRole` @NotNull; must not be `OWNER` |
| `CreateBuildingRequest` / `CreateBlockRequest` | `name` @NotBlank (≤100); `code` @NotBlank @Pattern `[A-Za-z0-9_-]{1,50}`; optional description/address; `status` default `ACTIVE` |
| `CreateFloorRequest` | `floorNumber` @NotNull; optional `name` (≤100), `description`; `status` default `ACTIVE` |
| `CreateUnitRequest` | `unitNumber` @NotBlank (≤50); `unitType` @NotNull; `capacity` @NotNull @PositiveOrZero; optional `unitName` (≤100), `description`; `status` default `AVAILABLE` |
| `UpdateUnitStatusRequest` | `status` @NotNull (valid `UnitStatus` value) |

### 12.2 Error cases → HTTP status

| # | Case | Status | Error code |
|---|---|---|---|
| 1 | Missing `X-User-Id` / `X-User-Role` (direct service call) | 400 | `MISSING_HEADER` |
| 2 | Duplicate organization code | 409 | `DUPLICATE_RESOURCE` |
| 3 | Organization not found | 404 | `RESOURCE_NOT_FOUND` |
| 4 | Duplicate membership (org, user) | 409 | `DUPLICATE_RESOURCE` |
| 5 | Duplicate pending join request | 409 | `DUPLICATE_RESOURCE` |
| 6 | Existing ACTIVE membership when requesting to join | 409 | `DUPLICATE_RESOURCE` |
| 7 | Unauthorized organization access (non-member) | 403 | `FORBIDDEN_OPERATION` |
| 8 | Invalid organization role assignment (e.g., assigning `ORGANIZATION_ADMIN` without OWNER) | 400 | `BAD_REQUEST` |
| 9 | Owner self-removal while organization ACTIVE | 400 | `BAD_REQUEST` |
| 10 | Operation on inactive/suspended organization | 400 | `BAD_REQUEST` |
| 11 | Duplicate building code in organization | 409 | `DUPLICATE_RESOURCE` |
| 12 | Duplicate block code in building | 409 | `DUPLICATE_RESOURCE` |
| 13 | Duplicate floor number in block | 409 | `DUPLICATE_RESOURCE` |
| 14 | Duplicate unit number in floor | 409 | `DUPLICATE_RESOURCE` |
| 15 | Negative capacity | 400 | `VALIDATION_ERROR` |
| 16 | Invalid organization hierarchy (cross-org parent, delete parent with children, child under non-ACTIVE parent) | 400 | `BAD_REQUEST` |
| 17 | Malformed JSON body | 400 | `MALFORMED_REQUEST` |
| 18 | DTO validation failures | 400 | `VALIDATION_ERROR` (+ field `details`) |
| 19 | Database constraint violation (race condition) | 409 | `DATA_CONSTRAINT_VIOLATION` |
| 20 | Invalid path/query parameter (non-numeric id, unknown enum) | 400 | `INVALID_ARGUMENT` |
| 21 | Missing/invalid/expired JWT (at Gateway) | 401 | Gateway `UNAUTHORIZED` |
| 22 | Unexpected server error | 500 | `INTERNAL_ERROR` |

## 13. Error Response Format

Follows the existing VOLTARAS envelope (same as auth/user/meter-reading services). No stack traces are exposed.

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Organization not found",
    "details": []
  },
  "timestamp": "2026-08-02T14:00:00",
  "path": "/api/organizations/100"
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
      { "field": "organizationCode", "message": "must match [A-Za-z0-9_-]{4,20}" },
      { "field": "name", "message": "Organization name is required" }
    ]
  },
  "timestamp": "2026-08-02T14:05:00",
  "path": "/api/organizations"
}
```

Gateway-level `401` keeps the gateway's own shape:

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired access token"
}
```

## 14. Acceptance Criteria

| # | Criterion |
|---|---|
| AC-1 | A user can register and log in normally (no code) and use personal features without any organization — confirmed unchanged |
| AC-2 | `POST /api/organizations` creates an ACTIVE organization and an ACTIVE `OWNER` membership for the caller atomically |
| AC-3 | Duplicate `organizationCode` returns `409 DUPLICATE_RESOURCE` |
| AC-4 | A non-member can create one `PENDING` join request; duplicates and requests from ACTIVE members return `409` |
| AC-5 | Approving a request creates an ACTIVE membership with the requested role in the same transaction |
| AC-6 | Rejecting a request without remarks returns `400`; reviewer id and time are stored |
| AC-7 | Only OWNER can assign/remove `ORGANIZATION_ADMIN`; other attempts return `400`/`403` |
| AC-8 | OWNER self-removal while ACTIVE is blocked |
| AC-9 | Non-members receive `403` for organization resources; nested structure calls validate the full hierarchy |
| AC-10 | Buildings/blocks/floors/units enforce per-parent unique codes and non-negative capacity |
| AC-11 | System ADMIN can list/view/suspend/activate any organization; `CONSUMER` role gets `403` on admin endpoints |
| AC-12 | Inactive/suspended organizations reject restricted operations |
| AC-13 | All errors use the standard envelope (§13); no stack traces exposed |
| AC-14 | Pagination works for member and admin organization lists |
| AC-15 | All endpoints verified via Postman (not Swagger) against `http://localhost:8080` |

## 15. Out-of-Scope Items

- `INDIVIDUAL` organization type (independent users need no organization)
- Ownership transfer / change-OWNER endpoint (future)
- Public organization search/listing (future; join flow uses known organization ids)
- Secret/invite codes for joining or for login (no codes anywhere in auth)
- Unit-to-meter assignment and meter management inside this service (future Meter Management Service)
- Organization-level billing, tariffs, prepaid/postpaid modes (future Billing & Tariff integration)
- Type-specific dashboards for hostel/institution/apartment/commercial (future)
- Notifications from the organization service (future Notification integration)
- Fine-grained per-member structure permissions beyond the MANAGER role (future)
- Explicit member reactivation (un-suspend) endpoint (future; suspended members are resolved via removal + re-join in this phase)
- Multi-currency / multi-utility organization support

## 16. Alignment with Existing VOLTARAS Documentation

| Item | Status | Note |
|---|---|---|
| Gateway trust model (`X-User-Id`, `X-User-Email`, `X-User-Role`) | ✅ Aligned | Same as `meter-reading-service` (docs/08) |
| Error envelope | ✅ Aligned | docs/09 + `ErrorResponse` in existing services |
| Database-per-service | ✅ Aligned | New `organization_db`, external refs to `auth_db.users.id`, no cross-DB FKs (docs/04) |
| Layered architecture & conventions | ✅ Aligned | docs/01, docs/03 |
| Postman over Swagger | ✅ Aligned | Established by meter-reading service |
| Port map (docs/03) | ✅ Resolved | Authoritative map: `organization-service` = 8085; `payment-service` → 8086; `complaint-service` → 8087; `notification-service` → 8088 (no duplicate ports) |
| Service list (docs/03/04/05/07) | ✅ Aligned | Docs 03/04/05/07 service catalogs updated with the official name `meter-reading-service` / "Meter Reading Service" and the new port map; Organization Service still needs a gateway route at implementation time (see docs/12 §0) |
| DELETE status code | ✅ Aligned | Organization Service DELETEs return `200 OK` + `{ "message": ... }`, matching the `meter-reading-service` convention (docs/09) |

> **End of Organization Service Requirements**
> *Documentation only — no Java code, no service folder, no database created.*
