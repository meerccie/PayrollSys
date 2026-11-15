# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Primary Operators (`ADMIN`, `HR`)**: Single Philippine SME/company HR and Finance staff working in daytime office environments to maintain employee records, review attendance/overtime and leave requests, run semi-monthly/monthly payroll cycles, and release statutory-compliant payslips.
- **Self-Service Employees (`EMPLOYEE`)**: Company employees signing in to log or inspect attendance, file leave requests, and download their released PDF payslips.

## Product Purpose

Deliver an end-to-end, auditable Philippine payroll workspace that turns daily attendance, leave, and compensation adjustments into accurate SSS, PhilHealth, Pag-IBIG, and BIR TRAIN withholding computations and downloadable PDF payslips. Success means HR/Finance can move a pay period from `DRAFT` to `PAID` with zero manual spreadsheet reconciliation, and employees can immediately verify their net pay and statutory deductions.

## Positioning

A unified single-company Philippine payroll engine where 2026 statutory tables (SSS 15% MSC brackets + EC, PhilHealth 5% floor/ceiling, Pag-IBIG 1%/2% capped at ₱10,000, BIR TRAIN brackets), semi-monthly cutoff scheduling (`SPLIT`, `FIRST_CUTOFF`, `SECOND_CUTOFF`), attendance/leave approvals, and OpenPDF payslip rendering live inside one clean-architecture system without third-party SaaS lock-in.

## Operating Context

- **Payroll Lifecycle**: Generate semi-monthly or monthly pay periods (`1–15`, `16–end of month`) → create `DRAFT` payroll run → `COMPUTED` (review gross, SSS, PhilHealth, Pag-IBIG, tax, net pay warnings) → `APPROVED` (`ADMIN` sign-off) → `PAID` (payslips released).
- **Daily Operations**: Onboarding employees (monthly/daily basis, `261` default annual work days), organizing departments and positions, approving time entries and overtime (`1.25x`–`2.60x` multipliers), and approving/rejecting leave requests.
- **Tech Stack**: Next.js 15 App Router + TypeScript (strict explicit types, no `any`) + TanStack Query + Tailwind CSS v4 frontend proxying `/api/*` to a Java 21 Spring Boot 4.1 Clean Architecture backend with PostgreSQL + Flyway.

## Capabilities and Constraints

- Preserve all existing routes (`/login`, `/dashboard`, `/employees`, `/departments`, `/attendance`, `/leave`, `/compensation`, `/payroll`, `/payslips`, `/settings`) and `/api/v1/*` backend API contracts.
- Preserve role-based navigation and permissions (`ADMIN`, `HR`, `EMPLOYEE`).
- Preserve Philippine 2026 statutory defaults and `PHP` currency precision (`2` decimal places, `HALF_UP`, tabular numerals for all financial columns).
- Replace the visual world freely; no external brand assets or logos are binding.

## Evidence on Hand

- Live Next.js 15 frontend (`frontend/src/app/*`) and Spring Boot 4.1 API (`backend/`) running locally against PostgreSQL 18.6 (`V1__baseline.sql`, `V2__seed_ph_statutory_2026.sql`).
- Real seeded statutory tables (`sss_brackets`, `philhealth_config`, `pagibig_config`, `bir_tax_brackets`, `leave_types`) and live `/api/v1/*` endpoints.

## Product Principles

1. **Calculation Transparency First**: Every peso of gross pay, statutory contribution, withholding tax, and net pay should be scannable and traceable to its cutoff rules.
2. **State-Driven Workflow Clarity**: Payroll runs (`DRAFT` → `COMPUTED` → `APPROVED` → `PAID`), time entries, and leave requests must make current status and the next valid action unmistakable.
3. **Role-Scoped Simplicity**: HR/Admin see operational controls and company-wide ledgers; Employees see only their own attendance, leave, and payslips.
4. **Zero-Friction Daily Density**: Forms, filters, and tables should support rapid office data entry without modal clutter or wasted viewport space.
