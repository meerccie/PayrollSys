"use client";

import React, { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { apiFetch, ApiError, Role } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Alert, Button, Field, Input, PageHeader, Panel, Select, StatusBadge } from "@/components/ui";

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

interface EmployeeOption {
  id: string;
  employeeNo: string;
  fullName: string;
  email: string;
  status: string;
}

export default function SettingsPage(): React.ReactElement {
  const { user } = useAuth();
  const qc = useQueryClient();
  const isAdmin = user?.role === "ADMIN";

  const configQuery = useQuery({
    queryKey: ["company-config"],
    queryFn: () => apiFetch<CompanyConfig>("/settings/company"),
    enabled: Boolean(user),
  });

  const usersQuery = useQuery({
    queryKey: ["users"],
    queryFn: () => apiFetch<UserItem[]>("/users"),
    enabled: Boolean(user) && isAdmin,
  });

  const employeesQuery = useQuery({
    queryKey: ["employees-all"],
    queryFn: () => apiFetch<{ content: EmployeeOption[] }>("/employees?size=100"),
    enabled: Boolean(user) && isAdmin,
  });

  const [companyName, setCompanyName] = useState("");
  const [annualWorkDays, setAnnualWorkDays] = useState(261);
  const [statutorySchedule, setStatutorySchedule] = useState<"SPLIT" | "FIRST_CUTOFF" | "SECOND_CUTOFF">("SPLIT");
  const [savedNotice, setSavedNotice] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newRole, setNewRole] = useState<Role>("HR");
  const [newEmployeeId, setNewEmployeeId] = useState("");
  const [userError, setUserError] = useState<string | null>(null);
  const [userNotice, setUserNotice] = useState<string | null>(null);
  const [draftLinks, setDraftLinks] = useState<Record<string, string>>({});
  const [draftRoles, setDraftRoles] = useState<Record<string, Role>>({});

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
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["company-config"] });
      setSavedNotice(true);
    },
  });

  const createUser = useMutation({
    mutationFn: () =>
      apiFetch("/users", {
        method: "POST",
        body: JSON.stringify({
          email: newEmail,
          password: newPassword,
          role: newRole,
          employeeId: newEmployeeId || null,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setNewEmail("");
      setNewPassword("");
      setNewEmployeeId("");
      setUserError(null);
      setUserNotice("User account created.");
    },
    onError: (err: unknown) => {
      setUserNotice(null);
      setUserError(err instanceof ApiError ? err.detail : "Failed to create user account");
    },
  });

  const updateUserLink = useMutation({
    mutationFn: ({ target, role, employeeId }: { target: UserItem; role: Role; employeeId: string | null }) =>
      apiFetch(`/users/${target.id}`, {
        method: "PUT",
        body: JSON.stringify({
          role,
          employeeId,
          active: target.active,
        }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setUserError(null);
      setUserNotice("User account updated.");
    },
    onError: (err: unknown) => {
      setUserNotice(null);
      setUserError(err instanceof ApiError ? err.detail : "Failed to update user account");
    },
  });

  const employeeList = employeesQuery.data?.content ?? [];

  return (
    <AppShell>
      <div className="space-y-6 max-w-4xl">
        <PageHeader
          title="Settings"
          description="Configure company payroll defaults and manage system user accounts"
        />

        <Panel className="p-5 sm:p-6">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setSavedNotice(false);
              saveConfig.mutate();
            }}
            className="space-y-4"
          >
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
              Company & Payroll Defaults
            </h2>
            {savedNotice && <Alert variant="success">Company settings saved.</Alert>}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-3">
                <Field label="Company Name">
                  <Input
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    disabled={!isAdmin}
                  />
                </Field>
              </div>
              <Field label="Annual Work Days">
                <Input
                  type="number"
                  value={annualWorkDays}
                  onChange={(e) => setAnnualWorkDays(Number(e.target.value))}
                  disabled={!isAdmin}
                  className="num"
                />
              </Field>
              <div className="md:col-span-2">
                <Field label="Semi-Monthly Statutory Schedule">
                  <Select
                    value={statutorySchedule}
                    onChange={(e) => setStatutorySchedule(e.target.value as typeof statutorySchedule)}
                    disabled={!isAdmin}
                  >
                    <option value="SPLIT">Split 50/50 across both cutoffs</option>
                    <option value="FIRST_CUTOFF">All on 1st cutoff</option>
                    <option value="SECOND_CUTOFF">All on 2nd cutoff</option>
                  </Select>
                </Field>
              </div>
            </div>

            {isAdmin && (
              <div className="pt-1">
                <Button type="submit" loading={saveConfig.isPending}>
                  {saveConfig.isPending ? "Saving..." : "Save Settings"}
                </Button>
              </div>
            )}
          </form>
        </Panel>

        {isAdmin && (
          <Panel className="p-5 sm:p-6 space-y-4">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
              User Accounts
            </h2>
            {userError && <Alert variant="error">{userError}</Alert>}
            {userNotice && <Alert variant="success">{userNotice}</Alert>}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createUser.mutate();
              }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2 items-center"
            >
              <Input
                type="email"
                required
                placeholder="Email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                className="lg:col-span-1"
              />
              <Input
                type="password"
                required
                minLength={8}
                placeholder="Password (8+ chars)"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              <Select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as Role)}
              >
                <option value="HR">HR</option>
                <option value="SUPERVISOR">SUPERVISOR</option>
                <option value="ADMIN">ADMIN</option>
                <option value="EMPLOYEE">EMPLOYEE</option>
              </Select>
              <Select
                aria-label="Link employee profile"
                value={newEmployeeId}
                onChange={(e) => setNewEmployeeId(e.target.value)}
              >
                <option value="">No employee link</option>
                {employeeList.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.employeeNo} · {emp.fullName}
                  </option>
                ))}
              </Select>
              <Button type="submit" loading={createUser.isPending} className="w-full">
                Create
              </Button>
            </form>

            <ul className="divide-y divide-[#dbe3ee] text-sm">
              {usersQuery.data?.map((u) => {
                const selectedLink = draftLinks[u.id] ?? u.employeeId ?? "";
                const selectedRole = draftRoles[u.id] ?? u.role;
                const hasChanged = selectedLink !== (u.employeeId ?? "") || selectedRole !== u.role;
                return (
                  <li key={u.id} className="py-3 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span className="text-[#0b1230] font-medium truncate">{u.email}</span>
                      <StatusBadge status={u.role} />
                      {(u.role === "EMPLOYEE" || u.role === "SUPERVISOR") && !u.employeeId && (
                        <StatusBadge status="PENDING" />
                      )}
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <Select
                        aria-label={`Role for ${u.email}`}
                        value={selectedRole}
                        onChange={(e) =>
                          setDraftRoles((prev) => ({ ...prev, [u.id]: e.target.value as Role }))
                        }
                        className="w-36"
                      >
                        <option value="ADMIN">ADMIN</option>
                        <option value="HR">HR</option>
                        <option value="SUPERVISOR">SUPERVISOR</option>
                        <option value="EMPLOYEE">EMPLOYEE</option>
                      </Select>
                      <Select
                        aria-label={`Employee profile for ${u.email}`}
                        value={selectedLink}
                        onChange={(e) =>
                          setDraftLinks((prev) => ({ ...prev, [u.id]: e.target.value }))
                        }
                        className="w-56"
                      >
                        <option value="">No employee linked</option>
                        {employeeList.map((emp) => (
                          <option key={emp.id} value={emp.id}>
                            {emp.employeeNo} · {emp.fullName}
                          </option>
                        ))}
                      </Select>
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={!hasChanged || updateUserLink.isPending}
                        onClick={() =>
                          updateUserLink.mutate({
                            target: u,
                            role: selectedRole,
                            employeeId: selectedLink || null,
                          })
                        }
                      >
                        Save
                      </Button>
                    </div>
                  </li>
                );
              })}
            </ul>
          </Panel>
        )}
      </div>
    </AppShell>
  );
}
