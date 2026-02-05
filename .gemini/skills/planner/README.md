# Planner

LLM-generated plans have gaps. I have seen missing error handling, vague
acceptance criteria, specs that nobody can implement. I built this skill with
two workflows -- planning and execution -- connected by quality gates that catch
these problems early.

## Planning Workflow

```
  Planning ----+
      |        |
      v        |
     QR -------+  [fail: restart planning]
      |
      v
     TW -------+
      |        |
      v        |
   QR-Docs ----+  [fail: restart TW]
      |
      v
   APPROVED
```

| Step                    | Actions                                                                    |
| ----------------------- | -------------------------------------------------------------------------- |
| Context & Scope         | Confirm path, define scope, identify approaches, list constraints          |
| Decision & Architecture | Evaluate approaches, select with reasoning, diagram, break into milestones |
| Refinement              | Document risks, add uncertainty flags, specify paths and criteria          |
| Final Verification      | Verify completeness, check specs, write to file                            |
| QR-Completeness         | Verify Decision Log complete, policy defaults confirmed, plan structure    |
| QR-Code                 | Read codebase, verify diff context, apply RULE 0/1/2 to proposed code      |
| Technical Writer        | Scrub temporal comments, add WHY comments, enrich rationale                |
| QR-Docs                 | Verify no temporal contamination, comments explain WHY not WHAT            |

So, why all the feedback loops? QR-Completeness and QR-Code run before TW to
catch structural issues early. QR-Docs runs after TW to validate documentation
quality. Doc issues restart only TW; structure issues restart planning. The loop
runs until both pass.

## Execution Workflow

```
  Plan --> Milestones --> QR --> Docs --> Retrospective
               ^          |
               +- [fail] -+

  * Reconciliation phase precedes Milestones when resuming partial work
```

After planning completes and context clears (`/clear`), execution proceeds:

| Step                   | Purpose                                                         |
| ---------------------- | --------------------------------------------------------------- |
| Execution Planning     | Analyze plan, detect reconciliation signals, output strategy    |
| Reconciliation         | (conditional) Validate existing code against plan               |
| Milestone Execution    | Delegate to agents, run tests; repeat until all complete        |
| Post-Implementation QR | Quality review of implemented code                              |
| Issue Resolution       | (conditional) Present issues, collect decisions, delegate fixes |
| Documentation          | Technical writer updates CLAUDE.md/README.md                    |
| Retrospective          | Present execution summary                                       |

I designed the coordinator to never write code directly -- it delegates to
developers. Separating coordination from implementation produces cleaner
results. The coordinator:

- Parallelizes independent work across up to 4 developers per milestone
- Runs quality review after all milestones complete
- Loops through issue resolution until QR passes
- Invokes technical writer only after QR passes

**Reconciliation** handles resume scenarios. When the user request contains
signals like "already implemented", "resume", or "partially complete", the
workflow validates existing code against plan requirements before executing
remaining milestones. Building on unverified code means rework.

**Issue Resolution** presents each QR finding individually with options (Fix /
Skip / Alternative). Fixes delegate to developers or technical writers, then QR
runs again. This cycle repeats until QR passes.

## Resource Sync Requirements

Resources are **authoritative sources**.

- **SKILL.md** references resources directly (main Claude can read files)
- **Agent prompts** embed resources 1:1 (sub-agents cannot access files
  reliably)

### plan-format.md

Plan template injected by `scripts/planner.py` at planning phase completion.

**No agent sync required** - the script reads and outputs the format directly,
so editing this file takes effect immediately without updating any agent
prompts.

### temporal-contamination.md

Authoritative source for temporal contamination detection. Full content embedded
1:1.

| Synced To                    | Embedded Section           |
| ---------------------------- | -------------------------- |
| `agents/technical-writer.md` | `<temporal_contamination>` |
| `agents/quality-reviewer.md` | `<temporal_contamination>` |

**When updating**: Modify `resources/temporal-contamination.md` first, then copy
content into both `<temporal_contamination>` sections.

### diff-format.md

Authoritative source for unified diff format. Full content embedded 1:1.

| Synced To             | Embedded Section |
| --------------------- | ---------------- |
| `agents/developer.md` | `<diff_format>`  |

**When updating**: Modify `resources/diff-format.md` first, then copy content
into `<diff_format>` section.

### default-conventions.md

Authoritative source for default structural conventions (four-tier decision
backing system). Embedded 1:1 in QR for RULE 2 enforcement; referenced by
planner.py for decision audit.

| Synced To                    | Embedded Section        |
| ---------------------------- | ----------------------- |
| `agents/quality-reviewer.md` | `<default_conventions>` |

**When updating**: Modify `resources/default-conventions.md` first, then copy
full content verbatim into `<default_conventions>` section in QR.

## Sync Verification

After modifying a resource, verify sync:

```bash
# Check temporal-contamination.md references
grep -l "temporal.contamination\|four detection questions\|change-relative\|baseline reference" agents/*.md

# Check diff-format.md references
grep -l "context lines\|AUTHORITATIVE\|APPROXIMATE\|context anchor" agents/*.md

# Check default-conventions.md references
grep -l "default_conventions\|domain: god-object\|domain: test-organization" agents/*.md
```

If grep finds files not listed in sync tables above, update this document.

## Three Pillars Pattern (QR Verification Loops)

The planner skill uses a consistent pattern for all QR verification checkpoints
to ensure fixes are always re-verified. This pattern is implemented in
`scripts/utils.py` and used across all three scripts.

### The Pattern

Every QR checkpoint has three mandatory elements:

| Pillar             | Purpose                              | Implementation                  |
| ------------------ | ------------------------------------ | ------------------------------- |
| **STATE BANNER**   | Visual header showing loop iteration | `get_qr_state_banner()`         |
| **STOP CONDITION** | Explicit blocker preventing skip     | `get_qr_stop_condition()`       |
| **RE-VERIFY MODE** | Different prompts when fixing issues | `--qr-iteration` + `--fixing-*` |

### CLI Flags

All scripts use consistent flags for QR verification loops:

| Flag              | Type    | Default | Purpose                              |
| ----------------- | ------- | ------- | ------------------------------------ |
| `--qr-iteration`  | integer | 1       | Loop count (1=initial, 2+=re-verify) |
| `--fixing-issues` | boolean | false   | Indicates re-work after failed QR    |

### QR Checkpoints

The pattern is applied at four checkpoints:

1. **planner.py review step 1**: Plan QR (Completeness + Code)
2. **planner.py review step 3**: Doc QR (post-TW validation)
3. **execute-milestones.py steps 2-3**: Batch QR gate (per wave)
4. **executor.py step 4**: Holistic post-implementation QR

### Example Flow

```
# Wave with milestones 1,2 - Initial batch QR (iteration 1)
python3 execute-milestones.py --milestones 1,2 --total-milestones 4 --step 2

# Batch QR finds issues -> step 3 routes to fix
python3 execute-milestones.py --milestones 1,2 --total-milestones 4 --step 3 --qr-result ISSUES

# Single developer fixes all issues (iteration 2)
python3 execute-milestones.py --milestones 1,2 --total-milestones 4 --step 1 \
  --fixing-qr-issues --qr-iteration 2

# RE-VERIFY (iteration 2) -- MANDATORY, cannot skip
python3 execute-milestones.py --milestones 1,2 --total-milestones 4 --step 2 \
  --qr-iteration 2
```

### Modifying the Pattern

To change QR verification loop behavior:

1. Edit `scripts/utils.py` (single source of truth)
2. Changes automatically apply to all scripts that import the functions
3. No agent prompt sync required -- pattern is in scripts, not agent prompts

## Limitations

In Gemini CLI, milestones execute sequentially rather than in parallel waves. While this increases execution time for projects with independent milestones, all planning and execution functionality remains available.
