import type {
  ApiError,
  CreateUrlRequest,
  PageResponse,
  UrlResponse,
  UrlStatsResponse,
} from "../types/api";

// Empty string -> same-origin relative requests, which is what production wants once the
// backend serves the built frontend itself (see the root Dockerfile). Local dev overrides
// this via VITE_API_BASE_URL in frontend/.env (see .env.example).
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiClientError extends Error {
  status: number;
  body: ApiError;

  constructor(status: number, body: ApiError) {
    super(body.message);
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
  });

  if (!res.ok) {
    const fallback: ApiError = {
      error: "UNKNOWN",
      message: res.statusText,
      details: [],
      timestamp: new Date().toISOString(),
    };
    const body = await res.json().catch(() => fallback);
    throw new ApiClientError(res.status, body);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export const api = {
  createUrl: (payload: CreateUrlRequest) =>
    request<UrlResponse>("/api/urls", { method: "POST", body: JSON.stringify(payload) }),

  listUrls: (page = 0, size = 20) =>
    request<PageResponse<UrlResponse>>(`/api/urls?page=${page}&size=${size}`),

  getUrl: (shortCode: string) => request<UrlResponse>(`/api/urls/${shortCode}`),

  getStats: (shortCode: string) => request<UrlStatsResponse>(`/api/urls/${shortCode}/stats`),

  deactivateUrl: (shortCode: string) =>
    request<void>(`/api/urls/${shortCode}`, { method: "DELETE" }),
};
