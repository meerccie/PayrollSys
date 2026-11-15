"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarDays, CheckCircle2, ClipboardCheck, Pencil, Plus, ShieldCheck, X } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  Alert,
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

interface LeaveType {
  id: string;
  code: string;
  name: string;
  paid: boolean;
  annualEntitlement: number;
  active: boolean;
}

interface LeaveBalance {
  id: string;
  employeeId: string;
  leaveTypeId: string;
  year: number;
  entitledDays: number;
  usedDays: number;
  remainingDays: number;
}

interface LeaveRequest {
  id: string;
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  days: number;
  halfDay: boolean;
  reason: string | null;
  status: string;
  supervisorBy: string | null;
  supervisorAt: string | null;
  supervisorRemarks: string | null;
  hrBy: string | null;
  hrAt: string | null;
  hrRemarks: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
}

interface EmployeeOption {
  id: string;
  employeeNo: string;
  fullName: string;
}

type PendingReviewAction = {
  request: LeaveRequest;
  action: "recommend" | "hr-verify" | "reject";
  note: string;
};

function localIsoDate(): string {
  return new Date().toLocaleDateString("en-CA");
}

export default function LeavePage(): React.ReactElement {
  const { user } = useAuth();
  const qc = useQueryClient();
  const isAdmin = user?.role === "ADMIN";
  const isHr = user?.role === "HR";
  const isSupervisor = user?.role === "SUPERVISOR";
  const isHrOrAdmin = isAdmin || isHr;
  const canSeeEmployees = isHrOrAdmin || isSupervisor;

  const currentYear = new Date().getFullYear();
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [leaveTypeId, setLeaveTypeId] = useState("");
  const [startDate, setStartDate] = useState(localIsoDate);
  const [endDate, setEndDate] = useState(localIsoDate);
  const [days, setDays] = useState("1");
  const [halfDay, setHalfDay] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [pendingReview, setPendingReview] = useState<PendingReviewAction | null>(null);

  // HR/Admin leave balance editor
  const [editingBalanceTypeId, setEditingBalanceTypeId] = useState<string | null>(null);
  const [balanceDraftDays, setBalanceDraftDays] = useState("");

  // HR/Admin leave policies manager
  const [showPolicies, setShowPolicies] = useState(false);
  const [editingPolicyId, setEditingPolicyId] = useState<string | null>(null);
  const [policyCode, setPolicyCode] = useState("");
  const [policyName, setPolicyName] = useState("");
  const [policyPaid, setPolicyPaid] = useState(true);
  const [policyEntitlement, setPolicyEntitlement] = useState("15");
  const [policyActive, setPolicyActive] = useState(true);

  const employees = useQuery({
    queryKey: ["employees-list"],
    queryFn: () => apiFetch<{ content: EmployeeOption[] }>("/employees?activeOnly=true&size=100"),
    enabled: Boolean(user) && canSeeEmployees,
  });

  const types = useQuery({
    queryKey: ["leave-types"],
    queryFn: () => apiFetch<LeaveType[]>("/leave-types"),
    enabled: Boolean(user),
  });

  const targetEmployeeId = isHrOrAdmin
    ? selectedEmployeeId || user?.employeeId || ""
    : (user?.employeeId ?? "");

  const balances = useQuery({
    queryKey: ["leave-balances", targetEmployeeId, currentYear],
    queryFn: () => {
      const params = new URLSearchParams({ year: String(currentYear) });
      if (targetEmployeeId) params.set("employeeId", targetEmployeeId);
      return apiFetch<LeaveBalance[]>(`/leave-balances?${params.toString()}`);
    },
    enabled: Boolean(user) && Boolean(targetEmployeeId),
  });

  const requests = useQuery({
    queryKey: ["leave-requests"],
    queryFn: () => apiFetch<LeaveRequest[]>("/leave-requests"),
    enabled: Boolean(user),
  });

  function invalidateLeaveQueries(): void {
    qc.invalidateQueries({ queryKey: ["leave-requests"] });
    qc.invalidateQueries({ queryKey: ["leave-pending"] });
    qc.invalidateQueries({ queryKey: ["leave-balances"] });
    qc.invalidateQueries({ queryKey: ["leave-types"] });
    qc.invalidateQueries({ queryKey: ["time-entries"] });
  }

  const fileMutation = useMutation({
    mutationFn: () =>
      apiFetch("/leave-requests", {
        method: "POST",
        body: JSON.stringify({
          employeeId: isHrOrAdmin ? targetEmployeeId : undefined,
          leaveTypeId,
          startDate,
          endDate,
          days: Number(days),
          halfDay,
          reason,
        }),
      }),
    onSuccess: () => {
      invalidateLeaveQueries();
      setReason("");
      setHalfDay(false);
      setError(null);
      setNotice("Leave application submitted and routed to your Immediate Supervisor.");
    },
    onError: (err: unknown) => {
      setNotice(null);
      setError(err instanceof ApiError ? err.detail : "Failed to file leave request");
    },
  });

  const stageActionMutation = useMutation({
    mutationFn: ({
      id,
      action,
      note,
    }: {
      id: string;
      action: "recommend" | "hr-verify" | "approve" | "reject" | "cancel";
      note?: string;
    }) => {
      const body =
        action === "reject"
          ? JSON.stringify({ reason: note ?? "" })
          : action === "recommend" || action === "hr-verify"
            ? JSON.stringify({ remarks: note ?? "" })
            : undefined;
      return apiFetch(`/leave-requests/${id}/${action}`, {
        method: "POST",
        body,
      });
    },
    onSuccess: (_data, vars) => {
      invalidateLeaveQueries();
      setPendingReview(null);
      setError(null);
      if (vars.action === "approve") {
        setNotice("Leave approved. Employee leave credits and attendance records have been updated.");
      } else if (vars.action === "hr-verify") {
        setNotice("HR verification recorded. Request forwarded for final approval.");
      } else if (vars.action === "recommend") {
        setNotice("Supervisor recommendation recorded. Forwarded to HR for credit and policy review.");
      } else if (vars.action === "reject") {
        setNotice("Leave request rejected.");
      } else {
        setNotice("Leave request cancelled.");
      }
    },
    onError: (err: unknown) => {
      setNotice(null);
      setError(err instanceof ApiError ? err.detail : "Failed to update leave request");
    },
  });

  const saveBalanceMutation = useMutation({
    mutationFn: ({ leaveTypeId: ltId, entitledDays }: { leaveTypeId: string; entitledDays: number }) =>
      apiFetch("/leave-balances", {
        method: "PUT",
        body: JSON.stringify({
          employeeId: targetEmployeeId,
          leaveTypeId: ltId,
          year: currentYear,
          entitledDays,
        }),
      }),
    onSuccess: () => {
      invalidateLeaveQueries();
      setEditingBalanceTypeId(null);
      setError(null);
      setNotice("Leave credits updated.");
    },
    onError: (err: unknown) => {
      setNotice(null);
      setError(err instanceof ApiError ? err.detail : "Failed to update leave balance");
    },
  });

  const savePolicyMutation = useMutation({
    mutationFn: () => {
      const payload = {
        code: policyCode,
        name: policyName,
        paid: policyPaid,
        annualEntitlement: Number(policyEntitlement),
        active: policyActive,
      };
      if (editingPolicyId) {
        return apiFetch(`/leave-types/${editingPolicyId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      }
      return apiFetch("/leave-types", {
        method: "POST",
        body: JSON.stringify(payload),
      });
    },
    onSuccess: () => {
      invalidateLeaveQueries();
      setEditingPolicyId(null);
      setPolicyCode("");
      setPolicyName("");
      setPolicyPaid(true);
      setPolicyEntitlement("15");
      setPolicyActive(true);
      setError(null);
      setNotice("Leave policy saved.");
    },
    onError: (err: unknown) => {
      setNotice(null);
      setError(err instanceof ApiError ? err.detail : "Failed to save leave policy");
    },
  });

  const canFileLeave = isHrOrAdmin || Boolean(user?.employeeId);
  const employeeLookup = new Map(
    (employees.data?.content ?? []).map((emp) => [emp.id, `${emp.employeeNo} · ${emp.fullName}`])
  );
  const leaveTypeLookup = new Map((types.data ?? []).map((t) => [t.id, t]));
  const balanceLookup = new Map((balances.data ?? []).map((b) => [b.leaveTypeId, b]));
  const selectedLeaveType = leaveTypeLookup.get(leaveTypeId);
  const selectedBalance = balanceLookup.get(leaveTypeId);
  const filteredRequests = (requests.data ?? []).filter(
    (r) => statusFilter === "ALL" || r.status === statusFilter
  );

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Leave Management"
          description="Submit leave applications, verify leave balances & policies, and complete multi-stage approvals"
          actions={
            isHrOrAdmin ? (
              <Button
                variant={showPolicies ? "secondary" : "primary"}
                size="sm"
                onClick={() => setShowPolicies((prev) => !prev)}
              >
                <ShieldCheck className="w-4 h-4" />
                {showPolicies ? "Hide Leave Policies" : "Manage Leave Policies"}
              </Button>
            ) : undefined
          }
        />

        {error && <Alert variant="error">{error}</Alert>}
        {notice && <Alert variant="success">{notice}</Alert>}

        {/* HR/Admin Leave Policies Manager */}
        {isHrOrAdmin && showPolicies && (
          <Panel className="p-4 sm:p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                Company Leave Policies ({types.data?.length ?? 0})
              </h2>
              {editingPolicyId && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setEditingPolicyId(null);
                    setPolicyCode("");
                    setPolicyName("");
                    setPolicyPaid(true);
                    setPolicyEntitlement("15");
                    setPolicyActive(true);
                  }}
                >
                  <X className="w-3.5 h-3.5" />
                  Cancel Edit
                </Button>
              )}
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                setError(null);
                savePolicyMutation.mutate();
              }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 items-end"
            >
              <Field label="Code">
                <Input
                  required
                  value={policyCode}
                  onChange={(e) => setPolicyCode(e.target.value)}
                  placeholder="VL"
                  className="uppercase"
                />
              </Field>
              <div className="lg:col-span-2">
                <Field label="Policy Name">
                  <Input
                    required
                    value={policyName}
                    onChange={(e) => setPolicyName(e.target.value)}
                    placeholder="Vacation Leave"
                  />
                </Field>
              </div>
              <Field label="Annual Days">
                <Input
                  type="number"
                  step="0.5"
                  min="0"
                  required
                  value={policyEntitlement}
                  onChange={(e) => setPolicyEntitlement(e.target.value)}
                  className="num"
                />
              </Field>
              <Field label="Pay & Status">
                <Select
                  value={`${policyPaid ? "PAID" : "UNPAID"}:${policyActive ? "ACTIVE" : "INACTIVE"}`}
                  onChange={(e) => {
                    const [paidPart, activePart] = e.target.value.split(":");
                    setPolicyPaid(paidPart === "PAID");
                    setPolicyActive(activePart === "ACTIVE");
                  }}
                >
                  <option value="PAID:ACTIVE">Paid · Active</option>
                  <option value="UNPAID:ACTIVE">Unpaid · Active</option>
                  <option value="PAID:INACTIVE">Paid · Inactive</option>
                  <option value="UNPAID:INACTIVE">Unpaid · Inactive</option>
                </Select>
              </Field>
              <Button type="submit" loading={savePolicyMutation.isPending} className="w-full">
                <Plus className="w-4 h-4" />
                {editingPolicyId ? "Update Policy" : "Add Policy"}
              </Button>
            </form>

            <div className="divide-y divide-[#dbe3ee] text-sm border border-[#dbe3ee] rounded-md">
              {types.data?.map((t) => (
                <div key={t.id} className="px-3 py-2 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono text-xs font-semibold text-slate-600">{t.code}</span>
                    <span className="font-medium text-[#0b1230]">{t.name}</span>
                    <StatusBadge status={t.paid ? "PAID" : "DRAFT"} />
                    {!t.active && <StatusBadge status="CANCELLED" />}
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-600 num">{t.annualEntitlement} days/yr</span>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setEditingPolicyId(t.id);
                        setPolicyCode(t.code);
                        setPolicyName(t.name);
                        setPolicyPaid(t.paid);
                        setPolicyEntitlement(String(t.annualEntitlement));
                        setPolicyActive(t.active);
                      }}
                    >
                      <Pencil className="w-3.5 h-3.5" />
                      Edit
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </Panel>
        )}

        {/* File Leave Request Form */}
        {canFileLeave ? (
          <Panel className="p-4 sm:p-5 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                {isHrOrAdmin ? "File / Record Leave Application" : "Submit Leave Application"}
              </h2>
              {selectedLeaveType && selectedBalance && (
                <span className="text-xs text-slate-600 num">
                  Available {selectedLeaveType.name}:{" "}
                  <strong className="text-[#0b1230]">{selectedBalance.remainingDays} days</strong>
                  {selectedLeaveType.paid ? " (Paid)" : " (Unpaid)"}
                </span>
              )}
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                setError(null);
                fileMutation.mutate();
              }}
              className={`grid grid-cols-1 sm:grid-cols-2 ${
                isHrOrAdmin ? "lg:grid-cols-8" : "lg:grid-cols-7"
              } gap-3 items-end`}
            >
              {isHrOrAdmin && (
                <Field label="Employee">
                  <Select
                    required
                    value={targetEmployeeId}
                    onChange={(e) => setSelectedEmployeeId(e.target.value)}
                  >
                    <option value="">Select employee...</option>
                    {employees.data?.content.map((emp) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.employeeNo} · {emp.fullName}
                      </option>
                    ))}
                  </Select>
                </Field>
              )}
              <Field label="Leave Type">
                <Select required value={leaveTypeId} onChange={(e) => setLeaveTypeId(e.target.value)}>
                  <option value="">Select...</option>
                  {types.data
                    ?.filter((t) => t.active)
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                </Select>
              </Field>
              <Field label="Start">
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    const nextStart = e.target.value;
                    setStartDate(nextStart);
                    if (endDate < nextStart) setEndDate(nextStart);
                  }}
                />
              </Field>
              <Field label="End">
                <Input
                  type="date"
                  min={startDate}
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </Field>
              <Field label="Days">
                <Input
                  type="number"
                  step="0.5"
                  min="0.5"
                  value={days}
                  onChange={(e) => setDays(e.target.value)}
                  className="num"
                />
              </Field>
              <Field label="Duration">
                <Select
                  value={halfDay ? "HALF" : "FULL"}
                  onChange={(e) => {
                    const isHalf = e.target.value === "HALF";
                    setHalfDay(isHalf);
                    if (isHalf) setDays("0.5");
                  }}
                >
                  <option value="FULL">Full Day(s)</option>
                  <option value="HALF">Half Day</option>
                </Select>
              </Field>
              <Field label="Reason">
                <Input
                  required={!isHrOrAdmin}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Reason for leave"
                />
              </Field>
              <Button type="submit" loading={fileMutation.isPending} className="w-full">
                Submit
              </Button>
            </form>
          </Panel>
        ) : (
          <Alert variant="info">
            Your user account is not linked to an employee profile yet. Ask an Administrator to link your account to
            your employee profile in Settings before filing a leave request.
          </Alert>
        )}

        {/* Leave Balances & Credits */}
        {targetEmployeeId && (
          <Panel className="p-4 sm:p-5 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                Leave Credits ({currentYear})
                {isHrOrAdmin && employeeLookup.get(targetEmployeeId)
                  ? ` · ${employeeLookup.get(targetEmployeeId)}`
                  : ""}
              </h2>
              {isHrOrAdmin && (
                <span className="text-xs text-slate-500">
                  HR can adjust entitled credits below before or during review.
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {(balances.data ?? []).map((b) => {
                const lt = leaveTypeLookup.get(b.leaveTypeId);
                const isEditing = editingBalanceTypeId === b.leaveTypeId;
                return (
                  <div
                    key={b.id}
                    className="rounded-md border border-[#dbe3ee] bg-[#f8fafd] px-3.5 py-2.5 flex items-center justify-between gap-3"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-slate-500">
                          {lt?.code ?? "LV"}
                        </span>
                        <span className="text-sm font-semibold text-[#0b1230] truncate">
                          {lt?.name ?? "Leave"}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 num mt-0.5">
                        Remaining: <strong className="text-[#0b1230]">{b.remainingDays}</strong> / {b.entitledDays} (Used{" "}
                        {b.usedDays})
                      </p>
                    </div>
                    {isHrOrAdmin && (
                      <div className="shrink-0">
                        {isEditing ? (
                          <div className="flex items-center gap-1">
                            <Input
                              type="number"
                              step="0.5"
                              min={b.usedDays}
                              aria-label="Entitled days"
                              value={balanceDraftDays}
                              onChange={(e) => setBalanceDraftDays(e.target.value)}
                              className="w-20 h-8 text-xs num"
                            />
                            <Button
                              size="sm"
                              loading={saveBalanceMutation.isPending}
                              onClick={() =>
                                saveBalanceMutation.mutate({
                                  leaveTypeId: b.leaveTypeId,
                                  entitledDays: Number(balanceDraftDays),
                                })
                              }
                            >
                              Save
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setEditingBalanceTypeId(null)}
                            >
                              <X className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        ) : (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => {
                              setEditingBalanceTypeId(b.leaveTypeId);
                              setBalanceDraftDays(String(b.entitledDays));
                            }}
                          >
                            <Pencil className="w-3.5 h-3.5" />
                            Adjust
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </Panel>
        )}

        {/* Inline Review / Remarks Confirmation Bar */}
        {pendingReview && (
          <Panel className="p-4 sm:p-5 border-[#1e5bff]/40 bg-[#eef4ff]/70">
            <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
              <div className="space-y-1">
                <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                  {pendingReview.action === "recommend" && "Step 2 · Immediate Supervisor Recommendation"}
                  {pendingReview.action === "hr-verify" && "Step 3 · HR Credit & Policy Verification"}
                  {pendingReview.action === "reject" && "Reject Leave Application"}
                </h2>
                <p className="text-sm text-slate-700">
                  {employeeLookup.get(pendingReview.request.employeeId) ?? "Employee"} ·{" "}
                  {leaveTypeLookup.get(pendingReview.request.leaveTypeId)?.name ?? "Leave"} ({pendingReview.request.days}d,{" "}
                  {pendingReview.request.startDate} to {pendingReview.request.endDate})
                </p>
              </div>
              <div className="flex flex-wrap items-end gap-2">
                <Field
                  label={
                    pendingReview.action === "reject" ? "Rejection Reason" : "Verification / Review Remarks"
                  }
                >
                  <Input
                    value={pendingReview.note}
                    onChange={(e) =>
                      setPendingReview((prev) => (prev ? { ...prev, note: e.target.value } : null))
                    }
                    placeholder={
                      pendingReview.action === "reject"
                        ? "State reason for rejection"
                        : "Optional review notes"
                    }
                    className="w-64"
                  />
                </Field>
                <Button
                  variant="secondary"
                  onClick={() => setPendingReview(null)}
                  disabled={stageActionMutation.isPending}
                >
                  Cancel
                </Button>
                <Button
                  variant={pendingReview.action === "reject" ? "danger" : "primary"}
                  loading={stageActionMutation.isPending}
                  onClick={() =>
                    stageActionMutation.mutate({
                      id: pendingReview.request.id,
                      action: pendingReview.action,
                      note: pendingReview.note,
                    })
                  }
                >
                  {pendingReview.action === "recommend" && "Confirm Recommendation"}
                  {pendingReview.action === "hr-verify" && "Confirm HR Verification"}
                  {pendingReview.action === "reject" && "Confirm Reject"}
                </Button>
              </div>
            </div>
          </Panel>
        )}

        {/* Leave Requests Queue */}
        <Panel className="overflow-hidden">
          <div className="px-4 py-3 border-b border-[#dbe3ee] flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
              Leave Applications ({filteredRequests.length})
            </h2>
            <Select
              aria-label="Filter by status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-56"
            >
              <option value="ALL">All Stages</option>
              <option value="PENDING">Pending Supervisor</option>
              <option value="SUPERVISOR_RECOMMENDED">Supervisor Recommended (HR Queue)</option>
              <option value="HR_VERIFIED">HR Verified (Final Approval)</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="CANCELLED">Cancelled</option>
            </Select>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                <tr>
                  {canSeeEmployees && <th className="px-4 py-2.5">Employee</th>}
                  <th className="px-4 py-2.5">Leave Type</th>
                  <th className="px-4 py-2.5">Period</th>
                  <th className="px-4 py-2.5 text-right">Days</th>
                  <th className="px-4 py-2.5">Reason & Audit Notes</th>
                  <th className="px-4 py-2.5">Status</th>
                  <th className="px-4 py-2.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dbe3ee]">
                {requests.isLoading && <TableSkeleton rows={4} cols={canSeeEmployees ? 7 : 6} />}
                {filteredRequests.map((r) => {
                  const isOwnRequest = Boolean(user?.employeeId && r.employeeId === user.employeeId);
                  const canRecommend = r.status === "PENDING" && (isAdmin || (isSupervisor && !isOwnRequest));
                  const canHrVerify = r.status === "SUPERVISOR_RECOMMENDED" && (isAdmin || (isHr && !isOwnRequest));
                  const canFinalApprove = r.status === "HR_VERIFIED" && (isAdmin || (isSupervisor && !isOwnRequest));
                  const canReject =
                    !isOwnRequest &&
                    ((r.status === "PENDING" && (isAdmin || isSupervisor || isHr)) ||
                      (r.status === "SUPERVISOR_RECOMMENDED" && (isAdmin || isHr)) ||
                      (r.status === "HR_VERIFIED" && (isAdmin || isSupervisor)));
                  const canCancel =
                    (isOwnRequest || isHrOrAdmin) &&
                    (r.status === "PENDING" ||
                      r.status === "SUPERVISOR_RECOMMENDED" ||
                      r.status === "HR_VERIFIED" ||
                      r.status === "APPROVED");

                  return (
                    <tr key={r.id} className="hover:bg-slate-50/80 transition-colors align-top">
                      {canSeeEmployees && (
                        <td className="px-4 py-3 text-slate-700">
                          {employeeLookup.get(r.employeeId) ?? r.employeeId.slice(0, 8)}
                        </td>
                      )}
                      <td className="px-4 py-3 font-medium text-[#0b1230]">
                        {leaveTypeLookup.get(r.leaveTypeId)?.name ?? "Leave"}
                        {r.halfDay && <span className="ml-1.5 text-xs text-slate-500">(Half)</span>}
                      </td>
                      <td className="px-4 py-3 num text-[#0b1230] whitespace-nowrap">
                        {r.startDate} to {r.endDate}
                      </td>
                      <td className="px-4 py-3 num text-right text-slate-700">{r.days}</td>
                      <td className="px-4 py-3 text-slate-600 space-y-1 max-w-xs">
                        <div>{r.reason || "—"}</div>
                        {r.supervisorRemarks && (
                          <div className="text-xs text-[#1746cc]">Supervisor: {r.supervisorRemarks}</div>
                        )}
                        {r.hrRemarks && <div className="text-xs text-cyan-800">HR: {r.hrRemarks}</div>}
                        {r.rejectionReason && (
                          <div className="text-xs text-red-700">Rejected: {r.rejectionReason}</div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={r.status} />
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="inline-flex flex-wrap justify-end items-center gap-1">
                          {canRecommend && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                setPendingReview({ request: r, action: "recommend", note: "" })
                              }
                              className="text-[#1746cc] hover:bg-[#eef4ff]"
                            >
                              <ClipboardCheck className="w-3.5 h-3.5" />
                              Recommend
                            </Button>
                          )}
                          {canHrVerify && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                setPendingReview({ request: r, action: "hr-verify", note: "" })
                              }
                              className="text-cyan-800 hover:bg-cyan-50"
                            >
                              <ShieldCheck className="w-3.5 h-3.5" />
                              HR Verify
                            </Button>
                          )}
                          {canFinalApprove && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                stageActionMutation.mutate({ id: r.id, action: "approve" })
                              }
                              disabled={stageActionMutation.isPending}
                              className="text-emerald-700 hover:bg-emerald-50"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              Approve
                            </Button>
                          )}
                          {canReject && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                setPendingReview({ request: r, action: "reject", note: "" })
                              }
                              className="text-red-700 hover:bg-red-50"
                            >
                              Reject
                            </Button>
                          )}
                          {canCancel && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                stageActionMutation.mutate({ id: r.id, action: "cancel" })
                              }
                              disabled={stageActionMutation.isPending}
                            >
                              Cancel
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {!requests.isLoading && filteredRequests.length === 0 && (
                  <tr>
                    <td colSpan={canSeeEmployees ? 7 : 6}>
                      <EmptyState
                        icon={CalendarDays}
                        title="No leave requests"
                        description="Leave requests submitted by employees will appear here across each approval stage."
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
