"use client";

import React from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, FileText, Wallet } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatDate, formatPhp } from "@/lib/format";
import {
  Button,
  EmptyState,
  PageHeader,
  Panel,
  PayrollPipeline,
  StatusBadge,
  TableSkeleton,
} from "@/components/ui";

interface PageResult<T> {
  content: T[];
  totalElements: number;
}

interface PayrollRun {
  id: string;
  status: string;
  employeeCount: number;
  totalGrossPay: number;
  totalNetPay: number;
}

interface LeaveRequest {
  id: string;
  status: string;
}

interface Payslip {
  id: string;
  basicPay: number;
  grossPay: number;
  totalDeductions: number;
  netPay: number;
  generatedAt: string;
}

export default function DashboardPage(): React.ReactElement {
  const { user } = useAuth();
  const isHrOrAdmin = user?.role === "ADMIN" || user?.role === "HR";

  const employeesQuery = useQuery({
    queryKey: ["employees-count"],
    queryFn: () => apiFetch<PageResult<unknown>>("/employees?activeOnly=true&size=1"),
    enabled: isHrOrAdmin,
  });

  const runsQuery = useQuery({
    queryKey: ["payroll-runs"],
    queryFn: () => apiFetch<PayrollRun[]>("/payroll-runs"),
    enabled: isHrOrAdmin,
  });

  const leavesQuery = useQuery({
    queryKey: ["leave-pending"],
    queryFn: () => apiFetch<LeaveRequest[]>("/leave-requests"),
    enabled: isHrOrAdmin || user?.role === "SUPERVISOR",
  });

  const myPayslipsQuery = useQuery({
    queryKey: ["my-payslips"],
    queryFn: () => apiFetch<Payslip[]>("/me/payslips"),
    enabled: user?.role === "EMPLOYEE" || user?.role === "SUPERVISOR",
  });

  const latestRun = runsQuery.data?.[0];
  const inFlightLeavesCount =
    leavesQuery.data?.filter(
      (l) => l.status === "PENDING" || l.status === "SUPERVISOR_RECOMMENDED" || l.status === "HR_VERIFIED"
    ).length ?? 0;

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Dashboard"
          description={`Signed in as ${user?.email ?? ""}`}
          actions={
            isHrOrAdmin ? (
              <Link href="/payroll">
                <Button size="sm">
                  Open Payroll Runs
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              </Link>
            ) : (
              <Link href="/payslips">
                <Button size="sm">
                  View My Payslips
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              </Link>
            )
          }
        />

        {isHrOrAdmin ? (
          <>
            <PayrollPipeline
              status={latestRun?.status ?? "DRAFT"}
              periodLabel={latestRun ? `Latest Run · ${latestRun.employeeCount} employees` : "No active run"}
              meta={latestRun ? `Net ${formatPhp(latestRun.totalNetPay)}` : undefined}
            />

            <Panel className="grid grid-cols-2 lg:grid-cols-4 divide-y lg:divide-y-0 lg:divide-x divide-[#dbe3ee]">
              <MetricCell
                label="Active Workforce"
                value={employeesQuery.isLoading ? "—" : String(employeesQuery.data?.totalElements ?? 0)}
                numeric
              />
              <MetricCell
                label="In-Flight Leave Requests"
                value={leavesQuery.isLoading ? "—" : String(inFlightLeavesCount)}
                numeric
              />
              <MetricCell
                label="Latest Run Status"
                value={runsQuery.isLoading ? "—" : latestRun?.status ?? "No runs"}
              />
              <MetricCell
                label="Latest Run Net Pay"
                value={runsQuery.isLoading ? "—" : latestRun ? formatPhp(latestRun.totalNetPay) : "—"}
                numeric
              />
            </Panel>

            <Panel className="overflow-hidden">
              <div className="px-4 py-3 border-b border-[#dbe3ee] flex items-center justify-between">
                <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                  Recent Payroll Runs
                </h2>
                <Link href="/payroll" className="text-xs font-semibold text-[#1e5bff] hover:underline">
                  Manage all runs
                </Link>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                    <tr>
                      <th className="px-4 py-2.5">Status</th>
                      <th className="px-4 py-2.5">Employees</th>
                      <th className="px-4 py-2.5 text-right">Total Gross</th>
                      <th className="px-4 py-2.5 text-right">Total Net Pay</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#dbe3ee]">
                    {runsQuery.isLoading && <TableSkeleton rows={3} cols={4} />}
                    {runsQuery.data?.slice(0, 5).map((run) => (
                      <tr key={run.id} className="hover:bg-[#f4f7fb] transition-colors">
                        <td className="px-4 py-3">
                          <StatusBadge status={run.status} />
                        </td>
                        <td className="px-4 py-3 num text-slate-700">{run.employeeCount}</td>
                        <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(run.totalGrossPay)}</td>
                        <td className="px-4 py-3 num text-right font-semibold text-[#0b1230]">
                          {formatPhp(run.totalNetPay)}
                        </td>
                      </tr>
                    ))}
                    {!runsQuery.isLoading && (runsQuery.data?.length ?? 0) === 0 && (
                      <tr>
                        <td colSpan={4}>
                          <EmptyState
                            icon={Wallet}
                            title="No payroll runs yet"
                            description="Generate pay periods and create your first payroll run to compute employee salaries."
                            action={
                              <Link href="/payroll">
                                <Button size="sm">Go to Payroll Runs</Button>
                              </Link>
                            }
                          />
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </Panel>
          </>
        ) : (
          <>
            <Panel
              className={`grid grid-cols-1 ${
                user?.role === "SUPERVISOR" ? "sm:grid-cols-3" : "sm:grid-cols-2"
              } divide-y sm:divide-y-0 sm:divide-x divide-[#dbe3ee]`}
            >
              <MetricCell
                label="Available Payslips"
                value={myPayslipsQuery.isLoading ? "—" : String(myPayslipsQuery.data?.length ?? 0)}
                numeric
              />
              <MetricCell
                label="Latest Net Pay"
                value={
                  myPayslipsQuery.isLoading
                    ? "—"
                    : myPayslipsQuery.data?.[0]
                    ? formatPhp(myPayslipsQuery.data[0].netPay)
                    : "—"
                }
                numeric
              />
              {user?.role === "SUPERVISOR" && (
                <MetricCell
                  label="Team Leave Requests"
                  value={leavesQuery.isLoading ? "—" : String(inFlightLeavesCount)}
                  numeric
                />
              )}
            </Panel>

            <Panel className="overflow-hidden">
              <div className="px-4 py-3 border-b border-[#dbe3ee] flex items-center justify-between">
                <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                  Recent Payslips
                </h2>
                <Link href="/payslips" className="text-xs font-semibold text-[#1e5bff] hover:underline">
                  View all payslips
                </Link>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                    <tr>
                      <th className="px-4 py-2.5">Generated</th>
                      <th className="px-4 py-2.5 text-right">Gross Pay</th>
                      <th className="px-4 py-2.5 text-right">Deductions</th>
                      <th className="px-4 py-2.5 text-right">Net Pay</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#dbe3ee]">
                    {myPayslipsQuery.isLoading && <TableSkeleton rows={3} cols={4} />}
                    {myPayslipsQuery.data?.slice(0, 5).map((slip) => (
                      <tr key={slip.id} className="hover:bg-[#f4f7fb] transition-colors">
                        <td className="px-4 py-3 text-slate-700">{formatDate(slip.generatedAt)}</td>
                        <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(slip.grossPay)}</td>
                        <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(slip.totalDeductions)}</td>
                        <td className="px-4 py-3 num text-right font-semibold text-[#0b1230]">
                          {formatPhp(slip.netPay)}
                        </td>
                      </tr>
                    ))}
                    {!myPayslipsQuery.isLoading && (myPayslipsQuery.data?.length ?? 0) === 0 && (
                      <tr>
                        <td colSpan={4}>
                          <EmptyState
                            icon={FileText}
                            title="No payslips released yet"
                            description="Once HR approves or pays a payroll run, your itemized payslips will appear here."
                          />
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </Panel>
          </>
        )}
      </div>
    </AppShell>
  );
}

function MetricCell({
  label,
  value,
  numeric = false,
}: {
  label: string;
  value: string;
  numeric?: boolean;
}): React.ReactElement {
  return (
    <div className="px-5 py-4">
      <div className="text-xs font-medium uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`font-display text-2xl font-bold text-[#0b1230] mt-1 ${numeric ? "num" : ""}`}>{value}</div>
    </div>
  );
}
