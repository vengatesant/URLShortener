# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A URL shortener: React + TypeScript SPA, Spring Boot (Java 21) API, PostgreSQL. Monorepo with `backend/` and `frontend/` as independent, separately-buildable projects, tied together by `docker-compose.yml`. Full architecture rationale (decisions, trade-offs, the three worked scenarios) lives in `README.md` and `URL_Shortener_Engineering_Deliverables.docx` — read the README before making non-trivial changes.

## Commands

### Backend (`backend/`, Maven Wrapper — no local Maven install needed)

```bash
./mvnw spring-boot:run                    # run the API on :8080 (needs Postgres — `docker compose up db`)
./mvnw test                               # full suite, including Testcontainers integration test (needs Docker)
./mvnw test -Dtest=ShortCodeEncoderTest    # single test class
./mvnw test -Dtest=UrlServiceTest#rejectsReservedAlias   # single test method
./mvnw package                            # build the jar (target/url-shortener-backend.jar)
```

`UrlControllerIntegrationTest` spins up a real PostgreSQL container via Testcontainers + `@ServiceConnection` — it will hang/fail without a Docker daemon. To run only the mocked unit tests (no Docker required):

```bash
./mvnw test -Dtest='ShortCodeEncoderTest,LongUrlValidatorTest,UrlServiceTest'
```

### Frontend (`frontend/`)

```bash
npm install
npm run dev          # Vite dev server on :5173 (expects backend on :8080 — see .env.example)
npm test             # Vitest, single run
npm run test:watch   # Vitest, watch mode
npm run lint          # ESLint
npm run build          # tsc -b && vite build — type-check is part of the build, not a separate step
```

### Full stack

```bash
docker compose up --build   # Postgres + backend + frontend, one command
```

## Architecture

### The two request paths don't share a bottleneck

Link **creation** goes through the SPA (`POST /api/urls`). Link **redirection** (`GET /r/{shortCode}`) is hit directly by browsers following a shortened link out in the wild and **never touches the SPA** — this is why `RedirectController` and `UrlController` are separate controllers, and why redirects live under `/r/` instead of the domain root (keeps that route space uncontested by SPA/API paths). When changing routing or CORS, remember the redirect path has no frontend origin to reason about.

### Short codes are computed, not looked up

`ShortCodeEncoder` base62-encodes a **pre-fetched PostgreSQL sequence value** (`UrlRepository.nextId()`, a native `nextval('urls_id_seq')` call) into the short code, so IDs never collide and there's no retry-on-conflict path for generated codes. `UrlEntity` therefore assigns its own `@Id` before persisting and implements `Persistable<Long>` (with a transient `isNew` flag, reset via `@PostLoad`/`@PostPersist`) so Spring Data calls `persist()` instead of `merge()` for these manually-assigned IDs. Custom aliases are the one path that *can* conflict — `UrlService.createShortUrl` pre-checks `existsByShortCodeAndActiveTrue`, then still catches `DataIntegrityViolationException` from the DB constraint as a defense against the race, via `saveAndFlush` so the conflict surfaces inside the try block rather than at end-of-transaction.

### 410 vs. 404 is deliberate, and drives the lookup shape

A deactivated or expired link must return 410 Gone; a code that never existed returns 404. Because of this, `UrlService.getActiveForRedirect` looks up **regardless of active status** (`findByShortCodeIncludingInactive`) and only then branches on `!active || expired`. If you're touching redirect lookup logic, preserve this — querying only active rows collapses the distinction back into an undifferentiated 404.

### Cache-aside sits only in front of the redirect read

`UrlService.getActiveForRedirect` is `@Cacheable` (Caffeine, configured in `CacheConfig`/`AppProperties`); `deactivate` is `@CacheEvict` on the same key. Nothing else is cached. If you add a new way to invalidate or mutate a link (e.g. editing the destination URL), it must evict this cache or reads will serve stale data — there's no TTL short enough to paper over a missed eviction in tests.

### Click tracking is fire-and-forget off the redirect thread

`ClickTrackingService.record` is `@Async` on a dedicated bounded executor (`AsyncConfig.CLICK_EVENT_EXECUTOR`, `CallerRunsPolicy` so a burst degrades gracefully instead of dropping events) and swallows its own exceptions — a failed click write must never surface as a failed redirect. Consequently, click counts are eventually consistent: `UrlControllerIntegrationTest` polls with Awaitility rather than asserting immediately after a redirect.

### Schema is Flyway-owned; Hibernate only validates

`spring.jpa.hibernate.ddl-auto: validate` — there is no environment where Hibernate is allowed to generate DDL. Any entity/column change needs a new `V{n}__description.sql` file under `backend/src/main/resources/db/migration/`; editing an already-applied migration breaks Flyway's checksum. (Concretely bit us once: `click_events.country` was declared `CHAR(2)` in migration SQL while the mapped `String` field validates as `VARCHAR(2)` — Hibernate's schema validator fails start-up on that class of mismatch, and only a real Postgres run catches it, not a mocked test.)

### Config is centralized in one typed properties record

`AppProperties` (`@ConfigurationProperties(prefix = "app")`, records for `Cors`/`Cache`/`RateLimit`) is the single source for base URL, CORS origin, cache TTL/size, rate-limit threshold, and the reserved-alias list — backed by `application.yml` with `${ENV_VAR:default}` placeholders. Prefer adding a field here over a scattered `@Value`.

### Rate limiting is a filter, not interceptor/service logic

`RateLimitFilter` (`OncePerRequestFilter`, auto-registered as a `@Component`) guards only `POST /api/urls`, keyed on `request.getRemoteAddr()` with a Caffeine-backed fixed window. It runs before Spring MVC dispatch, so it writes its own JSON error body via `ObjectMapper` rather than going through `GlobalExceptionHandler`.

### Frontend: typed fetch client + React Query, no axios

`api/client.ts` is a single hand-rolled `fetch` wrapper (`ApiClientError` carries the parsed backend `ErrorResponse` body) — all pages consume it through TanStack React Query (`useQuery`/`useMutation`), not direct fetch calls. `types/api.ts` mirrors the backend DTOs by hand; if a backend DTO shape changes, update this file too, there's no generated client.

## Testing approach (see README.md §Testing for full rationale)

Backend tests are split deliberately: unit tests (`ShortCodeEncoderTest`, `LongUrlValidatorTest`, `UrlServiceTest`) mock the repository and run without I/O; `UrlControllerIntegrationTest` runs the full stack against a real Testcontainers Postgres because races and async-write landing can't be verified through mocks. Keep new tests in whichever bucket matches what they need to prove — don't reach for Testcontainers for logic a mock can already verify.
