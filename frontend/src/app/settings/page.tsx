"use client";

import React, { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch, Role } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

interface CompanyConfig {
  companyName: string;
  companyTin: string | null;
  companyAddress: string | null;
  settings: {
    annualWorkDays: number;
    statutorySchedule: "SPLIT" | "FIRST_CUTOFF" | "SECOND_CUTOFF";
  };
}

interface UserItem {
  id: string;
  email: string;
  role: Role;
  employeeId: string | null;
  active: boolean;
}

export default function SettingsPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const isAdmin = user?.role === "ADMIN";

  const configQuery = useQuery({
    queryKey: ["company-config"],
    queryFn: () => apiFetch<CompanyConfig>("/settings/company"),
  });

  const usersQuery = useQuery({
    queryKey: ["users"],
    queryFn: () => apiFetch<UserItem[]>("/users"),
    enabled: isAdmin,
  });

  const [companyName, setCompanyName] = useState("");
  const [annualWorkDays, setAnnualWorkDays] = useState(261);
  const [statutorySchedule, setStatutorySchedule] = useState<"SPLIT" | "FIRST_CUTOFF" | "SECOND_CUTOFF">("SPLIT");
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newRole, setNewRole] = useState<Role>("HR");

  useEffect(() => {
    if (configQuery.data) {
      setCompanyName(configQuery.data.companyName);
      setAnnualWorkDays(configQuery.data.settings.annualWorkDays);
      setStatutorySchedule(configQuery.data.settings.statutorySchedule);
    }
  }, [configQuery.data]);

  const saveConfig = useMutation({
    mutationFn: () =>
      apiFetch("/settings/company", {
        method: "PUT",
        body: JSON.stringify({ companyName, annualWorkDays, statutorySchedule }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["company-config"] }),
  });

  const createUser = useMutation({
    mutationFn: () =>
      apiFetch("/users", {
        method: "POST",
        body: JSON.stringify({ email: newEmail, password: newPassword, role: newRole }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setNewEmail("");
      setNewPassword("");
    },
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Settings</h1>

        <form
          onSubmit={(e) => { e.preventDefault(); saveConfig.mutate(); }}
          className="bg-white rounded-xl border border-slate-200 p-6 space-y-4 max-w-2xl"
        >
          <h2 className="font-semibold text-slate-900">Company & Payroll Defaults</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-3">
              <label className="block text-xs font-medium text-slate-600 mb-1">Company Name</label>
              <input value={companyName} onChange={(e) => setCompanyName(e.target.value)} disabled={!isAdmin} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Annual Work Days</label>
              <input type="number" value={annualWorkDays} onChange={(e) => setAnnualWorkDays(Number(e.target.value))} disabled={!isAdmin} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div className="md:col-span-2">
              <label className="block text-xs font-medium text-slate-600 mb-1">Semi-Monthly Statutory Schedule</label>
              <select value={statutorySchedule} onChange={(e) => setStatutorySchedule(e.target.value as typeof statutorySchedule)} disabled={!isAdmin} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
                <option value="SPLIT">Split 50/50 across both cutoffs</option>
                <option value="FIRST_CUTOFF">All on 1st cutoff</option>
                <option value="SECOND_CUTOFF">All on 2nd cutoff</option>
              </select>
            </div>
          </div>
          {isAdmin && (
            <button type="submit" className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700">
              Save Settings
            </button>
          )}
        </form>

        {isAdmin && (
          <div className="bg-white rounded-xl border border-slate-200 p-6 space-y-4 max-w-2xl">
            <h2 className="font-semibold text-slate-900">Users</h2>
            <form onSubmit={(e) => { e.preventDefault(); createUser.mutate(); }} className="flex flex-wrap gap-2">
              <input type="email" required placeholder="Email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} className="flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <input type="password" required minLength={8} placeholder="Password (8+ chars)" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="w-40 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <select value={newRole} onChange={(e) => setNewRole(e.target.value as Role)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm">
                <option value="HR">HR</option>
                <option value="ADMIN">ADMIN</option>
                <option value="EMPLOYEE">EMPLOYEE</option>
              </select>
              <button className="rounded-lg bg-indigo-600 text-white px-3 py-1.5 text-sm font-medium">Create</button>
            </form>
            <ul className="divide-y divide-slate-200 text-sm">
              {usersQuery.data?.map((u) => (
                <li key={u.id} className="py-2 flex justify-between">
                  <span>{u.email}</span>
                  <span className="text-xs font-medium text-slate-500">{u.role}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </AppShell>
  );
}
