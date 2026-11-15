"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, FileText } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch, getAccessToken } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatDate, formatPhp } from "@/lib/format";
import { Button, EmptyState, PageHeader, Panel, TableSkeleton } from "@/components/ui";

interface Payslip {
  id: string;
  employeeNo: string;
  employeeName: string;
  basicPay: number;
  grossPay: number;
  totalDeductions: number;
  netPay: number;
  generatedAt: string;
}

export default function PayslipsPage() {
  const { user } = useAuth();
  const payslips = useQuery({
    queryKey: ["my-payslips"],
    queryFn: () => apiFetch<Payslip[]>("/me/payslips"),
    enabled: Boolean(user),
  });

  async function downloadPdf(id: string, employeeNo: string) {
    const token = getAccessToken();
    const res = await fetch(`/api/v1/payslips/${id}/pdf`, {
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
        <PageHeader title="My Payslips" description="View and download your released payslips" />

        <Panel className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                <tr>
                  <th className="px-4 py-2.5">Generated</th>
                  <th className="px-4 py-2.5 text-right">Basic Pay</th>
                  <th className="px-4 py-2.5 text-right">Gross Pay</th>
                  <th className="px-4 py-2.5 text-right">Deductions</th>
                  <th className="px-4 py-2.5 text-right">Net Pay</th>
                  <th className="px-4 py-2.5 text-right">PDF</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dbe3ee]">
                {payslips.isLoading && <TableSkeleton rows={4} cols={6} />}
                {payslips.data?.map((s: Payslip) => (
                  <tr key={s.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3 text-slate-700">{formatDate(s.generatedAt)}</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.basicPay)}</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.grossPay)}</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{formatPhp(s.totalDeductions)}</td>
                    <td className="px-4 py-3 num text-right font-semibold text-[#0b1230]">{formatPhp(s.netPay)}</td>
                    <td className="px-4 py-3 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => downloadPdf(s.id, s.employeeNo)}
                        className="text-[#1e5bff]"
                      >
                        <Download className="w-3.5 h-3.5" />
                        Download PDF
                      </Button>
                    </td>
                  </tr>
                ))}
                {!payslips.isLoading && payslips.data?.length === 0 && (
                  <tr>
                    <td colSpan={6}>
                      <EmptyState
                        icon={FileText}
                        title="No payslips available"
                        description="Payslips appear here as soon as a payroll run is approved or paid."
                      />
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>
    </AppShell>
  );
}
