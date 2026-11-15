"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarClock, Check } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  Button,
  EmptyState,
  Field,
  Input,
  PageHeader,
  Panel,
  Select,
  StatusBadge,
  TableSkeleton,
} from "@/components/ui";

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
  leaveRequestId?: string | null;
  remarks?: string | null;
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
  const isSupervisorOrAbove = isHrOrAdmin || user?.role === "SUPERVISOR";
  const today = new Date().toISOString().slice(0, 10);
  const monthStart = today.slice(0, 8) + "01";
  const [from, setFrom] = useState(monthStart);
  const [to, setTo] = useState(today);
  const [employeeId, setEmployeeId] = useState("");
  const [workDate, setWorkDate] = useState(today);
  const [regularHours, setRegularHours] = useState("8");
  const [overtimeHours, setOvertimeHours] = useState("0");
  const [lateMinutes] = useState("0");

  const employees = useQuery({
    queryKey: ["employees-list"],
    queryFn: () => apiFetch<{ content: Employee[] }>("/employees?activeOnly=true&size=100"),
    enabled: Boolean(user) && isSupervisorOrAbove,
  });

  const entries = useQuery({
    queryKey: ["time-entries", employeeId, from, to],
    queryFn: () => {
      const params = new URLSearchParams({ from, to });
      if (employeeId) params.set("employeeId", employeeId);
      return apiFetch<TimeEntry[]>(`/time-entries?${params.toString()}`);
    },
    enabled: Boolean(user),
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

  const pendingIds = entries.data?.filter((e: TimeEntry) => e.status === "PENDING").map((e: TimeEntry) => e.id) ?? [];

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Attendance & Overtime"
          description="Log daily attendance hours, overtime, and approve pending entries"
          actions={
            isHrOrAdmin && pendingIds.length > 0 ? (
              <Button
                loading={approveAll.isPending}
                onClick={() => approveAll.mutate(pendingIds)}
              >
                <Check className="w-4 h-4" />
                Approve Pending ({pendingIds.length})
              </Button>
            ) : undefined
          }
        />

        {isHrOrAdmin && (
          <Panel className="p-4 sm:p-5">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                upsert.mutate();
              }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 items-end"
            >
              <div className="lg:col-span-2">
                <Field label="Employee">
                  <Select required value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
                    <option value="">Select employee...</option>
                    {employees.data?.content.map((emp: Employee) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.employeeNo} - {emp.fullName}
                      </option>
                    ))}
                  </Select>
                </Field>
              </div>
              <Field label="Date">
                <Input type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} />
              </Field>
              <Field label="Reg Hours">
                <Input
                  type="number"
                  step="0.5"
                  min="0"
                  value={regularHours}
                  onChange={(e) => setRegularHours(e.target.value)}
                  className="num"
                />
              </Field>
              <Field label="OT Hours">
                <Input
                  type="number"
                  step="0.5"
                  min="0"
                  value={overtimeHours}
                  onChange={(e) => setOvertimeHours(e.target.value)}
                  className="num"
                />
              </Field>
              <Button type="submit" loading={upsert.isPending} className="w-full">
                Save Entry
              </Button>
            </form>
          </Panel>
        )}

        <Panel className="overflow-hidden">
          <div className="p-4 border-b border-[#dbe3ee] flex flex-wrap gap-3 items-center">
            {!isHrOrAdmin && isSupervisorOrAbove && (
              <Select
                aria-label="Filter by team employee"
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                className="w-56"
              >
                <option value="">All team members</option>
                {employees.data?.content.map((emp: Employee) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.employeeNo} - {emp.fullName}
                  </option>
                ))}
              </Select>
            )}
            <Input
              type="date"
              aria-label="Filter from date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="w-auto"
            />
            <span className="text-slate-500 text-xs font-medium uppercase">to</span>
            <Input
              type="date"
              aria-label="Filter to date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="w-auto"
            />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                <tr>
                  <th className="px-4 py-2.5">Date</th>
                  <th className="px-4 py-2.5">Day Type</th>
                  <th className="px-4 py-2.5 text-right">Regular</th>
                  <th className="px-4 py-2.5 text-right">Overtime</th>
                  <th className="px-4 py-2.5 text-right">Late (min)</th>
                  <th className="px-4 py-2.5">Status / Notes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dbe3ee]">
                {entries.isLoading && <TableSkeleton rows={4} cols={6} />}
                {entries.data?.map((e: TimeEntry) => (
                  <tr key={e.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3 num text-[#0b1230]">{e.workDate}</td>
                    <td className="px-4 py-3 text-slate-700">{e.dayType}</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{e.regularHours}h</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{e.overtimeHours}h</td>
                    <td className="px-4 py-3 num text-right text-slate-700">{e.lateMinutes}m</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusBadge status={e.status} />
                        {e.remarks && (
                          <span className="text-xs font-medium text-[#1746cc] bg-[#eef4ff] border border-[#bfdbfe] px-2 py-0.5 rounded">
                            {e.remarks}
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {!entries.isLoading && entries.data?.length === 0 && (
                  <tr>
                    <td colSpan={6}>
                      <EmptyState
                        icon={CalendarClock}
                        title="No time entries in this range"
                        description="Adjust the date filter above or log a new attendance entry."
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
