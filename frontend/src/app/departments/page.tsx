"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";

interface OrgItem {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export default function DepartmentsPage() {
  const qc = useQueryClient();
  const [deptCode, setDeptCode] = useState("");
  const [deptName, setDeptName] = useState("");
  const [posCode, setPosCode] = useState("");
  const [posName, setPosName] = useState("");

  const departments = useQuery({ queryKey: ["departments"], queryFn: () => apiFetch<OrgItem[]>("/departments") });
  const positions = useQuery({ queryKey: ["positions"], queryFn: () => apiFetch<OrgItem[]>("/positions") });

  const createDept = useMutation({
    mutationFn: () => apiFetch("/departments", { method: "POST", body: JSON.stringify({ code: deptCode, name: deptName }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["departments"] });
      setDeptCode("");
      setDeptName("");
    },
  });

  const createPos = useMutation({
    mutationFn: () => apiFetch("/positions", { method: "POST", body: JSON.stringify({ code: posCode, name: posName }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["positions"] });
      setPosCode("");
      setPosName("");
    },
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Departments & Positions</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
            <h2 className="font-semibold text-slate-900">Departments</h2>
            <form
              onSubmit={(e) => { e.preventDefault(); createDept.mutate(); }}
              className="flex gap-2"
            >
              <input placeholder="Code" value={deptCode} onChange={(e) => setDeptCode(e.target.value)} required className="w-28 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <input placeholder="Name" value={deptName} onChange={(e) => setDeptName(e.target.value)} required className="flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <button className="rounded-lg bg-indigo-600 text-white px-3 py-1.5 text-sm font-medium">Add</button>
            </form>
            <ul className="divide-y divide-slate-200 text-sm">
              {departments.data?.map((d) => (
                <li key={d.id} className="py-2 flex justify-between">
                  <span><span className="font-mono text-xs text-slate-500 mr-2">{d.code}</span>{d.name}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
            <h2 className="font-semibold text-slate-900">Positions</h2>
            <form
              onSubmit={(e) => { e.preventDefault(); createPos.mutate(); }}
              className="flex gap-2"
            >
              <input placeholder="Code" value={posCode} onChange={(e) => setPosCode(e.target.value)} required className="w-28 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <input placeholder="Name" value={posName} onChange={(e) => setPosName(e.target.value)} required className="flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <button className="rounded-lg bg-indigo-600 text-white px-3 py-1.5 text-sm font-medium">Add</button>
            </form>
            <ul className="divide-y divide-slate-200 text-sm">
              {positions.data?.map((p) => (
                <li key={p.id} className="py-2 flex justify-between">
                  <span><span className="font-mono text-xs text-slate-500 mr-2">{p.code}</span>{p.name}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
