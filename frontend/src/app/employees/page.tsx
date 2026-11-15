"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Pencil, RotateCcw, Search, UserPlus, Users, X } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch, ApiError } from "@/lib/api";
import { formatPhp } from "@/lib/format";
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

type EmploymentStatus = "PROBATIONARY" | "REGULAR" | "CONTRACTUAL" | "PROJECT_BASED" | "SEPARATED";
type PayBasis = "MONTHLY" | "DAILY";

interface Employee {
  id: string;
  employeeNo: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  fullName: string;
  birthDate: string | null;
  email: string;
  phone: string | null;
  address: string | null;
  tin: string | null;
  sssNo: string | null;
  philHealthNo: string | null;
  pagIbigNo: string | null;
  hireDate: string;
  separationDate: string | null;
  status: EmploymentStatus;
  payBasis: PayBasis;
  baseRate: number;
  departmentId: string | null;
  positionId: string | null;
  supervisorId: string | null;
  bankName: string | null;
  bankAccountNo: string | null;
  bankAccountName: string | null;
  restDays: string[];
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

interface EmployeeFormState {
  employeeNo: string;
  firstName: string;
  lastName: string;
  email: string;
  hireDate: string;
  status: EmploymentStatus;
  payBasis: PayBasis;
  baseRate: string;
  departmentId: string;
  positionId: string;
  supervisorId: string;
}

function localIsoDate(): string {
  return new Date().toLocaleDateString("en-CA");
}

function createEmptyForm(): EmployeeFormState {
  return {
    employeeNo: "",
    firstName: "",
    lastName: "",
    email: "",
    hireDate: localIsoDate(),
    status: "REGULAR",
    payBasis: "MONTHLY",
    baseRate: "30000",
    departmentId: "",
    positionId: "",
    supervisorId: "",
  };
}

export default function EmployeesPage(): React.ReactElement {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [showForm, setShowForm] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [form, setForm] = useState<EmployeeFormState>(createEmptyForm);
  const [pendingSeparate, setPendingSeparate] = useState<{
    employee: Employee;
    separationDate: string;
    previousStatus: EmploymentStatus;
  } | null>(null);
  const [lastSeparated, setLastSeparated] = useState<{
    id: string;
    fullName: string;
    employeeNo: string;
    previousStatus: EmploymentStatus;
  } | null>(null);

  const employees = useQuery({
    queryKey: ["employees", search, statusFilter],
    queryFn: () => {
      const params = new URLSearchParams({ q: search, size: "50" });
      if (statusFilter === "ACTIVE_ONLY") {
        params.set("activeOnly", "true");
      } else if (statusFilter !== "ALL") {
        params.set("status", statusFilter);
      }
      return apiFetch<PageResult<Employee>>(`/employees?${params.toString()}`);
    },
  });
  const departments = useQuery({
    queryKey: ["departments"],
    queryFn: () => apiFetch<OrgItem[]>("/departments"),
  });
  const positions = useQuery({
    queryKey: ["positions"],
    queryFn: () => apiFetch<OrgItem[]>("/positions"),
  });
  const supervisorOptions = useQuery({
    queryKey: ["employees-list"],
    queryFn: () => apiFetch<PageResult<Employee>>("/employees?activeOnly=true&size=100"),
  });

  function invalidateEmployeeQueries(): void {
    qc.invalidateQueries({ queryKey: ["employees"] });
    qc.invalidateQueries({ queryKey: ["employees-count"] });
    qc.invalidateQueries({ queryKey: ["employees-list"] });
    qc.invalidateQueries({ queryKey: ["employees-all"] });
  }

  function closeForm(): void {
    setShowForm(false);
    setEditingEmployee(null);
    setFormError(null);
    setForm(createEmptyForm());
  }

  function startEdit(emp: Employee): void {
    setActionError(null);
    setFormError(null);
    setEditingEmployee(emp);
    setForm({
      employeeNo: emp.employeeNo,
      firstName: emp.firstName,
      lastName: emp.lastName,
      email: emp.email,
      hireDate: emp.hireDate,
      status: emp.status,
      payBasis: emp.payBasis,
      baseRate: String(emp.baseRate),
      departmentId: emp.departmentId ?? "",
      positionId: emp.positionId ?? "",
      supervisorId: emp.supervisorId ?? "",
    });
    setShowForm(true);
  }

  function requestSeparate(emp: Employee): void {
    const today = localIsoDate();
    const defaultDate = today >= emp.hireDate ? today : emp.hireDate;
    setActionError(null);
    setPendingSeparate({
      employee: emp,
      separationDate: defaultDate,
      previousStatus: emp.status === "SEPARATED" ? "REGULAR" : emp.status,
    });
  }

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!editingEmployee) {
        return apiFetch<Employee>("/employees", {
          method: "POST",
          body: JSON.stringify({
            ...form,
            baseRate: Number(form.baseRate),
            departmentId: form.departmentId || null,
            positionId: form.positionId || null,
            supervisorId: form.supervisorId || null,
          }),
        });
      }

      if (form.status === "SEPARATED") {
        requestSeparate(editingEmployee);
        return editingEmployee;
      }

      return apiFetch<Employee>(`/employees/${editingEmployee.id}`, {
        method: "PUT",
        body: JSON.stringify({
          firstName: form.firstName,
          middleName: editingEmployee.middleName,
          lastName: form.lastName,
          birthDate: editingEmployee.birthDate,
          email: form.email,
          phone: editingEmployee.phone,
          address: editingEmployee.address,
          tin: editingEmployee.tin,
          sssNo: editingEmployee.sssNo,
          philHealthNo: editingEmployee.philHealthNo,
          pagIbigNo: editingEmployee.pagIbigNo,
          status: form.status,
          payBasis: form.payBasis,
          baseRate: Number(form.baseRate),
          departmentId: form.departmentId || null,
          positionId: form.positionId || null,
          supervisorId: form.supervisorId || null,
          bankName: editingEmployee.bankName,
          bankAccountNo: editingEmployee.bankAccountNo,
          bankAccountName: editingEmployee.bankAccountName,
          restDays: editingEmployee.restDays,
        }),
      });
    },
    onSuccess: () => {
      invalidateEmployeeQueries();
      closeForm();
    },
    onError: (err: unknown) => setFormError(err instanceof ApiError ? err.detail : "Failed to save employee"),
  });

  const separateMutation = useMutation({
    mutationFn: ({
      employee,
      separationDate,
    }: {
      employee: Employee;
      separationDate: string;
      previousStatus: EmploymentStatus;
    }) =>
      apiFetch<Employee>(`/employees/${employee.id}/separate`, {
        method: "POST",
        body: JSON.stringify({ separationDate }),
      }),
    onSuccess: (_data, vars) => {
      setActionError(null);
      setLastSeparated({
        id: vars.employee.id,
        fullName: vars.employee.fullName,
        employeeNo: vars.employee.employeeNo,
        previousStatus: vars.previousStatus,
      });
      setPendingSeparate(null);
      invalidateEmployeeQueries();
    },
    onError: (err: unknown) =>
      setActionError(err instanceof ApiError ? err.detail : "Failed to separate employee"),
  });

  const reinstateMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: EmploymentStatus }) =>
      apiFetch<Employee>(`/employees/${id}/reinstate`, {
        method: "POST",
        body: JSON.stringify({ status }),
      }),
    onSuccess: (_data, vars) => {
      setActionError(null);
      if (lastSeparated?.id === vars.id) {
        setLastSeparated(null);
      }
      invalidateEmployeeQueries();
    },
    onError: (err: unknown) =>
      setActionError(err instanceof ApiError ? err.detail : "Failed to reinstate employee"),
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Employees"
          description="Manage employee profiles, compensation rates, and employment status"
          actions={
            <Button
              variant={showForm ? "secondary" : "primary"}
              onClick={() => {
                if (showForm) {
                  closeForm();
                } else {
                  setEditingEmployee(null);
                  setForm(createEmptyForm());
                  setFormError(null);
                  setShowForm(true);
                }
              }}
            >
              {showForm ? (
                <>
                  <X className="w-4 h-4" />
                  Close Form
                </>
              ) : (
                <>
                  <UserPlus className="w-4 h-4" />
                  Add Employee
                </>
              )}
            </Button>
          }
        />

        {actionError && <Alert variant="error">{actionError}</Alert>}

        {lastSeparated && (
          <Alert variant="info">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <span>
                Separated <strong>{lastSeparated.fullName}</strong> ({lastSeparated.employeeNo}).
              </span>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="secondary"
                  loading={reinstateMutation.isPending}
                  onClick={() =>
                    reinstateMutation.mutate({
                      id: lastSeparated.id,
                      status: lastSeparated.previousStatus,
                    })
                  }
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  Undo Separation
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setLastSeparated(null)}
                >
                  Dismiss
                </Button>
              </div>
            </div>
          </Alert>
        )}

        {pendingSeparate && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-[#0b1230]/55 p-4 backdrop-blur-[1px]"
            role="dialog"
            aria-modal="true"
            aria-labelledby="separate-modal-title"
            onClick={() => {
              if (!separateMutation.isPending) setPendingSeparate(null);
            }}
          >
            <div
              className="w-full max-w-md rounded-lg border border-red-200 bg-white shadow-xl overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="h-[3px] w-full bg-[#e21d2d]" aria-hidden="true" />
              <div className="p-5 sm:p-6 space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="w-9 h-9 rounded-md bg-red-50 border border-red-200 text-[#e21d2d] flex items-center justify-center shrink-0">
                      <AlertTriangle className="w-4 h-4" />
                    </div>
                    <div>
                      <h2
                        id="separate-modal-title"
                        className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]"
                      >
                        Confirm Employee Separation
                      </h2>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {pendingSeparate.employee.fullName} ({pendingSeparate.employee.employeeNo})
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    aria-label="Close separation dialog"
                    disabled={separateMutation.isPending}
                    onClick={() => setPendingSeparate(null)}
                    className="p-1 rounded-md text-slate-400 hover:text-slate-700 hover:bg-slate-100 disabled:opacity-50"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                <p className="text-sm text-slate-700 leading-relaxed">
                  Marking this employee as <strong className="text-red-800">SEPARATED</strong> excludes them from
                  active payroll runs and active workforce counts. You can undo this or reinstate them later if needed.
                </p>

                <Field label="Separation Date">
                  <Input
                    type="date"
                    min={pendingSeparate.employee.hireDate}
                    value={pendingSeparate.separationDate}
                    onChange={(e) =>
                      setPendingSeparate((prev) =>
                        prev ? { ...prev, separationDate: e.target.value } : null
                      )
                    }
                  />
                </Field>

                <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#dbe3ee]">
                  <Button
                    variant="secondary"
                    onClick={() => setPendingSeparate(null)}
                    disabled={separateMutation.isPending}
                  >
                    Cancel
                  </Button>
                  <Button
                    variant="danger"
                    loading={separateMutation.isPending}
                    onClick={() =>
                      separateMutation.mutate({
                        employee: pendingSeparate.employee,
                        separationDate: pendingSeparate.separationDate,
                        previousStatus: pendingSeparate.previousStatus,
                      })
                    }
                  >
                    Confirm Separation
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        {showForm && (
          <Panel className="p-5 sm:p-6">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                setFormError(null);
                saveMutation.mutate();
              }}
              className="space-y-4"
            >
              <div className="flex items-center justify-between">
                <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
                  {editingEmployee ? `Edit Employee (${editingEmployee.employeeNo})` : "New Employee"}
                </h2>
              </div>

              {formError && <Alert variant="error">{formError}</Alert>}

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <Field label="Employee No">
                  <Input
                    value={form.employeeNo}
                    onChange={(e) => setForm({ ...form, employeeNo: e.target.value })}
                    placeholder="EMP-001"
                    disabled={Boolean(editingEmployee)}
                    required
                  />
                </Field>
                <Field label="First Name">
                  <Input
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Last Name">
                  <Input
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Email">
                  <Input
                    type="email"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Hire Date">
                  <Input
                    type="date"
                    value={form.hireDate}
                    onChange={(e) => setForm({ ...form, hireDate: e.target.value })}
                    disabled={Boolean(editingEmployee)}
                    required
                  />
                </Field>
                <Field label="Employment Status">
                  <Select
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value as EmploymentStatus })}
                  >
                    <option value="REGULAR">Regular</option>
                    <option value="PROBATIONARY">Probationary</option>
                    <option value="CONTRACTUAL">Contractual</option>
                    <option value="PROJECT_BASED">Project-Based</option>
                    {editingEmployee && <option value="SEPARATED">Separated</option>}
                  </Select>
                </Field>
                <Field label="Pay Basis">
                  <Select
                    value={form.payBasis}
                    onChange={(e) => setForm({ ...form, payBasis: e.target.value as PayBasis })}
                  >
                    <option value="MONTHLY">Monthly</option>
                    <option value="DAILY">Daily</option>
                  </Select>
                </Field>
                <Field label="Base Rate (PHP)">
                  <Input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.baseRate}
                    onChange={(e) => setForm({ ...form, baseRate: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Department">
                  <Select
                    value={form.departmentId}
                    onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
                  >
                    <option value="">Unassigned</option>
                    {departments.data?.map((d: OrgItem) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Position">
                  <Select
                    value={form.positionId}
                    onChange={(e) => setForm({ ...form, positionId: e.target.value })}
                  >
                    <option value="">Unassigned</option>
                    {positions.data?.map((p: OrgItem) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Immediate Supervisor">
                  <Select
                    value={form.supervisorId}
                    onChange={(e) => setForm({ ...form, supervisorId: e.target.value })}
                  >
                    <option value="">Unassigned</option>
                    {supervisorOptions.data?.content
                      .filter((candidate) => candidate.id !== editingEmployee?.id)
                      .map((candidate) => (
                        <option key={candidate.id} value={candidate.id}>
                          {candidate.employeeNo} · {candidate.fullName}
                        </option>
                      ))}
                  </Select>
                </Field>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="secondary" onClick={closeForm}>
                  Cancel
                </Button>
                <Button type="submit" loading={saveMutation.isPending}>
                  {saveMutation.isPending
                    ? "Saving..."
                    : editingEmployee
                      ? "Save Changes"
                      : "Create Employee"}
                </Button>
              </div>
            </form>
          </Panel>
        )}

        <Panel className="overflow-hidden">
          <div className="p-4 border-b border-[#dbe3ee] flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-3 flex-1 min-w-[260px]">
              <div className="relative w-full max-w-sm">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  type="search"
                  placeholder="Search by name, employee no, or email..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>
              <Select
                aria-label="Filter by employment status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-44"
              >
                <option value="ALL">All statuses</option>
                <option value="ACTIVE_ONLY">Active only</option>
                <option value="REGULAR">Regular</option>
                <option value="PROBATIONARY">Probationary</option>
                <option value="CONTRACTUAL">Contractual</option>
                <option value="PROJECT_BASED">Project-Based</option>
                <option value="SEPARATED">Separated</option>
              </Select>
            </div>
            {employees.data && (
              <span className="text-xs text-slate-600 num">
                {employees.data.totalElements} {employees.data.totalElements === 1 ? "employee" : "employees"}
              </span>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-[#edf2f7] text-slate-600 text-xs font-semibold uppercase tracking-wider border-b border-[#dbe3ee]">
                <tr>
                  <th className="px-4 py-2.5">Emp No</th>
                  <th className="px-4 py-2.5">Name</th>
                  <th className="px-4 py-2.5">Email</th>
                  <th className="px-4 py-2.5">Basis</th>
                  <th className="px-4 py-2.5 text-right">Base Rate</th>
                  <th className="px-4 py-2.5">Status</th>
                  <th className="px-4 py-2.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dbe3ee]">
                {employees.isLoading && <TableSkeleton rows={5} cols={7} />}
                {employees.data?.content.map((emp: Employee) => (
                  <tr key={emp.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-700">{emp.employeeNo}</td>
                    <td className="px-4 py-3 font-medium text-[#0b1230]">{emp.fullName}</td>
                    <td className="px-4 py-3 text-slate-600">{emp.email}</td>
                    <td className="px-4 py-3 text-slate-700">{emp.payBasis}</td>
                    <td className="px-4 py-3 num text-right font-medium text-[#0b1230]">
                      {formatPhp(emp.baseRate)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col items-start gap-0.5">
                        <StatusBadge status={emp.status} />
                        {emp.separationDate && (
                          <span className="text-[11px] text-slate-500 num">
                            Separated {emp.separationDate}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {emp.status !== "SEPARATED" ? (
                        <div className="inline-flex items-center gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => startEdit(emp)}
                            className="text-slate-700 hover:bg-slate-100"
                          >
                            <Pencil className="w-3.5 h-3.5" />
                            Edit
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => requestSeparate(emp)}
                            disabled={separateMutation.isPending}
                            className="text-red-700 hover:bg-red-50 hover:text-red-800"
                          >
                            Separate
                          </Button>
                        </div>
                      ) : (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => reinstateMutation.mutate({ id: emp.id, status: "REGULAR" })}
                          disabled={reinstateMutation.isPending}
                          className="text-[#1e5bff] hover:bg-[#eef4ff]"
                        >
                          <RotateCcw className="w-3.5 h-3.5" />
                          Reinstate
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
                {!employees.isLoading && employees.data?.content.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <EmptyState
                        icon={Users}
                        title="No matching employees"
                        description={
                          search || statusFilter !== "ALL"
                            ? "Try clearing or adjusting your search or status filter."
                            : "Add your first employee to start tracking attendance and running payroll."
                        }
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
