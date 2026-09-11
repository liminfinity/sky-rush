# Profile / collection acceptance — 2026-09-11

## Implemented

- Session-owned profile, 13 lifetime record fields, current daily rank/score and wallet balance.
- Lifetime earned fragments unlock two balloon patterns and two avatar frames; default balloon/frame remain available.
- Existing fragment-to-bonus conversion unchanged. Additive V5 retroactively recognises completed-round rewards.
- Persisted equip, locked-item rejection, CSRF, duplicate-unlock protection and account isolation.
- Pattern on the actual flight balloon; own frame in header, live ranking, tournament and profile.
- Inline unlock reveal and fixed-cashout/final-crash comparison using server values only.

## Automated checks

- Backend: **73 tests passed**, Gradle build passed. Includes an explicit V4 → V5 migration test comparing existing player tables before/after.
- Frontend: **80 tests passed**, standalone TypeScript check and production build passed.
- Full real-backend suite: **12/12 passed**, four each in Chromium, Firefox and WebKit. Includes existing gameplay, two-user ranking, admin, tickets, recovery and the new five-win collection journey.
- Final profile-only rerun after the copy adjustment: **3/3 passed** (Chromium, Firefox, WebKit), with reward screenshots captured after animation completion.
- All E2E accounts lived in unique disposable Docker projects; their volumes were removed on exit.

## Runtime and visual review

The real profile was opened in Chromium at 320, 375, 768, 1024, 1440 and 1920 px; screenshots are in this directory. The modal scrolls vertically, with a two-column collection on narrow screens and three columns on wider screens. Browser tests additionally checked dialog/document overflow and equip-button reachability at every width.

Reviewed: initial locked collection, unlocked/equipped items, expanded records, mobile flight with constellation pattern and bronze frame, current-player ranking frame, and the win/unlock result. Reduced-motion mode disables the new reveal animation; cosmetic patterns themselves are static SVG.

Main application updated through nginx at http://localhost:5173. All three containers healthy; health UP; demo login/profile and generated profile OpenAPI routes verified. V5 applied successfully. Four original accounts remain; checksums of users, wallets, rounds, reward_progress, wallet_entries and ticket_offers match the pre-update values. A private database backup was taken before update. No test users were added to the main database.

## Environment limitation

The normal backend Docker build was attempted twice and failed downloading Gradle 8.14.3 (network read timeout). Tests and the running application therefore used the locally built/tested JAR inside the same Java 21 runtime image through a temporary Compose override in `/tmp`. Repository Dockerfiles and Compose architecture were not changed. This verifies container runtime, fresh Flyway startup and browser integration, but does **not** claim a successful normal source Docker build on this network. Retry the normal build when the Gradle distribution endpoint is reachable.

## Kept small

No trails, marketplace, paid cosmetics, random cosmetic rewards or competitive modifiers. Records are calculated from completed history when the profile is refreshed; very large lifetime histories may eventually need an aggregate table. Cross-device equip is loaded on the next profile refresh, not pushed live. Recovered result screens can show their original unlock reveal again without granting another item.
