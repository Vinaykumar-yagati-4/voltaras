# VOLTARAS — UI Design

> **Project:** VOLTARAS — Smart Electricity Bill Tracking & Energy Analytics Platform  
> **Framework:** TrainingMug AI Development Framework (ADF) v1.0  
> **Phase:** 7 — UI Design  
> **Document:** `docs/06_UI_DESIGN.md`  

---

## 1. UI Design Goals

| # | Goal | Description |
|---|---|---|
| G1 | **Clarity & Transparency** | Present billing data, consumption metrics, and cost breakdowns in a clear, easy-to-understand visual format so users always know where they stand. |
| G2 | **Role-Specific Experience** | Deliver two distinct UI personalities — a consumer dashboard focused on personal usage and payments, and an admin dashboard focused on system-wide management and analytics. |
| G3 | **Task Efficiency** | Minimize the number of clicks required for core actions: submitting a reading (≤ 3 clicks), paying a bill (≤ 4 clicks), raising a complaint (≤ 4 clicks). |
| G4 | **Data-Driven Insights** | Use charts, comparisons, and trend indicators to help consumers understand their consumption patterns and help admins monitor system health at a glance. |
| G5 | **Responsive & Accessible** | Provide a consistent experience across desktop, tablet, and mobile viewports, meeting WCAG 2.1 AA accessibility standards. |
| G6 | **Professional Energy-Theme** | Create a visual identity that conveys trust, precision, and modernity — appropriate for a utility management platform. |

---

## 2. Target Users and Roles

| Role | Description | Key Tasks |
|---|---|---|
| **Consumer** | An individual electricity consumer who has registered on the platform. | Submit meter readings, view bills, make payments, raise complaints, view energy analytics, manage profile, receive notifications. |
| **Admin** | A system administrator employed by the utility provider. | Manage users, configure tariff slabs, monitor readings, generate bills, track payments, resolve complaints, send notifications, view reports and system KPIs. |

### Role Statistics (Assumptions for UI Design)

| Metric | Assumed Value |
|---|---|
| Total consumers | 1,000–5,000 initially |
| Concurrent consumers | 50–100 during peak hours |
| Concurrent admins | 5–10 |
| Daily readings per consumer | 1 |
| Monthly bills per consumer | 1 |
| Average complaint lifecycle | 2–5 days |

---

## 3. Design Principles

| Principle | Application |
|---|---|
| **Progressive Disclosure** | Show summary information first; allow drill-down into details. Dashboard widgets show KPIs; clicking navigates to full-page views. |
| **Consistency** | Reusable component library (cards, tables, forms, modals, alerts) with uniform spacing, typography, and color across all screens. |
| **Feedback Everywhere** | Every user action triggers a visual response: loading spinners during API calls, success toasts for completed actions, error messages for failures. |
| **Mobile-First Responsiveness** | Layouts are designed for mobile first, then enhanced for tablet and desktop — not the reverse. |
| **Error Prevention** | Forms validate input in real time (before submission). Destructive actions (deactivate user, delete slab) require confirmation modals. |
| **Accessibility** | Colour contrast ratios meet WCAG AA (4.5:1 for normal text, 3:1 for large text). All interactive elements are keyboard-accessible. Screen reader support via ARIA labels. |
| **Data Visualisation Best Practices** | Charts use appropriate types (line for trends, bar for comparisons, pie/donut for breakdowns). Axes are labelled, values are shown on hover. |

---

## 4. Visual Identity

### 4.1 Application Name & Tagline

- **Name:** VOLTARAS
- **Tagline:** *Smart Energy, Clear Bills.*
- **Logo Concept:** A stylised lightning bolt integrated with a bar chart, rendered in the primary brand colour. The logotype "VOLTARAS" is set in a clean sans-serif typeface with the "V" and "S" slightly emphasised.

### 4.2 Colour Palette

| Token | Hex | Usage | WCAG AA (on white) |
|---|---|---|---|
| **Primary** | `#1A56DB` | Buttons, links, active nav items, primary CTAs, header backgrounds | ✅ Pass |
| **Primary Dark** | `#1340A0` | Hover states for primary buttons, active tab indicators | ✅ Pass |
| **Primary Light** | `#E1EFFE` | Selected table rows, alert backgrounds for info messages, badge backgrounds | ✅ Pass |
| **Secondary** | `#7E3AF2` | Secondary buttons, featured highlights, pro/beta badges | ✅ Pass |
| **Secondary Light** | `#EDE9FE` | Secondary button hover backgrounds | ✅ Pass |
| **Success** | `#059669` | Paid status badges, success alerts, completed step indicators, positive trends (↑) | ✅ Pass |
| **Success Light** | `#D1FAE5` | Success alert backgrounds, paid badge background | ✅ Pass |
| **Warning** | `#D97706` | Pending/unpaid status badges, warning alerts, medium-priority indicators | ✅ Pass |
| **Warning Light** | `#FEF3C7` | Warning alert backgrounds, unpaid badge background | ✅ Pass |
| **Danger** | `#DC2626` | Error alerts, delete/destructive buttons, failed status badges, negative trends (↓), validation errors | ✅ Pass |
| **Danger Light** | `#FEE2E2` | Error alert backgrounds, deleted/suspended badge backgrounds | ✅ Pass |
| **Neutral 50** | `#F9FAFB` | Page background, card backgrounds | — |
| **Neutral 100** | `#F3F4F6` | Table row striping, sidebar background, input field background | — |
| **Neutral 200** | `#E5E7EB` | Borders, dividers, disabled input backgrounds | — |
| **Neutral 500** | `#6B7280` | Secondary text, placeholder text, icons (non-interactive) | ✅ Pass |
| **Neutral 700** | `#374151` | Body text, headings on light backgrounds | ✅ Pass |
| **Neutral 900** | `#111827` | Primary headings, high-emphasis text | ✅ Pass |

### 4.3 Typography

| Property | Specification |
|---|---|
| **Font Family (UI)** | `Inter`, system-ui, `-apple-system`, sans-serif |
| **Font Family (Monospace)** | `JetBrains Mono`, `Fira Code`, monospace (for meter values and transaction IDs) |
| **Base Font Size** | `16px` (1rem) |
| **Scale** | Minor third (1.200) |

| Element | Size | Weight | Line Height |
|---|---|---|---|
| **H1 — Page Title** | `2.25rem` (36px) | 700 | 1.25 |
| **H2 — Section Title** | `1.5rem` (24px) | 600 | 1.3 |
| **H3 — Card Title** | `1.125rem` (18px) | 600 | 1.4 |
| **H4 — Subsection** | `1rem` (16px) | 600 | 1.4 |
| **Body Large** | `1rem` (16px) | 400 | 1.5 |
| **Body Small** | `0.875rem` (14px) | 400 | 1.5 |
| **Caption / Helper** | `0.75rem` (12px) | 400 | 1.5 |
| **Label / Button** | `0.875rem` (14px) | 500 | 1.25 |
| **Monospace (Meter Value)** | `0.875rem` (14px) | 500 | 1.5 |

### 4.4 Spacing

| Token | Value | Usage |
|---|---|---|
| `space-xs` | `4px` (0.25rem) | Icon-to-text gaps, small label margins |
| `space-sm` | `8px` (0.5rem) | Element padding inside cards, badge padding |
| `space-md` | `16px` (1rem) | Card padding, form field spacing, table cell padding |
| `space-lg` | `24px` (1.5rem) | Section-to-section gaps, modal padding |
| `space-xl` | `32px` (2rem) | Page section margins, sidebar padding |
| `space-2xl` | `48px` (3rem) | Major page section separation |

### 4.5 Border Radius

| Token | Value | Usage |
|---|---|---|
| `radius-sm` | `4px` | Input fields, small badges, table cells |
| `radius-md` | `8px` | Cards, modals, dropdown menus, alerts |
| `radius-lg` | `12px` | Dashboard stat cards, large containers |
| `radius-xl` | `16px` | Modal dialogs on mobile |
| `radius-full` | `9999px` | Avatars, notification dots, pill badges |

### 4.6 Shadows

| Token | Value | Usage |
|---|---|---|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle card elevation in lists |
| `shadow-md` | `0 4px 6px -1px rgba(0,0,0,0.1)` | Default card elevation, dropdown menus |
| `shadow-lg` | `0 10px 15px -3px rgba(0,0,0,0.1)` | Modal dialogs, side panels |
| `shadow-xl` | `0 20px 25px -5px rgba(0,0,0,0.15)` | Toast notifications, floating action buttons |

### 4.7 Icon Usage

- **Icon Library:** [Lucide Icons](https://lucide.dev) (open-source, consistent stroke-based set)
- **Icon Size:** Default `20px` (1.25rem) for inline icons; `24px` (1.5rem) for standalone action icons; `16px` (1rem) for small badge/icons inside tables.
- **Stroke Width:** 1.5px (Lucide default)
- **Colour:** Icons inherit the text colour of their parent element unless explicitly coloured for status indication (success=green, warning=amber, danger=red).

---

## 5. Responsive Layout Strategy

### 5.1 Breakpoints

| Breakpoint | Min Width | Devices | Layout Behaviour |
|---|---|---|---|
| `xs` | 0–639px | Small phones | Single column, stacked navigation |
| `sm` | 640px+ | Large phones, small tablets | Single column, bottom tab bar for dashboard |
| `md` | 768px+ | Tablets | Two-column layouts possible, sidebar collapses to icons |
| `lg` | 1024px+ | Desktop, laptops | Full sidebar + main content, multi-column grids |
| `xl` | 1280px+ | Large desktops | Expanded padding, optional third column for side panels |

### 5.2 Responsive Behaviour Summary

| Component | Mobile (<768px) | Tablet (768–1023px) | Desktop (≥1024px) |
|---|---|---|---|
| **Sidebar** | Hidden; hamburger menu opens overlay | Collapsed (icons only) | Expanded (icons + labels) |
| **Top Navigation** | Hidden; mobile header with hamburger | Simplified (search + user menu) | Full (search, notifications, user menu) |
| **Data Tables** | Horizontal scroll; first 2–3 columns fixed | Horizontal scroll; key columns shown | All columns visible |
| **Dashboard Cards** | 1-column grid | 2-column grid | 4-column grid (stats row) |
| **Forms** | Single column, full width | Single column, max 480px width | Multi-column for complex forms |
| **Charts** | Full width, simplified (no legends aside) | Full width with legends | Responsive width with full legends |
| **Modals** | Full-screen sheet from bottom | Centered, 80% width | Centered, max 600px |
| **Page Header** | Title only | Title + 1–2 actions | Title + breadcrumbs + full actions |

---

## 6. Common Layout Components

### 6.1 Public Layout

```
┌──────────────────────────────────────────┐
│              Top Navigation              │  ← Logo + Login/Register links
├──────────────────────────────────────────┤
│                                          │
│              Main Content                │  ← Landing, login, register, forgot/reset password
│              (Centered, max 480px)       │
│                                          │
├──────────────────────────────────────────┤
│                 Footer                   │  ← Links, copyright
└──────────────────────────────────────────┘
```

- No sidebar.
- Header is minimal (brand logo, "Login", "Register" links).
- Forms are centred with a maximum width of 480px.

### 6.2 Customer Dashboard Layout

```
┌──────────┬─────────────────────────────────────────┐
│          │  Top Navigation (Search, Notif bell,    │
│          │  User dropdown)                         │
│ Sidebar  ├─────────────────────────────────────────┤
│ (Collaps │                                         │
│ ible)    │          Main Content Area               │
│ - Dash   │   ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     │
│ - Reading│   │ KPI │ │ KPI │ │ KPI │ │ KPI │     │
│ - Bills  │   └─────┘ └─────┘ └─────┘ └─────┘     │
│ - Payment│                                         │
│ - Complnt│   ┌──────────────────────────────┐     │
│ - Notif  │   │     Charts Section            │     │
│ - Analyt │   └──────────────────────────────┘     │
│ - Profile│                                         │
│          │   ┌──────────────────────────────┐     │
│          │   │     Recent Activity Table     │     │
│          │   └──────────────────────────────┘     │
│          │                                         │
└──────────┴─────────────────────────────────────────┘
```

### 6.3 Admin Dashboard Layout

```
┌──────────┬─────────────────────────────────────────┐
│          │  Top Navigation (Search, Notif, Admin   │
│          │  user dropdown)                         │
│ Sidebar  ├─────────────────────────────────────────┤
│ (Admin   │                                         │
│  menu)   │          Main Content Area               │
│          │   ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     │
│ - Dash   │   │ KPI │ │ KPI │ │ KPI │ │ KPI │     │
│ - Users  │   └─────┘ └─────┘ └─────┘ └─────┘     │
│ - Tariffs│                                         │
│ - Meters │   ┌──────────────────────────────┐     │
│ - Bills  │   │     Management Table/List     │     │
│ - Paymts │   │     (with filters, search,    │     │
│ - Complnt│   │      pagination)              │     │
│ - Notif  │   └──────────────────────────────┘     │
│ - Reports│                                         │
│ - Audit  │   ┌──────────────────────────────┐     │
│          │   │     Quick Actions / Charts    │     │
│          │   └──────────────────────────────┘     │
│          │                                         │
└──────────┴─────────────────────────────────────────┘
```

### 6.4 Top Navigation

| Element | Description |
|---|---|
| **Logo** | Left-aligned; links to dashboard home for authenticated users, or landing page for guests. |
| **Search Bar** | (Admin only) Global search for consumers, bills, complaints. Desktop: inline input. Mobile: expandable search icon. |
| **Notification Bell** | Shows unread count badge. Click opens a dropdown with the 5 most recent notifications and a "View All" link. |
| **User Dropdown** | Avatar (first letter of name) + name. Dropdown: Profile, Change Password, Logout. |
| **Mobile Hamburger** | Visible only on screens < 1024px; toggles sidebar overlay. |

### 6.5 Sidebar

| Section | Consumer Links | Admin Links |
|---|---|---|
| **Dashboard** | 📊 Dashboard | 📊 Dashboard |
| **Readings** | 🔢 Meter Reading | 📡 Meter Readings |
| **Bills** | 📄 My Bills | 💰 Bill Management |
| **Payments** | 💳 Payments | 💳 Payments |
| **Complaints** | 📝 Complaints | 📝 Complaints |
| **Notifications** | 🔔 Notifications | 🔔 Notifications |
| **Analytics** | 📈 Energy Analytics | — |
| **Profile** | 👤 Profile | — |
| **Management** | — | 👥 Users, 📋 Tariffs, 📋 Audit Logs |
| **Reports** | — | 📊 Reports |

- Active item is highlighted with primary colour and a left border accent.
- Icons are placed to the left of labels.
- On mobile, sidebar slides in as an overlay from the left with a backdrop.

### 6.6 Footer

- Simple footer with copyright: "© 2026 VOLTARAS. All rights reserved."
- Links: Privacy Policy, Terms of Service, Contact Support.
- Only visible on public pages and profile pages — hidden inside dashboard layouts.

### 6.7 Page Header

```
┌─────────────────────────────────────────────────┐
│ Breadcrumb > Current Page                        │
│ Page Title                    [Action Button(s)] │
│ Description text (optional)                     │
└─────────────────────────────────────────────────┘
```

- Breadcrumbs show navigation path (e.g., Home > Bills > Bill Details).
- Page title in H1.
- Action buttons aligned to the right (e.g., "Submit Reading", "Download PDF").
- Optional description or subtitle below the title.

### 6.8 Cards

| Card Type | Description | Shadow | Border Radius |
|---|---|---|---|
| **Stat Card** | KPI display with icon, value, label, optional trend indicator | `shadow-sm` | `radius-lg` |
| **Content Card** | Contains content like charts, reading details, bill breakdown | `shadow-sm` | `radius-md` |
| **Action Card** | Clickable card that navigates to a specific page | `shadow-md` (hover: `shadow-lg`) | `radius-md` |
| **Form Card** | Wrapper around form fields | `shadow-sm` | `radius-md` |

**Stat Card Layout:**
```
┌─────────────────────┐
│ 🔌  [Trend ↑ 5%]   │
│  350 kWh            │
│  Current Month      │
└─────────────────────┘
```

### 6.9 Tables

| Element | Specification |
|---|---|
| **Header** | Bold, uppercase labels (or sentence case), background `neutral-50` |
| **Rows** | Alternating white / `neutral-50` striping |
| **Hover** | Row highlights on hover (`primary-light` at 50% opacity) |
| **Padding** | `space-sm` vertical, `space-md` horizontal |
| **Typography** | Body small (`0.875rem`) |
| **Badges** | Status cells use coloured badges: PAID (green), UNPAID (amber), OVERDUE (red), ACTIVE (green), INACTIVE (grey) |
| **Actions Column** | Last column contains icon buttons for view/edit/delete — never use raw text links |

### 6.10 Forms

| Element | Specification |
|---|---|
| **Labels** | Above input fields, `0.875rem`, weight 500 |
| **Input Fields** | `1rem` text, padding `space-sm` `space-md`, border `neutral-200`, focus ring `primary` |
| **Validation States** | Green border + check icon (valid), red border + error icon (invalid) |
| **Error Messages** | Below input, `0.75rem`, danger colour, shown on blur or after submit attempt |
| **Helper Text** | Below input, `0.75rem`, `neutral-500` |
| **Submit Button** | Primary colour, full width on mobile, auto-width on desktop |
| **Required Indicator** | Red asterisk `*` next to label |

### 6.11 Modals

| Element | Specification |
|---|---|
| **Backdrop** | Black at 50% opacity, with blur effect on desktop |
| **Position** | Vertically and horizontally centred |
| **Width** | Mobile: 92% (full-screen sheet); Tablet: 80%; Desktop: max 520px |
| **Padding** | `space-lg` |
| **Header** | Title (H3) + Close (X) button |
| **Body** | Content or form fields |
| **Footer** | Cancel + Confirm/Submit buttons (right-aligned, Cancel on left) |
| **Animation** | Fade in + slight scale up (200ms) |
| **Dismiss** | Click backdrop, press Escape, click Close button |

### 6.12 Alerts and Toast Messages

| Type | Icon | Background | Border (Left) | Position |
|---|---|---|---|---|
| **Success** | ✅ Check circle | `success-light` | `success` | Toast: top-right; Inline: above form |
| **Warning** | ⚠️ Triangle | `warning-light` | `warning` | Toast: top-right; Inline: above form |
| **Error** | ❌ X circle | `danger-light` | `danger` | Toast: top-right; Inline: above form |
| **Info** | ℹ️ Info circle | `primary-light` | `primary` | Toast: top-right; Inline: above form |

**Toast Specifications:**
- Auto-dismiss after 5 seconds (success, info) or stay until manually dismissed (warning, error).
- Stack multiple toasts with 8px gap.
- Max width 400px, `radius-md`, `shadow-lg`.

### 6.13 Loaders and Empty States

| State | Component | Details |
|---|---|---|
| **Page Load** | Full-page spinner | Centred spinner with "Loading..." text, shown on initial page load and route transitions |
| **Section Load** | Skeleton cards | Animated placeholder cards (pulse animation) matching the layout of the content being loaded |
| **Table Load** | Skeleton rows | 3–5 rows of animated placeholder text in table format |
| **Button Load** | Spinner inside button | Button text replaced by a small spinner; button disabled during loading |
| **Empty State** | Illustration + message | Centred illustration (104px), bold message, subtitle, optional CTA button |

**Empty State Layout:**
```
┌────────────────────────────────────┐
│                                    │
│          [Illustration]            │
│                                    │
│     No readings submitted yet       │
│  Start tracking your electricity   │
│        consumption today.          │
│                                    │
│       [Submit Your First Reading]  │
│                                    │
└────────────────────────────────────┘
```

---

## 7. Public Screens

### 7.1 Landing Page

**Purpose:** Introduce VOLTARAS to visitors and guide them to login or register.

**Layout:** Centred single-column with hero section, feature highlights, and footer.

**Elements:**
- Hero section with tagline "Smart Energy, Clear Bills" and a brief value proposition.
- Feature grid (3×) showcasing: Daily Tracking, Smart Analytics, Easy Payments.
- Illustration or abstract design representing energy data.
- CTA buttons: "Get Started" (→ Register) and "Sign In" (→ Login).
- Footer with links.

**States:**
- **Default:** Static landing page content.
- **Logged-in redirect:** If user is already authenticated, redirect to the appropriate dashboard automatically.

### 7.2 Login

**Purpose:** Authenticate existing users (consumers and admins).

**Form Fields:**

| Field | Type | Validation | Required |
|---|---|---|---|
| Email | Email input | Valid email format | ✅ |
| Password | Password input | Min 8 characters | ✅ |
| Remember Me | Checkbox | — | ❌ |

**Layout:**
```
┌────────────────────────────┐
│         [VOLTARAS Logo]    │
│                            │
│   Email _________________  │
│                            │
│   Password ______________  │
│                            │
│   [x] Remember Me         │
│                            │
│   Forgot Password?         │
│                            │
│   [     Sign In        ]  │
│                            │
│   Don't have an account?  │
│        Register here       │
└────────────────────────────┘
```

**Validation Messages:**
- "Please enter a valid email address."
- "Password must be at least 8 characters."
- "Invalid email or password. Please try again."
- "Your account has been deactivated. Please contact support."

**States:**
- **Loading:** Submit button shows spinner, fields are disabled.
- **Success:** Redirect to Consumer Dashboard or Admin Dashboard based on role.
- **Error:** Inline error alert above the form; fields retain entered values.

### 7.3 Registration

**Purpose:** Allow new consumers to create an account.

**Form Fields:**

| Field | Type | Validation | Required |
|---|---|---|---|
| Full Name | Text | 2–100 characters | ✅ |
| Email | Email | Valid format, unique | ✅ |
| Phone | Tel | 10-digit pattern | ✅ |
| Address Line 1 | Text | Max 255 chars | ✅ |
| Address Line 2 | Text | Max 255 chars | ❌ |
| City | Text | Max 100 chars | ✅ |
| State | Text | Max 100 chars | ✅ |
| Pincode | Text | 6-digit pattern | ✅ |
| Password | Password | Min 8 chars, 1 upper, 1 lower, 1 digit | ✅ |
| Confirm Password | Password | Must match password | ✅ |

**Validation Messages (per field):**
- Full Name: "Name must be between 2 and 100 characters."
- Email: "Please enter a valid email address." / "This email is already registered."
- Phone: "Phone number must be 10 digits."
- Password: "Password must be at least 8 characters with 1 uppercase, 1 lowercase, and 1 digit."
- Confirm Password: "Passwords do not match."

**States:**
- **Loading:** Submit button spinner; all fields disabled.
- **Success:** Toast "Registration successful! Please log in." → Redirect to Login page.
- **Error:** Scroll to first error field; show inline error messages.

### 7.4 Forgot Password

**Purpose:** Allow users to request a password reset link (email-based; simulated in V1).

**Form Fields:**

| Field | Type | Validation | Required |
|---|---|---|---|
| Email | Email | Must be a registered email | ✅ |

**Layout:**
- Simple centred card with brief instruction text.
- Email input + "Send Reset Link" button.
- "Back to Login" link.

**States:**
- **Loading:** Button spinner.
- **Success:** "If an account with that email exists, a reset link has been sent."
- **Error:** Generic message (to avoid email enumeration): "If an account with that email exists, a reset link has been sent."

### 7.5 Reset Password

**Purpose:** Allow users to set a new password using a token from the reset email.

**Form Fields:**

| Field | Type | Validation | Required |
|---|---|---|---|
| New Password | Password | Same rules as registration | ✅ |
| Confirm Password | Password | Must match | ✅ |

**Layout:**
- Centred card with token validation (token passed in URL query param).
- Two password fields + "Reset Password" button.
- "Back to Login" link.

**States:**
- **Loading:** Button spinner.
- **Success:** "Password reset successful!" → Redirect to Login after 3 seconds.
- **Error:** "Invalid or expired reset link. Please try again."

---

## 8. Customer Screens

### 8.1 Customer Dashboard

**URL:** `/dashboard`  
**API:** `GET /api/dashboard/consumer`

**Widgets (4-column stat row):**

| Card | Data Source | Icon |
|---|---|---|
| Current Month Consumption | `currentMonth.consumption` | ⚡ |
| Current Month Bill | `currentMonth.billAmount` / `currentMonth.billStatus` | 💰 |
| Pending Bills Count | `pendingBillsCount` | 📄 |
| Unread Notifications | `unreadNotifications` | 🔔 |

**Charts Section (2-column on desktop, 1 on mobile):**

| Chart | Type | Data |
|---|---|---|
| Daily Consumption Trend | Line chart | `GET /api/dashboard/analytics?period=DAILY` → `trend[]` |
| Cost Breakdown by Slab | Pie/Doughnut chart | `GET /api/dashboard/analytics` → `costBreakdown[]` |

**Comparison Card:**

```
┌────────────────────────────────────────┐
│ Current vs Previous Month              │
│                                        │
│ Current: 350 kWh  |  Previous: 320 kWh │
│           ↑ 9.38%                      │
│                                        │
│ Current: ₹1,837.50 | Previous: ₹1,680  │
│           ↑ 9.38%                      │
└────────────────────────────────────────┘
```

**Quick Actions:**
- "Submit Reading" → `/readings/new`
- "View Bills" → `/bills`
- "Pay Now" → Navigates to first unpaid bill.

**Recent Notifications:** List of top 5 unread notifications with a "View All" link.

**States:**
- **Loading:** Skeleton cards for each widget.
- **Empty:** First-time user: "Welcome! Start by submitting your first meter reading."
- **Error:** Error banner: "Unable to load dashboard data. Please try again later."

### 8.2 Profile

**URL:** `/profile`  
**API:** `GET /api/users/me`, `PUT /api/users/me`, `POST /api/auth/change-password`

**Sections:**
1. **Profile Details** (view/edit mode toggle)
   - Full Name (editable)
   - Email (read-only)
   - Consumer Number (read-only)
   - Phone (editable)
   - Registration Date (read-only)

2. **Address** (edit via modal)
   - Address Line 1, Line 2, City, State, Pincode

3. **Change Password** (expandable section / modal)
   - Current Password, New Password, Confirm New Password

**Validation:** Same rules as registration.

**States:**
- **Loading:** Skeleton card.
- **Edit Mode:** Fields become editable; "Save" and "Cancel" buttons appear.
- **Success:** Toast "Profile updated successfully."
- **Error:** Inline error on the failed field.

### 8.3 Meter Reading Entry

**URL:** `/readings/new`  
**API:** `POST /api/readings`

**Form Fields:**

| Field | Type | Validation | Required |
|---|---|---|---|
| Meter Value | Number (decimal) | Must be > 0; must be > last submitted reading | ✅ |
| Reading Date | Date picker | Must not be in the future; must not have existing reading for this date | ✅ |

**Helper Information:**
- Previous reading value and date shown for reference.
- Units consumed (current − previous) calculated and displayed after submission.

**Layout:**
```
┌────────────────────────────────────┐
│   Submit Meter Reading             │
│                                    │
│   Previous Reading: 1,200.00       │
│   (Submitted on: 26 Jul 2026)      │
│                                    │
│   Current Meter Value ___________  │
│                                    │
│   Reading Date   [ 📅 27 Jul 2026]│
│                                    │
│   Units Consumed: — (auto on save) │
│                                    │
│   [     Submit Reading         ]   │
└────────────────────────────────────┘
```

**States:**
- **Validation Error (value too low):** "Meter value must be greater than the previous reading (1,200.00)."
- **Validation Error (duplicate date):** "A reading for this date has already been submitted."
- **Success:** Toast "Reading submitted successfully! Units consumed: 50.50 kWh." → Redirect to reading history.
- **Loading:** Button spinner.

### 8.4 Reading History

**URL:** `/readings`  
**API:** `GET /api/readings?page=&size=&fromDate=&toDate=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Date | `readingDate` | ✅ Desc (default) | ✅ Date range |
| Meter Value | `meterValue` | ❌ | ❌ |
| Units Consumed | `unitsConsumed` | ✅ | ❌ |
| Status | `status` | ❌ | ✅ (VERIFIED / SUSPICIOUS) |
| Submitted At | `submittedAt` | ✅ | ❌ |

**Empty State:** "You haven't submitted any readings yet."

### 8.5 Daily Bill Details

**URL:** `/bills/daily/{billId}`  
**API:** `GET /api/bills/{billId}`

**Content:**
- Bill number, date, type (DAILY).
- Units consumed.
- Slab-wise breakdown (table).
- Total amount.
- Payment status badge.

**Actions:**
- "Pay Now" button if UNPAID.
- "Back to Bills" link.

### 8.6 Monthly Bill Summary

**URL:** `/bills/monthly/{billId}`  
**API:** `GET /api/bills/{billId}`

**Content:**
- Bill number (e.g., BILL-2026-07-0001).
- Billing month, due date.
- Total units consumed.
- Slab-wise breakdown table.
- Summary: Energy Charge, Fixed Charges, Tax, Total Amount.
- Payment status badge.

**Actions:**
- "Pay Now" (if UNPAID).
- "Download PDF" (future — placeholder button).
- "Raise a Complaint" prefilled with bill reference.

### 8.7 Bill History

**URL:** `/bills`  
**API:** `GET /api/bills?type=&status=&month=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Bill Number | `billNumber` | ❌ | ❌ |
| Type | `type` (MONTHLY/DAILY) | ❌ | ✅ (type filter) |
| Billing Month | `billingMonth` | ✅ Desc (default) | ✅ (month picker) |
| Total Units | `totalUnits` | ✅ | ❌ |
| Total Amount | `totalAmount` | ✅ | ❌ |
| Status | `status` | ❌ | ✅ (PAID/UNPAID) |
| Actions | View / Pay icons | ❌ | ❌ |

**Tabs:** "All Bills" | "Monthly Bills" | "Daily Bills"

**Empty State:** "No bills have been generated yet."

### 8.8 Bill PDF/Download View

**URL:** `/bills/{billId}/download`  
**API:** Dispatch a PDF download (future implementation).

**UI Behaviour:**
- Button "Download PDF" on the bill detail page.
- Click triggers a loading state, then downloads a PDF.
- PDF template mirrors the web bill detail layout (logo, bill number, slab table, summary, payment info).
- In V1, this may show a "Coming Soon" placeholder modal.

### 8.9 Payment History

**URL:** `/payments`  
**API:** `GET /api/payments?fromDate=&toDate=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Transaction ID | `transactionId` | ❌ | ❌ |
| Bill Reference | `billNumber` | ❌ | ❌ |
| Amount | `amount` | ✅ | ❌ |
| Payment Method | `paymentMethod` | ❌ | ✅ |
| Status | `status` | ❌ | ✅ (COMPLETED / FAILED) |
| Paid At | `paidAt` | ✅ Desc (default) | ✅ Date range |

**Empty State:** "No payment history found."

### 8.10 Notifications

**URL:** `/notifications`  
**API:** `GET /api/notifications?unreadOnly=&page=&size=`

**List Items:**
- Icon based on `type` (INFO, WARNING, SUCCESS).
- Title, message, timestamp.
- Unread indicator (blue dot on the left).
- Click marks as read and navigates (if referenceType/referenceId present).

**Actions:**
- Toggle: "All" / "Unread Only".
- "Mark All as Read" button (top right).

**Empty State:**
- **All:** "No notifications yet."
- **Unread:** "All caught up! You have no unread notifications."

### 8.11 Complaints

**URL:** `/complaints`  
**API:** `GET /api/complaints?page=&size=`

**List View:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Ticket Number | `ticketNumber` | ❌ | ❌ |
| Subject | `subject` | ❌ | ❌ |
| Category | `category` | ❌ | ✅ |
| Status | `status` | ❌ | ✅ |
| Created At | `createdAt` | ✅ Desc (default) | ✅ Date range |

**Actions:**
- Click row to view complaint details (including comments/status history).
- "Raise New Complaint" button → complaint form.

**Complaint Form (Modal or new page):**

| Field | Type | Validation | Required |
|---|---|---|---|
| Category | Dropdown (Billing Issue, Meter Issue, Payment Issue, Other) | Must select one | ✅ |
| Subject | Text | 10–200 characters | ✅ |
| Description | Textarea | Min 20 characters, max 1000 | ✅ |

**States:**
- **Success:** Toast with ticket number: "Complaint raised successfully. Your ticket number is CMP-20260727-0001."
- **Empty:** "You haven't raised any complaints yet."

### 8.12 Energy Analytics and Reports

**URL:** `/analytics`  
**API:** `GET /api/dashboard/analytics?period=DAILY&month=`

**Sections:**

1. **Period Selector:** Tabs or dropdown for DAILY / WEEKLY / MONTHLY view. Month picker for selecting which month to analyse.

2. **Consumption Trend (Line Chart)**
   - X-axis: Dates (or weeks).
   - Y-axis: Units consumed.
   - Interactive hover tooltip showing exact value.
   - Highlight peak days with markers.

3. **Cost Breakdown (Doughnut/Bar Chart)**
   - Segments: Slab 1, Slab 2, Slab 3.
   - Hover shows slab name, units, amount.

4. **Peak Consumption Days (Card List)**
   - Top 3–5 days with highest consumption.
   - Day, date, units consumed, percentage above average.

5. **Summary Statistics**
   - Total consumption (month).
   - Average daily consumption.
   - Comparison vs previous month (% change).
   - Estimated monthly bill (if not yet generated).

**State:**
- **Loading:** Full skeleton with chart placeholders.
- **Empty (no readings):** "Submit readings to see your energy analytics."
- **Error:** "Unable to load analytics data."

---

## 9. Admin Screens

### 9.1 Admin Dashboard

**URL:** `/admin/dashboard`  
**API:** `GET /api/dashboard/admin`

**KPI Row (6 cards):**

| Card | Data Source |
|---|---|
| Total Users | `totalUsers` |
| Active Users | `activeUsers` |
| Total Readings | `totalReadings` |
| Total Bills | `totalBillsGenerated` |
| Total Collections | `totalAmountCollected` (formatted as ₹) |
| Pending Complaints | `pendingComplaints` |

**Charts (2-column grid):**
- Monthly Revenue Trend (bar chart over last 6 months).
- Collection Rate Gauge (circular progress, `collectionRate`).

**Quick Actions:**
- "Generate Monthly Bills" → admin billing page.
- "View Pending Complaints" → filtered complaints list.

**States:**
- **Loading:** Skeleton cards.
- **Error:** Error banner.

### 9.2 User Management

**URL:** `/admin/users`  
**API:** `GET /api/admin/users?search=&status=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Consumer # | `consumerNumber` | ✅ | ❌ |
| Full Name | `fullName` | ✅ | ✅ (text search) |
| Email | `email` | ✅ | ✅ (text search) |
| Phone | `phone` | ❌ | ❌ |
| Status | `isActive` | ❌ | ✅ (ACTIVE/INACTIVE) |
| Last Login | `lastLoginAt` | ✅ | ❌ |
| Created At | `createdAt` | ✅ | ❌ |
| Actions | View / Toggle Status icons | ❌ | ❌ |

**Actions:**
- Click "View" → User Details page.
- "Toggle Status" → Confirmation modal → API call.
- Search bar at top (searches name, email, consumer number).

**Empty State:** "No users match your search/filter criteria."

### 9.3 User Details

**URL:** `/admin/users/{userId}`  
**API:** `GET /api/admin/users/{userId}` (and related data)

**Sections:**
1. **Profile Summary** — Name, email, phone, consumer number, status badge, registration date.
2. **Address** — Full address display (read-only).
3. **Account Actions:**
   - "Activate" / "Deactivate" button (with confirmation modal).
   - "View Readings" → jump to filtered readings.
   - "View Bills" → jump to filtered bills.
4. **Recent Activity Summary** — Last reading, last bill, last payment (mini-cards).

### 9.4 Tariff Slab Management

**URL:** `/admin/tariffs`  
**API:** `GET /api/admin/tariff-slabs`, `POST`, `PUT`, `DELETE`

**Table Columns:**

| Column | Data | Sortable |
|---|---|---|
| Slab Name | `slabName` (e.g., "0–100 Units") | ✅ |
| Min Units | `minUnits` | ✅ |
| Max Units | `maxUnits` | ✅ |
| Rate per Unit | `ratePerUnit` (formatted as ₹) | ✅ |
| Status | `isActive` badge | ❌ |
| Actions | Edit / Delete icons | ❌ |

**Create/Edit Slab (Modal):**

| Field | Type | Validation |
|---|---|---|
| Slab Name | Text | Required, max 100 chars |
| Min Units | Number (decimal) | Required, >= 0 |
| Max Units | Number (decimal) | Required, > minUnits, no overlap with existing slabs |
| Rate per Unit | Number (decimal) | Required, > 0 |
| Is Active | Toggle | Default: true |

**Validation Messages:**
- "Max units must be greater than min units."
- "This slab range overlaps with an existing slab."
- "Rate must be greater than 0."

**Delete Confirmation:** "Are you sure you want to delete the '{slabName}' slab? This action cannot be undone."

### 9.5 Meter Reading Monitoring

**URL:** `/admin/readings`  
**API:** `GET /api/admin/readings?consumerId=&status=&fromDate=&toDate=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Consumer | `consumerNumber` / `fullName` | ❌ | ✅ (search) |
| Reading Date | `readingDate` | ✅ Desc (default) | ✅ (date range) |
| Meter Value | `meterValue` | ✅ | ❌ |
| Units Consumed | `unitsConsumed` | ✅ | ❌ |
| Status | `status` | ❌ | ✅ (VERIFIED/SUSPICIOUS) |
| Submitted At | `submittedAt` | ✅ | ❌ |
| Actions | Flag as Suspicious icon | ❌ | ❌ |

**Flag as Suspicious:** Opens a small modal/drawer with reason textarea and "Flag" button.

### 9.6 Bill Management

**URL:** `/admin/bills`  
**API:** `GET /api/admin/bills?consumerId=&status=&month=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Bill Number | `billNumber` | ❌ | ❌ |
| Consumer | `consumerNumber` / `fullName` | ❌ | ✅ (search) |
| Type | `type` | ❌ | ✅ (MONTHLY/DAILY) |
| Month | `billingMonth` | ✅ | ✅ (month picker) |
| Total Units | `totalUnits` | ✅ | ❌ |
| Total Amount | `totalAmount` | ✅ | ❌ |
| Status | `status` | ❌ | ✅ (PAID/UNPAID) |
| Generated At | `generatedAt` | ✅ | ❌ |

**Actions:**
- "Generate Bills" button (top right).
- Click row → bill detail (read-only view for admin).

**Generate Bills Modal:**

| Field | Type | Validation |
|---|---|---|
| Billing Month | Month picker | Must be a past month |
| Scope | Radio: "All Consumers" / "Specific Consumers" | Required |
| Consumer IDs | Multi-select (shown if "Specific") | At least 1 required |
| Force Regenerate | Toggle | Warning: "This will overwrite existing drafts." |

### 9.7 Payment Monitoring

**URL:** `/admin/payments`  
**API:** `GET /api/admin/payments?consumerId=&status=&fromDate=&toDate=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Transaction ID | `transactionId` | ❌ | ❌ |
| Consumer | `consumerNumber` / `fullName` | ❌ | ✅ (search) |
| Bill Reference | `billNumber` | ❌ | ❌ |
| Amount | `amount` | ✅ | ❌ |
| Method | `paymentMethod` | ❌ | ✅ |
| Status | `status` | ❌ | ✅ (COMPLETED/FAILED) |
| Paid At | `paidAt` | ✅ Desc (default) | ✅ (date range) |

### 9.8 Complaint Management

**URL:** `/admin/complaints`  
**API:** `GET /api/admin/complaints?status=&categoryId=&priority=&assignedTo=&fromDate=&toDate=&page=&size=`

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Ticket # | `ticketNumber` | ❌ | ❌ |
| Consumer | `consumerNumber` / `fullName` | ❌ | ❌ |
| Subject | `subject` | ❌ | ❌ |
| Category | `category` | ❌ | ✅ |
| Status | `status` (OPEN/IN_PROGRESS/RESOLVED/CLOSED) | ❌ | ✅ |
| Priority | `priority` | ❌ | ✅ |
| Created At | `createdAt` | ✅ Desc (default) | ✅ (date range) |
| Actions | View / Update Status icons | ❌ | ❌ |

**Complaint Detail (Side panel or page):**
- Full complaint info: ticket number, subject, description, category, status, timestamps.
- Comments/status history timeline.
- **Admin Actions:**
  - Status dropdown: OPEN → IN_PROGRESS → RESOLVED → CLOSED.
  - "Add Comment" textarea + button.
  - "Assign to Me" button.

**Status Transition Validation:** Prevent invalid transitions (e.g., OPEN → CLOSED).

### 9.9 Notification Management

**URL:** `/admin/notifications`  
**API:** `POST /api/admin/notifications`

**Compose Notification Form:**

| Field | Type | Validation |
|---|---|---|
| Title | Text | Required, max 200 chars |
| Message | Textarea | Required, max 1000 chars |
| Type | Dropdown (INFO, WARNING, SUCCESS, ERROR) | Required |
| Target | Radio: "All Consumers" / "Specific Consumer" | Required |
| Consumer | Search + Select (shown if "Specific Consumer") | Required for specific |

**Preview:** Card showing how the notification will appear to the consumer.

**Send History (Table below form):**

| Column | Data |
|---|---|
| Title | `title` |
| Type | `type` badge |
| Target | "All (25)" or "Specific: John Doe" |
| Sent At | `sentAt` |
| Recipients | `recipientCount` |

### 9.10 Audit Logs

**URL:** `/admin/audit-logs`

**Purpose:** Display system-level audit trail for compliance and debugging.

**Table Columns:**

| Column | Data | Sortable | Filter |
|---|---|---|---|
| Timestamp | `timestamp` | ✅ Desc (default) | ✅ Date range |
| Action | `action` (e.g., USER_DEACTIVATED, BILL_GENERATED, PAYMENT_VERIFIED) | ❌ | ✅ |
| Performed By | Admin name/email | ❌ | ✅ |
| Target | Target entity (e.g., user email, bill number) | ❌ | ❌ |
| Details | Summary of what changed | ❌ | ❌ |

**Empty State:** "No audit log entries found."

### 9.11 Admin Analytics and Reports

**URL:** `/admin/reports`  
**API:** `GET /api/admin/reports/revenue?fromMonth=&toMonth=`, `GET /api/admin/reports/consumption?fromMonth=&toMonth=`

**Sections:**

1. **Date Range Selector:** Month range picker for report period.

2. **Revenue Report (Bar Chart + Table)**
   - Bar chart: Monthly billed amount vs collected amount (grouped bars).
   - Table: Month, Bills Generated, Amount Billed, Amount Collected, Collection Rate (%).

3. **Consumption Report (Bar Chart + Table)**
   - Bar chart: Total units consumed per month.
   - Table: Month, Total Units, Number of Consumers Billed, Average per Consumer.

4. **Export Buttons:**
   - "Download CSV" (future — shows "Coming Soon" if not implemented).
   - "Download PDF" (future).

---

## 10. Screen-by-Screen Wireframe Descriptions

### Screen 1 — Landing Page
A hero section with the VOLTARAS logo, tagline "Smart Energy, Clear Bills", and a brief description. Below: 3 feature cards (Daily Tracking, Smart Analytics, Easy Payments). Bottom: CTA buttons for "Get Started" and "Sign In". Footer with copyright.

### Screen 2 — Login
Centred card on a light background. Logo at top. Email and password inputs with labels. "Remember Me" checkbox. "Forgot Password?" link. Blue "Sign In" button. Register link at bottom.

### Screen 3 — Registration
Longer centred card. Two-column field layout on desktop (name + email side by side, phone + pincode side by side), single column on mobile. Address section with 5 fields. Password section with password + confirm. Submit button at bottom.

### Screen 4 — Forgot Password
Simple centred card. Brief instruction text. Email input. "Send Reset Link" button. "Back to Login" link.

### Screen 5 — Reset Password
Centred card. Instruction text. New password + confirm password inputs. "Reset Password" button.

### Screen 6 — Consumer Dashboard
Sidebar on left (expanded on desktop, collapsible). Top bar with notification bell and user dropdown. 4 stat cards in a row. Below: 2-column chart section (line chart left, doughnut chart right). Below: Comparison card across full width. Bottom: Recent notifications list.

### Screen 7 — Meter Reading Entry
Page header with title. Card containing: previous reading reference, meter value input, date picker, auto-calculated units display (after submission), submit button.

### Screen 8 — Reading History
Page header with title + "Submit Reading" button. Filter bar: date range picker. Table with Date, Meter Value, Units, Status, Submitted At columns. Pagination at bottom.

### Screen 9 — Bill History
Page header with title. Tabs: All / Monthly / Daily. Filter bar: month picker + status dropdown. Table with Bill Number, Type, Month, Units, Amount, Status, Actions. Pagination.

### Screen 10 — Bill Detail
Page header with breadcrumbs + bill number. Card with bill info (type, month, status badge, dates). Slab breakdown table. Summary section (energy charge, fixed charges, tax, total). Action buttons: "Pay Now" (if unpaid) and "Download PDF".

### Screen 11 — Payment History
Page header. Filter bar: date range + payment method dropdown. Table with Transaction ID, Bill Reference, Amount, Method, Status, Paid At. Pagination.

### Screen 12 — Make Payment
Modal or page: Bill summary card (read-only), Amount (read-only, pre-filled), Payment Method dropdown, Transaction Reference (optional text input), "Pay Now" button. Confirmation dialog after payment.

### Screen 13 — Complaints List
Page header + "Raise New Complaint" button. Filter bar: status dropdown + category dropdown. Table with Ticket #, Subject, Category, Status, Created At. Click to expand/detail.

### Screen 14 — Raise Complaint (Modal)
Modal with: Category dropdown, Subject input, Description textarea. Submit and Cancel buttons.

### Screen 15 — Notifications
Page header + "Mark All as Read" button. Toggle: All / Unread. List of notification cards with icon, title, message, timestamp, unread dot.

### Screen 16 — Energy Analytics
Page header with period selector (DAILY/WEEKLY/MONTHLY tabs) and month picker. Consumption trend line chart (full width). Cost breakdown doughnut chart (half width). Peak days list (half width). Summary stat cards (total, average, comparison).

### Screen 17 — Profile
Page header. Card with profile fields in view mode. "Edit" button toggles to edit mode. Expandable "Change Password" section at bottom.

### Screen 18 — Admin Dashboard
Sidebar with admin-specific menu. Top bar with search, notifications, admin user dropdown. 6 stat cards (2 rows of 3 on desktop). Revenue bar chart. Collection rate gauge. Quick action buttons.

### Screen 19 — User Management
Page header + search bar. Table with Consumer #, Name, Email, Phone, Status, Last Login, Created, Actions. Status toggle in actions column. Confirmation modal for deactivation.

### Screen 20 — User Details
Page header with user name + status badge. Profile summary card. Address card. Account action buttons. Quick links to user's readings, bills, complaints.

### Screen 21 — Tariff Management
Page header + "Add Slab" button. Table with Slab Name, Min, Max, Rate, Status, Actions. Add/Edit modal with slab form fields. Delete confirmation modal.

### Screen 22 — Admin: Readings Monitoring
Page header with filters. Table with Consumer info, Reading Date, Meter Value, Units, Status, Actions. "Flag as Suspicious" modal.

### Screen 23 — Admin: Bill Management
Page header + "Generate Bills" button. Table with Bill Number, Consumer, Type, Month, Units, Amount, Status, Generated At. Generate Bills modal with month, scope, force options.

### Screen 24 — Admin: Payment Monitoring
Page header + filters. Table with Transaction ID, Consumer, Bill Reference, Amount, Method, Status, Paid At.

### Screen 25 — Admin: Complaint Management
Page header + filters. Table with Ticket #, Consumer, Subject, Category, Status, Priority, Created, Actions. Click row → side panel with full details, status timeline, admin comments, status updater.

### Screen 26 — Admin: Notification Management
Page header. Compose form: Title, Message, Type dropdown, Target radio, Consumer selector (shown conditionally). Preview card. Send button. Below: sent notification history table.

### Screen 27 — Admin: Audit Logs
Page header + date range filter + action type filter. Table with Timestamp, Action, Performed By, Target, Details.

### Screen 28 — Admin: Reports
Page header + month range selector. Two tabbed sections: Revenue Report and Consumption Report. Each with chart + table. Export buttons.

---

## 11. Navigation Structure

```
                              ┌─────────────────────────────┐
                              │       Landing Page (/)       │
                              │  (Public — not authenticated)│
                              └──────┬──────────┬───────────┘
                                     │          │
                          ┌──────────┘          └──────────┐
                          ▼                                ▼
                 ┌──────────────────┐          ┌──────────────────────┐
                 │   Login (/login) │          │ Register (/register) │
                 └───────┬──────────┘          └──────────────────────┘
                         │  (authenticated)
                         ▼
              ┌──────────────────────────────┐
              │   Role-Based Route Selection   │
              └──────┬───────────────┬────────┘
                     │               │
              CONSUMER              ADMIN
                     │               │
                     ▼               ▼
    ┌────────────────────────┐  ┌───────────────────────────┐
    │  Consumer Dashboard    │  │  Admin Dashboard          │
    │  /dashboard            │  │  /admin/dashboard         │
    │                        │  │                           │
    │  ├ /readings           │  │  ├ /admin/users           │
    │  ├ /readings/new       │  │  ├ /admin/users/:id       │
    │  ├ /bills              │  │  ├ /admin/tariffs         │
    │  ├ /bills/:id          │  │  ├ /admin/readings        │
    │  ├ /payments           │  │  ├ /admin/bills           │
    │  ├ /payments/new       │  │  ├ /admin/payments        │
    │  ├ /complaints         │  │  ├ /admin/complaints      │
    │  ├ /complaints/new     │  │  ├ /admin/notifications   │
    │  ├ /notifications      │  │  ├ /admin/audit-logs      │
    │  ├ /analytics          │  │  └ /admin/reports         │
    │  └ /profile            │  │                           │
    └────────────────────────┘  └───────────────────────────┘

    Shared Auth Pages:
    ├ /forgot-password
    └ /reset-password?token=xxx
```

---

## 12. Customer User Flow

```
                     ┌─────────────────────┐
                     │   Visits Landing    │
                     └─────────┬───────────┘
                               │
                     ┌─────────▼───────────┐
                     │   Has Account?      │
                     └───┬─────────────┬───┘
                    Yes  │             │  No
                         ▼             ▼
              ┌────────────────┐  ┌────────────────┐
              │   Login        │  │   Register     │
              │   /login       │  │   /register    │
              └───────┬────────┘  └───────┬────────┘
                      │                   │
                      └─────────┬─────────┘
                                ▼
                     ┌─────────────────────┐
                     │   Consumer Dashboard│
                     │   /dashboard        │
                     └───┬─────┬──────┬────┘
                         │     │      │
         ┌───────────────┘     │      └──────────────────┐
         ▼                     ▼                         ▼
   ┌────────────┐       ┌────────────┐          ┌────────────────┐
   │ Submit     │       │ View Bills │          │ View Analytics │
   │ Reading    │       │ /bills     │          │ /analytics     │
   │ /readings/ │       └─────┬──────┘          └────────────────┘
   │ new        │             │
   └────────────┘      ┌──────┴────────┐
                       ▼               ▼
                ┌────────────┐  ┌──────────────┐
                │ Pay Bill   │  │ Bill Details │
                │ /payments/ │  │ /bills/:id   │
                │ new        │  └──────────────┘
                └────────────┘
         ┌───────────────┐
         ▼               ▼
   ┌────────────┐  ┌────────────────┐
   │ Raise      │  │ View           │
   │ Complaint  │  │ Complaints     │
   │ /complaint │  │ /complaints    │
   │ s/new      │  └────────────────┘
   └────────────┘
         ┌───────────────┐
         ▼               ▼
   ┌────────────┐  ┌────────────────┐
   │ Profile    │  │ Notifications  │
   │ /profile   │  │ /notifications │
   └────────────┘  └────────────────┘
```

**Common exit paths:**
- **Logout:** User dropdown → Logout → Redirect to Landing.
- **Session expiry:** Redirect to Login with message "Session expired. Please log in again."

---

## 13. Admin User Flow

```
               ┌─────────────────────┐
               │   Admin Login       │
               │   /login            │
               └─────────┬───────────┘
                         ▼
               ┌─────────────────────┐
               │   Admin Dashboard   │
               │   /admin/dashboard  │
               └───┬──────┬──────┬───┘
                   │      │      │
        ┌──────────┘      │      └──────────────┐
        ▼                 ▼                      ▼
   ┌────────────────┐  ┌───────────┐     ┌────────────────┐
   │ User Management│  │ Tariff    │     │ Reading        │
   │ /admin/users   │  │ Slabs     │     │ Monitoring     │
   └───────┬────────┘  │ /admin/   │     │ /admin/        │
           │           │ tariffs   │     │ readings       │
           ▼           └───────────┘     └────────────────┘
   ┌────────────────┐
   │ User Details   │      ┌───────────┐     ┌────────────────┐
   │ /admin/users/  │      │ Bill      │     │ Payment        │
   │ :id            │      │ Management│     │ Monitoring     │
   └────────────────┘      │ /admin/   │     │ /admin/        │
                           │ bills     │     │ payments       │
                           └─────┬─────┘     └────────────────┘
                                 │
                           ┌─────▼─────┐     ┌────────────────┐
                           │ Generate   │     │ Complaint      │
                           │ Bills      │     │ Management     │
                           │ (Modal)    │     │ /admin/        │
                           └───────────┘     │ complaints     │
                                             └────────────────┘
        ┌──────────────┐      ┌───────────┐
        │ Notifications │      │ Reports   │     ┌────────────────┐
        │ /admin/notif  │      │ /admin/   │     │ Audit Logs     │
        │ ications      │      │ reports   │     │ /admin/audit-  │
        └───────────────┘      └───────────┘     │ logs           │
                                                 └────────────────┘
```

---

## 14. Form Fields and Validation Messages

| Screen | Field | Type | Validation Rule | Error Message |
|---|---|---|---|---|
| Login | Email | text | Valid email format | "Please enter a valid email address." |
| Login | Password | password | Min 8 characters | "Password must be at least 8 characters." |
| Login | — | — | Invalid credentials | "Invalid email or password. Please try again." |
| Login | — | — | Account deactivated | "Your account has been deactivated. Please contact support." |
| Register | Full Name | text | 2–100 chars | "Name must be between 2 and 100 characters." |
| Register | Email | email | Valid format, unique | "Please enter a valid email address." / "This email is already registered." |
| Register | Phone | tel | 10 digits | "Phone number must be 10 digits." |
| Register | Password | password | 8+ chars, 1 upper, 1 lower, 1 digit | "Password must be at least 8 characters with 1 uppercase, 1 lowercase, and 1 digit." |
| Register | Confirm Password | password | Match password | "Passwords do not match." |
| Register | Address Line 1 | text | Max 255 chars | "Address cannot exceed 255 characters." |
| Register | Pincode | text | 6 digits | "Pincode must be 6 digits." |
| Reading | Meter Value | number | > 0 and > last reading | "Meter value must be greater than the previous reading ({value})." |
| Reading | Reading Date | date | Not future, no duplicate | "A reading for this date already exists. Each day can have only one reading." |
| Bill Payment | Amount | number (read-only) | Must match bill total | "Amount does not match the bill total." |
| Complaint | Category | dropdown | Required | "Please select a complaint category." |
| Complaint | Subject | text | 10–200 chars | "Subject must be between 10 and 200 characters." |
| Complaint | Description | textarea | 20–1000 chars | "Description must be at least 20 characters." |
| Change Password | Current Password | password | Required | "Please enter your current password." |
| Change Password | New Password | password | Same rules as register | "Password must be at least 8 characters..." |
| Change Password | Confirm New Password | password | Match new password | "Passwords do not match." |
| Tariff Slab | Slab Name | text | Required, max 100 | "Slab name is required." |
| Tariff Slab | Min Units | number | >= 0 | "Min units must be 0 or greater." |
| Tariff Slab | Max Units | number | > minUnits | "Max units must be greater than min units." |
| Tariff Slab | Rate per Unit | number | > 0 | "Rate must be greater than 0." |
| Tariff Slab | — | — | Overlapping slabs | "This slab range overlaps with an existing slab ({name})." |
| Admin Notification | Title | text | Required, max 200 | "Title is required and must be under 200 characters." |
| Admin Notification | Message | textarea | Required, max 1000 | "Message is required and must be under 1000 characters." |
| Admin — Generate Bills | Billing Month | month picker | Must be past month | "Please select a valid past billing month." |
| Admin — Flag Reading | Remarks | textarea | Max 500 | "Remarks cannot exceed 500 characters." |

---

## 15. Table Columns, Filters, Sorting, and Pagination

### Generic Table Component Specification

| Feature | Specification |
|---|---|
| **Default Sort** | First sortable column, descending (usually timestamp or ID) |
| **Sort Indicator** | Up/down arrow in column header; active column highlighted |
| **Multi-Sort** | Not supported in V1 (single-column sort only) |
| **Filters** | Above the table, in a collapsible filter bar or inline dropdowns |
| **Date Filters** | Date range picker (from–to) |
| **Search** | Text input for global search within the list |
| **Page Size** | Default 10; options: 10, 25, 50 |
| **Pagination** | Bottom-centre: Previous, page numbers (max 5 shown + ellipsis), Next. Also show "Showing 1–10 of 150" text. |
| **Row Click** | Entire row clickable → navigates to detail view |
| **Selection** | Checkbox column (leftmost) for batch actions (future) |
| **Empty** | Centred message with optional CTA |

### Table Specifications by Screen

| Screen | Columns | Sortable | Filters |
|---|---|---|---|
| Reading History (Consumer) | Date, Meter Value, Units, Status, Submitted At | Date, Units, Submitted At | Date range, Status |
| Bill History (Consumer) | Bill #, Type, Month, Units, Amount, Status, Actions | Month, Units, Amount | Type, Month, Status |
| Payment History (Consumer) | Transaction ID, Bill, Amount, Method, Status, Paid At | Amount, Paid At | Date range, Method, Status |
| Complaints (Consumer) | Ticket #, Subject, Category, Status, Created At | Created At | Category, Status |
| Notifications (Consumer) | Icon, Title, Message, Timestamp, Read Status | Timestamp | Unread only toggle |
| Users (Admin) | Consumer #, Name, Email, Phone, Status, Last Login, Created, Actions | Consumer #, Name, Email, Last Login, Created | Text search, Status |
| Readings (Admin) | Consumer, Date, Meter Value, Units, Status, Submitted, Actions | Date, Meter Value, Units, Submitted | Consumer search, Status, Date range |
| Bills (Admin) | Bill #, Consumer, Type, Month, Units, Amount, Status, Generated | Month, Units, Amount, Generated | Consumer search, Status, Month, Type |
| Payments (Admin) | Transaction ID, Consumer, Bill, Amount, Method, Status, Paid At | Amount, Paid At | Consumer search, Method, Status, Date range |
| Complaints (Admin) | Ticket #, Consumer, Subject, Category, Status, Priority, Created, Actions | Created At | Status, Category, Priority, Date range |
| Tariff Slabs (Admin) | Slab Name, Min, Max, Rate, Status, Actions | Slab Name, Min, Max, Rate | — |
| Notifications History (Admin) | Title, Type, Target, Sent At, Recipients | Sent At | — |
| Audit Logs (Admin) | Timestamp, Action, Performed By, Target, Details | Timestamp | Action type, Performed By, Date range |

---

## 16. Dashboard Cards and Chart Requirements

### 16.1 Consumer Dashboard Cards

| Card ID | Title | Data Source | Format | Trend |
|---|---|---|---|---|
| CDC-1 | Current Month Consumption | `currentMonth.consumption` | `{value} kWh` | Arrow (↑ / ↓) + percentage vs prev month |
| CDC-2 | Current Month Bill | `currentMonth.billAmount` | `₹{value}` | Status badge (PAID / UNPAID) |
| CDC-3 | Pending Bills | `pendingBillsCount` | `{count}` badge | — |
| CDC-4 | Unread Notifications | `unreadNotifications` | `{count}` badge | — |

### 16.2 Admin Dashboard Cards

| Card ID | Title | Data Source | Format |
|---|---|---|---|
| ADC-1 | Total Users | `totalUsers` | `{count}` |
| ADC-2 | Active Users | `activeUsers` | `{count}` (/% of total) |
| ADC-3 | Total Readings | `totalReadings` | `{count}` |
| ADC-4 | Total Bills | `totalBillsGenerated` | `{count}` |
| ADC-5 | Total Collections | `totalAmountCollected` | `₹{value}` |
| ADC-6 | Pending Complaints | `pendingComplaints` | `{count}` (red badge if > 0) |

### 16.3 Chart Requirements

| Chart | Type | Library | Data Points | Interactivity |
|---|---|---|---|---|
| Daily Consumption Trend | Line (smooth) | Recharts / Chart.js | X: dates, Y: units consumed | Hover tooltip with exact value; click point → view that day's details |
| Cost Breakdown by Slab | Doughnut | Recharts / Chart.js | Slabs with units and amount | Hover shows slab name + units + amount; legend clickable for hide/show |
| Monthly Revenue (Admin) | Grouped Bar | Recharts / Chart.js | X: months, Y: billed vs collected (2 bars per month) | Hover tooltip; click bar → view that month's revenue detail |
| Collection Rate (Admin) | Radial Gauge | Custom SVG / Recharts | 0–100% value | Animated fill on load; label shows percentage |

**Chart Defaults:**
- Responsive (resize with container).
- Loading: skeleton placeholder matching chart dimensions.
- Empty: "No data available for this period."
- Error: "Unable to load chart data."

---

## 17. Loading, Success, Warning, Error, and Empty States

### 17.1 Loading States

| Scenario | Visual | Duration Behaviour |
|---|---|---|
| Initial page load | Full-page centred spinner | Shows until API responds or 500ms minimum |
| Section reload | Skeleton placeholders | 3–5 shimmer rows/cards |
| Form submission | Button spinner, fields disabled | Until API responds |
| Table data load | Skeleton rows (3–5) | Until data arrives |
| Chart load | Grey rectangle with centred spinner | Until chart data renders |

### 17.2 Success States

| Scenario | Visual | Dismiss |
|---|---|---|
| Form submitted | Green toast (top-right): "Reading submitted successfully!" | Auto-dismiss after 5s |
| Data saved | Green toast: "Profile updated successfully." | Auto-dismiss after 5s |
| Payment recorded | Green toast: "Payment of ₹1,837.50 confirmed! Transaction ID: PAY-..." | Auto-dismiss after 5s |
| Complaint raised | Green toast: "Complaint raised. Ticket: CMP-2026..." | Auto-dismiss after 5s |
| Bill generated | Green toast: "Bills generated for July 2026 (25 consumers)." | Auto-dismiss after 5s |
| User deactivated | Green toast: "User John Doe has been deactivated." | Auto-dismiss after 5s |

### 17.3 Warning States

| Scenario | Visual | Dismiss |
|---|---|---|
| Reading suspicious | Amber toast: "This reading shows unusual consumption." | Manual dismiss |
| Low balance (future) | Amber inline alert on dashboard | Manual dismiss |
| Session expiring | Amber toast: "Your session will expire in 5 minutes." | Until session refreshed |
| Pending complaints (Admin) | Amber badge on dashboard card: "{n} pending" | Persistent |

### 17.4 Error States

| Scenario | Visual | Dismiss |
|---|---|---|
| API failure | Red toast: "Something went wrong. Please try again." | Manual dismiss |
| Validation error | Red inline text below the specific field | Clear on valid input |
| Network offline | Red banner at top: "No internet connection. Some features may be unavailable." | Clears when online |
| 401 Unauthorized | Redirect to login with red toast: "Session expired." | — |
| 403 Forbidden | Red toast: "You do not have permission to perform this action." | Manual dismiss |
| 500 Server Error | Red toast: "Server error. Our team has been notified." | Manual dismiss |

### 17.5 Empty States

| Screen | Message | Illustration | CTA |
|---|---|---|---|
| Reading History | "No readings submitted yet." | Empty meter gauge illustration | "Submit Your First Reading" button |
| Bill History | "No bills generated yet." | Empty document illustration | — |
| Payment History | "No payments made yet." | Empty wallet illustration | — |
| Complaints | "No complaints raised." | Empty ticket illustration | "Raise a Complaint" button |
| Notifications | "No notifications yet." | Empty bell illustration | — |
| Analytics | "Submit readings to see your analytics." | Empty chart illustration | "Submit Reading" button |
| Admin: Users | "No users match your search." | Empty search illustration | — |
| Admin: Tariff Slabs | "No tariff slabs configured." | Empty list illustration | "Add Tariff Slab" button |
| Admin: Readings | "No readings found." | Empty list illustration | — |
| Admin: Audit Logs | "No audit log entries found." | Empty list illustration | — |

---

## 18. Accessibility Requirements

### 18.1 WCAG 2.1 AA Compliance

| Criteria | Implementation |
|---|---|
| **1.1.1 Non-text Content** | All icons have `aria-label` or hidden descriptive text. Images have `alt` attributes. |
| **1.4.3 Contrast (Minimum)** | All text meets 4.5:1 contrast ratio (normal text) and 3:1 (large text) against backgrounds. |
| **1.4.4 Resize Text** | Text can be resized up to 200% without loss of content or functionality. |
| **1.4.11 Non-text Contrast** | UI components (form inputs, focus indicators, borders) meet 3:1 contrast. |
| **2.1.1 Keyboard** | All functionality is operable via keyboard (Tab, Enter, Escape, arrow keys for dropdowns). |
| **2.1.2 No Keyboard Trap** | Focus never gets trapped in a component; modals trap focus but close with Escape. |
| **2.4.3 Focus Order** | Tab order follows visual layout in a logical sequence. |
| **2.4.7 Focus Visible** | All interactive elements have a visible focus ring (2px primary colour offset by 2px). |
| **2.5.3 Label in Name** | Accessible labels for buttons/links match visible text labels. |
| **3.3.1 Error Identification** | Validation errors are described inline next to the field in text. |
| **3.3.2 Labels or Instructions** | All form inputs have visible labels positioned above the field. |
| **4.1.2 Name, Role, Value** | All custom components (modals, dropdowns, tabs) have appropriate ARIA roles and states. |

### 18.2 Additional Accessibility Features

| Feature | Implementation |
|---|---|
| **Skip Navigation** | "Skip to main content" link as first focusable element on every authenticated page. |
| **ARIA Landmarks** | `<nav>` for sidebar/top nav, `<main>` for content, `<header>` for page headers, `<footer>` for footer. |
| **Screen Reader Announcements** | Toast messages use `aria-live="polite"` region. Dynamic content updates announce changes. |
| **Reduced Motion** | Respect `prefers-reduced-motion` by disabling animations and transitions. |
| **Touch Targets** | Minimum 44×44px for all interactive elements on touch devices. |
| **Colour Independence** | Status is never conveyed by colour alone. Badges also include text labels (e.g., "PAID" not just green). |
| **Zoom Support** | Layout does not break at 200% browser zoom. Horizontal scrolling avoided. |

---

## 19. Responsive Behaviour for Every Major Screen

| Screen | Mobile (<768px) | Tablet (768–1023px) | Desktop (≥1024px) |
|---|---|---|---|
| **Landing Page** | Stacked hero + feature cards (1-col). CTA buttons full width. | Feature cards 2-col grid. Centred CTA. | Full hero with feature cards 3-col grid. |
| **Login / Register** | Full-width card with no side padding. Single column fields. | Centred card (max 480px). | Same as tablet. |
| **Forgot/Reset Password** | Same as login. | Same as login. | Same as login. |
| **Consumer Dashboard** | 1-col stat cards. Charts stack vertically. Sidebar hidden (overlay). | 2-col stat cards. Charts side by side. Sidebar collapsed (icons). | 4-col stat row. Charts side by side. Sidebar expanded. |
| **Reading Entry** | Full-width form. Previous reading shown in compact card. | Centred form (max 480px). | Same as tablet. |
| **Reading History** | Horizontal scroll table. Show: Date, Units, Status. Hide: Meter Value, Submitted At. Pagination compact. | Scroll table. Show all columns. | Full table with all columns. |
| **Bill History** | Tabs as horizontal scroll pills. Table: Bill #, Amount, Status + actions. | Tabs + filter bar visible. Table with all columns. | Full layout. |
| **Bill Detail** | Single column. Slab table scrollable horizontally. | Single column with better spacing. | Summary + slab table side by side (optional). |
| **Payment History** | Similar to reading history — horizontal scroll. | Full columns visible. | Full layout. |
| **Complaints** | Horizontal scroll table. Ticket #, Status, Created. | All columns visible. | Full layout with inline filters. |
| **Analytics** | Charts full width, stacked vertically. Period selector as dropdown. | Charts full width. Period selector as tabs. | Charts side by side. Tabs + month picker. |
| **Notifications** | List cards (no table). Full-width items. | List cards or table. | Table view. |
| **Profile** | Single column, full width. | Centred (max 640px). | Same as tablet. |
| **Admin Dashboard** | 2-col stat grid (3 rows). Charts stacked. | 3-col (2 rows). Charts side by side. | 3-col (2 rows) or 6 in 1 row. Charts full width. |
| **User Management** | Horizontal scroll table. Name, Email, Status, Actions. | All columns visible. Search bar visible. | Full layout with all filters. |
| **User Details** | Stacked sections. | Side-by-side summary + activity. | Full layout. |
| **Tariff Management** | Horizontal scroll table. Slab Name, Rate, Actions. | All columns visible. | Full layout. |
| **Complaint Management** | Horizontal scroll table. Ticket #, Status, Actions. | Most columns visible. | Full layout with side panel for details. |
| **Admin Reports** | Charts stack vertically. Tables collapsed. | Charts side by side. Table compact. | Full layout with side-by-side charts. |

---

## 20. Role-Based UI Visibility

### 20.1 Navigation Items

| Route | Consumer | Admin |
|---|---|---|
| `/dashboard` | ✅ Visible | ❌ Redirect to `/admin/dashboard` |
| `/readings/*` | ✅ Visible | ❌ Not shown |
| `/bills/*` | ✅ Visible | ❌ Not shown |
| `/payments/*` | ✅ Visible | ❌ Not shown |
| `/complaints/*` | ✅ Visible | ❌ Not shown |
| `/notifications` | ✅ Visible | ❌ Not shown |
| `/analytics` | ✅ Visible | ❌ Not shown |
| `/profile` | ✅ Visible | ❌ Not shown |
| `/admin/dashboard` | ❌ Not shown | ✅ Visible |
| `/admin/users/*` | ❌ Not shown | ✅ Visible |
| `/admin/tariffs` | ❌ Not shown | ✅ Visible |
| `/admin/readings` | ❌ Not shown | ✅ Visible |
| `/admin/bills` | ❌ Not shown | ✅ Visible |
| `/admin/payments` | ❌ Not shown | ✅ Visible |
| `/admin/complaints` | ❌ Not shown | ✅ Visible |
| `/admin/notifications` | ❌ Not shown | ✅ Visible |
| `/admin/audit-logs` | ❌ Not shown | ✅ Visible |
| `/admin/reports` | ❌ Not shown | ✅ Visible |

### 20.2 Action Visibility

| Action | Consumer | Admin |
|---|---|---|
| Submit Reading | ✅ | ❌ |
| View Own Bills | ✅ | ❌ |
| Pay Own Bill | ✅ | ❌ |
| Raise Complaint | ✅ | ❌ |
| Mark Notification Read | ✅ | ❌ |
| View All Users | ❌ | ✅ |
| Create/Edit Tariff Slabs | ❌ | ✅ |
| Generate Bills | ❌ | ✅ |
| Update Complaint Status | ❌ | ✅ |
| Send Broadcast Notifications | ❌ | ✅ |
| View Audit Logs | ❌ | ✅ |
| View Reports | ❌ | ✅ |
| Deactivate User | ❌ | ✅ |
| Change Own Password | ✅ | ✅ |
| View Own Profile | ✅ | ✅ |

---

## 21. Reusable UI Component Inventory

| Component | Description | Used In |
|---|---|---|
| **`StatCard`** | Displays a metric with icon, value, label, optional trend | Dashboards (consumer + admin) |
| **`DataTable`** | Generic table with sortable columns, filters, pagination | All list screens |
| **`SearchInput`** | Text input with search icon and debounce | User management, admin searches |
| **`DateRangePicker`** | Two date inputs (from/to) for filtering | Reading history, payments, reports |
| **`StatusBadge`** | Coloured pill showing status with icon | Tables, cards, detail pages |
| **`ActionButton`** | Primary / Secondary / Danger / Ghost variants | Forms, page headers, tables |
| **`IconButton`** | Circular button with icon only | Table action columns, close buttons |
| **`ConfirmModal`** | Confirmation dialog with message + confirm/cancel | Deactivate user, delete slab |
| **`FormModal`** | Modal containing a form | Create/edit tariff slab, raise complaint |
| **`PageHeader`** | Breadcrumb + title + action buttons area | All authenticated pages |
| **`Breadcrumb`** | Navigation path with links | Detail pages (bill, user, complaint) |
| **`SkeletonLoader`** | Animated placeholder block | Loading states for cards, tables, charts |
| **`EmptyState`** | Illustration + message + optional CTA | Empty table states |
| **`ErrorState`** | Error icon + message + retry button | API failure states |
| **`Toast`** | Dismissible notification (success/warning/error/info) | Form submissions, system events |
| **`InlineAlert`** | Banner-style alert with icon and message | Form validation summary, page-level warnings |
| **`Sidebar`** | Vertical navigation menu with icons | Authenticated layouts (consumer + admin) |
| **`TopNav`** | Horizontal bar with logo, search, notifications, user menu | Authenticated layouts |
| **`Footer`** | Copyright + legal links | Public pages |
| **`LineChart`** | Reusable line chart wrapper | Consumer analytics, admin reports |
| **`BarChart`** | Reusable bar/grouped bar chart wrapper | Consumer analytics, admin reports |
| **`DoughnutChart`** | Reusable doughnut chart wrapper | Consumer cost breakdown |
| **`GaugeChart`** | Radial gauge for single metric | Admin collection rate |
| **`ChartTooltip`** | Hover tooltip for chart data points | All charts |
| **`TabGroup`** | Horizontal tab bar | Bill history (All/Monthly/Daily), analytics period |
| **`Pagination`** | Page numbers + prev/next + "Showing X–Y of Z" | All data tables |
| **`Dropdown`** | Select dropdown with search option | Forms (category, payment method) |
| **`Avatar`** | User avatar (first letter of name or image) | User menu, user list, user detail |
| **`NotificationDot`** | Small coloured dot indicating unread status | Notification bell, notification list |
| **`SidePanel`** | Slide-in panel for detail views | Admin complaint details |
| **`FileUpload`** | File drop zone (future) | Bill PDF upload/download |

---

## 22. Dummy Data Examples for Design Validation

### 22.1 Consumers

| Field | Consumer A | Consumer B | Consumer C |
|---|---|---|---|
| Full Name | Rajesh Sharma | Priya Patel | Amit Singh |
| Email | rajesh.sharma@email.com | priya.patel@email.com | amit.singh@email.com |
| Phone | 9876543210 | 9988776655 | 9123456780 |
| Consumer Number | VOL-2026-000001 | VOL-2026-000042 | VOL-2026-00105 |
| Address | 42, Green Park, Mumbai, MH 400001 | 7/B, Sunshine Apts, Pune, MH 411001 | 120, Lake Road, Delhi, DL 110001 |
| Status | Active | Active | Inactive |

### 22.2 Meter Readings (Consumer A — July 2026)

| Date | Meter Value | Previous Value | Units Consumed | Status |
|---|---|---|---|---|
| 01 Jul 2026 | 1200.00 | 1150.00 | 50.00 | VERIFIED |
| 02 Jul 2026 | 1212.50 | 1200.00 | 12.50 | VERIFIED |
| 03 Jul 2026 | 1223.00 | 1212.50 | 10.50 | VERIFIED |
| 05 Jul 2026 | 1245.00 | 1223.00 | 22.00 | VERIFIED |
| ... | ... | ... | ... | ... |
| 15 Jul 2026 | 1365.00 | 1346.50 | 18.50 | VERIFIED |
| 27 Jul 2026 | 1550.00 | 1500.50 | 50.50 | VERIFIED |

### 22.3 Tariff Slabs

| Slab Name | Min Units | Max Units | Rate (₹/unit) |
|---|---|---|---|
| 0–100 Units | 0 | 100 | 3.50 |
| 101–200 Units | 101 | 200 | 4.50 |
| 201–500 Units | 201 | 500 | 6.00 |
| 501+ Units | 501 | 999999 | 7.50 |

### 22.4 Monthly Bill (Consumer A — July 2026)

| Item | Value |
|---|---|
| Bill Number | BILL-2026-07-0001 |
| Consumer | Rajesh Sharma (VOL-2026-000001) |
| Total Units | 350.00 kWh |
| Slab 1: 0–100 × ₹3.50 | 100 × 3.50 = ₹350.00 |
| Slab 2: 101–200 × ₹4.50 | 100 × 4.50 = ₹450.00 |
| Slab 3: 201–500 × ₹6.00 | 150 × 6.00 = ₹900.00 |
| Total Energy Charge | ₹1,700.00 |
| Fixed Charges | ₹50.00 |
| Tax (5%) | ₹87.50 |
| **Total Amount** | **₹1,837.50** |
| Status | UNPAID |
| Due Date | 15 Aug 2026 |

### 22.5 Payment (Consumer A)

| Field | Value |
|---|---|
| Transaction ID | PAY-20260727-0001 |
| Bill Reference | BILL-2026-07-0001 |
| Amount | ₹1,837.50 |
| Payment Method | Bank Transfer |
| Status | COMPLETED |
| Paid At | 27 Jul 2026 12:00 PM |

### 22.6 Complaints

| Field | Complaint 1 | Complaint 2 |
|---|---|---|
| Ticket # | CMP-20260727-0001 | CMP-20260725-0003 |
| Consumer | Rajesh Sharma | Priya Patel |
| Category | Billing Issue | Meter Issue |
| Subject | Incorrect bill amount for July 2026 | Meter showing abnormally high reading |
| Description | My bill shows 350 units but I only used 200 units. Please review. | The meter reading jumped from 1200 to 1500 in one day. Something is wrong. |
| Status | OPEN | IN_PROGRESS |
| Created | 27 Jul 2026 | 25 Jul 2026 |

### 22.7 Admin Dashboard KPIs

| KPI | Value |
|---|---|
| Total Users | 1,250 |
| Active Users | 1,180 |
| Total Readings | 36,500 |
| Total Bills Generated | 1,200 |
| Total Amount Collected | ₹18,37,500 |
| Pending Complaints | 27 |
| Collection Rate | 78.5% |

---

## 23. UI Acceptance Criteria

| ID | Criteria | Verification |
|---|---|---|
| **UI-AC-01** | All 25+ screens defined in this document render without layout breakage across Chrome, Firefox, Edge (latest 2 versions) and Safari (latest). | Manual QA |
| **UI-AC-02** | The application is fully responsive at breakpoints 375px, 768px, 1024px, 1440px with no overlapping elements or horizontal scroll. | Responsive design testing |
| **UI-AC-03** | All colour combinations meet WCAG AA contrast ratios (4.5:1 normal text, 3:1 large text). | Automated aXe/Cypress a11y checks |
| **UI-AC-04** | Every form field shows inline validation errors on blur and on submit when invalid. | Unit tests + manual |
| **UI-AC-05** | Every API call displays a loading indicator (spinner/skeleton) within 300ms of initiation. | Manual + performance audit |
| **UI-AC-06** | All success/error API responses display a toast notification within 500ms. | Manual |
| **UI-AC-07** | All tables are paginated with max 10 rows per page by default; pagination controls work correctly. | Manual |
| **UI-AC-08** | Consumer cannot see or navigate to any admin route; admin cannot see consumer dashboard. | Role-based route test |
| **UI-AC-09** | All empty states display the correct illustration, message, and CTA (where applicable). | Manual |
| **UI-AC-10** | All interactive elements are keyboard-accessible and show a visible focus ring. | Keyboard-only navigation test |
| **UI-AC-11** | Charts render correctly with data; show appropriate empty/error state when no data. | Manual + mock data test |
| **UI-AC-12** | Confirmation modal appears before all destructive actions (deactivate user, delete slab). | Manual |
| **UI-AC-13** | All pages load within 3 seconds on a standard broadband connection (cached). | Lighthouse performance audit |
| **UI-AC-14** | Sidebar highlights the active route. Breadcrumbs reflect accurate navigation path. | Manual |
| **UI-AC-15** | Session expiry redirects to login page with appropriate message. Token invalidation clears stored state. | Manual + auth test |

---

## 24. Out-of-Scope Items

The following UI features are explicitly **out of scope** for Phase 7 (UI Design) and will not be implemented in this phase:

| Item | Reason |
|---|---|
| **Real-time meter integration UI** | The platform relies on consumer-submitted readings, not IoT/smart meters. |
| **Live payment gateway checkout UI** | Payments are recorded manually within the system; no third-party payment gateway integration in V1. |
| **Mobile native applications (iOS/Android)** | Phase 1 covers a responsive web application only. |
| **Multi-language support** | English-only UI in V1. |
| **Dark mode** | Not included in the initial design; could be a future enhancement. |
| **Advanced ML-based consumption forecasting charts** | V1 provides basic analytics (trends, comparisons) only. |
| **PDF generation and download** | The "Download PDF" button is a placeholder; PDF generation is future scope. |
| **CSV export for reports** | Export functionality is marked as future. |
| **Bulk reading upload (CSV)** | Consumers submit readings one at a time in V1. |
| **Advanced filtering (multi-select, saved filters)** | Filters are single-value in V1. |
| **Batch user actions (select multiple users → bulk deactivate)** | Not supported in V1. |
| **Push notifications / email notifications** | In-app notifications only in V1. |
| **Real-time data updates (WebSockets)** | All data refreshes via manual page reload or periodic polling (future: WebSocket for live updates). |
| **Role-based access control beyond Consumer & Admin** | Only two roles in V1. |
| **UI for late payment penalties / fine calculations** | Not part of Phase 1 business rules. |
| **OAuth / Social login UI** | Email + password authentication only. |

---

## 25. Phase 7 Completion Checklist

| # | Deliverable | Status |
|---|---|---|
| 1 | UI Design Goals defined | ✅ |
| 2 | Target Users and Roles identified | ✅ |
| 3 | Design Principles documented | ✅ |
| 4 | Visual Identity defined (colours, typography, spacing, shadows, icons) | ✅ |
| 5 | Responsive Layout Strategy documented (mobile, tablet, desktop) | ✅ |
| 6 | Common Layout Components specified (public, customer, admin layouts; nav, sidebar, footer, page header, cards, tables, forms, modals, alerts, loaders) | ✅ |
| 7 | Public Screens designed (landing, login, register, forgot/reset password) | ✅ |
| 8 | Customer Screens designed (dashboard, profile, reading entry, reading history, daily bill, monthly bill, bill history, payment history, notifications, complaints, analytics) | ✅ |
| 9 | Admin Screens designed (dashboard, user management, user details, tariffs, reading monitoring, bill management, payment monitoring, complaint management, notification management, audit logs, reports) | ✅ |
| 10 | Screen-by-Screen Wireframe Descriptions provided | ✅ |
| 11 | Navigation Structure defined (with Mermaid-style diagram) | ✅ |
| 12 | Customer User Flow documented | ✅ |
| 13 | Admin User Flow documented | ✅ |
| 14 | Form Fields and Validation Messages specified | ✅ |
| 15 | Table Columns, Filters, Sorting, and Pagination specified | ✅ |
| 16 | Dashboard Cards and Chart Requirements documented | ✅ |
| 17 | Loading, Success, Warning, Error, and Empty States covered | ✅ |
| 18 | Accessibility Requirements defined (WCAG 2.1 AA) | ✅ |
| 19 | Responsive Behaviour for every major screen documented | ✅ |
| 20 | Role-Based UI Visibility matrix defined | ✅ |
| 21 | Reusable UI Component Inventory catalogued | ✅ |
| 22 | Dummy Data Examples provided for design validation | ✅ |
| 23 | UI Acceptance Criteria listed | ✅ |
| 24 | Out-of-Scope Items documented | ✅ |
| 25 | Phase 7 Completion Checklist completed | ✅ |

---

> **End of Phase 7 — Deliverable**  
> *`docs/06_UI_DESIGN.md` has been generated.*  
> *Pending approval to proceed to implementation.*
