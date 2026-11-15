---
version: 1
slug: "frontend-src-app-payroll-page-tsx"
primary_target: "frontend/src/app/payroll/page.tsx"
related_targets: ["frontend/src/app/dashboard/page.tsx","frontend/src/components/app-shell.tsx"]
---

## Direction contract

- seed:5cedd72b2963a1c6
- world: Midnight Transit (wayfinding-cartography-signage-midnight-transit-diagram)
- mode: operate

### THESIS
An HR and payroll operations console modeled on a midnight-enamel transit dispatch board: high-contrast porcelain work sheets anchored by a dark `#0b1230` signal rail, where every payroll run moves visibly along a 4-station route (`Draft → Computed → Approved → Paid`).

### OWN-WORLD
- Deep midnight-blue enamel shell (`#0b1230` rail, `#0f1c30` header/pipeline tracks, `#1e3252` hairline dividers) paired with crisp daytime porcelain working surfaces (`#f4f7fb` canvas, `#ffffff` sheets, `#cbd5e1` 1px borders) for sustained Operate-mode legibility.
- The 4-line transit stripe motif (Scarlet `#e21d2d`, Cobalt `#1e5bff`, Amber `#d97706`, Emerald `#16a34a`) and circular station markers (`Draft`, `Computed`, `Approved`, `Paid`) give instant status legibility without decorative gradients or glass.
- Geometric transit sans (`Barlow` for headings/station labels/wordmark, `Inter` for dense UI and `tabular-nums` PHP currency figures).

### STORY
When a payroll operator opens `/payroll` or `/dashboard`, they are dispatching a semi-monthly Philippines 2026 payroll cycle: selecting or creating a period, computing gross pay and statutory deductions (SSS, PhilHealth, Pag-IBIG, BIR TRAIN), reviewing exceptions, approving, and marking paid—each transition advancing the active station on the 4-station pipeline rail.

### FIRST VIEWPORT
- Persistent midnight-enamel left navigation rail (`#0b1230`) with the 4-line transit stripe header, cobalt `#1e5bff` active route indicator, and live system/auth footer.
- On `/payroll` and `/dashboard`: immediate first-viewport **Payroll Pipeline** dark enamel strip (`Draft → Computed → Approved → Paid`) above dense, tabular-numeric run/KPI sheets and inline statutory action controls.

### FORM
- Standard web Operate controls (`Button`, `Input`, `Select`, `Panel`, `StatusBadge` with station dot, hairline tables with `tabular-nums`) with crisp 1px borders and zero floating chrome or gratuitous shadows.
- Browser surface theming: custom `::selection`, `caret-color`, `:focus-visible` cobalt ring, slim themed scrollbars, skeleton loaders, and instructive empty states.

### FINISH
- Every route (`/login`, `/dashboard`, `/employees`, `/departments`, `/attendance`, `/leave`, `/compensation`, `/payroll`, `/payslips`, `/settings`) shares the Midnight Transit vocabulary while preserving all `/api/v1/*` contracts and strict explicit TypeScript (`no any`).
- Verified by `impeccable detect`, desktop (1440px) + mobile (390px) inspection screenshots, finish review, `DESIGN.md` + `.impeccable/design.json`, and `npm run typecheck && npm test && npm run build`.
