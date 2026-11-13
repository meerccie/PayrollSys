"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

interface LeaveType { id: string; code: string; name: string; paid: boolean; }
interface LeaveRequest {
  id: string;
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: string;
}

export default function LeavePage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const isHrOrAdmin = user?.role === "ADMIN" || user?.role === "HR";
  const [leaveTypeId, setLeaveTypeId] = useState("");
  const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10));
  const [endDate, setEndDate] = useState(new Date().toISOString().slice(0, 10));
  const [days, setDays] = useState("1");
  const [reason, setReason] = useState("");

  const types = useQuery({ queryKey: ["leave-types"], queryFn: () => apiFetch<LeaveType[]>("/leave-types") });
  const requests = useQuery({ queryKey: ["leave-requests"], queryFn: () => apiFetch<LeaveRequest[]>("/leave-requests") });

  const fileMutation = useMutation({
    mutationFn: () =>
      apiFetch("/leave-requests", {
        method: "POST",
        body: JSON.stringify({ leaveTypeId, startDate, endDate, days: Number(days), halfDay: false, reason }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leave-requests"] });
      setReason("");
    },
  });

  const actionMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "approve" | "reject" | "cancel" }) =>
      apiFetch(`/leave-requests/${id}/${action}`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["leave-requests"] }),
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Leave Management</h1>

        {user?.employeeId && (
          <form
            onSubmit={(e) => { e.preventDefault(); fileMutation.mutate(); }}
            className="bg-white rounded-xl border border-slate-200 p-5 grid grid-cols-1 md:grid-cols-6 gap-3 items-end"
          >
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Leave Type</label>
              <select required value={leaveTypeId} onChange={(e) => setLeaveTypeId(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
                <option value="">Select...</option>
                {types.data?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Start</label>
              <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">End</label>
              <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Days</label>
              <input type="number" step="0.5" min="0.5" value={days} onChange={(e) => setDays(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Reason</label>
              <input value={reason} onChange={(e) => setReason(e.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <button type="submit" className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700">
              File Leave
            </button>
          </form>
        )}

        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Period</th>
                <th className="px-4 py-3">Days</th>
                <th className="px-4 py-3">Reason</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {requests.data?.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3">{r.startDate} to {r.endDate}</td>
                  <td className="px-4 py-3">{r.days}</td>
                  <td className="px-4 py-3 text-slate-600">{r.reason || "-"}</td>
                  <td className="px-4 py-3">{r.status}</td>
                  <td className="px-4 py-3 text-right space-x-2">
                    {isHrOrAdmin && r.status === "PENDING" && (
                      <>
                        <button onClick={() => actionMutation.mutate({ id: r.id, action: "approve" })} className="text-emerald-600 hover:underline text-xs">Approve</button>
                        <button onClick={() => actionMutation.mutate({ id: r.id, action: "reject" })} className="text-red-600 hover:underline text-xs">Reject</button>
                      </>
                    )}
                    {(r.status === "PENDING" || r.status === "APPROVED") && (
                      <button onClick={() => actionMutation.mutate({ id: r.id, action: "cancel" })} className="text-slate-600 hover:underline text-xs">Cancel</button>
                    )}
                  </td>
                </tr>
              ))}
              {requests.data?.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400">No leave requests</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
