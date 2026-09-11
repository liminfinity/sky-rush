#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# GitVerse's container runner is root; embedded PostgreSQL requires a real non-root user.
# GitHub's hosted runner already satisfies this requirement.
if [[ $(id -u) = 0 ]]; then
  [[ ${CI:-} = true ]] || { echo 'Run local checks as a non-root user.' >&2; exit 1; }
  command -v runuser >/dev/null || { echo 'CI runner needs runuser (util-linux).' >&2; exit 1; }
  ci_home=$(mktemp -d /tmp/skyrush-ci.XXXXXX)
  ci_uid=$(id -u nobody)
  ci_gid=$(id -g nobody)
  [[ "$ci_uid" != 0 ]] || exit 1
  chown -R "$ci_uid:$ci_gid" "$PWD" "$ci_home"
  # Keep toolchain PATH/JAVA_HOME and use a writable home, without root-owned caches.
  export GRADLE_USER_HOME=${GRADLE_USER_HOME:-$PWD/.gradle/ci-home}
  exec runuser -u nobody -- env HOME="$ci_home" PATH="$PATH" \
    GRADLE_USER_HOME="$GRADLE_USER_HOME" bash scripts/ci-check.sh
fi
./scripts/resolve-gradle-plugins.sh
exec ./scripts/check.sh
