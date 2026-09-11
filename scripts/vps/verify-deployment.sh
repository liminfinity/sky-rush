#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
"${compose[@]}" ps --format json | python3 -c '
import json,sys
raw=sys.stdin.read().strip()
rows=json.loads(raw) if raw.startswith("[") else [json.loads(s) for s in raw.splitlines() if s]
by={r["Service"]:r for r in rows}
for name in ("postgres","backend","frontend","caddy"):
 r=by.get(name,{})
 if r.get("State")!="running" or r.get("Health")!="healthy": sys.exit(name+" is not healthy")
print("Containers: healthy")
'
origin="https://$domain"
args=(--fail --silent --show-error --connect-timeout 5 --max-time 20 --retry 6 --retry-delay 5 --retry-all-errors)
curl "${args[@]}" "$origin/" | python3 -c 'import sys; assert "<div id=\"root\">" in sys.stdin.read(), "Frontend HTML missing"'
curl "${args[@]}" "$origin/actuator/health" | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"]=="UP"'
curl "${args[@]}" "$origin/api/auth/csrf" | python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("token") and d.get("headerName")'
status=$(curl --silent --show-error --connect-timeout 5 --max-time 20 -o /dev/null -w '%{http_code}' "$origin/api/auth/me")
[[ "$status" = 401 ]] || { echo "Expected unauthenticated /me = 401, got $status" >&2; exit 1; }
printf 'HTTPS, frontend, health and CSRF: OK\nOpen %s\n' "$origin"
