#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/bin:$PATH"
for tool in docker curl ssh; do
  command -v "$tool" >/dev/null || { echo "Missing $tool. Install Docker, curl and OpenSSH" >&2; exit 1; }
done
docker info >/dev/null 2>&1 || { echo 'Start Docker first.' >&2; exit 1; }
[[ -f .env ]] || cp .env.example .env
export LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)"
compose=(docker compose -f docker-compose.yml -f docker-compose.demo.yml)
"${compose[@]}" config --quiet || { echo 'Docker Compose >= 2.24.4 is required.' >&2; exit 1; }
"${compose[@]}" up --build -d --wait --wait-timeout 120
# Read the actual port from Compose; do not source untrusted .env as shell code.
binding=$("${compose[@]}" port frontend 5173)
origin="http://localhost:${binding##*:}"
curl --connect-timeout 5 --max-time 15 --fail --silent --show-error "$origin/" >/dev/null
curl --connect-timeout 5 --max-time 15 --fail --silent --show-error "$origin/actuator/health" | grep -q '"status":"UP"'
status=$(curl --connect-timeout 5 --max-time 15 --silent --show-error --output /dev/null --write-out '%{http_code}' "$origin/api/auth/me")
[[ "$status" = 401 ]] || { echo "Expected unauthenticated API status 401, received $status" >&2; exit 1; }
echo "Unified local origin: $origin"
echo 'Temporary PUBLIC demo. demo / demo12345 grants evaluator access.'
echo 'Use the LATEST HTTPS URL: free domains may rotate while connected; log in again after rotation.'
echo 'Share only while needed; Ctrl+C stops the tunnel. Docker data remains.'
# Do not use personal SSH keys, agent forwarding or user-specific SSH config.
# accept-new remembers first-use host keys and rejects changed keys.
exec ssh -F /dev/null -T \
  -o StrictHostKeyChecking=accept-new \
  -o IdentityAgent=none -o IdentitiesOnly=yes -o IdentityFile=none \
  -o ForwardAgent=no -o BatchMode=yes -o ExitOnForwardFailure=yes \
  -o ConnectTimeout=20 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 \
  -R "80:127.0.0.1:${binding##*:}" nokey@localhost.run
