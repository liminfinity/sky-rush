#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
[[ -z "$(git status --porcelain)" ]] || { echo 'Repository is dirty. Commit or preserve changes before deploying.' >&2; exit 1; }
git pull --ff-only
# Re-read deployment configuration after pulling an update.
source scripts/vps/common.sh
umask 077
mkdir -p runtime/config
for file in game-config.yml prototype-config.yml social-config.yml; do
  [[ -f runtime/config/$file ]] || cp "config/$file" "runtime/config/$file"
done
expected=$("${compose[@]}" config --format json | python3 -c 'import json,sys; print(json.load(sys.stdin)["services"]["backend"]["user"])')
[[ "$expected" = "$(id -u):$(id -g)" && $(id -u) != 0 ]] || {
  echo 'Run as a non-root deployment user; set LOCAL_UID/LOCAL_GID in .env.prod to its id -u / id -g.' >&2; exit 1;
}
"${compose[@]}" build
"${compose[@]}" run --rm --no-deps caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
if ! "${compose[@]}" up -d --wait --wait-timeout 180; then
  "${compose[@]}" logs --tail 60 backend frontend caddy
  # Avoid an unattended restart loop after invalid application configuration.
  "${compose[@]}" stop backend frontend caddy
  echo 'Startup failed. Database retained; fix the reported configuration and deploy again.' >&2
  exit 1
fi
"${compose[@]}" ps
exec scripts/vps/verify-deployment.sh
