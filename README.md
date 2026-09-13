# SkyRush

SkyRush is a web-based balloon crash game for a hackathon. The server-side gameplay MVP supports starting a round, authoritative flight state, cashout, crash loss, boosters, points, Sky Fragment rewards, history, and live configuration reload. The React frontend includes theme selection, betting, flight, cashout, results, Rules and History. Optional demo modules add real account rankings, a shared daily tournament, simulated tickets, evaluator configuration forms, instant repeat, and outcome commitments. Swagger remains available for independent API evaluation.

## Quick start for judges

- Hosted game: [Open SkyRush](https://91-210-169-21.sslip.io/)
- [Presentation and defense materials](https://clck.ru/3VoAFc)

With Docker running, from the repository root (macOS/Linux):

```sh
cp -n .env.example .env
# Choose a local POSTGRES_PASSWORD in .env.
export LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)"
docker compose up --build -d
```

- Game: <http://localhost:5173>
- Backend: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- Login: **demo / demo12345** (judge/evaluator), or **Создать аккаунт** for an independent player.
- Each new account starts with **1000 bonus units** by default.

Log in → choose a balloon → bet → **Начать** → **Забрать** after level 1, or wait for crash → result → **Играть снова** / **Повторить ставку**. Header icons open Rules, History and Tournament; **⋯ → Настройки игры** opens the evaluator tools when signed in as demo. No local Java/Node server is needed. Check readiness with `docker compose ps` (all three services should be healthy).

## Stack and structure

- Java 21, Spring Boot 3, Gradle wrapper, Spring JDBC
- React, TypeScript, Vite
- PostgreSQL 17, Flyway
- Springdoc OpenAPI / Swagger UI, Actuator health
- JUnit 5, pure math tests, real PostgreSQL integration tests
- Docker Compose; no paid services or additional runtime infrastructure

```text
sky-rush/
├── backend/
│   ├── gradle/wrapper/
│   ├── src/main/java/com/skyrush/
│   │   ├── users/              # Demo identity and user locking
│   │   ├── wallet/             # Bonus balance and ledger
│   │   ├── rounds/             # Lifecycle, transactions, REST and completion
│   │   ├── gameconfig/         # Validation, immutable snapshots, hot reload
│   │   ├── gamemath/           # Pure Java calculations and random providers
│   │   ├── history/            # Completed-round queries
│   │   ├── rewards/            # Sky Fragment grants and bonus exchange
│   │   ├── profile/            # Lifetime records, collection and cosmetic equip
│   │   └── shared/             # Clock, errors and API documentation
│   ├── src/main/resources/db/migration/
│   └── src/test/
├── frontend/src/               # pages, components, features, api, assets
├── config/game-config.yml      # Editable gameplay parameters
├── docs/
│   ├── architecture.md
│   ├── game-math.md
│   └── gameplay.md
├── .env.example
└── docker-compose.yml
```

The backend is authoritative: the client cannot choose crash point, booster position, multipliers, level completion, points, rewards, or payout. All game math is isolated from controllers and persistence.

## Prerequisites

Local backend: JDK 21 and PostgreSQL 17. Gradle is downloaded by the included checksum-verified wrapper. Frontend: Node.js 22.12+ and npm (Node 22 LTS recommended).

Compose: Docker Engine and Compose v2, or a compatible open-source local Docker runtime. Host JDK/Node are unnecessary for this path. Initial builds require access to public artifact and image registries.

Tests launch a disposable real PostgreSQL 17 process through the open-source Zonky embedded PostgreSQL test dependency, without Docker. Run tests as a non-root OS user; PostgreSQL refuses to run as root. Native libraries for your OS must be available. Test ports and data directories are temporary.

## Local development

From the repository root:

```sh
cp -n .env.example .env
# Set your local database password in the ignored .env file.
docker compose up -d postgres
```

Backend, in one terminal from the repository root:

```sh
set -a
. ./.env
set +a
export DB_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}"
export DB_USERNAME="$POSTGRES_USER"
export DB_PASSWORD="$POSTGRES_PASSWORD"
export SERVER_PORT="${BACKEND_PORT:-8080}"
cd backend
./gradlew bootRun
```

For an existing database, supply `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` directly. Spring Boot does not automatically load the root `.env`; the commands above export it. Flyway applies all migrations (V1–V6) on startup. Its history is explicitly stored in `public` so restarting with the `skyrush` database role remains safe. The default game-config path is `../config/game-config.yml`, relative to the backend working directory; set an absolute `SKYRUSH_CONFIG_PATH` when launching from elsewhere.

Frontend, in another terminal:

```sh
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Open <http://localhost:5173>. `VITE_API_BASE_URL=/api`; Vite proxies it to `API_PROXY_TARGET=http://localhost:8080`. If the backend port changes, update the proxy target. `VITE_` values are public build-time values, never secrets. After login, the entry screen selects a balloon theme, then opens bet selection. Choose GREEN or RED, select a bet, and start a real backend round. Rules and History are available in the header.

## Docker Compose

```sh
cp -n .env.example .env  # First setup only; set your local database password.
# Let the non-root backend write the mounted demo configuration directory:
export LOCAL_UID="$(id -u)"
export LOCAL_GID="$(id -g)"
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Compose waits for PostgreSQL health, starts the backend, then waits for backend health before starting the frontend. All published ports bind to localhost. The demo account has evaluator access and can change future game rules; ordinary accounts cannot. Public demo credentials make this a trusted demo tool, not a secured public admin. `LOCAL_UID`/`LOCAL_GID` match the host directory owner (defaults 1000/1000); exported values above take precedence over `.env`. The backend mounts the host `config/` directory writable for the evaluator admin: edits to `config/game-config.yml` hot reload without rebuilding. The frontend container serves the production build through nginx and proxies same-origin `/api` requests to `backend:8080`. No Vite/Node runtime runs in the frontend container. nginx re-resolves the backend service after container recreation.

```sh
docker compose down
```

The database persists in a named volume. `docker compose down -v` also deletes all local game data. The example password is a public development placeholder, not a deployed credential. Changing PostgreSQL credentials after first initialization requires changing the existing database credentials or recreating its disposable volume. Rebuild after Java/frontend source changes; game-config changes need no rebuild.

## Demo gameplay

Fresh databases automatically get `SkyRush Demo`, ID `00000000-0000-0000-0000-000000000001`, with **1000.00 bonus units**. Log in with **demo / demo12345**, or register an independent account. Every private gameplay endpoint uses the server-authenticated account. The existing demo UUID and its data survive the V4 migration. Seeding runs once per missing wallet and does not refill balance on restart.

- Swagger: <http://localhost:8080/swagger-ui.html>
- OpenAPI: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

Use the game login or Swagger to authenticate first. Swagger writes require the token from `GET /api/auth/csrf` in **Authorize → csrf**; fetch a new token after login rotates the session. See [account/API instructions](docs/accounts.md).

In Swagger, execute `POST /api/rounds` with `{"requestId":"a fresh UUID","theme":"GREEN","betOptionId":"DOUBLE"}`. Save the returned `id`, poll `GET /api/rounds/{id}/state`, then `POST /api/rounds/{id}/cashout`. Inspect your own `GET /api/wallet` and `GET /api/history`.

Use a fresh request UUID for each new round (`uuidgen` generates one), and the same ID only to retry the same selections without another debit. Poll state every 250–500 ms. Cashout becomes possible after level 1 (800 ms with defaults), if the balloon has not already crashed. Cashout fixes and credits payout but **does not stop flight**. Wait for crash before starting another round or expecting it in history. A repeated cashout is rejected.

RED has 12 levels, GREEN has 9. Default bets are BASIC = 10/x1, DOUBLE = 20/x2, TRIPLE = 30/x3, QUAD = 40/x4. The server samples the booster level and activates it if crossed before cashout. Losses preserve game points. Completed wins grant 2 Sky Fragments, losses grant 1; every 5 fragments automatically grants 5.00 bonus units, retaining the remainder. Fragments, game points, and wallet bonus units are separate concepts.

## Configuration and hot reload

Edit `config/game-config.yml` to change theme thresholds, bets, weighted booster locations, crash/growth rules, points, or rewards. The file is validated on startup and on reload. Missing sections, unsupported themes/boosters, wrong level counts, invalid thresholds, zero-total weights, and invalid numeric ranges/precision fail clearly.

To evaluate a points change:

1. Change `points.points_per_level: 10` to `37` and save.
2. Finish the current flight, which keeps its original rules.
3. Start a new BASIC round with a fresh request ID.
4. Its `pointsRules.pointsPerLevel` is 37, and each crossed level awards 37 points.

The backend checks the file every second and again on each new start, so the next new round observes a saved valid change immediately. Publication is thread-safe. Each round stores its entire immutable snapshot and a content hash `configVersion`; existing rounds remain unchanged even across process restart. Invalid live edits block new starts/config reads with 503 until corrected, while existing rounds remain playable. Prefer atomic file replacement for scripted edits.

Runtime environment options:

| Variable | Default / purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection; password required |
| `SERVER_PORT` | 8080 |
| `SKYRUSH_PROTOTYPE_CONFIG_PATH` | `../config/prototype-config.yml`; general/active and ticket settings |
| `SKYRUSH_CONFIG_PATH` | `../config/game-config.yml`; Compose uses `/app/config/game-config.yml` |
| `SKYRUSH_RANDOM_SEED` | Unset: SecureRandom; optional signed integer seed for reproducible development |
| `SKYRUSH_DEMO_ENABLED` | true; false skips seeding, without deleting/disabling an existing account |
| `SKYRUSH_DEMO_INITIAL_BALANCE` | 1000.00, applied only to a new demo wallet |

For deterministic local development, export `SKYRUSH_RANDOM_SEED=42` before `./gradlew bootRun`. In Compose, add it to `.env` and recreate the backend container. Same seed/config/draw order reproduces random outcomes; cashout still uses server time. The seed is never returned through the API. Leave it unset for normal random generation.

## Verification

```sh
cd backend
./gradlew test
./gradlew build
cd ../frontend
npm ci
npm run build
cd ..
docker compose config --quiet
```

Backend tests also cover registration, BCrypt, login/logout, CSRF, ownership and shared scoring. Backend tests cover exact math and rounding, seeded randomness, configuration validation and reload, PostgreSQL/Flyway startup, lifecycle, points, fragments, history, malicious payloads, Swagger, concurrency, and transactional rollback. Reports: `backend/build/reports/tests/test/index.html`. The executable JAR is `backend/build/libs/skyrush.jar`.

`npm run build` checks TypeScript and emits `frontend/dist`. `npm run preview` serves that static build without the development API proxy; the Docker frontend includes the `/api` reverse proxy. A separately deployed frontend needs an equivalent same-origin `/api` reverse proxy for session cookies; cross-origin credential/CORS hosting is not configured.

See [architecture](docs/architecture.md), [exact game formulas and examples](docs/game-math.md), and the [five-scenario evaluator walkthrough](docs/gameplay.md).

## Scope

Simple Spring Security accounts are implemented; production identity hardening, real payment/lottery integration, SSE and WebSocket are not. Rankings contain real registered players; simulated tickets have no real draw or monetary value. The functional game UI is included. The backend is a local demo with virtual bonus units; mathematical parameters are deliberately simple and do not target a calibrated return-to-player value.


## Frontend demo scenario

1. Open <http://localhost:5173>, select a RED or GREEN balloon, and check the demo bonus balance.
2. Choose RED (12 levels) or GREEN (9 levels). Open **Правила** before betting if needed.
3. Select one of four bets. Unaffordable options are disabled and labelled.
4. Press **Начать**. The backend deducts the stake once.
5. After level 1, press **Забрать**, or wait for the balloon to crash.
6. Watch the server-reported booster level, activation, multiplier and points. Cashout fixes the payout while the balloon keeps flying.
7. After crash, inspect the win/loss result, points, fragments and refreshed wallet balance.
8. Press **Играть снова** to return with the same theme. Ten seconds without interaction returns to **theme selection**; opening dialogs or processing repeat pauses the timer. “Repeat same bet” starts directly with the same selection using current server rules and balance checks.
9. Open **История** for completed rounds and pagination.

To demonstrate configuration changes, edit `points_per_level`, finish the existing round, and start a new one. **⋯ → Проверка полёта** displays the round's own points-per-level rule, while prior rounds retain their snapshots.

### Frontend state and recovery

The typed client consumes `/api/auth/me`, `/api/wallet`, `/api/game/config/public` (including its four bet options), `POST /api/rounds`, `POST /api/rounds/{id}/cashout`, `/api/rounds/{id}/state`, and `/api/history?limit=10&offset=...`. Round DTOs additionally include `ranking` and `integrity`; no client-supplied game results are accepted.

React hooks own orchestration; feature components render betting, flight, result, rules and history. There is no Redux or game engine. One serialized request loop polls 450 ms after each completed state request. A queued cashout waits for an in-flight poll, prevents duplicate clicks, and reconciles uncertain responses with a state read. Requests have an 8-second timeout and are aborted on unmount. Completed rounds stop polling. Temporary failures retain the flight screen and show a reconnecting message.

Round IDs and pending command IDs are stored in localStorage under the account UUID with a one-hour idle lifetime. Refresh restores a known active/completed round. An uncertain start reuses its command ID. When no local round/command exists, `/api/rounds/active` discovers the authenticated account's flight. Completed rounds without a saved ID remain in History; the server does not auto-reopen an arbitrary old result. Expired/corrupt local metadata is discarded. This is prototype recovery, not authenticated 30-day cross-device product persistence.

Balloon movement uses CSS transitions between server-reported completed-level positions. Displayed multipliers, points, payout, crash and rewards come directly from backend DTOs. Point differences only animate visual `+N` feedback. Server timestamps are compared for response ordering, never used to calculate gameplay.

### Responsive layout and accessibility

The interface supports 320–1920 px. The player view is a full sky scene: at narrow widths the altitude path stays beside the balloon and Cashout occupies a bottom dock. After cashout the scene reserves space for the fixed payout. Desktop uses the additional width for the landscape, hero balloon and large multiplier. Native modal dialogs support Escape, focus containment and focus restoration; controls support keyboard use, visible focus, and reduced motion. Results and booster states use text as well as color.

### Frontend tests

```sh
cd frontend
npm run typecheck
npm test
npm run build
# Real-backend browser tests (Docker must be running):
npx playwright install chromium firefox webkit
npm run test:e2e
# Or one installed browser:
npm run test:e2e -- --project=chromium
```

`npm run test:e2e` runs `scripts/run-e2e.sh`: it builds an **isolated Docker project**, copies the demo configuration into a temporary directory, verifies that the fresh database contains only demo, then runs the browsers. The assigned frontend port is automatic. The test containers, database volume and config copy are deleted on exit, including failure. Your regular accounts, config and demo balance are untouched. Do not point Playwright at the shared demo database; direct runs without the isolation marker are rejected.

Tests register temporary real accounts and use real server-calculated scores. No fake competitors are seeded. Screenshots are written under `/tmp`, failure traces under `frontend/test-results`. Run on macOS/Linux with Docker Compose 2.24.4+; install browsers once as above.

## Judge and team reference

- [Evaluation checklist](docs/evaluation-checklist.md): evidence, honest gaps and submission gate.
- [Product model](docs/product-model.md): themes, decisions, points and fragment progression.
- [Configuration reference](docs/configuration.md): every field, validation range and Scenario 5.
- [Demo script](docs/demo-script.md): short exact walkthrough and disclosed deterministic rehearsal mode.
- [Game mathematics](docs/game-math.md): formulas, rounding, examples and parameter/economy analysis.

The optional `config/game-demo.yml` is **not loaded by default**. It is a validated rehearsal preset with long enough flights and an early booster; use it only with the procedure in the demo script. Default random generation remains SecureRandom when the seed is unset. The model intentionally has generous expected bonus returns; do not present it as a calibrated real-money economy.


## Optional prototype modules

- **Theme screen:** select GREEN/RED; Play Again returns to bets, idle result returns to themes.
- **Live ranking:** nearby real accounts and your global rank, included in the normal 450 ms state poll. Score is persisted points from rounds started today (UTC); no simulated opponents.
- **Tournament:** trophy button opens real top three, scrollable rest, pinned current player, masking toggle and server countdown. UTC daily score sums persisted points from rounds started that day. The background worker advances unfinished rounds every 250 ms; open tables poll every 900 ms. Ties use creation time then UUID. No tournament prizes.
- **Admin:** “Ещё (⋯) → Настройки игры” opens typed forms for all math/themes/weights/stakes/points/rewards, plus general active flag and ticket rules. Counts RED12/GREEN9 and the uniform model are fixed. Save the two forms independently. Invalid values return useful errors; stale versions require reload. Valid saves atomically replace YAML; active rounds and existing offers retain snapshots.
- **Simulated upsell:** eligible completed wins (default payout ≥50) may show “Демо-билеты” once per tab/game session. Accept/decline or wait the default 10 seconds. Default ticket price 5, maximum 5, budget limited to 10% of payout, original stake and available balance. Accept debits bonus balance and persists tickets transactionally. Collection count is shown on selection screens. No real lottery, drawing, payment or prize exists.
- **Instant repeat:** result action uses the previous theme/bet ID with a fresh idempotency key and current config/balance. Unavailable or unaffordable options show an error; no server checks are bypassed.
- **Integrity demo:** open **Ещё (⋯) → Проверка полёта** and expand “Проверить исход” during flight to see the salted SHA-256 commitment. After crash, use “Проверить” to verify the revealed string. Legacy rounds created before V3 have no commitment. This proves commitment consistency, not randomness quality or gambling certification.
- **Sound:** muted by default. Enable with “Звук”; short quiet generated tones are throttled and require a user gesture. Reduced-motion CSS suppresses nonessential animation.

Public extension APIs: `GET /api/tournament?masked=true`, `GET /api/rounds/active`, `POST /api/upsell/offers`, `POST /api/upsell/offers/{id}/decision`, `GET /api/upsell/tickets`. Evaluator APIs: `GET/PUT /api/admin/game-config`, `GET/PUT /api/admin/prototype-config`. All are in Swagger.

Known limits: simple accounts without password reset, MFA, rate limits or persistent sessions; public demo credentials include evaluator access; ticket-offer session IDs are frequency identifiers, not authentication; no calibrated economy, load-tested scale, or real tournament/ticket prizes. Admin saves require write access to the **directory**, not only the file. External editors should not save concurrently with admin; API version checks protect admin-to-admin lost updates, but no OS file lock coordinates external editors. No `.git` metadata is present in this workspace; source publication and presentation remain team tasks.

### Player visual design

GREEN uses a turquoise coastal sky; RED uses a coral sunset and sharper mountains. Select a floating balloon, then one of four bet tokens. The first available cashout shows a four-second hint once per tab. Points, boosters, cashout, crash and rewards use brief visual feedback; exact values still come from the backend. History, Rules and Tournament use the compact header icons. Evaluator controls are under **Ещё (⋯)**. See [visual redesign and copy audit](docs/visual-redesign.md) for assets, screenshots and verification.

## Accounts and multiplayer

Register with a unique username (3–40 letters/digits/underscores), public display name (up to 40 characters) and password (8+ characters, at most 72 UTF-8 bytes). Registration signs in automatically. **⋯ → Выйти** logs out. Demo credentials are **demo / demo12345**; only this seeded account can open evaluator configuration tools.

Spring Security + BCrypt + an HttpOnly SameSite=Lax session cookie identify each player. Private wallet, rounds, history, fragments, tickets and recovery are isolated by server identity. Login sessions expire after two idle hours and after backend restart; PostgreSQL account/game data persists. `SESSION_COOKIE_SECURE=true` is available for HTTPS hosting. Keep the default false for local HTTP.

To demonstrate real multiplayer:

1. Open http://localhost:5173 and register Player A.
2. Open an incognito window or another browser and register Player B.
3. Play on both; each account has its own initial balance and history.
4. Watch the same daily scores in the flight ranking and trophy table; uncheck name masking to see full public display names.
5. Refresh either game to recover its own flight.

The leaderboard aggregates server-persisted points from rounds started today, UTC. Active rounds count; scores update through the existing serialized polls. There are no simulated participants. Display names and scores are public; masking is optional presentation, not a privacy boundary. Full model, migration, Swagger instructions and security limits: [accounts.md](docs/accounts.md).

`npm run test:e2e` automatically copies the disclosed game-demo preset into its disposable stack; `multiplayer.spec.ts` uses two isolated browser contexts and verifies balances, histories, shared score updates and forbidden cross-user round access. Tests do not fabricate outcomes. Public evaluator credentials, missing rate limits/password recovery and in-memory sessions remain explicit deployment limitations; this is not hardened public identity infrastructure.

Earlier account verification: **66 backend tests**, **64 frontend tests**, TypeScript, backend/frontend builds and **9 Docker E2E tests** passed (Chromium/Firefox/WebKit, including two independent users in each). Clean-volume startup, demo seeding, V4 legacy migration, health, Swagger and login/logout were verified. Normal configuration is restored. [Evidence and limits](docs/accounts.md#verification--2026-09-11).

## Public multiplayer demo

With Docker running, from the repository root:

```sh
./scripts/start-public-demo.sh
```

Copy the **HTTPS URL printed by localhost.run** and send it to another person. Open that same URL in two browsers: log in with **demo / demo12345** or register separate accounts, play and watch the shared leaderboard. No tunnel software installation or service account is needed; the helper uses OpenSSH (included with macOS).

**Temporary hackathon access only:** demo/demo12345 is intentionally public **evaluator/admin access**, never a production credential. Public registration has no rate limiting. Keep the tunnel running only while testing and stop it afterwards. Do not enter sensitive personal information. Session cookies remain HttpOnly, Secure and SameSite=Lax; CSRF stays enabled.

Stop the tunnel with **Ctrl+C** in its terminal. To stop Docker too (database preserved):

```sh
./scripts/stop-public-demo.sh
```

The helper requires Docker Compose **2.24.4+**, curl and OpenSSH. It initializes a missing `.env`, builds/starts the stack, waits for all three healthchecks, checks nginx and `/api/auth/me` (expected unauthenticated 401), then keeps SSH output visible. SSH uses no personal identity or agent; first-use server keys are remembered and changed keys are rejected. **Free anonymous URLs can rotate even while SSH remains connected (observed during testing). Always use the latest HTTPS URL in the terminal. After rotation, share it again and log in again; accounts and scores remain in PostgreSQL.** A disconnected tunnel needs a new script invocation.

The local unified origin is normally http://localhost:5173; the script reads the actual port from Compose. **Use the generated HTTPS address for public-mode account testing:** Secure-cookie exceptions for local HTTP differ between browsers. `docker-compose.demo.yml` removes PostgreSQL/backend host ports; only loopback nginx is published. Ordinary local HTTP development remains available with `docker compose up --build -d` (without the demo override).

Manual tunnel command after starting the demo Compose stack:

```sh
ssh -R 80:localhost:5173 nokey@localhost.run
```

Routing: Internet → localhost.run HTTPS/SSH tunnel → nginx → React `/` or internal backend `/api/*`, `/swagger-ui.html`, `/swagger-ui/*`, `/v3/api-docs*`, `/actuator/health`. SPA fallback is preserved. Cookies, CSRF headers and request bodies pass through nginx. The browser uses one origin and relative API paths; no public backend URL or CORS exception is needed. Only the isolated demo backend trusts normalized nginx forwarding headers.

[localhost.run documentation](https://localhost.run/docs/) explains the SSH workflow and HTTPS termination. [Free-tier limits](https://localhost.run/docs/forever-free/) include changing domain names and limited speed. If outbound SSH is blocked, use another network; do not disable CSRF or cookie security. This is temporary demo access, not a production hosting service.

Verification and security details: [public demo acceptance](docs/public-demo.md).

Public acceptance: Chromium and Firefox passed all three E2E scenarios each. WebKit passed locally over HTTPS but its public run failed on reload-time fetch errors and a post-registration timeout; see the report before a Safari-based live demo.

## Тексты и честный рейтинг

В рейтинге — только зарегистрированные аккаунты и их реальные очки за UTC-день. Пустые места не заполняются; подиум показывает до трёх игроков с положительным счётом. E2E запускается в одноразовой Docker-базе, поэтому тестовые Alice/Bob больше не попадают в обычную игру. [Аудит текстов, очистка подтверждённых фикстур и проверки](docs/copy-and-ranking-audit.md).

После редакции: backend **68/68**, frontend **69/69**, TypeScript/build и изолированные Docker E2E **9/9** (Chromium/Firefox/WebKit). Основной Docker-стек обновлён; данные обычных аккаунтов сохранены.

## Профиль, рекорды и коллекция

Нажми на имя/аватар. В профиле — личные рекорды, баланс, место в дневном рейтинге и коллекция. Каждые 5 заработанных за всё время фрагментов открывают следующий узор шара или рамку: «Созвездия» (5), «Бронза» (10), «Ленты» (15), «Сияние» (20). Нажми **Надеть**: выбор сохраняется для аккаунта и виден в игре.

Бонусы — валюта ставок; очки — результат для рейтинга; фрагменты — прогресс коллекции. Прежний автоматический обмен фрагментов на бонусы сохранён и не сбрасывает коллекцию. Косметика **не влияет на исход, выплаты или силу бустеров**. Старые награды учитываются миграцией V5 без изменения кошельков и истории. [Правила и API профиля](docs/progression.md).

Проверка профиля/коллекции: **73 backend**, **80 frontend**, TypeScript/build; **12 E2E** в Chromium/Firefox/WebKit и повторный прогон профиля **3/3**. Проверены ширины 320–1920 px и сохранность существующих данных при V5. [Отчёт проверки профиля и скриншоты](docs/acceptance/progression/verification.md).

## Достижения, сегодня и онлайн

В профиле — 11 постоянных достижений и одно общее задание на UTC-день. Задание даёт **+1 фрагмент один раз**; достижения — статус без валютных наград. Фрагмент сразу учитывается коллекцией, а обычный обмен на бонусы происходит при следующем завершённом полёте.

«Сейчас в игре» показывает до пяти реальных событий и число уникальных аккаунтов с активностью за последние 90 секунд. Нет ботов, искусственного онлайна и выдуманных событий. В скрытых вкладках опрос приостанавливается. [Правила, API и настройки](docs/social.md).

Параметры — в `config/social-config.yml`; после их изменения перезапусти backend. Дневной порог уже выбранного задания сохраняется до следующего UTC-дня. Для проверки двух игроков открой приложение в двух браузерах, зарегистрируй разные аккаунты и сыграй: второй увидит реальные события первого через несколько секунд.

Проверка социальных функций: **82 backend**, **90 frontend**, TypeScript/build, **15/15 E2E** в Chromium/Firefox/WebKit и финальный социальный прогон **3/3**. Обычная Docker-сборка и чистые тестовые тома проверены; данные основной базы сохранены при V6. [Отчёт и скриншоты](docs/acceptance/social/verification.md).

## Code quality

Java 21, Node 22.12+ и Docker Compose CLI. Сначала `npm --prefix frontend ci`.

```bash
./scripts/check.sh                   # все проверки и frontend build
(cd backend && ./gradlew check)       # тесты, Spotless, SpotBugs, Checkstyle
npm --prefix frontend run check      # ESLint, CSS, формат, типы, тесты
./scripts/format.sh                  # только форматирование
./scripts/audit-dependencies.sh      # отдельный онлайн-аудит зависимостей
```

`check` не исправляет файлы и не запускает приложение. Тестовая PostgreSQL изолирована от игровых данных. [Версии инструментов и обоснования исключений](docs/code-quality.md).

## Production/VPS deployment

For an Ubuntu VPS with one domain, automatic HTTPS, private PostgreSQL and manual updates, follow [VPS deployment](docs/deployment-vps.md). Use `.env.prod` and the separate `docker-compose.prod.yml` override. Local Compose startup remains unchanged.
