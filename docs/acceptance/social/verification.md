# Social acceptance — 2026-09-11

- Backend: **82/82** tests passed; Gradle build passed.
- Frontend: **90/90** tests passed; TypeScript and production build passed.
- Full Docker E2E: **15/15**, five each in Chromium, Firefox and WebKit.
- Final social-only rerun with the explicit social configuration mount: **3/3**, one in each browser.
- Normal source Docker builds succeeded in this iteration, including Gradle download. No local-JAR override was needed.
- Both disposable Docker runs started with a fresh volume. Startup assertions checked one demo account and zero achievement ownership, daily progress, presence and activity. Only test-run registrations then created Alice/Bob and other fixtures. Cleanup removed only their isolated project volumes.
- The real Alice/Bob browser scenario verified recent-account presence, cashout/booster events visible to Bob, completed daily challenge, exactly one additional fragment, achievement reveal and persistence through reload. Old gameplay, wallet isolation, ranking, admin/config, upsell, collection and recovery checks remain in the full suite.

## Main application

The stopped main PostgreSQL volume was preserved and backed up privately before update. V5 → V6 applied successfully. The same four accounts remain; checksums of users, wallets, game_rounds, reward_progress, wallet_entries and ticket_offers were identical before and after migration. Before the first login, all four new social ownership/presence/event stores were empty.

The normal Compose stack now runs at http://localhost:5173. PostgreSQL, backend and nginx are healthy. Demo login and profile worked through nginx; health returned UP; browser reported no page errors. That actual login produced online=1, events=0, daily progress=0. No gameplay or extra accounts were created in the main database for verification. OpenAPI lists all four new endpoints. Backend startup logs contained no errors.

## Visual review

Reviewed the daily completion card, earned/locked achievement list and real feed screenshots at mobile and desktop sizes. Automated checks cover 320, 375, 768, 1024, 1440 and 1920 px. Achievements are collapsed initially; an expanded list scrolls inside the existing profile dialog. The feed remains secondary to the game. Screenshots in this directory contain disposable registered test accounts, apart from the explicitly named main-profile demo screenshot.

## Deliberately small

Achievements give status only. One shared challenge per UTC day grants exactly one fragment, automatically. The extra fragment joins collection immediately and the existing bonus exchange on the next completed round. Configurable thresholds live in validated social-config.yml; restart applies them, while an already selected day retains its stored target. No extra admin editor, cron, sockets, bots, fake events or new service infrastructure.

Presence is recent authenticated account activity, not an exact connection detector. Historical event rows are retained but only the latest five within an hour are exposed. Achievement evaluation reads a compact lifetime history projection; a large production history could later use aggregates. Pre-existing results are recognised on the next completed round; migration does not fabricate unlock activity.
