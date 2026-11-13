"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";

interface EarningType { id: string; code: string; name: string; taxable: boolean; }
interface DeductionType { id: string; code: string; name: string; loanType: boolean; }

export default function CompensationPage() {
  const qc = useQueryClient();
  const [eCode, setECode] = useState("");
  const [eName, setEName] = useState("");
  const [eTaxable, setETaxable] = useState(true);
  const [dCode, setDCode] = useState("");
  const [dName, setDName] = useState("");
  const [dLoan, setDLoan] = useState(false);

  const earningTypes = useQuery({ queryKey: ["earning-types"], queryFn: () => apiFetch<EarningType[]>("/earning-types") });
  const deductionTypes = useQuery({ queryKey: ["deduction-types"], queryFn: () => apiFetch<DeductionType[]>("/deduction-types") });

  const addEarning = useMutation({
    mutationFn: () => apiFetch("/earning-types", { method: "POST", body: JSON.stringify({ code: eCode, name: eName, taxable: eTaxable, recurringAllowed: true, active: true }) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["earning-types"] }); setECode(""); setEName(""); },
  });

  const addDeduction = useMutation({
    mutationFn: () => apiFetch("/deduction-types", { method: "POST", body: JSON.stringify({ code: dCode, name: dName, recurringAllowed: true, loanType: dLoan, active: true }) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["deduction-types"] }); setDCode(""); setDName(""); },
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Earnings & Deductions</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
            <h2 className="font-semibold text-slate-900">Earning Types</h2>
            <form onSubmit={(e) => { e.preventDefault(); addEarning.mutate(); }} className="flex flex-wrap gap-2 items-center">
              <input placeholder="Code" value={eCode} onChange={(e) => setECode(e.target.value)} required className="w-24 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <input placeholder="Name" value={eName} onChange={(e) => setEName(e.target.value)} required className="flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <label className="text-xs flex items-center gap-1"><input type="checkbox" checked={eTaxable} onChange={(e) => setETaxable(e.target.checked)} />Taxable</label>
              <button className="rounded-lg bg-indigo-600 text-white px-3 py-1.5 text-sm font-medium">Add</button>
            </form>
            <ul className="divide-y divide-slate-200 text-sm">
              {earningTypes.data?.map((t) => (
                <li key={t.id} className="py-2 flex justify-between">
                  <span><span className="font-mono text-xs text-slate-500 mr-2">{t.code}</span>{t.name}</span>
                  <span className="text-xs text-slate-500">{t.taxable ? "Taxable" : "Non-taxable"}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
            <h2 className="font-semibold text-slate-900">Deduction Types</h2>
            <form onSubmit={(e) => { e.preventDefault(); addDeduction.mutate(); }} className="flex flex-wrap gap-2 items-center">
              <input placeholder="Code" value={dCode} onChange={(e) => setDCode(e.target.value)} required className="w-24 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <input placeholder="Name" value={dName} onChange={(e) => setDName(e.target.value)} required className="flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm" />
              <label className="text-xs flex items-center gap-1"><input type="checkbox" checked={dLoan} onChange={(e) => setDLoan(e.target.checked)} />Loan</label>
              <button className="rounded-lg bg-indigo-600 text-white px-3 py-1.5 text-sm font-medium">Add</button>
            </form>
            <ul className="divide-y divide-slate-200 text-sm">
              {deductionTypes.data?.map((t) => (
                <li key={t.id} className="py-2 flex justify-between">
                  <span><span className="font-mono text-xs text-slate-500 mr-2">{t.code}</span>{t.name}</span>
                  <span className="text-xs text-slate-500">{t.loanType ? "Loan" : "Regular"}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
