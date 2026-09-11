#!/usr/bin/env bash
# Resolve build plugins before CI checks. Never retry or skip a failing test/analysis task.
set -euo pipefail
cd "$(dirname "$0")/.."
log=$(mktemp)
trap 'rm -f "$log"' EXIT
for attempt in 1 2 3; do
  args=(--no-daemon --stacktrace)
  # Retry remote resolution rather than reusing a cached negative result.
  if (( attempt > 1 )); then args+=(--refresh-dependencies); fi
  printf 'Gradle plugin resolution: attempt %s/3\n' "$attempt"
  if ./backend/gradlew -p backend help "${args[@]}" >"$log" 2>&1; then
    cat "$log"
    exit 0
  fi
  cat "$log" >&2
  if ! grep -Eq 'Plugin \[.*\] was not found|Could not (resolve|GET|HEAD|get resource)|Read timed out|Connection reset|UnknownHostException|SocketTimeoutException|Temporary failure in name resolution|Remote host terminated' "$log"; then
    exit 1
  fi
  if (( attempt == 3 )); then
    echo 'Plugin resolution failed after 3 attempts. Check runner access to plugins.gradle.org and its artifact/CDN redirects; see the stacktrace above.' >&2
    exit 1
  fi
  sleep "$((attempt * 10))"
done
