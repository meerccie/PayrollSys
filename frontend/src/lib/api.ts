export type Role = "ADMIN" | "HR" | "SUPERVISOR" | "EMPLOYEE";

export interface AuthUser {
  id: string;
  email: string;
  role: Role;
  employeeId: string | null;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  expiresAt: string;
  user: AuthUser;
}

interface ProblemDetailBody {
  detail?: string;
  title?: string;
  errors?: Record<string, string>;
}

let inMemoryToken: string | null = null;
let refreshPromise: Promise<string | null> | null = null;
let initialAuthPromise: Promise<void> | null = null;

export function setAccessToken(token: string | null): void {
  inMemoryToken = token;
}

export function getAccessToken(): string | null {
  return inMemoryToken;
}

export function setInitialAuthPromise(promise: Promise<void> | null): void {
  initialAuthPromise = promise;
}

export class ApiError extends Error {
  status: number;
  detail: string;
  errors?: Record<string, string>;

  constructor(status: number, detail: string, errors?: Record<string, string>) {
    super(detail);
    this.status = status;
    this.detail = detail;
    this.errors = errors;
  }
}

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async (): Promise<string | null> => {
    try {
      const res = await fetch("/api/v1/auth/refresh", {
        method: "POST",
        credentials: "include",
      });
      if (!res.ok) {
        inMemoryToken = null;
        return null;
      }
      const data = (await res.json()) as AuthResponse;
      inMemoryToken = data.accessToken;
      return data.accessToken;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  if (initialAuthPromise && !path.startsWith("/auth/")) {
    await initialAuthPromise;
  }

  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (inMemoryToken) {
    headers.set("Authorization", `Bearer ${inMemoryToken}`);
  }

  const res = await fetch(`/api/v1${path}`, {
    ...init,
    headers,
    credentials: "include",
  });

  if (res.status === 401 && retry && !path.startsWith("/auth/")) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      return apiFetch<T>(path, init, false);
    }
  }

  if (!res.ok) {
    let detail = `Request failed (${res.status})`;
    let errors: Record<string, string> | undefined;
    try {
      const body = (await res.json()) as ProblemDetailBody;
      detail = body.detail ?? body.title ?? detail;
      errors = body.errors;
    } catch {
      // ignore non-json
    }
    throw new ApiError(res.status, detail, errors);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}
