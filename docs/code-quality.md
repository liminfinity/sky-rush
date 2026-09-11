# Code quality

Run `./scripts/check.sh` after `npm --prefix frontend ci`. Java 21, Node 22.12+ and the Docker Compose CLI are required. Docker need not be running: only Compose syntax is validated. Backend tests start their own temporary embedded PostgreSQL, never the application database.

`./scripts/format.sh` applies Spotless and Prettier only. ESLint fixes are a separate explicit `npm --prefix frontend run lint:fix` command. `check` never rewrites source files or starts the application.

## Tools

| Area              | Pinned tooling                                             |
| ----------------- | ---------------------------------------------------------- |
| TypeScript/React  | ESLint 9.39.5, @eslint/js 9.39.5, typescript-eslint 8.70.0 |
| React correctness | eslint-plugin-react 7.37.5, react-hooks 7.1.1              |
| Accessibility     | eslint-plugin-jsx-a11y 6.10.2                              |
| Formatting        | Prettier 3.9.6, eslint-config-prettier 10.1.8              |
| CSS               | Stylelint 17.15.0, stylelint-config-standard 40.0.0        |
| Java formatting   | Spotless 8.10.2, google-java-format 1.28.0                 |
| Gradle formatting | Groovy Eclipse formatter 4.35 through Spotless             |
| Java analysis     | SpotBugs plugin 6.5.11, analyzer 4.10.4                    |
| Java hygiene      | Checkstyle 10.26.1                                         |

ESLint uses flat configuration and TypeScript project service for application code, unit tests and E2E. The React/accessibility plugins currently declare support through ESLint 9; ESLint 10 is not forced past their peer constraints. npm marks ESLint 9 deprecated, so revisit this pin when both plugins support 10. No `--legacy-peer-deps` workaround is used.

Formatting covers frontend source/configuration, the new quality workflow and this document. SQL migrations, public assets, reference files and saved acceptance reports are excluded from mass formatting. Existing README prose and game YAML are not rewritten by format commands.

## Intentional exceptions

- Stylelint checks valid declarations/selectors and duplicates. Class/keyframe naming, descending specificity, equivalent color/media spellings and blank-line conventions are not enforced: the existing game cascade and Prettier own those choices.
- Native dialog backdrop clicks have an exact JSX accessibility exception: Escape and the visible close button already provide keyboard alternatives. The secondary native `details` menu delegates native button clicks, including keyboard activation. Only those nodes have exceptions, not whole components or directories.
- SpotBugs analyzes all production bytecode at maximum effort, including low-confidence findings. JUnit bytecode is not analyzed; test sources still compile, run and pass Checkstyle/Spotless.
- `backend/config/spotbugs/exclude.xml` lists individual constructor/field pairs for shared Spring beans. These dependencies are deliberately retained, not copied. A separate constructor-only finalizer-attack warning is excluded for the Spring-owned transactional demo seed service. DTO collections have defensive copies instead of suppressions.
- Asymmetric Vitest matchers are typed as `unknown` when nested in expectation objects. HTTP JSON is explicitly typed at test API boundaries using the existing DTOs, not `any`.

No Git hooks are installed: this distribution has no `.git` directory. The same checks run in GitHub Actions on push/PR and can be run manually in any checkout. Browser E2E remains an explicit `npm --prefix frontend run test:e2e` command, using disposable Docker data.

## Findings and dependency audit

The initial run reported 208 ESLint findings, 572 Stylelint findings (mostly whitespace rules), 71 Checkstyle findings and 56 SpotBugs findings. These are diagnostics, not counts of distinct defects. Compiler findings were missing `serialVersionUID` and calls to overridable methods from two configuration-service constructors.

Fixes include explicit button types, typed auth form values and HTTP test responses, explicit async form ownership, duplicate CSS consolidation, modern screen-reader clipping, immutable DTO collections, required SQL-result checks, precise configuration exceptions and removal of a dead fragment accumulator. Game formulas, payout logic, API field names and migration SQL are unchanged.

`./scripts/audit-dependencies.sh` scans Gradle's resolved production Maven coordinates using the [OSV API](https://google.github.io/osv.dev/post-v1-querybatch/) and runs npm audit for production and all frontend dependencies. It needs internet access and Python 3, but no API key. Reports are written under `backend/build/reports/dependencies`. Advisory findings exit 1 for review; an incomplete OSV scan exits 2. Advisories are not proof of exploitability.

The initial runtime scan reported nine advisories across Jackson Databind 2.21.4, Commons Lang 3.17.0, Log4j API 2.24.3, Tomcat 10.1.55 and PostgreSQL JDBC 42.7.11. Applied compatible fixes: Jackson BOM 2.21.5, Commons Lang 3.20.0, Log4j 2.25.5, Tomcat 10.1.59 and PostgreSQL JDBC 42.7.12. Spring Boot stays on 3.5.16. These versions also keep SpotBugs' Commons Lang dependency compatible with its analyzer; the application BOM had downgraded that tool dependency.

Docker keeps all existing routes, health checks and loopback exposure. nginx now runs as its existing unprivileged `nginx` account on port 5173 with a writable cache and `/tmp` PID file. Browser traces/reports are excluded from Docker build context. A BuildKit cache retains the Gradle distribution and dependencies across source rebuilds. Hadolint is not installed in the verification environment; no hadolint pass is claimed.

## Verification

- `backend/gradlew -p backend clean check` and `spotlessCheck`: pass, 82 backend tests. No application compiler warnings or unsuppressed SpotBugs findings remain.
- `npm --prefix frontend run check` and `build`: pass, 92 frontend tests. A regression test also covers a rejected audio-device close during unmount; the optional resource cleanup no longer leaks an unhandled promise.
- `./scripts/check.sh`: pass, including Compose validation without reading `.env`.
- OSV: 72 resolved runtime packages, no remaining advisories at verification time. npm audit: zero findings for both production-only and all dependencies. This is a point-in-time result, not a security guarantee.
- Computed styles for the consolidated selectors match the previous Docker image at 320, 375, 768, 1024, 1440 and 1920 px. An initially shifted mobile record gap was restored before the final check.

The development toolchain still uses ESLint 9 because the current React/a11y plugins do not declare ESLint 10 support. This maintenance limitation is explicit rather than bypassing npm compatibility checks.

Docker regression: all 15 existing real-backend E2E scenarios pass across Chromium, Firefox and WebKit. The isolated stack verified frontend UID 101, health `UP`, OpenAPI 3.1.0 and clean database seeding. A first run exposed the base image's `/run/nginx.pid` permission issue; it was fixed and re-tested. A subsequent Plugin Portal download failure cleared on retry. Test containers and their database volume were removed automatically; application data was not reset.

All six migration SQL files were compared byte-for-byte with the previous working backend image and are unchanged. GitHub Actions configuration is provided, but no remote CI run is claimed. Hadolint was unavailable.
