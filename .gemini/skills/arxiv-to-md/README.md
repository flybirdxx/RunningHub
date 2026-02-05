# arxiv-to-md

Convert arXiv papers (TeX source) to clean, LLM-consumable markdown.

## Usage

Single paper:

```
Convert arxiv.org/abs/2503.05179 to markdown
```

Multiple papers:

```
Convert these papers to markdown: 2503.05179, 2401.12345, 2312.09876
```

## Prerequisites

- pandoc binary installed (`brew install pandoc` or equivalent)

## Installation

```bash
gemini extensions install /path/to/gemini-skills/arxiv-to-md
```

## How It Works

The skill orchestrates a multi-step workflow:

1. **Discover** - Extract arXiv IDs from user input or context
2. **Fetch** - Download TeX source from arXiv
3. **Preprocess** - Clean and prepare TeX for conversion
4. **Convert** - Use pandoc to convert to markdown
5. **Clean** - Remove artifacts and format output
6. **Verify** - Validate output quality

The skill processes each paper systematically and outputs clean markdown files.
