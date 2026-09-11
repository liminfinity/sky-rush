After opening the app, log in with **demo / demo12345** or create an account. All gameplay below belongs to that authenticated account. See [accounts](accounts.md).

# Gameplay and evaluator guide

The backend implements the five mandatory gameplay scenarios. The frontend at <http://localhost:5173> provides betting, flight, results, Rules and History. Use Swagger at <http://localhost:8080/swagger-ui.html> or the REST examples below. Private endpoints require login and use only the authenticated account. Select accounts by login, never by submitting user IDs.

## Demo identity and balance

`GET /api/users/demo` returns `SkyRush Demo`, ID `00000000-0000-0000-0000-000000000001`. A fresh database automatically receives 1000.00 bonus units. Seeding is idempotent and never refills an existing wallet on restart. `GET /api/wallet` returns `wallet.bonusBalance` and separate `skyFragments`. Points belong to round history, not wallet balance.

`SKYRUSH_DEMO_INITIAL_BALANCE` changes the initial balance only for a new demo wallet. `SKYRUSH_DEMO_ENABLED=false` disables demo seeding and gameplay access; it does not add production authentication. To reset a disposable Compose demo database, `docker compose down -v` deletes its data; the next startup seeds a fresh wallet.

## REST reference

| Method and path | Purpose |
| --- | --- |
| `GET /api/users/demo` | Demo identity |
| `GET /api/wallet` | Current bonus balance and fragments; settle due flight first |
| `GET /api/game/config/public` | Current themes, thresholds, points/reward rules and config version |
| `GET /api/game/bet-options` | Four configured bets |
| `POST /api/rounds` | Start or safely retry a round |
| `POST /api/rounds/{id}/cashout` | Fix payout once, after level 1 and before crash |
| `GET /api/rounds/{id}` | Authoritative round state |
| `GET /api/rounds/{id}/state` | Same state, intended for 250–500 ms polling |
| `GET /api/history?limit=20&offset=0` | Completed rounds, newest first; limit 1–100 |

All API responses use `Cache-Control: no-store`. Start returns 200 for both the original command and a successful retry. History uses bounded offset pagination. Unknown or malformed fields are rejected with 400, including client-provided multipliers, crash points, positions, rewards, points, payouts, and timestamps. Cashout accepts no body or `{}`. UUIDs and selection DTOs are validated.

Errors consistently contain `code`, `message`, `status`, `path`, and `timestamp`. Business conflicts use 409: `INSUFFICIENT_BALANCE`, `ROUND_IN_PROGRESS`, `REQUEST_ID_CONFLICT`, `LEVEL_ONE_REQUIRED`, `ALREADY_CASHED_OUT`, or `ROUND_CRASHED`. Missing rounds use 404; an invalid external config prevents new starts with 503 `CONFIG_INVALID`.

## Scenario 1: start

```sh
curl -s http://localhost:8080/api/users/demo
curl -s http://localhost:8080/api/wallet
curl -s http://localhost:8080/api/game/bet-options
curl -s -X POST http://localhost:8080/api/rounds \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"2c60592f-b44c-4da2-8695-e18a98b9124d","theme":"GREEN","betOptionId":"DOUBLE"}'
```

Use a **fresh UUID** for each new round (for example `uuidgen`), and reuse the original ID only when retrying that same start. The same ID and selections return the existing round without another debit. Reusing an ID with different selections fails. One flight at a time is allowed per user, including the period after cashout and before crash.

Save the response `id`. Stake is deducted once. The visible booster level is chosen at start. `crashMultiplier`, `completedAt`, and `reward` stay null until crash. The full internal snapshot and crash deadline are never public.

## Scenario 2: cashout

```sh
curl -s http://localhost:8080/api/rounds/ROUND_ID/state
curl -s -X POST http://localhost:8080/api/rounds/ROUND_ID/cashout
```

Replace `ROUND_ID` with the returned ID. Poll every 250–500 ms; when `canCashout` is true, request cashout. With defaults, level 1 arrives at 800 ms, but an early crash may happen first. The payout and `cashoutMultiplier` become fixed immediately. The state becomes `CASHED_OUT`; it is still a live flight. Wait until crash for `COMPLETED_WIN`, final points, crash multiplier, reward, and history. A second cashout fails without crediting again.

## Scenario 3: loss

Start another round and do not cash out. Once the server deadline is reached, state becomes `COMPLETED_LOSS`, payout remains zero, and level/booster points are retained. Reward rules apply. A background job checks due rounds every 250 ms, so closing the browser does not leave a round active forever. Reads of state, wallet, and history also catch up due rounds immediately. After a backend restart, stored deadlines and snapshots let the same job finish overdue flights.

```sh
curl -s http://localhost:8080/api/history
```

History includes round ID, theme, stake, booster selection/location/activation, fixed multiplier and payout, revealed crash multiplier, points, reward grant/redemption, result state, and timestamps. It includes only completed flights.

## Scenario 4: booster

Select DOUBLE, TRIPLE, or QUAD. Observe `booster.level`, then poll until it is reached. If reached before cashout, `booster.active` becomes true, `currentMultiplier` gains the effect, and the configured point bonus is included. Cashout before that level permanently prevents activation. x1/BASIC has no booster level or effect. See [game math](game-math.md) for exact timing and tie rules.

## Scenario 5: change points without restart

1. Open `config/game-config.yml` and change `points.points_per_level` from `10` to `37`.
2. Save the file. A background check runs every second, and every new start checks the file again before choosing its snapshot.
3. Finish any current flight. It continues using 10 points per level and its original `configVersion`.
4. Start a **new** round with a fresh request ID. Its `pointsRules.pointsPerLevel` is 37 and its `configVersion` changes.
5. Cross a level; `earnedPoints` reflects 37 per level plus any applicable bonuses. BASIC has no booster bonus, making the difference easy to inspect.

In Compose the whole `config/` directory is mounted writable for the demo admin into the backend; edit the host file. Mounting the directory also supports editors that save by replacing a file. No rebuild or restart is necessary.

The file is fully parsed and validated before publication under a synchronized lock. Each round persists its own immutable snapshot. Invalid startup config fails startup. Invalid later edits are logged, cannot replace a valid snapshot, and cause new starts/config reads to return 503 until fixed. Existing rounds, including cashout and settlement, continue independently. Use an atomic file replacement when scripting changes to avoid transient parse errors during partial writes.

## Deterministic development

Set `SKYRUSH_RANDOM_SEED=42` before starting the backend, or add it to the root `.env` for Compose and recreate the backend container. Leave it unset for `SecureRandom`. The seed is never exposed by the public API. Reproducibility requires the same configuration and sequence of starts; wall-clock cashout timing still determines payouts. Tests use a controlled server `Clock` as well as controlled random inputs, with no browser timestamps.

## Deliberate scope

Login/registration and real shared tournament scores are implemented. Real payment/lottery integration, SSE and WebSocket are absent; only tickets remain simulated. The functional frontend game flow is implemented. This is a local demo API using virtual bonus units. Reward conversions give fragments a concrete progression benefit. Current game mathematics and configuration rules are described in [game-math.md](game-math.md).


## Extended evaluator journey

Start with one of the two balloon buttons. The later theme switch still works. Open **Турнир** to see the daily UTC board, top three and pinned current player; uncheck masking to see real public nicknames. During a flight the horizontal ranking uses today's persisted account points and updates in the same polling response. Opponents are real registered accounts; personal history contains only actual persisted games.

Open **Ещё (⋯) → Настройки игры** for the evaluator admin. Change `points.pointsPerLevel`, save “Математика и награды”, close, then start BASIC. The panel and next state response show the new rule. An active round keeps its old version. Invalid values display an error and are not published. Reload forms after a version conflict. The separate “Игра и билеты” form controls game active/name/ID and simulated ticket thresholds/price/timeout. Saving one form does not silently save changes in the other.

After a qualifying completed win (default payout ≥50), the ticket offer can appear once per tab session. Accepting spends only the server-quoted bonus total; decline or timeout spends nothing. The final game payout remains unchanged. View purchased ticket count on theme/bet selection. Tickets are stored simulation collectibles, not entries in a real draw. The server enforces one offer per supplied session ID; sessionStorage also prevents redisplay after reload. Clearing storage/opening a fresh session can generate a new ID, so this is not an abuse-resistant production frequency limit. Each round can fund at most one purchase across IDs.

On the result, **Повторить ставку** validates current balance/config and starts a new round directly; **Играть снова** returns to bets with the same theme. Ten inactive seconds return to theme selection. Dialogs and a pending repeat pause the result timer. **Звук** is off initially and can be enabled/muted without affecting gameplay.

During flight open **Ещё (⋯) → Проверка полёта**, expand the integrity details and retain the commitment. After crash the exact payload appears; its SHA-256 must match. The built-in check uses Web Crypto (localhost or HTTPS); if unavailable the payload remains available for independent hashing. Only rounds created after V3 have this proof.

Refresh restores a locally known unfinished or completed round. Local metadata older than one hour is discarded. Without it, GET `/api/rounds/active` discovers the shared user's unfinished flight; if no flight remains, use History for completed games. This is deliberately simpler than future 30-day authenticated recovery.

| Endpoint | Input / result |
|---|---|
| GET /api/rounds/active | `{round: RoundView or null}` |
| GET /api/tournament?masked=true | Daily board, sorted entries, pinned player and server remainingSeconds |
| GET/PUT /api/admin/game-config | Save `{version, configuration}` using camelCase; server validates and writes snake_case YAML |
| GET/PUT /api/admin/prototype-config | Independent versioned general/upsell form |
| POST /api/upsell/offers | `{sessionId: UUID, roundId: UUID}` → `{offer, tickets}`; offer may be null |
| POST /api/upsell/offers/{id}/decision | `{sessionId: UUID, accept: boolean}` → stored offer decision/inventory |
| GET /api/upsell/tickets | Persisted simulated ticket count |

New errors: CONFIG_VALIDATION (400), CONFIG_CONFLICT/GAME_INACTIVE/OFFER_SESSION/OFFER_CLOSED (409), OFFER_NOT_FOUND (404), CONFIG_WRITE_FAILED (503). Ticket insufficiency uses INSUFFICIENT_BALANCE. Quotes reject client quantity/price; ranking has no write endpoint. See Swagger for schemas.

## Cosmetic progression (V5)

The secondary profile derives lifetime records from completed rounds. Collection unlocks use lifetime earned fragments, including fragments already exchanged for bonus balance; the existing reward/conversion formula is unchanged. Unlocks and equipped items are persistent and session-owned. Cosmetic patterns/frames never enter game mathematics or configuration snapshots. See [profile and collection](progression.md) for thresholds, API, ownership and implementation limits.

## Social progression (V6)

[Achievements, UTC daily challenge and real activity](social.md) are implemented. A completed daily challenge grants exactly one additional fragment transactionally; lifetime collection/profile totals include it. Round reward formulas and fixed payouts remain unchanged. Achievements have no currency reward. Presence and feed use slow authenticated polling, only persisted real accounts/events, and no new infrastructure.
