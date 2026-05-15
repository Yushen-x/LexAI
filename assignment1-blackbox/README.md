# Assignment 1 Black-Box Testing Package

This folder contains a report-ready black-box testing package for `LexAI`.

## Project Title

LLM-Based Dynamic Black-Box Testing for LexAI REST APIs

## Scope

The experiment focuses on externally observable REST API behavior instead of internal implementation details. The chosen techniques are:

- Equivalence Partitioning
- Boundary Value Analysis
- Decision Table Testing

Covered API groups:

- `GET /api/contracts`
- `POST /api/contracts`
- `PUT /api/contracts/{id}/status`
- `GET /api/tasks`
- `PUT /api/tasks/{id}/status`
- `POST /api/legal/consultation`
- `POST /api/legal/contract-review`
- `POST /api/legal/contract-draft`

## Artifact Map

- `input/lexai_blackbox_requirements.md`: black-box requirement summary derived from the current project charter and REST contracts
- `input/lexai_blackbox_spec.json`: structured input for the testcase generator
- `tool/model_artifact.md`: model and artifact notes for the assignment submission
- `tool/prompt_template.md`: prompt used for LLM-assisted testcase generation
- `tool/generate_blackbox_tests.py`: local generator that converts the structured spec into JSON and Markdown outputs
- `output/lexai_blackbox_testcases.json`: generated testcase artifact
- `output/lexai_blackbox_summary.md`: human-readable generated summary
- `analysis/experimental_analysis.md`: experiment design, observations, and report material

## Recommended Submission Framing

- Input: `LexAI` REST API requirements
- Tool artifact: `GPT-5` prompt template, generator script, generated code
- Generated output: black-box testcases in JSON and Markdown
- Experimental analysis: coverage, effectiveness, limits, and improvement strategy

## Regeneration

From the repo root:

```bash
python assignment1-blackbox/tool/generate_blackbox_tests.py ^
  --spec assignment1-blackbox/input/lexai_blackbox_spec.json ^
  --json-out assignment1-blackbox/output/lexai_blackbox_testcases.json ^
  --markdown-out assignment1-blackbox/output/lexai_blackbox_summary.md
```

## Notes

- The test design is requirement-driven and keeps a black-box perspective.
- The current package is designed to be directly reusable in the report and presentation.
- If you later want executable API tests, this package can be extended into Postman or pytest cases.
- The bundled `run_api_pytest.ps1` defaults to the current repo path `D:\Desktop\LexAI` and can be overridden with `LEXAI_REPO_ROOT`.
