# deepthink

Structured multi-step reasoning for open-ended analytical questions where the answer structure is itself unknown.

## Usage

Invoke when you need structured analysis of complex questions:

```
What are the fundamental trade-offs in database consistency models?
```

```
How should I think about categorizing software testing strategies?
```

## When to Use

- Taxonomy design (creating categorization frameworks)
- Conceptual analysis (exploring abstract concepts)
- Trade-off exploration (comparing multiple options)
- Definitional questions (what is X, fundamentally?)

## Installation

```bash
gemini extensions install /path/to/gemini-skills/deepthink
```

## How It Works

The skill uses a 14-step reasoning workflow:

1. **Question Analysis** - Understand what's being asked
2. **Dimension Identification** - Find key aspects to explore
3. **Hypothesis Generation** - Propose initial frameworks
4. **Evidence Gathering** - Collect supporting/contradicting data
5. **Synthesis** - Build coherent answer structure
6. **Verification** - Check for gaps and validate reasoning

This produces well-structured, evidence-backed analysis for complex questions.
