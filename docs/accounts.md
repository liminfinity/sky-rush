# Accounts and shared leaderboard

SkyRush now requires a real server session. Game mathematics, wallet transactions, configuration snapshots and scene rendering are unchanged. Registration adds an account plus its wallet and fragment progress in one PostgreSQL transaction. Initial bonus units use `SKYRUSH_DEMO_INITIAL_BALANCE` (1000 by default); the browser cannot choose this amount.

## Account/session model

- Username: 3–40 ASCII letters/digits/underscores, case-insensitive uniqueness, stored lowercase. Display name: nonblank, at most 40 characters; public in the leaderboard.
- Password: at least 8 characters, at most 72 UTF-8 bytes, BCrypt (cost 10). Password hashes never occur in public DTOs. No passwords are logged.
- Spring Security uses a server-side `HttpSession`, identified by an HttpOnly `JSESSIONID`, SameSite=Lax, two-hour inactivity timeout. Login/register invalidate the previous session and explicitly save the new security context. Logout invalidates the session.
- CSRF remains enabled for **all writes**, including login/register/logout. `GET /api/auth/csrf` supplies a session token/header name. The API client fetches a token before each write. It is separate from the authentication cookie; no access token is stored in browser storage.
- `SESSION_COOKIE_SECURE=true` enables HTTPS-only cookies when hosted over HTTPS. Local HTTP defaults to false. Docker still binds PostgreSQL and backend ports to localhost; use the frontend's same-origin `/api` proxy.
- Gameplay services resolve UUID exclusively from the authenticated security context. Private round queries include that UUID. User-row locking, idempotency keys and transactional debits/credits remain intact. The internal completion job resolves the owner from a stored round, never request input.

Implementation follows Spring Security's [explicit context persistence](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html) and [session-backed CSRF protection](https://www.springframework.org/spring-security/reference/6.5/servlet/exploits/csrf.html).

## APIs

| Endpoint | Behavior |
| --- | --- |
| `GET /api/auth/csrf` | Public CSRF bootstrap, creates session if needed |
| `POST /api/auth/register` | `{username, displayName, password}`; creates account and signs in |
| `POST /api/auth/login` | `{username, password}`; signs in |
| `POST /api/auth/logout` | Invalidates session |
| `GET /api/auth/me` | Safe id, username, displayName, createdAt, evaluator flag |
| `GET /api/tournament/current/leaderboard?masked=false` | Shared daily table; alias `/api/tournament` retained |

The table retains the existing frontend DTO vocabulary: `position` = rank, `name` = public display name, `points` = score, `currentPlayer` = caller identity. `simulated` is always false. Wallet, rounds, history, tickets, recovery and configuration reads require login. `/api/users/demo` is a compatibility URI for the **current** player; it cannot select the demo identity. Unknown authority fields in write JSON are rejected. Health and API documentation remain public.

Swagger: execute `/api/auth/csrf`, copy `token` into **Authorize → csrf**, then execute login. Fetch and authorize with a fresh token after login (the session rotated). Cookies are managed by the browser. The player UI performs these steps automatically.

## Demo and migration

Demo credentials: **demo / demo12345**. Demo remains the evaluator account and has access to `/api/admin/**`; registered players receive 403 there. No registration field can grant evaluator access. The menu hides evaluator tools for normal players.

V4 adds username/password_hash and unique username constraint; it does not modify V1–V3. The existing demo UUID `00000000-0000-0000-0000-000000000001` and all its foreign-key-linked data are retained. Only an uninitialized demo hash is seeded; startup never resets balances or overwrites an existing password hash. Unexpected legacy accounts are assigned unique `legacy_…` usernames and disabled hash `*` rather than giving them a shared password. Fresh databases seed demo automatically. `SKYRUSH_DEMO_ENABLED=false` prevents seeding; it does not delete or disable an already existing account.

## Scoring and live updates

Daily tournament boundary is 00:00 UTC. Score is the sum of authoritative persisted `earned_points` for rounds **started that UTC day**, including active rounds. No separate writable score total is introduced, avoiding divergence from game data. Points persist transactionally with round advancement. Ties sort by account creation time, then UUID.

The completion worker advances all unfinished rounds every 250 ms, so another player's score progresses even without an open polling tab. Its private path uses the same user locks and math; it does not construct a public round response. The leaderboard reads an indexed daily range, groups by user and includes real accounts with zero points. No simulated opponents remain.

The existing serialized flight poll (450 ms after each response) includes nearby real players and the current player's global position. The tournament table polls every 900 ms, without overlapping requests. Typical peer update latency is about 250 ms worker interval plus 450/900 ms polling and request latency; this is a local responsiveness target, not an internet latency guarantee. Masking shortens other display names on request; your own name remains identifiable. Display names are public nicknames, not private identity data.

## Multiplayer rehearsal

1. Start Docker and open http://localhost:5173.
2. Register Player A in a normal browser.
3. Open an incognito window or another browser and register Player B.
4. Start rounds on both; inspect the ranking strip or trophy table.
5. Watch shared scores change while balances, histories and ticket collections remain separate.
6. Refresh either window: its account session and flight recover. Login after backend restart if the in-memory session has expired; game data remains in PostgreSQL.

Browser recovery metadata is namespaced by account UUID, with the existing one-hour lifetime. An account cannot replay another account's pending start intent after a switch. Tabs in the same browser profile share the login cookie; use separate profiles/contexts for separate players. Backend checks remain authoritative regardless of stale local metadata.

## Remaining security and scale limits

This is simple hackathon identity infrastructure, not a production identity platform. There is no password reset, email verification, MFA, rate limiting, registration abuse prevention or persistent session store. Backend restart logs users out; rounds remain recoverable after login. Public demo credentials also grant evaluator power: **do not expose this build to untrusted users as a secured public service** without replacing/disabling evaluator credentials/access and adding deployment protections. Account registration grants virtual bonus units and can be repeated; these units/tickets have no real-money value. Rankings reveal public display names and scores; masking is presentation, not access control. Large-user pagination, worker batching and load tests remain future work. No distributed infrastructure was added.

## Verification — 2026-09-11

- Backend: **66 tests**, zero failures/errors/skips; Gradle build passed. Includes real PostgreSQL, registration/uniqueness/hash checks, login failure/success/logout, real CSRF token/session rotation, unauthenticated APIs, ownership, private history/wallet/recovery, evaluator denial, daily scores, stable ties and masking. Existing gameplay/concurrency/math tests remain.
- Frontend: **64 tests passed**, TypeScript and production build passed. New account tests cover login, registration, saved session, expired session, duplicate submit, logout failure and account-scoped recovery. Real-player ranking rendering remains covered.
- Docker E2E: **9/9 passed** — three tests each in Chromium, Firefox and WebKit. Both previous gameplay suites remain, with login/CSRF prerequisites. The new two-context suite registers Alice/Bob, runs independent GREEN/RED rounds, checks their separate balances/history, watches peer score changes through real flight polling, verifies global order/current-player identity and denies cross-user round/cashout/admin access. Uses the disclosed game-demo preset and seed 42, no API route mocks.
- V4 upgraded the existing V3 demo database, preserving its UUID and 1000 balance. `docker compose down -v` / `docker compose up --build -d` also passed: V1–V4 and demo seeded automatically; all three services healthy.
- After tests, normal configuration and unset seed were restored, and the final fresh database contains only demo with 1000 units. Real-browser demo login/logout passed; wallet returned 401 after logout. Health UP, Swagger and all 22 OpenAPI paths available. No application error found in exercised/final container logs.
- Login layout checked at 320, 375, 768, 1024, 1440, 1920 px; login and real tournament screenshots reviewed. Existing full gameplay responsive checks run in all three browser suites.

Machine-readable evidence: test counts (local verification artifact), E2E output (local verification artifact), migration/seed (local verification artifact), containers (local verification artifact), final HTTP/auth smoke (local verification artifact). These account results supersede the prior demo-only acceptance record. Public-device/load/security certification and external deployment were not performed.
