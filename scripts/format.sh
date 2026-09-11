#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./backend/gradlew -p backend spotlessApply
npm --prefix frontend run format
# Semantic ESLint/Stylelint fixes remain explicit reviewable developer commands.
