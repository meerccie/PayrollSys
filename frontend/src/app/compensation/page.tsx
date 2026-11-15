"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CreditCard, Plus } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Button, EmptyState, Input, PageHeader, Panel } from "@/components/ui";

interface EarningType {
  id: string;
  code: string;
  name: string;
  taxable: boolean;
}

interface DeductionType {
  id: string;
  code: string;
  name: string;
  loanType: boolean;
}

export default function CompensationPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [eCode, setECode] = useState("");
  const [eName, setEName] = useState("");
  const [eTaxable, setETaxable] = useState(true);
  const [dCode, setDCode] = useState("");
  const [dName, setDName] = useState("");
  const [dLoan, setDLoan] = useState(false);

  const earningTypes = useQuery({
    queryKey: ["earning-types"],
    queryFn: () => apiFetch<EarningType[]>("/earning-types"),
    enabled: Boolean(user),
  });
  const deductionTypes = useQuery({
    queryKey: ["deduction-types"],
    queryFn: () => apiFetch<DeductionType[]>("/deduction-types"),
    enabled: Boolean(user),
  });

  const addEarning = useMutation({
    mutationFn: () =>
      apiFetch("/earning-types", {
        method: "POST",
        body: JSON.stringify({ code: eCode, name: eName, taxable: eTaxable, recurringAllowed: true, active: true }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["earning-types"] });
      setECode("");
      setEName("");
    },
  });

  const addDeduction = useMutation({
    mutationFn: () =>
      apiFetch("/deduction-types", {
        method: "POST",
        body: JSON.stringify({ code: dCode, name: dName, recurringAllowed: true, loanType: dLoan, active: true }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["deduction-types"] });
      setDCode("");
      setDName("");
    },
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Earnings & Deductions"
          description="Configure taxable allowances, bonuses, and loan deduction categories"
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Panel className="p-5 space-y-4">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">Earning Types</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                addEarning.mutate();
              }}
              className="flex flex-wrap gap-2 items-center"
            >
              <Input
                placeholder="Code"
                value={eCode}
                onChange={(e) => setECode(e.target.value)}
                required
                className="w-24 font-mono text-xs"
              />
              <Input
                placeholder="Earning name"
                value={eName}
                onChange={(e) => setEName(e.target.value)}
                required
                className="flex-1 min-w-[140px]"
              />
              <label className="text-xs text-slate-700 flex items-center gap-1.5 px-1 cursor-pointer">
                <input
                  type="checkbox"
                  checked={eTaxable}
                  onChange={(e) => setETaxable(e.target.checked)}
                  className="accent-[#1e5bff] rounded"
                />
                Taxable
              </label>
              <Button type="submit" loading={addEarning.isPending}>
                <Plus className="w-4 h-4" />
                Add
              </Button>
            </form>
            {earningTypes.isLoading ? (
              <div className="space-y-2 py-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-8 rounded bg-slate-100 animate-pulse" />
                ))}
              </div>
            ) : earningTypes.data?.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="No earning types"
                description="Add an allowance or bonus type above."
              />
            ) : (
              <ul className="divide-y divide-[#dbe3ee] text-sm">
                {earningTypes.data?.map((t: EarningType) => (
                  <li key={t.id} className="py-2.5 flex items-center justify-between gap-2">
                    <span className="text-[#0b1230]">
                      <span className="font-mono text-xs text-slate-500 mr-2.5">{t.code}</span>
                      {t.name}
                    </span>
                    <span className="text-xs font-medium text-slate-600">
                      {t.taxable ? "Taxable" : "Non-taxable"}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          <Panel className="p-5 space-y-4">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">Deduction Types</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                addDeduction.mutate();
              }}
              className="flex flex-wrap gap-2 items-center"
            >
              <Input
                placeholder="Code"
                value={dCode}
                onChange={(e) => setDCode(e.target.value)}
                required
                className="w-24 font-mono text-xs"
              />
              <Input
                placeholder="Deduction name"
                value={dName}
                onChange={(e) => setDName(e.target.value)}
                required
                className="flex-1 min-w-[140px]"
              />
              <label className="text-xs text-slate-700 flex items-center gap-1.5 px-1 cursor-pointer">
                <input
                  type="checkbox"
                  checked={dLoan}
                  onChange={(e) => setDLoan(e.target.checked)}
                  className="accent-[#1e5bff] rounded"
                />
                Loan
              </label>
              <Button type="submit" loading={addDeduction.isPending}>
                <Plus className="w-4 h-4" />
                Add
              </Button>
            </form>
            {deductionTypes.isLoading ? (
              <div className="space-y-2 py-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-8 rounded bg-slate-100 animate-pulse" />
                ))}
              </div>
            ) : deductionTypes.data?.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="No deduction types"
                description="Add a recurring deduction or loan type above."
              />
            ) : (
              <ul className="divide-y divide-[#dbe3ee] text-sm">
                {deductionTypes.data?.map((t: DeductionType) => (
                  <li key={t.id} className="py-2.5 flex items-center justify-between gap-2">
                    <span className="text-[#0b1230]">
                      <span className="font-mono text-xs text-slate-500 mr-2.5">{t.code}</span>
                      {t.name}
                    </span>
                    <span className="text-xs font-medium text-slate-600">{t.loanType ? "Loan" : "Regular"}</span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>
        </div>
      </div>
    </AppShell>
  );
}
