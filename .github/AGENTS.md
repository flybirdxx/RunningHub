# GitHub Automation Instructions

## Scope

This directory owns GitHub Actions, Dependabot configuration and the pull request template.

## Rules

- Android CI must keep running `./gradlew verifyL1Android` on pull requests and pushes to active protected branches.
- iOS CI is manual `workflow_dispatch`; do not claim iOS CI or Simulator smoke passed without a real macOS run.
- Keep workflow JDK at 17 unless Gradle and Android tooling are upgraded together.
- Keep report and test-result artifact uploads for failure forensics.
- Do not weaken `concurrency`, scheduled/manual evidence collection or PR template risk fields without updating governance docs.
- Dependency submission must keep the minimum permission needed for GitHub Dependency Graph.
- Do not add secrets, tokens or production signing material to workflows.

## Verification

- For workflow edits, run `.\gradlew.bat --console=plain checkLongTermGovernance` when available.
- Validate YAML syntax through GitHub or a local parser before claiming workflow correctness.
- For L1 evidence workflow changes, also inspect `checkL1CiWorkflows` / related root Gradle tasks.

