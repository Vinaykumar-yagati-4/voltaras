# VOLTARAS — API Design

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 6 — API Design
> **Document:** `docs/05_API_DESIGN.md`

---

## 1. API Design Overview

This document defines the complete REST API surface for all 9 microservices in the VOLTARAS platform. APIs are designed following RESTful principles with consistent naming, request/response structures, error handling, and security patterns.

### Services Covered

| # | Service | Base Path | Route Prefix Through Gateway |
|---|---|---|---|
| 1 | **API Gateway** | — | — |
| 2 | **Eureka Service Registry** | — | — |
| 3 | **Auth Service** | `/api/auth` | `/api/auth/**` |
| 4 | **User Service** | `/api/users` | `/api/users/**` |
| 5 | **Meter Service** | `/api/readings` | `/api/readings/**` |
| 6 | **Billing Service** | `/api/bills` | `/api/bills/**` |
| 7 | **Payment Service** | `/api/payments` | `/api/payments/**` |
| 8 | **Complaint Service** | `/api/complaints` | `/api/complaints/**` |
| 9 | **Notification Service** | `/api/notifications` | `/api/notifications/**` |

---

## 2. API Design Standards

### 2.1 REST Conventions

| Rule | Standard | Example |
|---|---|---|
| Base URL | `/api/{service}` | `/api/auth`, `/api/bills` |
| HTTP Verbs | GET (read), POST (create), PUT (full update), PATCH (partial), DELETE (remove) | `GET /api/bills/{id}` |
| Plural Nouns | All resource paths use plural nouns | `/api/users`, `/api/tariff-slabs` |
| Kebab-case | Multi-word paths use kebab-case | `/api/tariff-slabs`, `/api/meter-readings` |
| Nesting | Related resources are nested | `/api/users/{userId}/addresses` |
| Query Params | camelCase query parameters | `?consumerId=123&page=0&size=10` |
| Versioning | No URL versioning in V1; use Accept header if needed | `/api/bills` (not `/api/v1/bills`) |

### 2.2 HTTP Methods & CRUD Mapping

| HTTP Method | CRUD Operation | Idempotent | Safe |
|---|---|---|---|
| `GET` | Read/Retrieve | ✅ Yes | ✅ Yes |
| `POST` | Create | ❌ No | ❌ No |
| `PUT` | Full Update (replace) | ✅ Yes | ❌ No |
| `PATCH` | Partial Update | ❌ No | ❌ No |
| `DELETE` | Delete | ✅ Yes | ❌ No |

### 2.3 HTTP Status Codes

| Status Code | Usage |
|---|---|
| `200 OK` | Successful GET, PUT, PATCH |
| `201 Created` | Successful POST (resource created) |
| `204 No Content` | Successful DELETE (no response body) |
| `400 Bad Request` | Validation error, missing/invalid fields |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | Authenticated but insufficient role/permissions |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Duplicate resource (email already exists, duplicate reading) |
| `422 Unprocessable Entity` | Business rule violation (reading < previous, overlapping slabs) |
| `500 Internal Server Error` | Unexpected server error |

### 2.4 Request/Response Format

All requests and responses use `Content-Type: application/json` (unless otherwise noted).

**Standard Response Envelope:**

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-07-27T10:30:00Z"
}
```

**Error Response Envelope:**

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "email",
        "message": "Email must be a valid email address"
      }
    ]
  },
  "timestamp": "2026-07-27T10:30:00Z"
}
```

### 2.5 Authentication Header

All protected endpoints require:

```
Authorization: Bearer <jwt_token>
X-Correlation-Id: <uuid>  (optional, for request tracing)
```

### 2.6 Pagination

List endpoints support pagination via query parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `10` | Page size (max 100) |
| `sort` | `string` | `createdAt,desc` | Sort field and direction |

**Paginated Response:**

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "last": false
  }
}
```

### 2.7 Common Request Headers

| Header | Required | Description |
|---|---|---|
| `Authorization` | Yes (protected routes) | `Bearer <token>` |
| `Content-Type` | Yes (POST/PUT/PATCH) | `application/json` |
| `Accept` | No | `application/json` |
| `X-Correlation-Id` | No | UUID for request tracing |

---

## 3. API Gateway

### 3.1 Overview

The **API Gateway** (Spring Cloud Gateway) is the single entry point for all client requests. It does not expose business APIs directly — it routes requests to downstream services and enforces authentication at the gateway level.

### 3.2 Route Configuration

| Route ID | Path Pattern | Target Service | Strip Prefix | Auth Required |
|---|---|---|---|---|
| `auth-route` | `/api/auth/**` | `lb://auth-service` | Yes (strip `/api/auth`) | Public (register/login only) |
| `user-route` | `/api/users/**` | `lb://user-service` | Yes | Yes |
| `reading-route` | `/api/readings/**` | `lb://meter-service` | Yes | Yes |
| `bill-route` | `/api/bills/**` | `lb://billing-service` | Yes | Yes |
| `payment-route` | `/api/payments/**` | `lb://payment-service` | Yes | Yes |
| `complaint-route` | `/api/complaints/**` | `lb://complaint-service` | Yes | Yes |
| `notification-route` | `/api/notifications/**` | `lb://notification-service` | Yes | Yes |

### 3.3 Gateway-Level Security Filters

| Filter | Description |
|---|---|
| **JWT Validation Filter** | Validates JWT signature and expiration on all protected routes before forwarding |
| **Role Check Filter** | For admin-only routes (under `/api/admin/**`), verifies `role = ADMIN` claim in JWT |
| **CORS Filter** | Allows requests only from configured frontend origins |
| **Rate Limiting Filter** | (Future) Limits requests per client IP or user |
| **Correlation ID Filter** | Adds `X-Correlation-Id` header if not present |

### 3.4 Gateway Endpoints

The gateway itself exposes minimal endpoints:

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | No | Health check endpoint (load balancer probe) |
| `GET` | `/actuator/info` | No | Gateway info |
| `GET` | `/fallback/auth` | No | Circuit breaker fallback for Auth Service |
| `GET` | `/fallback/users` | No | Circuit breaker fallback for User Service |
| `GET` | `/fallback/readings` | No | Circuit breaker fallback for Meter Service |
| `GET` | `/fallback/bills` | No | Circuit breaker fallback for Billing Service |
| `GET` | `/fallback/payments` | No | Circuit breaker fallback for Payment Service |
| `GET` | `/fallback/complaints` | No | Circuit breaker fallback for Complaint Service |
| `GET` | `/fallback/notifications` | No | Circuit breaker fallback for Notification Service |

---

## 4. Eureka Service Registry

### 4.1 Overview

The **Eureka Service Registry** handles service discovery. It exposes Eureka REST endpoints for service registration and discovery. These are used internally by services and are not exposed to the frontend.

### 4.2 Eureka Endpoints (Internal Only)

| Method | Path | Description |
|---|---|---|
| `POST` | `/eureka/apps/{appName}` | Register a service instance |
| `PUT` | `/eureka/apps/{appName}/{instanceId}` | Send heartbeat/renew lease |
| `GET` | `/eureka/apps/{appName}` | Get all instances of a service |
| `GET` | `/eureka/apps` | Get all registered services |
| `DELETE` | `/eureka/apps/{appName}/{instanceId}` | Deregister a service instance |
| `GET` | `/eureka/status` | Eureka server status |

> **Note:** These endpoints are for inter-service communication only. They are not proxied through the API Gateway and are not accessible from the frontend.

---

## 5. Auth Service APIs

**Base Path:** `/api/auth` (via gateway)
**Owns:** `auth_db`
**Public Endpoints:** Register, Login
**Protected Endpoints:** All others

### 5.1 Endpoint Summary

| # | Method | Path | Auth | Role | Description |
|---|---|---|---|---|---|
| 1 | `POST` | `/api/auth/register` | No | — | Register a new consumer account |
| 2 | `POST` | `/api/auth/login` | No | — | Authenticate and receive JWT token |
| 3 | `POST` | `/api/auth/logout` | Yes | ANY | Logout (client-side token invalidation) |
| 4 | `POST` | `/api/auth/change-password` | Yes | ANY | Change current user's password |
| 5 | `GET` | `/api/auth/me` | Yes | ANY | Get current authenticated user's details |
| 6 | `POST` | `/api/auth/refresh` | No | — | (Future) Refresh expired JWT token |

### 5.2 Endpoint Details

---

#### 5.2.1 POST /api/auth/register

Register a new consumer account.

**Request Body:**

```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "address": {
    "addressLine1": "123 Green Street",
    "addressLine2": "Apt 4B",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  },
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
```

**Validation Rules:**
- `fullName`: 2–100 characters, required
- `email`: Valid email format, unique, max 255 characters
- `phone`: 10-digit format, required
- `password`: Min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit
- `confirmPassword`: Must match `password`

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "consumerNumber": "VOL-2026-000001",
    "email": "john@example.com",
    "fullName": "John Doe",
    "message": "Registration successful. Please log in."
  }
}
```

**Error Responses:**
- `400 Bad Request` — Validation failure
- `409 Conflict` — Email already registered

---

#### 5.2.2 POST /api/auth/login

Authenticate and receive JWT token.

**Request Body:**

```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "role": "CONSUMER",
    "consumerNumber": "VOL-2026-000001",
    "fullName": "John Doe"
  }
}
```

**Error Responses:**
- `401 Unauthorized` — Invalid email or password
- `403 Forbidden` — Account is deactivated

---

#### 5.2.3 POST /api/auth/logout

Invalidate the current session (client-side token removal).

**Headers:** `Authorization: Bearer <token>`

**Request Body:** None

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

#### 5.2.4 POST /api/auth/change-password

Change the authenticated user's password.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!",
  "confirmNewPassword": "NewSecurePass456!"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request` — Validation failure or new password same as old
- `401 Unauthorized` — Current password is incorrect

---

#### 5.2.5 GET /api/auth/me

Get the currently authenticated user's details.

**Headers:** `Authorization: Bearer <token>`

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "john@example.com",
    "role": "CONSUMER",
    "isActive": true,
    "lastLoginAt": "2026-07-27T10:30:00Z",
    "createdAt": "2026-07-01T08:00:00Z"
  }
}
```

---

### 5.3 Internal Endpoints (Auth Service → Other Services Only)

| Method | Path | Description | Called By |
|---|---|---|---|
| `POST` | `/api/auth/internal/validate` | Validate JWT and return user details | API Gateway, other services |
| `GET` | `/api/auth/internal/users/{userId}` | Get auth user details by ID (no auth required — internal network only) | User Service, Billing Service |

---

## 6. User Service APIs

**Base Path:** `/api/users` (via gateway)
**Owns:** `user_db`
**Auth:** All endpoints require JWT
**Internal Endpoints:** Prefixed with `/api/users/internal`

### 6.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `GET` | `/api/users/me` | CONSUMER | Get own profile |
| 2 | `PUT` | `/api/users/me` | CONSUMER | Update own profile |
| 3 | `GET` | `/api/users/me/addresses` | CONSUMER | Get own addresses |
| 4 | `POST` | `/api/users/me/addresses` | CONSUMER | Add a new address |
| 5 | `PUT` | `/api/users/me/addresses/{addressId}` | CONSUMER | Update an address |
| 6 | `DELETE` | `/api/users/me/addresses/{addressId}` | CONSUMER | Delete an address |
| 7 | `GET` | `/api/users/me/meters` | CONSUMER | Get own assigned meters |
| 8 | `GET` | `/api/admin/users` | ADMIN | List all consumers |
| 9 | `GET` | `/api/admin/users/{userId}` | ADMIN | Get any consumer profile |
| 10 | `PUT` | `/api/admin/users/{userId}/status` | ADMIN | Activate/deactivate a consumer |

### 6.2 Endpoint Details

---

#### 6.2.1 GET /api/users/me

Get the authenticated consumer's profile.

**Headers:** `Authorization: Bearer <token>`

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "consumerNumber": "VOL-2026-000001",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "9876543210",
    "isActive": true,
    "createdAt": "2026-07-01T08:00:00Z",
    "updatedAt": "2026-07-27T10:30:00Z",
    "addresses": [
      {
        "id": 1,
        "addressLine1": "123 Green Street",
        "addressLine2": "Apt 4B",
        "city": "Mumbai",
        "state": "Maharashtra",
        "pincode": "400001",
        "isPrimary": true
      }
    ]
  }
}
```

---

#### 6.2.2 PUT /api/users/me

Update the authenticated consumer's profile.

**Request Body:**

```json
{
  "fullName": "John Updated Doe",
  "phone": "9988776655"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "fullName": "John Updated Doe",
    "phone": "9988776655",
    "updatedAt": "2026-07-27T11:00:00Z"
  }
}
```

---

#### 6.2.3 GET /api/admin/users

List all consumers with search and pagination (Admin only).

**Headers:** `Authorization: Bearer <token>` (ADMIN role)

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `search` | `string` | No | Search by name or email |
| `status` | `string` | No | Filter by `ACTIVE` or `INACTIVE` |
| `page` | `int` | No | Page number (default 0) |
| `size` | `int` | No | Page size (default 10) |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "consumerNumber": "VOL-2026-000001",
        "fullName": "John Doe",
        "email": "john@example.com",
        "phone": "9876543210",
        "isActive": true,
        "lastLoginAt": "2026-07-27T10:30:00Z",
        "createdAt": "2026-07-01T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

---

#### 6.2.4 PUT /api/admin/users/{userId}/status

Activate or deactivate a consumer account (Admin only).

**Request Body:**

```json
{
  "isActive": false
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "User account has been deactivated"
}
```

---

### 6.3 Internal Endpoints (User Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `POST` | `/api/users/internal` | Create a consumer profile (during registration) | Auth Service |
| `GET` | `/api/users/internal/{userId}` | Get consumer profile by ID | Billing, Payment, Complaint, Notification Services |
| `GET` | `/api/users/internal/batch` | Get multiple consumer profiles by IDs | Notification Service (broadcast) |
| `GET` | `/api/users/internal/exists/{userId}` | Check if a consumer exists | All services (for external ref validation) |

---

## 7. Meter Service APIs

**Base Path:** `/api/readings` (via gateway)
**Owns:** `meter_db`
**Auth:** All endpoints require JWT

### 7.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `POST` | `/api/readings` | CONSUMER | Submit a daily meter reading |
| 2 | `GET` | `/api/readings` | CONSUMER | Get own reading history |
| 3 | `GET` | `/api/readings/current` | CONSUMER | Get last submitted reading |
| 4 | `GET` | `/api/readings/{readingId}` | CONSUMER | Get reading details by ID |
| 5 | `GET` | `/api/admin/readings` | ADMIN | View all consumer readings |
| 6 | `PATCH` | `/api/admin/readings/{readingId}/status` | ADMIN | Flag a reading as suspicious |
| 7 | `GET` | `/api/admin/meters` | ADMIN | List all meters |
| 8 | `POST` | `/api/admin/meters` | ADMIN | Register a new meter |
| 9 | `PUT` | `/api/admin/meters/{meterId}` | ADMIN | Update meter details |
| 10 | `POST` | `/api/admin/meters/{meterId}/assign` | ADMIN | Assign meter to a consumer |

### 7.2 Endpoint Details

---

#### 7.2.1 POST /api/readings

Submit a daily meter reading.

**Headers:** `Authorization: Bearer <token>` (CONSUMER)

**Request Body:**

```json
{
  "meterValue": 1250.50,
  "readingDate": "2026-07-27"
}
```

**Validation Rules:**
- `meterValue`: Must be >= 0 and > last submitted reading
- `readingDate`: Must not be in the future; must not have a reading already for this date

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "readingId": 42,
    "meterValue": 1250.50,
    "previousMeterValue": 1200.00,
    "unitsConsumed": 50.50,
    "readingDate": "2026-07-27",
    "status": "VERIFIED",
    "submittedAt": "2026-07-27T10:35:00Z"
  }
}
```

**Error Responses:**
- `400 Bad Request` — Validation failure
- `409 Conflict` — Reading already submitted for this date
- `422 Unprocessable Entity` — Meter value must be greater than previous reading

---

#### 7.2.2 GET /api/readings

Get paginated reading history for the authenticated consumer.

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fromDate` | `date` | No | Filter readings from this date |
| `toDate` | `date` | No | Filter readings to this date |
| `page` | `int` | No | Page number (default 0) |
| `size` | `int` | No | Page size (default 10) |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "readingDate": "2026-07-27",
        "meterValue": 1250.50,
        "unitsConsumed": 50.50,
        "status": "VERIFIED",
        "submittedAt": "2026-07-27T10:35:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15
  }
}
```

---

#### 7.2.3 PATCH /api/admin/readings/{readingId}/status

Flag a reading as suspicious (Admin only).

**Request Body:**

```json
{
  "status": "SUSPICIOUS",
  "remarks": "Reading shows unusually high consumption compared to historical average"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "readingId": 42,
    "status": "SUSPICIOUS",
    "remarks": "Reading shows unusually high consumption compared to historical average",
    "updatedAt": "2026-07-27T11:00:00Z"
  }
}
```

---

### 7.3 Internal Endpoints (Meter Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `GET` | `/api/readings/internal/consumer/{consumerId}` | Get readings for a consumer in date range | Billing Service |
| `GET` | `/api/readings/internal/consumer/{consumerId}/last` | Get last reading for a consumer | Billing Service (for validation) |
| `GET` | `/api/readings/internal/{readingId}` | Get reading by ID | Billing Service |

**Internal Query Parameters for Billing Service:**

`GET /api/readings/internal/consumer/{consumerId}?fromDate=2026-07-01&toDate=2026-07-31`

---

## 8. Billing Service APIs

**Base Path:** `/api/bills` (via gateway)
**Owns:** `billing_db`
**Auth:** All endpoints require JWT

### 8.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `GET` | `/api/bills` | CONSUMER | Get own bill history |
| 2 | `GET` | `/api/bills/monthly` | CONSUMER | Get own monthly bills |
| 3 | `GET` | `/api/bills/daily` | CONSUMER | Get own daily bills |
| 4 | `GET` | `/api/bills/{billId}` | CONSUMER | Get bill details with line items |
| 5 | `GET` | `/api/bills/{billId}/items` | CONSUMER | Get slab-wise breakdown of a bill |
| 6 | `GET` | `/api/admin/bills` | ADMIN | View all bills |
| 7 | `POST` | `/api/admin/bills/generate` | ADMIN | Generate monthly bills |
| 8 | `GET` | `/api/admin/tariff-plans` | ADMIN | List all tariff plans |
| 9 | `POST` | `/api/admin/tariff-plans` | ADMIN | Create a new tariff plan |
| 10 | `PUT` | `/api/admin/tariff-plans/{planId}` | ADMIN | Update a tariff plan |
| 11 | `DELETE` | `/api/admin/tariff-plans/{planId}` | ADMIN | Delete a tariff plan |
| 12 | `GET` | `/api/admin/tariff-slabs` | ADMIN | List all tariff slabs |
| 13 | `POST` | `/api/admin/tariff-slabs` | ADMIN | Create a tariff slab |
| 14 | `PUT` | `/api/admin/tariff-slabs/{slabId}` | ADMIN | Update a tariff slab |
| 15 | `DELETE` | `/api/admin/tariff-slabs/{slabId}` | ADMIN | Delete a tariff slab |

### 8.2 Endpoint Details

---

#### 8.2.1 GET /api/bills

Get paginated bill history for the authenticated consumer.

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `type` | `string` | No | `MONTHLY` or `DAILY` (default: both) |
| `status` | `string` | No | `PAID` or `UNPAID` |
| `month` | `date` | No | Filter by billing month (e.g., `2026-07-01`) |
| `page` | `int` | No | Page number |
| `size` | `int` | No | Page size |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "billNumber": "BILL-2026-07-0001",
        "type": "MONTHLY",
        "billingMonth": "2026-07-01",
        "totalUnits": 350.00,
        "totalAmount": 1205.00,
        "status": "UNPAID",
        "generatedAt": "2026-07-27T00:00:00Z",
        "dueDate": "2026-08-15"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

#### 8.2.2 GET /api/bills/{billId}

Get detailed bill information with slab-wise breakdown.

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "billNumber": "BILL-2026-07-0001",
    "type": "MONTHLY",
    "billingMonth": "2026-07-01",
    "totalUnits": 350.00,
    "status": "UNPAID",
    "generatedAt": "2026-07-27T00:00:00Z",
    "paidAt": null,
    "lineItems": [
      {
        "slabName": "0–100 Units",
        "unitsInSlab": 100.00,
        "ratePerUnit": 3.50,
        "lineAmount": 350.00
      },
      {
        "slabName": "101–200 Units",
        "unitsInSlab": 100.00,
        "ratePerUnit": 4.50,
        "lineAmount": 450.00
      },
      {
        "slabName": "201+ Units",
        "unitsInSlab": 150.00,
        "ratePerUnit": 6.00,
        "lineAmount": 900.00
      }
    ],
    "summary": {
      "totalEnergyCharge": 1700.00,
      "fixedCharges": 50.00,
      "taxPercentage": 5.00,
      "taxAmount": 87.50,
      "totalAmount": 1837.50
    }
  }
}
```

---

#### 8.2.3 POST /api/admin/bills/generate

Generate monthly bills for consumers (Admin only).

**Request Body:**

```json
{
  "billingMonth": "2026-07-01",
  "consumerIds": [],
  "generateForAll": true
}
```

**Validation:**
- `billingMonth`: Must be in the past (cannot generate future bills)
- `consumerIds`: If not empty, generate only for specified consumers; if empty and `generateForAll` is true, generate for all consumers
- A bill for the same consumer and month must not already exist (unless `force` flag is set)

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "billingMonth": "2026-07-01",
    "totalConsumers": 25,
    "billsGenerated": 25,
    "totalAmount": 45937.50,
    "errors": []
  }
}
```

---

### 8.3 Internal Endpoints (Billing Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `PATCH` | `/api/bills/internal/{billId}/status` | Update bill status to PAID | Payment Service |
| `GET` | `/api/bills/internal/{billId}` | Get bill by ID for payment validation | Payment Service |

**Internal PATCH Request (from Payment Service):**

```json
{
  "status": "PAID",
  "paidAt": "2026-07-27T12:00:00Z"
}
```

---

## 9. Payment Service APIs

**Base Path:** `/api/payments` (via gateway)
**Owns:** `payment_db`
**Auth:** All endpoints require JWT

### 9.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `POST` | `/api/payments` | CONSUMER | Make a payment against a bill |
| 2 | `GET` | `/api/payments` | CONSUMER | Get own payment history |
| 3 | `GET` | `/api/payments/{paymentId}` | CONSUMER | Get payment details |
| 4 | `GET` | `/api/admin/payments` | ADMIN | View all payments |
| 5 | `GET` | `/api/admin/payments/{paymentId}` | ADMIN | View payment details |

### 9.2 Endpoint Details

---

#### 9.2.1 POST /api/payments

Record a payment against an unpaid bill (V1 — simulated/manual).

**Request Body:**

```json
{
  "billId": 10,
  "amount": 1837.50,
  "paymentMethodId": 1,
  "transactionRef": "CHQ-123456"
}
```

**Validation Rules:**
- `billId`: Must reference an existing, UNPAID bill
- `amount`: Must match the bill's `totalAmount` (full payment only in V1)
- `paymentMethodId`: Must reference an active payment method
- Duplicate payment: Bill must not already have a COMPLETED payment

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "paymentId": 100,
    "transactionId": "PAY-20260727-0001",
    "billId": 10,
    "billNumber": "BILL-2026-07-0001",
    "amount": 1837.50,
    "paymentMethod": "Bank Transfer",
    "status": "COMPLETED",
    "paidAt": "2026-07-27T12:00:00Z"
  }
}
```

**Error Responses:**
- `400 Bad Request` — Amount does not match bill total
- `409 Conflict` — Bill already paid
- `404 Not Found` — Bill not found

---

#### 9.2.2 GET /api/payments

Get paginated payment history for the authenticated consumer.

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fromDate` | `date` | No | Filter payments from this date |
| `toDate` | `date` | No | Filter payments to this date |
| `page` | `int` | No | Page number |
| `size` | `int` | No | Page size |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 100,
        "transactionId": "PAY-20260727-0001",
        "billNumber": "BILL-2026-07-0001",
        "billingMonth": "2026-07-01",
        "amount": 1837.50,
        "paymentMethod": "Bank Transfer",
        "status": "COMPLETED",
        "paidAt": "2026-07-27T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 20,
    "totalPages": 2
  }
}
```

---

### 9.3 Internal Endpoints (Payment Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `GET` | `/api/payments/internal/bill/{billId}` | Get payment status for a bill | Billing Service (for status sync) |

---

## 10. Complaint Service APIs

**Base Path:** `/api/complaints` (via gateway)
**Owns:** `complaint_db`
**Auth:** All endpoints require JWT

### 10.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `POST` | `/api/complaints` | CONSUMER | Raise a new complaint |
| 2 | `GET` | `/api/complaints` | CONSUMER | Get own complaint history |
| 3 | `GET` | `/api/complaints/{complaintId}` | CONSUMER | Get complaint details with comments |
| 4 | `POST` | `/api/complaints/{complaintId}/comments` | CONSUMER | Add a comment to own complaint |
| 5 | `GET` | `/api/admin/complaints` | ADMIN | View all complaints |
| 6 | `GET` | `/api/admin/complaints/{complaintId}` | ADMIN | View complaint details |
| 7 | `PATCH` | `/api/admin/complaints/{complaintId}/status` | ADMIN | Update complaint status |
| 8 | `POST` | `/api/admin/complaints/{complaintId}/comments` | ADMIN | Add admin resolution comment |
| 9 | `PUT` | `/api/admin/complaints/{complaintId}/assign` | ADMIN | Assign complaint to an admin |

### 10.2 Endpoint Details

---

#### 10.2.1 POST /api/complaints

Raise a new complaint.

**Request Body:**

```json
{
  "categoryId": 1,
  "subject": "Incorrect bill amount for July 2026",
  "description": "My bill shows 350 units consumed but I only used about 200 units. Please review."
}
```

**Validation Rules:**
- `categoryId`: Must reference an active complaint category
- `subject`: 10–200 characters, required
- `description`: Min 20 characters, required

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "complaintId": 5,
    "ticketNumber": "CMP-20260727-0001",
    "status": "OPEN",
    "priority": "NORMAL",
    "createdAt": "2026-07-27T14:00:00Z"
  }
}
```

---

#### 10.2.2 PATCH /api/admin/complaints/{complaintId}/status

Update the status of a complaint (Admin only).

**Request Body:**

```json
{
  "status": "IN_PROGRESS"
}
```

**Valid Status Transitions:** `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "complaintId": 5,
    "ticketNumber": "CMP-20260727-0001",
    "previousStatus": "OPEN",
    "currentStatus": "IN_PROGRESS",
    "updatedAt": "2026-07-27T15:00:00Z"
  }
}
```

---

#### 10.2.3 GET /api/admin/complaints

List all complaints with filters (Admin only).

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `status` | `string` | No | Filter by status |
| `categoryId` | `int` | No | Filter by category |
| `priority` | `string` | No | Filter by priority |
| `assignedTo` | `int` | No | Filter by assigned admin |
| `fromDate` | `date` | No | Filter from creation date |
| `toDate` | `date` | No | Filter to creation date |
| `page` | `int` | No | Page number |
| `size` | `int` | No | Page size |

---

### 10.3 Internal Endpoints (Complaint Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `GET` | `/api/complaints/internal/count` | Get count of complaints by status | Dashboard Service (for KPIs) |

---

## 11. Notification Service APIs

**Base Path:** `/api/notifications` (via gateway)
**Owns:** `notification_db`
**Auth:** All endpoints require JWT

### 11.1 Endpoint Summary

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `GET` | `/api/notifications` | CONSUMER | Get own notifications |
| 2 | `GET` | `/api/notifications/unread-count` | CONSUMER | Get count of unread notifications |
| 3 | `PATCH` | `/api/notifications/{notificationId}/read` | CONSUMER | Mark a notification as read |
| 4 | `PATCH` | `/api/notifications/read-all` | CONSUMER | Mark all notifications as read |
| 5 | `POST` | `/api/admin/notifications` | ADMIN | Send a notification (broadcast or targeted) |

### 11.2 Endpoint Details

---

#### 11.2.1 GET /api/notifications

Get paginated list of notifications for the authenticated consumer.

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `unreadOnly` | `boolean` | No | Filter unread only (default false) |
| `page` | `int` | No | Page number |
| `size` | `int` | No | Page size |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 200,
        "title": "Bill Generated",
        "message": "Your monthly bill for July 2026 has been generated. Amount: ₹1,837.50",
        "type": "INFO",
        "isRead": false,
        "referenceType": "BILL",
        "referenceId": 10,
        "createdAt": "2026-07-27T00:00:00Z"
      }
    ],
    "unreadCount": 3,
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2
  }
}
```

---

#### 11.2.2 GET /api/notifications/unread-count

Get the count of unread notifications for the authenticated consumer.

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  }
}
```

---

#### 11.2.3 PATCH /api/notifications/{notificationId}/read

Mark a specific notification as read.

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Notification marked as read"
}
```

---

#### 11.2.4 PATCH /api/notifications/read-all

Mark all notifications as read for the authenticated consumer.

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": {
    "markedCount": 15
  }
}
```

---

#### 11.2.5 POST /api/admin/notifications

Send a notification (broadcast or targeted). Admin only.

**Request Body:**

```json
{
  "title": "Scheduled Maintenance",
  "message": "The system will be offline for maintenance on August 1st from 2:00 AM to 4:00 AM.",
  "type": "WARNING",
  "target": "ALL_CONSUMERS",
  "consumerId": null
}
```

**Target Options:**
- `ALL_CONSUMERS` — Broadcast to all active consumers
- `SPECIFIC_CONSUMER` — Send to one consumer (requires `consumerId`)

**Success Response (201 Created):**

```json
{
  "success": true,
  "data": {
    "notificationId": 201,
    "title": "Scheduled Maintenance",
    "type": "WARNING",
    "target": "ALL_CONSUMERS",
    "recipientCount": 25,
    "sentAt": "2026-07-27T16:00:00Z"
  }
}
```

---

### 11.3 Internal Endpoints (Notification Service)

| Method | Path | Description | Called By |
|---|---|---|---|
| `POST` | `/api/notifications/internal` | Create a system-generated notification | Billing Service, Payment Service, Complaint Service |

**Internal Request Body:**

```json
{
  "title": "Payment Confirmed",
  "message": "Your payment of ₹1,837.50 for July 2026 bill has been confirmed.",
  "type": "INFO",
  "referenceType": "PAYMENT",
  "referenceId": 100,
  "consumerIds": [1],
  "createdBy": 0
}
```

---

## 12. Dashboard & Analytics APIs

**Note:** Dashboard and analytics endpoints aggregate data from multiple services. In the microservices architecture, these can be implemented in a dedicated Dashboard Service, or within the gateway with aggregated calls.

### 12.1 Consumer Dashboard

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 1 | `GET` | `/api/dashboard/consumer` | CONSUMER | Get consumer dashboard summary |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "currentMonth": {
      "consumption": 350.00,
      "billAmount": 1837.50,
      "billStatus": "UNPAID",
      "dailyAverage": 11.67
    },
    "previousMonth": {
      "consumption": 320.00,
      "billAmount": 1680.00,
      "billStatus": "PAID"
    },
    "comparison": {
      "consumptionChange": 9.38,
      "amountChange": 9.38
    },
    "pendingBillsCount": 1,
    "lastReadingDate": "2026-07-27",
    "lastReadingValue": 1250.50,
    "unreadNotifications": 3,
    "openComplaints": 1
  }
}
```

### 12.2 Energy Analytics

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 2 | `GET` | `/api/dashboard/analytics` | CONSUMER | Get energy analytics and charts data |

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `period` | `string` | No | `DAILY`, `WEEKLY`, `MONTHLY` (default `DAILY`) |
| `month` | `date` | No | Month to analyze (default: current month) |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "trend": [
      { "date": "2026-07-01", "consumption": 12.5 },
      { "date": "2026-07-02", "consumption": 10.2 }
    ],
    "costBreakdown": [
      { "slabName": "0–100 Units", "units": 100, "amount": 350.00 },
      { "slabName": "101–200 Units", "units": 100, "amount": 450.00 },
      { "slabName": "201+ Units", "units": 150, "amount": 900.00 }
    ],
    "peakDays": [
      { "date": "2026-07-15", "consumption": 18.5 },
      { "date": "2026-07-22", "consumption": 16.2 }
    ],
    "averageDailyConsumption": 11.67,
    "totalConsumption": 350.00
  }
}
```

### 12.3 Admin Dashboard

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 3 | `GET` | `/api/dashboard/admin` | ADMIN | Get admin dashboard KPIs |

**Success Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "totalUsers": 25,
    "activeUsers": 22,
    "totalReadings": 3500,
    "totalBillsGenerated": 25,
    "totalPayments": 20,
    "totalAmountCollected": 36750.00,
    "pendingComplaints": 3,
    "collectionRate": 80.00
  }
}
```

### 12.4 Admin Reports

| # | Method | Path | Role | Description |
|---|---|---|---|---|
| 4 | `GET` | `/api/admin/reports/revenue` | ADMIN | Get monthly revenue report |
| 5 | `GET` | `/api/admin/reports/consumption` | ADMIN | Get monthly consumption report |

**Revenue Report Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `fromMonth` | `date` | No | Start month (e.g., 2026-01-01) |
| `toMonth` | `date` | No | End month (e.g., 2026-12-01) |

**Revenue Report Response:**

```json
{
  "success": true,
  "data": {
    "monthlyData": [
      {
        "month": "2026-07-01",
        "totalBillsGenerated": 25,
        "totalAmountBilled": 45937.50,
        "totalPaymentsCollected": 36750.00,
        "collectionRate": 80.00
      }
    ]
  }
}
```

---

## 13. Internal Service APIs Summary

Internal APIs are endpoints that are **not exposed through the API Gateway**. They are used exclusively for inter-service communication within the internal network.

### 13.1 Internal Endpoints by Service

| Service | Internal Endpoint | Purpose | Consumed By |
|---|---|---|---|
| **Auth Service** | `POST /internal/validate` | Validate JWT token | API Gateway, all services |
| **Auth Service** | `GET /internal/users/{id}` | Get auth user details | User, Billing, Complaint, Notification |
| **User Service** | `POST /internal` | Create consumer profile | Auth Service |
| **User Service** | `GET /internal/{id}` | Get consumer profile | Billing, Payment, Complaint, Notification |
| **User Service** | `GET /internal/batch` | Get multiple profiles | Notification Service (broadcast) |
| **User Service** | `GET /internal/exists/{id}` | Check consumer exists | All services |
| **Meter Service** | `GET /internal/consumer/{id}` | Get readings for date range | Billing Service |
| **Meter Service** | `GET /internal/consumer/{id}/last` | Get last reading | Billing Service |
| **Billing Service** | `PATCH /internal/{billId}/status` | Update bill payment status | Payment Service |
| **Billing Service** | `GET /internal/{billId}` | Get bill for validation | Payment Service |
| **Payment Service** | `GET /internal/bill/{billId}` | Get payment status for bill | Billing Service |
| **Complaint Service** | `GET /internal/count` | Get complaint counts by status | Dashboard Service |
| **Notification Service** | `POST /internal` | Create system notification | Billing, Payment, Complaint |

---

## 14. Inter-Service Communication Matrix

### 14.1 Synchronous REST Calls

| Triggering Event | Caller Service | Endpoint Called | Purpose |
|---|---|---|---|
| User Registration | Auth Service | `POST /internal` (User Service) | Create consumer profile |
| Reading Submitted | Meter Service | — | (Future: trigger daily bill computation) |
| Bill Generation | Billing Service | `GET /internal/consumer/{id}` (Meter Service) | Fetch readings for the billing period |
| Bill Generation | Billing Service | `GET /internal/{id}` (User Service) | Validate consumer exists |
| Payment Recorded | Payment Service | `PATCH /internal/{billId}/status` (Billing Service) | Mark bill as PAID |
| Payment Confirmed | Payment Service | `POST /internal` (Notification Service) | Send payment confirmation notification |
| Bill Generated | Billing Service | `POST /internal` (Notification Service) | Send bill generation notification |
| Complaint Status Changed | Complaint Service | `POST /internal` (Notification Service) | Notify consumer of status change |
| Notification Sent | Notification Service | `GET /internal/batch` (User Service) | Fetch all active consumer IDs for broadcast |
| Dashboard Load | Dashboard/Client | Multiple service calls | Aggregate data for KPIs and charts |

### 14.2 Communication Pattern

```
                  ┌─────────────────────────────┐
                  │     API Gateway (8080)       │
                  │  - Routes client requests    │
                  │  - Validates JWT             │
                  └──────────┬──────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
       ┌──────▼──────┐ ┌────▼────┐  ┌──────▼──────┐
       │ Auth Service│ │User Svc │  │Meter Service│
       │  :8081      │ │ :8082   │  │  :8083      │
       └──────┬──────┘ └────┬────┘  └──────┬──────┘
              │              │              │
              │              │              │
       ┌──────▼──────┐ ┌────▼────┐  ┌──────▼──────┐
       │Billing Svc  │ │Payment  │  │ Complaint   │
       │  :8084      │ │ :8085   │  │  :8086      │
       └──────┬──────┘ └────┬────┘  └──────┬──────┘
              │              │              │
              │        ┌─────▼──────┐       │
              │        │Notification│       │
              └────────┤ Service    ├───────┘
                       │ :8087      │
                       └────────────┘
```

---

## 15. API Security Summary

### 15.1 Authentication & Authorization Matrix

| Endpoint Pattern | Auth Required | Role Required | Notes |
|---|---|---|---|
| `POST /api/auth/register` | ❌ No | — | Public registration |
| `POST /api/auth/login` | ❌ No | — | Public login |
| `GET /api/auth/me` | ✅ Yes | ANY | Current user info |
| `POST /api/auth/change-password` | ✅ Yes | ANY | Self-service |
| `/api/users/me/**` | ✅ Yes | CONSUMER | Own profile only |
| `/api/readings` (consumer) | ✅ Yes | CONSUMER | Own readings only |
| `/api/bills` (consumer) | ✅ Yes | CONSUMER | Own bills only |
| `/api/payments` (consumer) | ✅ Yes | CONSUMER | Own payments only |
| `/api/complaints` (consumer) | ✅ Yes | CONSUMER | Own complaints only |
| `/api/notifications` (consumer) | ✅ Yes | CONSUMER | Own notifications only |
| `/api/dashboard/consumer` | ✅ Yes | CONSUMER | Consumer dashboard |
| `/api/admin/**` | ✅ Yes | ADMIN | All admin operations |
| `/api/internal/**` | Internal Only | — | Service-to-service only (not exposed via gateway) |

### 15.2 Authorization Enforcement

| Level | Mechanism | Applied At |
|---|---|---|
| **Gateway** | Route-based role check | API Gateway — routes prefixed with `/api/admin/` reject non-ADMIN tokens |
| **Service** | Resource ownership check | Each service verifies the JWT `userId` matches the resource owner for consumer-level operations |
| **Service** | Method-level `@PreAuthorize` | Spring Security method annotations for fine-grained control |

### 15.3 Resource Ownership Rules

| Operation | Rule |
|---|---|
| View profile | Consumer can only view own profile (`userId` match) |
| Submit reading | Consumer submits reading for own meter assignment |
| View bills | Consumer can only view own bills |
| Make payment | Consumer can only pay own bills |
| Raise complaint | Consumer can only raise complaint for self |
| View notifications | Consumer can only view own notifications |
| Admin operations | Admin can access any consumer's data |

---

## 16. Error Codes Reference

### 16.1 Standard Error Codes

| HTTP Status | Error Code | Description |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Request body validation failed |
| `400` | `MISSING_PARAMETER` | Required query parameter missing |
| `400` | `INVALID_FORMAT` | Field format is invalid |
| `401` | `UNAUTHORIZED` | Missing or invalid JWT token |
| `401` | `TOKEN_EXPIRED` | JWT token has expired |
| `403` | `FORBIDDEN` | Insufficient role permissions |
| `403` | `ACCOUNT_DEACTIVATED` | User account has been deactivated |
| `404` | `RESOURCE_NOT_FOUND` | Requested resource does not exist |
| `409` | `DUPLICATE_RESOURCE` | Resource already exists (email, duplicate reading) |
| `409` | `ALREADY_PAID` | Bill has already been paid |
| `422` | `BUSINESS_RULE_VIOLATION` | Business validation failed (reading < previous, amount mismatch) |
| `429` | `RATE_LIMIT_EXCEEDED` | Too many requests (Future) |
| `500` | `INTERNAL_ERROR` | Unexpected server error |
| `503` | `SERVICE_UNAVAILABLE` | Downstream service unavailable |

### 16.2 Error Response Body Format

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for the request",
    "details": [
      {
        "field": "email",
        "message": "must be a well-formed email address"
      }
    ]
  },
  "timestamp": "2026-07-27T10:30:00Z",
  "path": "/api/auth/register",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

> **End of Phase 6 — API Design**
> *All 9 service API specifications have been documented.*
> *`docs/05_API_DESIGN.md` is complete.*
> *Pending approval to proceed to Phase 7 (UI Flow).*
