#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/bin:$PATH"
for tool in docker npm; do command -v "$tool" >/dev/null || { echo "Missing $tool" >&2; exit 1; }; done
docker info >/dev/null 2>&1 || { echo 'Start Docker first.' >&2; exit 1; }
export E2E_CONFIG_DIR
E2E_CONFIG_DIR=$(mktemp -d "${TMPDIR:-/tmp}/skyrush-e2e.XXXXXX")
project="skyrush-e2e-$(date +%s)-$$"
export LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)"
# Ephemeral test database credential. Do not read or modify the real .env.
export POSTGRES_PASSWORD="e2e-$project" POSTGRES_DB=skyrush POSTGRES_USER=skyrush
compose=(docker compose -p "$project" -f "$PWD/docker-compose.yml" -f "$PWD/docker-compose.e2e.yml")
cleanup() {
  local result=$?
  trap - EXIT
  if [ "$result" -ne 0 ]; then
    "${compose[@]}" logs --no-color --tail 80 backend frontend >&2 || true
  fi
  "${compose[@]}" down -v --remove-orphans || result=1
  rm -rf "$E2E_CONFIG_DIR"
  exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
cp config/game-demo.yml "$E2E_CONFIG_DIR/game-config.yml"
cp config/prototype-config.yml "$E2E_CONFIG_DIR/prototype-config.yml"
cp config/social-config.yml "$E2E_CONFIG_DIR/social-config.yml"
"${compose[@]}" up --build -d --wait --wait-timeout 120
uid=$("${compose[@]}" exec -T frontend id -u)
[[ "$uid" != 0 ]] || { echo 'Frontend must run without root.' >&2; exit 1; }
binding=$("${compose[@]}" port frontend 5173)
export E2E_BASE_URL="http://127.0.0.1:${binding##*:}" E2E_ISOLATED_RUN=1
# Verify startup does not seed competitor accounts before any browser registers users.
seed=$("${compose[@]}" exec -T postgres psql -U skyrush -d skyrush -Atc "SELECT count(*)=1 AND bool_and(username='demo') FROM skyrush.users")
[[ "$seed" = t ]] || { echo 'Clean test DB must contain only the demo account.' >&2; exit 1; }
empty=$("${compose[@]}" exec -T postgres psql -U skyrush -d skyrush -Atc "SELECT (SELECT count(*) FROM skyrush.user_achievements)+(SELECT count(*) FROM skyrush.daily_challenge_progress)+(SELECT count(*) FROM skyrush.user_presence)+(SELECT count(*) FROM skyrush.player_activity_events)")
[[ "$empty" = 0 ]] || { echo 'Clean test DB must have no social player fixtures.' >&2; exit 1; }
echo "Isolated browser tests: $E2E_BASE_URL ($project)"
cd frontend
./node_modules/.bin/playwright test "$@"
