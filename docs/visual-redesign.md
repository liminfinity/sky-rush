# Player experience: visual redesign

This pass changes presentation only. REST DTOs, polling, transactions, game mathematics, configuration and server authority remain unchanged. The existing audio, recovery, ranking, tournament, ticket simulation, repeat bet, proof verification and evaluator forms remain available.

## Direction and initial review

The previous player screens gave headings, explanatory panels, status boxes and the balloon similar visual weight. Bet options resembled pricing cards; a small flight viewport sat inside a conventional application layout. The existing application was run and its theme, betting, flight, cashout, result/offer and secondary dialogs were captured before editing. The brief's illustrated theme, bet, flight and booster references were inspected for spatial hierarchy, without reusing their artwork.

The new composition uses an illustrated turquoise coast for GREEN and a coral sunset with sharper mountains for RED. Balloons are the theme-selection controls. The flight fills the viewport beneath a compact HUD. The multiplier, hero balloon, vertical altitude markers and gold cashout control carry the experience; secondary information is visually quieter.

## Components and assets

- `SkyWorld.tsx`: original layered SVG landscapes, sun/haze, three cloud depths and birds. No external assets or image requests.
- `Balloon.tsx`: original fabric/seams/highlights, ropes, burner and woven basket; theme colours come from CSS.
- `GameIcon.tsx`: consistent SVG controls, faceted reward fragment and score star. No emoji graphics or external icon library.
- `ThemeSelection` / `BetSelection`: large interactive balloons and four tactile bet tokens. Amount, booster and affordability retain their server data and validation.
- `FlightScene` / `FlightPanel` / `BoosterStatus`: scene-level multiplier, altitude path, booster token and cashout dock. Cashout keeps the fixed payout visible while the flight continues.
- `ResultScreen` / `FragmentProgress`: payout, points and fragments form the reward composition over the landscape. Play Again is primary; repeat is secondary. Fragment exchange and progress are retained.
- `AppShell` / `HomePage`: compact icon navigation. The overflow menu contains evaluator access and the in-flight commitment check. Admin keeps structured form controls.

All SVG artwork was authored in this repository for SkyRush. No third-party artwork or font attribution is required. The font stack uses locally available Trebuchet MS / Arial / sans-serif; no font binaries are bundled.

## Copy audit

| Previous pattern | New treatment |
| --- | --- |
| Persistent explanation of server state, level rules and flight behaviour | Removed from the primary flight scene; detailed rules and integrity verification remain in secondary views. |
| `Доступно после первого уровня` | `После первого уровня`, only while the action is unavailable. |
| `Ожидает своего уровня` | A token at the server-provided level and `До ×3 — 2 ур.` |
| `Активирован · +20 очков` | `×3 активирован`, brief power-up feedback and the actual server point delta; configured activation bonus is available in the status tooltip. |
| `Недоступен после cashout` | Desaturated, crossed-out token and `×3 упущен`. |
| Cashout success panel / repeated explanation | `Забрали ×…`, fixed payout and `Шар летит дальше…`; `Могли бы забрать больше` stays secondary. |
| `WIN · ВЫИГРЫШ` / `LOSS · ПРОИГРЫШ` | `Победа!` / `Шар лопнул`; a loss also states `Ставка сгорела`. |
| Permanent point-event text | A transient `+N`, derived only from consecutive server totals. |
| `Sky Fragments · 3 в запасе` and lengthy progression copy | Crystal, `Фрагменты 3 / 5`, meter and `Каждые 5 → 5,00 Б`; full rules remain in Rules. |
| English cashout wording in player instructions | Short Russian wording using `Забрать` / `выплата`. |
| Full round identifier in every history row | Collapsed `Номер полёта`. |
| Prominent evaluator navigation | `Ещё → Настройки игры`, marked `Для оценщика`. |

The first available cashout in a tab shows “Нажми «Забрать», пока шар не лопнул” with a small arrow for four seconds. It disappears on cashout/crash, is not shown again in that tab session, and is tested under React StrictMode. It has no effect on server permission or timing.

## Motion and mobile behaviour

CSS transforms animate the balloon between server-provided level snapshots. The multiplier remains the exact formatted server value; no browser time model predicts outcomes or money. Flight, booster activation, point feedback, crash particles and reward reveal have separate lightweight effects. The existing server-completion delay still controls the transition to results.

At mobile widths the navigation becomes a compact second HUD row, the path stays on the left, and cashout sits near the bottom. After cashout the flight area reserves additional room for the fixed payout. Desktop uses additional width for atmosphere rather than extra panels. Reduced-motion preferences disable movement and transitions; native dialogs preserve focus trapping, Escape dismissal and focus return.

## Verification and visual review

Final results and screenshot links are recorded below after the runtime review. Screenshots use real rounds on a disposable local PostgreSQL database and the existing deterministic demo configuration. Those demo settings are not shipped as production defaults.

Reviewed visually in real Chromium sessions: theme selection, all four bets, GREEN waiting booster, early cashout with missed booster, RED activated booster, continued flight after cashout, burst/crash, win, loss, ticket offer, history, rules, tournament and evaluator forms. Width checks covered 320, 375, 768, 1024, 1440 and 1920 px. Mobile screenshots use 320 × 740; desktop screenshots use 1440 × 900.

A second cleanup pass fixed overlap between mobile cashout feedback and the booster path, moved floating point feedback away from permanent counters, strengthened altitude-label contrast, restored compact fragment progress on mobile bets, and made dialog headers non-scrolling. A final desktop review also brought booster checkpoints in front of the balloon and reserved extra flight space above the cashout confirmation. The close control was checked after scrolling every long secondary dialog at 320 px.

Selected captured states:

- [Theme selection, desktop](screenshots/visual-redesign/theme-desktop.png) · [mobile](screenshots/visual-redesign/theme-mobile.png)
- [Bet selection, mobile](screenshots/visual-redesign/bets-mobile.png)
- [GREEN, waiting booster](screenshots/visual-redesign/green-waiting.png)
- [GREEN, early cashout and missed booster](screenshots/visual-redesign/green-cashout-mobile.png)
- [RED, activated booster: mobile](screenshots/visual-redesign/red-active-mobile.png) · [desktop](screenshots/visual-redesign/red-active.png) · [continued flight after cashout](screenshots/visual-redesign/red-cashout.png)
- [Crash, fixed win retained](screenshots/visual-redesign/green-crash.png)
- [Win reward](screenshots/visual-redesign/win-mobile.png) · [loss](screenshots/visual-redesign/loss-desktop.png)
- [Tournament](screenshots/visual-redesign/tournament-mobile.png) · [history](screenshots/visual-redesign/history-mobile.png) · [admin](screenshots/visual-redesign/admin-mobile.png)

| Check | Result |
| --- | --- |
| `npm run typecheck` | Passed |
| `npm test` | 57 passed; all existing gameplay assertions retained, one onboarding lifetime/StrictMode test added |
| `npm run build` | Passed; approximately 87 KB gzip JavaScript + 8.2 KB gzip CSS |
| Real-backend Playwright | 6 passed: two scenarios each in Chromium, Firefox and WebKit |
| Real late-booster visual walkthrough | Waiting → early cashout → missed → crash → fixed win; passed without fabricated API state |
| Six representative widths | No document horizontal overflow; mobile cashout and dialog close controls checked |
| Backend source/config/build-definition hash comparison | No changes |

The E2E suite retains its checks for real RED/GREEN rounds, server booster activation, cashout, continued flight, recovery, loss, history, inactivity return, ticket purchases, admin snapshot isolation, insufficient balance, repeat and commitment verification. Updates replace changed Russian labels and open the new overflow menu; a viewport assertion was added for dialog close controls. The loss assertion explicitly waits for the result heading rather than the preceding burst caption; all three browsers passed that final gameplay recheck.

## Remaining limits

The illustrated style is deliberately lightweight SVG, not a raster/3D game engine. Browser-engine checks do not replace a final test on physical iOS/Android devices; 60 FPS and low-end GPU performance have not been instrumented. The existing simple audio synthesis was retained. No API field for a live estimated payout was invented: exact payout is shown after the authoritative cashout response. Technical proof details and evaluator forms intentionally retain denser copy.
