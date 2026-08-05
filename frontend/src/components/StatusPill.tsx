import type { UrlResponse } from "../types/api";

function statusOf(link: Pick<UrlResponse, "active" | "expiresAt">): {
  label: string;
  tone: "good" | "warning" | "critical";
} {
  if (!link.active) return { label: "Deactivated", tone: "critical" };
  if (link.expiresAt && new Date(link.expiresAt) < new Date()) {
    return { label: "Expired", tone: "warning" };
  }
  return { label: "Active", tone: "good" };
}

export default function StatusPill({ link }: { link: Pick<UrlResponse, "active" | "expiresAt"> }) {
  const { label, tone } = statusOf(link);
  return <span className={`pill pill-${tone}`}>{label}</span>;
}
