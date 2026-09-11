# SkyRush game mathematics (model v1)

All calculations live in `backend/src/main/java/com/skyrush/gamemath`. Controllers accept intentions; repositories store inputs and outcomes. This is a simple virtual-bonus hackathon model, not a calibrated return-to-player or certified provably-fair system. A separate commitment demonstration is described below.

## Inputs and random generation

Each round stores its complete immutable configuration, SHA-256 configuration version, theme, selected bet, server start time, base crash point, booster level, and derived crash deadline. No seed or random generator state is stored or returned. These inputs and the cashout timestamp let an evaluator reconstruct the result from the database.

`RandomProvider.nextUnit()` supplies a uniform decimal sample `U` in `[0, 1)`. The default provider uses `SecureRandom`. `SKYRUSH_RANDOM_SEED` selects a Java `Random` with a fixed signed 64-bit seed for development. Identical seeds, configurations, and draw order yield identical crash points and booster positions. A boosted round consumes two draws (crash, then booster); x1 consumes only the crash draw. Rejected or retried starts do not normally draw; a transaction failing after generation may consume draws despite rolling back. Concurrent calls are synchronized per draw, so reproducibility assumes the same request order. Restarting with a seed resets the sequence. Existing rounds never resample.

## Crash and flight

Let `a = crash_model.min_multiplier`, `b = crash_model.max_multiplier`, and `g = growth.multiplier_per_second`.

```text
C = floor4(a + U × (b - a))
durationMs = ceil((C - 1) × 1000 / g)
crashAt = startedAt + durationMs
B(t) = floor4(1 + g × max(0, elapsedMilliseconds) / 1000)
```

`floor4` truncates to four decimal places. `C` is sampled before flight begins and lies in `[a, b)`. `B` is the base multiplier, independent of boosters. Server time has millisecond resolution; timestamps are captured after acquiring the user lock, so a queued cashout cannot use stale request-arrival time.

Crash occurs when `serverTime >= crashAt`. At crash the public multiplier is exactly `C` times the eligible booster effect, rather than a value extrapolated beyond crash. Until crash, neither `C` nor `crashAt` is returned. The browser receives only current server state; it cannot set time or multipliers.

The default `a=1.10`, `b=12.00`, `g=0.25` gives flights from 0.4 seconds up to 44 seconds at millisecond resolution. The uniform distribution is intentionally easy to explain and test. It does not target a particular house edge.

## Levels and boosters

A level is crossed when the **base** multiplier reaches its configured threshold before crash. RED has 12 thresholds and GREEN has 9. A boosted effective multiplier does not skip levels or recursively trigger more bonuses.

For weight `w[i]` at each level, booster position is sampled using a second uniform draw:

```text
probability(level i) = w[i] / sum(w)
choose first i where cumulativeWeight(i) > U × sum(w)
```

Weights may be zero but must sum to a positive value. Default equal weights give every level equal probability. The chosen level is public from the start; x1 has no level and never activates a booster.

For x2/x3/x4, reaching the chosen level while cashout is still available activates the booster. Effective multiplier `M = B × boosterMultiplier` once activated, otherwise `M = B`. If cashout precedes the booster level, the booster is permanently disabled for that round. An already activated booster stays active during the remaining flight.

The server processes crossed levels and booster activation before cashout at the same millisecond. Crash has priority over both: a threshold simultaneous with crash is **not** crossed. At completion, levels are evaluated at `crashAt - 1 ms`. This explicit tie rule prevents cashout or booster awards at crash.

## Payout and points

Cashout requires at least level 1 and `now < crashAt`. Only one cashout is accepted.

```text
fixedMultiplier = effective multiplier at cashout
payout = floor2(stake × fixedMultiplier)
points = crossedLevels × points_per_level
       + (cashedOut ? points_cashout_bonus : 0)
       + (boosterActivated ? configuredBoosterBonus : 0)
```

Payout is credited immediately and never recalculated. Flight continues until crash. Level points continue accumulating after cashout, but a missed booster cannot activate afterwards. Losses preserve crossed-level and activated-booster points; they receive no cashout bonus. Points are recalculated as a total from the snapshot and timing, never incremented on every poll, so repeated polling cannot duplicate awards.

All decimal calculations use `BigDecimal`. Persisted stakes and payouts use exact `NUMERIC(...,2)`; multipliers use `NUMERIC(12,4)`. Payout truncates down to two decimal places once, after multiplying the stake by the fixed four-decimal multiplier. For example, `10.01 × 1.2345 = 12.357345`, paid as `12.35`. Game points are integer values and are distinct from wallet bonus balance.

## Sky Fragments

Default completion grants: win = 2 fragments, loss = 1. Every 5 fragments automatically converts to 5.00 bonus balance, with the remainder retained. The round's reward snapshot governs both its fragment grant and conversion. A config change does not retroactively convert existing fragments; conversion occurs when the next round completes under that round's rules.

```text
heldPlusGrant = heldFragments + (win ? fragments_win : fragments_loss)
conversions = floor(heldPlusGrant / fragments_per_bonus)
redeemed = conversions × fragments_per_bonus
remaining = heldPlusGrant - redeemed
bonusCredit = conversions × bonus_amount
```

Fragments are stored separately in `reward_progress`; the resulting credit has its own `FRAGMENT_BONUS` ledger entry and is not part of the fixed game payout. Completion, reward progression, and any bonus credit share one transaction. Repeated state/history requests cannot redeem twice.

## Examples with default rules

1. GREEN/BASIC stakes 10.00. At 1 second the base multiplier is 1.2500, level 1 is crossed, and cashout pays 12.50. Points at that moment are `10 + 25 = 35`. Later level crossings add points without changing the 12.50 payout.
2. GREEN/TRIPLE stakes 30.00 with booster at level 1. At 1 second the effective multiplier is `1.25 × 3 = 3.75`. Cashout pays 112.50 and points are `10 + 25 + 20 = 55`.
3. The same TRIPLE bet with booster at level 2, cashed out at 1 second, pays 37.50. Level 2 arrives at 2 seconds, but the booster stays inactive forever.
4. `U=0` produces `C=1.10`: crash at 400 ms, before level 1 (800 ms). The result is a loss, payout and points are zero, and one fragment is granted.
5. A completed loss while holding 4 fragments grants 1, redeems 5, credits 5.00 bonus balance, and leaves 0 fragments. The loss payout remains zero.


## Parameter sensitivity and economy analysis

The following simple calculations use the continuous uniform approximation, ignoring four-decimal truncation, millisecond ties, request latency, and two-decimal payout truncation. They explain behavior; exact outcomes still use the Java implementation above.

For a target **base** multiplier `x` between `a` and `b`, probability of reaching it before crash is approximately `(b-x)/(b-a)`. For targets below `a`, it is approximately 1; at or above `b`, zero. Default first-level survival is `(12-1.2)/10.9 ≈ 99.08%`. Reaching base 3 gives about 82.57%; base 6 gives about 55.05%. These are unconditional start-of-round probabilities, not guarantees for an individual flight.

| Change | Simple consequence with other parameters held fixed |
| --- | --- |
| Growth 0.25 → 0.50 | Level 1 arrives in 0.4 rather than 0.8 seconds; base 3 arrives in 4 rather than 8 seconds. Same crash samples, less reaction time |
| Maximum 12 → 16 | Approximate survival to base 3 rises from 82.57% to 87.25%; mean flight duration rises from about 22.2 to 30.2 seconds at growth 0.25 |
| Minimum 1.10 → 1.50 | No crash before base 1.50; level 1 becomes safely reachable in the model, but network delays still matter |
| Points per level 10 → 37 | Four crossed levels yield 148 rather than 40 level points; stake, multiplier and payout unchanged |
| Booster weights moved to level 1 | Booster activates much earlier for players who wait; crash generation is unchanged |
| Fragment threshold 5 → 10 | Ten rather than five default loss fragments are needed for each 5.00 bonus conversion |
| Conversion bonus 5 → 10 | Same fragment progress, double the balance injection per conversion |

For BASIC stake 10 and a policy of cashout at base 3, expected gross payout is approximately `10 × 3 × 9/10.9 = 24.77`, compared with the 10 stake. This is a **positive expected net bonus gain of about 14.77**, before fragments and latency. It demonstrates that the model is generous and not a calibrated gambling economy. A higher stake scales both exposure and gross payout; stronger paired boosters can amplify returns further.

Conditional on a chosen booster level being reached and activated, payout uses the boosted multiplier. One cannot multiply every round's expected payout by the booster factor: some rounds crash first and some players cash out before activation. Equal level weights also do not mean equal activation chances, because later levels are harder to reach.

RED and GREEN share crash and growth parameters. RED's extra milestones change point opportunities and the distribution of booster thresholds; they do not directly increase crash probability. Game points currently provide a per-round performance record, not a spendable economy. Sky Fragments provide the actual cross-round conversion benefit.

No model change was made during the final audit. Calibration, long-run balance policy and strategy simulations remain future product work. Exact valid parameter ranges and operational limits are in [configuration.md](configuration.md).


## Outcome commitment demonstration

Before insert/initial response, sample crash and booster as before, then generate an independent 32-byte SecureRandom salt (64 lowercase hex characters). It consumes **no** seeded gameplay draws. Build this exact UTF-8 string, with literal `|` separators and no trailing newline:

```text
skyrush-v1|roundUUID|RED-or-GREEN|crashBaseFourDecimals|boosterInteger|levelInteger-or-none|configVersion|startedAtInstant|saltHex
```

`startedAtInstant` uses Java Instant.toString() at millisecond resolution (it may omit zero fractional seconds). Publish `SHA-256(payload)` as 64 lowercase hex in `integrity.commitment`, with `integrity.reveal=null`. After completion reveal the exact original payload. The UI hashes the received string byte-for-byte; no reconstruction or decimal reformatting is needed. A changed crash point, booster, salt, round ID, configuration version or start timestamp changes the hash. The random salt prevents brute-forcing the small crash space before reveal. Legacy V2 rounds have `integrity=null`.

This demonstrates that a **previously retained** commitment matches the final disclosure. It does not prove unbiased initial sampling, rule quality, honest server software, an external timestamp, or cryptographic gambling certification. An evaluator should retain the initial response instead of trusting only a final hash fetched from the same server. Public results keep effective crash multiplier, while the reveal includes its base input. Default randomness remains SecureRandom; seeded demo outcomes repeat, but salts/UUIDs and therefore commitments do not.

## Optional tickets and competition

Ticket offers do not alter any round formula. Only completed wins at least `min_win_amount` qualify:

```text
budget = min(payout × payout_fraction, original stake, current bonus balance)
quantity = min(max_tickets, floor(budget / ticket_price))
total = quantity × ticket_price
```

No offer if quantity is zero. With payout112.50, stake30, balance1000, fraction0.10, price5, cap5: budget11.25, quantity2, total10.00. Quantity/price/total are snapshotted by the server and rechecked against current balance on purchase; no client monetary inputs are accepted. Total uses exact decimals; quantity division floors to an integer. Purchased rows are inventory/debit evidence, not a random draw. Existing offers retain price and timeout when admin changes settings.

Live ranking demo opponent `i=0..4` has `(i+1)×7 + elapsedWholeSeconds×(i mod 3 + 1)` points. Real entry is the round's earnedPoints. Daily UTC tournament opponent `i=0..11` has `(i+1)×20 + elapsedWholeMinutesToday×(i mod 3 + 1)`; real entry is the persisted sum of earnedPoints for rounds started that day, including an active round's last persisted score. These are explicitly simulated pacing formulas, not client-generated scores or actual played opponent rounds. Both views are sorted descending by the server. Daily rollover starts a new automatic demo tournament, no monetary reward.

## Cosmetic progression (V5)

The secondary profile derives lifetime records from completed rounds. Collection unlocks use lifetime earned fragments, including fragments already exchanged for bonus balance; the existing reward/conversion formula is unchanged. Unlocks and equipped items are persistent and session-owned. Cosmetic patterns/frames never enter game mathematics or configuration snapshots. See [profile and collection](progression.md) for thresholds, API, ownership and implementation limits.
