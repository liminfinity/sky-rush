# Release-candidate acceptance

This is the pre-account release record. For the newer authenticated multiplayer release, see [accounts verification](../accounts.md#verification--2026-09-11) and account evidence (local verification artifact).

This pass verifies the Docker-served production frontend and the existing gameplay. It adds no game features or visual changes.

## Reliability corrections

1. **Flyway restart:** PostgreSQL's default `"$user", public` search path resolves to `skyrush` after V1 creates that schema. The original first startup placed Flyway history in `public`; a later startup searched in `skyrush` and failed with “non-empty schema … but no schema history table”. `spring.flyway.default-schema: public` fixes the location without baselining, deleting data, or changing migrations. A PostgreSQL regression test reproduces the role/schema collision and checks a repeat migration.
2. **Frontend artifact:** Docker now builds the actual Vite production bundle and serves it through nginx. `/api/` is proxied to the Docker backend service with Docker DNS re-resolution. No host npm/Gradle server or frontend Vite runtime participates. A frontend healthcheck complements PostgreSQL and backend healthchecks.
3. **Judge instructions:** the README begins with the minimum initialization commands and documented URLs. Host UID/GID are supplied so evaluator saves can replace mounted YAML files on macOS/Linux.

## Acceptance method

- Existing Docker database backed up before the explicitly requested destructive reset.
- Separate source copy prepared without `.env`, node_modules, Gradle caches, or compiled output; README startup instructions followed there.
- Standard random configuration exercised first through Docker URLs.
- Existing full browser suites use the disclosed `game-demo.yml` rehearsal configuration for reliable booster/upsell cases; normal defaults are restored afterwards.
- External YAML edits test hot reload separately from the admin API.
- Database content compared before/after ordinary Compose shutdown/recreation; clean-volume initialization repeated separately.
- Final running stack is returned to the original repository and normal configuration.

## Results — 2026-09-11

**PASS.** Docker Desktop on macOS arm64, Linux containers; Engine 29.7.2 / Compose 5.5.1. The final stack is running from the original repository, with normal game configuration, no random seed, a fresh database and 1000 bonus units.

| Check | Result / evidence |
| --- | --- |
| `docker compose up --build -d` | Images built; PostgreSQL, backend and frontend all running/healthy: containers.json (local verification artifact) |
| README startup from clean source copy | Passed without host Java/Node servers, node_modules, compiled output or copied `.env`; Docker layer caches were available |
| Clean-volume initialization, repeated | Both fresh starts passed after `down -v`; V1–V3 applied successfully: migrations.txt (local verification artifact) |
| Persistence across `down` / `up --build -d` | 16 rounds, balance 2244.70, 1 remaining fragment and 6 ticket offers unchanged: checks.json (local verification artifact) |
| HTTP and API routing | nginx production assets, same-origin wallet API, health UP, Swagger and OpenAPI (16 paths): http.json (local verification artifact) |
| Default-randomness browser smoke | GREEN cashout/continued flight/recovery/win, RED loss, four bets, Rules and persisted History; every browser request used the frontend origin: default-browser.json (local verification artifact) |
| Existing real-backend E2E | **6/6 passed**, 2 each Chromium/Firefox/WebKit: e2e.txt (local verification artifact) |
| External YAML hot reload | 10→43 points; old round retained 10, new round used 43 and earned 86 after two levels: hot-reload.json (local verification artifact) |
| Backend tests and build | **57 tests**, zero failures/errors/skips; `./gradlew test build --rerun-tasks` passed |
| Frontend tests / TypeScript / production build | **57 tests passed**; `npm run typecheck`, `npm test`, `npm run build` passed |
| Compose configuration | `docker compose config --quiet` passed |
| Container logs | No application ERROR/FATAL/exception or nginx error found after gameplay and final startup. Springdoc reports intentionally enabled evaluator endpoints; Alpine PostgreSQL reports unavailable system locales |

The existing E2E suite also exercises admin edits and snapshots, live ranking/tournament, booster activation, reward results, ticket accept/decline, repeat bet, local/server recovery, commitment verification, insufficient balance, history and result inactivity return. Layout assertions run at 320, 375, 768, 1024, 1440 and 1920 px. Docker screenshots reviewed include history, mobile active flight and upsell. Gameplay assertions were not weakened or changed.

## Remaining submission tasks

- Publish the source and run these instructions from the actual remote clone: this workspace has no `.git` metadata or remote, so acceptance used a clean source copy rather than claiming a verified remote clone.
- Warm Docker images on the presentation machine. The first download stalled on the network; retry succeeded without replacing the pinned Gradle distribution or using host-built artifacts.
- Rehearse on the actual presentation browser/device. Playwright engine coverage is not physical-device certification.

No functional Docker acceptance blocker remains on this machine. No gameplay, math or visual behavior changed in this pass. The original pre-reset database backup remains outside the repository at `/tmp/skyrush-before-acceptance.dump`; temporary test balances were deliberately discarded by the requested final clean-volume reset.
