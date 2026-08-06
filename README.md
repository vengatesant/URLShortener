# URL Shortener

**Live:** https://url-shortener-zaxs.onrender.com — deployed on Render's free tier (single Docker web service, cold-starts after ~15 min idle) with a free Neon Postgres database. See [Production deployment](#production-deployment) below for how, and what's different from local dev.

A URL shortener built with **React + TypeScript**, **Spring Boot**, and **PostgreSQL** — core create/redirect APIs, per-link click analytics, and the reliability features (caching, rate limiting, soft-delete semantics) called for in the assignment brief.

Full architecture rationale (component diagram, data model, sequence flows, and the trade-off table behind every non-obvious decision) lives in the companion **High-Level Design** document produced earlier in this engagement; the summary below is the condensed version.

## Quick start

**Requires:** Docker + Docker Compose. Nothing else — no local Java, Node, or Postgres install needed.

```bash
docker compose up --build
```

| Service    | URL                                            |
| ---------- | ----------------------------------------------- |
| Frontend   | http://localhost:5173                            |
| Backend API| http://localhost:8080                            |
| Postgres   | localhost:5432 (`urlshortener` / `urlshortener`) |

Open the frontend, paste a URL, shorten it, click **My Links** to see it, click **Stats** to watch clicks land. That's the golden path.

### Without Docker

```bash
# Postgres — bring your own, or just run the db service from compose:
docker compose up db

# Backend (Maven Wrapper included, no local Maven needed)
cd backend
./mvnw spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
npm run dev
```

## Architecture summary

```
Browser (creation) ─▶ React SPA ─▶ POST /api/urls ─┐
                                                     ├─▶ Spring Boot API ─▶ PostgreSQL
Browser (redirect)  ───────────▶ GET /r/{code} ─────┘        │
                                                          Caffeine cache (cache-aside)
```

- **Redirect never touches the SPA.** A shortened link points straight at the API's `/r/{shortCode}`, so the hot, latency-sensitive path doesn't depend on the frontend being up or fast.
- **Short codes are base62-encoded sequence IDs** (`ShortCodeEncoder`), not hashes or random strings — collision-free by construction, no retry logic needed. Custom aliases go through a separate uniqueness check + DB constraint.
- **Cache-aside** (Caffeine, in-process) sits in front of Postgres for the redirect lookup, invalidated explicitly on deactivate.
- **Click tracking is fire-and-forget** (`@Async`) off the redirect thread, so analytics writes never add latency to a 302 response.
- **410 vs 404 is a deliberate distinction:** a deactivated/expired code returns 410 Gone; a code that never existed returns 404. Soft delete (`is_active`) preserves click history instead of losing it on delete.
- **Per-IP rate limiting** on `POST /api/urls` is the abuse control, since there's no auth in this iteration to gate on.

Key modules:

| Layer | Backend | Frontend |
|---|---|---|
| Entry points | `UrlController`, `RedirectController` | `CreateLinkPage`, `MyLinksPage`, `LinkAnalyticsPage` |
| Business logic | `UrlService`, `AnalyticsService`, `ClickTrackingService` | `api/client.ts` (typed fetch wrapper) |
| Data | `UrlRepository`, `ClickEventRepository`, Flyway migrations | `types/api.ts` |
| Cross-cutting | `RateLimitFilter`, `GlobalExceptionHandler`, `CacheConfig` | React Query (server-state cache, mutations) |

## Testing approach

**Backend** (`backend/src/test`):
- Unit tests for the pieces with real logic to get wrong: `ShortCodeEncoderTest` (determinism, no collisions across 100k ids), `LongUrlValidatorTest` (scheme allowlist, open-redirect guard), `UrlServiceTest` (alias conflict, concurrent-alias race, 404 vs 410 branching) — all mocked, no I/O.
- `UrlControllerIntegrationTest` runs the full stack (controller → service → **real Postgres via Testcontainers**) rather than mocks, because the failure modes that matter here — unique-constraint races, whether an async click write actually lands — only show up against a real engine. Covers create→redirect→stats end to end, duplicate alias, invalid scheme, and the deactivate→410 (not 404) distinction.

Run: `cd backend && ./mvnw test` (spins up a Postgres container automatically; requires Docker).

**Frontend** (`frontend/src`):
- `StatusPill.test.tsx` — pure logic (active/expired/deactivated branching).
- `CreateLinkPage.test.tsx` — renders through React Query with a mocked `fetch`, drives the form via Testing Library, asserts both the success path and a server error (409) surfacing through `ErrorNotice`.

Run: `cd frontend && npm test`. Also `npm run lint` and `npm run build` (`tsc -b && vite build`) as the type-checking/quality gate.

**End-to-end validation actually performed** (not just "should work"): since this environment had no Docker, Maven, or Postgres preinstalled, verification meant provisioning real tooling — a portable JDK 21 + Maven, a real (non-Docker) PostgreSQL 18 instance, and a headless Chromium via Playwright — then running the actual jar against the actual database and driving the actual frontend through create → list → click → analytics. Two real bugs surfaced this way and were fixed before this app was called done:

1. **Schema/entity type mismatch**: the Flyway migration declared `click_events.country` as `CHAR(2)`, but Hibernate's schema validator expects `VARCHAR(2)` for a mapped `String` field — the app failed to start under `ddl-auto: validate`. This only surfaces when Flyway and Hibernate both actually run against a real database; it's invisible to a mocked unit test. Fixed in `V1__init_schema.sql`.
2. **Browser-only regex failure**: the alias input's HTML `pattern="[A-Za-z0-9_-]{3,16}"` threw a `SyntaxError` in Chromium's newer unicode-sets (`v`-flag) validation mode — a trailing unescaped `-` in a character class isn't parsed the same way browsers parse it as Java's regex engine does. Only visible by driving a real browser and reading `console --errors`, not from `tsc` or a jsdom test. Fixed by escaping to `[A-Za-z0-9_\-]{3,16}`.

This is also why both a mocked unit-test layer *and* a real-dependency integration layer are kept — each catches a class of bug the other structurally cannot.

## The three assignment scenarios

**Greenfield** — this entire repository. Requirement → HLD (component diagram, data model, API surface, decisions table) → task-by-task implementation (migrations → entities → services → controllers → tests → frontend) → live end-to-end validation.

**Brownfield** — the schema/entity mismatch above (testing approach, item 1) is a genuine brownfield fix performed mid-build: a real defect in already-written code, root-caused (migration type vs. Hibernate's inferred type), fixed at the source (the migration, not papered over with a Hibernate override), and re-validated by rerunning the full startup and the API smoke test rather than assumed fixed.

**Ambiguous requirement** — the brief specifies "core APIs, analytics, and reliability features" but not, for example, whether link creation requires authentication. That ambiguity was resolved explicitly rather than silently assumed: anonymous creation for this iteration, with `created_by` left as a nullable column reserved for an auth pass later, so the decision is reversible without a schema redesign. Same treatment applied to custom-alias support, default expiry behavior, and analytics granularity — each recorded as an explicit assumption rather than an implicit one.

## Risks, trade-offs, and known limitations

| Area | Limitation | Why it's acceptable for this iteration |
|---|---|---|
| Auth | None — creation is anonymous | Not specified in the brief; rate limiting is the abuse control instead |
| Scale | Single Postgres sequence for id generation; in-process (Caffeine) cache | Fine for one instance; documented upgrade path is per-instance id ranges and Redis if this runs on >1 node |
| Rate limiting | Keyed on `request.getRemoteAddr()` | Correct for direct connections; behind a real load balancer this needs `X-Forwarded-For` instead |
| Analytics | Click count, referrer, coarse geo only | No user-level tracking — deliberate, sidesteps PII handling for this scope |
| Custom domains | Not supported | Single shortener domain only |
| CI/CD | Not included | Out of scope for this exercise; setup instructions above cover local + Docker runs |

## Production deployment

**Live at https://url-shortener-zaxs.onrender.com** — Render (free web service) + Neon (free Postgres), chosen over Heroku specifically because both have genuinely free tiers with no time-limited trial.

**Topology:** single app, not the two-container split `docker-compose.yml` uses locally. The root-level `Dockerfile` (distinct from `backend/Dockerfile` and `frontend/Dockerfile`, which remain for local Docker Compose) is a three-stage build: Node builds the frontend, Maven embeds the built frontend as Spring Boot static resources and packages the jar, then a slim JRE image runs it. One process serves the API and the SPA from the same origin, which is also why `SpaForwardController` exists — client-side routes like `/links` need a server-side forward to `index.html` on a hard refresh, since there's no static file at that path.

**Environment variables** (set in Render's dashboard, not in the repo): `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` point at the Neon database; `APP_BASE_URL` is the Render-assigned URL, used to build the `shortUrl` returned by the create endpoint. `PORT` is injected by Render itself — `application.yml`'s `server.port: ${PORT:8080}` picks it up automatically, falling back to 8080 for local/Docker Compose use.

**Caught during this deployment, not before:** running the combined build for the first time surfaced a real bug — `NoResourceFoundException` (Spring's static-resource-handler exception for a genuinely missing path) was being caught by `GlobalExceptionHandler`'s generic `Exception.class` fallback and turned into a 500, so every bad URL on the deployed app would have 500'd instead of 404'd. This only shows up once static-resource serving is actually in play, which local dev's split frontend/backend never exercises. Fixed with a specific `NoResourceFoundException → 404` handler, verified locally (packaged jar + local Postgres) before deploying.

**Known trade-off of the free tier:** the Render service spins down after ~15 minutes idle and cold-starts (10–50s) on the next request — acceptable for a portfolio/review link, not for something that needs to always be warm.

## Project layout

```
backend/    Spring Boot API (Java 21, Maven)
frontend/   React + TypeScript SPA (Vite)
docker-compose.yml   Postgres + backend + frontend, one command
```
