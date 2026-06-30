# API Reference Instructions

## Scope

This directory contains retained RunningHub API and product reference documents.

## Rules

- Use these files for endpoint names, request fields, response examples and product terminology only after checking `doc/README.md`.
- Do not use this directory for new phase plans, progress reports, design drafts or handoff notes; put current governance under `docs/`.
- Treat interface examples as evidence snapshots, not guaranteed live API behavior.
- Preserve exact field names, casing and example values when a task asks for API compatibility.
- Do not paste real credentials, tokens, cookies or full private request bodies into examples.
- Keep `RunningHub-KMP-架构迁移验收标准.md` in place while migration gates still reference it.

## Verification

- API contract changes should be backed by Data-layer tests or documented as unverified external API assumptions.
- If external API docs are refreshed, record the source date and do not overwrite older evidence without a reason.

