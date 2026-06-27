#!/usr/bin/env bash
set -euo pipefail

stdin="$(cat)"
session_id="$(printf '%s' "$stdin" | sed -n 's/.*"session_id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"

if [[ -z "${session_id}" ]]; then
    exit 0
fi

safe_session_id="$(printf '%s' "$session_id" | tr -c 'A-Za-z0-9_.-' '_')"
track_file="/tmp/runninghub_codex_edits_${safe_session_id}.txt"
review_file_threshold=2

if [[ -f "$track_file" ]]; then
    file_count="$(sort -u "$track_file" | wc -l | tr -d ' ')"
    rm -f "$track_file"
    if [[ "$file_count" =~ ^[0-9]+$ ]] && [[ "$file_count" -ge "$review_file_threshold" ]]; then
        echo "[强制规则] 检测到 ${file_count} 个项目文件被修改，请确认是否已按 .codex/skills/code_review/SKILL.md 完成审查。" >&2
        exit 2
    fi
fi

exit 0
