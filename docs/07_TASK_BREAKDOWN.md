# VOLTARAS — Phase 8: Lightweight Task Breakdown

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 8 — Lightweight Task Breakdown
> **Document:** `docs/07_TASK_BREAKDOWN.md`

---

## 1. Phase 8 Objective

Provide a lightweight, implementation-ready roadmap that sequences every major module of VOLTARAS — backend infrastructure, business services, frontend screens, and integration layers — so that a development team can build the entire platform in the correct dependency order with clear acceptance criteria per module.

---

## 2. Current Project Status

| Artifact | Status |
|---|---|
| `docs/00_PROJECT_UNDERSTANDING.md` | ✅ Complete |
| `docs/01_PROJECT_CONTEXT.md` | ✅ Complete |
| `docs/02_REQUIREMENTS.md` | ✅ Complete |
| `docs/03_ARCHITECTURE.md` | ✅ Complete |
| `docs/04_DATABASE.md` | ✅ Complete |
| `docs/05_API_DESIGN.md` | ✅ Complete |
| `docs/06_UI_DESIGN.md` | ✅ Complete |
| Backend source code | 📋 Not started |
| Frontend source code | 📋 Not started |
| Docker / CI-CD | 📋 Not started |

---

## 3. Backend Implementation Order

| Step | Module | Depends On |
|---|---|---|
| 1 | **Eureka Server** | Nothing |
| 2 | **API Gateway** | Eureka Server |
| 3 | **Backend Common Foundation** | Nothing |
| 4 | **Authentication & JWT** | Common Foundation, Eureka Server, API Gateway |
| 5 | **User Module** | Authentication & JWT |
| 6 | **Meter Reading Module** | User Module |
| 7 | **Tariff Module** | Nothing (standalone CRUD) |
| 8 | **Billing Module** | Meter Reading Module, Tariff Module |
| 9 | **Admin Module** | User Module, Billing Module |
| 10 | **Reports & Analytics** | Billing Module, Payment Module |
| 11 | **Notification Module** | User Module |
| 12 | **Swagger & Validation** | All backend modules |
| 13 | **Backend Testing** | All backend modules |
| 14 | **Docker Preparation** | All backend modules |

### Module Details

#### 1. Eureka Server
| Field | Detail |
|---|---|
| **Purpose** | Service registry for all microservices — enables dynamic discovery |
| **Main deliverables** | Spring Boot app with `@EnableEurekaServer`, `application.yml` with port 8761, health check defaults |
| **Dependencies** | None |
| **Branch name** | `feature/eureka-server` |
| **Acceptance criteria** | Eureka dashboard loads at `http://localhost:8761`; services register and deregister on start/stop |

#### 2. API Gateway
| Field | Detail |
|---|---|
| **Purpose** | Single entry point for all client requests — routes, JWT validation, CORS |
| **Main deliverables** | Spring Cloud Gateway app, route predicates for each service, JWT gateway filter, CORS config |
| **Dependencies** | Eureka Server (runtime) |
| **Branch name** | `feature/api-gateway` |
| **Acceptance criteria** | Routes `/api/auth/**` to Auth Service, `/api/users/**` to User Service, etc. Invalid JWT returns 401. Admin routes return 403 for CONSUMER role. |

#### 3. Backend Common Foundation
| Field | Detail |
|---|---|
| **Purpose** | Shared library for all microservices — DTOs, exceptions, utility classes |
| **Main deliverables** | `voltaras-common` JAR with global exception handler, standard error response, pagination response, date utilities, constants |
| **Dependencies** | Nothing (independent library) |
| **Branch name** | `feature/backend-common` |
| **Acceptance criteria** | Exception handler returns consistent `{status, message, timestamp, details}` shape. Utility classes compile and pass unit tests. |

#### 4. Authentication & JWT
| Field | Detail |
|---|---|
| **Purpose** | User registration, login, JWT token generation and validation |
| **Main deliverables** | Auth Service with `/api/auth/register`, `/api/auth/login`; BCrypt password hashing; JwtTokenProvider; JwtAuthenticationFilter; CustomUserDetailsService; Role seeding for CONSUMER and ADMIN |
| **Dependencies** | Common Foundation, Eureka Server, API Gateway |
| **Branch name** | `feature/backend-auth` |
| **Acceptance criteria** | Register with valid data returns success. Duplicate email returns error. Login returns JWT with `sub`, `userId`, `role`, `exp` claims. Invalid credentials return 401. Deactivated user login returns error. |

#### 5. User Module
| Field | Detail |
|---|---|
| **Purpose** | Consumer profile management, admin user management |
| **Main deliverables** | User Service with `/api/users/{id}` (GET/PUT), `/api/users/{id}/change-password`; admin endpoints for listing/searching/activating/deactivating users; consumer number auto-generation |
| **Dependencies** | Authentication & JWT |
| **Branch name** | `feature/backend-user` |
| **Acceptance criteria** | Consumer views own profile. Profile updates persist. Admin lists all users with search. Toggle active/inactive works. Deactivated user cannot log in. |

#### 6. Meter Reading Module
| Field | Detail |
|---|---|
| **Purpose** | Consumer meter reading submission, validation, and history |
| **Main deliverables** | Meter Reading Service with `/api/readings` (POST, GET); validation (reading > previous, no duplicate date); units-consumed calculation; paginated reading history; admin endpoint to view all readings and flag suspicious entries |
| **Dependencies** | User Module (consumer existence) |
| **Branch name** | `feature/backend-meter` |
| **Acceptance criteria** | Valid reading saves and shows units consumed. Reading value less than previous returns error. Duplicate date returns error. Paginated history returns correct data. Suspicious flagging works. |

#### 7. Tariff Module
| Field | Detail |
|---|---|
| **Purpose** | Admin CRUD for tariff slabs used in bill calculation |
| **Main deliverables** | Tariff Slab endpoints: POST/GET/PUT/DELETE `/api/tariff-slabs`; admin-only access; overlap validation; active/inactive marking |
| **Dependencies** | Nothing (standalone CRUD) |
| **Branch name** | `feature/backend-tariff` |
| **Acceptance criteria** | Admin creates/updates/deletes slabs. Overlapping slabs rejected. Inactive slabs excluded from billing. Non-overlapping range validation. |

#### 8. Billing Module
| Field | Detail |
|---|---|
| **Purpose** | Daily bill computation, monthly bill aggregation, bill history |
| **Main deliverables** | Billing Service with daily bill auto-computation on reading submission; monthly bill generation (admin-triggered); slab-wise breakdown; bill history with pagination/filtering; status tracking (PAID/UNPAID); internal endpoint for Payment Service to update status |
| **Dependencies** | Meter Reading Module (fetches readings), Tariff Module (fetches slabs) |
| **Branch name** | `feature/backend-billing` |
| **Acceptance criteria** | Daily bill computes correctly on reading submission. Monthly bill aggregates correctly. Slab breakdown matches tariff slabs. Admin triggers bill for all/specific consumer. Cannot regenerate same month without force flag. Bill status updates after payment callback. |

#### 9. Admin Module
| Field | Detail |
|---|---|
| **Purpose** | Aggregated admin dashboard endpoints and complaint management |
| **Main deliverables** | Complaint endpoints (raise, list, update status, add resolution); admin dashboard KPI aggregation (total users, readings, bills, payments, pending complaints); ticket number generation |
| **Dependencies** | User Module, Billing Module |
| **Branch name** | `feature/backend-admin` |
| **Acceptance criteria** | Complaint raised with ticket number. Admin views all complaints with filters. Status workflow (OPEN→IN_PROGRESS→RESOLVED→CLOSED). Resolution comments visible. Consumer notified on status change. Dashboard KPIs match database counts. |

#### 10. Reports & Analytics
| Field | Detail |
|---|---|
| **Purpose** | Revenue and consumption reports for admin |
| **Main deliverables** | Report endpoints: monthly revenue report (total collections, collection rate); monthly consumption report (total units, avg/consumer); date range filtering |
| **Dependencies** | Billing Module, Payment Module |
| **Branch name** | `feature/backend-reports` |
| **Acceptance criteria** | Revenue report numbers match payment records. Consumption report numbers match reading/bill records. Date range filter works. Reports include percentage/rate calculations. |

#### 11. Notification Module
| Field | Detail |
|---|---|
| **Purpose** | In-app notifications — auto-generated and admin-sent |
| **Main deliverables** | Notification Service with notification creation (bill generated, payment confirmed, complaint status changed); admin broadcast/targeted endpoints; consumer notification list; mark-as-read (single and bulk) |
| **Dependencies** | User Module (consumer existence) |
| **Branch name** | `feature/backend-notification` |
| **Acceptance criteria** | Auto-notification created on bill generation, payment, complaint status change. Admin broadcast reaches all active consumers. Targeted notification reaches one consumer. Mark-as-read updates unread count. |

#### 12. Swagger & Validation
| Field | Detail |
|---|---|
| **Purpose** | API documentation and consistent input validation across all services |
| **Main deliverables** | Springdoc OpenAPI 3 config per service; `@Schema` annotations on all DTOs; `@Operation` / `@Tag` on all controllers; Jakarta validation annotations on all request DTOs; custom validators for complex rules |
| **Dependencies** | All backend modules (annotations added to existing code) |
| **Branch name** | `feature/backend-swagger` |
| **Acceptance criteria** | Swagger UI loads at `/swagger-ui.html` for each service. All endpoints documented with request/response schemas. Validation error responses are consistent. |

#### 13. Backend Testing
| Field | Detail |
|---|---|
| **Purpose** | Unit and integration tests for all backend modules |
| **Main deliverables** | JUnit 5 + Mockito tests: service-layer unit tests (≥75% coverage), controller integration tests (WebMvcTest), repository tests (@DataJpaTest), test profiles with H2/Testcontainers |
| **Dependencies** | All backend modules |
| **Branch name** | `feature/backend-testing` |
| **Acceptance criteria** | `mvn test` passes for all modules. ≥75% line coverage on service and controller layers. Tests cover success paths, validation errors, and edge cases. |

#### 14. Docker Preparation
| Field | Detail |
|---|---|
| **Purpose** | Containerize all backend services for local and CI development |
| **Main deliverables** | `Dockerfile` per service; `docker-compose.yml` with all services + MySQL containers; multi-stage builds for smaller images; `.env` file for configuration |
| **Dependencies** | All backend modules |
| **Branch name** | `feature/backend-docker` |
| **Acceptance criteria** | `docker-compose up` starts all services. Eureka shows all services registered. Frontend can reach API Gateway through Docker network. Containers restart without data loss. |

---

## 4. Frontend Implementation Order

| Step | Module | Depends On |
|---|---|---|
| 1 | **Frontend Project Setup** | Nothing |
| 2 | **Routing and Layouts** | Frontend Project Setup |
| 3 | **Authentication UI** | Routing and Layouts |
| 4 | **Customer Dashboard** | Authentication UI |
| 5 | **Meter Reading UI** | Customer Dashboard |
| 6 | **Bills and Payments UI** | Customer Dashboard |
| 7 | **Complaints and Notifications UI** | Customer Dashboard |
| 8 | **Analytics UI** | Customer Dashboard |
| 9 | **Admin UI** | Authentication UI |
| 10 | **API Integration** | All UI modules |
| 11 | **Frontend Testing** | API Integration |
| 12 | **Responsive and Accessibility Review** | All frontend modules |

### Module Details

#### 1. Frontend Project Setup
| Field | Detail |
|---|---|
| **Purpose** | Initialize React + TypeScript + Vite project with all dependencies |
| **Main deliverables** | Vite project scaffold, Tailwind CSS config, TypeScript config, Axios setup, project folder structure per `01_PROJECT_CONTEXT.md`, ESLint + Prettier config |
| **Dependencies** | Nothing (independent setup) |
| **Branch name** | `feature/frontend-setup` |
| **Acceptance criteria** | `npm run dev` starts on port 5173. Tailwind utility classes render. TypeScript compiles without errors. Axios instance is configurable. |

#### 2. Routing and Layouts
| Field | Detail |
|---|---|
| **Purpose** | Set up React Router with role-based routing and all page layouts |
| **Main deliverables** | `AppRoutes.tsx` with public, consumer, and admin route groups; `ProtectedRoute.tsx` and `AdminRoute.tsx` guards; `AdminLayout.tsx`, `ConsumerLayout.tsx`, `PublicLayout.tsx`; sidebar, top nav, footer components; lazy-loaded page suspense; 404 and unauthorized pages |
| **Dependencies** | Frontend Project Setup |
| **Branch name** | `feature/frontend-routing` |
| **Acceptance criteria** | Public routes accessible without auth. Consumer routes redirect to login. Admin routes return 403 for CONSUMER role. Sidebar collapses on mobile. Breadcrumbs render correctly. |

#### 3. Authentication UI
| Field | Detail |
|---|---|
| **Purpose** | Login, registration, forgot password, and reset password pages |
| **Main deliverables** | `LoginPage.tsx`, `RegisterPage.tsx`, `ForgotPasswordPage.tsx`, `ResetPasswordPage.tsx`; form validation (email, password strength, confirm match); JWT storage in localStorage; role-based redirect after login; error state handling; loading indicators |
| **Dependencies** | Routing and Layouts |
| **Branch name** | `feature/frontend-auth` |
| **Acceptance criteria** | Registration validates all fields and shows inline errors. Login stores JWT and redirects by role. Invalid credentials show error. Forgot password flow works (validation only). Deactivated account error shown. |

#### 4. Customer Dashboard
| Field | Detail |
|---|---|
| **Purpose** | Consumer landing page with KPI cards, recent activity, and quick actions |
| **Main deliverables** | `DashboardPage.tsx` with stat cards (current month consumption, pending bills, last reading, notifications); quick action buttons (Submit Reading, View Bills, Pay Now); recent notifications widget; loading skeletons; empty state for new users |
| **Dependencies** | Authentication UI |
| **Branch name** | `feature/frontend-customer-dashboard` |
| **Acceptance criteria** | Dashboard loads KPIs within 3 seconds. Stat cards show correct data. Quick action buttons navigate to correct pages. Loading state shows skeleton. Empty state shows helpful message. |

#### 5. Meter Reading UI
| Field | Detail |
|---|---|
| **Purpose** | Submit meter readings and view reading history |
| **Main deliverables** | `SubmitReadingPage.tsx` with meter value input and date picker; validation (value > previous, no future dates, no duplicate); units-consumed confirmation on submit; `ReadingHistoryPage.tsx` with paginated table; date range filter |
| **Dependencies** | Customer Dashboard |
| **Branch name** | `feature/frontend-meter` |
| **Acceptance criteria** | Reading form validates meter value > previous. Duplicate date shows error. Success toast with units consumed shown. History table paginates correctly. Filter by date range works. |

#### 6. Bills and Payments UI
| Field | Detail |
|---|---|
| **Purpose** | View bills (daily/monthly), bill details, make payments, payment history |
| **Main deliverables** | `MyBillsPage.tsx` (bill list with type/status filters); `BillDetailPage.tsx` (slab-wise breakdown); `MakePaymentPage.tsx` (payment form); `PaymentHistoryPage.tsx` (paginated, filterable); PENDING/PAID status badges, Pay Now button on unpaid bills, transaction ID display |
| **Dependencies** | Customer Dashboard |
| **Branch name** | `feature/frontend-bills-payments` |
| **Acceptance criteria** | Bill list shows type, amount, status. Bill detail shows slab breakdown. Pay Now visible only on unpaid bills. Payment recorded shows success with transaction ID. Duplicate payment prevented. |

#### 7. Complaints and Notifications UI
| Field | Detail |
|---|---|
| **Purpose** | Raise complaints, track status, view and manage notifications |
| **Main deliverables** | `ComplaintsPage.tsx` (complaint list with status); `RaiseComplaintPage.tsx` (subject, category, description form); `NotificationsPage.tsx` (list with read/unread icons, mark-all-read); status workflow (OPEN→IN_PROGRESS→RESOLVED→CLOSED); ticket number display |
| **Dependencies** | Customer Dashboard |
| **Branch name** | `feature/frontend-complaints-notifications` |
| **Acceptance criteria** | Complaint form validates minimum description length. Ticket number shown on submit. Status workflow visible in list. Notifications show type icon and date. Unread count updates. Mark all as read works. |

#### 8. Analytics UI
| Field | Detail |
|---|---|
| **Purpose** | Energy analytics with consumption charts, cost breakdown, and comparisons |
| **Main deliverables** | `EnergyAnalyticsPage.tsx` with consumption trend line chart (Recharts/Chart.js); cost breakdown bar chart (by slab); current vs previous month comparison card; peak consumption days highlight; average daily consumption display; interactive chart tooltips |
| **Dependencies** | Customer Dashboard |
| **Branch name** | `feature/frontend-analytics` |
| **Acceptance criteria** | Line chart renders daily consumption for current month. Bar chart shows slab-wise cost. Comparison card shows month-over-month. Peak days highlighted. Charts are interactive on hover. Loading state while data fetches. |

#### 9. Admin UI
| Field | Detail |
|---|---|
| **Purpose** | Admin management screens — users, tariffs, readings, bills, payments, complaints, notifications, reports |
| **Main deliverables** | `AdminDashboardPage.tsx` (KPI cards, charts); `ManageUsersPage.tsx` (search, list, activate/deactivate); `ManageTariffsPage.tsx` (CRUD slab table with overlap validation); `MonitorReadingsPage.tsx` (all readings, flag suspicious); `GenerateBillPage.tsx` (month/year selector, consumer selector, progress); `MonitorPaymentsPage.tsx`; `ManageComplaintsPage.tsx` (status update, resolution form); `SendNotificationsPage.tsx` (broadcast/targeted form); `ReportsPage.tsx` (revenue & consumption tables) |
| **Dependencies** | Authentication UI |
| **Branch name** | `feature/frontend-admin` |
| **Acceptance criteria** | User list searches by name/email. Tariff slab CRUD validates overlap. Bill generation shows progress summary. Complaint status update triggers notification. Broadcast notification reaches consumers. Report tables match API data. All data tables paginate and filter. |

#### 10. API Integration
| Field | Detail |
|---|---|
| **Purpose** | Connect all frontend pages to actual backend REST APIs |
| **Main deliverables** | API service files (`authApi.ts`, `userApi.ts`, `readingApi.ts`, `billApi.ts`, `paymentApi.ts`, `complaintApi.ts`, `notificationApi.ts`, `dashboardApi.ts`); Axios interceptors for JWT injection and 401 handling; loading states wired to API calls; error toasts on failures; pagination/filter params mapped to query strings |
| **Dependencies** | All UI modules |
| **Branch name** | `feature/frontend-api-integration` |
| **Acceptance criteria** | All pages fetch data from real backend. JWT automatically attached to requests. 401 responses redirect to login. Error toasts show on API failures. Pagination and filters pass correct query params. |

#### 11. Frontend Testing
| Field | Detail |
|---|---|
| **Purpose** | Unit and component tests for frontend modules |
| **Main deliverables** | Vitest + React Testing Library tests: component rendering tests, form validation tests, API mock tests with MSW, route guard tests, accessibility assertion tests |
| **Dependencies** | API Integration |
| **Branch name** | `feature/frontend-testing` |
| **Acceptance criteria** | `npm test` passes. Key components tested (login form, reading form, bill detail, complaint form). Form validation shows correct error messages. Route guards redirect unauthenticated users. |

#### 12. Responsive and Accessibility Review
| Field | Detail |
|---|---|
| **Purpose** | Ensure all screens work across breakpoints and meet WCAG 2.1 AA standards |
| **Main deliverables** | Responsive testing on mobile (xs 480px), tablet (md 768px), desktop (lg 1024px+); keyboard navigation audit; screen reader labels (aria-*); color contrast checks; focus indicators; semantic HTML review; touch target sizing |
| **Dependencies** | All frontend modules |
| **Branch name** | `feature/frontend-responsive-accessibility` |
| **Acceptance criteria** | All pages render without horizontal scroll on 480px viewport. All interactive elements keyboard-accessible. Color contrast ≥ 4.5:1 for text. Form fields have associated labels. Focus indicators visible. Touch targets ≥ 44×44px. |

---

## 5. Infrastructure and Integration Order

| Step | Item | Depends On |
|---|---|---|
| 1 | **MySQL Docker Containers** | Nothing |
| 2 | **Flyway Migration Scripts** | Database Design (`docs/04_DATABASE.md`) |
| 3 | **Docker Compose (Backend + DB)** | All backend Dockerfiles |
| 4 | **Docker Compose (Frontend)** | Frontend project |
| 5 | **CI Pipeline (GitHub Actions)** | All backend + frontend Dockerfiles |
| 6 | **Postman API Collection** | All backend APIs |
| 7 | **Environment Configuration** | Nothing (parallel to all) |
| 8 | **README Update** | All modules |

---

## 6. Module Dependency Map

```
Eureka Server
    └── API Gateway
            ├── Auth Service ─────────────────────────┐
            │       └── User Service                   │
            │               ├── Meter Reading Service  │
            │               │       └── Billing Service │
            │               │               ├── Tariff (separate) │
            │               │               └── Payment Service   │
            │               └── Notification Service    │
            │                       └── Complaint (Admin)          │
            └── Reports & Analytics                             │
                                                                │
Frontend ──── API Integration Layer ─────────────────────────────┘

Lateral Dependencies (service-to-service REST):
  Auth Service    → User Service     (create profile on register)
  Billing Service → Meter Reading Service    (fetch readings for billing)
  Billing Service → Tariff Service   (fetch slabs for billing)
  Payment Service → Billing Service  (update bill status)
  Notification    → User Service     (fetch consumer details)
```

---

## 7. Recommended Feature Branch Names

| Module | Branch Name |
|---|---|
| Eureka Server | `feature/eureka-server` |
| API Gateway | `feature/api-gateway` |
| Backend Common | `feature/backend-common` |
| Authentication & JWT | `feature/backend-auth` |
| User Module | `feature/backend-user` |
| Meter Reading Module | `feature/backend-meter` |
| Tariff Module | `feature/backend-tariff` |
| Billing Module | `feature/backend-billing` |
| Admin Module | `feature/backend-admin` |
| Reports & Analytics | `feature/backend-reports` |
| Notification Module | `feature/backend-notification` |
| Swagger & Validation | `feature/backend-swagger` |
| Backend Testing | `feature/backend-testing` |
| Docker Preparation | `feature/backend-docker` |
| Frontend Project Setup | `feature/frontend-setup` |
| Routing and Layouts | `feature/frontend-routing` |
| Authentication UI | `feature/frontend-auth` |
| Customer Dashboard | `feature/frontend-customer-dashboard` |
| Meter Reading UI | `feature/frontend-meter` |
| Bills and Payments UI | `feature/frontend-bills-payments` |
| Complaints and Notifications UI | `feature/frontend-complaints-notifications` |
| Analytics UI | `feature/frontend-analytics` |
| Admin UI | `feature/frontend-admin` |
| API Integration | `feature/frontend-api-integration` |
| Frontend Testing | `feature/frontend-testing` |
| Responsive & Accessibility | `feature/frontend-responsive-accessibility` |

---

## 8. Definition of Done for Each Major Module

| Criterion | Description |
|---|---|
| **Code Complete** | All source code for the module is written and compiles without errors |
| **Unit Tests Pass** | Module-specific unit tests pass (≥75% coverage for service/controller layers) |
| **API Tested** | All API endpoints work correctly (verified via Swagger UI or Postman) |
| **Error Handling** | All error paths return consistent error responses and are handled client-side |
| **UI Renders** | All corresponding UI screens render with real/mocked data (frontend modules) |
| **Responsive** | UI works on mobile, tablet, and desktop viewports (frontend modules) |
| **Accessibility** | Keyboard navigation works, labels present, contrast OK (frontend modules) |
| **No Regression** | Existing module tests still pass after the new module is merged |
| **Branch Merged** | Feature branch merged into `develop` via PR with CI passing |

---

## 9. Testing Checkpoints

| Checkpoint | When | Scope |
|---|---|---|
| **T1** | After Backend Common Foundation | Shared exception handler and pagination response shape verified |
| **T2** | After Auth + User modules | Full registration→login→profile fetch flow tested end-to-end |
| **T3** | After Meter + Tariff + Billing modules | Reading submission→daily bill→monthly bill flow tested end-to-end |
| **T4** | After Payment module | Full billing→payment→bill status update flow tested |
| **T5** | After Notification + Complaint modules | Complaint raise→status update→notification flow tested |
| **T6** | After Reports module | Revenue + consumption report data verified against source data |
| **T7** | After all backend modules | `mvn test` passes across all microservices |
| **T8** | After frontend API integration | All consumer screens connected to real backend |
| **T9** | After admin UI | All admin CRUD operations verified end-to-end |
| **T10** | Final integration | Full user journey: register → login → submit reading → view bill → pay → raise complaint → receive notification |

---

## 10. Pull Request and Merge Workflow

```
main  (production-ready, protected)
  └── develop  (integration branch, protected)
        ├── feature/eureka-server
        ├── feature/api-gateway
        ├── feature/backend-common
        ├── feature/backend-auth
        ├── feature/backend-user
        ├── feature/backend-meter
        ├── feature/backend-tariff
        ├── feature/backend-billing
        ├── feature/backend-admin
        ├── feature/backend-reports
        ├── feature/backend-notification
        ├── feature/backend-swagger
        ├── feature/backend-testing
        ├── feature/backend-docker
        ├── feature/frontend-setup
        ├── feature/frontend-routing
        ├── feature/frontend-auth
        ├── feature/frontend-customer-dashboard
        ├── feature/frontend-meter
        ├── feature/frontend-bills-payments
        ├── feature/frontend-complaints-notifications
        ├── feature/frontend-analytics
        ├── feature/frontend-admin
        ├── feature/frontend-api-integration
        ├── feature/frontend-testing
        └── feature/frontend-responsive-accessibility
```

**Workflow Rules:**

| Step | Action |
|---|---|
| 1 | Create `feature/<name>` branch from latest `develop` |
| 2 | Implement the module with commits following conventional commit format |
| 3 | Run module tests: `mvn test` (backend) or `npm test` (frontend) |
| 4 | Open Pull Request to `develop` with description of changes |
| 5 | At least one reviewer approves the PR |
| 6 | CI checks pass (build + test + lint) |
| 7 | Merge into `develop` using squash merge |
| 8 | Delete the feature branch after merge |
| 9 | After milestone validation, merge `develop` into `main` |

---

## 11. Final Milestone Order

| Milestone | Modules Included | Estimated Effort |
|---|---|---|
| **M1: Infrastructure** | Eureka Server, API Gateway, Backend Common Foundation | Small |
| **M2: Core Authentication** | Auth, User, Frontend Setup, Routing, Auth UI | Medium |
| **M3: Meter & Tariff** | Meter, Tariff, Frontend Meter UI | Medium |
| **M4: Billing Engine** | Billing, Frontend Bills/Payments UI | Medium |
| **M5: Complaints & Notifications** | Admin (complaints), Notification, Frontend Complaints/Notifications UI | Medium |
| **M6: Analytics & Reports** | Reports & Analytics, Frontend Analytics UI | Small |
| **M7: Admin Complete** | Admin Module, Frontend Admin UI | Large |
| **M8: Integration & Polish** | API Integration, Swagger, Frontend Testing, Responsive Review | Medium |
| **M9: Containerization & CI** | Docker, Backend Testing, CI Pipeline | Medium |

---

## 12. Phase 8 Completion Checklist

| # | Item | Status |
|---|---|---|
| 1 | Backend implementation order documented with 14 modules | ✅ |
| 2 | Frontend implementation order documented with 12 modules | ✅ |
| 3 | Infrastructure and integration steps documented | ✅ |
| 4 | Module dependency map showing service-to-service dependencies | ✅ |
| 5 | Recommended feature branch names for all 26 modules | ✅ |
| 6 | Definition of done with 9 acceptance criteria per module | ✅ |
| 7 | 10 testing checkpoints from unit to full integration | ✅ |
| 8 | PR and merge workflow with squash merge strategy | ✅ |
| 9 | 9 final milestones with effort estimates | ✅ |
| 10 | No implementation source code created | ✅ |
| 11 | No existing documentation files modified | ✅ |
| 12 | Roadmap aligns with all existing VOLTARAS documents (00–06) | ✅ |

---

## Summary

### File Created
`docs/07_TASK_BREAKDOWN.md`

### Modules Covered
- **Backend:** 14 modules (Eureka Server, API Gateway, Common Foundation, Auth & JWT, User, Meter Reading, Tariff, Billing, Admin, Reports & Analytics, Notification, Swagger & Validation, Backend Testing, Docker Preparation)
- **Frontend:** 12 modules (Project Setup, Routing & Layouts, Auth UI, Customer Dashboard, Meter Reading UI, Bills & Payments UI, Complaints & Notifications UI, Analytics UI, Admin UI, API Integration, Frontend Testing, Responsive & Accessibility Review)
- **Infrastructure:** 8 items (MySQL containers, Flyway migrations, Docker Compose, CI pipeline, Postman collection, env config, README update)

### Assumptions
1. **Microservices Architecture** — The roadmap follows the database-per-service, Eureka-based microservices architecture defined in `docs/03_ARCHITECTURE.md`
2. **Shared Common Library** — Backend Common Foundation is built as a shared JAR before any business service
3. **Eureka → API Gateway First** — Infrastructure services are implemented before any business logic, as specified
4. **Frontend Depends on Backend** — Frontend routing and UI are built with mocked data initially and wired to real APIs in the API Integration step
5. **Independent Frontend Setup** — The frontend project scaffold is independent of backend and can be started in parallel with backend work
6. **PR-per-Module** — Each module has its own feature branch and PR, keeping changes reviewable
7. **Feature Branch Naming** — All branch names follow the `feature/<scope>-<name>` convention from `docs/01_PROJECT_CONTEXT.md`

### Confirmation: No Code Created
No Java, React, TypeScript, HTML, CSS, or Tailwind source files were created. No backend or frontend application was initialized. No existing documentation files were modified. This document is strictly a task breakdown roadmap.
