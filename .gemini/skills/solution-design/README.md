# solution-design

Generate diverse solutions from multiple reasoning perspectives for defined problems.

## Usage

Use when you have a clear problem and need solution options:

```
I need solutions for reducing API latency (currently 500ms, target 100ms)
```

```
How can I handle database migration with zero downtime?
```

## When to Use

- You've identified a specific problem or root cause
- You need multiple solution approaches
- You want to explore trade-offs before choosing

**Not for problem identification** - Use `problem-analysis` skill to find root causes first.

## Installation

```bash
gemini extensions install /path/to/gemini-skills/solution-design
```

## How It Works

The skill generates solutions using multiple reasoning perspectives:

1. **First Principles** - Build from fundamental constraints
2. **Analogical** - Apply patterns from other domains
3. **Constraint Relaxation** - Question fixed assumptions
4. **Decomposition** - Break problem into solvable parts
5. **Inversion** - Prevent the problem instead of solving it

Each perspective produces distinct solution candidates. The skill then:

- **Evaluates** feasibility and trade-offs
- **Compares** approaches across dimensions (cost, complexity, risk)
- **Recommends** based on your context and constraints

This produces a diverse solution set rather than a single "best" answer.
