import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import ErrorNotice from "../components/ErrorNotice";
import StatusPill from "../components/StatusPill";

const PAGE_SIZE = 20;

export default function MyLinksPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const linksQuery = useQuery({
    queryKey: ["urls", page],
    queryFn: () => api.listUrls(page, PAGE_SIZE),
  });

  const deactivateMutation = useMutation({
    mutationFn: (shortCode: string) => api.deactivateUrl(shortCode),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["urls"] }),
  });

  return (
    <section>
      <h1>My links</h1>

      {linksQuery.isLoading && <p className="muted">Loading…</p>}
      <ErrorNotice error={linksQuery.error} />

      {linksQuery.data && linksQuery.data.items.length === 0 && (
        <p className="muted">
          No links yet. <Link to="/">Create your first one.</Link>
        </p>
      )}

      {linksQuery.data && linksQuery.data.items.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Short code</th>
                <th>Destination</th>
                <th>Created</th>
                <th>Status</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {linksQuery.data.items.map((link) => (
                <tr key={link.shortCode}>
                  <td>
                    <code>{link.shortCode}</code>
                  </td>
                  <td className="truncate" title={link.longUrl}>
                    {link.longUrl}
                  </td>
                  <td className="num">{new Date(link.createdAt).toLocaleDateString()}</td>
                  <td>
                    <StatusPill link={link} />
                  </td>
                  <td className="actions">
                    <Link to={`/links/${link.shortCode}/stats`}>Stats</Link>
                    {link.active && (
                      <button
                        type="button"
                        className="link-button"
                        disabled={deactivateMutation.isPending}
                        onClick={() => deactivateMutation.mutate(link.shortCode)}
                      >
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {linksQuery.data && linksQuery.data.totalPages > 1 && (
        <div className="pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span className="muted">
            Page {page + 1} of {linksQuery.data.totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= linksQuery.data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
