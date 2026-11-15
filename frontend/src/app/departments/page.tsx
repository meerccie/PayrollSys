"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, Check, Pencil, Plus, Trash2, X } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Alert, Button, EmptyState, Input, PageHeader, Panel } from "@/components/ui";

interface OrgItem {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export default function DepartmentsPage(): React.ReactElement {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [deptCode, setDeptCode] = useState("");
  const [deptName, setDeptName] = useState("");
  const [deptError, setDeptError] = useState<string | null>(null);
  const [editingDeptId, setEditingDeptId] = useState<string | null>(null);
  const [editDeptCode, setEditDeptCode] = useState("");
  const [editDeptName, setEditDeptName] = useState("");
  const [editDeptActive, setEditDeptActive] = useState(true);

  const [posCode, setPosCode] = useState("");
  const [posName, setPosName] = useState("");
  const [posError, setPosError] = useState<string | null>(null);
  const [editingPosId, setEditingPosId] = useState<string | null>(null);
  const [editPosCode, setEditPosCode] = useState("");
  const [editPosName, setEditPosName] = useState("");
  const [editPosActive, setEditPosActive] = useState(true);

  const departments = useQuery({
    queryKey: ["departments"],
    queryFn: () => apiFetch<OrgItem[]>("/departments"),
    enabled: Boolean(user),
  });
  const positions = useQuery({
    queryKey: ["positions"],
    queryFn: () => apiFetch<OrgItem[]>("/positions"),
    enabled: Boolean(user),
  });

  const createDept = useMutation({
    mutationFn: () =>
      apiFetch("/departments", { method: "POST", body: JSON.stringify({ code: deptCode, name: deptName }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["departments"] });
      setDeptCode("");
      setDeptName("");
      setDeptError(null);
    },
    onError: (err: unknown) =>
      setDeptError(err instanceof ApiError ? err.detail : "Failed to add department"),
  });

  const updateDept = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/departments/${id}`, {
        method: "PUT",
        body: JSON.stringify({ code: editDeptCode, name: editDeptName, active: editDeptActive }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["departments"] });
      setEditingDeptId(null);
      setDeptError(null);
    },
    onError: (err: unknown) =>
      setDeptError(err instanceof ApiError ? err.detail : "Failed to update department"),
  });

  const deleteDept = useMutation({
    mutationFn: (id: string) => apiFetch(`/departments/${id}`, { method: "DELETE" }),
    onSuccess: (_data: unknown, id: string) => {
      qc.invalidateQueries({ queryKey: ["departments"] });
      if (editingDeptId === id) {
        setEditingDeptId(null);
      }
      setDeptError(null);
    },
    onError: (err: unknown) =>
      setDeptError(
        err instanceof ApiError
          ? err.detail
          : "Cannot delete department while employees are assigned to it"
      ),
  });

  const createPos = useMutation({
    mutationFn: () =>
      apiFetch("/positions", { method: "POST", body: JSON.stringify({ code: posCode, name: posName }) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["positions"] });
      setPosCode("");
      setPosName("");
      setPosError(null);
    },
    onError: (err: unknown) =>
      setPosError(err instanceof ApiError ? err.detail : "Failed to add position"),
  });

  const updatePos = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/positions/${id}`, {
        method: "PUT",
        body: JSON.stringify({ code: editPosCode, name: editPosName, active: editPosActive }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["positions"] });
      setEditingPosId(null);
      setPosError(null);
    },
    onError: (err: unknown) =>
      setPosError(err instanceof ApiError ? err.detail : "Failed to update position"),
  });

  const deletePos = useMutation({
    mutationFn: (id: string) => apiFetch(`/positions/${id}`, { method: "DELETE" }),
    onSuccess: (_data: unknown, id: string) => {
      qc.invalidateQueries({ queryKey: ["positions"] });
      if (editingPosId === id) {
        setEditingPosId(null);
      }
      setPosError(null);
    },
    onError: (err: unknown) =>
      setPosError(
        err instanceof ApiError
          ? err.detail
          : "Cannot delete position while employees are assigned to it"
      ),
  });

  function beginEditDept(item: OrgItem): void {
    setDeptError(null);
    setEditingDeptId(item.id);
    setEditDeptCode(item.code);
    setEditDeptName(item.name);
    setEditDeptActive(item.active);
  }

  function beginEditPos(item: OrgItem): void {
    setPosError(null);
    setEditingPosId(item.id);
    setEditPosCode(item.code);
    setEditPosName(item.name);
    setEditPosActive(item.active);
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          title="Departments & Positions"
          description="Define organizational units and job titles assigned to employee profiles"
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Panel className="p-5 space-y-4">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
              Departments
            </h2>
            {deptError && <Alert variant="error">{deptError}</Alert>}
            <form
              onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
                e.preventDefault();
                createDept.mutate();
              }}
              className="flex flex-wrap sm:flex-nowrap gap-2"
            >
              <Input
                placeholder="Code"
                value={deptCode}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDeptCode(e.target.value)}
                required
                className="w-28 shrink-0 font-mono text-xs"
              />
              <Input
                placeholder="Department name"
                value={deptName}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDeptName(e.target.value)}
                required
                className="flex-1 min-w-[140px]"
              />
              <Button type="submit" loading={createDept.isPending} className="shrink-0">
                <Plus className="w-4 h-4" />
                Add
              </Button>
            </form>
            {departments.isLoading ? (
              <div className="space-y-2 py-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-8 rounded bg-slate-100 animate-pulse" />
                ))}
              </div>
            ) : departments.data?.length === 0 ? (
              <EmptyState
                icon={Building2}
                title="No departments yet"
                description="Add a department code and name above to organize your workforce."
              />
            ) : (
              <ul className="divide-y divide-[#dbe3ee] text-sm">
                {departments.data?.map((d: OrgItem) => {
                  const isEditing = editingDeptId === d.id;
                  return (
                    <li key={d.id} className="py-2.5">
                      {isEditing ? (
                        <form
                          onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
                            e.preventDefault();
                            updateDept.mutate(d.id);
                          }}
                          className="flex flex-wrap sm:flex-nowrap items-center gap-2"
                        >
                          <Input
                            aria-label="Edit department code"
                            value={editDeptCode}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                              setEditDeptCode(e.target.value)
                            }
                            required
                            className="w-24 shrink-0 font-mono text-xs"
                          />
                          <Input
                            aria-label="Edit department name"
                            value={editDeptName}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                              setEditDeptName(e.target.value)
                            }
                            required
                            className="flex-1 min-w-[120px]"
                          />
                          <div className="inline-flex items-center gap-1 shrink-0">
                            <Button
                              type="submit"
                              size="sm"
                              loading={updateDept.isPending}
                              aria-label="Save department"
                            >
                              <Check className="w-3.5 h-3.5" />
                              Save
                            </Button>
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              onClick={() => setEditingDeptId(null)}
                              aria-label="Cancel editing department"
                            >
                              <X className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[#0b1230] min-w-0 truncate">
                            <span className="font-mono text-xs text-slate-500 mr-2.5">{d.code}</span>
                            {d.name}
                          </span>
                          <div className="inline-flex items-center gap-1 shrink-0">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => beginEditDept(d)}
                              className="text-slate-700 hover:bg-slate-100"
                            >
                              <Pencil className="w-3.5 h-3.5" />
                              Edit
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => deleteDept.mutate(d.id)}
                              disabled={deleteDept.isPending}
                              className="text-red-700 hover:bg-red-50 hover:text-red-800"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              Delete
                            </Button>
                          </div>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </Panel>

          <Panel className="p-5 space-y-4">
            <h2 className="font-display text-sm font-bold uppercase tracking-wider text-[#0b1230]">
              Positions
            </h2>
            {posError && <Alert variant="error">{posError}</Alert>}
            <form
              onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
                e.preventDefault();
                createPos.mutate();
              }}
              className="flex flex-wrap sm:flex-nowrap gap-2"
            >
              <Input
                placeholder="Code"
                value={posCode}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPosCode(e.target.value)}
                required
                className="w-28 shrink-0 font-mono text-xs"
              />
              <Input
                placeholder="Position title"
                value={posName}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPosName(e.target.value)}
                required
                className="flex-1 min-w-[140px]"
              />
              <Button type="submit" loading={createPos.isPending} className="shrink-0">
                <Plus className="w-4 h-4" />
                Add
              </Button>
            </form>
            {positions.isLoading ? (
              <div className="space-y-2 py-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-8 rounded bg-slate-100 animate-pulse" />
                ))}
              </div>
            ) : positions.data?.length === 0 ? (
              <EmptyState
                icon={Building2}
                title="No positions yet"
                description="Add a position code and title above to assign to employees."
              />
            ) : (
              <ul className="divide-y divide-[#dbe3ee] text-sm">
                {positions.data?.map((p: OrgItem) => {
                  const isEditing = editingPosId === p.id;
                  return (
                    <li key={p.id} className="py-2.5">
                      {isEditing ? (
                        <form
                          onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
                            e.preventDefault();
                            updatePos.mutate(p.id);
                          }}
                          className="flex flex-wrap sm:flex-nowrap items-center gap-2"
                        >
                          <Input
                            aria-label="Edit position code"
                            value={editPosCode}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                              setEditPosCode(e.target.value)
                            }
                            required
                            className="w-24 shrink-0 font-mono text-xs"
                          />
                          <Input
                            aria-label="Edit position name"
                            value={editPosName}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                              setEditPosName(e.target.value)
                            }
                            required
                            className="flex-1 min-w-[120px]"
                          />
                          <div className="inline-flex items-center gap-1 shrink-0">
                            <Button
                              type="submit"
                              size="sm"
                              loading={updatePos.isPending}
                              aria-label="Save position"
                            >
                              <Check className="w-3.5 h-3.5" />
                              Save
                            </Button>
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              onClick={() => setEditingPosId(null)}
                              aria-label="Cancel editing position"
                            >
                              <X className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[#0b1230] min-w-0 truncate">
                            <span className="font-mono text-xs text-slate-500 mr-2.5">{p.code}</span>
                            {p.name}
                          </span>
                          <div className="inline-flex items-center gap-1 shrink-0">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => beginEditPos(p)}
                              className="text-slate-700 hover:bg-slate-100"
                            >
                              <Pencil className="w-3.5 h-3.5" />
                              Edit
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => deletePos.mutate(p.id)}
                              disabled={deletePos.isPending}
                              className="text-red-700 hover:bg-red-50 hover:text-red-800"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              Delete
                            </Button>
                          </div>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </Panel>
        </div>
      </div>
    </AppShell>
  );
}
