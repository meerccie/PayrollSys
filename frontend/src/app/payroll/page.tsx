"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, Plus, Wallet } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch, getAccessToken } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatPhp } from "@/lib/format";
import {
  Button,
  EmptyState,
  Field,
  Input,
  PageHeader,
  Panel,
  PayrollPipeline,
  Select,
  StatusBadge,
  TableSkeleton,
} from "@/components/ui";

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

export default function PayrollPage(): React.ReactElement {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [year, setYear] = useState(2026);
  const [selectedPeriodId, setSelectedPeriodId] = useState("");
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);

  const periods = useQuery({
    queryKey: ["pay-periods"],
    queryFn: () => apiFetch<PayPeriod[]>("/pay-periods"),
    enabled: Boolean(user),
  });
  const runs = useQuery({
    queryKey: ["payroll-runs"],
    queryFn: () => apiFetch<PayrollRun[]>("/payroll-runs"),
    enabled: Boolean(user),
  });
  const payslips = useQuery({
    queryKey: ["run-payslips", selectedRunId],
    queryFn: () => apiFetch<Payslip[]>(`/payroll-runs/${selectedRunId}/payslips`),
    enabled: Boolean(user) && Boolean(selectedRunId),
  });

  const generatePeriods = useMutation({
    mutationFn: () => apiFetch(`/pay-periods/generate?year=${year}&frequency=SEMI_MONTHLY`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pay-periods"] }),
  });

  const createRun = useMutation({
    mutationFn: () =>
      apiFetch<PayrollRun>("/payroll-runs", {
        method: "POST",
        body: JSON.stringify({ payPeriodId: selectedPeriodId }),
      }),
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

  async function downloadPdf(payslipId: string, employeeNo: string): Promise<void> {
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

  const activeRun = runs.data?.find((r) => r.id === selectedRunId) ?? runs.data?.[0];
  const activePeriod = activeRun ? periods.data?.find((pp) => pp.id === activeRun.payPeriodId) : undefined;

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Payroll Runs"
          description="Generate pay periods, compute statutory deductions, and approve payouts"
          actions={
            <div className="flex items-center gap-2">
              <Input
                type="number"
                aria-label="Payroll year"
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
                className="w-24 num"
              />
              <Button
                variant="secondary"
                loading={generatePeriods.isPending}
                onClick={() => generatePeriods.mutate()}
              >
                Generate {year} Periods
              </Button>
            </div>
          }
        />

        <PayrollPipeline
          status={activeRun?.status ?? "DRAFT"}
          periodLabel={activePeriod ? `${activePeriod.startDate} to ${activePeriod.endDate}` : "No active run"}
          meta={
            activeRun
              ? `${activeRun.employeeCount} employees · Net ${formatPhp(activeRun.totalNetPay)}`
              : undefined
          }
        />

        <Panel className="p-4 sm:p-5 flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-[240px]">
            <Field label="Create Payroll Run for Period">
              <Select
                value={selectedPeriodId}
                onChange={(e) => setSelectedPeriodId(e.target.value)}
              >
                <option value="">Select pay period...</option>
                {periods.data?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.year} #{p.periodNumber} ({p.startDate} to {p.endDate}) {p.closed ? "[CLOSED]" : ""}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
          <Button
            disabled={!selectedPeriodId}
            loading={createRun.isPending}
            onClick={() => createRun.mutate()}
          >
            <Plus className="w-4 h-4" />
            Create Draft Run
          </Button>
        </Panel>

        <Panel className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                <tr>
                  <th className="px-4 py-2.5">Period</th>
                  <th className="px-4 py-2.5">Status</th>
                  <th className="px-4 py-2.5 text-right">Employees</th>
                  <th className="px-4 py-2.5 text-right">Gross Pay</th>
                  <th className="px-4 py-2.5 text-right">Deductions</th>
                  <th className="px-4 py-2.5 text-right">Net Pay</th>
                  <th className="px-4 py-2.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dbe3ee]">
                {runs.isLoading && <TableSkeleton rows={4} cols={7} />}
                {runs.data?.map((run) => {
                  const p = periods.data?.find((pp) => pp.id === run.payPeriodId);
                  const selected = selectedRunId === run.id;
                  return (
                    <tr
                      key={run.id}
                      className={`transition-colors ${
                        selected ? "bg-[#eef4ff]" : "hover:bg-[#f4f7fb]"
                      }`}
                    >
                      <td className="px-4 py-3 font-medium">
                        <button
                          type="button"
                          onClick={() => setSelectedRunId(run.id)}
                          className="text-left text-[#0b1230] hover:text-[#1e5bff] hover:underline cursor-pointer num"
                        >
                          {p ? `${p.startDate} to ${p.endDate}` : run.id.slice(0, 8)}
                        </button>
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={run.status} />
                      </td>
                      <td className="px-4 py-3 num text-right text-slate-700">{run.employeeCount}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(run.totalGrossPay)}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(run.totalDeductions)}</td>
                      <td className="px-4 py-3 num text-right font-semibold text-[#0b1230]">
                        {formatPhp(run.totalNetPay)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          {(run.status === "DRAFT" || run.status === "COMPUTED") && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => runAction.mutate({ id: run.id, action: "compute" })}
                              disabled={runAction.isPending}
                              className="text-[#1e5bff] hover:bg-[#eef4ff]"
                            >
                              {run.status === "DRAFT" ? "Compute" : "Recompute"}
                            </Button>
                          )}
                          {run.status === "COMPUTED" && user?.role === "ADMIN" && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => runAction.mutate({ id: run.id, action: "approve" })}
                              disabled={runAction.isPending}
                              className="text-[#16a34a] hover:bg-emerald-50"
                            >
                              Approve
                            </Button>
                          )}
                          {run.status === "APPROVED" && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => runAction.mutate({ id: run.id, action: "mark-paid" })}
                              disabled={runAction.isPending}
                              className="text-[#16a34a] hover:bg-emerald-50"
                            >
                              Mark Paid
                            </Button>
                          )}
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => setSelectedRunId(run.id)}
                          >
                            Payslips
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {!runs.isLoading && runs.data?.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <EmptyState
                        icon={Wallet}
                        title="No payroll runs yet"
                        description="Generate periods for the year above, select a pay period, and create a draft run."
                      />
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>

        {selectedRunId && (
          <Panel className="overflow-hidden">
            <div className="px-4 py-3 border-b border-[#dbe3ee] flex items-center justify-between">
              <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                Payslips in Selected Run
              </h2>
              <span className="text-xs text-slate-600 num">
                {payslips.data ? `${payslips.data.length} payslips` : ""}
              </span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                  <tr>
                    <th className="px-4 py-2.5">Emp No</th>
                    <th className="px-4 py-2.5">Employee</th>
                    <th className="px-4 py-2.5 text-right">Gross</th>
                    <th className="px-4 py-2.5 text-right">SSS</th>
                    <th className="px-4 py-2.5 text-right">PhilHealth</th>
                    <th className="px-4 py-2.5 text-right">Pag-IBIG</th>
                    <th className="px-4 py-2.5 text-right">Tax</th>
                    <th className="px-4 py-2.5 text-right">Net Pay</th>
                    <th className="px-4 py-2.5 text-right">PDF</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#dbe3ee]">
                  {payslips.isLoading && <TableSkeleton rows={4} cols={9} />}
                  {payslips.data?.map((s) => (
                    <tr
                      key={s.id}
                      className={`transition-colors ${
                        s.hasNetPayWarning ? "bg-amber-50/70 hover:bg-amber-50" : "hover:bg-[#f4f7fb]"
                      }`}
                    >
                      <td className="px-4 py-3 font-mono text-xs text-slate-700">{s.employeeNo}</td>
                      <td className="px-4 py-3 font-medium text-[#0b1230]">{s.employeeName}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.grossPay)}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.sssEmployee)}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.philHealthEmployee)}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.pagIbigEmployee)}</td>
                      <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.withholdingTax)}</td>
                      <td className="px-4 py-3 num text-right font-semibold text-[#0b1230]">
                        {formatPhp(s.netPay)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => downloadPdf(s.id, s.employeeNo)}
                          className="text-[#1e5bff]"
                        >
                          <Download className="w-3.5 h-3.5" />
                          PDF
                        </Button>
                      </td>
                    </tr>
                  ))}
                  {!payslips.isLoading && payslips.data?.length === 0 && (
                    <tr>
                      <td colSpan={9} className="px-4 py-8 text-center text-sm text-slate-500">
                        No payslips generated yet. Click Compute on the selected run above.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </Panel>
        )}
      </div>
    </AppShell>
  );
}
