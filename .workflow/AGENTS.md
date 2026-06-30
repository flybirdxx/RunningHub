# Workflow Artifact Instructions

## Scope

This directory holds ad hoc orchestration plans, worker packets and final reports from prior multi-agent workflows.

## Rules

- Treat workflow files as historical execution artifacts unless the current task explicitly resumes that workflow.
- Do not assume commands inside old workflow plans are still valid; verify against current `settings.gradle.kts`, `AGENTS.md` and Gradle tasks.
- Preserve packet ownership boundaries when resuming multi-agent work.
- Do not overwrite worker reports or final reports without keeping the decision trail clear.
- If a workflow references retired modules such as `shared`, update the active plan before executing commands.
- Keep production credentials and external API secrets out of workflow notes.

## Verification

- Before resuming a workflow, run `git status --short --branch` and check whether referenced files still exist.
- After completing resumed work, write the actual verifier commands and any skipped checks into the final workflow report.

