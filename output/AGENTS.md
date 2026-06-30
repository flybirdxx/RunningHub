# Output Directory Instructions

## Scope

This directory contains raw generated output such as screenshots, UI XML dumps, HTML captures and temporary verification files.

## Rules

- Do not treat files here as source of truth for product behavior without checking current code or live runtime.
- Do not delete, rename or mass-regenerate existing files unless the task explicitly owns the output set.
- Prefer `artifacts/` for curated reports; keep raw captures here.
- Do not store credentials, request bodies, cookies, tokens or private user data in captures.
- When adding new runtime captures, use names that include the flow or screen being verified.
- If a capture is used as evidence, record the producing command or scenario in the final response or companion report.

## Verification

- For visual QA tasks, inspect the generated image/XML before citing it as evidence.
- For stale output cleanup, show the candidate list first and get explicit approval before deleting files.

