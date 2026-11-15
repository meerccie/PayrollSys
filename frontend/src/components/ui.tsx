import React from "react";
import { AlertCircle, CheckCircle2, Info, Loader2 } from "lucide-react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize = "sm" | "md";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
}

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary:
    "bg-[#1e5bff] text-white border border-transparent hover:bg-[#1746cc] active:bg-[#1239a8]",
  secondary:
    "bg-white text-[#0b1230] border border-[#cbd5e1] hover:bg-[#f4f7fb] active:bg-[#e8eef7]",
  ghost:
    "bg-transparent text-slate-700 border border-transparent hover:bg-slate-100 active:bg-slate-200/70",
  danger:
    "bg-[#e21d2d] text-white border border-transparent hover:bg-[#c51624] active:bg-[#a6111d]",
};

const BUTTON_SIZES: Record<ButtonSize, string> = {
  sm: "h-8 px-3 text-xs gap-1.5 rounded-md",
  md: "h-9 px-3.5 text-sm gap-2 rounded-lg",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  disabled,
  className = "",
  children,
  type = "button",
  ...props
}: ButtonProps): React.ReactElement {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center font-medium transition-colors duration-150 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap ${BUTTON_VARIANTS[variant]} ${BUTTON_SIZES[size]} ${className}`}
      {...props}
    >
      {loading && <Loader2 className="w-3.5 h-3.5 animate-spin shrink-0" />}
      {children}
    </button>
  );
}

const CONTROL_BASE =
  "h-9 rounded-lg border border-[#cbd5e1] bg-white px-3 text-sm text-[#0b1230] transition-colors duration-150 focus:outline-none focus:border-[#1e5bff] focus:ring-2 focus:ring-[#1e5bff]/20 disabled:bg-slate-100 disabled:text-slate-500";

export function Input({ className = "", ...props }: React.InputHTMLAttributes<HTMLInputElement>): React.ReactElement {
  const widthClass = /\bw-\S+/.test(className) ? "" : "w-full";
  return <input className={`${widthClass} ${CONTROL_BASE} ${className}`.trim()} {...props} />;
}

export function Select({
  className = "",
  children,
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement>): React.ReactElement {
  const widthClass = /\bw-\S+/.test(className) ? "" : "w-full";
  return (
    <select className={`${widthClass} ${CONTROL_BASE} ${className}`.trim()} {...props}>
      {children}
    </select>
  );
}

export function Field({
  label,
  hint,
  className = "",
  children,
}: {
  label: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <label className={`block space-y-1 ${className}`}>
      <span className="block text-xs font-medium text-slate-700">{label}</span>
      {children}
      {hint && <span className="block text-[11px] text-slate-500">{hint}</span>}
    </label>
  );
}

export function TransitStripe({ className = "" }: { className?: string }): React.ReactElement {
  return (
    <div className={`grid grid-cols-4 h-[3px] w-full overflow-hidden ${className}`} aria-hidden="true">
      <span className="bg-[#e21d2d]" />
      <span className="bg-[#1e5bff]" />
      <span className="bg-[#d97706]" />
      <span className="bg-[#16a34a]" />
    </div>
  );
}

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: React.ReactNode;
}): React.ReactElement {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 border-b border-[#dbe3ee] pb-4">
      <div className="space-y-1">
        <h1 className="font-display text-2xl font-bold tracking-tight text-[#0b1230]">{title}</h1>
        {description && <p className="text-sm text-slate-600">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Panel({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}): React.ReactElement {
  return (
    <div className={`bg-white rounded-lg border border-[#dbe3ee] ${className}`}>
      {children}
    </div>
  );
}

interface StatusStyle {
  badge: string;
  dot: string;
}

const STATUS_STYLES: Record<string, StatusStyle> = {
  REGULAR: { badge: "bg-emerald-50 text-emerald-800 border-emerald-200", dot: "bg-[#16a34a]" },
  ACTIVE: { badge: "bg-emerald-50 text-emerald-800 border-emerald-200", dot: "bg-[#16a34a]" },
  APPROVED: { badge: "bg-emerald-50 text-emerald-800 border-emerald-200", dot: "bg-[#16a34a]" },
  PAID: { badge: "bg-emerald-50 text-emerald-900 border-emerald-300", dot: "bg-[#16a34a]" },
  COMPUTED: { badge: "bg-[#eef4ff] text-[#1746cc] border-[#bfdbfe]", dot: "bg-[#1e5bff]" },
  OPEN: { badge: "bg-[#eef4ff] text-[#1746cc] border-[#bfdbfe]", dot: "bg-[#1e5bff]" },
  SUPERVISOR_RECOMMENDED: { badge: "bg-[#eef4ff] text-[#1746cc] border-[#bfdbfe]", dot: "bg-[#1e5bff]" },
  HR_VERIFIED: { badge: "bg-cyan-50 text-cyan-800 border-cyan-200", dot: "bg-cyan-600" },
  PROBATIONARY: { badge: "bg-amber-50 text-amber-800 border-amber-200", dot: "bg-[#d97706]" },
  PENDING: { badge: "bg-amber-50 text-amber-800 border-amber-200", dot: "bg-[#d97706]" },
  DRAFT: { badge: "bg-slate-100 text-slate-700 border-slate-200", dot: "bg-slate-400" },
  CLOSED: { badge: "bg-slate-100 text-slate-600 border-slate-200", dot: "bg-slate-400" },
  SEPARATED: { badge: "bg-red-50 text-red-800 border-red-200", dot: "bg-[#e21d2d]" },
  REJECTED: { badge: "bg-red-50 text-red-800 border-red-200", dot: "bg-[#e21d2d]" },
  CANCELLED: { badge: "bg-slate-100 text-slate-600 border-slate-200", dot: "bg-slate-400" },
};

export function StatusBadge({ status }: { status: string }): React.ReactElement {
  const key = status.toUpperCase();
  const style = STATUS_STYLES[key] ?? {
    badge: "bg-slate-100 text-slate-700 border-slate-200",
    dot: "bg-slate-400",
  };
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[11px] font-semibold tracking-wide uppercase ${style.badge}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${style.dot}`} aria-hidden="true" />
      {status}
    </span>
  );
}

const PIPELINE_STATIONS = [
  { key: "DRAFT", code: "01", label: "Draft", detail: "Period & inputs" },
  { key: "COMPUTED", code: "02", label: "Computed", detail: "Gross & statutory" },
  { key: "APPROVED", code: "03", label: "Approved", detail: "Locked for release" },
  { key: "PAID", code: "04", label: "Paid", detail: "Disbursed & closed" },
] as const;

export function PayrollPipeline({
  status,
  periodLabel,
  meta,
}: {
  status: string;
  periodLabel?: string;
  meta?: string;
}): React.ReactElement {
  const normalized = status.toUpperCase();
  const activeIndex = PIPELINE_STATIONS.findIndex((s) => s.key === normalized);
  const resolvedIndex = activeIndex === -1 ? 0 : activeIndex;

  return (
    <div className="rounded-lg bg-[#0f1c30] border border-[#1e3252] text-white overflow-hidden">
      <TransitStripe />
      <div className="px-4 py-3.5 sm:px-5 flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <span className="font-display text-xs font-bold uppercase tracking-[0.14em] text-slate-300">
              Payroll Dispatch Line
            </span>
            {periodLabel && (
              <span className="text-xs font-medium text-white bg-[#152544] border border-[#1e3252] px-2 py-0.5 rounded">
                {periodLabel}
              </span>
            )}
          </div>
          {meta && <span className="text-xs text-slate-300 num">{meta}</span>}
        </div>

        <ol className="grid grid-cols-2 lg:grid-cols-4 gap-3" aria-label="Payroll run stages">
          {PIPELINE_STATIONS.map((station, idx) => {
            const isDone = idx < resolvedIndex || (normalized === "PAID" && idx === 3);
            const isCurrent = idx === resolvedIndex && normalized !== "PAID";
            const dotClass = isDone
              ? "bg-[#16a34a] border-[#16a34a] text-white"
              : isCurrent
                ? "bg-[#1e5bff] border-[#60a5fa] text-white ring-4 ring-[#1e5bff]/25"
                : "bg-[#0b1230] border-[#334e78] text-slate-400";
            const trackClass = idx < resolvedIndex ? "bg-[#16a34a]" : "bg-[#1e3252]";

            return (
              <li key={station.key} className="relative flex items-start gap-2.5">
                <div className="flex flex-col items-center shrink-0 pt-0.5">
                  <span
                    className={`w-6 h-6 rounded-full border-2 flex items-center justify-center text-[10px] font-bold num ${dotClass}`}
                  >
                    {station.code}
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span
                      className={`font-display text-sm font-semibold tracking-tight ${
                        isCurrent || isDone ? "text-white" : "text-slate-400"
                      }`}
                    >
                      {station.label}
                    </span>
                    {idx < PIPELINE_STATIONS.length - 1 && (
                      <span className={`hidden lg:block h-[2px] flex-1 ${trackClass}`} aria-hidden="true" />
                    )}
                  </div>
                  <p className="text-[11px] text-slate-400 truncate">{station.detail}</p>
                </div>
              </li>
            );
          })}
        </ol>
      </div>
    </div>
  );
}

export function Alert({
  variant = "error",
  children,
}: {
  variant?: "error" | "info" | "success";
  children: React.ReactNode;
}): React.ReactElement {
  const styles = {
    error: "bg-red-50 border-red-200 text-red-900",
    info: "bg-[#eef4ff] border-[#bfdbfe] text-[#0b1230]",
    success: "bg-emerald-50 border-emerald-200 text-emerald-900",
  }[variant];
  const Icon = variant === "error" ? AlertCircle : variant === "success" ? CheckCircle2 : Info;
  return (
    <div role="alert" className={`flex items-start gap-2.5 rounded-lg border px-3.5 py-2.5 text-sm ${styles}`}>
      <Icon className="w-4 h-4 shrink-0 mt-0.5" />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}

export function TableSkeleton({ rows = 5, cols = 5 }: { rows?: number; cols?: number }): React.ReactElement {
  return (
    <>
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r} className="animate-pulse">
          {Array.from({ length: cols }).map((__, c) => (
            <td key={c} className="px-4 py-3">
              <div className="h-3.5 bg-slate-200/80 rounded w-3/4" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
  action?: React.ReactNode;
}): React.ReactElement {
  return (
    <div className="py-12 px-6 text-center flex flex-col items-center gap-2.5">
      <div className="w-10 h-10 rounded-lg bg-[#eef4ff] border border-[#bfdbfe] text-[#1e5bff] flex items-center justify-center">
        <Icon className="w-5 h-5" />
      </div>
      <div className="text-sm font-semibold text-[#0b1230]">{title}</div>
      <p className="text-xs text-slate-600 max-w-sm">{description}</p>
      {action && <div className="pt-2">{action}</div>}
    </div>
  );
}
