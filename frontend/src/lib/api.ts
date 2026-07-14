const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export interface AuthResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  companyId: number | null;
}

export interface MessageResponse {
  id: number;
  role: string;
  content: string;
  createdAt: string;
}

export interface ChatSessionResponse {
  id: number;
  status: string;
  customerEmail: string | null;
  customerName: string | null;
  resolved: boolean;
  createdAt: string;
  messages: MessageResponse[];
}

export interface AnalyticsResponse {
  totalConversations: number;
  openTickets: number;
  resolvedTickets: number;
  aiResolutionRate: number;
  averageResponseTimeMs: number;
  customerSatisfaction: number;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "Request failed" }));
    throw new Error(error.message || "Request failed");
  }

  return response.json();
}

export const api = {
  register: (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    companyName: string;
  }) => request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(data) }),

  login: (data: { email: string; password: string }) =>
    request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(data) }),

  getAnalytics: (companyId: number) =>
    request<AnalyticsResponse>(`/analytics?companyId=${companyId}`),

  getSessions: (companyId: number) =>
    request<ChatSessionResponse[]>(`/chat/sessions?companyId=${companyId}`),

  startWidgetSession: (companyId: number, customerEmail?: string, customerName?: string) => {
    const params = new URLSearchParams({ companyId: String(companyId) });
    if (customerEmail) params.set("customerEmail", customerEmail);
    if (customerName) params.set("customerName", customerName);
    return request<ChatSessionResponse>(`/chat/widget/sessions?${params}`, { method: "POST" });
  },

  sendWidgetMessage: (sessionId: number, content: string) =>
    request<ChatSessionResponse>(`/chat/widget/sessions/${sessionId}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),
};
