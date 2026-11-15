"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { apiFetch, AuthResponse, AuthUser, setAccessToken, setInitialAuthPromise } from "./api";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const initPromise = (async (): Promise<void> => {
      try {
        const res = await apiFetch<AuthResponse>("/auth/refresh", { method: "POST" }, false);
        if (active) {
          setAccessToken(res.accessToken);
          setUser(res.user);
        }
      } catch {
        if (active) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        setInitialAuthPromise(null);
        if (active) setLoading(false);
      }
    })();
    setInitialAuthPromise(initPromise);
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<void> => {
    const res = await apiFetch<AuthResponse>(
      "/auth/login",
      { method: "POST", body: JSON.stringify({ email, password }) },
      false
    );
    setAccessToken(res.accessToken);
    setUser(res.user);
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await apiFetch<void>("/auth/logout", { method: "POST" }, false);
    } finally {
      setAccessToken(null);
      setUser(null);
      queryClient.clear();
    }
  }, [queryClient]);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
