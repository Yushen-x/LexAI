# Project Report Outline

## Cover Page

- Title: `LLM-Based Dynamic Black-Box Testing for LexAI REST APIs`
- Team ID: `<fill in>`
- Member names: `<fill in>`
- Student IDs: `<fill in>`

## 1. Introduction

- Briefly introduce `LexAI` as the system under test.
- State that the project uses `LLM-assisted dynamic black-box testing`.
- State the selected techniques:
  - Equivalence Partitioning
  - Boundary Value Analysis
  - Decision Table Testing

## 2. Input

- Describe the requirement source:
  - `assignment1-blackbox/input/lexai_blackbox_requirements.md`
  - API contracts derived from the current backend
- Explain the testing scope:
  - contracts APIs
  - tasks APIs
  - legal workspace APIs

## 3. Tool Artifact

- Model used: `GPT-5`
- Prompt used: `assignment1-blackbox/tool/prompt_template.md`
- Generated helper code: `assignment1-blackbox/tool/generate_blackbox_tests.py`
- Structured input spec: `assignment1-blackbox/input/lexai_blackbox_spec.json`

Describe the workflow:

1. Read requirement summary.
2. Identify endpoint inputs and constraints.
3. Generate EP, BVA, and decision-table artifacts.
4. Output concrete testcases in JSON and Markdown.

## 4. Generated Output

Use:

- `assignment1-blackbox/output/lexai_blackbox_testcases.json`
- `assignment1-blackbox/output/lexai_blackbox_summary.md`

Suggested numbers to report from the current run:

- endpoints covered: `8`
- equivalence partitions: `63`
- boundary sets: `4`
- decision rows: `19`
- concrete testcases: `56`

Include 4 to 8 representative testcases in table form.

## 5. Experimental Analysis

### 5.1 Coverage

Discuss:

- all selected endpoints are covered
- required-field validation is covered
- enum-domain validation is covered
- pagination and positive-value boundaries are covered
- missing-resource cases are covered

### 5.2 Effectiveness

Discuss how the generated tests help detect:

- missing validation
- invalid enum handling
- wrong default handling
- broken pagination normalization
- incorrect 404 behavior

### 5.3 Generalizability

Discuss how the same workflow can be reused for:

- future LexAI APIs
- other CRUD-based web systems
- systems with mixed query/body/path inputs

## 6. Comparison With Traditional Non-AI Technique

### Traditional approach

- manual reading of requirements
- manual testcase drafting
- manual formatting into tables

### AI-assisted approach

- faster identification of input variables
- more consistent testcase structure
- easier generation of multiple endpoint artifacts

### Pros

- speed
- consistency
- easier expansion

### Cons

- still needs human review
- may produce overlapping cases
- may miss hidden business rules not clearly documented

## 7. Limitations and Improvements

Limitations:

- semantic correctness is harder to assert for AI-facing endpoints
- requirement text may not include every business rule
- AI can over-generate similar cases

Improvements:

- add testcase deduplication
- add executable API scripts
- add measured pass/fail execution results
- add stronger prompt constraints per endpoint

## 8. Summary

Conclude that LLM-assisted black-box testing is effective for requirement-driven REST API testing, especially for validation rules, default handling, and interface-level coverage.
