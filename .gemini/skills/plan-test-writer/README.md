# Plan Test Writer

A test-driven development skill that translates implementation plans into comprehensive test suites.

## Purpose

This skill ensures that before any code is written, there are executable specifications (tests) that:

- Validate expected behavior
- Prevent scope creep
- Serve as documentation
- Enable confident refactoring

## When to Use

Use this skill when:

- You have a clear implementation plan documented
- You want to follow TDD principles
- You need tests that serve as executable specifications

**Do not use** when:

- No plan exists yet (establish the plan first)
- Requirements are unclear or ambiguous
- The task is trivial and self-documenting

## Test Categories Generated

1. **Happy Path Tests** - Normal, expected usage
2. **Boundary Tests** - Edge cases at input limits
3. **Error Handling Tests** - Invalid inputs, failure modes
4. **Integration Tests** - Feature interactions with existing code

## Output

For each planned feature, you receive:

- Test file location recommendation
- Complete, runnable test code
- Coverage notes linking tests to plan requirements
- List of any ambiguities needing resolution

## Installation

### Gemini CLI

```bash
gemini extensions install https://github.com/axatbhardwaj/gemini-skills/tree/main/plan-test-writer
```

### Claude Code

Copy `SKILL.md` to your Claude Code skills directory or reference it in your project's CLAUDE.md.
