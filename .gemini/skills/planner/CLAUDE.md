# skills/planner/

## Overview

Planning skill with resources that must stay synced with agent prompts.

## Index

| File/Directory                        | Contents                                       | Read When                                    |
| ------------------------------------- | ---------------------------------------------- | -------------------------------------------- |
| `SKILL.md`                            | Planning workflow, phases                      | Using the planner skill                      |
| `scripts/planner.py`                  | Step-by-step planning orchestration            | Debugging planner behavior                   |
| `scripts/executor.py`                 | Plan execution orchestration                   | Debugging executor behavior                  |
| `scripts/execute-milestones.py`       | Wave execution with batch QR gates             | Debugging wave execution                     |
| `scripts/utils.py`                    | Shared utilities (Three Pillars Pattern)       | Modifying QR verification loop behavior      |
| `resources/plan-format.md`            | Plan template (injected by script)             | Editing plan structure                       |
| `resources/temporal-contamination.md` | Detection heuristic for contaminated comments  | Updating TW/QR temporal contamination logic  |
| `resources/diff-format.md`            | Unified diff spec for code changes             | Updating Developer diff consumption logic    |
| `resources/default-conventions.md`    | Default structural conventions (4-tier system) | Updating QR RULE 2 or planner decision audit |
| `README.md`                           | Resource sync requirements, Three Pillars      | Understanding architecture decisions         |
