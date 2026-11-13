"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch, ApiError } from "@/lib/api";
import { formatPhp } from "@/lib/format";

interface Employee {
  id: string;
  employeeNo: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  hireDate: string;
  status: string;
  payBasis: "MONTHLY" | "DAILY";
  baseRate: number;
  departmentId: string | null;
  positionId: string | null;
}

interface PageResult<T> {
  content: T[];
  totalElements: number;
}

interface OrgItem {
  id: string;
  code: string;
  name: string;
}

export default function EmployeesPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    employeeNo: "",
    firstName: "",
    lastName: "",
    email: "",
    hireDate: new Date().toISOString().slice(0, 10),
    status: "REGULAR",
    payBasis: "MONTHLY",
    baseRate: "30000",
    departmentId: "",
    positionId: "",
  });

  const employees = useQuery({
    queryKey: ["employees", search],
    queryFn: () => apiFetch<PageResult<Employee>>(`/employees?q=${encodeURIComponent(search)}&size=50`),
  });
  const departments = useQuery({
    queryKey: ["departments"],
    queryFn: () => apiFetch<OrgItem[]>("/departments"),
  });
  const positions = useQuery({
    queryKey: ["positions"],
    queryFn: () => apiFetch<OrgItem[]>("/positions"),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      apiFetch("/employees", {
        method: "POST",
        body: JSON.stringify({
          ...form,
          baseRate: Number(form.baseRate),
          departmentId: form.departmentId || null,
          positionId: form.positionId || null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      setShowForm(false);
      setError(null);
    },
    onError: (err) => setError(err instanceof ApiError ? err.detail : "Failed to save employee"),
  });

  const separateMutation = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/employees/${id}/separate`, {
        method: "POST",
        body: JSON.stringify({ separationDate: new Date().toISOString().slice(0, 10) }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employees"] }),
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Employees</h1>
            <p className="text-sm text-slate-500">Manage employee profiles, rates, and status</p>
          </div>
          <button
            onClick={() => setShowForm((s) => !s)}
            className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700"
          >
            {showForm ? "Cancel" : "Add Employee"}
          </button>
        </div>

        {showForm && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createMutation.mutate();
            }}
            className="bg-white rounded-xl border border-slate-200 p-6 space-y-4"
          >
            <h2 className="font-semibold text-slate-900">New Employee</h2>
            {error && <div className="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{error}</div>}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input label="Employee No" value={form.employeeNo} onChange={(v) => setForm({ ...form, employeeNo: v })} required />
              <Input label="First Name" value={form.firstName} onChange={(v) => setForm({ ...form, firstName: v })} required />
              <Input label="Last Name" value={form.lastName} onChange={(v) => setForm({ ...form, lastName: v })} required />
              <Input label="Email" type="email" value={form.email} onChange={(v) => setForm({ ...form, email: v })} required />
              <Input label="Hire Date" type="date" value={form.hireDate} onChange={(v) => setForm({ ...form, hireDate: v })} required />
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Pay Basis</label>
                <select
                  value={form.payBasis}
                  onChange={(e) => setForm({ ...form, payBasis: e.target.value })}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="DAILY">Daily</option>
                </select>
              </div>
              <Input label="Base Rate (PHP)" type="number" value={form.baseRate} onChange={(v) => setForm({ ...form, baseRate: v })} required />
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Department</label>
                <select
                  value={form.departmentId}
                  onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">-- None --</option>
                  {departments.data?.map((d) => (
                    <option key={d.id} value={d.id}>{d.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Position</label>
                <select
                  value={form.positionId}
                  onChange={(e) => setForm({ ...form, positionId: e.target.value })}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">-- None --</option>
                  {positions.data?.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="rounded-lg bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700"
            >
              {createMutation.isPending ? "Saving..." : "Create Employee"}
            </button>
          </form>
        )}

        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div className="p-4 border-b border-slate-200">
            <input
              type="search"
              placeholder="Search by name, employee no, or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-sm rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 text-slate-600 text-xs uppercase">
                <tr>
                  <th className="px-4 py-3">Emp No</th>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Basis</th>
                  <th className="px-4 py-3">Base Rate</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {employees.data?.content.map((emp) => (
                  <tr key={emp.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono text-xs">{emp.employeeNo}</td>
                    <td className="px-4 py-3 font-medium text-slate-900">{emp.fullName}</td>
                    <td className="px-4 py-3 text-slate-600">{emp.email}</td>
                    <td className="px-4 py-3">{emp.payBasis}</td>
                    <td className="px-4 py-3">{formatPhp(emp.baseRate)}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex rounded-full px-2 py-0.5 text-xs font-medium bg-slate-100 text-slate-700">
                        {emp.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {emp.status !== "SEPARATED" && (
                        <button
                          onClick={() => separateMutation.mutate(emp.id)}
                          className="text-xs text-red-600 hover:underline"
                        >
                          Separate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {employees.data?.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                      No employees found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function Input({
  label,
  value,
  onChange,
  type = "text",
  required = false,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <div>
      <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
      <input
        type={type}
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
      />
    </div>
  );
}
