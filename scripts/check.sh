#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
printf 'Backend checks\n'
./backend/gradlew -p backend check
printf 'Frontend checks\n'
npm --prefix frontend run check
npm --prefix frontend run build
printf 'Docker configuration\n'
# A placeholder validates interpolation without reading or printing local secrets.
POSTGRES_PASSWORD=quality-check-only docker compose --env-file /dev/null config --quiet
POSTGRES_PASSWORD=quality-check-only docker compose --env-file /dev/null -f docker-compose.yml -f docker-compose.demo.yml config --quiet
SKYRUSH_DOMAIN=play.example.com POSTGRES_PASSWORD=quality-check-only docker compose --env-file /dev/null -f docker-compose.yml -f docker-compose.prod.yml config --quiet
for script in scripts/vps/*.sh; do bash -n "$script"; done
printf 'All checks passed\n'
