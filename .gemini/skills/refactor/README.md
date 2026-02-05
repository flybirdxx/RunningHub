# refactor

Refactoring analysis and technical debt review across multiple code quality dimensions.

## Usage

Request analysis of any scope:

```
Analyze this codebase for refactoring opportunities
```

```
Quick look at refactoring needs in src/utils/
```

```
Comprehensive technical debt review
```

## Installation

```bash
gemini extensions install /path/to/gemini-skills/refactor
```

## How It Works

The skill explores code quality across randomly selected categories:

- **Code smells** - Duplicated code, long methods, feature envy
- **Architecture** - Layer violations, circular dependencies
- **Security** - Input validation, resource cleanup
- **Performance** - Unnecessary allocations, N+1 queries
- **Maintainability** - Magic numbers, incomplete error handling

After exploration, the skill:

1. **Triages** findings with unique IDs
2. **Clusters** related issues by root cause
3. **Contextualizes** based on your stated goals
4. **Synthesizes** actionable work items

## Scope Control

The `--n` parameter controls how many categories to explore:

- **Small** (single file, quick look): 5 categories
- **Medium** (module, standard analysis): 10 categories (default)
- **Large** (entire codebase, comprehensive): 25 categories

The skill automatically adjusts based on your request phrasing.
