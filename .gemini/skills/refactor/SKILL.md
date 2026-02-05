---
name: refactor
description: Invoke when user requests refactoring analysis, technical debt review, or code quality improvement. Explores codebase for code smells and generates actionable work items.
---

# Refactor

When this skill activates, IMMEDIATELY invoke the script. The script IS the workflow.

## Invocation

```bash
python3 scripts/refactor.py --step 1 --total-steps 5 --n 10
```

| Argument        | Required | Description                                   |
| --------------- | -------- | --------------------------------------------- |
| `--step`        | Yes      | Current step (starts at 1)                    |
| `--total-steps` | Yes      | Total steps (5 for full workflow)             |
| `--n`           | No       | Number of categories to explore (default: 10) |

Do NOT explore or analyze first. Run the script and follow its output.

## Workflow Phases

1. **Dispatch** - Explore refactoring categories sequentially
2. **Triage** - Structure smell findings with IDs
3. **Cluster** - Group smells by shared root cause
4. **Contextualize** - Extract user intent, prioritize issues
5. **Synthesize** - Generate actionable work items

## Determining N (category count)

Default: N = 10

Adjust based on user request scope:

- SMALL (single file, specific concern, "quick look"): N = 5
- MEDIUM (directory, module, standard analysis): N = 10
- LARGE (entire codebase, "thorough", "comprehensive"): N = 25

The script randomly selects N categories from the available code quality categories.
