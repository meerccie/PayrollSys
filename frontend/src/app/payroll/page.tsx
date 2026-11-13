"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch, getAccessToken } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatPhp } from "@/lib/format";

interface PayPeriod {
  id: string;
  startDate: string;
  endDate: string;
  payDate: string;
  frequency: string;
  cutoff: string;
  year: number;
  periodNumber: number;
  closed: boolean;
}

interface PayrollRun {
  id: string;
  payPeriodId: string;
  status: "DRAFT" | "COMPUTED" | "APPROVED" | "PAID";
  employeeCount: number;
  totalGrossPay: number;
  totalDeductions: number;
  totalNetPay: number;
  totalEmployerContributions: number;
}

interface Payslip {
  id: string;
  employeeNo: string;
  employeeName: string;
  grossPay: number;
  sssEmployee: number;
  philHealthEmployee: number;
  pagIbigEmployee: number;
  withholdingTax: number;
  otherDeductions: number;
  totalDeductions: number;
  netPay: number;
  hasNetPayWarning: boolean;
}

export default function PayrollPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [year, setYear] = useState(2026);
  const [selectedPeriodId, setSelectedPeriodId] = useState("");
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);

  const periods = useQuery({ queryKey: ["pay-periods"], queryFn: () => apiFetch<PayPeriod[]>("/pay-periods") });
  const runs = useQuery({ queryKey: ["payroll-runs"], queryFn: () => apiFetch<PayrollRun[]>("/payroll-runs") });
  const payslips = useQuery({
    queryKey: ["run-payslips", selectedRunId],
    queryFn: () => apiFetch<Payslip[]>(`/payroll-runs/${selectedRunId}/payslips`),
    enabled: Boolean(selectedRunId),
  });

  const generatePeriods = useMutation({
    mutationFn: () => apiFetch(`/pay-periods/generate?year=${year}&frequency=SEMI_MONTHLY`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pay-periods"] }),
  });

  const createRun = useMutation({
    mutationFn: () => apiFetch<PayrollRun>("/payroll-runs", { method: "POST", body: JSON.stringify({ payPeriodId: selectedPeriodId }) }),
    onSuccess: (run) => {
      qc.invalidateQueries({ queryKey: ["payroll-runs"] });
      setSelectedRunId(run.id);
    },
  });

  const runAction = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "compute" | "reset" | "approve" | "reopen" | "mark-paid" }) =>
      apiFetch<PayrollRun>(`/payroll-runs/${id}/${action}`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["payroll-runs"] });
      qc.invalidateQueries({ queryKey: ["run-payslips"] });
      qc.invalidateQueries({ queryKey: ["pay-periods"] });
    },
  });

  async function downloadPdf(payslipId: string, employeeNo: string) {
    const token = getAccessToken();
    const res = await fetch(`/api/v1/payslips/${payslipId}/pdf`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      credentials: "include",
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `payslip-${employeeNo}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="text-2xl font-bold text-slate-900">Payroll Runs</h1>
          <div className="flex gap-2 items-center">
            <input
              type="number"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
              className="w-24 rounded-lg border border-slate-300 px-3 py-1.5 text-sm"
            />
            <button
              onClick={() => generatePeriods.mutate()}
              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium hover:bg-slate-50"
            >
              Generate {year} Periods
            </button>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-5 flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-[240px]">
            <label className="block text-xs font-medium text-slate-600 mb-1">Create Payroll Run for Period</label>
            <select
              value={selectedPeriodId}
              onChange={(e) => setSelectedPeriodId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="">Select pay period...</option>
              {periods.data?.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.year} #{p.periodNumber} ({p.startDate} to {p.endDate}) {p.closed ? "[CLOSED]" : ""}
                </option>
              ))}
            </select>
          </div>
          <button
            disabled={!selectedPeriodId || createRun.isPending}
            onClick={() => createRun.mutate()}
            className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
          >
            Create Draft Run
          </button>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Run</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Employees</th>
                <th className="px-4 py-3">Gross Pay</th>
                <th className="px-4 py-3">Deductions</th>
                <th className="px-4 py-3">Net Pay</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {runs.data?.map((run) => {
                const p = periods.data?.find((pp) => pp.id === run.payPeriodId);
                return (
                  <tr key={run.id} className={selectedRunId === run.id ? "bg-indigo-50/40" : ""}>
                    <td className="px-4 py-3 font-medium">
                      <button onClick={() => setSelectedRunId(run.id)} className="hover:underline text-left">
                        {p ? `${p.startDate} to ${p.endDate}` : run.id.slice(0, 8)}
                      </button>
                    </td>
                    <td className="px-4 py-3">{run.status}</td>
                    <td className="px-4 py-3">{run.employeeCount}</td>
                    <td className="px-4 py-3">{formatPhp(run.totalGrossPay)}</td>
                    <td className="px-4 py-3">{formatPhp(run.totalDeductions)}</td>
                    <td className="px-4 py-3 font-semibold">{formatPhp(run.totalNetPay)}</td>
                    <td className="px-4 py-3 text-right space-x-2">
                      {(run.status === "DRAFT" || run.status === "COMPUTED") && (
                        <button onClick={() => runAction.mutate({ id: run.id, action: "compute" })} className="text-indigo-600 hover:underline text-xs">
                          {run.status === "DRAFT" ? "Compute" : "Recompute"}
                        </button>
                      )}
                      {run.status === "COMPUTED" && user?.role === "ADMIN" && (
                        <button onClick={() => runAction.mutate({ id: run.id, action: "approve" })} className="text-emerald-600 hover:underline text-xs">
                          Approve
                        </button>
                      )}
                      {run.status === "APPROVED" && (
                        <button onClick={() => runAction.mutate({ id: run.id, action: "mark-paid" })} className="text-emerald-700 hover:underline text-xs">
                          Mark Paid
                        </button>
                      )}
                      <button onClick={() => setSelectedRunId(run.id)} className="text-slate-600 hover:underline text-xs">
                        Payslips
                      </button>
                    </td>
                  </tr>
                );
              })}
              {runs.data?.length === 0 && (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400">No payroll runs yet</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {selectedRunId && (
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            <div className="px-4 py-3 border-b border-slate-200 font-semibold text-slate-900">
              Payslips in Selected Run
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
                  <tr>
                    <th className="px-4 py-3">Emp No</th>
                    <th className="px-4 py-3">Employee</th>
                    <th className="px-4 py-3">Gross</th>
                    <th className="px-4 py-3">SSS</th>
                    <th className="px-4 py-3">PhilHealth</th>
                    <th className="px-4 py-3">Pag-IBIG</th>
                    <th className="px-4 py-3">Tax</th>
                    <th className="px-4 py-3">Net Pay</th>
                    <th className="px-4 py-3 text-right">PDF</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {payslips.data?.map((s) => (
                    <tr key={s.id}>
                      <td className="px-4 py-3 font-mono text-xs">{s.employeeNo}</td>
                      <td className="px-4 py-3 font-medium">{s.employeeName}</td>
                      <td className="px-4 py-3">{formatPhp(s.grossPay)}</td>
                      <td className="px-4 py-3">{formatPhp(s.sssEmployee)}</td>
                      <td className="px-4 py-3">{formatPhp(s.philHealthEmployee)}</td>
                      <td className="px-4 py-3">{formatPhp(s.pagIbigEmployee)}</td>
                      <td className="px-4 py-3">{formatPhp(s.withholdingTax)}</td>
                      <td className="px-4 py-3 font-semibold">{formatPhp(s.netPay)}</td>
                      <td className="px-4 py-3 text-right">
                        <button onClick={() => downloadPdf(s.id, s.employeeNo)} className="text-indigo-600 hover:underline text-xs">
                          PDF
                        </button>
                      </td>
                    </tr>
                  ))}
                  {payslips.data?.length === 0 && (
                    <tr><td colSpan={9} className="px-4 py-6 text-center text-slate-400">Click Compute to generate payslips</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  );
}
