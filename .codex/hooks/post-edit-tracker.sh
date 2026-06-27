#!/usr/bin/env bash
set -euo pipefail

stdin="$(cat)"
file_path="$(printf '%s' "$stdin" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
session_id="$(printf '%s' "$stdin" | sed -n 's/.*"session_id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"

if [[ -z "${session_id}" || -z "${file_path}" ]]; then
    exit 0
fi

safe_session_id="$(printf '%s' "$session_id" | tr -c 'A-Za-z0-9_.-' '_')"
track_file="/tmp/runninghub_codex_edits_${safe_session_id}.txt"

if [[ "$file_path" =~ \.(kt|kts|java|xml|gradle|swift|md|json|ya?ml|properties)$ ]]; then
    printf '%s\n' "$file_path" >> "$track_file"
fi

exit 0
