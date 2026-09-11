# Final evaluation checklist

Audit of the implemented repository against the supplied “Воздушный Шар” scoring criteria and the requested scope. This is an evidence checklist, **not a predicted score or a claim of full marks**. Paths below are relative to the repository root. The supplied PDF remains the authority for scoring; no optional feature is counted as mandatory work.

Status: ✓ implemented/evidenced; △ partial or team action remains; — intentionally omitted.

| Area | Status / what satisfies it | Evidence: file, API or screen | Remaining weakness | Concrete improvement / final action |
| --- | --- | --- | --- | --- |
| Presentation clarity and live demo | △ Script covers the five scenarios, model and architecture | docs/demo-script.md; README | No submitted slide deck, recording, or rehearsed speaker delivery | Team: create short deck, assign speaker/operator, rehearse ≤7 minutes |
| Gameplay loop | ✓ Bet → flight → cashout/loss → continued flight → crash → result → same-theme replay | HomePage, FlightPanel, ResultScreen; Playwright smoke | Browser/network timing can make late cashout lose | Use disclosed rehearsal preset, never promise click-time payout |
| Mathematical model and economy | ✓ Isolated Java calculations, exact formulas and numerical sensitivity analysis | gamemath/*; docs/game-math.md; GameMathTests | Uniform model is generous; no calibrated RTP or long-run scarcity | Explain positive expected returns honestly; defer calibration |
| Game parameter management | ✓ Validated YAML, hot reload, immutable persisted snapshots | GameConfigService; docs/configuration.md; config/game-config.yml | Invalid edits briefly block new starts; high allowed growth can be unplayable | Use safe example values, atomic saves and Scenario 5 instructions |
| Motivation and progression | ✓ Stake/result/history loop; persistent fragments convert to spendable bonuses | docs/product-model.md; FragmentProgress; RewardService; /api/wallet | Points now rank real registered players; empty wallet has no free refill | Present points as recorded performance, fragments as actual progression; prepare demo balance |
| Additional product features | ✓ One-hour local recovery, pending-start recovery and server active discovery | useGame, storage; orchestration tests; E2E reload | Authenticated account-specific recovery is implemented; sessions are in-memory | Demonstrate local ID and server active discovery; do not claim authenticated 30-day recovery |
| Instant repeat / tournament / ranking / upsell / admin UI | ✓ Optional implemented | New feature components; competition, upsell, AdminController; extension tests | Explicit demo simulation, no production auth/prizes | Show current rules, server validation and ticket debit; see requirements-audit.md |
| Backend architecture and data model | ✓ Modular monolith, server-authoritative DTOs, exact decimals, constraints and ledger | docs/architecture.md; V2__gameplay.sql; RoundService | Internal mutable JDBC model; demo identity only | Explain boundaries and transactional invariants, not production readiness |
| Scalability and future development | △ Per-user row locking, bounded history, indexed due rounds; client-only animation | RoundRepository, RoundCompletionJob, useRoundPolling | Current API shares one identity; polling reads lock rows; no load measurement | Acknowledge limits; add identity/access control and measure before scaling; no new infrastructure now |
| Code quality and modularity | ✓ Typed API, feature components, pure math, behavioral tests | frontend/src/features; backend feature packages | Compact JSX and one broad orchestration service could grow unwieldy | Keep scope bounded; no rewrite immediately before submission |
| Reproducibility and documentation | ✓ Wrapper/checksum, npm lockfile, environment examples, migrations, startup guide | README, Dockerfiles, docker-compose.yml | Docker build, fresh source-copy start and clean-volume reset passed; workspace has no .git directory | Team: publish source and check the actual remote clone; [Docker acceptance](acceptance/README.md) |
| Independence from paid/proprietary services | ✓ Runtime stack uses Java, React, PostgreSQL and Compose; no external paid API | build.gradle, package.json, compose | First build needs public registries; runtime OS still must support dependencies | Warm caches and use an open-source Docker runtime; do not require paid Docker Desktop |
| Backend technical verifiability | ✓ Swagger, health, public rules, state/history and explicit errors | /swagger-ui.html, /v3/api-docs, /actuator/health; integration tests | Commitment is visible initially; salted payload revealed only after crash | Retain initial commitment and verify the final disclosure; no certification claim |
| Scenario 1: start | ✓ Four bets, affordability, theme counts, rules/history before start, one debit | BetSelection; POST /api/rounds; start retry/concurrency tests | Shared user may already have an active round | Use one presenter tab and verify wallet before demo |
| Scenario 2: cashout | ✓ Level-1 gate, fixed payout, continued flight, final win, history, replay/idle return | FlightPanel, ResultScreen; cashout tests; E2E | Exact click outcome depends on server processing time | Cash out early in disclosed preset; show fixed and live values together |
| Scenario 3: loss | ✓ Stake lost, points preserved, reward and final crash displayed | COMPLETED_LOSS; results/history; E2E | Loss reward can be confused with payout | Separate labels and fragment conversion explanation now shown |
| Scenario 4: booster | ✓ Server marker, waiting/active/missed labels, multiplier effect, point feedback | BoosterStatus, FlightScene; math/integration/frontend tests | Late-booster / early-cashout branch reviewed against the real backend in the visual pass | Screenshot and scope: [visual-redesign.md](visual-redesign.md); unit/integration coverage retained |
| Scenario 5: config change | ✓ Save points setting; new round changes, active snapshot unchanged | docs/configuration.md; hot-reload tests; real API check | Poll may skip first level, so score may already be 74 | Explain total points and use BASIC to isolate level points |
| UX/UI and responsiveness | ✓ Distinct warm/cool skies, large multiplier, fixed mobile cashout, dialogs, text labels and reduced motion | styles.css; 320–1920 width checks; screenshots | Cross-browser automation results in requirements-audit.md; physical touch devices and screen readers remain team checks | Team: keyboard/mobile/browser pass and projector readability check |
| Reward visibility | ✓ Stored progress on betting/result screens, threshold and conversion wording, ledger-backed grant | FragmentProgress; ResultScreen; Rules; RewardCalculator | Public current rules can differ from an existing round's snapshot | UI explicitly labels current rules; never computes an award locally |

## Submission gate

- [ ] Source uploaded to the organizer-approved repository; clean-clone instructions tested.
- [ ] Docker build/start and all three service health checks passed on the presentation machine.
- [ ] Demo preset rehearsed with a sufficient wallet, one browser, and clear disclosure of deterministic mode.
- [ ] Default config restored and seed unset for the normal entry path.
- [ ] Slides, screenshots, backup recording and speaker timing prepared.
- [ ] Safari/Firefox or the actual evaluation browser checked; 320 px touch and keyboard interactions reviewed.
- [ ] Test/build outputs captured from the final submission revision.

This checklist reflects the fuller optional-module pass; the baseline/final requirement matrix and exact automated verification are in requirements-audit.md. unchecked human tasks above must not be presented as completed by the code audit.

Final acceptance on this machine passed Docker builds, all three healthy services, two clean-volume starts, database persistence, configuration changes and all six browser E2E flows. Backend/frontend each have 57 passing tests. See [acceptance evidence](acceptance/README.md). Presentation-machine and source-publication checkboxes remain team-owned.
