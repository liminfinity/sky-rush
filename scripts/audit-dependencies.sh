#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./backend/gradlew -p backend runtimeDependencyManifest
# Always run both ecosystem scans. Exit 2 means the online Maven scan was incomplete.
status=0
python3 scripts/audit-maven.py || status=$?
if ! npm --prefix frontend audit --omit=dev; then
  if [ "$status" -eq 0 ]; then status=1; fi
fi
if ! npm --prefix frontend audit; then
  if [ "$status" -eq 0 ]; then status=1; fi
fi
exit "$status"
