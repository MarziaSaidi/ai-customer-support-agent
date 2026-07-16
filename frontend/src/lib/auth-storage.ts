import type { AuthResponse } from "@/lib/api";

const TOKEN_KEY = "token";
const USER_KEY = "user";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): AuthResponse | null {
  if (typeof window === "undefined") return null;
  const stored = localStorage.getItem(USER_KEY);
  if (!stored) return null;
  try {
    return JSON.parse(stored) as AuthResponse;
  } catch {
    return null;
  }
}

export function saveAuth(response: AuthResponse): void {
  localStorage.setItem(TOKEN_KEY, response.token);
  localStorage.setItem(USER_KEY, JSON.stringify(response));
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isAuthenticated(): boolean {
  return Boolean(getToken() && getStoredUser());
}
