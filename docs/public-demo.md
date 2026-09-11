# Public demo acceptance — 2026-09-11

## Architecture and startup

Run `./scripts/start-public-demo.sh` with Docker Desktop running. The helper builds the existing Compose stack plus `docker-compose.demo.yml`, waits for health, checks the local unified origin, then starts an anonymous `nokey@localhost.run` SSH reverse tunnel. Copy the HTTPS address from its output. No Cloudflare client or account is required.

Internet HTTPS → localhost.run TLS termination → encrypted SSH → loopback nginx:5173 → React assets or backend:8080 → PostgreSQL. Only nginx publishes a loopback host port. API calls remain relative `/api`; no gameplay, authentication or mathematics code was changed.

nginx passes request bodies, cookies and CSRF headers; normalizes forwarded host/protocol; routes API, Swagger/OpenAPI and health; retains SPA fallback. The demo backend enables forwarded-header processing and Secure cookies. Normal Compose remains suitable for local HTTP development.

Ctrl+C terminates the foreground SSH tunnel. `./scripts/stop-public-demo.sh` stops Docker without deleting its database. Free anonymous addresses can rotate even with SSH still connected; this happened during verification. Read and share the latest address in the terminal. Cookies belong to the previous domain, so players must log in again; server account/game data remains intact. If SSH exits, rerun the helper. The SSH helper ignores personal SSH configuration/identities and agents, remembers first-use host keys and rejects changed keys.

## Verification

- Clean-volume demo Compose startup passed: PostgreSQL, backend and frontend healthy; migrations applied. A pre-reset database backup was retained outside the repository.
- nginx configuration validation passed. PostgreSQL/backend have no published host ports; nginx binds only 127.0.0.1:5173. Container logs inspected without application errors.
- Backend: 66 tests, zero failures/errors/skips; Gradle build passed.
- Frontend: 64 tests passed; TypeScript and production build passed.
- All 9 existing browser scenarios passed through a temporary local HTTPS terminator, across Chromium, Firefox and WebKit. Only client acceptance of the temporary self-signed certificate was added in temporary test copies; no gameplay assertions changed. The temporary terminator was removed afterwards.
- An initial plain-HTTP demo-mode run passed in Chromium/Firefox but failed login in WebKit: Secure cookies require HTTPS there. This is why the public workflow explicitly requires the generated HTTPS address.
- Real localhost.run HTTPS opened SkyRush in Chromium with HTTP 200. Public registration, authenticated `/me`, logout, login, health and OpenAPI passed. Session cookie verified Secure + HttpOnly + SameSite=Lax. Missing-CSRF POST returned 403; ordinary account admin access returned 403. See auth evidence (local verification artifact).

- Initial public run: Chromium 3/3 and Firefox extensions passed. Then localhost.run rotated the anonymous domain; Firefox login on the old address timed out. The obsolete-address run was stopped (one failure, one interrupted, three not run). No tests or assertions were weakened. See first public run (local verification artifact).

- Fresh-domain Firefox rerun: **3/3 passed**, including live updates between two independent browser contexts. See Firefox evidence (local verification artifact).

- Public WebKit extensions: gameplay assertions completed, but the strict empty-page-error assertion failed. A `Fetch API cannot load .../state due to access control checks` event occurred 446 ms after `page.reload()` began. Subsequent same-origin state/cashout responses were 200 and recovery completed. This suggests a navigation-related request interruption, but its exact cause is not established; no CORS workaround or error suppression was added. Local HTTPS WebKit passed all 3 scenarios. See failure evidence (local verification artifact).

- Public WebKit final result: **0/3 passed**. Both full gameplay scenarios reached their final assertions but reported the reload-time fetch/page error above. Multiplayer timed out waiting 5 seconds for the post-registration game screen; public WebKit multiplayer is not verified. See WebKit output (local verification artifact). Public Chromium and Firefox each passed all 3. This is **not** a claim of a fully green public cross-browser release.
- Normal game/prototype configuration restored after verification. No game/client/security tests or source were modified to suppress the public failures.

- Final cleanup: both owned localhost.run tunnels closed. Stop helper and data-preserving Compose restart passed; all three containers healthy with normal configuration. Backend tests/build rerun successfully after restoration. Docker remains running locally; invoke the start helper for a new public URL.

## Limits

Demo credentials `demo / demo12345` intentionally grant evaluator/admin access. Public registration lacks rate limiting; use the tunnel only during supervised hackathon testing. Display names/scores are public. Sessions remain in backend memory. Do not use production passwords or personal data. TLS is terminated by the tunnel provider; local Docker traffic is HTTP. Neither free-tunnel uptime nor stable domain names are guaranteed.

Cloudflare was attempted before the requested switch: allocated URLs did not deliver the app (Cloudflare 1033), with native DNS discovery failures and Docker edge connection timeouts. No Cloudflare public gameplay success is claimed.

Provider references: [SSH workflow](https://localhost.run/docs/), [HTTP forwarding](https://localhost.run/docs/http-tunnels/), [free-tier limits](https://localhost.run/docs/forever-free/).
