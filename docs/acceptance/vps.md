# VPS configuration verification — 2026-09-11

Scope: local Docker Desktop, production Compose override, real Caddy/nginx/Spring/PostgreSQL. **No real VPS or public domain was supplied. Ubuntu bootstrap, public DNS, ACME issuance and certificate renewal were not executed on a VPS.**

## Verified locally

- Production Compose parses; only Caddy publishes TCP 80/443. PostgreSQL/backend/nginx publish no host ports. All four services become healthy.
- Caddy configuration validation and nginx syntax check pass. HTTPS frontend, health, CSRF and Swagger work. Caddy used its local CA for `localhost`; the verification script used the CA certificate without `curl -k`. Browser fixture contexts explicitly trusted the test setup with `ignoreHTTPSErrors`.
- Two independent Chromium contexts registered real temporary accounts. Secure/HttpOnly/SameSite=Lax/Path=/ cookies, login/logout/me, missing-CSRF rejection, independent wallets and private-round isolation passed. A real round cashed out, continued and completed; another account observed real points/activity. Profile, collection, daily and achievement endpoints responded successfully.
- Ordinary account admin access returns 403. Demo evaluator can read and save game YAML over HTTPS to writable `runtime/config`.
- Repeated authentication POSTs return 429, including attempts with varying spoofed X-Forwarded-For headers. CSRF GET remains available. Oversized requests return 413.
- The actual backup script produced a valid custom-format dump. Ordinary Compose down/up retained user/round counts, wallet totals, game points and fragments. The documented restore ran against the isolated database with application services stopped; the same values matched afterwards and HTTPS health passed.
- The actual deploy script ran from a clean temporary Git checkout with a local-only upstream: fast-forward pull, initial runtime configuration, build, proxy validation, health wait and HTTPS checks passed. Dirty-checkout rejection was tested separately. A second deployment preserved the evaluator's YAML byte-for-byte.
- Normal local Compose also rebuilt and started healthy without Caddy or production cookie settings; the existing main database was not reset.

## Regression checks

- `./scripts/check.sh`: passed, including production Compose validation and VPS shell syntax.
- Backend: 82 tests passed.
- Frontend: 92 tests passed; lint, CSS lint, formatting, TypeScript and build passed.
- Full real-backend E2E: **15/15 passed**, five each in Chromium, Firefox and WebKit; includes gameplay, two-user multiplayer, admin, collection and social progression.

All synthetic verification accounts lived in an isolated test project. Local test dumps, TLS material, logs and runtime files are ignored by Git. No production secret or real certificate is included in the repository. No application code, migrations or game model changed.
