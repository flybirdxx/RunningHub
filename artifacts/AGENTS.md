# Artifacts Instructions

## Scope

This directory holds curated reports, audits, screenshots and other task deliverables.

## Rules

- Treat files here as evidence or deliverables, not application source.
- Do not edit or delete existing artifacts unless the task explicitly owns that artifact.
- New artifact names should include the subject and date.
- Screenshots and reports must not expose credentials, request bodies, private user data or authorization headers.
- If an artifact supports a completion claim, include the command, device, environment or source data needed to interpret it.
- Keep rough runtime dumps in `output/`; move only curated deliverables here.

## Verification

- For report-only artifacts, run a lightweight scan for placeholders and sensitive terms.
- For screenshot artifacts, record whether they came from emulator, device, browser, design tool or static file inspection.

