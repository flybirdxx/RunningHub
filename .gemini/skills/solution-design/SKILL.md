---
name: solution-design
description: Generate diverse solution options from multiple reasoning perspectives when user has a defined problem or root cause and needs solution options.
---

# Solution Design

When this skill activates, IMMEDIATELY invoke the script. The script IS the
workflow.

This skill generates solutions for a defined problem or root cause. It does NOT
identify problems or perform root cause analysis--use problem-analysis for that.

## Invocation

```bash
python3 scripts/design.py --step 1 --total-steps 9
```

Do NOT explore or analyze first. Run the script and follow its output.
