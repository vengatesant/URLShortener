export interface UrlResponse {
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  createdAt: string;
  expiresAt: string | null;
  active: boolean;
}

export interface CreateUrlRequest {
  longUrl: string;
  alias?: string | null;
  expiresAt?: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DailyCount {
  day: string;
  clicks: number;
}

export interface ReferrerCount {
  referrer: string;
  clicks: number;
}

export interface UrlStatsResponse {
  shortCode: string;
  totalClicks: number;
  byDay: DailyCount[];
  byReferrer: ReferrerCount[];
}

export interface ApiError {
  error: string;
  message: string;
  details: string[];
  timestamp: string;
}
