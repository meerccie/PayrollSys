"use client";

import React, { useEffect } from "react";
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
  Settings,
  Users,
  Wallet,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { Role } from "@/lib/api";

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["ADMIN", "HR", "EMPLOYEE"] },
  { href: "/employees", label: "Employees", icon: Users, roles: ["ADMIN", "HR"] },
  { href: "/departments", label: "Org & Positions", icon: Building2, roles: ["ADMIN", "HR"] },
  { href: "/attendance", label: "Attendance & OT", icon: CalendarClock, roles: ["ADMIN", "HR", "EMPLOYEE"] },
  { href: "/leave", label: "Leave", icon: CalendarDays, roles: ["ADMIN", "HR", "EMPLOYEE"] },
  { href: "/compensation", label: "Earnings & Deductions", icon: CreditCard, roles: ["ADMIN", "HR"] },
  { href: "/payroll", label: "Payroll Runs", icon: Wallet, roles: ["ADMIN", "HR"] },
  { href: "/payslips", label: "Payslips", icon: FileText, roles: ["ADMIN", "HR", "EMPLOYEE"] },
  { href: "/settings", label: "Settings", icon: Settings, roles: ["ADMIN", "HR"] },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-600 text-sm">
        Loading session...
      </div>
    );
  }

  const visibleNav = NAV_ITEMS.filter((item) => item.roles.includes(user.role));

  return (
    <div className="min-h-screen flex bg-slate-50">
      <aside className="w-64 bg-slate-900 text-slate-100 flex flex-col shrink-0">
        <div className="px-5 py-5 border-b border-slate-800">
          <div className="text-lg font-semibold tracking-tight">PH Payroll</div>
          <div className="text-xs text-slate-400">Clean Architecture</div>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {visibleNav.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(item.href + "/");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  active
                    ? "bg-indigo-600 text-white"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`}
              >
                <Icon className="w-4 h-4 shrink-0" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="p-4 border-t border-slate-800 flex items-center justify-between gap-2">
          <div className="min-w-0">
            <div className="text-xs font-medium truncate">{user.email}</div>
            <div className="text-[11px] text-slate-400">{user.role}</div>
          </div>
          <button
            onClick={async () => {
              await logout();
              router.replace("/login");
            }}
            title="Sign out"
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </aside>
      <main className="flex-1 min-w-0 p-6 md:p-8 overflow-y-auto">{children}</main>
    </div>
  );
}
