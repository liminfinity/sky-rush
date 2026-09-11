#!/usr/bin/env bash
# Shared setup; never source .env.prod as executable shell code.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
for tool in docker python3 curl; do
  command -v "$tool" >/dev/null || { echo "Missing $tool" >&2; exit 1; }
done
env_file=${VPS_ENV_FILE:-.env.prod}
[[ -f "$env_file" ]] || { echo 'Copy .env.prod.example to .env.prod and fill domain/password.' >&2; exit 1; }
compose=(docker compose --project-name "${VPS_PROJECT_NAME:-sky-rush-prod}" --env-file "$env_file" -f docker-compose.yml -f docker-compose.prod.yml)
"${compose[@]}" config --quiet
# Print only the non-secret hostname; configuration containing credentials stays in the pipe.
domain=$("${compose[@]}" config --format json | python3 -c '
import json,sys,re
c=json.load(sys.stdin)
d=c["services"]["caddy"]["environment"]["SKYRUSH_DOMAIN"]
p=c["services"]["postgres"]["environment"]["POSTGRES_PASSWORD"]
if not re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?",d) or d.endswith("example.com"):
 sys.exit("Set a real hostname without scheme, path or port")
if len(p)<32 or p=="REPLACE_WITH_RANDOM_HEX":
 sys.exit("Set POSTGRES_PASSWORD to a strong random value (at least 32 characters)")
print(d)
')
