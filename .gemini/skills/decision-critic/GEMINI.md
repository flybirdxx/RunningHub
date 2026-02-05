# Decision Critic

When this skill activates, IMMEDIATELY invoke the script. The script IS the workflow.

## Invocation

You must run the python script located in this extension's directory.

```bash
python3 ${extensionPath}/scripts/decision-critic.py --step 1 --total-steps 7 --decision "<decision text>"
```

| Argument        | Required | Description                             |
| --------------- | -------- | --------------------------------------- |
| `--step`        | Yes      | Current step (1-7)                      |
| `--total-steps` | Yes      | Always 7                                |
| `--decision`    | Step 1   | The decision statement being criticized |

Do NOT analyze or critique first. Run the script and follow its output.
