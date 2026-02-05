# Solution Design

When this skill activates, IMMEDIATELY invoke the script. The script IS the
workflow.

This skill generates solutions for a defined problem or root cause. It does NOT
identify problems or perform root cause analysis--use problem-analysis for that.

## Invocation

You must run the python script located in this extension's directory.

```bash
python3 ${extensionPath}/scripts/design.py --step 1 --total-steps 9
```

Do NOT explore or analyze first. Run the script and follow its output.
