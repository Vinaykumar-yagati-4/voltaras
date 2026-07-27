# VOLTARAS — Requirements Engineering

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0
> **Phase:** 3 — Requirements Engineering
> **Document:** `docs/02_REQUIREMENTS.md`

---

## 1. Functional Requirements

### FR-01: Authentication

| ID | Requirement | Priority |
|---|---|---|
| FR-01.01 | The system shall allow new users to register with full name, email, phone number, address, and password. | High |
| FR-01.02 | The system shall allow registered users to log in using email and password. | High |
| FR-01.03 | The system shall issue a JWT token upon successful login. | High |
| FR-01.04 | The system shall validate the JWT token on every authenticated request. | High |
| FR-01.05 | The system shall support password encryption using BCrypt before storing. | High |
| FR-01.06 | The system shall allow users to log out (invalidate token on client side). | Medium |
| FR-01.07 | The system shall differentiate between Consumer and Admin roles during registration/login. | High |
| FR-01.08 | The system shall show appropriate validation errors for invalid credentials, duplicate emails, and missing fields. | High |

### FR-02: User Profile

| ID | Requirement | Priority |
|---|---|---|
| FR-02.01 | The system shall allow authenticated users to view their profile details. | High |
| FR-02.02 | The system shall allow users to update their profile (name, phone, address). | High |
| FR-02.03 | The system shall allow users to change their password with current password verification. | Medium |
| FR-02.04 | The system shall display the user's consumer ID/account number. | Medium |
| FR-02.05 | The system shall allow admin to view a list of all registered users. | High |
| FR-02.06 | The system shall allow admin to activate or deactivate a user account. | High |

### FR-03: Meter Reading Management

| ID | Requirement | Priority |
|---|---|---|
| FR-03.01 | The system shall allow consumers to submit daily meter readings with the current meter value and date. | High |
| FR-03.02 | The system shall validate that a new reading is greater than the previous reading. | High |
| FR-03.03 | The system shall prevent duplicate reading submissions for the same date. | High |
| FR-03.04 | The system shall calculate and display the units consumed (current reading − previous reading). | High |
| FR-03.05 | The system shall allow consumers to view their reading history in a paginated list. | Medium |
| FR-03.06 | The system shall allow admin to view all consumer readings and flag anomalous readings. | Medium |
| FR-03.07 | The system shall mark readings that exceed expected consumption thresholds as "suspicious." | Low |

### FR-04: Bill Generation

| ID | Requirement | Priority |
|---|---|---|
| FR-04.01 | The system shall compute a daily bill based on the day's consumption and applicable tariff slabs. | High |
| FR-04.02 | The system shall compute a monthly bill by aggregating daily consumption and applying slab-based tariff rates. | High |
| FR-04.03 | The system shall display a detailed bill breakdown: units consumed per slab, rate per slab, total amount, fixed charges, and taxes. | High |
| FR-04.04 | The system shall allow consumers to view all their generated bills (daily and monthly). | High |
| FR-04.05 | The system shall allow consumers to view the details of a specific bill. | High |
| FR-04.06 | The system shall mark bills as PAID or UNPAID based on payment status. | High |
| FR-04.07 | The system shall allow admin to trigger bill generation for all consumers or a specific consumer. | High |
| FR-04.08 | The system shall allow admin to view all generated bills across the system. | Medium |

### FR-05: Payment Management

| ID | Requirement | Priority |
|---|---|---|
| FR-05.01 | The system shall allow consumers to make a payment against an unpaid bill. | High |
| FR-05.02 | The system shall record payment transaction details: amount, date, payment method, transaction ID, and bill reference. | High |
| FR-05.03 | The system shall update bill status to PAID upon successful payment recording. | High |
| FR-05.04 | The system shall allow consumers to view their complete payment history. | High |
| FR-05.05 | The system shall allow admin to view all payments across the system. | Medium |
| FR-05.06 | The system shall support recording of multiple payment methods (cash, bank transfer, card — manual entry). | Low |

### FR-06: Dashboard & Energy Analytics

| ID | Requirement | Priority |
|---|---|---|
| FR-06.01 | The system shall display a consumer dashboard showing: current month consumption, pending bills, recent readings, and notifications summary. | High |
| FR-06.02 | The system shall display a consumption trend chart (daily/weekly/monthly). | High |
| FR-06.03 | The system shall display a cost breakdown chart showing consumption per slab. | Medium |
| FR-06.04 | The system shall show peak consumption days in the current billing period. | Medium |
| FR-06.05 | The system shall show comparative analytics: current month vs. previous month consumption and cost. | Medium |
| FR-06.06 | The system shall display the average daily consumption for the current billing period. | Medium |
| FR-06.07 | The system shall display an admin dashboard with KPIs: total users, total readings, total bills, total payments, pending complaints. | High |
| FR-06.08 | The system shall allow admin to filter dashboard data by date range. | Medium |

### FR-07: Complaint Management

| ID | Requirement | Priority |
|---|---|---|
| FR-07.01 | The system shall allow consumers to raise a complaint with subject, description, and category (e.g., Billing Issue, Meter Issue, Payment Issue, Other). | High |
| FR-07.02 | The system shall assign a unique complaint ticket number to each complaint. | High |
| FR-07.03 | The system shall allow consumers to view their complaint history with status tracking. | High |
| FR-07.04 | The system shall show complaint status: OPEN, IN_PROGRESS, RESOLVED, CLOSED. | High |
| FR-07.05 | The system shall allow admin to view all complaints and update their status. | High |
| FR-07.06 | The system shall allow admin to add resolution comments to a complaint. | Medium |
| FR-07.07 | The system shall notify the consumer when the complaint status changes. | Medium |

### FR-08: Notifications

| ID | Requirement | Priority |
|---|---|---|
| FR-08.01 | The system shall send a notification when a bill is generated. | High |
| FR-08.02 | The system shall send a notification when a payment is confirmed. | High |
| FR-08.03 | The system shall send a notification when a complaint status is updated. | High |
| FR-08.04 | The system shall allow consumers to view all their notifications in a list. | High |
| FR-08.05 | The system shall mark notifications as read/unread. | Medium |
| FR-08.06 | The system shall allow admin to send broadcast notifications to all consumers. | Medium |
| FR-08.07 | The system shall allow admin to send targeted notifications to a specific consumer. | Medium |

### FR-09: Admin Management

| ID | Requirement | Priority |
|---|---|---|
| FR-09.01 | The system shall allow admin to view all registered consumers. | High |
| FR-09.02 | The system shall allow admin to activate or deactivate consumer accounts. | High |
| FR-09.03 | The system shall allow admin to create, update, and delete tariff slabs. | High |
| FR-09.04 | The system shall allow admin to view all submitted meter readings with consumer details. | High |
| FR-09.05 | The system shall allow admin to generate bills for all consumers or a specific consumer. | High |
| FR-09.06 | The system shall allow admin to view all payments and their statuses. | Medium |
| FR-09.07 | The system shall allow admin to view, update status, and resolve complaints. | High |
| FR-09.08 | The system shall allow admin to send notifications (broadcast or targeted). | Medium |

### FR-10: Reports

| ID | Requirement | Priority |
|---|---|---|
| FR-10.01 | The system shall generate a monthly revenue report showing total collections. | Medium |
| FR-10.02 | The system shall generate a consumption report showing total units sold per month. | Medium |
| FR-10.03 | The system shall generate a complaints report showing complaint categories and resolution times. | Low |
| FR-10.04 | The system shall allow admin to export reports (CSV/PDF). | Low |

---

## 2. Non-Functional Requirements

### NFR-01: Performance

| ID | Requirement | Target |
|---|---|---|
| NFR-01.01 | Page load time for authenticated pages shall not exceed 3 seconds. | ≤ 3 seconds |
| NFR-01.02 | API response time for read operations shall not exceed 500 ms (under normal load). | ≤ 500 ms |
| NFR-01.03 | API response time for write operations shall not exceed 1 second. | ≤ 1 second |
| NFR-01.04 | The system shall support at least 100 concurrent users without degradation. | ≥ 100 concurrent users |
| NFR-01.05 | Database queries for bill generation shall complete within 5 seconds for up to 10,000 consumers. | ≤ 5 seconds |

### NFR-02: Security

| ID | Requirement | Target |
|---|---|---|
| NFR-02.01 | All passwords shall be hashed using BCrypt. | BCrypt |
| NFR-02.02 | All API endpoints (except login/register) shall require JWT authentication. | JWT |
| NFR-02.03 | Admin endpoints shall be protected with role-based access control. | Role check |
| NFR-02.04 | SQL injection shall be prevented via parameterized queries (JPA/Hibernate). | No raw SQL |
| NFR-02.05 | XSS protection shall be implemented on all user input fields. | Input sanitization |
| NFR-02.06 | CORS configuration shall restrict frontend origin only. | Whitelist origins |
| NFR-02.07 | Sensitive data (passwords, tokens) shall not appear in logs or error responses. | Masked |

### NFR-03: Scalability

| ID | Requirement | Target |
|---|---|---|
| NFR-03.01 | The backend architecture shall support horizontal scaling by adding more instances behind a load balancer. | Scalable |
| NFR-03.02 | The database schema shall support indexing on frequently queried columns (userId, date, status). | Indexed |
| NFR-03.03 | Stateless authentication (JWT) shall allow any backend instance to handle any request. | Stateless |

### NFR-04: Availability

| ID | Requirement | Target |
|---|---|---|
| NFR-04.01 | The system shall be available 99.5% of the time during business hours (8 AM – 10 PM). | 99.5% uptime |
| NFR-04.02 | Planned maintenance windows shall be communicated 48 hours in advance. | 48 hr notice |

### NFR-05: Reliability

| ID | Requirement | Target |
|---|---|---|
| NFR-05.01 | Data consistency shall be maintained for all financial transactions (bills, payments). | ACID compliant |
| NFR-05.02 | The system shall validate all input data before processing. | Pre-validation |
| NFR-05.03 | Automatic database backups shall be performed daily for production. | Daily backup |

### NFR-06: Maintainability

| ID | Requirement | Target |
|---|---|---|
| NFR-06.01 | The codebase shall follow a layered architecture (Controller → Service → Repository). | Layered |
| NFR-06.02 | Code shall follow SOLID principles and be testable. | SOLID |
| NFR-06.03 | Unit test coverage shall be at least 75% for service and controller layers. | ≥ 75% |
| NFR-06.04 | API documentation shall be auto-generated using Swagger/OpenAPI. | Swagger |

### NFR-07: Usability

| ID | Requirement | Target |
|---|---|---|
| NFR-07.01 | The UI shall be responsive and work on desktop (1024px+) and tablet (768px+). | Responsive |
| NFR-07.02 | Form validation errors shall be displayed inline next to the relevant field. | Inline errors |
| NFR-07.03 | Loading states shall be shown during API calls (spinners/skeletons). | Visual feedback |
| NFR-07.04 | Navigation shall be intuitive with clear labels and breadcrumbs. | Clear nav |

### NFR-08: Logging

| ID | Requirement | Target |
|---|---|---|
| NFR-08.01 | All API requests shall be logged with HTTP method, endpoint, status code, and response time. | Request logging |
| NFR-08.02 | All business-critical actions (login, registration, bill generation, payment) shall be logged at INFO level. | Business logging |
| NFR-08.03 | All exceptions shall be logged at ERROR level with stack trace in development. | Error logging |

### NFR-09: Auditability

| ID | Requirement | Target |
|---|---|---|
| NFR-09.01 | All payment transactions shall have an immutable audit trail with timestamp, user ID, and amount. | Audit trail |
| NFR-09.02 | All bill generation operations shall be logged with the generating admin's identity. | Bill audit |
| NFR-09.03 | All user profile changes shall be logged with old and new values. | Profile audit |

---

## 3. User Roles

### Role 1: Consumer

**Description:** An end-user who has registered on the platform to track their electricity consumption, view bills, make payments, and manage their account.

**Responsibilities:**
- Register and maintain their own profile.
- Submit daily meter readings.
- View daily and monthly bills.
- Make payments for unpaid bills.
- View payment history.
- Raise and track complaints.
- View notifications.
- View dashboard and energy analytics.

**Permissions:**

| Module | Permission |
|---|---|
| Authentication | Register, Login, Logout |
| Profile | View own profile, Update own profile, Change password |
| Meter Readings | Submit own reading, View own reading history |
| Bills | View own bills (daily & monthly), View bill details |
| Payments | Make payment, View own payment history |
| Complaints | Raise complaint, View own complaints, Track status |
| Notifications | View own notifications, Mark as read |
| Dashboard | View personal analytics dashboard |

### Role 2: Admin

**Description:** A system administrator who manages the platform, users, tariff slabs, bills, payments, complaints, and notifications.

**Responsibilities:**
- Manage all consumer accounts (activate/deactivate).
- Configure and maintain tariff slabs.
- Monitor all meter readings.
- Generate bills for consumers.
- Monitor payments.
- Resolve consumer complaints.
- Send system notifications.
- View reports and system-wide dashboard.

**Permissions:**

| Module | Permission |
|---|---|
| Authentication | Login, Logout (pre-registered) |
| User Management | View all users, Search users, Activate/Deactivate users |
| Tariff Slabs | Create, Update, Delete, View all slabs |
| Meter Readings | View all readings, Flag suspicious readings |
| Bill Generation | Generate bills for all/specific consumers, View all bills |
| Payments | View all payments, Verify payments |
| Complaints | View all complaints, Update status, Add resolution, Close |
| Notifications | Send broadcast notifications, Send targeted notifications |
| Reports | View revenue report, View consumption report, Export |
| Dashboard | View admin analytics dashboard |

---

## 4. User Stories

### Authentication

---

**US-AUTH-01: Consumer Registration**

> **As a** new consumer  
> **I want to** create an account by providing my name, email, phone, address, and password  
> **So that** I can access the platform to track my electricity consumption and bills.

**Acceptance Criteria:**
- [ ] Registration form accepts: full name, email, phone, address, password, confirm password.
- [ ] Email is validated for format and uniqueness.
- [ ] Phone is validated for 10-digit format.
- [ ] Password is validated for minimum 8 characters with at least one uppercase, one lowercase, and one digit.
- [ ] Password and confirm password must match.
- [ ] On successful registration, a success message is displayed and user is redirected to login.
- [ ] On validation failure, inline error messages are shown.
- [ ] Duplicate email registration shows "Email already registered" error.

---

**US-AUTH-02: User Login**

> **As a** registered user (consumer or admin)  
> **I want to** log in using my email and password  
> **So that** I can access my account and use the platform features.

**Acceptance Criteria:**
- [ ] Login form accepts email and password.
- [ ] Valid credentials return a JWT token and redirect to the appropriate dashboard (consumer or admin).
- [ ] Invalid credentials show "Invalid email or password" error.
- [ ] Deactivated accounts show "Account is deactivated. Contact admin." error.
- [ ] JWT token expires after 24 hours.
- [ ] Token is stored securely on the client side.

---

### User Profile

---

**US-PRO-01: View Profile**

> **As a** consumer  
> **I want to** view my profile details  
> **So that** I can see my registered information and consumer account number.

**Acceptance Criteria:**
- [ ] Profile page displays: full name, email, phone, address, consumer account number, registration date.
- [ ] Email is displayed but not editable.
- [ ] Consumer account number is auto-generated and read-only.

---

**US-PRO-02: Update Profile**

> **As a** consumer  
> **I want to** update my name, phone, and address  
> **So that** my contact information stays current.

**Acceptance Criteria:**
- [ ] Editable fields: full name, phone, address.
- [ ] Changes are saved and persisted after clicking "Save".
- [ ] Success toast/message is shown on update.
- [ ] Validation rules apply: name (2–50 chars), phone (10 digits), address (max 200 chars).

---

**US-PRO-03: Change Password**

> **As a** consumer  
> **I want to** change my password by providing my current password and a new password  
> **So that** I can keep my account secure.

**Acceptance Criteria:**
- [ ] Form requires: current password, new password, confirm new password.
- [ ] Current password is verified before allowing change.
- [ ] New password follows same validation rules as registration.
- [ ] On success, password is updated and user is informed.
- [ ] Wrong current password shows "Current password is incorrect" error.

---

### Meter Reading Management

---

**US-READ-01: Submit Daily Reading**

> **As a** consumer  
> **I want to** submit my daily electricity meter reading (current meter value and date)  
> **So that** my consumption and bill are calculated accurately.

**Acceptance Criteria:**
- [ ] Form accepts: meter reading value (numeric, positive), reading date (date picker).
- [ ] Current meter value must be greater than the last submitted reading.
- [ ] Only one reading per day is allowed.
- [ ] If duplicate date is submitted, show "Reading already submitted for this date" error.
- [ ] Units consumed (current − previous) is calculated and displayed on confirmation.
- [ ] On success, the reading is saved and a confirmation is shown.

---

**US-READ-02: View Reading History**

> **As a** consumer  
> **I want to** view my past meter readings in a paginated list  
> **So that** I can track my consumption over time.

**Acceptance Criteria:**
- [ ] List displays: date, meter value, units consumed, and status (verified/suspicious).
- [ ] List is paginated (10 readings per page).
- [ ] Readings are sorted by date in descending order.
- [ ] Search/filter by date range is available.

---

### Bill Generation

---

**US-BILL-01: View Daily Bill**

> **As a** consumer  
> **I want to** view my computed daily bill for each day I submitted a reading  
> **So that** I can see my daily electricity cost in real time.

**Acceptance Criteria:**
- [ ] Daily bill shows: date, units consumed, applicable slab rate(s), amount per slab, total amount.
- [ ] Daily bills are auto-computed when reading is submitted.
- [ ] Unpaid daily bills are clearly marked.

---

**US-BILL-02: View Monthly Bill**

> **As a** consumer  
> **I want to** view my monthly bill with a full breakdown  
> **So that** I can understand my total monthly charges, slab-wise consumption, and pay my bill.

**Acceptance Criteria:**
- [ ] Monthly bill shows: billing period, total units consumed, slab-wise breakdown (units per slab × rate), total energy charge, fixed charges, total amount.
- [ ] Bill status (PAID/UNPAID) is clearly displayed.
- [ ] If unpaid, a "Pay Now" button is shown.
- [ ] Monthly bill is generated when admin triggers bill generation for the period.

---

**US-BILL-03: View Bill History**

> **As a** consumer  
> **I want to** view all my generated bills (daily and monthly)  
> **So that** I can track my billing history.

**Acceptance Criteria:**
- [ ] List shows: bill period, type (daily/monthly), total amount, status (PAID/UNPAID), and date.
- [ ] Clicking a bill shows detailed breakdown.
- [ ] List is paginated and filterable by month and status.

---

### Payment Management

---

**US-PAY-01: Make Payment**

> **As a** consumer  
> **I want to** make a payment against an unpaid bill  
> **So that** I can clear my electricity dues.

**Acceptance Criteria:**
- [ ] "Pay Now" is available only on unpaid bills.
- [ ] Payment form shows: bill amount (read-only), payment method (dropdown), transaction reference (optional).
- [ ] On submission, payment is recorded and bill status is updated to PAID.
- [ ] A success message with transaction ID is shown.
- [ ] Duplicate payment for the same bill is prevented.

---

**US-PAY-02: View Payment History**

> **As a** consumer  
> **I want to** view all my past payments  
> **So that** I can track what I have paid and when.

**Acceptance Criteria:**
- [ ] Payment history displays: date, bill period, amount paid, payment method, transaction ID, status.
- [ ] List is paginated and sorted by date descending.
- [ ] Filter by date range is available.

---

### Dashboard & Energy Analytics

---

**US-DASH-01: View Consumer Dashboard**

> **As a** consumer  
> **I want to** see a dashboard with my current month consumption, pending bills, recent readings, and notifications  
> **So that** I can get a quick overview of my electricity status.

**Acceptance Criteria:**
- [ ] Dashboard widgets: current month consumption (units), current month bill amount (if generated), number of pending bills, last reading date.
- [ ] Recent notifications are displayed (top 5).
- [ ] Quick action buttons: "Submit Reading", "View Bills", "Pay Now".
- [ ] Dashboard loads within 3 seconds.

---

**US-DASH-02: View Energy Analytics**

> **As a** consumer  
> **I want to** see energy analytics charts showing my consumption trends, cost breakdown, and peak usage  
> **So that** I can understand my usage patterns and find ways to save energy.

**Acceptance Criteria:**
- [ ] Line chart: daily consumption trend for current month.
- [ ] Bar chart: cost breakdown by slab.
- [ ] Comparison card: current month vs. previous month (units and cost).
- [ ] Peak consumption days are highlighted.
- [ ] Average daily consumption is displayed.
- [ ] Charts are interactive (hover to see values).

---

### Complaint Management

---

**US-COMP-01: Raise a Complaint**

> **As a** consumer  
> **I want to** raise a complaint with a subject, description, and category  
> **So that** I can report issues related to my bill, meter, or payment.

**Acceptance Criteria:**
- [ ] Complaint form: subject (required), category (dropdown: Billing Issue, Meter Issue, Payment Issue, Other), description (required, min 20 chars).
- [ ] On submission, a unique ticket number (e.g., `CMP-20260727-0001`) is generated and displayed.
- [ ] Initial status is set to OPEN.
- [ ] Consumer can view the complaint in their history immediately.

---

**US-COMP-02: Track Complaint Status**

> **As a** consumer  
> **I want to** view my complaint history and track the status of each complaint  
> **So that** I know if my issue has been acknowledged, is being worked on, or has been resolved.

**Acceptance Criteria:**
- [ ] Complaint list: ticket number, subject, category, status, raised date, last updated date.
- [ ] Status values: OPEN, IN_PROGRESS, RESOLVED, CLOSED.
- [ ] Clicking a complaint shows full details including admin resolution comments.
- [ ] Status changes are clearly visible.

---

### Notifications

---

**US-NOT-01: Receive Notifications**

> **As a** consumer  
> **I want to** receive notifications when a bill is generated, payment is confirmed, or complaint status changes  
> **So that** I stay informed about important account activities.

**Acceptance Criteria:**
- [ ] Notifications are generated for: new bill, payment confirmation, complaint status change.
- [ ] Notification includes: message, type, date, and a link to the relevant page.
- [ ] Unread notifications are visually distinguished (bold/badge).

---

**US-NOT-02: View and Manage Notifications**

> **As a** consumer  
> **I want to** view all my notifications and mark them as read  
> **So that** I can stay organized and not miss important updates.

**Acceptance Criteria:**
- [ ] Notification list: message, type icon, date, read/unread status.
- [ ] Clicking a notification marks it as read and navigates to the relevant section.
- [ ] "Mark All as Read" button is available.

---

### Admin Management

---

**US-ADM-01: Manage Users**

> **As an** admin  
> **I want to** view all registered consumers, search for them, and activate or deactivate accounts  
> **So that** I can manage platform access and handle account issues.

**Acceptance Criteria:**
- [ ] User list: name, email, phone, status, registration date, last login.
- [ ] Search by name or email.
- [ ] Toggle button to activate/deactivate a user.
- [ ] Deactivated users cannot log in.
- [ ] Confirmation dialog before deactivating.

---

**US-ADM-02: Manage Tariff Slabs**

> **As an** admin  
> **I want to** create, update, and delete tariff slabs (unit range, rate per unit)  
> **So that** billing is calculated according to the current tariff structure.

**Acceptance Criteria:**
- [ ] Slab form: slab name, unit from, unit to, rate per unit (₹).
- [ ] Slabs are ordered by unit range and must not overlap.
- [ ] A slab can be marked as active/inactive.
- [ ] Deleting a slab shows a confirmation.
- [ ] Changes to slabs affect future bills only.

---

**US-ADM-03: Monitor Readings**

> **As an** admin  
> **I want to** view all consumer-submitted meter readings and flag suspicious entries  
> **So that** I can identify potential errors or tampering.

**Acceptance Criteria:**
- [ ] Reading list: consumer name, account number, date, meter value, units consumed, status.
- [ ] Filter by consumer, date range, or status.
- [ ] Option to flag a reading as "suspicious" with a reason.

---

**US-ADM-04: Generate Bills**

> **As an** admin  
> **I want to** trigger bill generation for all consumers or a specific consumer for a given month  
> **So that** monthly bills are computed and made available to consumers.

**Acceptance Criteria:**
- [ ] Bill generation form: select month/year, select all consumers (default) or specific consumer.
- [ ] System calculates bills based on daily readings and tariff slabs.
- [ ] Progress indicator during generation.
- [ ] Summary report after generation: total bills generated, total amount, errors (if any).
- [ ] Cannot regenerate bills for a month that already has generated bills (unless forced).

---

**US-ADM-05: Resolve Complaints**

> **As an** admin  
> **I want to** view all consumer complaints, update their status, and add resolution comments  
> **So that** consumer issues are addressed in a timely manner.

**Acceptance Criteria:**
- [ ] Complaint list: ticket number, consumer, subject, category, status, date raised.
- [ ] Filter by status and category.
- [ ] Click to view complaint details and add resolution.
- [ ] Status can be updated: OPEN → IN_PROGRESS → RESOLVED → CLOSED.
- [ ] Consumer is notified on status change.

---

**US-ADM-06: Send Notifications**

> **As an** admin  
> **I want to** send broadcast notifications to all consumers or targeted notifications to a specific consumer  
> **So that** I can communicate important information.

**Acceptance Criteria:**
- [ ] Notification form: title, message, type (INFO, WARNING, ALERT), target (all consumers / specific consumer).
- [ ] Broadcast sends to all active consumers.
- [ ] Targeted allows searching and selecting a consumer.
- [ ] Sent notification is recorded in the notification history.

---

### Reports

---

**US-REP-01: View Revenue Report**

> **As an** admin  
> **I want to** view a monthly revenue report showing total collections  
> **So that** I can track the utility's financial performance.

**Acceptance Criteria:**
- [ ] Report shows: month, total bills generated, total amount billed, total payments collected, collection rate (%).
- [ ] Filter by month and year.
- [ ] Data is displayed in a table and optionally a bar chart.

---

**US-REP-02: View Consumption Report**

> **As an** admin  
> **I want to** view a consumption report showing total units sold and average consumption per consumer  
> **So that** I can analyze energy demand patterns.

**Acceptance Criteria:**
- [ ] Report shows: month, total units consumed, average consumption per consumer, min/max consumption, number of consumers billed.
- [ ] Filter by month and year.
- [ ] Data is displayed in a table and chart.

---

## 5. Acceptance Criteria (Per Module)

### Module: Authentication

| ID | Criterion | Measure |
|---|---|---|
| AC-AUTH-01 | User can register with valid data | Registration succeeds and redirects to login |
| AC-AUTH-02 | User cannot register with duplicate email | Error message displayed |
| AC-AUTH-03 | User can log in with valid credentials | JWT token returned, dashboard loaded |
| AC-AUTH-04 | User cannot log in with invalid credentials | Error message displayed |
| AC-AUTH-05 | Deactivated user cannot log in | Error message displayed |
| AC-AUTH-06 | JWT token expires and access is denied | 401 response on expired token |

### Module: User Profile

| ID | Criterion | Measure |
|---|---|---|
| AC-PRO-01 | Profile displays all required fields | All fields visible and accurate |
| AC-PRO-02 | Profile updates are persisted | Refreshed page shows updated values |
| AC-PRO-03 | Password change with correct current password | Password updated successfully |
| AC-PRO-04 | Password change with incorrect current password | Error message displayed |

### Module: Meter Reading

| ID | Criterion | Measure |
|---|---|---|
| AC-READ-01 | Reading submission with valid data | Reading saved, units calculated |
| AC-READ-02 | Reading value less than previous | Validation error shown |
| AC-READ-03 | Duplicate date submission | Error message shown |
| AC-READ-04 | Reading history shows all past entries | Paginated list with correct data |

### Module: Bill Generation

| ID | Criterion | Measure |
|---|---|---|
| AC-BILL-01 | Daily bill computed correctly on reading submission | Amount matches slab calculation |
| AC-BILL-02 | Monthly bill aggregates daily readings correctly | Total matches sum of daily units × slabs |
| AC-BILL-03 | Bill detail shows slab-wise breakdown | All slab rows visible with correct math |
| AC-BILL-04 | Paid/Unpaid status reflects payment | Status updates after payment |

### Module: Payment

| ID | Criterion | Measure |
|---|---|---|
| AC-PAY-01 | Payment recorded against an unpaid bill | Bill status changes to PAID |
| AC-PAY-02 | Payment history shows all transactions | Complete list with all details |
| AC-PAY-03 | Duplicate payment prevented | Error on paying already-paid bill |

### Module: Dashboard & Analytics

| ID | Criterion | Measure |
|---|---|---|
| AC-DASH-01 | Consumer dashboard loads with all widgets | All KPIs, charts, and quick actions visible |
| AC-DASH-02 | Charts render with correct data | Hover values match database |
| AC-DASH-03 | Month-over-month comparison is accurate | Values match query results |
| AC-DASH-04 | Admin dashboard shows correct system KPIs | Numbers match database counts |

### Module: Complaint

| ID | Criterion | Measure |
|---|---|---|
| AC-COMP-01 | Complaint raised successfully | Ticket number generated, status = OPEN |
| AC-COMP-02 | Complaint history shows all user complaints | Complete list with current statuses |
| AC-COMP-03 | Admin can update complaint status | Status changes reflected in consumer view |
| AC-COMP-04 | Admin can add resolution | Resolution text visible in complaint detail |

### Module: Notification

| ID | Criterion | Measure |
|---|---|---|
| AC-NOT-01 | Auto-notification on bill generation | Notification appears in consumer list |
| AC-NOT-02 | Auto-notification on payment | Notification appears in consumer list |
| AC-NOT-03 | Auto-notification on complaint status change | Notification appears in consumer list |
| AC-NOT-04 | Admin broadcast reaches all active consumers | All active consumers see the notification |
| AC-NOT-05 | Mark as read works correctly | Unread count decreases, badge updates |

### Module: Admin Management

| ID | Criterion | Measure |
|---|---|---|
| AC-ADM-01 | User list shows all consumers with search | Search results match query |
| AC-ADM-02 | Activate/deactivate works immediately | Status change reflected on login attempt |
| AC-ADM-03 | Tariff slab CRUD operations work correctly | Slabs saved, validated for overlap |
| AC-ADM-04 | Bill generation completes with summary | Summary shows correct counts and amounts |

### Module: Reports

| ID | Criterion | Measure |
|---|---|---|
| AC-REP-01 | Revenue report matches payments data | Numbers match payment records |
| AC-REP-02 | Consumption report matches readings data | Numbers match meter reading records |

---

## 6. Out of Scope (Version 1)

The following features are explicitly **out of scope** for Version 1 of VOLTARAS:

| # | Feature | Reason |
|---|---|---|
| OS-01 | **Real-time / IoT Smart Meter Integration** | Automated meter reading hardware integration is a future phase. Version 1 relies on consumer-submitted manual readings only. |
| OS-02 | **Payment Gateway Integration** | Actual payment processing (credit card, UPI, net banking) is out of scope for V1. Payments are recorded manually within the system as a transaction log. |
| OS-03 | **Mobile Applications (Android/iOS)** | Version 1 is limited to a responsive web application. Native mobile apps are a future enhancement. |
| OS-04 | **Multi-language / i18n Support** | The platform supports English only in V1. |
| OS-05 | **Advanced Predictive Analytics / ML** | Consumption forecasting using machine learning is not included. Analytics are limited to historical data visualization. |
| OS-06 | **Multi-Tenancy / Multiple Utility Providers** | The system is designed for a single electricity distribution company. |
| OS-07 | **Late Payment Fees / Penalties / Interest Calculation** | V1 does not include late fee or penalty logic on overdue bills. |
| OS-08 | **SMS / Email Notifications** | Notifications are limited to in-app (UI) notifications in V1. SMS and email integration are future scope. |
| OS-09 | **Invoice Download (PDF)** | PDF generation for bills/invoices is deferred to a later phase. |
| OS-10 | **Bulk User Upload via CSV** | Admin user management is manual; bulk import is not supported in V1. |
| OS-11 | **Two-Factor Authentication (2FA)** | Basic JWT authentication only; 2FA is not included. |
| OS-12 | **Audit Log Dashboard** | While audit logging exists in the database, a dedicated audit log UI for admin is not included in V1. |
| OS-13 | **Rate Limiting / API Throttling** | Not implemented in V1. |
| OS-14 | **Graphical Report Export** | Reports are viewable on-screen only; PDF/CSV export is deferred. |

---

> **End of Phase 3 — Deliverable**
> *`docs/02_REQUIREMENTS.md` has been generated.*
> *Pending approval to proceed to Phase 4.*
