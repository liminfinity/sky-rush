# Game configuration reference

Edit `config/game-config.yml`. For a separately selected file, edit the path named by `SKYRUSH_CONFIG_PATH`; Compose normally mounts the host `config/` directory at `/app/config/`. YAML uses snake_case, while REST DTO fields use camelCase. All sections and fields are required; unknown keys, duplicate keys, missing values, and invalid ranges are rejected.

Ranges below match `GameConfiguration.validate()`. Decimal precision means at most that many meaningful decimal places (trailing zeros are allowed). Bounds are inclusive unless stated otherwise.

| Field | Meaning | Accepted values / precision | Example | Effect |
| --- | --- | --- | --- | --- |
| `themes` | Available theme map | Exactly RED and GREEN | RED, GREEN | Presentation and level schedules; not separate crash distributions |
| `themes.*.level_thresholds` | Base multiplier for each level | RED: exactly 12; GREEN: exactly 9. Strictly increasing, each 1.0001..max_multiplier, 4 decimals | `[1.2, 1.5, ...]` | Lower thresholds are reached earlier. Threshold equal to maximum cannot be reached before crash |
| `themes.*.booster_position_weights` | Relative chance per level | Same length as thresholds; each 0..1000000, 4 decimals; positive total | all 1 | Probability is weight/sum. Zero excludes a level; earlier concentration makes activation easier |
| `bet_options` | Four stake/booster pairs | Exactly four unique IDs and multipliers covering 1,2,3,4 | BASIC..QUAD | Controls available selections |
| `bet_options[].id` | Stable selection identifier | `[A-Z][A-Z0-9_]{0,31}` | BASIC | Clients send this ID, not a price; changing it can invalidate stale selections |
| `bet_options[].stake` | Bonus units debited | 0.01..1000000, 2 decimals | 10.00 | Scales amount at risk and payout; no effect on sampled crash |
| `bet_options[].booster_multiplier` | Effective multiplier factor | Integers 1,2,3,4, each once | 3 | x1 has no booster; others multiply after activation |
| `crash_model.min_multiplier` | Lower crash bound | 1.0001..100, 4 decimals; less than maximum | 1.10 | Increasing it removes early crashes |
| `crash_model.max_multiplier` | Exclusive upper crash bound | Greater than minimum, at most 100, 4 decimals | 12.00 | Increasing it permits longer flights and higher multipliers |
| `growth.multiplier_per_second` | Base multiplier growth rate | 0.01..100, 4 decimals | 0.25 | Higher values shorten real-time decision windows; do not change sampled crash distribution |
| `points.points_per_level` | Points per crossed level | Integer 0..1000000 | 10 | Changes game score only; no balance effect |
| `points.points_cashout_bonus` | Additional points for accepted cashout | Integer 0..1000000 | 25 | Rewards cashout with score, not extra payout |
| `points.booster_activation_bonuses` | Points for activating each booster | Exactly keys 1,2,3,4; integer 0..1000000; key 1 must be zero | `{1:0,2:10,3:20,4:30}` | One bonus included in total if activated; independent of stake |
| `reward.fragments_win` | Fragments on completed win | Integer 0..1000000 | 2 | Increases reward progress |
| `reward.fragments_loss` | Fragments on completed loss | Integer 0..1000000; at least one win/loss grant must be positive | 1 | Preserves some progression after losing the stake |
| `reward.fragments_per_bonus` | Conversion threshold | Integer 1..1000000 | 5 | Lower value converts fragments more often |
| `reward.bonus_amount` | Bonus units per conversion | 0.01..1000000, 2 decimals | 5.00 | Higher value increases balance reward, not game payout |

These are safety/representation limits, not recommended design extremes. A growth rate of 100 can outrun human reactions and 450 ms polling. Keep demonstrable rates near the defaults or the documented rehearsal preset.

## Scenario 5: exact demonstration

1. While a BASIC round is active, change `points_per_level: 10` to `points_per_level: 37` in the loaded file.
2. Save. Do not edit Java or restart the backend. The active round's panel still says 10 points per level.
3. Let that round finish, press Play Again, choose BASIC and start a new round.
4. Cross level 1 without cashing out. With default growth its threshold 1.2 arrives at 800 ms, unless crash occurs first.
5. The new round panel says 37 points per level. Its first level earns 37; if polling skips ahead to level 2 it correctly shows 74. Cashout adds the separate configured cashout bonus. Verify `pointsRules.pointsPerLevel` and `configVersion` at `/api/rounds/{id}/state` if needed.
6. Restore 10 after the demonstration. The just-started round retains 37.

Use the [demo script](demo-script.md) preset to avoid an early crash while demonstrating this change. BASIC avoids a booster bonus obscuring the example.

## Reload, snapshot and failure behavior

The server checks the file every second and again before new starts. It reads and validates a complete candidate under synchronization, then publishes an immutable snapshot. Existing rounds persist their snapshot in PostgreSQL and survive both later edits and backend restart. The version is a SHA-256 of file bytes: even comments change the version without changing numerical behavior.

Missing/invalid initial config fails startup. Invalid later config blocks new starts and public config reads with `503 CONFIG_INVALID`; it does not break existing round state, cashout or settlement. Correct the file to recover. Save atomically when scripting changes; a partial write can briefly cause 503.

Current fragment progress is account-wide. Conversion uses the completing round's reward snapshot, even if current public rules have changed. A fragment meter labelled “current rules” is informational, not a promise to convert an already-running round at a new threshold.

The environment seed is not part of this YAML and is never public. Set/unset `SKYRUSH_RANDOM_SEED` before backend startup; changing it requires a restart. See README for credentials, demo enablement and connection variables.


## Evaluator admin and prototype settings

The header **Настройки игры** exposes typed controls for every gameplay field above. Counts and model type remain fixed to avoid violating the mandatory rules. Each save sends the displayed `version` and candidate configuration; server validation reports specific rule errors, a stale admin version returns 409, and only a valid candidate is atomically published. File comments/formatting are normalized on an admin save. Do not edit externally at the same time: the version guard coordinates API saves, not an OS-level editor lock. The backend needs directory write permissions. Compose uses LOCAL_UID/LOCAL_GID as documented in README.

`config/prototype-config.yml` has a separate form/version, read on new starts, offer issuance and admin reads. Override its path with `SKYRUSH_PROTOTYPE_CONFIG_PATH`. Both files must exist at startup. Settings apply to future operations; active game snapshots and issued ticket quotes are unchanged.

| Field | Valid values | Default / effect |
|---|---|---|
| game_id | lowercase letters/digits/hyphens, 1–32 | sky-rush; evaluator metadata |
| name | nonblank, up to 80 chars | SkyRush; evaluator metadata (brand artwork stays SkyRush) |
| type | BALLOON_CRASH only | Fixed supported gameplay type |
| active | boolean | true; false blocks new starts, including repeat, but permits active cashout/settlement |
| upsell.enabled | boolean | true; whether new quotes may be issued |
| upsell.min_win_amount | 0.01–1000000, ≤2 decimals | 50.00; minimum completed-win payout |
| upsell.popup_timeout_seconds | integer3–60 | 10; server deadline starts at offer issuance, retries do not extend it |
| upsell.ticket_price | 0.01–1000000, ≤2 decimals | 5.00 virtual bonus units |
| upsell.max_tickets | integer1–100 | 5; cap per offer |
| upsell.payout_fraction | >0 through1, ≤4 decimals | 0.10; payout fraction in server quote budget |

Session frequency is fixed at one issued offer per session ID; it is not reset by admin changes. Local game sessions are tab-scoped sessionStorage IDs, not authenticated users. Purchased ticket counts persist in PostgreSQL. Rounding and example budgets are in game-math.md.
