# .codex Instructions

## Scope

This directory owns AI collaboration rules, local skills, review agents, hooks and generated reference material for RunningHub.

## Responsibilities

- `rules/` holds current project constraints and conflict-resolution policy.
- `skills/` holds repository-local workflow checklists; keep them executable as instructions, not historical notes.
- `agents/` holds specialized reviewer/automation roles used by AI tasks.
- `references/` holds module facts and dependency summaries derived from current source.
- `hooks/` holds local guard scripts; keep Windows PowerShell and POSIX shell variants behaviorally aligned.

## Rules

- Do not edit `.codex` templates or rules from memory; verify current source, Gradle modules and docs first.
- Do not leave scaffold placeholders such as double-brace variables in active rules, agents, references or hooks.
- If `_scan.json` is absent, use `settings.gradle.kts`, `dependencies.md` and module reference files as the source of truth; regenerate rather than invent metadata.
- Keep module reference facts aligned with `settings.gradle.kts`; there are currently 25 Gradle modules.
- Do not add product claims, L1 seal claims, CI pass claims or external evidence unless the matching evidence file exists for the current HEAD.
- Hook changes must be non-destructive and platform-aware; Windows behavior matters for this workspace.

## Verification

- For `.codex` rule/reference changes, run placeholder scanning and a module-reference count check.
- For hook changes, run syntax checks for both `.ps1` and `.sh` variants where possible.
- For architecture rule changes, also run `.\gradlew.bat --console=plain checkArchitectureBoundaries`.

