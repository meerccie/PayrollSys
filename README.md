# Philippine Payroll System

A full-stack payroll system for a single Philippine company, built with **Java 21 + Spring Boot 4.1** (Clean Architecture) and **Next.js 15 + TypeScript + Tailwind CSS**.

- **Backend (`/backend`)**: Multi-module Maven project (`payroll-domain`, `payroll-application`, `payroll-adapters`, `payroll-bootstrap`), PostgreSQL + Flyway, JWT auth with rotating `HttpOnly` refresh cookies, OpenPDF payslip generation, deployed on **Render**.
- **Frontend (`/frontend`)**: Next.js App Router, TanStack Query, same-origin `/api/*` rewrites to the backend, deployed on **Vercel**.

---

## Architecture

### System Overview

```
Browser ──▶ Vercel (Next.js App) ──rewrite /api/*──▶ Render (Spring Boot API) ──▶ Render PostgreSQL
```

`frontend/next.config.ts` rewrites `/api/:path*` to `${BACKEND_URL}/api/:path*`, so all browser API requests are same-origin and refresh tokens can use `HttpOnly; SameSite=Strict` cookies without third-party cookie issues.

### Backend Clean Architecture (`/backend`)

| Module | Responsibility | Dependencies |
|---|---|---|
| `payroll-domain` | Pure Java 21 domain entities, `Money` (PHP, scale 2, `HALF_UP`), `PayrollRun` state machine, and PH statutory/payroll calculators (`SssTable`, `PhilHealthTable`, `PagIbigTable`, `BirTaxTable`, `PayrollCalculator`) | None (no Spring/JPA) |
| `payroll-application` | Use-case services (`AuthService`, `OrganizationService`, `EmployeeService`, `AttendanceService`, `LeaveService`, `CompensationService`, `PayPeriodService`, `PayrollRunService`, `PayslipService`, `StatutoryService`) and `ports/out` interfaces | `payroll-domain` only |
| `payroll-adapters` | REST controllers (`/api/v1/*`), RFC 9457 `ProblemDetail` handler, Spring Data JPA entities/repositories, Spring Security 7 + Nimbus JWT, Bucket4j login rate limiter, OpenPDF payslip renderer | `payroll-application`, `payroll-domain` |
| `payroll-bootstrap` | Spring Boot entry point, bean wiring, `application*.yml`, Flyway migrations (`V1__baseline.sql`, `V2__seed_ph_statutory_2026.sql`), ArchUnit & Testcontainers tests | All modules |

Layer boundaries are enforced by Maven dependencies and `ArchitectureTest` (ArchUnit).

---

## Philippine Payroll Rules (Seeded 2026 Defaults)

- **Pay Frequency**: Semi-monthly (1–15 and 16–end of month) and monthly; default statutory schedule splits contributions 50/50 across semi-monthly cutoffs (`SPLIT`, configurable to `FIRST_CUTOFF` or `SECOND_CUTOFF`).
- **Daily Rate Factor**: `261` annual work days (5-day workweek, configurable in company settings).
- **SSS**: 15% of Monthly Salary Credit (5% EE, 10% ER), MSC ₱5,000–₱35,000 in ₱500 steps; EC (employer only) ₱10 (< ₱15,000 MSC) or ₱30 (≥ ₱15,000 MSC).
- **PhilHealth**: 5% of monthly basic salary (floor ₱10,000, ceiling ₱100,000), split 50/50 between employee and employer.
- **Pag-IBIG (HDMF)**: 1% EE (≤ ₱1,500) / 2% EE (> ₱1,500) and 2% ER, capped at ₱10,000 Maximum Fund Salary (₱200/month cap each).
- **BIR Withholding Tax (TRAIN)**: Monthly and semi-monthly brackets applied to `taxableGross - (SSS_EE + PhilHealth_EE + PagIBIG_EE)`.
- **Overtime & Holiday Multipliers**: Regular OT `1.25x`, Rest/Special day `1.30x` (OT `1.69x`), Regular Holiday `2.00x` (OT `2.60x`), Night Differential `+10%`.

---

## Roles & API Overview (`/api/v1`)

Roles: `ADMIN`, `HR`, `EMPLOYEE`.

- **Auth & Users**: `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/change-password`, `/users` (`ADMIN` only).
- **Organization & Employees**: `/departments`, `/positions`, `/employees` (search, CRUD, `POST /employees/{id}/separate`), `GET /employees/me`.
- **Attendance & Holidays**: `/time-entries` (CRUD, `PUT /time-entries/bulk`, `POST /time-entries/approve`), `/holidays`.
- **Leave**: `/leave-types`, `/leave-balances`, `/leave-requests` (`file`, `approve`, `reject`, `cancel`).
- **Compensation**: `/earning-types`, `/deduction-types`, `/employees/{id}/earnings`, `/employees/{id}/deductions`.
- **Payroll & Payslips**: `/pay-periods` (`POST /pay-periods/generate`), `/payroll-runs` (`compute`, `reset`, `approve`, `reopen`, `mark-paid`), `/payroll-runs/{id}/payslips`, `/me/payslips`, `/payslips/{id}`, `GET /payslips/{id}/pdf`.
- **Statutory & Settings**: `/statutory/{sss|philhealth|pagibig|tax}`, `/settings/company`.

---

## Local Development

### Prerequisites
- Java 21+, Maven (or `./mvnw` in `/backend`)
- Node.js 22+
- Docker (for local PostgreSQL and Testcontainers integration tests)

### Setup & Run

1. Copy `.env.example` to `.env` and adjust values as needed.
2. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
3. Run the backend (Flyway runs automatically and seeds the initial `ADMIN` user from `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`):
   ```bash
   cd backend
   ./mvnw -pl payroll-bootstrap spring-boot:run -Dspring-boot.run.profiles=local
   ```
   Swagger UI: `http://localhost:8080/swagger-ui.html`
4. Run the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open `http://localhost:3000` and sign in (local defaults: `admin@example.com` / `ChangeMe123!`).

---

## Verification

- **Backend**:
  ```bash
  cd backend
  ./mvnw -B verify
  ```
- **Frontend**:
  ```bash
  cd frontend
  npm run typecheck && npm test && npm run build
  ```

---

## Deployment

- **Backend (Render)**:
  - Connect this repository as a Render Blueprint using [render.yaml](render.yaml) (provisions `payroll-db` PostgreSQL and `payroll-api` Docker web service using [backend/Dockerfile](backend/Dockerfile)).
  - Set `CORS_ALLOWED_ORIGINS` (your Vercel URL), `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` in the Render dashboard.
- **Frontend (Vercel)**:
  - Import this repository in Vercel with **Root Directory** set to `frontend`.
  - Set environment variable `BACKEND_URL` to your Render API URL (e.g. `https://payroll-api.onrender.com`).
