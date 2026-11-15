"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Building2,
  CalendarClock,
  CalendarDays,
  CreditCard,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  Users,
  Wallet,
  X,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { Role } from "@/lib/api";
import { TransitStripe } from "@/components/ui";

interface NavItem {
  href: string;
  label: string;
  code: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", code: "01", icon: LayoutDashboard, roles: ["ADMIN", "HR", "SUPERVISOR", "EMPLOYEE"] },
  { href: "/employees", label: "Employees", code: "02", icon: Users, roles: ["ADMIN", "HR"] },
  { href: "/departments", label: "Org & Positions", code: "03", icon: Building2, roles: ["ADMIN", "HR"] },
  { href: "/attendance", label: "Attendance & OT", code: "04", icon: CalendarClock, roles: ["ADMIN", "HR", "SUPERVISOR", "EMPLOYEE"] },
  { href: "/leave", label: "Leave", code: "05", icon: CalendarDays, roles: ["ADMIN", "HR", "SUPERVISOR", "EMPLOYEE"] },
  { href: "/compensation", label: "Earnings & Deductions", code: "06", icon: CreditCard, roles: ["ADMIN", "HR"] },
  { href: "/payroll", label: "Payroll Runs", code: "07", icon: Wallet, roles: ["ADMIN", "HR"] },
  { href: "/payslips", label: "Payslips", code: "08", icon: FileText, roles: ["ADMIN", "HR", "SUPERVISOR", "EMPLOYEE"] },
  { href: "/settings", label: "Settings", code: "09", icon: Settings, roles: ["ADMIN", "HR"] },
];

export function AppShell({ children }: { children: React.ReactNode }): React.ReactElement {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  if (loading || !user) {
    return (
      <div className="min-h-screen flex bg-[#f4f7fb]">
        <div className="hidden lg:block w-64 bg-[#0b1230] shrink-0 p-5 space-y-6 border-r border-[#1e3252]">
          <div className="h-6 w-32 rounded bg-[#152544] animate-pulse" />
          <div className="space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-9 rounded-md bg-[#152544]/80 animate-pulse" />
            ))}
          </div>
        </div>
        <div className="flex-1 p-6 lg:p-8 space-y-6">
          <div className="h-8 w-48 rounded bg-slate-200 animate-pulse" />
          <div className="h-40 rounded-lg bg-white border border-[#dbe3ee] animate-pulse" />
        </div>
      </div>
    );
  }

  const visibleNav = NAV_ITEMS.filter((item) => item.roles.includes(user.role));

  async function handleLogout(): Promise<void> {
    await logout();
    router.replace("/login");
  }

  const sidebarContent = (
    <div className="flex flex-col h-full bg-[#0b1230] text-slate-100 border-r border-[#1e3252]">
      <TransitStripe />
      <div className="px-5 h-16 flex items-center justify-between border-b border-[#1e3252]">
        <Link href="/dashboard" className="flex items-center gap-2.5">
          <span className="w-7 h-7 rounded bg-[#1e5bff] text-white font-display text-xs font-bold flex items-center justify-center tracking-tight">
            PH
          </span>
          <div className="leading-none">
            <span className="block font-display text-base font-bold tracking-tight text-white">PAYROLL</span>
            <span className="block text-[10px] font-medium uppercase tracking-[0.14em] text-slate-400">
              PH 2026 Dispatch
            </span>
          </div>
        </Link>
        <button
          type="button"
          onClick={() => setMobileOpen(false)}
          aria-label="Close navigation menu"
          className="lg:hidden p-1.5 rounded-md text-slate-400 hover:text-white hover:bg-[#152544]"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <nav aria-label="Primary" className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {visibleNav.map((item) => {
          const Icon = item.icon;
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={`group flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-150 ${
                active
                  ? "bg-[#1e5bff] text-white"
                  : "text-slate-300 hover:bg-[#101c3a] hover:text-white"
              }`}
            >
              <span
                className={`text-[10px] font-bold num w-4 shrink-0 ${
                  active ? "text-blue-100" : "text-slate-500 group-hover:text-slate-400"
                }`}
              >
                {item.code}
              </span>
              <Icon className="w-4 h-4 shrink-0" />
              <span className="truncate">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="p-3.5 border-t border-[#1e3252] bg-[#091029] flex items-center justify-between gap-2">
        <div className="min-w-0">
          <div className="text-xs font-medium text-slate-100 truncate">{user.email}</div>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[#16a34a]" aria-hidden="true" />
            <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">{user.role}</span>
          </div>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          title="Sign out"
          aria-label="Sign out"
          className="p-2 rounded-md text-slate-400 hover:text-white hover:bg-[#152544] transition-colors cursor-pointer"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-[#f4f7fb]">
      {/* Mobile top bar */}
      <header className="lg:hidden bg-[#0b1230] text-white border-b border-[#1e3252] sticky top-0 z-30">
        <TransitStripe />
        <div className="h-14 px-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span className="w-6 h-6 rounded bg-[#1e5bff] text-white font-display text-[11px] font-bold flex items-center justify-center">
              PH
            </span>
            <span className="font-display text-sm font-bold tracking-tight">PAYROLL</span>
          </div>
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            aria-label="Open navigation menu"
            className="p-2 rounded-md text-slate-300 hover:text-white hover:bg-[#152544]"
          >
            <Menu className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden flex">
          <div
            className="fixed inset-0 bg-[#0b1230]/70"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />
          <aside className="relative w-64 max-w-[80vw] h-full z-50">{sidebarContent}</aside>
        </div>
      )}

      {/* Desktop sticky sidebar */}
      <aside className="hidden lg:block w-64 shrink-0 sticky top-0 h-screen">
        {sidebarContent}
      </aside>

      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
        {children}
      </main>
    </div>
  );
}
