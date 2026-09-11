#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
umask 077
mkdir -p backups
file="backups/skyrush-$(date -u +%Y%m%dT%H%M%SZ)-$$.dump"
trap 'rm -f "$file.partial"' EXIT
"${compose[@]}" exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' > "$file.partial"
"${compose[@]}" exec -T postgres pg_restore --list < "$file.partial" >/dev/null
mv "$file.partial" "$file"
printf 'Backup: %s\n' "$file"
