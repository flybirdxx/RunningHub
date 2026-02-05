---
name: plan-test-writer
description: Translates implementation plans into comprehensive test suites for test-driven development.
---

# Plan Test Writer

Translates implementation plans into comprehensive test suites using test-driven
development principles.

## When to Use

- After establishing a clear implementation plan
- Before writing implementation code
- When you need executable specifications that prevent scope creep

## Prerequisites

Before writing tests, verify:

1. A plan or specification exists (in conversation, comments, or planning files)
2. Expected inputs and outputs are defined
3. Success criteria are clear
4. Edge cases and error conditions are identified

If any are missing, ask for clarification first.

## Test Writing Methodology

### Structure (AAA Pattern)

- **Arrange**: Set up preconditions and inputs
- **Act**: Execute the behavior being tested
- **Assert**: Verify the expected outcome

### Test Categories

1. **Happy Path**: Normal, expected usage per the plan
2. **Boundary**: Edge cases at limits of valid input
3. **Error Handling**: Invalid inputs, missing dependencies, failure modes
4. **Integration Points**: How the feature interacts with existing code

### Naming Convention

Use descriptive names that read like specifications:

- `should_[expected_behavior]_when_[condition]`
- `returns_[output]_given_[input]`
- `throws_[error]_if_[invalid_condition]`

## Output Format

For each planned feature, provide:

1. **Test File Location**: Where the test file should live
2. **Test Cases**: Complete, runnable test code
3. **Coverage Notes**: What aspects of the plan each test validates
4. **Missing Specifications**: Any ambiguities needing resolution

## Quality Principles

- Tests should fail meaningfully when behavior deviates from the plan
- Each test verifies ONE specific behavior
- Tests must be deterministic and repeatable
- Avoid testing implementation details; test observable behavior
- Include comments linking tests to specific plan requirements

## Self-Verification

Before presenting tests, verify:

- [ ] Every planned feature has corresponding test coverage
- [ ] Tests are independent and can run in any order
- [ ] Error messages clearly indicate what went wrong
- [ ] No tests depend on external state or network calls
- [ ] Tests align with the project's existing patterns
