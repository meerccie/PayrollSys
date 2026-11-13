"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

interface TimeEntry {
  id: string;
  employeeId: string;
  workDate: string;
  regularHours: number;
  overtimeHours: number;
  nightDiffHours: number;
  lateMinutes: number;
  dayType: string;
  status: string;
}

interface Employee {
  id: string;
  employeeNo: string;
  fullName: string;
}

export default function AttendancePage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const isHrOrAdmin = user?.role === "ADMIN" || user?.role === "HR";
  const today = new Date().toISOString().slice(0, 10);
  const monthStart = today.slice(0, 8) + "01";
  const [from, setFrom] = useState(monthStart);
  const [to, setTo] = useState(today);
  const [employeeId, setEmployeeId] = useState("");
  const [workDate, setWorkDate] = useState(today);
  const [regularHours, setRegularHours] = useState("8");
  const [overtimeHours, setOvertimeHours] = useState("0");
  const [lateMinutes, setLateMinutes] = useState("0");

  const employees = useQuery({
    queryKey: ["employees-list"],
    queryFn: () => apiFetch<{ content: Employee[] }>("/employees?size=100"),
    enabled: isHrOrAdmin,
  });

  const entries = useQuery({
    queryKey: ["time-entries", employeeId, from, to],
    queryFn: () => {
      const params = new URLSearchParams({ from, to });
      if (employeeId) params.set("employeeId", employeeId);
      return apiFetch<TimeEntry[]>(`/time-entries?${params.toString()}`);
    },
  });

  const upsert = useMutation({
    mutationFn: () =>
      apiFetch("/time-entries", {
        method: "POST",
        body: JSON.stringify({
          employeeId,
          workDate,
          regularHours: Number(regularHours),
          overtimeHours: Number(overtimeHours),
          nightDiffHours: 0,
          lateMinutes: Number(lateMinutes),
          undertimeMinutes: 0,
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["time-entries"] }),
  });

  const approveAll = useMutation({
    mutationFn: (ids: string[]) =>
      apiFetch("/time-entries/approve", { method: "POST", body: JSON.stringify({ ids }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["time-entries"] }),
  });

  const pendingIds = entries.data?.filter((e) => e.status === "PENDING").map((e) => e.id) ?? [];

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="text-2xl font-bold text-slate-900">Attendance & Overtime</h1>
          {isHrOrAdmin && pendingIds.length > 0 && (
            <button
              onClick={() => approveAll.mutate(pendingIds)}
              className="rounded-lg bg-emerald-600 text-white px-4 py-2 text-sm font-medium hover:bg-emerald-700"
            >
              Approve Pending ({pendingIds.length})
            </button>
          )}
        </div>

        {isHrOrAdmin && (
          <form
            onSubmit={(e) => { e.preventDefault(); upsert.mutate(); }}
            className="bg-white rounded-xl border border-slate-200 p-5 grid grid-cols-1 md:grid-cols-6 gap-3 items-end"
          >
            <div className="md:col-span-2">
              <label className="block text-xs font-medium text-slate-600 mb-1">Employee</label>
              <select
                required
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Select employee...</option>
                {employees.data?.content.map((emp) => (
                  <option key={emp.id} value={emp.id}>{emp.employeeNo} - {emp.fullName}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Date</label>
              <input type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Reg Hours</label>
              <input type="number" step="0.5" value={regularHours} onChange={(e) => setRegularHours(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">OT Hours</label>
              <input type="number" step="0.5" value={overtimeHours} onChange={(e) => setOvertimeHours(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <button type="submit" className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700">
              Save Entry
            </button>
          </form>
        )}

        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex flex-wrap gap-3 items-center">
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
            <span className="text-slate-400 text-sm">to</span>
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
          </div>
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Day Type</th>
                <th className="px-4 py-3">Regular</th>
                <th className="px-4 py-3">Overtime</th>
                <th className="px-4 py-3">Late (min)</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {entries.data?.map((e) => (
                <tr key={e.id}>
                  <td className="px-4 py-3">{e.workDate}</td>
                  <td className="px-4 py-3">{e.dayType}</td>
                  <td className="px-4 py-3">{e.regularHours}h</td>
                  <td className="px-4 py-3">{e.overtimeHours}h</td>
                  <td className="px-4 py-3">{e.lateMinutes}m</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                      e.status === "APPROVED" ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"
                    }`}>
                      {e.status}
                    </span>
                  </td>
                </tr>
              ))}
              {entries.data?.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">No time entries</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
