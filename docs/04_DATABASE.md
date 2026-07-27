# VOLTARAS — Database Design

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 5 — Database Design
> **Document:** `docs/04_DATABASE.md`

---

## 1. Database Design Overview

### Database-per-Service Pattern

VOLTARAS follows the **Database-per-Service** pattern, where each microservice owns and operates its own independent MySQL database. This is a core tenet of the microservices architecture established in Phase 4.

| Pattern | Description |
|---|---|
| **One Database per Service** | Each of the 7 services that persist data has a dedicated database. API Gateway and Eureka Service Registry do not own databases. |
| **Independent Schema** | Each database has its own schema, tables, indexes, and constraints — independent of all other services. |
| **Separate MySQL Instances** | In development, each database runs in its own Docker MySQL container. In production, they may use separate managed RDS instances or a shared MySQL server with separate database names. |

### Service Data Ownership

Each service is the **sole owner** and **sole writer** of its database. No other service, directly or indirectly, writes to another service's database.

| Service | Database | Ownership Scope |
|---|---|---|
| Auth Service | `auth_db` | Credentials, roles, authentication tokens |
| User Service | `user_db` | Consumer profiles, address, account details |
| Meter Service | `meter_db` | Meter registrations, readings, status |
| Billing Service | `billing_db` | Tariff plans, slabs, bills, line items |
| Payment Service | `payment_db` | Payment transactions, methods, references |
| Complaint Service | `complaint_db` | Complaints, categories, comments, status history |
| Notification Service | `notification_db` | Notifications, recipients, read status |

### Data Isolation

- **No direct cross-database access:** A service never queries, joins, or writes to another service's database directly. All cross-service data access occurs exclusively through REST API calls.
- **Logical separation:** Even if databases run on the same MySQL server, they use different database names with distinct credentials and connection pools.
- **Independent scaling:** Each database can be scaled (vertically or via read replicas) independently based on its workload.

### Service Communication for Data Access

When a service needs data owned by another service, it follows this pattern:

```
Service A (needs data) → REST API call → Service B (owns data) → queries its own DB → returns data via API response
```

**Common cross-service data flows:**

| Flow | Caller | API Provider | Data Requested |
|---|---|---|---|
| Registration | Auth Service | User Service | Create user profile |
| Bill Calculation | Billing Service | Meter Service | Fetch readings for date range |
| Bill Status Update | Payment Service | Billing Service | Mark bill as PAID |
| Notification Trigger | Notification Service | User Service | Fetch consumer details |
| Complaint Notification | Complaint Service | Notification Service | Trigger notification on status change |

### No Cross-Database Foreign Keys

- Foreign key constraints exist **only within a single database**.
- References across databases use **logical external IDs** (e.g., `auth_user_id`, `consumer_id`, `meter_id`) stored as plain BIGINT or VARCHAR columns.
- Referential integrity across services is enforced at the **application layer** (service logic validates that referenced IDs exist by calling the owning service's API).
- This avoids tight coupling between databases and allows each service to evolve its schema independently.

---

## 2. Database List

| # | Service | Database Name | Purpose | Main Tables |
|---|---|---|---|---|
| 1 | **Auth Service** | `auth_db` | Stores authentication credentials, roles, and JWT-related data. Does not store profile details — only what is needed for login/register. | `users`, `roles`, `user_roles`, `refresh_tokens` (future) |
| 2 | **User Service** | `user_db` | Stores consumer profile information, addresses, and electricity connection/account details. | `consumer_profiles`, `consumer_addresses` |
| 3 | **Meter Service** | `meter_db` | Stores meter registrations, meter-to-consumer assignments, and all meter readings submitted by consumers. | `meters`, `meter_assignments`, `meter_readings` |
| 4 | **Billing Service** | `billing_db` | Stores tariff plans, tariff slabs, generated daily bills, monthly bills, and bill line items with slab-wise breakdown. | `tariff_plans`, `tariff_slabs`, `daily_bills`, `monthly_bills`, `bill_line_items` |
| 5 | **Payment Service** | `payment_db` | Stores payment transactions, payment methods, and payment status. No real payment gateway integration in V1. | `payments`, `payment_methods` |
| 6 | **Complaint Service** | `complaint_db` | Stores consumer complaints, categories, resolution comments, and status change history. | `complaints`, `complaint_categories`, `complaint_comments`, `complaint_status_history` |
| 7 | **Notification Service** | `notification_db` | Stores in-app notifications, notification types, recipient mappings, and read/delivery status. | `notifications`, `notification_recipients` |

### Services Without Databases

| Service | Reason |
|---|---|
| **API Gateway** | Routes requests and validates JWT tokens — no persistent data. Configuration is externalized (application.yml / config server). |
| **Eureka Service Registry** | Maintains in-memory service registry with health status. No persistent storage needed — registrations are ephemeral. |

---

## 3. ER Diagrams

### 3.1 auth_db — Authentication Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         auth_db                                        │
│                                                                        │
│  ┌──────────────────────────────────────┐                              │
│  │              users                   │                              │
│  ├──────────────────────────────────────┤                              │
│  │  id              BIGINT  (PK)        │──┐                           │
│  │  email           VARCHAR(255) (UQ)   │  │                           │
│  │  password_hash   VARCHAR(255)        │  │                           │
│  │  is_active       BOOLEAN             │  │                           │
│  │  last_login_at   DATETIME            │  │                           │
│  │  created_at      DATETIME            │  │                           │
│  │  updated_at      DATETIME            │  │                           │
│  └──────────────────────────────────────┘  │                           │
│                                            │  user_roles               │
│  ┌──────────────────────────────────────┐  │                           │
│  │              roles                   │  │                           │
│  ├──────────────────────────────────────┤  │                           │
│  │  id              BIGINT  (PK)        │──┤                           │
│  │  name            VARCHAR(50) (UQ)    │  │                           │
│  │  description     VARCHAR(255)        │  │                           │
│  │  created_at      DATETIME            │  │                           │
│  └──────────────────────────────────────┘  │                           │
│                                            │                           │
│  ┌──────────────────────────────────────┐  │                           │
│  │           user_roles                 │◄─┘                           │
│  ├──────────────────────────────────────┤                              │
│  │  user_id         BIGINT  (PK, FK)    │── references users(id)      │
│  │  role_id         BIGINT  (PK, FK)    │── references roles(id)      │
│  │  created_at      DATETIME            │                              │
│  └──────────────────────────────────────┘                              │
│                                                                        │
│  ┌──────────────────────────────────────┐                              │
│  │          refresh_tokens              │  (FUTURE)                    │
│  ├──────────────────────────────────────┤                              │
│  │  id              BIGINT  (PK)        │                              │
│  │  user_id         BIGINT  (FK)        │── references users(id)      │
│  │  token           VARCHAR(512) (UQ)   │                              │
│  │  expires_at      DATETIME            │                              │
│  │  is_revoked      BOOLEAN             │                              │
│  │  created_at      DATETIME            │                              │
│  └──────────────────────────────────────┘                              │
│                                                                        │
│  KEY RELATIONSHIPS:                                                    │
│  ─────────────────                                                     │
│  • users 1──M user_roles (one user has many roles)                    │
│  • roles 1──M user_roles (one role assigned to many users)            │
│  • users 1──M refresh_tokens (one user can have many tokens) (FUTURE) │
│  • Users ↔ Roles = M:M (resolved via user_roles junction table)      │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 user_db — User Profile Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         user_db                                        │
│                                                                        │
│  ┌──────────────────────────────────────────────┐                     │
│  │            consumer_profiles                  │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  auth_user_id        BIGINT  (UQ, EXT_REF)    │  ← references       │
│  │                     (External ID — auth_db)   │    auth_db.users.id │
│  │  consumer_number     VARCHAR(50) (UQ)         │                     │
│  │  full_name           VARCHAR(100)             │                     │
│  │  email               VARCHAR(255) (UQ)        │                     │
│  │  phone               VARCHAR(20)              │                     │
│  │  is_active           BOOLEAN                  │                     │
│  │  created_at          DATETIME                 │                     │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                          │                                             │
│                          │ 1                                          │
│                          │                                             │
│                          │ M                                          │
│  ┌──────────────────────────────────────────────┐                     │
│  │           consumer_addresses                  │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  consumer_id         BIGINT  (FK)             │── references        │
│  │                      (references              │   consumer_profiles │
│  │                       consumer_profiles.id)   │   .id               │
│  │  address_line1       VARCHAR(255)             │                     │
│  │  address_line2       VARCHAR(255)             │                     │
│  │  city                VARCHAR(100)             │                     │
│  │  state               VARCHAR(100)             │                     │
│  │  pincode             VARCHAR(10)              │                     │
│  │  is_primary          BOOLEAN                  │                     │
│  │  created_at          DATETIME                 │                     │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                                                                        │
│  KEY RELATIONSHIPS:                                                    │
│  ─────────────────                                                     │
│  • consumer_profiles 1──M consumer_addresses (one consumer has         │
│    many addresses, one marked as primary)                             │
│  • auth_user_id is an EXTERNAL REFERENCE to auth_db.users.id          │
│    No foreign key constraint exists — validated via REST API call     │
│  • consumer_number is auto-generated (e.g., VOL-2026-00001)           │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.3 meter_db — Meter Reading Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         meter_db                                       │
│                                                                        │
│  ┌──────────────────────────────────────────────┐                     │
│  │                meters                         │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  meter_number        VARCHAR(50) (UQ)         │                     │
│  │  meter_type          ENUM / VARCHAR(20)       │  (e.g., DIGITAL,   │
│  │  installation_date   DATE                     │   ANALOG, SMART)   │
│  │  is_active           BOOLEAN                  │                     │
│  │  created_at          DATETIME                 │                     │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                          │                                             │
│                          │ 1                                          │
│                          │                                             │
│                          │ M                                          │
│  ┌──────────────────────────────────────────────┐                     │
│  │          meter_assignments                    │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  meter_id            BIGINT  (FK)             │── references        │
│  │                      (references meters.id)   │   meters.id         │
│  │  consumer_id         BIGINT  (EXT_REF)        │  ← references       │
│  │                      (External ID — user_db)  │    user_db          │
│  │  assigned_from       DATE                     │                     │
│  │  assigned_until      DATE                     │  (NULL = current)   │
│  │  is_active           BOOLEAN                  │                     │
│  │  created_at          DATETIME                 │                     │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                          │                                             │
│                          │ 1                                          │
│                          │                                             │
│                          │ M                                          │
│  ┌──────────────────────────────────────────────┐                     │
│  │            meter_readings                     │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  meter_assignment_id BIGINT  (FK)             │── references        │
│  │                      (references              │   meter_assignments │
│  │                       meter_assignments.id)   │   .id               │
│  │  reading_date        DATE                     │                     │
│  │  meter_value         DECIMAL(12,2)            │                     │
│  │  units_consumed      DECIMAL(10,2)            │                     │
│  │  status              ENUM / VARCHAR(20)       │  (VERIFIED,         │
│  │  remarks             VARCHAR(255)             │   SUSPICIOUS)       │
│  │  submitted_by        BIGINT  (EXT_REF)        │  ← consumer who     │
│  │  created_at          DATETIME                 │    submitted        │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                                                                        │
│  UNIQUE CONSTRAINT:                                                    │
│  (meter_assignment_id, reading_date) — one reading per day per meter  │
│                                                                        │
│  KEY RELATIONSHIPS:                                                    │
│  ─────────────────                                                     │
│  • meters 1──M meter_assignments (one meter can be assigned to         │
│    different consumers over time)                                     │
│  • meter_assignments 1──M meter_readings (one assignment has many     │
│    readings over time)                                                │
│  • consumer_id is EXTERNAL REFERENCE to user_db.consumer_profiles.id  │
│  • submitted_by is EXTERNAL REFERENCE to auth_db.users.id             │
│  • readings must be strictly increasing in meter_value for each       │
│    meter — enforced at application layer                              │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.4 billing_db — Billing Database

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         billing_db                                        │
│                                                                            │
│  ┌──────────────────────────────────────────────┐                         │
│  │              tariff_plans                     │                         │
│  ├──────────────────────────────────────────────┤                         │
│  │  id                  BIGINT  (PK)             │                         │
│  │  plan_name           VARCHAR(100)             │                         │
│  │  effective_from      DATE                     │                         │
│  │  effective_to        DATE                     │  (NULL = current)       │
│  │  fixed_charges       DECIMAL(10,2)            │                         │
│  │  tax_percentage      DECIMAL(5,2)             │                         │
│  │  is_active           BOOLEAN                  │                         │
│  │  created_at          DATETIME                 │                         │
│  │  updated_at          DATETIME                 │                         │
│  └──────────────────────────────────────────────┘                         │
│                          │                                                 │
│                          │ 1                                              │
│                          │                                                 │
│                          │ M                                              │
│  ┌──────────────────────────────────────────────┐                         │
│  │              tariff_slabs                     │                         │
│  ├──────────────────────────────────────────────┤                         │
│  │  id                  BIGINT  (PK)             │                         │
│  │  tariff_plan_id      BIGINT  (FK)             │── references            │
│  │                      (references              │   tariff_plans.id       │
│  │                       tariff_plans.id)        │                         │
│  │  slab_name           VARCHAR(100)             │  (e.g., "0-100 units") │
│  │  unit_from           INT                      │                         │
│  │  unit_to             INT                      │  (NULL = unlimited)     │
│  │  rate_per_unit       DECIMAL(8,2)             │                         │
│  │  created_at          DATETIME                 │                         │
│  │  updated_at          DATETIME                 │                         │
│  └──────────────────────────────────────────────┘                         │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                      monthly_bills                                   │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │  id                  BIGINT  (PK)                                     │ │
│  │  bill_number         VARCHAR(50) (UQ)                                 │ │
│  │  consumer_id         BIGINT  (EXT_REF)      ← user_db                 │ │
│  │  billing_month       DATE                     (1st of month)          │ │
│  │  total_units         DECIMAL(10,2)                                    │ │
│  │  total_energy_charge DECIMAL(12,2)                                    │ │
│  │  fixed_charges       DECIMAL(10,2)                                    │ │
│  │  tax_amount          DECIMAL(10,2)                                    │ │
│  │  total_amount        DECIMAL(12,2)                                    │ │
│  │  status              ENUM / VARCHAR(20)      (PAID, UNPAID, PARTIAL)  │ │
│  │  generated_by        BIGINT  (EXT_REF)      ← auth_db (admin user)    │ │
│  │  generated_at        DATETIME                                         │ │
│  │  paid_at             DATETIME                                         │ │
│  │  created_at          DATETIME                                         │ │
│  │  updated_at          DATETIME                                         │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                          │                                                 │
│                          │ 1                                              │
│                          │                                                 │
│                          │ M                                              │
│  ┌──────────────────────────────────────────────┐                         │
│  │           bill_line_items                     │                         │
│  ├──────────────────────────────────────────────┤                         │
│  │  id                  BIGINT  (PK)             │                         │
│  │  monthly_bill_id     BIGINT  (FK)             │── references            │
│  │                      (references              │   monthly_bills.id      │
│  │                       monthly_bills.id)       │                         │
│  │  slab_name           VARCHAR(100)             │                         │
│  │  units_in_slab       DECIMAL(10,2)            │                         │
│  │  rate_per_unit       DECIMAL(8,2)             │                         │
│  │  line_amount         DECIMAL(12,2)            │                         │
│  │  created_at          DATETIME                 │                         │
│  └──────────────────────────────────────────────┘                         │
│                                                                            │
│  ┌──────────────────────────────────────────────┐                         │
│  │             daily_bills                       │                         │
│  ├──────────────────────────────────────────────┤                         │
│  │  id                  BIGINT  (PK)             │                         │
│  │  meter_reading_id    BIGINT  (EXT_REF)        │  ← meter_db            │
│  │  consumer_id         BIGINT  (EXT_REF)        │  ← user_db             │
│  │  bill_date           DATE                     │                         │
│  │  units_consumed      DECIMAL(10,2)            │                         │
│  │  amount              DECIMAL(10,2)            │                         │
│  │  status              ENUM / VARCHAR(20)       │  (PAID, UNPAID)         │
│  │  created_at          DATETIME                 │                         │
│  │  updated_at          DATETIME                 │                         │
│  └──────────────────────────────────────────────┘                         │
│                                                                            │
│  UNIQUE CONSTRAINTS:                                                       │
│  ─────────────────────                                                       │
│  • (consumer_id, billing_month) — one monthly bill per consumer per month  │
│  • (meter_reading_id) — one daily bill per reading                         │
│  • tariff_slabs: (tariff_plan_id, unit_from) — no overlap per plan        │
│                                                                            │
│  KEY RELATIONSHIPS:                                                        │
│  ─────────────────                                                         │
│  • tariff_plans 1──M tariff_slabs (one plan has many slabs)               │
│  • monthly_bills 1──M bill_line_items (one bill has many slab-wise rows)  │
│  • consumer_id is EXTERNAL REFERENCE to user_db.consumer_profiles.id      │
│  • meter_reading_id is EXTERNAL REFERENCE to meter_db.meter_readings.id   │
│  • generated_by is EXTERNAL REFERENCE to auth_db.users.id (admin)         │
│  • Daily bills are generated per reading; monthly bills aggregate them    │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.5 payment_db — Payment Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         payment_db                                     │
│                                                                        │
│  ┌──────────────────────────────────────────────┐                     │
│  │            payment_methods                    │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  method_name         VARCHAR(50) (UQ)         │  (CASH, BANK_       │
│  │  is_active           BOOLEAN                  │   TRANSFER, CARD)   │
│  │  created_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                          │                                             │
│                          │ 1                                          │
│                          │                                             │
│                          │ M                                          │
│  ┌──────────────────────────────────────────────┐                     │
│  │              payments                         │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  transaction_id      VARCHAR(100) (UQ)        │  (auto-generated)   │
│  │  bill_id             BIGINT  (EXT_REF)        │  ← billing_db       │
│  │  consumer_id         BIGINT  (EXT_REF)        │    (monthly_bills   │
│  │  amount              DECIMAL(12,2)            │     .id)            │
│  │  payment_method_id   BIGINT  (FK)             │── references        │
│  │                      (references              │   payment_methods   │
│  │                       payment_methods.id)     │   .id               │
│  │  transaction_ref     VARCHAR(255)             │  (optional manual   │
│  │  status              ENUM / VARCHAR(20)       │   ref from payer)   │
│  │  paid_at             DATETIME                 │  (COMPLETED,        │
│  │  created_at          DATETIME                 │   FAILED, REFUNDED) │
│  │  updated_at          DATETIME                 │                     │
│  └──────────────────────────────────────────────┘                     │
│                                                                        │
│  UNIQUE CONSTRAINT:                                                    │
│  ────────────────────                                                  │
│  • (bill_id, status = COMPLETED) — one successful payment per bill     │
│    (enforced via application layer since bill_id is external ref)      │
│                                                                        │
│  KEY RELATIONSHIPS:                                                    │
│  ─────────────────                                                     │
│  • payment_methods 1──M payments (one method used in many payments)   │
│  • bill_id is EXTERNAL REFERENCE to billing_db.monthly_bills.id       │
│  • consumer_id is EXTERNAL REFERENCE to user_db.consumer_profiles.id  │
│  • V1 uses simulated/manual payment recording — no real gateway       │
│  • Payment Service calls Billing Service API to mark bill as PAID     │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.6 complaint_db — Complaint Database

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         complaint_db                                      │
│                                                                            │
│  ┌──────────────────────────────────────────────┐                         │
│  │          complaint_categories                 │                         │
│  ├──────────────────────────────────────────────┤                         │
│  │  id                  BIGINT  (PK)             │                         │
│  │  name                VARCHAR(50) (UQ)         │  (BILLING_ISSUE,        │
│  │  description         VARCHAR(255)             │   METER_ISSUE,          │
│  │  is_active           BOOLEAN                  │   PAYMENT_ISSUE, OTHER) │
│  │  created_at          DATETIME                 │                         │
│  └──────────────────────────────────────────────┘                         │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                         complaints                                   │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │  id                  BIGINT  (PK)                                     │ │
│  │  ticket_number       VARCHAR(50) (UQ)     (e.g., CMP-20260727-0001)   │ │
│  │  consumer_id         BIGINT  (EXT_REF)    ← user_db                   │ │
│  │  category_id         BIGINT  (FK)         ── references               │ │
│  │                      (references           complaint_categories.id    │ │
│  │                       complaint_categories.id)                        │ │
│  │  subject             VARCHAR(200)                                     │ │
│  │  description         TEXT                                              │ │
│  │  status              ENUM / VARCHAR(20)  (OPEN, IN_PROGRESS,          │ │
│  │  priority            ENUM / VARCHAR(10)   RESOLVED, CLOSED)           │ │
│  │  assigned_to         BIGINT  (EXT_REF)   ← auth_db (admin user)       │ │
│  │  resolved_at         DATETIME                                         │ │
│  │  created_at          DATETIME                                         │ │
│  │  updated_at          DATETIME                                         │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                          │                                                 │
│                          │ 1                                              │
│                          │                                                 │
│                          │ M                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                    complaint_comments                                  │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │  id                  BIGINT  (PK)                                     │ │
│  │  complaint_id        BIGINT  (FK)         ── references               │ │
│  │                      (references           complaints.id              │ │
│  │                       complaints.id)                                  │ │
│  │  comment_text        TEXT                                              │ │
│  │  author_id           BIGINT  (EXT_REF)    ← auth_db (user or admin)   │ │
│  │  is_admin_comment    BOOLEAN                                          │ │
│  │  created_at          DATETIME                                         │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │               complaint_status_history                                 │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │  id                  BIGINT  (PK)                                     │ │
│  │  complaint_id        BIGINT  (FK)         ── references               │ │
│  │                      (references           complaints.id              │ │
│  │                       complaints.id)                                  │ │
│  │  from_status         VARCHAR(20)                                      │ │
│  │  to_status           VARCHAR(20)                                      │ │
│  │  changed_by          BIGINT  (EXT_REF)    ← auth_db (who changed)     │ │
│  │  changed_at          DATETIME                                         │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  KEY RELATIONSHIPS:                                                        │
│  ─────────────────                                                         │
│  • complaint_categories 1──M complaints (one category has many complaints)│
│  • complaints 1──M complaint_comments (one complaint has many comments)   │
│  • complaints 1──M complaint_status_history (audit trail of status        │
│    changes)                                                               │
│  • consumer_id is EXTERNAL REFERENCE to user_db.consumer_profiles.id      │
│  • assigned_to is EXTERNAL REFERENCE to auth_db.users.id (admin)          │
│  • author_id and changed_by are EXTERNAL REFERENCES to auth_db.users.id   │
│  • Status transition: OPEN → IN_PROGRESS → RESOLVED → CLOSED              │
│  • When status changes, Complaint Service calls Notification Service      │
│    to notify the consumer                                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.7 notification_db — Notification Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         notification_db                                │
│                                                                        │
│  ┌──────────────────────────────────────────────┐                     │
│  │            notifications                      │                     │
│  ├──────────────────────────────────────────────┤                     │
│  │  id                  BIGINT  (PK)             │                     │
│  │  title               VARCHAR(200)             │                     │
│  │  message             TEXT                     │                     │
│  │  type                ENUM / VARCHAR(20)       │  (INFO, WARNING,    │
│  │  reference_type      VARCHAR(50)              │   ALERT)            │
│  │  reference_id        BIGINT                   │  (e.g., BILL,       │
│  │  created_by          BIGINT  (EXT_REF)        │   PAYMENT, COMPLAINT│
│  │  created_at          DATETIME                 │   )                 │
│  └──────────────────────────────────────────────┘  ← identifies the    │
│                          │                          source entity ID   │
│                          │ 1                                            │
│                          │                                              │
│                          │ M                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐
│  │                     notification_recipients                             │
│  ├──────────────────────────────────────────────────────────────────────────┤
│  │  id                  BIGINT  (PK)                                        │
│  │  notification_id     BIGINT  (FK)         ── references                 │
│  │                      (references           notifications.id             │
│  │                       notifications.id)                                 │
│  │  consumer_id         BIGINT  (EXT_REF)    ← user_db.consumer_profiles   │
│  │  is_read             BOOLEAN              (default: FALSE)               │
│  │  read_at             DATETINE                                          │
│  │  created_at          DATETIME                                          │
│  └──────────────────────────────────────────────────────────────────────────┘
│                                                                        │
│  INDEXES:                                                              │
│  ────────                                                              │
│  • notification_recipients(consumer_id, is_read) — quick lookup of     │
│    unread notifications for a consumer                                 │
│  • notification_recipients(consumer_id, created_at) — paginated        │
│    notification list sorted by date                                    │
│                                                                        │
│  KEY RELATIONSHIPS:                                                    │
│  ─────────────────                                                     │
│  • notifications 1──M notification_recipients (one notification sent   │
│    to many consumers for broadcast; one recipient for targeted)        │
│  • consumer_id is EXTERNAL REFERENCE to user_db.consumer_profiles.id  │
│  • created_by is EXTERNAL REFERENCE to auth_db.users.id (admin or      │
│    system-generated)                                                   │
│  • V1 notifications are in-app only — no SMS/email (out of scope)      │
│  • Broadcast = insert one notification + N recipient rows             │
│  • Targeted = insert one notification + 1 recipient row               │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Cross-Database External References Summary

The following diagram illustrates how external IDs reference data across service boundaries without foreign key constraints:

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   auth_db    │       │   user_db    │       │   meter_db   │
│              │       │              │       │              │
│  users.id ◄──┼───────┼── auth_user_id           │              │
│              │       │              │       │  consumer_id │
│              │       │ consumer_    │       │◄─────────────┤
│              │       │ profiles.id  │       │  meter_db    │
│              │       │              │       │              │
└──────────────┘       └──────┬───────┘       └──────────────┘
                              │
                    ┌─────────┼─────────┐
                    │         │         │
                    ▼         ▼         ▼
           ┌────────────┐ ┌────────┐ ┌────────────┐
           │ billing_db │ │payment │ │complaint_db│
           │            │ │ _db    │ │            │
           │ consumer_id│ │consumer│ │ consumer_id│
           │            │ │ _id    │ │            │
           │ generated_ │ │ bill_id│ │ assigned_to│
           │ by         │ │        │ │            │
           └────────────┘ └────────┘ └────────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │ notification_db │
                     │                 │
                     │ consumer_id     │
                     │ created_by      │
                     └────────────────┘
```

**Key rule:** All external references are stored as plain `BIGINT` or `VARCHAR` columns. Referential integrity is maintained through application-level validation (service calls to the owning service's REST API).

---

> **End of Phase 5 — Sections 1, 2, 3**
> *Sections 4–5 follow below.*

---

## 4. auth_db Design

### 4.1 Purpose

The `auth_db` database is owned by the **Auth Service** and stores all authentication and authorization-related data. It is the **only** database that holds:

- Login credentials (email + hashed password)
- Role assignments (CONSUMER or ADMIN)
- JWT and refresh token data (future)

No profile information, addresses, or business data is stored here. The `auth_db` is strictly security-focused.

**Key design rules:**
- Passwords are **never** stored in plain text — only BCrypt hashes.
- Email is the unique user identifier for login.
- Role is stored via a junction table (user_roles) to support future multi-role expansion.
- The `auth_user_id` produced by this service is propagated to other services as an external reference.

---

### 4.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `users` | Core | Stores login credentials and authentication status |
| 2 | `roles` | Lookup | Predefined roles (CONSUMER, ADMIN) |
| 3 | `user_roles` | Junction | M:M relationship between users and roles |
| 4 | `refresh_tokens` | Future | Refresh token storage (V2 — marked as future) |

---

### 4.3 Table: `users`

The central authentication table. One row per registered user.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key, internal unique identifier |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | — | User's email address (login identifier) |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | — | BCrypt hash of the user's password |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether the account is active (admin can deactivate) |
| `password_updated_at` | `DATETIME` | `NULLABLE` | `NULL` | Timestamp of last password change |
| `last_login_at` | `DATETIME` | `NULLABLE` | `NULL` | Timestamp of last successful login |
| `last_login_ip` | `VARCHAR(45)` | `NULLABLE` | `NULL` | IP address of last login (IPv4 or IPv6) |
| `login_attempts` | `INT` | `NOT NULL` | `0` | Consecutive failed login attempts (for rate-limiting) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_users_email` | `email` | UNIQUE | Fast login lookup by email |
| `idx_users_is_active` | `is_active` | NON-UNIQUE | Filter active/deactivated accounts |
| `idx_users_last_login` | `last_login_at` | NON-UNIQUE | Admin view of last login activity |

#### Relationships

| Relationship | Related Table | Type | Via |
|---|---|---|---|
| Users → Roles | `roles` | M:M | `user_roles` junction table |
| Users → Refresh Tokens | `refresh_tokens` | 1:M | `users.id` → `refresh_tokens.user_id` (FUTURE) |

#### Validation Rules

| Field | Rule |
|---|---|
| `email` | Must be a valid email format; max 255 characters; unique across all users |
| `password_hash` | Must be a valid BCrypt hash (60 characters); never stored in plain text |
| `is_active` | Only active users can log in; deactivated users receive "account disabled" error |
| `login_attempts` | Reset to 0 on successful login; cap at application level (e.g., 5 attempts triggers temporary lock) |

---

### 4.4 Table: `roles`

A lookup table for predefined system roles.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `name` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Role name (e.g., CONSUMER, ADMIN) |
| `description` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Human-readable description of the role |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_roles_name` | `name` | UNIQUE | Fast lookup by role name |

#### Seed Data (Optional — Documentation Only)

```sql
-- Predefined roles for VOLTARAS (documentation only)
INSERT INTO roles (name, description) VALUES
('CONSUMER', 'Regular electricity consumer who can submit readings, view bills, make payments, and raise complaints'),
('ADMIN', 'System administrator who can manage users, tariff slabs, generate bills, resolve complaints, and view reports');
```

---

### 4.5 Table: `user_roles`

Junction table that resolves the M:M relationship between users and roles.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `user_id` | `BIGINT` | `PK`, `NOT NULL`, `FK → users(id)` | — | Reference to the user |
| `role_id` | `BIGINT` | `PK`, `NOT NULL`, `FK → roles(id)` | — | Reference to the role |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the role was assigned |

#### Composite Primary Key

```
PRIMARY KEY (user_id, role_id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_user_roles_user` | `user_id` | `users(id)` | CASCADE |
| `fk_user_roles_role` | `role_id` | `roles(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_user_roles_role_id` | `role_id` | NON-UNIQUE | Reverse lookup: find all users with a given role |

#### Relationships

- `users` 1──M `user_roles` (one user can have multiple roles)
- `roles` 1──M `user_roles` (one role can be assigned to multiple users)
- Users ↔ Roles = M:M resolved through this junction table

---

### 4.6 Table: `refresh_tokens` (Future — V2)

> **Note:** This table is **not part of V1**. It is documented here for future reference when refresh token rotation is implemented.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `user_id` | `BIGINT` | `NOT NULL`, `FK → users(id)` | — | Reference to the token owner |
| `token` | `VARCHAR(512)` | `NOT NULL`, `UNIQUE` | — | The refresh token value (hashed in DB) |
| `expires_at` | `DATETIME` | `NOT NULL` | — | Token expiration timestamp |
| `is_revoked` | `BOOLEAN` | `NOT NULL` | `FALSE` | Whether the token has been revoked |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the token was issued |
| `revoked_at` | `DATETIME` | `NULLABLE` | `NULL` | When the token was revoked (if applicable) |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_refresh_tokens_user` | `user_id` | `users(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_refresh_tokens_token` | `token` | UNIQUE | Fast token lookup on refresh |
| `idx_refresh_tokens_user_id` | `user_id` | NON-UNIQUE | Find all tokens for a user (for revocation) |
| `idx_refresh_tokens_expires` | `expires_at` | NON-UNIQUE | Cleanup expired tokens |

---

### 4.7 Relationships Summary (auth_db)

```
┌────────────────────────────────────────────────────────────────────┐
│                      auth_db RELATIONSHIPS                          │
│                                                                     │
│  ┌─────────┐       ┌──────────────┐       ┌─────────┐             │
│  │  users  │──M──M──│  user_roles  │──M──M──│  roles  │            │
│  └────┬────┘       └──────────────┘       └─────────┘             │
│       │                                                            │
│       │ 1:M (FUTURE)                                               │
│       │                                                            │
│       ▼                                                            │
│  ┌────────────────┐                                               │
│  │ refresh_tokens │  (V2 — Future Enhancement)                     │
│  └────────────────┘                                               │
└────────────────────────────────────────────────────────────────────┘
```

---

### 4.8 Complete Index List (auth_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `users` | `PRIMARY` | `id` | PRIMARY |
| `users` | `idx_users_email` | `email` | UNIQUE |
| `users` | `idx_users_is_active` | `is_active` | NON-UNIQUE |
| `users` | `idx_users_last_login` | `last_login_at` | NON-UNIQUE |
| `roles` | `PRIMARY` | `id` | PRIMARY |
| `roles` | `idx_roles_name` | `name` | UNIQUE |
| `user_roles` | `PRIMARY` | `(user_id, role_id)` | PRIMARY (composite) |
| `user_roles` | `idx_user_roles_role_id` | `role_id` | NON-UNIQUE |
| `refresh_tokens` | `PRIMARY` | `id` | PRIMARY |
| `refresh_tokens` | `idx_refresh_tokens_token` | `token` | UNIQUE |
| `refresh_tokens` | `idx_refresh_tokens_user_id` | `user_id` | NON-UNIQUE |
| `refresh_tokens` | `idx_refresh_tokens_expires` | `expires_at` | NON-UNIQUE |

---

### 4.9 Sample MySQL Schema — auth_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: auth_db
-- PURPOSE:  Authentication credentials, roles, and tokens
-- OWNER:    Auth Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS auth_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE auth_db;

-- -----------------------------------------
-- TABLE: users
-- Stores login credentials and auth status
-- -----------------------------------------
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    password_updated_at DATETIME    NULL,
    last_login_at   DATETIME        NULL,
    last_login_ip   VARCHAR(45)     NULL,
    login_attempts  INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_users_email (email),
    INDEX idx_users_is_active (is_active),
    INDEX idx_users_last_login (last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: roles
-- Lookup table for system roles
-- -----------------------------------------
CREATE TABLE roles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: user_roles
-- Junction: M:M relationship between users and roles
-- -----------------------------------------
CREATE TABLE user_roles (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_roles_role_id (role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: refresh_tokens (FUTURE - V2)
-- Refresh token storage for JWT rotation
-- -----------------------------------------
-- CREATE TABLE refresh_tokens (
--     id              BIGINT          NOT NULL AUTO_INCREMENT,
--     user_id         BIGINT          NOT NULL,
--     token           VARCHAR(512)    NOT NULL,
--     expires_at      DATETIME        NOT NULL,
--     is_revoked      BOOLEAN         NOT NULL DEFAULT FALSE,
--     created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     revoked_at      DATETIME        NULL,
--
--     PRIMARY KEY (id),
--     UNIQUE INDEX idx_refresh_tokens_token (token),
--     INDEX idx_refresh_tokens_user_id (user_id),
--     INDEX idx_refresh_tokens_expires (expires_at),
--
--     CONSTRAINT fk_refresh_tokens_user
--         FOREIGN KEY (user_id) REFERENCES users(id)
--         ON DELETE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 5. user_db Design

### 5.1 Purpose

The `user_db` database is owned by the **User Service** and stores all consumer profile information and address data. This is the **profile and account** database.

**Key design rules:**
- No authentication data is stored here (no passwords, no JWT, no tokens).
- `auth_user_id` is the external reference linking back to `auth_db.users.id`.
- `consumer_number` is a human-readable, auto-generated account identifier.
- Email is stored here for display purposes only — authentication still uses `auth_db`.
- Profile status (`is_active`) is replicated from auth for quick authorization checks, but the source of truth for active/inactive is `auth_db.users.is_active`.

---

### 5.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `consumer_profiles` | Core | Stores primary consumer profile information |
| 2 | `consumer_addresses` | Child | Stores one or more addresses per consumer |

---

### 5.3 Table: `consumer_profiles`

The primary profile table. One row per registered consumer.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key, internal unique identifier |
| `auth_user_id` | `BIGINT` | `NOT NULL`, `UNIQUE` | — | External reference to `auth_db.users.id` (no FK constraint) |
| `consumer_number` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Auto-generated human-readable account number (e.g., VOL-2026-00001) |
| `full_name` | `VARCHAR(100)` | `NOT NULL` | — | Consumer's full name |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | — | Email for display/contact (authenticated via auth_db) |
| `phone` | `VARCHAR(20)` | `NULLABLE` | `NULL` | Contact phone number |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Profile active status (mirrored from auth_db for convenience) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_consumer_profiles_auth_user_id` | `auth_user_id` | UNIQUE | Fast lookup by auth reference (1:1 with auth_db.users) |
| `idx_consumer_profiles_consumer_number` | `consumer_number` | UNIQUE | Lookup by human-readable account number |
| `idx_consumer_profiles_email` | `email` | UNIQUE | Lookup by email for display/profile operations |
| `idx_consumer_profiles_phone` | `phone` | NON-UNIQUE | Search by phone number |
| `idx_consumer_profiles_full_name` | `full_name` | NON-UNIQUE | Search/filter by name |
| `idx_consumer_profiles_is_active` | `is_active` | NON-UNIQUE | Filter active/inactive consumers |

#### Relationships

| Relationship | Related Table | Type | Via |
|---|---|---|---|
| Consumer → Addresses | `consumer_addresses` | 1:M | `consumer_profiles.id` → `consumer_addresses.consumer_id` |
| Consumer → Auth User | `auth_db.users` | 1:1 (external) | `consumer_profiles.auth_user_id` → logical reference only |

#### Validation Rules

| Field | Rule |
|---|---|
| `auth_user_id` | Must be a valid, existing user ID in `auth_db.users` (validated via REST call to Auth Service during registration) |
| `consumer_number` | Auto-generated format: `VOL-YYYY-NNNNNN` (e.g., VOL-2026-000001); unique and read-only after creation |
| `full_name` | 2–100 characters; must not be empty |
| `email` | Valid email format; max 255 characters; unique |
| `phone` | 10-digit format with optional country code; nullable |

---

### 5.4 Table: `consumer_addresses`

Stores addresses associated with a consumer profile. Each consumer can have multiple addresses, with one marked as primary.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `consumer_id` | `BIGINT` | `NOT NULL`, `FK → consumer_profiles(id)` | — | Reference to the owning consumer profile |
| `address_line1` | `VARCHAR(255)` | `NOT NULL` | — | Primary address line (street, building, etc.) |
| `address_line2` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Secondary address line (area, landmark, etc.) |
| `city` | `VARCHAR(100)` | `NOT NULL` | — | City or town |
| `state` | `VARCHAR(100)` | `NOT NULL` | — | State or province |
| `pincode` | `VARCHAR(10)` | `NOT NULL` | — | Postal/ZIP code |
| `is_primary` | `BOOLEAN` | `NOT NULL` | `FALSE` | Whether this is the consumer's primary address |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_consumer_addresses_profile` | `consumer_id` | `consumer_profiles(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_consumer_addresses_consumer_id` | `consumer_id` | NON-UNIQUE | Find all addresses for a consumer |
| `idx_consumer_addresses_primary` | `consumer_id, is_primary` | NON-UNIQUE | Quick lookup of primary address |
| `idx_consumer_addresses_pincode` | `pincode` | NON-UNIQUE | Geographic lookup/filtering |

#### Relationships

- `consumer_profiles` 1──M `consumer_addresses` (one consumer has many addresses)
- Only one address per consumer should have `is_primary = TRUE` (enforced at application layer, not via constraint, to allow temporary state during updates)

#### Validation Rules

| Field | Rule |
|---|---|
| `consumer_id` | Must reference an existing `consumer_profiles.id` |
| `address_line1` | Max 255 characters; required |
| `city` | Max 100 characters; required |
| `state` | Max 100 characters; required |
| `pincode` | 6-digit numeric format (India) or appropriate format; max 10 characters |
| `is_primary` | Only one address per consumer should be primary at any given time |

---

### 5.5 Relationships Summary (user_db)

```
┌────────────────────────────────────────────────────────────────────┐
│                      user_db RELATIONSHIPS                          │
│                                                                     │
│  ┌──────────────────────┐                                          │
│  │   consumer_profiles   │                                          │
│  │                       │                                          │
│  │  auth_user_id ────────┼──── (external ref to auth_db.users.id)  │
│  │  (EXT_REF, 1:1)      │                                          │
│  └──────────┬───────────┘                                          │
│             │                                                       │
│             │ 1                                                     │
│             │                                                       │
│             │ M                                                     │
│  ┌──────────▼───────────┐                                          │
│  │ consumer_addresses    │                                          │
│  │                       │                                          │
│  │ consumer_id (FK) ─────┼── references consumer_profiles.id       │
│  │ is_primary (boolean)  │                                          │
│  └───────────────────────┘                                          │
│                                                                     │
│  KEY EXTERNAL REFERENCES:                                          │
│  ─────────────────────────                                          │
│  • consumer_profiles.auth_user_id → auth_db.users.id               │
│    (1:1 relationship — one user profile per auth user)             │
│    NOT a database FK — validated via REST API to Auth Service      │
└────────────────────────────────────────────────────────────────────┘
```

---

### 5.6 Complete Index List (user_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `consumer_profiles` | `PRIMARY` | `id` | PRIMARY |
| `consumer_profiles` | `idx_consumer_profiles_auth_user_id` | `auth_user_id` | UNIQUE |
| `consumer_profiles` | `idx_consumer_profiles_consumer_number` | `consumer_number` | UNIQUE |
| `consumer_profiles` | `idx_consumer_profiles_email` | `email` | UNIQUE |
| `consumer_profiles` | `idx_consumer_profiles_phone` | `phone` | NON-UNIQUE |
| `consumer_profiles` | `idx_consumer_profiles_full_name` | `full_name` | NON-UNIQUE |
| `consumer_profiles` | `idx_consumer_profiles_is_active` | `is_active` | NON-UNIQUE |
| `consumer_addresses` | `PRIMARY` | `id` | PRIMARY |
| `consumer_addresses` | `idx_consumer_addresses_consumer_id` | `consumer_id` | NON-UNIQUE |
| `consumer_addresses` | `idx_consumer_addresses_primary` | `consumer_id, is_primary` | NON-UNIQUE |
| `consumer_addresses` | `idx_consumer_addresses_pincode` | `pincode` | NON-UNIQUE |

---

### 5.7 Sample MySQL Schema — user_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: user_db
-- PURPOSE:  Consumer profiles, addresses, and account details
-- OWNER:    User Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE user_db;

-- -----------------------------------------
-- TABLE: consumer_profiles
-- Stores primary consumer profile information
-- -----------------------------------------
CREATE TABLE consumer_profiles (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    auth_user_id      BIGINT          NOT NULL,
    consumer_number   VARCHAR(50)     NOT NULL,
    full_name         VARCHAR(100)    NOT NULL,
    email             VARCHAR(255)    NOT NULL,
    phone             VARCHAR(20)     NULL,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_consumer_profiles_auth_user_id (auth_user_id),
    UNIQUE INDEX idx_consumer_profiles_consumer_number (consumer_number),
    UNIQUE INDEX idx_consumer_profiles_email (email),
    INDEX idx_consumer_profiles_phone (phone),
    INDEX idx_consumer_profiles_full_name (full_name),
    INDEX idx_consumer_profiles_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: consumer_addresses
-- Stores one or more addresses per consumer
-- -----------------------------------------
CREATE TABLE consumer_addresses (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    consumer_id       BIGINT          NOT NULL,
    address_line1     VARCHAR(255)    NOT NULL,
    address_line2     VARCHAR(255)    NULL,
    city              VARCHAR(100)    NOT NULL,
    state             VARCHAR(100)    NOT NULL,
    pincode           VARCHAR(10)     NOT NULL,
    is_primary        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_consumer_addresses_consumer_id (consumer_id),
    INDEX idx_consumer_addresses_primary (consumer_id, is_primary),
    INDEX idx_consumer_addresses_pincode (pincode),

    CONSTRAINT fk_consumer_addresses_profile
        FOREIGN KEY (consumer_id) REFERENCES consumer_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

> **End of Phase 5 — Sections 4 & 5**
> *Sections 6–7 follow below.*

---

## 6. meter_db Design

### 6.1 Purpose

The `meter_db` database is owned by the **Meter Service** and stores all meter-related data including physical meter records, meter-to-consumer assignments over time, and all submitted meter readings. This is the **consumption data** database.

**Key design rules:**
- A `meters` record represents a physical electricity meter installed at a location.
- `meter_assignments` tracks which consumer is associated with a meter over a date range (supports meter reassignment).
- `meter_readings` stores daily readings submitted by consumers with strict validation rules.
- `consumer_id` and `submitted_by` are external references (no FK constraints).

**Validation rules enforced at application layer:**
- `meter_value` must be >= 0 (no negative readings).
- `meter_value` must be strictly greater than the previous reading (current > previous).
- One reading per `meter_assignment_id` per `reading_date` — no duplicate daily readings.
- `units_consumed` = current `meter_value` − previous `meter_value`.

---

### 6.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `meters` | Core | Physical meter registry (one row per installed meter) |
| 2 | `meter_assignments` | Child/Junction | Maps a meter to a consumer for a specific date range |
| 3 | `meter_readings` | Child | Daily meter readings submitted by consumers |

---

### 6.3 Table: `meters`

Represents a physical electricity meter installed at a consumer location. Meters are registered in the system and can be reassigned to different consumers over time.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key, internal unique identifier |
| `meter_number` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Physical meter serial number (unique, human-readable) |
| `meter_type` | `VARCHAR(20)` | `NOT NULL` | — | Type of meter (e.g., `DIGITAL`, `ANALOG`, `SMART`) |
| `installation_date` | `DATE` | `NULLABLE` | `NULL` | Date the meter was installed |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether the meter is currently active/decommissioned |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_meters_meter_number` | `meter_number` | UNIQUE | Fast lookup by physical meter serial number |
| `idx_meters_is_active` | `is_active` | NON-UNIQUE | Filter active/decommissioned meters |
| `idx_meters_meter_type` | `meter_type` | NON-UNIQUE | Filter by meter type for reporting |

#### Validation Rules

| Field | Rule |
|---|---|
| `meter_number` | Must be unique; alphanumeric with optional hyphens; max 50 characters |
| `meter_type` | One of: `DIGITAL`, `ANALOG`, `SMART` (enforced via ENUM or application check) |

---

### 6.4 Table: `meter_assignments`

Tracks which consumer is assigned to which meter over what time period. This allows meter reassignment history to be preserved.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `meter_id` | `BIGINT` | `NOT NULL`, `FK → meters(id)` | — | Reference to the physical meter |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `assigned_from` | `DATE` | `NOT NULL` | — | Start date of the assignment |
| `assigned_until` | `DATE` | `NULLABLE` | `NULL` | End date of the assignment (`NULL` = currently active) |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether this assignment is currently active |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_meter_assignments_meter` | `meter_id` | `meters(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_meter_assignments_meter_id` | `meter_id` | NON-UNIQUE | Find all assignments for a meter (history) |
| `idx_meter_assignments_consumer_id` | `consumer_id` | NON-UNIQUE | Find current/historical meter for a consumer (EXT_REF) |
| `idx_meter_assignments_active` | `meter_id, is_active` | NON-UNIQUE | Quick lookup of currently active assignment per meter |
| `idx_meter_assignments_dates` | `consumer_id, assigned_from` | NON-UNIQUE | Check for overlapping assignment dates |

#### Relationships

- `meters` 1──M `meter_assignments` (one meter can have multiple assignments over time)
- A consumer can only have one active assignment at any given time (enforced at application layer)
- `consumer_id` is an external reference to `user_db.consumer_profiles.id`

#### Validation Rules

| Field | Rule |
|---|---|
| `meter_id` | Must reference an existing `meters.id` |
| `consumer_id` | Must reference an existing consumer in `user_db` (validated via REST call to User Service) |
| `assigned_from`, `assigned_until` | `assigned_until` must be >= `assigned_from` if provided; `NULL` `assigned_until` = currently active |
| Overlap | A meter cannot have two active assignments with overlapping date ranges |

---

### 6.5 Table: `meter_readings`

Stores daily meter readings submitted by consumers. This is the core data table from which consumption and bills are calculated.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `meter_assignment_id` | `BIGINT` | `NOT NULL`, `FK → meter_assignments(id)` | — | Reference to the active meter assignment |
| `reading_date` | `DATE` | `NOT NULL` | — | Date of the reading submission |
| `meter_value` | `DECIMAL(12,2)` | `NOT NULL` | — | Current meter reading value (in kWh or units) |
| `units_consumed` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Calculated units: current `meter_value` − previous `meter_value` |
| `status` | `VARCHAR(20)` | `NOT NULL` | `VERIFIED` | Status: `VERIFIED` (normal), `SUSPICIOUS` (flagged by admin) |
| `remarks` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Admin remarks when flagging as suspicious |
| `submitted_by` | `BIGINT` | `NOT NULL` | — | External reference to `auth_db.users.id` (consumer who submitted) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_meter_readings_assignment` | `meter_assignment_id` | `meter_assignments(id)` | CASCADE |

#### Unique Constraints

```
UNIQUE KEY uq_reading_per_day (meter_assignment_id, reading_date)
```

This enforces the business rule: **one reading per meter per day**.

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_meter_readings_assignment_id` | `meter_assignment_id` | NON-UNIQUE | Find all readings for an assignment |
| `idx_meter_readings_reading_date` | `reading_date` | NON-UNIQUE | Filter readings by date (admin reports) |
| `idx_meter_readings_status` | `status` | NON-UNIQUE | Filter by status (VERIFIED / SUSPICIOUS) |
| `idx_meter_readings_assignment_date` | `meter_assignment_id, reading_date` | UNIQUE | (same as unique constraint) ensure no duplicate daily reading |
| `idx_meter_readings_submitted_by` | `submitted_by` | NON-UNIQUE | Find all readings submitted by a specific user (EXT_REF) |

#### Relationships

- `meter_assignments` 1──M `meter_readings` (one assignment has many readings over time)
- `submitted_by` is an external reference to `auth_db.users.id`

#### Validation Rules

| Rule | Enforcement |
|---|---|
| `meter_value >= 0` | Negative readings are rejected at the application layer |
| `current_meter_value > previous_meter_value` | The submitted value must be strictly greater than the last accepted reading for this meter assignment |
| No duplicate date per assignment | `UNIQUE (meter_assignment_id, reading_date)` constraint prevents a second reading on the same date |
| `units_consumed` = current − previous | Calculated automatically by the service after validation; cannot be manually set |
| `reading_date` cannot be in the future | Must be <= current date |

---

### 6.6 Relationships Summary (meter_db)

```
┌────────────────────────────────────────────────────────────────────┐
│                      meter_db RELATIONSHIPS                         │
│                                                                     │
│  ┌────────────────┐                                                │
│  │     meters      │                                                │
│  │                 │                                                │
│  │  meter_number   │  (unique, physical serial number)              │
│  │  meter_type     │                                                │
│  └────────┬───────┘                                                │
│           │                                                         │
│           │ 1                                                       │
│           │                                                         │
│           │ M (over time — reassignments)                           │
│           │                                                         │
│  ┌────────▼───────────┐                                            │
│  │ meter_assignments   │                                            │
│  │                     │                                            │
│  │ consumer_id (EXT_REF)  ← user_db.consumer_profiles.id           │
│  │ assigned_from/until    date range of assignment                  │
│  └────────┬───────────┘                                            │
│           │                                                         │
│           │ 1                                                       │
│           │                                                         │
│           │ M (daily readings)                                      │
│           │                                                         │
│  ┌────────▼───────────┐                                            │
│  │  meter_readings     │                                            │
│  │                     │                                            │
│  │  reading_date       │  (UQ with meter_assignment_id)             │
│  │  meter_value >= 0   │  (must be > previous)                     │
│  │  units_consumed     │  (auto-calculated)                        │
│  │  submitted_by       │  (EXT_REF → auth_db.users.id)             │
│  └─────────────────────┘                                            │
│                                                                     │
│  EXTERNAL REFERENCES:                                              │
│  ───────────────────                                                │
│  • meter_assignments.consumer_id → user_db.consumer_profiles.id    │
│  • meter_readings.submitted_by   → auth_db.users.id                │
└────────────────────────────────────────────────────────────────────┘
```

---

### 6.7 Complete Index List (meter_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `meters` | `PRIMARY` | `id` | PRIMARY |
| `meters` | `idx_meters_meter_number` | `meter_number` | UNIQUE |
| `meters` | `idx_meters_is_active` | `is_active` | NON-UNIQUE |
| `meters` | `idx_meters_meter_type` | `meter_type` | NON-UNIQUE |
| `meter_assignments` | `PRIMARY` | `id` | PRIMARY |
| `meter_assignments` | `idx_meter_assignments_meter_id` | `meter_id` | NON-UNIQUE |
| `meter_assignments` | `idx_meter_assignments_consumer_id` | `consumer_id` | NON-UNIQUE |
| `meter_assignments` | `idx_meter_assignments_active` | `meter_id, is_active` | NON-UNIQUE |
| `meter_assignments` | `idx_meter_assignments_dates` | `consumer_id, assigned_from` | NON-UNIQUE |
| `meter_readings` | `PRIMARY` | `id` | PRIMARY |
| `meter_readings` | `uq_reading_per_day` | `meter_assignment_id, reading_date` | UNIQUE |
| `meter_readings` | `idx_meter_readings_assignment_id` | `meter_assignment_id` | NON-UNIQUE |
| `meter_readings` | `idx_meter_readings_reading_date` | `reading_date` | NON-UNIQUE |
| `meter_readings` | `idx_meter_readings_status` | `status` | NON-UNIQUE |
| `meter_readings` | `idx_meter_readings_submitted_by` | `submitted_by` | NON-UNIQUE |

---

### 6.8 Sample MySQL Schema — meter_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: meter_db
-- PURPOSE:  Meter registry, assignments, and daily readings
-- OWNER:    Meter Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS meter_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE meter_db;

-- -----------------------------------------
-- TABLE: meters
-- Physical electricity meter registry
-- -----------------------------------------
CREATE TABLE meters (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    meter_number        VARCHAR(50)     NOT NULL,
    meter_type          VARCHAR(20)     NOT NULL,
    installation_date   DATE            NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_meters_meter_number (meter_number),
    INDEX idx_meters_is_active (is_active),
    INDEX idx_meters_meter_type (meter_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: meter_assignments
-- Maps meters to consumers over date ranges
-- -----------------------------------------
CREATE TABLE meter_assignments (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    meter_id            BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    assigned_from       DATE            NOT NULL,
    assigned_until      DATE            NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_meter_assignments_meter_id (meter_id),
    INDEX idx_meter_assignments_consumer_id (consumer_id),
    INDEX idx_meter_assignments_active (meter_id, is_active),
    INDEX idx_meter_assignments_dates (consumer_id, assigned_from),

    CONSTRAINT fk_meter_assignments_meter
        FOREIGN KEY (meter_id) REFERENCES meters(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: meter_readings
-- Daily meter readings with validation
-- -----------------------------------------
CREATE TABLE meter_readings (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    meter_assignment_id     BIGINT          NOT NULL,
    reading_date            DATE            NOT NULL,
    meter_value             DECIMAL(12,2)   NOT NULL,
    units_consumed          DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'VERIFIED',
    remarks                 VARCHAR(255)    NULL,
    submitted_by            BIGINT          NOT NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uq_reading_per_day (meter_assignment_id, reading_date),
    INDEX idx_meter_readings_assignment_id (meter_assignment_id),
    INDEX idx_meter_readings_reading_date (reading_date),
    INDEX idx_meter_readings_status (status),
    INDEX idx_meter_readings_submitted_by (submitted_by),

    CONSTRAINT fk_meter_readings_assignment
        FOREIGN KEY (meter_assignment_id) REFERENCES meter_assignments(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 7. billing_db Design

### 7.1 Purpose

The `billing_db` database is owned by the **Billing Service** and stores all billing-related data including tariff plans, tariff slabs (unit-based pricing tiers), generated monthly bills, daily bills, and bill line items (slab-wise breakdown). This is the **financial calculation** database.

**Key design rules:**
- `tariff_plans` define a billing period with fixed charges and tax percentage.
- `tariff_slabs` define the unit-based pricing tiers within a plan (e.g., 0–100 units at ₹3.50/unit).
- `monthly_bills` store the computed monthly bill for each consumer.
- `bill_line_items` store the slab-wise breakdown of each monthly bill for detailed display.
- `daily_bills` store per-day computed bills based on each reading.
- `consumer_id`, `meter_reading_id`, `generated_by` are external references — **no cross-database foreign keys**.

---

### 7.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `tariff_plans` | Core | Defines a tariff plan with effective date range, fixed charges, and tax |
| 2 | `tariff_slabs` | Child | Unit-based pricing slabs within a tariff plan |
| 3 | `monthly_bills` | Core | Generated monthly bills per consumer |
| 4 | `bill_line_items` | Child | Slab-wise breakdown rows for each monthly bill |
| 5 | `daily_bills` | Core | Daily computed bills per reading (aggregated into monthly bills) |

---

### 7.3 Table: `tariff_plans`

Defines a tariff plan — the overall pricing structure for a given period. A plan contains fixed charges, tax percentage, and one or more tariff slabs.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `plan_name` | `VARCHAR(100)` | `NOT NULL` | — | Human-readable plan name (e.g., "Residential Tariff 2026") |
| `description` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Optional description of the plan |
| `effective_from` | `DATE` | `NOT NULL` | — | Start date of the tariff plan |
| `effective_to` | `DATE` | `NULLABLE` | `NULL` | End date of the plan (`NULL` = currently active, no end date) |
| `fixed_charges` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Fixed monthly charges (e.g., meter rent, service fee) |
| `tax_percentage` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | Tax percentage applied to total energy charge (e.g., 5.00) |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether this plan is currently active (only one plan should be active at a time) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_tariff_plans_effective` | `effective_from, effective_to` | NON-UNIQUE | Find which plan is active for a given date |
| `idx_tariff_plans_is_active` | `is_active` | NON-UNIQUE | Quick lookup of the currently active plan |

#### Validation Rules

| Field | Rule |
|---|---|
| `effective_from` | Must be a valid date; should not overlap with existing plan date ranges |
| `effective_to` | If provided, must be >= `effective_from`; `NULL` = no end date (currently active) |
| `fixed_charges` | Must be >= 0 |
| `tax_percentage` | Must be >= 0 and <= 100 |
| Active plan | Only one plan should be active at any given time (enforced at application layer) |

---

### 7.4 Table: `tariff_slabs`

Defines the unit-based pricing tiers within a tariff plan. Each slab has a unit range and a rate per unit. Slabs within a plan must not overlap.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `tariff_plan_id` | `BIGINT` | `NOT NULL`, `FK → tariff_plans(id)` | — | Reference to the parent tariff plan |
| `slab_name` | `VARCHAR(100)` | `NOT NULL` | — | Display name (e.g., "0–100 Units", "101–200 Units") |
| `unit_from` | `INT` | `NOT NULL` | — | Start of unit range (inclusive, e.g., 0) |
| `unit_to` | `INT` | `NULLABLE` | `NULL` | End of unit range (inclusive, e.g., 100; `NULL` = unlimited / above all) |
| `rate_per_unit` | `DECIMAL(8,2)` | `NOT NULL` | — | Price per unit in this slab (e.g., 3.50) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_tariff_slabs_plan` | `tariff_plan_id` | `tariff_plans(id)` | CASCADE |

#### Unique Constraints

```
UNIQUE KEY uq_slab_range (tariff_plan_id, unit_from)
```

This ensures that within a plan, no two slabs start at the same unit value (enforces non-overlapping ranges).

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_tariff_slabs_plan_id` | `tariff_plan_id` | NON-UNIQUE | Find all slabs in a plan |
| `idx_tariff_slabs_range` | `tariff_plan_id, unit_from` | UNIQUE | Enforce non-overlapping slab ranges |

#### Relationships

- `tariff_plans` 1──M `tariff_slabs` (one plan has many slabs)
- Slabs must cover a contiguous range: 0–100, 101–200, 201–300, 301+ (up to the last slab with `unit_to = NULL`)

#### Bill Calculation Example

For a consumption of 250 units under slabs: 0–100 @ ₹3.50, 101–200 @ ₹4.50, 201+ @ ₹6.00:

| Slab | Units | Rate | Amount |
|---|---|---|---|
| 0–100 | 100 | ₹3.50 | ₹350.00 |
| 101–200 | 100 | ₹4.50 | ₹450.00 |
| 201+ | 50 | ₹6.00 | ₹300.00 |
| **Energy Charge** | | | **₹1,100.00** |
| Fixed Charges | | | ₹50.00 |
| Tax (5%) | | | ₹55.00 |
| **Total** | | | **₹1,205.00** |

---

### 7.5 Table: `monthly_bills`

Stores the generated monthly bill for each consumer. One row per consumer per billing month.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `bill_number` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Auto-generated bill number (e.g., BILL-2026-07-0001) |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `billing_month` | `DATE` | `NOT NULL` | — | First day of the billing month (e.g., 2026-07-01 for July 2026) |
| `total_units` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Total units consumed in the billing month |
| `total_energy_charge` | `DECIMAL(12,2)` | `NOT NULL` | `0.00` | Sum of slab-wise energy charges before fixed charges and tax |
| `fixed_charges` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Fixed charges from the tariff plan |
| `tax_amount` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Tax amount = (total_energy_charge + fixed_charges) × tax_percentage |
| `total_amount` | `DECIMAL(12,2)` | `NOT NULL` | `0.00` | Grand total = total_energy_charge + fixed_charges + tax_amount |
| `status` | `VARCHAR(20)` | `NOT NULL` | `UNPAID` | Bill status: `PAID`, `UNPAID`, `PARTIAL` |
| `generated_by` | `BIGINT` | `NOT NULL` | — | External reference to `auth_db.users.id` (admin who triggered generation) |
| `generated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the bill was generated |
| `paid_at` | `DATETIME` | `NULLABLE` | `NULL` | When the bill was fully paid (updated by Payment Service) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Unique Constraints

```
UNIQUE KEY uq_bill_per_month (consumer_id, billing_month)
```

This ensures **one monthly bill per consumer per billing month**.

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_monthly_bills_bill_number` | `bill_number` | UNIQUE | Fast lookup by bill number |
| `idx_monthly_bills_consumer_id` | `consumer_id` | NON-UNIQUE | Find all bills for a consumer (EXT_REF) |
| `idx_monthly_bills_billing_month` | `billing_month` | NON-UNIQUE | Filter bills by billing period (admin reports) |
| `idx_monthly_bills_status` | `status` | NON-UNIQUE | Filter unpaid/paid bills |
| `idx_monthly_bills_consumer_month` | `consumer_id, billing_month` | UNIQUE | (same as unique constraint) |
| `idx_monthly_bills_generated_by` | `generated_by` | NON-UNIQUE | Track which admin generated which bills (EXT_REF) |

#### Relationships

- `monthly_bills` 1──M `bill_line_items` (one bill has many slab-wise breakdown rows)
- `consumer_id` is an external reference to `user_db.consumer_profiles.id`
- `generated_by` is an external reference to `auth_db.users.id`

#### Bill Status Transitions

```
UNPAID → PAID (when Payment Service confirms full payment)
UNPAID → PARTIAL → PAID (if partial payments are supported)
```

---

### 7.6 Table: `bill_line_items`

Stores the slab-wise breakdown of each monthly bill. Each row represents one slab tier with the units consumed in that tier and the calculated amount.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `monthly_bill_id` | `BIGINT` | `NOT NULL`, `FK → monthly_bills(id)` | — | Reference to the parent monthly bill |
| `slab_name` | `VARCHAR(100)` | `NOT NULL` | — | Name of the slab (e.g., "0–100 Units") |
| `unit_range_from` | `INT` | `NOT NULL` | — | Start of unit range for this line (for display) |
| `unit_range_to` | `INT` | `NULLABLE` | `NULL` | End of unit range (`NULL` = unlimited) |
| `units_in_slab` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Number of units consumed within this slab's range |
| `rate_per_unit` | `DECIMAL(8,2)` | `NOT NULL` | — | Rate per unit for this slab |
| `line_amount` | `DECIMAL(12,2)` | `NOT NULL` | `0.00` | Calculated amount = `units_in_slab` × `rate_per_unit` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_bill_line_items_bill` | `monthly_bill_id` | `monthly_bills(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_bill_line_items_bill_id` | `monthly_bill_id` | NON-UNIQUE | Find all line items for a bill |

#### Relationships

- `monthly_bills` 1──M `bill_line_items` (one bill has many slab-wise rows)
- Rows are ordered by `unit_range_from` ascending to display the slab breakdown in order

---

### 7.7 Table: `daily_bills`

Stores the per-day computed bill for each meter reading. Daily bills are automatically generated when a reading is submitted and are aggregated into the monthly bill.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `meter_reading_id` | `BIGINT` | `NOT NULL`, `UNIQUE` | — | External reference to `meter_db.meter_readings.id` (1:1 with reading) |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `bill_date` | `DATE` | `NOT NULL` | — | Date of the bill (same as reading date) |
| `units_consumed` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Units consumed on this day (from the reading) |
| `amount` | `DECIMAL(10,2)` | `NOT NULL` | `0.00` | Calculated daily bill amount |
| `status` | `VARCHAR(20)` | `NOT NULL` | `UNPAID` | Status: `PAID`, `UNPAID` |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_daily_bills_meter_reading_id` | `meter_reading_id` | UNIQUE | 1:1 mapping — one daily bill per reading |
| `idx_daily_bills_consumer_id` | `consumer_id` | NON-UNIQUE | Find all daily bills for a consumer (EXT_REF) |
| `idx_daily_bills_bill_date` | `bill_date` | NON-UNIQUE | Filter by date range |
| `idx_daily_bills_consumer_date` | `consumer_id, bill_date` | NON-UNIQUE | Quick lookup: daily bills for a consumer in a date range |
| `idx_daily_bills_status` | `status` | NON-UNIQUE | Filter unpaid daily bills |

#### Relationships

- `meter_reading_id` is an external reference to `meter_db.meter_readings.id` (1:1 — one reading produces one daily bill)
- `consumer_id` is an external reference to `user_db.consumer_profiles.id`

---

### 7.8 Relationships Summary (billing_db)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                       billing_db RELATIONSHIPS                               │
│                                                                              │
│  ┌──────────────────┐                                                        │
│  │   tariff_plans    │                                                        │
│  │                   │                                                        │
│  │  fixed_charges   │──┐                                                     │
│  │  tax_percentage   │  │ 1                                                   │
│  └──────────────────┘  │                                                     │
│           │            │                                                     │
│           │ M          │                                                     │
│           ▼            │                                                     │
│  ┌──────────────────┐  │                                                     │
│  │   tariff_slabs    │  │                                                     │
│  │                   │  │                                                     │
│  │  unit_from → to  │  │                                                     │
│  │  rate_per_unit    │  │                                                     │
│  └──────────────────┘  │                                                     │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐│
│  │                         monthly_bills                                    ││
│  │                                                                            ││
│  │  consumer_id (EXT_REF)   ← user_db.consumer_profiles.id                  ││
│  │  billing_month           DATE (1st of month)                              ││
│  │  total_amount = Σ line_items + fixed_charges + tax                       ││
│  │  status                  PAID / UNPAID / PARTIAL                          ││
│  │  generated_by (EXT_REF)  ← auth_db.users.id (admin)                      ││
│  └────────────────────────┬─────────────────────────────────────────────────┘│
│                           │ 1                                                │
│                           │                                                  │
│                           │ M                                                │
│  ┌────────────────────────▼─────────────────────────────────────────────────┐│
│  │                         bill_line_items                                   ││
│  │                                                                            ││
│  │  slab_name              (e.g., "0–100 Units")                            ││
│  │  units_in_slab          units consumed in this slab                       ││
│  │  line_amount = units_in_slab × rate_per_unit                             ││
│  └──────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐│
│  │                         daily_bills                                       ││
│  │                                                                            ││
│  │  meter_reading_id (EXT_REF, UQ)  ← meter_db.meter_readings.id (1:1)      ││
│  │  consumer_id (EXT_REF)           ← user_db.consumer_profiles.id          ││
│  │  amount                          daily computed amount                    ││
│  │  status                          PAID / UNPAID                            ││
│  └──────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  EXTERNAL REFERENCES:                                                        │
│  ───────────────────                                                          │
│  • monthly_bills.consumer_id      → user_db.consumer_profiles.id            │
│  • monthly_bills.generated_by     → auth_db.users.id                        │
│  • daily_bills.meter_reading_id   → meter_db.meter_readings.id              │
│  • daily_bills.consumer_id        → user_db.consumer_profiles.id            │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

### 7.9 Complete Index List (billing_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `tariff_plans` | `PRIMARY` | `id` | PRIMARY |
| `tariff_plans` | `idx_tariff_plans_effective` | `effective_from, effective_to` | NON-UNIQUE |
| `tariff_plans` | `idx_tariff_plans_is_active` | `is_active` | NON-UNIQUE |
| `tariff_slabs` | `PRIMARY` | `id` | PRIMARY |
| `tariff_slabs` | `idx_tariff_slabs_plan_id` | `tariff_plan_id` | NON-UNIQUE |
| `tariff_slabs` | `idx_tariff_slabs_range` | `tariff_plan_id, unit_from` | UNIQUE |
| `monthly_bills` | `PRIMARY` | `id` | PRIMARY |
| `monthly_bills` | `idx_monthly_bills_bill_number` | `bill_number` | UNIQUE |
| `monthly_bills` | `idx_monthly_bills_consumer_id` | `consumer_id` | NON-UNIQUE |
| `monthly_bills` | `idx_monthly_bills_billing_month` | `billing_month` | NON-UNIQUE |
| `monthly_bills` | `idx_monthly_bills_status` | `status` | NON-UNIQUE |
| `monthly_bills` | `idx_monthly_bills_consumer_month` | `consumer_id, billing_month` | UNIQUE |
| `monthly_bills` | `idx_monthly_bills_generated_by` | `generated_by` | NON-UNIQUE |
| `bill_line_items` | `PRIMARY` | `id` | PRIMARY |
| `bill_line_items` | `idx_bill_line_items_bill_id` | `monthly_bill_id` | NON-UNIQUE |
| `daily_bills` | `PRIMARY` | `id` | PRIMARY |
| `daily_bills` | `idx_daily_bills_meter_reading_id` | `meter_reading_id` | UNIQUE |
| `daily_bills` | `idx_daily_bills_consumer_id` | `consumer_id` | NON-UNIQUE |
| `daily_bills` | `idx_daily_bills_bill_date` | `bill_date` | NON-UNIQUE |
| `daily_bills` | `idx_daily_bills_consumer_date` | `consumer_id, bill_date` | NON-UNIQUE |
| `daily_bills` | `idx_daily_bills_status` | `status` | NON-UNIQUE |

---

### 7.10 Sample MySQL Schema — billing_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: billing_db
-- PURPOSE:  Tariff plans, slabs, monthly bills, and bill items
-- OWNER:    Billing Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS billing_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE billing_db;

-- -----------------------------------------
-- TABLE: tariff_plans
-- Defines billing periods with fixed charges and tax
-- -----------------------------------------
CREATE TABLE tariff_plans (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    plan_name           VARCHAR(100)    NOT NULL,
    description         VARCHAR(255)    NULL,
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    fixed_charges       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    tax_percentage      DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_tariff_plans_effective (effective_from, effective_to),
    INDEX idx_tariff_plans_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: tariff_slabs
-- Unit-based pricing tiers within a plan
-- -----------------------------------------
CREATE TABLE tariff_slabs (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    tariff_plan_id      BIGINT          NOT NULL,
    slab_name           VARCHAR(100)    NOT NULL,
    unit_from           INT             NOT NULL,
    unit_to             INT             NULL,
    rate_per_unit       DECIMAL(8,2)    NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_tariff_slabs_range (tariff_plan_id, unit_from),
    INDEX idx_tariff_slabs_plan_id (tariff_plan_id),

    CONSTRAINT fk_tariff_slabs_plan
        FOREIGN KEY (tariff_plan_id) REFERENCES tariff_plans(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: monthly_bills
-- Generated monthly bills per consumer
-- -----------------------------------------
CREATE TABLE monthly_bills (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    bill_number             VARCHAR(50)     NOT NULL,
    consumer_id             BIGINT          NOT NULL,
    billing_month           DATE            NOT NULL,
    total_units             DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_energy_charge     DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    fixed_charges           DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    tax_amount              DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount            DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    generated_by            BIGINT          NOT NULL,
    generated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at                 DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_monthly_bills_bill_number (bill_number),
    UNIQUE INDEX idx_monthly_bills_consumer_month (consumer_id, billing_month),
    INDEX idx_monthly_bills_consumer_id (consumer_id),
    INDEX idx_monthly_bills_billing_month (billing_month),
    INDEX idx_monthly_bills_status (status),
    INDEX idx_monthly_bills_generated_by (generated_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: bill_line_items
-- Slab-wise breakdown rows for each monthly bill
-- -----------------------------------------
CREATE TABLE bill_line_items (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    monthly_bill_id     BIGINT          NOT NULL,
    slab_name           VARCHAR(100)    NOT NULL,
    unit_range_from     INT             NOT NULL,
    unit_range_to       INT             NULL,
    units_in_slab       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    rate_per_unit       DECIMAL(8,2)    NOT NULL,
    line_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_bill_line_items_bill_id (monthly_bill_id),

    CONSTRAINT fk_bill_line_items_bill
        FOREIGN KEY (monthly_bill_id) REFERENCES monthly_bills(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: daily_bills
-- Per-day computed bills from each meter reading
-- -----------------------------------------
CREATE TABLE daily_bills (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    meter_reading_id    BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    bill_date           DATE            NOT NULL,
    units_consumed      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    amount              DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_daily_bills_meter_reading_id (meter_reading_id),
    INDEX idx_daily_bills_consumer_id (consumer_id),
    INDEX idx_daily_bills_bill_date (bill_date),
    INDEX idx_daily_bills_consumer_date (consumer_id, bill_date),
    INDEX idx_daily_bills_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

> **End of Phase 5 — Sections 6 & 7**
> *Sections 8–10 follow below.*

---

## 8. payment_db Design

### 8.1 Purpose

The `payment_db` database is owned by the **Payment Service** and stores all payment transaction data. This is the **financial transaction** database.

**Key design rules:**
- **V1 uses simulated/manual payment recording** — no real payment gateway integration. Payments are recorded manually in the system as a transaction log.
- `bill_id` and `consumer_id` are external references (no FK constraints).
- Each successful payment triggers a REST call to Billing Service to update the bill status to `PAID`.
- A bill can only be paid once (enforced at application layer).

---

### 8.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `payment_methods` | Lookup | Predefined payment methods (CASH, BANK_TRANSFER, CARD) |
| 2 | `payments` | Core | Payment transaction records |

---

### 8.3 Table: `payment_methods`

A lookup table for supported payment methods.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `method_name` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Payment method name (CASH, BANK_TRANSFER, CARD) |
| `description` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Optional description of the payment method |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether this payment method is currently available |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_payment_methods_name` | `method_name` | UNIQUE | Fast lookup by method name |

#### Seed Data (Optional — Documentation Only)

```sql
INSERT INTO payment_methods (method_name, description, is_active) VALUES
('CASH', 'Cash payment at utility office', TRUE),
('BANK_TRANSFER', 'Direct bank transfer / NEFT / RTGS', TRUE),
('CARD', 'Credit or debit card payment (manual entry)', TRUE);
```

---

### 8.4 Table: `payments`

Records each payment transaction against a bill.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `transaction_id` | `VARCHAR(100)` | `NOT NULL`, `UNIQUE` | — | Auto-generated unique transaction reference (e.g., PAY-20260727-0001) |
| `bill_id` | `BIGINT` | `NOT NULL` | — | External reference to `billing_db.monthly_bills.id` (no FK) |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `amount` | `DECIMAL(12,2)` | `NOT NULL` | — | Amount paid (must match bill amount in V1) |
| `payment_method_id` | `BIGINT` | `NOT NULL`, `FK → payment_methods(id)` | — | Reference to the payment method used |
| `transaction_ref` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Optional manual reference (cheque number, UTR, etc.) |
| `status` | `VARCHAR(20)` | `NOT NULL` | `COMPLETED` | Payment status: `COMPLETED`, `FAILED`, `REFUNDED` |
| `remarks` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Admin remarks if payment is refunded or failed |
| `paid_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the payment was completed |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_payments_method` | `payment_method_id` | `payment_methods(id)` | RESTRICT |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_payments_transaction_id` | `transaction_id` | UNIQUE | Fast lookup by transaction ID |
| `idx_payments_bill_id` | `bill_id` | NON-UNIQUE | Find all payments for a bill (EXT_REF) |
| `idx_payments_consumer_id` | `consumer_id` | NON-UNIQUE | Find all payments by a consumer (EXT_REF) |
| `idx_payments_status` | `status` | NON-UNIQUE | Filter payments by status |
| `idx_payments_paid_at` | `paid_at` | NON-UNIQUE | Date-range payment queries (reports) |
| `idx_payments_consumer_date` | `consumer_id, paid_at` | NON-UNIQUE | Consumer payment history sorted by date |

#### Relationships

- `payment_methods` 1──M `payments` (one method used in many payments)
- `bill_id` is an external reference to `billing_db.monthly_bills.id`
- `consumer_id` is an external reference to `user_db.consumer_profiles.id`

#### Validation Rules

| Rule | Enforcement |
|---|---|
| Duplicate payment | A bill can only be paid once (app layer checks no existing COMPLETED payment for this `bill_id`) |
| Amount match | Payment amount must match the bill's `total_amount` (V1 full-payment only) |
| `transaction_id` | Auto-generated format: `PAY-YYYYMMDD-NNNNNN`; unique across all payments |
| Payment method | Must reference an active `payment_methods.id` |

---

### 8.5 Relationships Summary (payment_db)

```
┌────────────────────────────────────────────────────────────────────┐
│                     payment_db RELATIONSHIPS                        │
│                                                                     │
│  ┌──────────────────────┐                                          │
│  │   payment_methods     │                                          │
│  │                       │                                          │
│  │  method_name (UQ)    │──┐                                       │
│  │  is_active            │  │ 1                                    │
│  └───────────────────────┘  │                                       │
│                            │                                       │
│                            │ M                                      │
│  ┌───────────────────────┐  │                                       │
│  │       payments         │  │                                       │
│  │                        │  │                                       │
│  │  transaction_id (UQ)  │  │                                       │
│  │  bill_id (EXT_REF)    │  │  ← billing_db.monthly_bills.id        │
│  │  consumer_id (EXT_REF)│  │  ← user_db.consumer_profiles.id      │
│  │  status                │  │  COMPLETED / FAILED / REFUNDED        │
│  │  paid_at               │  │                                       │
│  └────────────────────────┘  │                                       │
│                                                                     │
│  EXTERNAL REFERENCES:                                              │
│  ───────────────────                                                │
│  • payments.bill_id      → billing_db.monthly_bills.id             │
│  • payments.consumer_id  → user_db.consumer_profiles.id            │
└────────────────────────────────────────────────────────────────────┘
```

---

### 8.6 Complete Index List (payment_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `payment_methods` | `PRIMARY` | `id` | PRIMARY |
| `payment_methods` | `idx_payment_methods_name` | `method_name` | UNIQUE |
| `payments` | `PRIMARY` | `id` | PRIMARY |
| `payments` | `idx_payments_transaction_id` | `transaction_id` | UNIQUE |
| `payments` | `idx_payments_bill_id` | `bill_id` | NON-UNIQUE |
| `payments` | `idx_payments_consumer_id` | `consumer_id` | NON-UNIQUE |
| `payments` | `idx_payments_status` | `status` | NON-UNIQUE |
| `payments` | `idx_payments_paid_at` | `paid_at` | NON-UNIQUE |
| `payments` | `idx_payments_consumer_date` | `consumer_id, paid_at` | NON-UNIQUE |

---

### 8.7 Sample MySQL Schema — payment_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: payment_db
-- PURPOSE:  Payment transactions and methods
-- OWNER:    Payment Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS payment_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE payment_db;

-- -----------------------------------------
-- TABLE: payment_methods
-- Lookup table for supported payment methods
-- -----------------------------------------
CREATE TABLE payment_methods (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    method_name     VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_payment_methods_name (method_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: payments
-- Payment transaction records (simulated in V1)
-- -----------------------------------------
CREATE TABLE payments (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    transaction_id      VARCHAR(100)    NOT NULL,
    bill_id             BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    amount              DECIMAL(12,2)   NOT NULL,
    payment_method_id   BIGINT          NOT NULL,
    transaction_ref     VARCHAR(255)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    remarks             VARCHAR(255)    NULL,
    paid_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_payments_transaction_id (transaction_id),
    INDEX idx_payments_bill_id (bill_id),
    INDEX idx_payments_consumer_id (consumer_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_paid_at (paid_at),
    INDEX idx_payments_consumer_date (consumer_id, paid_at),

    CONSTRAINT fk_payments_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 9. complaint_db Design

### 9.1 Purpose

The `complaint_db` database is owned by the **Complaint Service** and stores all consumer complaints, categories, resolution comments, and status change history. This is the **customer support** database.

**Key design rules:**
- `consumer_id`, `assigned_to`, and `author_id` are external references — **no cross-database foreign keys**.
- Only FKs within `complaint_db` are enforced (between `complaints`, `complaint_categories`, `complaint_comments`).
- Complaint status transitions: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.
- Status changes are recorded in `complaint_status_history` for audit trail.

---

### 9.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `complaint_categories` | Lookup | Predefined complaint categories (BILLING_ISSUE, METER_ISSUE, PAYMENT_ISSUE, OTHER) |
| 2 | `complaints` | Core | Consumer complaint records |
| 3 | `complaint_comments` | Child | Admin/consumer comments on a complaint |
| 4 | `complaint_status_history` | Child | Audit trail of status changes |

---

### 9.3 Table: `complaint_categories`

A lookup table for predefined complaint categories.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `name` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Category name (BILLING_ISSUE, METER_ISSUE, PAYMENT_ISSUE, OTHER) |
| `description` | `VARCHAR(255)` | `NULLABLE` | `NULL` | Description of what this category covers |
| `is_active` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether this category is available for new complaints |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_complaint_categories_name` | `name` | UNIQUE | Fast lookup by category name |

---

### 9.4 Table: `complaints`

The core table for storing consumer complaints.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `ticket_number` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | — | Auto-generated ticket number (e.g., CMP-20260727-0001) |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `category_id` | `BIGINT` | `NOT NULL`, `FK → complaint_categories(id)` | — | Reference to the complaint category |
| `subject` | `VARCHAR(200)` | `NOT NULL` | — | Short summary of the complaint |
| `description` | `TEXT` | `NOT NULL` | — | Detailed description of the issue |
| `status` | `VARCHAR(20)` | `NOT NULL` | `OPEN` | Complaint status: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `priority` | `VARCHAR(10)` | `NOT NULL` | `NORMAL` | Priority level: `LOW`, `NORMAL`, `HIGH`, `URGENT` |
| `assigned_to` | `BIGINT` | `NULLABLE` | `NULL` | External reference to `auth_db.users.id` (admin assigned to resolve) |
| `assigned_at` | `DATETIME` | `NULLABLE` | `NULL` | When the complaint was assigned to an admin |
| `resolved_at` | `DATETIME` | `NULLABLE` | `NULL` | When the complaint was marked RESOLVED |
| `closed_at` | `DATETIME` | `NULLABLE` | `NULL` | When the complaint was marked CLOSED |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |
| `updated_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP ON UPDATE` | Audit: last modification timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_complaints_category` | `category_id` | `complaint_categories(id)` | RESTRICT |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_complaints_ticket_number` | `ticket_number` | UNIQUE | Fast lookup by ticket number |
| `idx_complaints_consumer_id` | `consumer_id` | NON-UNIQUE | Find all complaints by a consumer (EXT_REF) |
| `idx_complaints_category_id` | `category_id` | NON-UNIQUE | Filter complaints by category |
| `idx_complaints_status` | `status` | NON-UNIQUE | Filter by status (admin dashboard) |
| `idx_complaints_priority` | `priority` | NON-UNIQUE | Filter by priority level |
| `idx_complaints_assigned_to` | `assigned_to` | NON-UNIQUE | Find complaints assigned to an admin (EXT_REF) |
| `idx_complaints_created_at` | `created_at` | NON-UNIQUE | Sort/filter by creation date |
| `idx_complaints_status_assigned` | `status, assigned_to` | NON-UNIQUE | Find open complaints assigned to a specific admin |

#### Relationships

- `complaint_categories` 1──M `complaints` (one category has many complaints)
- `complaints` 1──M `complaint_comments` (one complaint has many comments)
- `complaints` 1──M `complaint_status_history` (one complaint has a status change history)

#### Validation Rules

| Rule | Enforcement |
|---|---|
| `ticket_number` | Auto-generated format: `CMP-YYYYMMDD-NNNNNN`; unique |
| Status transition | Must follow: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED` (enforced at app layer) |
| `priority` | One of: `LOW`, `NORMAL`, `HIGH`, `URGENT` |
| `consumer_id` | Must reference a valid consumer in `user_db` (validated via REST call) |
| Resolution | `resolved_at` is set when status changes to `RESOLVED` |

---

### 9.5 Table: `complaint_comments`

Stores comments/updates on a complaint by both consumers and admins.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `complaint_id` | `BIGINT` | `NOT NULL`, `FK → complaints(id)` | — | Reference to the parent complaint |
| `comment_text` | `TEXT` | `NOT NULL` | — | The comment or resolution text |
| `author_id` | `BIGINT` | `NOT NULL` | — | External reference to `auth_db.users.id` (who wrote the comment) |
| `is_admin_comment` | `BOOLEAN` | `NOT NULL` | `FALSE` | Whether the comment was made by an admin (vs. consumer) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_complaint_comments_complaint` | `complaint_id` | `complaints(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_complaint_comments_complaint_id` | `complaint_id` | NON-UNIQUE | Find all comments for a complaint |
| `idx_complaint_comments_author_id` | `author_id` | NON-UNIQUE | Find all comments by a specific user (EXT_REF) |

---

### 9.6 Table: `complaint_status_history`

Audit trail recording every status change on a complaint.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `complaint_id` | `BIGINT` | `NOT NULL`, `FK → complaints(id)` | — | Reference to the parent complaint |
| `from_status` | `VARCHAR(20)` | `NULLABLE` | `NULL` | Previous status (`NULL` for initial creation) |
| `to_status` | `VARCHAR(20)` | `NOT NULL` | — | New status |
| `changed_by` | `BIGINT` | `NOT NULL` | — | External reference to `auth_db.users.id` (who made the change) |
| `changed_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the change occurred |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_complaint_status_history_complaint` | `complaint_id` | `complaints(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_complaint_history_complaint_id` | `complaint_id` | NON-UNIQUE | Find all status changes for a complaint |

---

### 9.7 Complete Index List (complaint_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `complaint_categories` | `PRIMARY` | `id` | PRIMARY |
| `complaint_categories` | `idx_complaint_categories_name` | `name` | UNIQUE |
| `complaints` | `PRIMARY` | `id` | PRIMARY |
| `complaints` | `idx_complaints_ticket_number` | `ticket_number` | UNIQUE |
| `complaints` | `idx_complaints_consumer_id` | `consumer_id` | NON-UNIQUE |
| `complaints` | `idx_complaints_category_id` | `category_id` | NON-UNIQUE |
| `complaints` | `idx_complaints_status` | `status` | NON-UNIQUE |
| `complaints` | `idx_complaints_priority` | `priority` | NON-UNIQUE |
| `complaints` | `idx_complaints_assigned_to` | `assigned_to` | NON-UNIQUE |
| `complaints` | `idx_complaints_created_at` | `created_at` | NON-UNIQUE |
| `complaints` | `idx_complaints_status_assigned` | `status, assigned_to` | NON-UNIQUE |
| `complaint_comments` | `PRIMARY` | `id` | PRIMARY |
| `complaint_comments` | `idx_complaint_comments_complaint_id` | `complaint_id` | NON-UNIQUE |
| `complaint_comments` | `idx_complaint_comments_author_id` | `author_id` | NON-UNIQUE |
| `complaint_status_history` | `PRIMARY` | `id` | PRIMARY |
| `complaint_status_history` | `idx_complaint_history_complaint_id` | `complaint_id` | NON-UNIQUE |

---

### 9.8 Sample MySQL Schema — complaint_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: complaint_db
-- PURPOSE:  Consumer complaints, categories, and status tracking
-- OWNER:    Complaint Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS complaint_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE complaint_db;

-- -----------------------------------------
-- TABLE: complaint_categories
-- Lookup table for complaint categories
-- -----------------------------------------
CREATE TABLE complaint_categories (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_complaint_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: complaints
-- Core complaint records
-- -----------------------------------------
CREATE TABLE complaints (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ticket_number       VARCHAR(50)     NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    category_id         BIGINT          NOT NULL,
    subject             VARCHAR(200)    NOT NULL,
    description         TEXT            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    priority            VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    assigned_to         BIGINT          NULL,
    assigned_at         DATETIME        NULL,
    resolved_at         DATETIME        NULL,
    closed_at           DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_complaints_ticket_number (ticket_number),
    INDEX idx_complaints_consumer_id (consumer_id),
    INDEX idx_complaints_category_id (category_id),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_priority (priority),
    INDEX idx_complaints_assigned_to (assigned_to),
    INDEX idx_complaints_created_at (created_at),
    INDEX idx_complaints_status_assigned (status, assigned_to),

    CONSTRAINT fk_complaints_category
        FOREIGN KEY (category_id) REFERENCES complaint_categories(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: complaint_comments
-- Comments and updates on complaints
-- -----------------------------------------
CREATE TABLE complaint_comments (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    complaint_id        BIGINT          NOT NULL,
    comment_text        TEXT            NOT NULL,
    author_id           BIGINT          NOT NULL,
    is_admin_comment    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_complaint_comments_complaint_id (complaint_id),
    INDEX idx_complaint_comments_author_id (author_id),

    CONSTRAINT fk_complaint_comments_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: complaint_status_history
-- Audit trail for complaint status changes
-- -----------------------------------------
CREATE TABLE complaint_status_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    complaint_id        BIGINT          NOT NULL,
    from_status         VARCHAR(20)     NULL,
    to_status           VARCHAR(20)     NOT NULL,
    changed_by          BIGINT          NOT NULL,
    changed_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_complaint_history_complaint_id (complaint_id),

    CONSTRAINT fk_complaint_status_history_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 10. notification_db Design

### 10.1 Purpose

The `notification_db` database is owned by the **Notification Service** and stores all in-app notifications. This is the **communication** database.

**Key design rules (V1 scope):**
- **V1 notifications are in-app only** — no SMS, email, or push notification delivery.
- Notifications are created automatically by system events (bill generation, payment confirmation, complaint status change) or manually by admin broadcasts.
- The `notifications` table stores the notification message and metadata.
- The `notification_recipients` table maps notifications to consumers and tracks read/unread status.
- Broadcast = one `notifications` row + N `notification_recipients` rows (one per active consumer).
- Targeted = one `notifications` row + 1 `notification_recipients` row.

---

### 10.2 Tables Overview

| # | Table Name | Type | Purpose |
|---|---|---|---|
| 1 | `notifications` | Core | Stores notification messages and metadata |
| 2 | `notification_recipients` | Child | Maps notifications to consumers with read status |

---

### 10.3 Table: `notifications`

Stores the notification message, type, and reference metadata.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `title` | `VARCHAR(200)` | `NOT NULL` | — | Notification title (e.g., "Bill Generated", "Payment Confirmed") |
| `message` | `TEXT` | `NOT NULL` | — | Notification body text |
| `type` | `VARCHAR(20)` | `NOT NULL` | `INFO` | Notification type: `INFO`, `WARNING`, `ALERT` |
| `reference_type` | `VARCHAR(50)` | `NULLABLE` | `NULL` | Source entity type (e.g., BILL, PAYMENT, COMPLAINT) |
| `reference_id` | `BIGINT` | `NULLABLE` | `NULL` | ID of the source entity (e.g., bill_id, payment_id, complaint_id) |
| `created_by` | `BIGINT` | `NOT NULL` | — | External reference to `auth_db.users.id` (system or admin who created it) |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_notifications_type` | `type` | NON-UNIQUE | Filter by notification type |
| `idx_notifications_reference` | `reference_type, reference_id` | NON-UNIQUE | Find notifications related to a specific entity |
| `idx_notifications_created_at` | `created_at` | NON-UNIQUE | Sort by creation date |

#### Relationships

- `notifications` 1──M `notification_recipients` (one notification can be sent to many consumers)
- `created_by` is an external reference to `auth_db.users.id`

---

### 10.4 Table: `notification_recipients`

Maps notifications to individual consumers and tracks read status.

#### Columns

| Column | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | `PK`, `NOT NULL`, `AUTO_INCREMENT` | — | Primary key |
| `notification_id` | `BIGINT` | `NOT NULL`, `FK → notifications(id)` | — | Reference to the notification |
| `consumer_id` | `BIGINT` | `NOT NULL` | — | External reference to `user_db.consumer_profiles.id` (no FK) |
| `is_read` | `BOOLEAN` | `NOT NULL` | `FALSE` | Whether the consumer has read the notification |
| `read_at` | `DATETIME` | `NULLABLE` | `NULL` | When the consumer read the notification |
| `is_delivered` | `BOOLEAN` | `NOT NULL` | `TRUE` | Whether the notification was delivered (always TRUE for in-app) |
| `delivered_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | When the notification was delivered/created |
| `created_at` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | Audit: row creation timestamp |

#### Primary Key

```
PRIMARY KEY (id)
```

#### Foreign Keys

| Constraint Name | Column | Reference | On Delete |
|---|---|---|---|
| `fk_notif_recipients_notification` | `notification_id` | `notifications(id)` | CASCADE |

#### Indexes

| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `idx_notif_recipients_notification_id` | `notification_id` | NON-UNIQUE | Find all recipients of a notification |
| `idx_notif_recipients_consumer_id` | `consumer_id` | NON-UNIQUE | Find all notifications for a consumer (EXT_REF) |
| `idx_notif_recipients_unread` | `consumer_id, is_read` | NON-UNIQUE | Quick lookup of unread notifications for a consumer |
| `idx_notif_recipients_consumer_date` | `consumer_id, created_at` | NON-UNIQUE | Paginated notification list sorted by date for a consumer |

#### Relationships

- `notifications` 1──M `notification_recipients` (one notification sent to many consumers)
- `consumer_id` is an external reference to `user_db.consumer_profiles.id`

#### Validation Rules

| Rule | Enforcement |
|---|---|
| Read status | `is_read` defaults to `FALSE`; set to `TRUE` with `read_at` timestamp when consumer views it |
| Delivery | `is_delivered` is always `TRUE` for in-app V1 notifications (no external delivery channels) |
| Broadcast | App layer inserts one `notifications` row + one `notification_recipients` row per active consumer |
| Targeted | App layer inserts one `notifications` row + one `notification_recipients` row for the target consumer |

---

### 10.5 Complete Index List (notification_db)

| Table | Index Name | Column(s) | Type |
|---|---|---|---|
| `notifications` | `PRIMARY` | `id` | PRIMARY |
| `notifications` | `idx_notifications_type` | `type` | NON-UNIQUE |
| `notifications` | `idx_notifications_reference` | `reference_type, reference_id` | NON-UNIQUE |
| `notifications` | `idx_notifications_created_at` | `created_at` | NON-UNIQUE |
| `notification_recipients` | `PRIMARY` | `id` | PRIMARY |
| `notification_recipients` | `idx_notif_recipients_notification_id` | `notification_id` | NON-UNIQUE |
| `notification_recipients` | `idx_notif_recipients_consumer_id` | `consumer_id` | NON-UNIQUE |
| `notification_recipients` | `idx_notif_recipients_unread` | `consumer_id, is_read` | NON-UNIQUE |
| `notification_recipients` | `idx_notif_recipients_consumer_date` | `consumer_id, created_at` | NON-UNIQUE |

---

### 10.6 Sample MySQL Schema — notification_db

> ⚠️ **Documentation Only** — These CREATE TABLE statements are provided as documentation of the intended schema. They are not meant to be executed directly; Flyway migrations will be used in implementation.

```sql
-- ============================================================
-- DATABASE: notification_db
-- PURPOSE:  In-app notifications and recipient tracking
-- OWNER:    Notification Service
-- ============================================================

CREATE DATABASE IF NOT EXISTS notification_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE notification_db;

-- -----------------------------------------
-- TABLE: notifications
-- Stores notification messages and metadata
-- -----------------------------------------
CREATE TABLE notifications (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL,
    message             TEXT            NOT NULL,
    type                VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    reference_type      VARCHAR(50)     NULL,
    reference_id        BIGINT          NULL,
    created_by          BIGINT          NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_notifications_type (type),
    INDEX idx_notifications_reference (reference_type, reference_id),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- TABLE: notification_recipients
-- Maps notifications to consumers with read status
-- -----------------------------------------
CREATE TABLE notification_recipients (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    notification_id     BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             DATETIME        NULL,
    is_delivered        BOOLEAN         NOT NULL DEFAULT TRUE,
    delivered_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_notif_recipients_notification_id (notification_id),
    INDEX idx_notif_recipients_consumer_id (consumer_id),
    INDEX idx_notif_recipients_unread (consumer_id, is_read),
    INDEX idx_notif_recipients_consumer_date (consumer_id, created_at),

    CONSTRAINT fk_notif_recipients_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

> **End of Phase 5 — Sections 8, 9, 10**
> *Sections 11–17 follow below (completing Phase 5).*

---

## 11. Relationships

### 11.1 Relationships Inside Each Database

The following table summarizes all **internal** relationships (those enforced via foreign key constraints within the same database):

| Database | Parent Table | Child Table | Relationship | Foreign Key | On Delete |
|---|---|---|---|---|---|
| `auth_db` | `users` | `user_roles` | 1:M | `user_roles.user_id → users.id` | CASCADE |
| `auth_db` | `roles` | `user_roles` | 1:M | `user_roles.role_id → roles.id` | CASCADE |
| `auth_db` | `users` | `refresh_tokens` | 1:M (Future) | `refresh_tokens.user_id → users.id` | CASCADE |
| `user_db` | `consumer_profiles` | `consumer_addresses` | 1:M | `consumer_addresses.consumer_id → consumer_profiles.id` | CASCADE |
| `meter_db` | `meters` | `meter_assignments` | 1:M | `meter_assignments.meter_id → meters.id` | CASCADE |
| `meter_db` | `meter_assignments` | `meter_readings` | 1:M | `meter_readings.meter_assignment_id → meter_assignments.id` | CASCADE |
| `billing_db` | `tariff_plans` | `tariff_slabs` | 1:M | `tariff_slabs.tariff_plan_id → tariff_plans.id` | CASCADE |
| `billing_db` | `monthly_bills` | `bill_line_items` | 1:M | `bill_line_items.monthly_bill_id → monthly_bills.id` | CASCADE |
| `payment_db` | `payment_methods` | `payments` | 1:M | `payments.payment_method_id → payment_methods.id` | RESTRICT |
| `complaint_db` | `complaint_categories` | `complaints` | 1:M | `complaints.category_id → complaint_categories.id` | RESTRICT |
| `complaint_db` | `complaints` | `complaint_comments` | 1:M | `complaint_comments.complaint_id → complaints.id` | CASCADE |
| `complaint_db` | `complaints` | `complaint_status_history` | 1:M | `complaint_status_history.complaint_id → complaints.id` | CASCADE |
| `notification_db` | `notifications` | `notification_recipients` | 1:M | `notification_recipients.notification_id → notifications.id` | CASCADE |

### 11.2 M:M Relationships (Resolved via Junction Tables)

| Database | Left Entity | Right Entity | Junction Table |
|---|---|---|---|
| `auth_db` | `users` | `roles` | `user_roles` |

### 11.3 External References Between Services

The following references cross service boundaries and are **NOT enforced as database foreign keys**. Referential integrity is maintained through REST API calls at the application layer.

| Source Database | Column | Target Service | Target Table | Purpose |
|---|---|---|---|---|
| `user_db` | `consumer_profiles.auth_user_id` | Auth Service | `auth_db.users.id` | Link profile to login credentials |
| `meter_db` | `meter_assignments.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link meter to consumer |
| `meter_db` | `meter_readings.submitted_by` | Auth Service | `auth_db.users.id` | Identify who submitted the reading |
| `billing_db` | `monthly_bills.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link bill to consumer |
| `billing_db` | `monthly_bills.generated_by` | Auth Service | `auth_db.users.id` | Identify admin who generated the bill |
| `billing_db` | `daily_bills.meter_reading_id` | Meter Service | `meter_db.meter_readings.id` | Link daily bill to reading |
| `billing_db` | `daily_bills.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link daily bill to consumer |
| `payment_db` | `payments.bill_id` | Billing Service | `billing_db.monthly_bills.id` | Link payment to bill |
| `payment_db` | `payments.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link payment to consumer |
| `complaint_db` | `complaints.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link complaint to consumer |
| `complaint_db` | `complaints.assigned_to` | Auth Service | `auth_db.users.id` | Link complaint to assigned admin |
| `complaint_db` | `complaint_comments.author_id` | Auth Service | `auth_db.users.id` | Identify comment author |
| `complaint_db` | `complaint_status_history.changed_by` | Auth Service | `auth_db.users.id` | Identify who changed status |
| `notification_db` | `notification_recipients.consumer_id` | User Service | `user_db.consumer_profiles.id` | Link notification recipient |
| `notification_db` | `notifications.created_by` | Auth Service | `auth_db.users.id` | Identify sender (system or admin) |

### 11.4 Service Communication for Relationship Integrity

Since cross-database foreign keys are not used, relationship integrity is maintained through the following communication patterns:

| Pattern | Description | Example |
|---|---|---|
| **Registration Flow** | Auth Service validates input → calls User Service REST API to create profile → User Service returns `consumer_id` → Auth Service stores it | Auth Service → User Service |
| **Pre-validation** | Before writing a record with an external ID, the service calls the owning service's API to confirm the referenced record exists | Billing Service calls User Service to verify `consumer_id` exists before generating a bill |
| **Cascading Notification** | When a complaint status changes, Complaint Service calls Notification Service to create a notification — no FK needed | Complaint Service → Notification Service |
| **Status Update Callback** | After recording a payment, Payment Service calls Billing Service to update bill status to PAID | Payment Service → Billing Service |

### 11.5 Handling Deleted or Inactive External Records

| Scenario | Handling Strategy |
|---|---|
| User deactivated (auth_db) | Auth Service sets `is_active = FALSE`. Other services should treat inactive users gracefully — existing bills, readings, and complaints remain accessible but new actions may be restricted. |
| Consumer profile deleted | Soft delete recommended (see Section 14). All associated records (readings, bills, payments, complaints, notifications) reference a deleted profile via external ID — the external ID remains in place for historical integrity. |
| Meter decommissioned | `meters.is_active = FALSE`. Historical meter_assignments and meter_readings remain accessible. New readings cannot be submitted for inactive meters. |
| Tariff plan deactivated | `tariff_plans.is_active = FALSE`. Existing bills linked to that plan remain unchanged. New bills use the current active plan. |

---

## 12. Constraints

### 12.1 Primary Key Constraints

Every table in every database uses `BIGINT AUTO_INCREMENT` as its primary key.

| Pattern | Rule |
|---|---|
| Naming | `PRIMARY KEY (id)` — always a single column named `id` |
| Data Type | `BIGINT` (64-bit integer, supports up to 9+ quintillion rows) |
| Generation | `AUTO_INCREMENT` (database-generated, sequential) |
| Exception | Junction tables use composite primary keys: `PRIMARY KEY (entity1_id, entity2_id)` |

**Tables with composite primary keys:**

| Database | Table | Composite PK Columns |
|---|---|---|
| `auth_db` | `user_roles` | `(user_id, role_id)` |

### 12.2 Foreign Key Constraints (Within Same Database Only)

| Database | Constraint Name | Columns | Referenced Table | On Delete |
|---|---|---|---|---|
| `auth_db` | `fk_user_roles_user` | `user_roles.user_id` | `users(id)` | CASCADE |
| `auth_db` | `fk_user_roles_role` | `user_roles.role_id` | `roles(id)` | CASCADE |
| `auth_db` | `fk_refresh_tokens_user` | `refresh_tokens.user_id` | `users(id)` | CASCADE |
| `user_db` | `fk_consumer_addresses_profile` | `consumer_addresses.consumer_id` | `consumer_profiles(id)` | CASCADE |
| `meter_db` | `fk_meter_assignments_meter` | `meter_assignments.meter_id` | `meters(id)` | CASCADE |
| `meter_db` | `fk_meter_readings_assignment` | `meter_readings.meter_assignment_id` | `meter_assignments(id)` | CASCADE |
| `billing_db` | `fk_tariff_slabs_plan` | `tariff_slabs.tariff_plan_id` | `tariff_plans(id)` | CASCADE |
| `billing_db` | `fk_bill_line_items_bill` | `bill_line_items.monthly_bill_id` | `monthly_bills(id)` | CASCADE |
| `payment_db` | `fk_payments_method` | `payments.payment_method_id` | `payment_methods(id)` | RESTRICT |
| `complaint_db` | `fk_complaints_category` | `complaints.category_id` | `complaint_categories(id)` | RESTRICT |
| `complaint_db` | `fk_complaint_comments_complaint` | `complaint_comments.complaint_id` | `complaints(id)` | CASCADE |
| `complaint_db` | `fk_complaint_status_history_complaint` | `complaint_status_history.complaint_id` | `complaints(id)` | CASCADE |
| `notification_db` | `fk_notif_recipients_notification` | `notification_recipients.notification_id` | `notifications(id)` | CASCADE |

### 12.3 Unique Constraints

| Database | Table | Unique Column(s) | Business Rule |
|---|---|---|---|
| `auth_db` | `users` | `email` | No two users can register with the same email |
| `auth_db` | `roles` | `name` | Role names must be unique (CONSUMER, ADMIN) |
| `auth_db` | `refresh_tokens` | `token` | (Future) Token values must be unique |
| `user_db` | `consumer_profiles` | `auth_user_id` | One profile per auth user (1:1) |
| `user_db` | `consumer_profiles` | `consumer_number` | Account numbers must be unique |
| `user_db` | `consumer_profiles` | `email` | No duplicate emails across profiles |
| `meter_db` | `meters` | `meter_number` | Physical meter serial numbers must be unique |
| `meter_db` | `meter_readings` | `(meter_assignment_id, reading_date)` | One reading per meter per day |
| `billing_db` | `tariff_slabs` | `(tariff_plan_id, unit_from)` | Slab ranges must not overlap within a plan |
| `billing_db` | `monthly_bills` | `bill_number` | Bill numbers must be unique |
| `billing_db` | `monthly_bills` | `(consumer_id, billing_month)` | One bill per consumer per month |
| `billing_db` | `daily_bills` | `meter_reading_id` | One daily bill per reading (1:1) |
| `payment_db` | `payment_methods` | `method_name` | Payment method names must be unique |
| `payment_db` | `payments` | `transaction_id` | Transaction IDs must be unique |
| `complaint_db` | `complaint_categories` | `name` | Category names must be unique |
| `complaint_db` | `complaints` | `ticket_number` | Ticket numbers must be unique |

### 12.4 Check Constraints (Application Layer)

MySQL does not enforce CHECK constraints in all storage engines reliably. The following business rules are enforced at the **application layer** (Service layer):

| Database | Table | Check Rule | Description |
|---|---|---|---|
| `auth_db` | `users` | `email` format | Must be valid email format |
| `auth_db` | `users` | `password_hash` length | Must be valid BCrypt hash (60 chars) |
| `user_db` | `consumer_profiles` | `full_name` length | 2–100 characters |
| `user_db` | `consumer_profiles` | `phone` format | 10-digit with optional country code |
| `meter_db` | `meter_readings` | `meter_value >= 0` | No negative readings |
| `meter_db` | `meter_readings` | `meter_value > previous` | Reading must increase |
| `billing_db` | `tariff_plans` | `tax_percentage` | Must be 0–100 |
| `billing_db` | `tariff_plans` | `fixed_charges >= 0` | Fixed charges cannot be negative |
| `billing_db` | `tariff_slabs` | `unit_from >= 0` | Unit ranges start from 0 |
| `billing_db` | `tariff_slabs` | `rate_per_unit > 0` | Rates must be positive |
| `billing_db` | `monthly_bills` | `total_amount >= 0` | Bill total cannot be negative |
| `payment_db` | `payments` | `amount > 0` | Payment amount must be positive |
| `complaint_db` | `complaints` | status transition | Must follow OPEN → IN_PROGRESS → RESOLVED → CLOSED |

### 12.5 Default Values

| Database | Table | Column | Default | Rationale |
|---|---|---|---|---|
| `auth_db` | `users` | `is_active` | `TRUE` | New accounts start active |
| `auth_db` | `users` | `login_attempts` | `0` | No prior failed attempts |
| `auth_db` | `refresh_tokens` | `is_revoked` | `FALSE` | New tokens start active |
| `user_db` | `consumer_profiles` | `is_active` | `TRUE` | New profiles start active |
| `user_db` | `consumer_addresses` | `is_primary` | `FALSE` | Explicitly mark primary |
| `meter_db` | `meters` | `is_active` | `TRUE` | New meters start active |
| `meter_db` | `meter_assignments` | `is_active` | `TRUE` | New assignments start active |
| `meter_db` | `meter_readings` | `status` | `'VERIFIED'` | Readings start as verified |
| `meter_db` | `meter_readings` | `units_consumed` | `0.00` | Calculated after validation |
| `billing_db` | `tariff_plans` | `fixed_charges` | `0.00` | Default no fixed charges |
| `billing_db` | `tariff_plans` | `tax_percentage` | `0.00` | Default no tax |
| `billing_db` | `monthly_bills` | `status` | `'UNPAID'` | Bills start unpaid |
| `billing_db` | `daily_bills` | `status` | `'UNPAID'` | Daily bills start unpaid |
| `payment_db` | `payments` | `status` | `'COMPLETED'` | In V1, payments are manual recordings |
| `complaint_db` | `complaints` | `status` | `'OPEN'` | Complaints start as OPEN |
| `complaint_db` | `complaints` | `priority` | `'NORMAL'` | Default priority level |
| `complaint_db` | `complaint_comments` | `is_admin_comment` | `FALSE` | Default to consumer comment |
| `notification_db` | `notification_recipients` | `is_read` | `FALSE` | Notifications start unread |
| `notification_db` | `notification_recipients` | `is_delivered` | `TRUE` | In-app delivery is immediate |

### 12.6 Not-Null Constraints Summary

| Rule | Application |
|---|---|
| **Required fields** | All `id`, foreign key, business-critical data (email, amount, dates) are `NOT NULL` |
| **Nullable fields** | Optional addresses (`address_line2`), end dates (`effective_to`, `assigned_until`), timestamps (`resolved_at`, `read_at`), and references (`assigned_to`) are `NULLABLE` |

---

## 13. Index Recommendations

### 13.1 Index Categories

| Category | Purpose | Examples |
|---|---|---|
| **Primary Key Index** | Automatic — every `PRIMARY KEY` creates a clustered index | All `id` columns |
| **Unique Index** | Enforces uniqueness and speeds up lookups | `email`, `meter_number`, `bill_number` |
| **Foreign Key Index** | Speeds up joins — automatic in some engines, explicit recommended | `consumer_id`, `meter_id`, `complaint_id` |
| **Query Performance Index** | Speeds up frequent WHERE/ORDER BY/SORT queries | `status`, `created_at`, `reading_date` |
| **Composite Index** | Multi-column queries (covering index) | `(consumer_id, is_read)`, `(consumer_id, billing_month)` |

### 13.2 Recommended Indexes by Column

#### `email`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `auth_db` | `users` | UNIQUE | `idx_users_email` |
| `user_db` | `consumer_profiles` | UNIQUE | `idx_consumer_profiles_email` |

**Why needed:** Email is the primary login identifier and a unique identifier for consumers. Every login, registration (uniqueness check), and profile lookup will query by email. Without this index, every login would require a full table scan.

---

#### `consumer_number`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `user_db` | `consumer_profiles` | UNIQUE | `idx_consumer_profiles_consumer_number` |

**Why needed:** The consumer number is the human-readable account identifier used in support interactions, billing statements, and customer queries. Admin and consumer searches by account number require fast lookup.

---

#### `meter_number`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `meter_db` | `meters` | UNIQUE | `idx_meters_meter_number` |

**Why needed:** The meter number is the physical serial number of the electricity meter. Admin operations (assigning meters, looking up meter history) and consumer queries require instant lookup by this identifier.

---

#### `reading_date`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `meter_db` | `meter_readings` | NON-UNIQUE | `idx_meter_readings_reading_date` |
| `billing_db` | `daily_bills` | NON-UNIQUE | `idx_daily_bills_bill_date` |

**Why needed:** Reading date is used in range queries ("show readings for July 2026"), in calculations (billing period aggregation), and in admin reports. A full table scan on date range queries without an index would be prohibitively slow as data grows.

---

#### `bill_number`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `billing_db` | `monthly_bills` | UNIQUE | `idx_monthly_bills_bill_number` |

**Why needed:** Bill numbers are used in payment references, support inquiries, and consumer queries. Uniqueness enforcement and quick lookup require this index.

---

#### `payment_reference`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `payment_db` | `payments` | UNIQUE | `idx_payments_transaction_id` |

**Why needed:** Transaction IDs are the primary reference for payment tracking, reconciliation, and support. Uniqueness enforcement and fast lookup by transaction reference are essential.

---

#### `complaint_status`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `complaint_db` | `complaints` | NON-UNIQUE | `idx_complaints_status` |
| `complaint_db` | `complaints` | COMPOSITE | `idx_complaints_status_assigned` (status, assigned_to) |

**Why needed:** Admin dashboards filter complaints by status ("Show all OPEN complaints"). The composite index `(status, assigned_to)` enables efficient queries like "Show all OPEN complaints assigned to me." Without this, every admin dashboard load would scan thousands of records.

---

#### `notification_recipient`

| Database | Table | Index Type | Index Name |
|---|---|---|---|
| `notification_db` | `notification_recipients` | NON-UNIQUE | `idx_notif_recipients_consumer_id` |
| `notification_db` | `notification_recipients` | COMPOSITE | `idx_notif_recipients_unread` (consumer_id, is_read) |
| `notification_db` | `notification_recipients` | COMPOSITE | `idx_notif_recipients_consumer_date` (consumer_id, created_at) |

**Why needed:** The consumer notification list is one of the most frequently queried views. The composite index `(consumer_id, is_read)` enables instant lookup of unread notification count (shown in the UI header badge). The `(consumer_id, created_at)` composite index powers the paginated notification history sorted by date.

---

### 13.3 Additional Performance Indexes

| Database | Table | Index | Purpose |
|---|---|---|---|
| `auth_db` | `users` | `idx_users_is_active` | Admin user management: filter active/deactivated users |
| `auth_db` | `users` | `idx_users_last_login` | Admin reports: last login activity |
| `auth_db` | `refresh_tokens` | `idx_refresh_tokens_expires` | Token cleanup job: find expired tokens |
| `user_db` | `consumer_profiles` | `idx_consumer_profiles_is_active` | Filter active consumers for broadcast notifications |
| `user_db` | `consumer_profiles` | `idx_consumer_profiles_phone` | Search consumer by phone |
| `meter_db` | `meters` | `idx_meters_is_active` | Filter active/decommissioned meters |
| `meter_db` | `meter_readings` | `idx_meter_readings_status` | Filter suspicious readings for admin review |
| `billing_db` | `monthly_bills` | `idx_monthly_bills_billing_month` | Monthly revenue queries and reports |
| `billing_db` | `monthly_bills` | `idx_monthly_bills_status` | Filter unpaid bills |
| `payment_db` | `payments` | `idx_payments_paid_at` | Date-range payment reports |
| `complaint_db` | `complaints` | `idx_complaints_created_at` | Sort complaints by date |

---

## 14. Audit Fields

### 14.1 Standard Audit Fields

Every table in VOLTARAS follows a consistent audit field pattern:

| Field | Data Type | Constraints | Purpose | Applies To |
|---|---|---|---|---|
| `created_at` | `DATETIME` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Records when the row was created | **All tables** |
| `updated_at` | `DATETIME` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Records the last modification time | All tables **except** read-only/historical tables |

### 14.2 Tables with `created_at` ONLY (No `updated_at`)

These tables are append-only and records do not change after creation:

| Database | Table | Reason |
|---|---|---|
| `auth_db` | `roles` | Roles are predefined and rarely modified |
| `auth_db` | `user_roles` | Once assigned, role changes create new records |
| `auth_db` | `refresh_tokens` | Tokens are revoked, not modified |
| `billing_db` | `bill_line_items` | Bill breakdown is immutable after generation |
| `complaint_db` | `complaint_comments` | Comments are append-only |
| `complaint_db` | `complaint_status_history` | History is append-only audit trail |
| `notification_db` | `notifications` | Notification content is immutable after creation |
| `payment_db` | `payment_methods` | Predefined, rarely modified |
| `complaint_db` | `complaint_categories` | Predefined, rarely modified |

### 14.3 `created_by` and `updated_by` Fields

| Field | Usage | Example |
|---|---|---|
| `created_by` | Used in tables where the creating user/actor is important | Not included as a standard field — instead, specific named fields like `submitted_by`, `generated_by`, `assigned_to`, `author_id`, `changed_by`, `created_by` serve this purpose contextually |
| `updated_by` | Used in tables where tracking who last modified a record is critical | Not included as a standard field — tracked via application-level logging for now |

The external user reference fields that serve as `created_by` equivalents:

| Database | Table | Field | References |
|---|---|---|---|
| `meter_db` | `meter_readings` | `submitted_by` | `auth_db.users.id` |
| `billing_db` | `monthly_bills` | `generated_by` | `auth_db.users.id` |
| `complaint_db` | `complaints` | `assigned_to` | `auth_db.users.id` |
| `complaint_db` | `complaint_comments` | `author_id` | `auth_db.users.id` |
| `complaint_db` | `complaint_status_history` | `changed_by` | `auth_db.users.id` |
| `notification_db` | `notifications` | `created_by` | `auth_db.users.id` |

### 14.4 Soft Delete (`deleted_at`)

**Decision:** VOLTARAS **does not use soft deletes** in V1.

**Rationale:**
- Financial data (bills, payments, readings) must never be deleted — they remain as permanent records.
- Consumer profiles should not be deleted; instead, `is_active = FALSE` deactivates the account.
- Meters should not be deleted; `is_active = FALSE` marks them as decommissioned.
- Complaints should not be deleted; they remain for audit purposes.
- Notifications should not be deleted; they remain for user reference.

**Exception (Future):** If user-requested account deletion is required in V2, soft delete (`deleted_at`) can be added to `consumer_profiles` and `auth_db.users`.

---

## 15. Naming Standards

### 15.1 Database Names

| Rule | Example |
|---|---|
| Lowercase with underscores | `auth_db`, `user_db`, `meter_db` |
| Suffixed with `_db` | `billing_db`, `payment_db` |
| Reflects owning service name | `complaint_db`, `notification_db` |

### 15.2 Table Names

| Rule | Example |
|---|---|
| Lowercase snake_case | `consumer_profiles`, `meter_readings` |
| Plural nouns | `users`, `roles`, `meters`, `payments` |
| Descriptive compound names | `meter_assignments`, `tariff_slabs`, `bill_line_items` |
| No prefixes or suffixes | Use `users` (not `tbl_users` or `users_table`) |

### 15.3 Column Names

| Rule | Example |
|---|---|
| Lowercase snake_case | `meter_number`, `reading_date`, `total_amount` |
| Primary key always `id` | `id` (not `user_id` or `pk_id`) |
| Foreign keys named after referenced table + `_id` | `user_id`, `role_id`, `meter_id`, `complaint_id` |
| Boolean columns use `is_` or `has_` prefix | `is_active`, `is_read`, `is_primary`, `is_delivered` |
| Date-only columns use `_date` suffix | `reading_date`, `bill_date`, `installation_date` |
| Timestamp columns use `_at` suffix | `created_at`, `updated_at`, `paid_at`, `resolved_at` |
| Enum/varchar status columns are singular nouns | `status`, `type`, `priority` |
| Decimal/monetary columns use descriptive names | `total_amount`, `rate_per_unit`, `fixed_charges` |

### 15.4 Primary Key Naming

| Rule | Example |
|---|---|
| Always named `id` | `id BIGINT NOT NULL AUTO_INCREMENT` |
| Composite PKs use the two column names | `PRIMARY KEY (user_id, role_id)` |

### 15.5 Foreign Key Naming

| Rule | Example |
|---|---|
| Pattern: `fk_<child_table>_<parent_table>` | `fk_meter_assignments_meter` |
| Use singular table names in constraint name | `fk_user_roles_user`, `fk_bill_line_items_bill` |
| Keep names under 64 characters (MySQL limit) | `fk_complaint_status_history_complaint` ✓ |

### 15.6 Index Naming

| Index Type | Pattern | Example |
|---|---|---|
| PRIMARY KEY | `PRIMARY` | (MySQL default) |
| UNIQUE | `uq_<table>_<column(s)>` | `uq_reading_per_day` |
| NON-UNIQUE | `idx_<table>_<column(s)>` | `idx_users_email`, `idx_meter_readings_reading_date` |
| COMPOSITE | `idx_<table>_<col1>_<col2>` | `idx_monthly_bills_consumer_month`, `idx_notif_recipients_unread` |

### 15.7 Constraint Naming

| Constraint Type | Pattern | Example |
|---|---|---|
| FOREIGN KEY | `fk_<child>_<parent>` | `fk_meter_readings_assignment` |
| UNIQUE | `uq_<table>_<columns>` | `uq_reading_per_day` |
| CHECK | (Not used in MySQL; enforced at application layer) | — |
| DEFAULT | (Uses MySQL inline syntax) | `DEFAULT TRUE`, `DEFAULT 0.00` |

### 15.8 Enum / Status Value Naming

| Rule | Example |
|---|---|
| UPPER_SNAKE_CASE | `BILLING_ISSUE`, `IN_PROGRESS`, `BANK_TRANSFER` |
| Single-word values are all uppercase | `OPEN`, `PAID`, `INFO`, `HIGH` |
| Multi-word values use underscore | `IN_PROGRESS`, `BANK_TRANSFER`, `NOTIFICATION_SENT` |
| Column type: `VARCHAR` with application-level validation | Stored as strings, not MySQL ENUM type (for flexibility) |

### 15.9 Summary Reference Table

| Element | Case | Example |
|---|---|---|
| Database | `snake_case` + `_db` | `billing_db` |
| Table | `snake_case` plural | `meter_readings` |
| Column | `snake_case` | `total_amount` |
| Primary Key | `id` | `id` |
| Foreign Key | `<table>_id` | `consumer_id` |
| FK Constraint | `fk_<child>_<parent>` | `fk_meter_readings_assignment` |
| Unique Constraint | `uq_<table>_<columns>` | `uq_reading_per_day` |
| Index (non-unique) | `idx_<table>_<columns>` | `idx_users_email` |
| Boolean | `is_` or `has_` prefix | `is_active` |
| Timestamp | `_at` suffix | `created_at` |
| Date | `_date` suffix | `reading_date` |
| Enum Value | `UPPER_SNAKE_CASE` | `IN_PROGRESS` |

---

## 16. Sample MySQL Schema (Documentation Only)

> ⚠️ **Critical Note:** The following are **representative examples** of the CREATE TABLE statements for each database. Complete schemas with all columns, constraints, and indexes were provided in Sections 4–10 of this document. These examples show the core structure of one or two key tables per database.
>
> **Do NOT execute these directly.** Use Flyway migration scripts for actual database creation.

---

### 16.1 auth_db — Core User Table

```sql
-- Representative example: auth_db.users
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login_at   DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_users_email (email),
    INDEX idx_users_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.2 user_db — Core Consumer Profile Table

```sql
-- Representative example: user_db.consumer_profiles
CREATE TABLE consumer_profiles (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    auth_user_id      BIGINT          NOT NULL,
    consumer_number   VARCHAR(50)     NOT NULL,
    full_name         VARCHAR(100)    NOT NULL,
    email             VARCHAR(255)    NOT NULL,
    phone             VARCHAR(20)     NULL,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_consumer_profiles_auth_user_id (auth_user_id),
    UNIQUE INDEX idx_consumer_profiles_consumer_number (consumer_number),
    UNIQUE INDEX idx_consumer_profiles_email (email),
    INDEX idx_consumer_profiles_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.3 meter_db — Core Meter Reading Table

```sql
-- Representative example: meter_db.meter_readings
CREATE TABLE meter_readings (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    meter_assignment_id     BIGINT          NOT NULL,
    reading_date            DATE            NOT NULL,
    meter_value             DECIMAL(12,2)   NOT NULL,
    units_consumed          DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'VERIFIED',
    submitted_by            BIGINT          NOT NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uq_reading_per_day (meter_assignment_id, reading_date),
    INDEX idx_meter_readings_assignment_id (meter_assignment_id),
    INDEX idx_meter_readings_reading_date (reading_date),
    INDEX idx_meter_readings_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.4 billing_db — Core Monthly Bill Table

```sql
-- Representative example: billing_db.monthly_bills
CREATE TABLE monthly_bills (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    bill_number             VARCHAR(50)     NOT NULL,
    consumer_id             BIGINT          NOT NULL,
    billing_month           DATE            NOT NULL,
    total_units             DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_energy_charge     DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    fixed_charges           DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    tax_amount              DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount            DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    generated_by            BIGINT          NOT NULL,
    generated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at                 DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_monthly_bills_bill_number (bill_number),
    UNIQUE INDEX idx_monthly_bills_consumer_month (consumer_id, billing_month),
    INDEX idx_monthly_bills_consumer_id (consumer_id),
    INDEX idx_monthly_bills_billing_month (billing_month),
    INDEX idx_monthly_bills_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.5 payment_db — Core Payment Table

```sql
-- Representative example: payment_db.payments
CREATE TABLE payments (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    transaction_id      VARCHAR(100)    NOT NULL,
    bill_id             BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    amount              DECIMAL(12,2)   NOT NULL,
    payment_method_id   BIGINT          NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    paid_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_payments_transaction_id (transaction_id),
    INDEX idx_payments_bill_id (bill_id),
    INDEX idx_payments_consumer_id (consumer_id),
    INDEX idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.6 complaint_db — Core Complaint Table

```sql
-- Representative example: complaint_db.complaints
CREATE TABLE complaints (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ticket_number       VARCHAR(50)     NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    category_id         BIGINT          NOT NULL,
    subject             VARCHAR(200)    NOT NULL,
    description         TEXT            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    priority            VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    assigned_to         BIGINT          NULL,
    resolved_at         DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_complaints_ticket_number (ticket_number),
    INDEX idx_complaints_consumer_id (consumer_id),
    INDEX idx_complaints_category_id (category_id),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_priority (priority),
    INDEX idx_complaints_assigned_to (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16.7 notification_db — Core Notification Table

```sql
-- Representative example: notification_db.notification_recipients
CREATE TABLE notification_recipients (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    notification_id     BIGINT          NOT NULL,
    consumer_id         BIGINT          NOT NULL,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_notif_recipients_notification_id (notification_id),
    INDEX idx_notif_recipients_consumer_id (consumer_id),
    INDEX idx_notif_recipients_unread (consumer_id, is_read),
    INDEX idx_notif_recipients_consumer_date (consumer_id, created_at),

    CONSTRAINT fk_notif_recipients_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 17. Assumptions and Open Questions

### 17.1 Assumptions

| # | Assumption | Rationale |
|---|---|---|
| A-01 | **MySQL 8.x is the database technology** | Established in Phase 2 (Technology Stack) and Phase 4 (Architecture). All schemas use MySQL-specific syntax (utf8mb4, InnoDB, AUTO_INCREMENT, CURRENT_TIMESTAMP ON UPDATE). |
| A-02 | **Each service has its own MySQL database with separate credentials** | Follows the Database-per-Service pattern. Even if databases run on the same server instance, they use different database names and connection pools. |
| A-03 | **No cross-database foreign keys** | Referential integrity across services is maintained through application-layer REST API calls, not database constraints. This preserves loose coupling. |
| A-04 | **BIGINT is sufficient for all primary keys** | 64-bit integers provide up to 9.2 quintillion rows, which is adequate for the expected scale (thousands to low-hundreds-of-thousands of consumers). |
| A-05 | **DECIMAL is used for all monetary and meter values** | FLOAT/DOUBLE precision issues are avoided. DECIMAL(12,2) handles up to ₹99,999,999,999.99. DECIMAL(10,2) handles up to ₹99,999,999.99 for smaller amounts. |
| A-06 | **VARCHAR for status/enum fields instead of MySQL ENUM type** | VARCHAR with application-level validation is more flexible (easier to add new values without ALTER TABLE) and more portable across databases. |
| A-07 | **Flyway for database migrations** | All schema changes will be managed through Flyway migration scripts in the `backend/resources/db/migration/` directory, as established in Phase 2. |
| A-08 | **V1 does not use soft deletes** | Records are never physically deleted. Accounts are deactivated via `is_active = FALSE`. Historical data is preserved for audit and reporting. |
| A-09 | **Single-currency support (₹ INR)** | All monetary columns use DECIMAL with Indian Rupee in mind. Multi-currency is not supported in V1. |
| A-10 | **V1 uses simulated/manual payment recording** | No real payment gateway integration. Payments are recorded as manual transaction logs. Bill status is updated via service-to-service API call. |
| A-11 | **V1 notifications are in-app only** | No SMS, email, or push notification integration. Notifications are displayed within the web application UI only. |
| A-12 | **Auth_user_id is the external linking mechanism** | The `auth_db.users.id` value is propagated to `user_db.consumer_profiles.auth_user_id` and serves as the canonical user identifier across all services. |
| A-13 | **Consumer numbers are human-readable and unique** | Format: `VOL-YYYY-NNNNNN` (e.g., VOL-2026-000001). Generated by User Service during registration. |
| A-14 | **Billing month is stored as the first day of the month** | e.g., `2026-07-01` represents July 2026. This enables simple date-range queries and comparisons. |

### 17.2 Future Improvements (V2+)

| # | Improvement | Impacted Database | Description |
|---|---|---|---|
| FI-01 | **Refresh Token Support** | `auth_db` | Add the `refresh_tokens` table (already designed as commented-out schema) to support JWT refresh token rotation with short-lived access tokens. |
| FI-02 | **Real Payment Gateway Integration** | `payment_db` | Add columns for gateway transaction ID, gateway response, payment status webhook tracking. May require a `payment_transactions` table for gateway call history. |
| FI-03 | **SMS/Email Notification Delivery** | `notification_db` | Add delivery channel tracking (SMS, EMAIL, PUSH) with external provider status columns. Add `sent_at`, `delivery_attempts`, `delivery_error` columns to `notification_recipients`. |
| FI-04 | **Soft Delete Support** | `user_db`, `auth_db` | Add `deleted_at` column to `consumer_profiles` and `users` to support GDPR-compliant account deletion requests. |
| FI-05 | **Audit Log Tables** | All databases | Introduce dedicated audit log tables (e.g., `user_audit_log`) capturing old/new values for critical data changes in billing, payments, and user profiles. |
| FI-06 | **Read Replicas for Reporting** | `billing_db`, `meter_db` | Deploy read replicas for heavy reporting queries (revenue reports, consumption analytics) to avoid impacting write performance. |
| FI-07 | **Archive/Purge Strategy** | `meter_db`, `billing_db` | Implement data archiving for readings and bills older than a configurable retention period (e.g., 3 years) to manage database growth. |
| FI-08 | **Rate Limiting and Login Attempts** | `auth_db` | Complete the `login_attempts` rate-limiting logic with account lockout after N consecutive failed attempts. |

### 17.3 Open Questions (Requiring Approval)

| # | Question | Options | Recommendation |
|---|---|---|---|
| OQ-01 | **Should consumer profiles support multiple active meters simultaneously?** | (a) Yes — one consumer can have multiple meters (e.g., home + shop) | **Option (b)** for V1 — one active meter per consumer. Multiple meters can be supported in V2 by allowing multiple active `meter_assignments`. |
| | | (b) No — one active meter per consumer in V1 | |
| OQ-02 | **Should partial payments be allowed for a single bill?** | (a) Yes — allow PARTIAL status | **Option (a)** — the `monthly_bills` table already includes a `PARTIAL` status value. The UI and API logic for partial payments can be implemented in V2. V1 requires full payment. |
| | | (b) No — bills must be paid in full | |
| OQ-03 | **Should tariff slabs support seasonal or time-of-day rates?** | (a) Yes — separate slab sets per season | **Option (b)** for V1. Simple flat monthly slabs. Seasonal rates can be introduced by creating separate `tariff_plans` with different effective date ranges. |
| | | (b) No — simple flat monthly slabs | |
| OQ-04 | **Should admin users have consumer profiles?** | (a) Yes — admins also have consumer_profiles rows | **Option (b)** — admins exist only in auth_db with ADMIN role. They do not have consumer profiles, meter readings, bills, or payments. This keeps the data model clean. |
| | | (b) No — admins exist only in auth_db | |
| OQ-05 | **What is the expected peak data volume for V1 launch?** | (a) < 1,000 consumers | **Pending from product team.** This affects index strategy, caching decisions, and whether read replicas are needed at launch. |
| | | (b) 1,000 – 10,000 consumers | |
| | | (c) 10,000+ consumers | |
| OQ-06 | **Should bill numbers include the service name prefix?** | (a) Prefix: `VOL-BILL-YYYY-NNNNNN` | **Option (b)** — cleaner, shorter, and sufficient for uniqueness. The service context is implicit from the database. |
| | | (b) Prefix: `BILL-YYYY-NNNNNN` | |

### 17.4 Decisions Inherited from Previous Phases

| # | Decision | Source Phase | Impact on Database |
|---|---|---|---|
| D-01 | **Microservices Architecture** | Phase 4 | Database-per-Service pattern with 7 databases |
| D-02 | **Complaint Service added** | Phase 4 (approval) | Added `complaint_db` with 4 tables |
| D-03 | **MySQL 8.x** | Phase 2 | All schemas use MySQL-specific features |
| D-04 | **JWT Authentication** | Phase 4 | `auth_db` stores credentials and roles; profile data in `user_db` |
| D-05 | **No real payment gateway in V1** | Phase 3 (Out of Scope) | `payment_db` records manual transactions only |
| D-06 | **No SMS/Email notifications in V1** | Phase 3 (Out of Scope) | `notification_db` is in-app only |
| D-07 | **Manual meter readings** | Phase 3 (Out of Scope) | `meter_db` accepts consumer-submitted readings; no IoT/smart meter integration |
| D-08 | **BCrypt password hashing** | Phase 2 | `users.password_hash` stores BCrypt hash |
| D-09 | **Flyway for migrations** | Phase 2 | All schema changes managed via Flyway scripts |
| D-10 | **Snake_case naming** | Phase 2 | All tables, columns, indexes use snake_case |

---

> **End of Phase 5 — Complete**
> *All 17 sections of the Database Design document have been generated.*
> *`docs/04_DATABASE.md` is complete.*
> *Pending approval to proceed to Phase 6.*
