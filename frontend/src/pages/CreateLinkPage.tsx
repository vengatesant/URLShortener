import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import type { UrlResponse } from "../types/api";
import ErrorNotice from "../components/ErrorNotice";

export default function CreateLinkPage() {
  const [longUrl, setLongUrl] = useState("");
  const [alias, setAlias] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [created, setCreated] = useState<UrlResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const mutation = useMutation({
    mutationFn: () =>
      api.createUrl({
        longUrl,
        alias: alias.trim() === "" ? null : alias.trim(),
        expiresAt: expiresAt === "" ? null : new Date(expiresAt).toISOString(),
      }),
    onSuccess: (result) => {
      setCreated(result);
      setCopied(false);
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setCreated(null);
    mutation.mutate();
  }

  function handleCopy() {
    if (!created) return;
    navigator.clipboard.writeText(created.shortUrl).then(() => setCopied(true));
  }

  return (
    <section className="page-narrow">
      <h1>Shorten a link</h1>
      <p className="lede">Paste a long URL, optionally pick your own code, and get a short one back.</p>

      <form onSubmit={handleSubmit} className="card form">
        <label htmlFor="longUrl">Long URL</label>
        <input
          id="longUrl"
          type="text"
          placeholder="https://example.com/a/very/long/path"
          value={longUrl}
          onChange={(e) => setLongUrl(e.target.value)}
          required
        />

        <div className="form-row">
          <div>
            <label htmlFor="alias">Custom alias (optional)</label>
            <input
              id="alias"
              type="text"
              placeholder="my-link"
              value={alias}
              onChange={(e) => setAlias(e.target.value)}
              pattern="[A-Za-z0-9_\-]{3,16}"
              title="3-16 characters: letters, numbers, - or _"
            />
          </div>
          <div>
            <label htmlFor="expiresAt">Expires (optional)</label>
            <input
              id="expiresAt"
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
            />
          </div>
        </div>

        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Shortening…" : "Shorten"}
        </button>
      </form>

      <ErrorNotice error={mutation.error} />

      {created && (
        <div className="card result" role="status">
          <span className="eyebrow">Your short link</span>
          <div className="result-row">
            <code>{created.shortUrl}</code>
            <button type="button" onClick={handleCopy} className="secondary">
              {copied ? "Copied" : "Copy"}
            </button>
          </div>
          <p className="muted">
            Redirects to <span title={created.longUrl}>{truncate(created.longUrl, 60)}</span>
          </p>
          <Link to={`/links/${created.shortCode}/stats`}>View analytics &rarr;</Link>
        </div>
      )}
    </section>
  );
}

function truncate(value: string, max: number): string {
  return value.length > max ? `${value.slice(0, max)}…` : value;
}
