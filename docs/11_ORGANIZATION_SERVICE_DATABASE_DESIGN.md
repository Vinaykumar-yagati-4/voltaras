# VOLTARAS — Organization Service Database Design

> **Service:** `organization-service` · **Port:** `8085` · **Database:** `organization_db`
> **Base package:** `com.voltaras.organizationservice`
> **Owner:** Organization Service (database-per-service pattern, docs/04)
> **Document:** `docs/11_ORGANIZATION_SERVICE_DATABASE_DESIGN.md`

---

## 1. Database Name & Ownership

| Item | Value |
|---|---|
| Database name | `organization_db` |
| Owner | `organization-service` (sole reader/writer) |
| Character set | `utf8mb4` / `utf8mb4_unicode_ci` |
| Engine | InnoDB |

No other service reads or writes `organization_db`. External references to
`auth_db.users.id` are stored as plain `BIGINT` columns **without** foreign key
constraints (per the VOLTARAS cross-database rule in docs/04); referential
integrity is enforced at the application layer using `X-User-Id`.

## 2. Entity Descriptions

| # | Entity | Table | Description |
|---|---|---|---|
| 1 | `Organization` | `organizations` | A hostel, institution, apartment complex, or commercial entity |
| 2 | `OrganizationMembership` | `organization_memberships` | Link between an auth user and an organization with an organization-level role |
| 3 | `OrganizationJoinRequest` | `organization_join_requests` | A user's request to join an organization |
| 4 | `Building` | `buildings` | A physical building (or tower) belonging to an organization |
| 5 | `Block` | `blocks` | A block / wing / department within a building |
| 6 | `Floor` | `floors` | A floor level within a block |
| 7 | `Unit` | `units` | A room, flat, classroom, lab, office, or shop on a floor |

The structure chain `organizations → buildings → blocks → floors → units` is the
generic reusable hierarchy used by all four organization types.

## 3. Table Definitions

### 3.1 `organizations`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `name` | `VARCHAR(100)` | `NOT NULL` | — | Display name; must not be blank |
| `organization_code` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Client-provided unique short code (`[A-Za-z0-9_-]{4,20}`) |
| `organization_type` | `VARCHAR(20)` | `NOT NULL` | — | `HOSTEL`, `INSTITUTION`, `APARTMENT`, `COMMERCIAL` |
| `description` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional description |
| `email` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Optional contact email |
| `phone` | `VARCHAR(20)` | `NULLABLE` | `NULL` | Optional contact phone |
| `address_line_1` | `VARCHAR(255)` | `NOT NULL` | — | Primary address line |
| `address_line_2` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Secondary address line |
| `city` | `VARCHAR(100)` | `NULLABLE` | `NULL` | City |
| `state` | `VARCHAR(100)` | `NULLABLE` | `NULL` | State / province |
| `country` | `VARCHAR(100)` | `NULLABLE` | `NULL` | Country |
| `postal_code` | `VARCHAR(20)` | `NULLABLE` | `NULL` | Postal / ZIP code |
| `created_by_auth_user_id` | `BIGINT` | `NOT NULL` | — | External ref → `auth_db.users.id` (from `X-User-Id`) |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'ACTIVE'` | `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rules:** unique `organization_code`; new organizations start `ACTIVE`; creator automatically becomes `OWNER` + `ACTIVE` member in the same transaction.

### 3.2 `organization_memberships`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `organization_id` | `BIGINT` | `NOT NULL`, `FK → organizations(id)` | — | Owning organization |
| `auth_user_id` | `BIGINT` | `NOT NULL` | — | External ref → `auth_db.users.id` |
| `membership_role` | `VARCHAR(30)` | `NOT NULL` | — | `OWNER`, `ORGANIZATION_ADMIN`, `MANAGER`, `MEMBER`, `TENANT`, `STUDENT`, `STAFF` |
| `membership_status` | `VARCHAR(20)` | `NOT NULL` | `'ACTIVE'` | `ACTIVE`, `SUSPENDED`, `REMOVED` |
| `joined_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When membership began |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rules:** unique `(organization_id, auth_user_id)` — one membership per user per organization; `OWNER` cannot remove themselves while the organization is `ACTIVE`.

### 3.3 `organization_join_requests`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `organization_id` | `BIGINT` | `NOT NULL`, `FK → organizations(id)` | — | Target organization |
| `auth_user_id` | `BIGINT` | `NOT NULL` | — | External ref → `auth_db.users.id` (requester) |
| `requested_role` | `VARCHAR(30)` | `NULLABLE` | `'MEMBER'` | Desired role; never `OWNER`/`ORGANIZATION_ADMIN` |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'PENDING'` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `request_message` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional note from requester |
| `rejection_remarks` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Mandatory on rejection |
| `reviewed_by_auth_user_id` | `BIGINT` | `NULLABLE` | `NULL` | External ref → reviewer auth user |
| `reviewed_at` | `DATETIME` | `NULLABLE` | `NULL` | When reviewed |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rules:** one `PENDING` request per `(organization_id, auth_user_id)`; ACTIVE members cannot create requests; only OWNER / ORGANIZATION_ADMIN can approve or reject; rejection requires remarks.

### 3.4 `buildings`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `organization_id` | `BIGINT` | `NOT NULL`, `FK → organizations(id)` | — | Owning organization |
| `name` | `VARCHAR(100)` | `NOT NULL` | — | Building / tower name |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | Short code, unique within the organization |
| `description` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional |
| `address` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Optional address override |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'ACTIVE'` | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rule:** building code unique inside one organization (`(organization_id, code)`).

### 3.5 `blocks`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `building_id` | `BIGINT` | `NOT NULL`, `FK → buildings(id)` | — | Owning building |
| `name` | `VARCHAR(100)` | `NOT NULL` | — | Block / wing / department name |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | Short code, unique within the building |
| `description` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'ACTIVE'` | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rule:** block code unique inside one building (`(building_id, code)`).

### 3.6 `floors`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `block_id` | `BIGINT` | `NOT NULL`, `FK → blocks(id)` | — | Owning block |
| `floor_number` | `INT` | `NOT NULL` | — | Floor level (may be negative for basements), unique within the block |
| `name` | `VARCHAR(100)` | `NULLABLE` | `NULL` | Optional floor name (e.g., "Ground", "Mezzanine") |
| `description` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'ACTIVE'` | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rule:** floor number unique inside one block (`(block_id, floor_number)`).

### 3.7 `units`

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `floor_id` | `BIGINT` | `NOT NULL`, `FK → floors(id)` | — | Owning floor |
| `unit_number` | `VARCHAR(50)` | `NOT NULL` | — | Unit/room number, unique within the floor |
| `unit_name` | `VARCHAR(100)` | `NULLABLE` | `NULL` | Optional friendly name (e.g., "Deluxe Room") |
| `unit_type` | `VARCHAR(20)` | `NOT NULL` | — | `ROOM`, `FLAT`, `CLASSROOM`, `LAB`, `OFFICE`, `SHOP`, `OTHER` |
| `capacity` | `INT` | `NOT NULL`, `CHECK (capacity >= 0)` | `1` | Occupant capacity; cannot be negative |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'AVAILABLE'` | `AVAILABLE`, `OCCUPIED`, `INACTIVE`, `MAINTENANCE` |
| `description` | `VARCHAR(500)` | `NULLABLE` | `NULL` | Optional |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Audit |

**Business rules:** unit number unique inside one floor (`(floor_id, unit_number)`); `capacity >= 0` (DB `CHECK` + DTO `@PositiveOrZero`).

## 4. Primary Keys, Foreign Keys, Unique Constraints

### 4.1 Primary keys

Every table uses a single surrogate `BIGINT AUTO_INCREMENT` primary key (`id`).

### 4.2 Foreign keys (internal to `organization_db`)

| Constraint | Column | Reference | On Delete |
|---|---|---|---|
| `fk_memberships_organization` | `organization_memberships.organization_id` | `organizations(id)` | CASCADE |
| `fk_join_requests_organization` | `organization_join_requests.organization_id` | `organizations(id)` | CASCADE |
| `fk_buildings_organization` | `buildings.organization_id` | `organizations(id)` | CASCADE |
| `fk_blocks_building` | `blocks.building_id` | `buildings(id)` | CASCADE |
| `fk_floors_block` | `floors.block_id` | `blocks(id)` | CASCADE |
| `fk_units_floor` | `units.floor_id` | `floors(id)` | CASCADE |

> Cascades exist for referential hygiene. In practice organizations are deactivated, never hard-deleted; `DELETE` on a structure is blocked at the application layer when it has children.

### 4.3 External references (no DB FK — application-layer validation)

| Column | Points to |
|---|---|
| `organizations.created_by_auth_user_id` | `auth_db.users.id` |
| `organization_memberships.auth_user_id` | `auth_db.users.id` |
| `organization_join_requests.auth_user_id` | `auth_db.users.id` |
| `organization_join_requests.reviewed_by_auth_user_id` | `auth_db.users.id` |

### 4.4 Unique constraints

| Constraint | Columns | Purpose |
|---|---|---|
| `uk_organizations_code` | `organization_code` | One organization per code (global) |
| `uk_memberships_org_user` | `(organization_id, auth_user_id)` | No duplicate membership in one organization |
| `uk_join_requests_org_user_status` | `(organization_id, auth_user_id, status)` | One `PENDING` request per user per organization (also blocks duplicate APPROVED rows); MySQL has no partial unique index, so this composite is the DB-level guard while the app layer enforces the "one PENDING" rule and allows re-requesting after `REJECTED`/`CANCELLED` |
| `uk_buildings_org_code` | `(organization_id, code)` | Building code unique per organization |
| `uk_blocks_building_code` | `(building_id, code)` | Block code unique per building |
| `uk_floors_block_number` | `(block_id, floor_number)` | Floor number unique per block |
| `uk_units_floor_number` | `(floor_id, unit_number)` | Unit number unique per floor |

## 5. Recommended Indexes

| Table | Index | Columns | Type | Purpose |
|---|---|---|---|---|
| `organizations` | `idx_organizations_code` | `organization_code` | UNIQUE | Fast code lookup / duplicate check |
| `organizations` | `idx_organizations_type` | `organization_type` | NON-UNIQUE | Admin filters by type |
| `organizations` | `idx_organizations_status` | `status` | NON-UNIQUE | Admin filters by status |
| `organizations` | `idx_organizations_created_by` | `created_by_auth_user_id` | NON-UNIQUE | "My created organizations" queries |
| `organization_memberships` | `uk_memberships_org_user` | `(organization_id, auth_user_id)` | UNIQUE | Duplicate prevention + lookup |
| `organization_memberships` | `idx_memberships_user` | `auth_user_id` | NON-UNIQUE | "My organizations" (`GET /me`) |
| `organization_memberships` | `idx_memberships_org_status` | `(organization_id, membership_status)` | NON-UNIQUE | Member list by org + status |
| `organization_join_requests` | `uk_join_requests_org_user_status` | `(organization_id, auth_user_id, status)` | UNIQUE | PENDING dedupe guard |
| `organization_join_requests` | `idx_join_requests_org_status` | `(organization_id, status)` | NON-UNIQUE | Review queue by org + status |
| `organization_join_requests` | `idx_join_requests_user` | `auth_user_id` | NON-UNIQUE | Requester's request history |
| `buildings` | `uk_buildings_org_code` | `(organization_id, code)` | UNIQUE | Duplicate prevention + lookup |
| `buildings` | `idx_buildings_org` | `organization_id` | NON-UNIQUE | Buildings of an organization |
| `blocks` | `uk_blocks_building_code` | `(building_id, code)` | UNIQUE | Duplicate prevention + lookup |
| `blocks` | `idx_blocks_building` | `building_id` | NON-UNIQUE | Blocks of a building |
| `floors` | `uk_floors_block_number` | `(block_id, floor_number)` | UNIQUE | Duplicate prevention + lookup |
| `floors` | `idx_floors_block` | `block_id` | NON-UNIQUE | Floors of a block |
| `units` | `uk_units_floor_number` | `(floor_id, unit_number)` | UNIQUE | Duplicate prevention + lookup |
| `units` | `idx_units_floor` | `floor_id` | NON-UNIQUE | Units of a floor |
| `units` | `idx_units_type` | `unit_type` | NON-UNIQUE | Filter by unit type |
| `units` | `idx_units_status` | `status` | NON-UNIQUE | Filter by unit status (e.g., available units) |

## 6. Entity Relationships

```
organizations 1 ── M organization_memberships      (a user can belong to many organizations;
organizations 1 ── M organization_join_requests     a membership belongs to one organization)
organizations 1 ── M buildings
buildings     1 ── M blocks
blocks        1 ── M floors
floors        1 ── M units

External references (no FK):
  organization_memberships.auth_user_id  → auth_db.users.id
  organization_join_requests.auth_user_id → auth_db.users.id
  organizations.created_by_auth_user_id  → auth_db.users.id
```

```
┌──────────────────────────────────────────────────────────────┐
│                        organization_db                        │
│                                                              │
│  ┌──────────────────────────┐        ┌─────────────────────┐ │
│  │       organizations      │        │ organization_       │ │
│  │                          │        │ join_requests       │ │
│  │ id (PK)                  │1     M │ organization_id (FK)│ │
│  │ organization_code (UQ)   ├───────►│ auth_user_id (EXT)  │ │
│  │ created_by_auth_user_id  │        │ status              │ │
│  │ status                   │        └─────────────────────┘ │
│  └──────────┬───────────────┘                                │
│             │ 1            │ 1                               │
│             │ M            │ M                               │
│  ┌──────────▼───────────┐  │  ┌──────────────────────────┐  │
│  │ organization_        │  │  │         buildings         │  │
│  │ memberships          │  │  │ organization_id (FK)      │  │
│  │ organization_id (FK) │  │  │ code (UQ per org)         │  │
│  │ auth_user_id (EXT)   │  │  └───────────┬──────────────┘  │
│  │ membership_role      │  │              │ 1              │
│  │ membership_status    │  │              │ M              │
│  └──────────────────────┘  │  ┌───────────▼──────────────┐  │
│                            │  │          blocks           │  │
│                            │  │ building_id (FK)         │  │
│                            │  │ code (UQ per building)    │  │
│                            │  └───────────┬──────────────┘  │
│                            │              │ 1              │
│                            │              │ M              │
│                            │  ┌───────────▼──────────────┐  │
│                            │  │          floors           │  │
│                            │  │ block_id (FK)            │  │
│                            │  │ floor_number (UQ/block)  │  │
│                            │  └───────────┬──────────────┘  │
│                            │              │ 1              │
│                            │              │ M              │
│                            │  ┌───────────▼──────────────┐  │
│                            │  │          units            │  │
│                            │  │ floor_id (FK)            │  │
│                            │  │ unit_number (UQ/floor)   │  │
│                            │  │ capacity (>= 0)          │  │
│                            │  └──────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## 7. MySQL Creation Order

Create tables in dependency order (children after parents):

1. `organizations`
2. `organization_memberships` (depends on 1)
3. `organization_join_requests` (depends on 1)
4. `buildings` (depends on 1)
5. `blocks` (depends on 4)
6. `floors` (depends on 5)
7. `units` (depends on 6)

## 8. Sample Records

> Documentation only — illustrative seed data for manual testing.

```sql
USE organization_db;

INSERT INTO organizations
  (name, organization_code, organization_type, description, email, phone,
   address_line_1, city, state, country, postal_code, created_by_auth_user_id, status)
VALUES
  ('Sunrise Hostel', 'SUNRISE-HST', 'HOSTEL', 'Student hostel near campus',
   'sunrise@example.com', '9876543210', '12 College Road', 'Pune', 'Maharashtra', 'India', '411001', 1, 'ACTIVE'),
  ('Greenfield Institute', 'GRNFLD-INST', 'INSTITUTION', 'Engineering college',
   'info@greenfield.edu', '9123456789', '45 Knowledge Park', 'Bengaluru', 'Karnataka', 'India', '560001', 2, 'ACTIVE'),
  ('Lakeview Apartments', 'LKVIEW-APT', 'APARTMENT', 'Residential apartment complex',
   'care@lakeview.in', '9000000001', '8 Lake View Road', 'Mumbai', 'Maharashtra', 'India', '400001', 3, 'ACTIVE');

INSERT INTO organization_memberships (organization_id, auth_user_id, membership_role, membership_status)
VALUES
  (1, 1, 'OWNER', 'ACTIVE'),
  (1, 4, 'MANAGER', 'ACTIVE'),
  (1, 5, 'TENANT', 'ACTIVE'),
  (2, 2, 'OWNER', 'ACTIVE'),
  (2, 6, 'STUDENT', 'ACTIVE'),
  (3, 3, 'OWNER', 'ACTIVE'),
  (3, 7, 'MEMBER', 'ACTIVE');

INSERT INTO organization_join_requests
  (organization_id, auth_user_id, requested_role, status, request_message, rejection_remarks, reviewed_by_auth_user_id, reviewed_at)
VALUES
  (1, 8, 'TENANT', 'PENDING', 'I would like to join the hostel', NULL, NULL, NULL),
  (1, 9, 'TENANT', 'REJECTED', 'Requesting a room', 'Seats full for this semester', 1, '2026-08-02 11:00:00'),
  (2, 10, 'STUDENT', 'APPROVED', 'Admission letter attached', NULL, 2, '2026-08-02 12:30:00');

INSERT INTO buildings (organization_id, name, code, description, address, status)
VALUES
  (1, 'Main Hostel Building', 'MAIN', 'Four-storey hostel block', '12 College Road, Pune', 'ACTIVE'),
  (1, 'Annex Building', 'ANNEX', 'New annex with 2 floors', '12A College Road, Pune', 'ACTIVE'),
  (2, 'Academic Block', 'ACAD', 'Classrooms and labs', '45 Knowledge Park, Bengaluru', 'ACTIVE');

INSERT INTO blocks (building_id, name, code, description, status)
VALUES
  (1, 'Block A', 'A', 'Boys block', 'ACTIVE'),
  (1, 'Block B', 'B', 'Girls block', 'ACTIVE'),
  (3, 'Wing 1', 'W1', 'Ground-floor wing', 'ACTIVE');

INSERT INTO floors (block_id, floor_number, name, description, status)
VALUES
  (1, 1, 'First Floor', NULL, 'ACTIVE'),
  (1, 2, 'Second Floor', NULL, 'ACTIVE'),
  (2, 1, 'First Floor', NULL, 'ACTIVE'),
  (3, 1, 'Ground Floor', NULL, 'ACTIVE');

INSERT INTO units (floor_id, unit_number, unit_name, unit_type, capacity, status, description)
VALUES
  (1, '101', 'Deluxe Room', 'ROOM', 2, 'AVAILABLE', 'Double sharing with attached bath'),
  (1, '102', 'Standard Room', 'ROOM', 3, 'OCCUPIED', NULL),
  (2, '201', 'Deluxe Room', 'ROOM', 2, 'MAINTENANCE', 'Repainting in progress'),
  (4, 'G1', 'Physics Lab', 'LAB', 40, 'AVAILABLE', NULL),
  (4, 'G2', 'Classroom G2', 'CLASSROOM', 60, 'AVAILABLE', NULL);
```

## 9. Data Lifecycle & Status Transitions

### 9.1 `organizations.status`

```
ACTIVE ──(OWNER deactivate / admin action)──► INACTIVE
ACTIVE ──(system ADMIN suspend)────────────► SUSPENDED
INACTIVE ──(system ADMIN activate)─────────► ACTIVE
SUSPENDED ──(system ADMIN activate)────────► ACTIVE
```

- New organizations start `ACTIVE`.
- `INACTIVE` and `SUSPENDED` organizations reject restricted operations (`400 BAD_REQUEST`).
- Hard delete is not exposed; deactivation is the lifecycle end state for owners.

### 9.2 `organization_memberships.membership_status`

```
ACTIVE ──(admin suspend)──► SUSPENDED
ACTIVE / SUSPENDED ──(admin remove)──► REMOVED (terminal)
```

- Suspension is **one-way in this phase**: the API list has no
  member-reactivation endpoint, so a suspended member is only resolved via
  removal (then a fresh join request) or a future reactivate-member endpoint.
- Creator is `OWNER` + `ACTIVE` on creation.
- Approval of a join request creates `ACTIVE` membership with the requested role.
- The `OWNER` cannot be suspended; the `OWNER` cannot remove themselves while
  the organization is `ACTIVE`.

### 9.3 `organization_join_requests.status`

```
PENDING ──(approve by OWNER/ORG_ADMIN)──► APPROVED (creates ACTIVE membership)
PENDING ──(reject by OWNER/ORG_ADMIN, remarks mandatory)──► REJECTED
PENDING ──(cancel by requester or OWNER/ORG_ADMIN)────────► CANCELLED
```

`APPROVED`, `REJECTED`, `CANCELLED` are terminal; the user may submit a new request afterwards.

### 9.4 `buildings/blocks/floors.status` (StructureStatus)

```
ACTIVE ⇄ INACTIVE
ACTIVE ⇄ MAINTENANCE
```

- Children can only be created under an `ACTIVE` parent.
- A structure with children cannot be deleted (`400 BAD_REQUEST`).

### 9.5 `units.status` (UnitStatus)

```
AVAILABLE ⇄ OCCUPIED
AVAILABLE ⇄ INACTIVE
AVAILABLE ⇄ MAINTENANCE
OCCUPIED → MAINTENANCE
```

Any other transition (e.g., `OCCUPIED → INACTIVE` directly, `INACTIVE → OCCUPIED`) → `400 BAD_REQUEST`.

## 10. Sample MySQL Schema (Documentation Only)

> ⚠️ **Documentation only** — provided to describe the intended schema. In
> implementation, tables are created by Hibernate `ddl-auto` (as in
> `meter-reading-service`) or Flyway migrations; nothing here is executed now.

```sql
-- ============================================================
-- DATABASE: organization_db
-- PURPOSE:  Organizations, memberships, join requests, structure
-- OWNER:    Organization Service
-- ============================================================
CREATE DATABASE IF NOT EXISTS organization_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE organization_db;

-- 1. organizations
CREATE TABLE organizations (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(100)    NOT NULL,
    organization_code       VARCHAR(50)     NOT NULL,
    organization_type       VARCHAR(20)     NOT NULL,
    description             VARCHAR(500)    NULL,
    email                   VARCHAR(255)    NULL,
    phone                   VARCHAR(20)     NULL,
    address_line_1          VARCHAR(255)    NOT NULL,
    address_line_2          VARCHAR(255)    NULL,
    city                    VARCHAR(100)    NULL,
    state                   VARCHAR(100)    NULL,
    country                 VARCHAR(100)    NULL,
    postal_code             VARCHAR(20)     NULL,
    created_by_auth_user_id BIGINT          NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_organizations_code (organization_code),
    INDEX idx_organizations_type (organization_type),
    INDEX idx_organizations_status (status),
    INDEX idx_organizations_created_by (created_by_auth_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. organization_memberships
CREATE TABLE organization_memberships (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id   BIGINT          NOT NULL,
    auth_user_id      BIGINT          NOT NULL,
    membership_role   VARCHAR(30)     NOT NULL,
    membership_status VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    joined_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_memberships_org_user (organization_id, auth_user_id),
    INDEX idx_memberships_user (auth_user_id),
    INDEX idx_memberships_org_status (organization_id, membership_status),

    CONSTRAINT fk_memberships_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. organization_join_requests
CREATE TABLE organization_join_requests (
    id                       BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id          BIGINT          NOT NULL,
    auth_user_id             BIGINT          NOT NULL,
    requested_role           VARCHAR(30)     NULL,
    status                   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    request_message          VARCHAR(500)    NULL,
    rejection_remarks        VARCHAR(500)    NULL,
    reviewed_by_auth_user_id BIGINT          NULL,
    reviewed_at              DATETIME        NULL,
    created_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_join_requests_org_user_status (organization_id, auth_user_id, status),
    INDEX idx_join_requests_org_status (organization_id, status),
    INDEX idx_join_requests_user (auth_user_id),

    CONSTRAINT fk_join_requests_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. buildings
CREATE TABLE buildings (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    description     VARCHAR(500)    NULL,
    address         VARCHAR(255)    NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_buildings_org_code (organization_id, code),
    INDEX idx_buildings_org (organization_id),

    CONSTRAINT fk_buildings_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. blocks
CREATE TABLE blocks (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    building_id BIGINT          NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    description VARCHAR(500)    NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_blocks_building_code (building_id, code),
    INDEX idx_blocks_building (building_id),

    CONSTRAINT fk_blocks_building
        FOREIGN KEY (building_id) REFERENCES buildings(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. floors
CREATE TABLE floors (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    block_id     BIGINT          NOT NULL,
    floor_number INT             NOT NULL,
    name         VARCHAR(100)    NULL,
    description  VARCHAR(500)    NULL,
    status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_floors_block_number (block_id, floor_number),
    INDEX idx_floors_block (block_id),

    CONSTRAINT fk_floors_block
        FOREIGN KEY (block_id) REFERENCES blocks(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. units
CREATE TABLE units (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    floor_id     BIGINT          NOT NULL,
    unit_number  VARCHAR(50)     NOT NULL,
    unit_name    VARCHAR(100)    NULL,
    unit_type    VARCHAR(20)     NOT NULL,
    capacity     INT             NOT NULL DEFAULT 1,
    status       VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    description  VARCHAR(500)    NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_units_floor_number (floor_id, unit_number),
    INDEX idx_units_floor (floor_id),
    INDEX idx_units_type (unit_type),
    INDEX idx_units_status (status),

    CONSTRAINT fk_units_floor
        FOREIGN KEY (floor_id) REFERENCES floors(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_units_capacity CHECK (capacity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> **End of Organization Service Database Design**
> *Documentation only — no database created.*
