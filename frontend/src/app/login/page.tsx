"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api";
import { Alert, Button, Field, Input, Panel, TransitStripe } from "@/components/ui";

export default function LoginPage(): React.ReactElement {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      router.replace("/dashboard");
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : "Invalid email or password. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#f4f7fb] p-4">
      <Panel className="w-full max-w-sm overflow-hidden border-[#cbd5e1]">
        <div className="bg-[#0b1230] text-white">
          <TransitStripe />
          <div className="px-7 py-5 flex items-center gap-3 border-b border-[#1e3252]">
            <span className="w-8 h-8 rounded bg-[#1e5bff] text-white font-display text-xs font-bold flex items-center justify-center tracking-tight shrink-0">
              PH
            </span>
            <div>
              <h1 className="font-display text-lg font-bold tracking-tight text-white leading-tight">
                PH PAYROLL
              </h1>
              <p className="text-[11px] uppercase tracking-[0.14em] text-slate-400">
                Operations Dispatch Console
              </p>
            </div>
          </div>
        </div>

        <div className="p-7">
          {error && (
            <div className="mb-4">
              <Alert variant="error">{error}</Alert>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Email">
              <Input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@example.com"
              />
            </Field>
            <Field label="Password">
              <Input
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
              />
            </Field>
            <Button type="submit" loading={submitting} className="w-full mt-2">
              {submitting ? "Signing in..." : "Sign in"}
            </Button>
          </form>
        </div>
      </Panel>
    </div>
  );
}
