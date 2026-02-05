# Prompt Engineer

When this skill activates, IMMEDIATELY invoke the script. The script IS the workflow.

## Invocation

You must run the python script located in this extension's directory.

```bash
python3 ${extensionPath}/scripts/optimize.py --step 1 --total-steps 5
```

| Argument        | Required | Description                 |
| --------------- | -------- | --------------------------- |
| `--step`        | Yes      | Current step (starts at 1)  |
| `--total-steps` | Yes      | Minimum 5; adjust if needed |

Do NOT analyze or explore first. Run the script and follow its output.
