import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import ErrorNotice from "../components/ErrorNotice";

export default function LinkAnalyticsPage() {
  const { shortCode = "" } = useParams();

  const statsQuery = useQuery({
    queryKey: ["stats", shortCode],
    queryFn: () => api.getStats(shortCode),
  });

  return (
    <section>
      <Link to="/links" className="back-link">
        &larr; My links
      </Link>
      <h1>
        Analytics for <code>{shortCode}</code>
      </h1>

      {statsQuery.isLoading && <p className="muted">Loading…</p>}
      <ErrorNotice error={statsQuery.error} />

      {statsQuery.data && (
        <>
          <div className="stat-tile">
            <span className="stat-label">Total clicks</span>
            <span className="stat-value">{statsQuery.data.totalClicks}</span>
          </div>

          <h2>Clicks by day</h2>
          {statsQuery.data.byDay.length === 0 ? (
            <p className="muted">No clicks recorded yet.</p>
          ) : (
            <BarChart
              className="chart-day"
              items={statsQuery.data.byDay.map((d) => ({
                label: new Date(d.day).toLocaleDateString(undefined, { month: "short", day: "numeric" }),
                value: d.clicks,
              }))}
            />
          )}

          <h2>Top referrers</h2>
          {statsQuery.data.byReferrer.length === 0 ? (
            <p className="muted">No referrer data yet.</p>
          ) : (
            <BarChart
              className="chart-referrer"
              items={statsQuery.data.byReferrer.map((r) => ({ label: r.referrer, value: r.clicks }))}
            />
          )}
        </>
      )}
    </section>
  );
}

function BarChart({ items, className }: { items: { label: string; value: number }[]; className: string }) {
  const max = Math.max(...items.map((i) => i.value), 1);
  return (
    <div className={`bar-chart ${className}`} role="img" aria-label="Bar chart of click counts">
      {items.map((item) => (
        <div className="bar-row" key={item.label} title={`${item.label}: ${item.value} clicks`}>
          <span className="bar-label">{item.label}</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(item.value / max) * 100}%` }} />
          </div>
          <span className="bar-value">{item.value}</span>
        </div>
      ))}
    </div>
  );
}
