"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { api, type AuthResponse, type UserProfile } from "@/lib/api";
import { clearAuth, getStoredUser, getToken, saveAuth } from "@/lib/auth-storage";

interface AuthContextValue {
  user: AuthResponse | null;
  profile: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    companyName: string;
  }) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function initialLoadingState(): boolean {
  if (typeof window === "undefined") return true;
  return Boolean(getToken());
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthResponse | null>(() => getStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(initialLoadingState);

  const logout = useCallback(() => {
    clearAuth();
    setUser(null);
    setProfile(null);
    router.push("/login");
  }, [router]);

  const refreshUser = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      setProfile(null);
      return;
    }

    try {
      const me = await api.getMe(token);
      const nextUser: AuthResponse = {
        token,
        email: me.email,
        firstName: me.firstName,
        lastName: me.lastName,
        role: me.role,
        companyId: me.companyId,
      };
      saveAuth(nextUser);
      setUser(nextUser);
      setProfile(me);
    } catch {
      logout();
    }
  }, [logout]);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      return;
    }

    api
      .getMe(token)
      .then((me) => {
        const nextUser: AuthResponse = {
          token,
          email: me.email,
          firstName: me.firstName,
          lastName: me.lastName,
          role: me.role,
          companyId: me.companyId,
        };
        saveAuth(nextUser);
        setUser(nextUser);
        setProfile(me);
      })
      .catch(() => {
        clearAuth();
        setUser(null);
        setProfile(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await api.login({ email, password });
      saveAuth(response);
      setUser(response);
      const me = await api.getMe(response.token);
      setProfile(me);
      router.push("/dashboard");
    },
    [router]
  );

  const register = useCallback(
    async (data: {
      email: string;
      password: string;
      firstName: string;
      lastName: string;
      companyName: string;
    }) => {
      const response = await api.register(data);
      saveAuth(response);
      setUser(response);
      const me = await api.getMe(response.token);
      setProfile(me);
      router.push("/dashboard");
    },
    [router]
  );

  const value = useMemo(
    () => ({ user, profile, loading, login, register, logout, refreshUser }),
    [user, profile, loading, login, register, logout, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
