> Account iteration supersedes earlier demo-only identity/roster limitations below. Real registration, sessions, account isolation and real shared daily scores are now implemented. See [accounts.md](accounts.md) for the current architecture and verification record. The earlier audit is retained as historical scope/evidence, not a claim that accounts are still absent.

# Requirements audit

Baseline inspected before implementation. Status vocabulary: IMPLEMENTED, PARTIAL, MISSING, NOT REQUIRED / EXAMPLE ONLY. Classification is separate from status: requested optional modules are not reclassified as mandatory hackathon rules. No score is predicted. Paths are repository-relative. The workspace has no Git metadata, so git status/diff is unavailable.

## Mandatory baseline (A)

| ID | Requirement | Class | Baseline | Evidence |
|---|---|---|---|---|
| A1 | RED/GREEN versions | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A2 | RED exactly 12 levels | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A3 | GREEN exactly 9 levels | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A4 | Four stakes | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A5 | Booster tiers x1–x4 | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A6 | Unaffordable bets disabled | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A7 | Rules before start | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A8 | Completed history accessible | MANDATORY | IMPLEMENTED | HistoryController; RoundRepository.history; HistoryList; history.test.tsx |
| A9 | Stake deducted exactly once | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A10 | Hidden server crash sampled before flight | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A11 | Server booster position | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A12 | Cashout after level 1 | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A13 | Server payout stake × multiplier | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A14 | Fixed payout immutable | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A15 | Flight continues after cashout | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A16 | No early cashout | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A17 | No duplicate cashout | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A18 | No cashout after crash | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A19 | Loss without cashout | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A20 | Configured level points | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A21 | Configured booster points | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A22 | Booster effect before cashout only | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A23 | Early cashout permanently misses booster | MANDATORY | IMPLEMENTED | RoundService; GameplayIntegrationTests |
| A24 | Complete result fields | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A25 | Completed history persistence | MANDATORY | IMPLEMENTED | HistoryController; RoundRepository.history; HistoryList; history.test.tsx |
| A26 | Play Again preserves theme | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A27 | 10-second result inactivity return | MANDATORY | IMPLEMENTED | HomePage; BetSelection; FlightPanel; ResultScreen; screens/orchestration tests; e2e/gameplay.spec.ts |
| A28 | Balance/points/rewards separated | MANDATORY | IMPLEMENTED | RewardService; FragmentProgress; Rules; fragmentProgressionCreditsBonusOnce |
| A29 | Useful fragment progression | MANDATORY | IMPLEMENTED | RewardService; FragmentProgress; Rules; fragmentProgressionCreditsBonusOnce |
| A30 | External config | MANDATORY | IMPLEMENTED | GameConfigService; GameConfigTests; hotReloadAffectsOnlyNewRoundSnapshotsEvenAcrossRepositoryReads |
| A31 | New round adopts points changes | MANDATORY | IMPLEMENTED | GameConfigService; GameConfigTests; hotReloadAffectsOnlyNewRoundSnapshotsEvenAcrossRepositoryReads |
| A32 | Active config snapshot isolation | MANDATORY | IMPLEMENTED | GameConfigService; GameConfigTests; hotReloadAffectsOnlyNewRoundSnapshotsEvenAcrossRepositoryReads |

## Requested extension baseline

| ID | Requirement | Class | Baseline | Evidence / gap and implementation plan |
|---|---|---|---|---|
| B | Separate animated theme screen | OPTIONAL REQUESTED | MISSING | Existing entry is BetSelection; add ThemeSelection using Balloon |
| C | Live horizontal ranking ≤1s | OPTIONAL REQUESTED | MISSING | Add server-owned demo scores to polled round DTO; no browser scores |
| D | Tournament/top3/current player/status/time/masking | OPTIONAL REQUESTED | MISSING | Add bounded daily demo tournament view, server totals and labelled simulated nicknames |
| E | Qualifying one-session simulated ticket offer, expiry and transactional purchase | OPTIONAL REQUESTED | MISSING | Add persisted offers/purchases, server prices/eligibility, sessionStorage identity; no real lottery |
| F | Typed validated demo admin | OPTIONAL REQUESTED | MISSING | Existing GameConfigService validates YAML; add versioned atomic saves and forms; snapshots unchanged |
| G | Instant repeat | OPTIONAL REQUESTED | MISSING | Existing Play Again only; reuse start command/idempotency with new config/balance |
| H | Recovery with expiry and discovery | QUALITY / SUPPORT | PARTIAL | storage/useGame restores known round; add 1h expiry and active discovery |
| I | Outcome commitment/reveal | OPTIONAL REQUESTED | MISSING | Hidden outcome persisted but no public commitment; add salted SHA-256 proof |
| J | Visual/audio feedback | QUALITY / SUPPORT | PARTIAL | FlightScene/Panel/BoosterStatus cover core visual feedback; add ranking/theme motion and opt-in sound |
| K | Performance | QUALITY / SUPPORT | PARTIAL | Serialized 450ms useRoundPolling, CSS transforms; profile new query/render paths; no measured FPS guarantee |
| L | 320–1920 responsiveness | QUALITY / SUPPORT | PARTIAL | Existing six-width Chromium smoke; extend new screens |
| M | Chrome/Firefox/Safari portability | QUALITY / SUPPORT | PARTIAL | Chromium verified previously; attempt Firefox/WebKit smoke (not branded stable Safari certification) |
| N | Authority/write integrity | QUALITY / SUPPORT | PARTIAL | Strict DTOs and user locks; add coverage for admin and tickets; demo admin is privileged, unauthenticated |
| O | Persistent history / coherent participants | QUALITY / SUPPORT | IMPLEMENTED | RoundRepository and HistoryList persist actual user rounds; CompetitionService uses only registered accounts and persisted points |
| P | Independent docs | QUALITY / SUPPORT | PARTIAL | README/docs cover MVP only; update extension endpoints and limitations |
| Q | Automated coverage | QUALITY / SUPPORT | PARTIAL | 42 backend/38 frontend tests baseline; extend optional features |
| R | Full runtime/build/browser/Docker verification | QUALITY / SUPPORT | PARTIAL | Repeat after changes; Docker daemon previously unavailable |
| S | Priority/modular monolith | QUALITY / SUPPORT | IMPLEMENTED | Preserve server math and transactions; no new infrastructure |

## Examples / exclusions

| Requirement | Status | Reason |
|---|---|---|
| Exact reference pixels, bird assets and sound timing | NOT REQUIRED / EXAMPLE ONLY | Original CSS/SVG and usable feedback satisfy intent |
| Real lottery/payment integrations | NOT REQUIRED / EXAMPLE ONLY | Explicitly excluded; tickets are a simulation |
| 30-day cross-device authenticated recovery | NOT REQUIRED / EXAMPLE ONLY | Requested prototype scope is one-hour local metadata plus simple server discovery |
| Cryptographic gambling certification | NOT REQUIRED / EXAMPLE ONLY | Commitment demonstrates unchanged outcome only |
| Editable theme counts outside RED12/GREEN9 | NOT REQUIRED / EXAMPLE ONLY | Would violate mandatory counts; thresholds/weights remain editable |
| Multiple crash-distribution algorithms | NOT REQUIRED / EXAMPLE ONLY | Keep existing uniform model and expose its parameters |
| Production auth, distributed services, external deployment | NOT REQUIRED / EXAMPLE ONLY | Explicitly excluded |

## Final implementation and verification

The baseline above is retained to show what changed. All A1–A32 are IMPLEMENTED in the final application. A27 now returns to theme selection on idle (PDF §1.5), while A26 remains same-theme betting. The final detail matrix below distinguishes implemented optional functionality from examples and environment-dependent verification.

| ID | Final requirement | Class | Final status | Evidence |
|---|---|---|---|---|
| B1 | Animated separate entry, selectable RED12/GREEN9, original style, later switching | OPTIONAL IMPLEMENTED | IMPLEMENTED | ThemeSelection; Balloon; styles.css; extensions test and E2E |
| C1 | Current player plus several competitors, descending server points | OPTIONAL IMPLEMENTED | IMPLEMENTED | CompetitionService.live; RoundView.ranking; liveRankingUsesAuthoritativePointsAndDescendingOrder |
| C2 | Live update, moved position/highlight and horizontal scroll follows player | OPTIONAL IMPLEMENTED | IMPLEMENTED | LiveRanking; normal serialized 450ms useRoundPolling; no ranking query on flight poll |
| C3 | Simulation owned by server and disclosed | OPTIONAL IMPLEMENTED | IMPLEMENTED | CompetitionService formulas; live UI caption; game-math.md |
| D1 | Trophy access, prominent top3, scrollable rest, pinned player | OPTIONAL IMPLEMENTED | IMPLEMENTED | TournamentTable; .podium/.tournament-rest/.pinned-player; real E2E |
| D2 | Scores, active status, server remaining time, plausible masked names | OPTIONAL IMPLEMENTED | IMPLEMENTED | GET /api/tournament?masked=true; competition tests; 900ms serialized dialog poll |
| E1 | Only completed qualifying wins and configurable minimum | OPTIONAL IMPLEMENTED | IMPLEMENTED | UpsellService.offer; upsellOnlyQualifyingCompletedWins |
| E2 | Once per tab/game session, including declines/timeouts/later wins | OPTIONAL IMPLEMENTED | IMPLEMENTED | unique(user_id,session_id); UpsellPrompt/sessionStorage; offerExpiryDeclineSessionAndDuplicateRoundAreProtected; E2E second win |
| E3 | Server quote from payout/stake/balance, no client price | OPTIONAL IMPLEMENTED | IMPLEMENTED | gamemath/TicketQuoteCalculator; strict OfferRequest/Decision; config/prototype-config.yml |
| E4 | Accept/decline, persisted tickets, atomic debit, no double purchase | OPTIONAL IMPLEMENTED | IMPLEMENTED | ticket_offers; same user lock; purchase concurrency/rollback/insufficiency tests; E2E purchase |
| E5 | Configurable server expiry/autoclose, default10s, explicit imitation | OPTIONAL IMPLEMENTED | IMPLEMENTED | Offer.expiresAt/remainingSeconds; UpsellPrompt; timer test; no real lottery integration |
| F1 | Typed general ID/name/type/active controls | OPTIONAL IMPLEMENTED | IMPLEMENTED | AdminPanel; PrototypeConfiguration; inactiveGamePreservesCurrentCashoutAndPrototypeValidation |
| F2 | Math min/max/growth, theme thresholds/weights, four stakes/boosters | OPTIONAL IMPLEMENTED | IMPLEMENTED | AdminPanel Fields; GET/PUT /api/admin/game-config; existing GameConfiguration validator |
| F3 | Points, cashout/booster bonuses, fragments, all upsell settings | OPTIONAL IMPLEMENTED | IMPLEMENTED | Both admin forms; GameConfiguration/PrototypeConfiguration validation |
| F4 | Explanations, useful errors, validation, save/apply feedback | OPTIONAL IMPLEMENTED | IMPLEMENTED | AdminPanel help; CONFIG_VALIDATION/CONFIG_CONFLICT; frontend admin tests; fractional-integer rejection |
| F5 | New-round updates, old snapshots, file reuse, thread safety | OPTIONAL IMPLEMENTED | IMPLEMENTED | Synchronized validate/version-check/atomic rename; adminSaveValidatesVersionAndRetainsActiveSnapshot; real E2E |
| F6 | Explicit demo admin without production auth | OPTIONAL IMPLEMENTED | IMPLEMENTED | UI warning; localhost Compose ports; README trusted local scope; demo-disabled guard |
| G1 | Direct same-theme/bet repeat with fresh ID/current config/balance | OPTIONAL IMPLEMENTED | IMPLEMENTED | useGame.start(repeat); ResultScreen; orchestration tests; real E2E |
| H1 | Known active/completed recovery, safe stale discard, one-hour metadata | QUALITY / SUPPORT | IMPLEMENTED | storage.ts; useGame.restore; orchestration and extension tests |
| H2 | Server active discovery without browser storage | OPTIONAL IMPLEMENTED | IMPLEMENTED | GET /api/rounds/active; activeDiscoveryFindsFlightAndClearsAfterSettlement; real E2E |
| H3 | Strict Mode abort/cancellation race handled | QUALITY / SUPPORT | IMPLEMENTED | Captured lifecycle signal in discovery; regression test; E2E no spurious alert |
| I1 | Hidden outcome committed before initial response | OPTIONAL IMPLEMENTED | IMPLEMENTED | OutcomeProof; separate SecureRandom salt; proof_hash/proof_salt; initial response assertion |
| I2 | Reveal only after crash and independent hash verification | OPTIONAL IMPLEMENTED | IMPLEMENTED | integrity.reveal; IntegrityProof; backend tamper/seed tests; real browser SHA-256 check |
| J1 | Multiplier/levels/points/booster/cashout/crash/reward feedback | MANDATORY | IMPLEMENTED | Existing FlightScene/Panel/BoosterStatus/FragmentProgress/ResultScreen retained; all previous tests |
| J2 | Theme floating/clouds, ranking highlight, opt-in muted/throttled audio | OPTIONAL IMPLEMENTED | IMPLEMENTED | ThemeSelection/styles.css; LiveRanking; useGameAudio; reduced-motion rules |
| K1 | No overlapping polls, timer/abort cleanup, lightweight animation | QUALITY / SUPPORT | IMPLEMENTED | useRoundPolling tests; TournamentTable cleanup; CSS transforms; no new framework |
| K2 | Avoid excessive additional database queries | QUALITY / SUPPORT | IMPLEMENTED | No query in live ranking; one indexed daily SUM per tournament poll; bounded roster; no load/FPS guarantee |
| L1 | Six widths for theme/bets/flight/tournament/admin/upsell/result/history/rules | QUALITY / SUPPORT | IMPLEMENTED | e2e/extensions.spec.ts and gameplay.spec.ts width loops; runtime screenshots; screen content scrolls as needed |
| M1 | Portable current-engine code; no unsupported experimental dependency | QUALITY / SUPPORT | IMPLEMENTED | Native dialog, Web Crypto localhost/HTTPS with verification fallback; opt-in AudioContext failure message; browser execution below |
| N1 | No client outcomes/ranking writes or monetary quote inputs | MANDATORY | IMPLEMENTED | Strict Jackson unknown/missing/null/float validation; intention DTOs; malicious input tests |
| N2 | Idempotent stakes/cashout/payout/ticket debit, invalid admin blocked | MANDATORY | IMPLEMENTED | Existing concurrency tests plus ticket concurrency/rollback/validation tests; constraints V2/V3 |
| O1 | Persisted history coherent with actual users; simulated identities disclosed | MANDATORY | IMPLEMENTED | One actual demo identity; RoundRepository.history/HistoryList; no fictional opponent rounds injected |
| P1 | Startup/API/modules/math/config/demo/limits documentation | QUALITY / SUPPORT | IMPLEMENTED | README; architecture/gameplay/game-math/configuration/demo-script; this audit |
| Q1 | Preserve baseline and extend backend/frontend behavior tests | QUALITY / SUPPORT | IMPLEMENTED | 56 backend and 56 frontend tests; detailed checks below |
| R1 | Real PostgreSQL migration/startup/Swagger and frontend integration | QUALITY / SUPPORT | IMPLEMENTED | V3 migration applied to existing V2 database; health/OpenAPI; real Playwright no route mocks |
| S1 | Preserve mandatory math and modular monolith | MANDATORY | IMPLEMENTED | No crash/growth/payout/point/reward formula changes; no distributed infrastructure |


## Concrete limitations (not silently counted as implemented)

| Requirement / claim | Status | Reason / remaining action |
|---|---|---|
| Measured 60 FPS on all evaluation hardware and load-tested scale | PARTIAL | Lightweight transforms/bounded polling are implemented; no hardware-wide frame-rate or concurrency capacity claim. Profile on target machine |
| Branded current Chrome/Safari/Firefox on physical devices | PARTIAL | Playwright engine results below are automated evidence, not certification of every branded browser/device |
| Docker container build/start | IMPLEMENTED | Final acceptance: all three containers healthy; clean-volume startup, persistent restart and Docker-served cross-browser E2E passed. See [acceptance](acceptance/README.md) |
| Real multiplayer competition and account isolation | OPTIONAL IMPLEMENTED | Real registration/session accounts, private history and shared daily scores; see accounts.md and copy-and-ranking-audit.md |
| Independent tournament prize/payout or ticket drawing | NOT REQUIRED / EXAMPLE ONLY | Not requested; no invented real-money value or lottery integration |
| Production admin security / abuse-resistant session limits | NOT REQUIRED / EXAMPLE ONLY | Explicit unauthenticated evaluator tool; session IDs are replaceable client identifiers. Never deploy publicly as production |
| 30-day authenticated recovery | NOT REQUIRED / EXAMPLE ONLY | One-hour local metadata plus shared-user active discovery is the requested simplified prototype |
| Exact mockup art, birds/sound cadence, multiple distributions, arbitrary level counts | NOT REQUIRED / EXAMPLE ONLY | Decorative examples; fixed counts/model preserve mandatory correctness |
| Slides, repository publication, seven-minute delivery | PARTIAL | Source/docs/demo script ready; team-owned submission/presentation work. No Git metadata in workspace |

## Original extension verification record

The following records the earlier extension pass. It is superseded for release readiness by the [2026-09-11 Docker acceptance](acceptance/README.md): 57 backend tests, 57 frontend tests, six Docker E2E flows, both builds and clean-volume startup passed.

- Backend: **56 tests, 0 failures/errors/skips**, `./gradlew test build` successful. Existing tests preserved; migration assertion advanced from V2 to V3.
- Frontend: **56 tests passed**, TypeScript check and production build successful. Tests include repeat, stale recovery and Strict Mode cancellation, admin typing/errors, tournament, offer acceptance/decline/timeout and unchanged mandatory flow.
- PostgreSQL: real local PostgreSQL17; Flyway validated all three migrations and upgraded an existing V2 schema to V3. Separate disposable PostgreSQL also runs in integration tests.
- Chromium: **2 real-backend E2E tests passed**, including mandatory gameplay and all extension flows. No API route mocks. Layout loops cover 320,375,768,1024,1440,1920.
- Firefox: **2 real-backend E2E tests passed**. WebKit: **2 real-backend E2E tests passed**. Together with Chromium: **6 passing browser flows**. These are Playwright engines, not physical-device or branded stable-browser certification.
- Docker Compose: standalone Compose `--env-file .env.example config --quiet` passed. Container runtime unavailable.
- Direct real API check: external YAML points change 10→11 affected the next round while the active round retained 10; restored temporary config afterwards. Initial commitment matched final reveal. Health was UP and new paths appeared in OpenAPI. Twenty sequential localhost state reads measured median 1.05ms / max 1.75ms on this machine only; this is not a load/FPS benchmark.
- Runtime configuration uses disposable copies of game-demo.yml/prototype-config.yml, with seed42; committed default gameplay parameters remain unchanged.

No requested functional optional module was intentionally skipped. The exceptions above are excluded product examples, deployment limitations, or measurements that cannot honestly be inferred from passing functional tests.

## Presentation follow-up

The frontend-only scene redesign preserves this functional audit. Its latest test counts, browser checks, screenshot review and copy changes are recorded in [visual-redesign.md](visual-redesign.md). Evaluator access is now **Ещё (⋯) → Настройки игры**; the in-flight proof is under **Ещё (⋯) → Проверка полёта**.

## Release-candidate follow-up

Docker startup is now verified, including a fresh source copy, production frontend proxying, PostgreSQL volume persistence, V1–V3 initialization on empty volumes and all three healthy services. The only code/configuration correction to backend behavior was a stable `public` Flyway history location, with a regression test; game rules are unchanged. See [acceptance report and evidence](acceptance/README.md). Remote source publication and the actual presentation-machine rehearsal remain team tasks.
