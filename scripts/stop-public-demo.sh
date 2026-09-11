#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
echo 'Stop the tunnel with Ctrl+C in its terminal. Stopping Docker now; database is preserved.'
docker compose -f docker-compose.yml -f docker-compose.demo.yml down
