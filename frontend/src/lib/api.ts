const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export interface AuthResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  companyId: number | null;
}

export interface UserProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  companyId: number | null;
  companyName: string | null;
  emailVerified: boolean;
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

export interface Company {
  id: number;
  name: string;
  slug: string;
  website: string | null;
  aiSystemPrompt: string | null;
  createdAt: string;
}

export interface CompanyMember {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  joinedAt: string;
}

export interface DocumentItem {
  id: number;
  title: string;
  filename: string;
  type: string;
  fileUrl: string;
  processed: boolean;
  createdAt: string;
}

export interface DocumentChunkMatch {
  chunkId: number;
  documentId: number;
  documentTitle: string;
  content: string;
  chunkIndex: number;
  score: number;
}

export interface RagSource {
  documentId: number;
  documentTitle: string;
  excerpt: string;
  score: number;
}

export interface RagAnswer {
  answer: string;
  sources: RagSource[];
}

export class ApiError extends Error {
  status: number;
  errors?: Record<string, string>;

  constructor(message: string, status: number, errors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errors = errors;
  }
}

type RequestOptions = RequestInit & {
  token?: string | null;
};

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, ...init } = options;
  const authToken = token !== undefined ? token : (typeof window !== "undefined" ? localStorage.getItem("token") : null);

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init.headers as Record<string, string>),
  };

  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: "Request failed" }));
    throw new ApiError(body.message || "Request failed", response.status, body.errors);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

async function uploadRequest<T>(path: string, formData: FormData): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  const headers: Record<string, string> = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: "Upload failed" }));
    throw new ApiError(body.message || "Upload failed", response.status, body.errors);
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
  }) => request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(data), token: null }),

  login: (data: { email: string; password: string }) =>
    request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(data), token: null }),

  getMe: (token?: string) => request<UserProfile>("/auth/me", { token }),

  getCompany: (companyId: number) => request<Company>(`/companies/${companyId}`),

  updateCompany: (companyId: number, data: { name?: string; website?: string; aiSystemPrompt?: string }) =>
    request<Company>(`/companies/${companyId}`, { method: "PUT", body: JSON.stringify(data) }),

  getCompanyMembers: (companyId: number) =>
    request<CompanyMember[]>(`/companies/${companyId}/members`),

  addCompanyMember: (companyId: number, data: { email: string; role: string }) =>
    request<CompanyMember>(`/companies/${companyId}/members`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  removeCompanyMember: (companyId: number, userId: number) =>
    request<void>(`/companies/${companyId}/members/${userId}`, { method: "DELETE" }),

  getDocuments: (companyId: number) =>
    request<DocumentItem[]>(`/documents?companyId=${companyId}`),

  uploadDocument: (companyId: number, title: string, type: string, file: File) => {
    const formData = new FormData();
    formData.append("companyId", String(companyId));
    formData.append("title", title);
    formData.append("type", type);
    formData.append("file", file);
    return uploadRequest<DocumentItem>("/documents", formData);
  },

  deleteDocument: (documentId: number) =>
    request<void>(`/documents/${documentId}`, { method: "DELETE" }),

  searchDocuments: (companyId: number, query: string, limit = 5) =>
    request<DocumentChunkMatch[]>("/documents/search", {
      method: "POST",
      body: JSON.stringify({ companyId, query, limit }),
    }),

  askKnowledge: (companyId: number, question: string) =>
    request<RagAnswer>("/knowledge/ask", {
      method: "POST",
      body: JSON.stringify({ companyId, question }),
    }),

  getAnalytics: (companyId: number) =>
    request<AnalyticsResponse>(`/analytics?companyId=${companyId}`),

  getSessions: (companyId: number) =>
    request<ChatSessionResponse[]>(`/chat/sessions?companyId=${companyId}`),

  startWidgetSession: (companyId: number, customerEmail?: string, customerName?: string) => {
    const params = new URLSearchParams({ companyId: String(companyId) });
    if (customerEmail) params.set("customerEmail", customerEmail);
    if (customerName) params.set("customerName", customerName);
    return request<ChatSessionResponse>(`/chat/widget/sessions?${params}`, { method: "POST", token: null });
  },

  sendWidgetMessage: (sessionId: number, content: string) =>
    request<ChatSessionResponse>(`/chat/widget/sessions/${sessionId}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
      token: null,
    }),
};
