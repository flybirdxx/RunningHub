# Plan Test Writer

Translates implementation plans into comprehensive test suites using test-driven
development principles.

## When to Use

- After establishing a clear implementation plan
- Before writing implementation code
- When you need executable specifications that prevent scope creep

## Prerequisites

Before writing tests, verify:

1. A plan or specification exists
2. Expected inputs and outputs are defined
3. Success criteria are clear
4. Edge cases are identified

If any are missing, ask for clarification first.

## Test Categories

1. **Happy Path**: Normal, expected usage per the plan
2. **Boundary**: Edge cases at limits of valid input
3. **Error Handling**: Invalid inputs, failure modes
4. **Integration Points**: How the feature interacts with existing code

## Output Format

For each planned feature, provide:

1. **Test File Location**: Where the test file should live
2. **Test Cases**: Complete, runnable test code
3. **Coverage Notes**: What aspects of the plan each test validates
4. **Missing Specifications**: Any ambiguities needing resolution
