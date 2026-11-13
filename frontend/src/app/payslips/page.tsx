"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch, getAccessToken } from "@/lib/api";
import { formatDate, formatPhp } from "@/lib/format";

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
  const payslips = useQuery({
    queryKey: ["my-payslips"],
    queryFn: () => apiFetch<Payslip[]>("/me/payslips"),
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
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My Payslips</h1>
          <p className="text-sm text-slate-500">View and download your payslips</p>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Generated</th>
                <th className="px-4 py-3">Basic Pay</th>
                <th className="px-4 py-3">Gross Pay</th>
                <th className="px-4 py-3">Deductions</th>
                <th className="px-4 py-3">Net Pay</th>
                <th className="px-4 py-3 text-right">PDF</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {payslips.data?.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-3">{formatDate(s.generatedAt)}</td>
                  <td className="px-4 py-3">{formatPhp(s.basicPay)}</td>
                  <td className="px-4 py-3">{formatPhp(s.grossPay)}</td>
                  <td className="px-4 py-3">{formatPhp(s.totalDeductions)}</td>
                  <td className="px-4 py-3 font-semibold">{formatPhp(s.netPay)}</td>
                  <td className="px-4 py-3 text-right">
                    <button onClick={() => downloadPdf(s.id, s.employeeNo)} className="text-indigo-600 hover:underline text-xs font-medium">
                      Download PDF
                    </button>
                  </td>
                </tr>
              ))}
              {payslips.data?.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">No payslips available</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
