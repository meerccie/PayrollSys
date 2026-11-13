"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatPhp } from "@/lib/format";

interface PageResult<T> {
  content: T[];
  totalElements: number;
}

interface PayrollRun {
  id: string;
  status: string;
  employeeCount: number;
  totalNetPay: number;
}

interface LeaveRequest {
  id: string;
  status: string;
}

interface Payslip {
  id: string;
  netPay: number;
  grossPay: number;
  generatedAt: string;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const isHrOrAdmin = user?.role === "ADMIN" || user?.role === "HR";

  const employeesQuery = useQuery({
    queryKey: ["employees-count"],
    queryFn: () => apiFetch<PageResult<unknown>>("/employees?size=1"),
    enabled: isHrOrAdmin,
  });

  const runsQuery = useQuery({
    queryKey: ["payroll-runs"],
    queryFn: () => apiFetch<PayrollRun[]>("/payroll-runs"),
    enabled: isHrOrAdmin,
  });

  const leavesQuery = useQuery({
    queryKey: ["leave-pending"],
    queryFn: () => apiFetch<LeaveRequest[]>("/leave-requests?status=PENDING"),
    enabled: isHrOrAdmin,
  });

  const myPayslipsQuery = useQuery({
    queryKey: ["my-payslips"],
    queryFn: () => apiFetch<Payslip[]>("/me/payslips"),
    enabled: user?.role === "EMPLOYEE",
  });

  const latestRun = runsQuery.data?.[0];

  return (
    <AppShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Welcome back, {user?.email}</p>
        </div>

        {isHrOrAdmin ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <StatCard label="Employees" value={String(employeesQuery.data?.totalElements ?? "-")} />
            <StatCard label="Pending Leave Requests" value={String(leavesQuery.data?.length ?? "-")} />
            <StatCard label="Latest Run Status" value={latestRun?.status ?? "None"} />
            <StatCard
              label="Latest Run Net Pay"
              value={latestRun ? formatPhp(latestRun.totalNetPay) : "-"}
            />
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <StatCard label="My Payslips" value={String(myPayslipsQuery.data?.length ?? 0)} />
            <StatCard
              label="Latest Net Pay"
              value={myPayslipsQuery.data?.[0] ? formatPhp(myPayslipsQuery.data[0].netPay) : "-"}
            />
          </div>
        )}
      </div>
    </AppShell>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
      <div className="text-xs font-medium text-slate-500 uppercase tracking-wider">{label}</div>
      <div className="text-2xl font-bold text-slate-900 mt-2">{value}</div>
    </div>
  );
}
