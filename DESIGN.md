---
name: PH Payroll
description: Philippine Payroll Operations & Statutory Dispatch System (Midnight Transit)
world: "wayfinding-cartography-signage-midnight-transit-diagram"
colors:
  primary: "#1e5bff"
  primary-hover: "#1746cc"
  primary-soft: "#eef4ff"
  primary-border: "#bfdbfe"
  enamel-base: "#0b1230"
  enamel-elevated: "#0f1c30"
  enamel-hover: "#152544"
  enamel-border: "#1e3252"
  neutral-canvas: "#f4f7fb"
  neutral-panel: "#ffffff"
  neutral-subtle: "#edf2f7"
  neutral-border: "#dbe3ee"
  neutral-border-strong: "#cbd5e1"
  ink-primary: "#0b1230"
  ink-muted: "#475569"
  ink-subtle: "#64748b"
  signal-scarlet: "#e21d2d"
  signal-amber: "#d97706"
  signal-green: "#16a34a"
typography:
  display:
    fontFamily: "Barlow, Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0em"
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral-panel}"
    rounded: "{rounded.lg}"
    height: "36px"
    padding: "0 14px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-secondary:
    backgroundColor: "{colors.neutral-panel}"
    textColor: "{colors.ink-primary}"
    rounded: "{rounded.lg}"
    height: "36px"
    padding: "0 14px"
  panel:
    backgroundColor: "{colors.neutral-panel}"
    rounded: "{rounded.lg}"
  pipeline-rail:
    backgroundColor: "{colors.enamel-elevated}"
    textColor: "{colors.neutral-panel}"
    rounded: "{rounded.lg}"
---

## Overview

PH Payroll uses the **Midnight Transit** Operate-mode system: a midnight-enamel navigation rail (`#0b1230`) and 4-station `PayrollPipeline` dispatch track (`Draft → Computed → Approved → Paid`) paired with high-legibility daytime porcelain working surfaces (`#f4f7fb` canvas, `#ffffff` sheets, `#dbe3ee` 1px borders).

## Colors

- **Primary Action**: Cobalt `#1e5bff` (`#1746cc` hover, `#eef4ff` soft highlight, `#bfdbfe` border).
- **Enamel Chrome**: `#0b1230` sidebar rail, `#0f1c30` pipeline dispatch bar, `#152544` active/elevated enamel, `#1e3252` enamel hairlines.
- **Porcelain Surfaces**: `#f4f7fb` app canvas, `#ffffff` panels and form controls, `#edf2f7` table headers.
- **Signal Inks**: Green `#16a34a` (`APPROVED`, `PAID`, `ACTIVE`, `REGULAR`), Cobalt `#1e5bff` (`COMPUTED`, `OPEN`), Amber `#d97706` (`PENDING`, `PROBATIONARY`, net-pay warning), Scarlet `#e21d2d` (`REJECTED`, `SEPARATED`, destructive actions).

## Typography

- **Display (`--font-display`)**: `Barlow` (500, 600, 700) for page titles, panel section headers, metric readouts, and transit station labels.
- **Body & Tables (`--font-sans`)**: `Inter` (400, 500, 600, 700) with `.num` (`font-variant-numeric: tabular-nums`) on all PHP currency amounts, dates, and counts.

## Layout

- Persistent `256px` (`w-64`) sticky midnight-enamel left navigation rail on desktop (`lg:`), collapsing to a top enamel header + slide-over drawer on mobile.
- Main content centered up to `max-w-7xl` with `24px` (`space-y-6`) vertical rhythm.

## Elevation & Depth

- Flat architectural framing with `1px` crisp borders (`#dbe3ee` on porcelain, `#1e3252` on midnight enamel). No decorative drop shadows or backdrop blur.

## Shapes

- Crisp architectural radii: `4px` badges/emblems, `6px` small buttons and nav links, `8px` panels, inputs, and pipeline bars.
- Signature 4-color `TransitStripe` (`3px` height: `#e21d2d`, `#1e5bff`, `#d97706`, `#16a34a`) capping the sidebar rail, login header, and `PayrollPipeline`.

## Components

- `PayrollPipeline` (`frontend/src/components/ui.tsx`): 4-station route (`01 Draft`, `02 Computed`, `03 Approved`, `04 Paid`) in `#0f1c30` midnight enamel.
- `StatusBadge`: Uppercase pill with a leading circular transit station dot.
- `Button`, `Input`, `Select`, `Field`, `Panel`, `PageHeader`, `Alert`, `TableSkeleton`, `EmptyState` in `frontend/src/components/ui.tsx`.

## Do's and Don'ts

- **Do** apply `.num` (`tabular-nums`) to every PHP currency column, employee count, and date cell.
- **Do** keep navigation, tables, and form controls standard web Operate components.
- **Don't** add `border-l-*` side-tab accent stripes, decorative gradients, or glassmorphism.
