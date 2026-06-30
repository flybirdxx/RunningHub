# Documentation Instructions

## Scope

This directory contains current governance, ADRs, migration evidence, design drafts, dated scrapes and archives.

## Where To Look

- `README.md` is the documentation routing index.
- `adr/` contains accepted architecture decisions.
- `governance/` contains active long-term guardrails, baselines and task templates.
- `migration/` contains L1 migration state and evidence that may still be referenced by gates.
- `scrape/` contains dated external data snapshots; verify freshness before using them as product facts.
- `design/` contains design drafts and `.pen` files.
- `archive/` contains historical context only; it is not current source of truth.

## Rules

- Prefer `ARCHITECTURE.md`, `DEVELOPMENT.md` and this directory's `README.md` before reading historical migration material.
- Do not treat archived PRDs, plans or old scrape outputs as current product behavior without checking source code.
- Do not read or grep `.pen` files directly; use Pencil MCP tools for encrypted design files.
- Evidence files must not contain Token, Cookie, API Key, Authorization header, request body or private user data.
- Do not move migration evidence or acceptance sources unless the active gate no longer references them.
- New governance docs must name the verifier that enforces or checks the rule.

## Verification

- Documentation-only changes should run placeholder scanning and, when touching governance or migration gates, `.\gradlew.bat --console=plain checkLongTermGovernance`.
- If a doc claims CI, macOS, device or login-state verification, link it to an actual evidence file for the current commit.

