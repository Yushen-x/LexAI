# Experimental Analysis

## 1. Objective

Evaluate whether an LLM-assisted black-box testing workflow can generate useful, structured, and reasonably complete API testcases for `LexAI` from requirement-level inputs.

## 2. Experiment Setup

### Input

- project charter and API-level requirement summary
- observable request fields, enum domains, default values, and error behavior

### Tool

- model: `GPT-5`
- prompt artifact: `assignment1-blackbox/tool/prompt_template.md`
- generator: `assignment1-blackbox/tool/generate_blackbox_tests.py`

### Covered APIs

- contract list
- contract creation
- contract status update
- task list
- task status update
- legal consultation
- contract review
- contract draft

### Testing Techniques

- Equivalence Partitioning
- Boundary Value Analysis
- Decision Table Testing

### Current Generated Snapshot

- covered endpoints: `8`
- equivalence partitions: `63`
- boundary sets: `4`
- decision-table rows: `19`
- concrete testcases: `56`

## 3. Evaluation Dimensions

### 3.1 Coverage

Measure how many documented endpoints and input rules are reflected by generated testcases.

Recommended metrics:

- endpoint coverage
- required-field coverage
- enum-domain coverage
- numeric-boundary coverage
- error-response coverage

### 3.2 Effectiveness

Measure whether the generated tests are useful for detecting incorrect behavior such as:

- missing validation
- wrong default handling
- incorrect enum handling
- broken pagination normalization
- incorrect resource-not-found behavior

### 3.3 Generalizability

Measure whether the same prompt and generator pattern can be reused for:

- new LexAI endpoints
- other CRUD-style systems
- systems with mixed query/body/path validation rules

## 4. Initial Observations

### Strengths

- The API contracts expose clear input domains, which makes them a good fit for EP and BVA.
- The generated tests are structured enough to be transformed into executable tests later.
- Decision-table rows are especially useful for documenting default handling and missing-resource behavior.
- The same workflow works across business APIs and AI-facing APIs.

### Weaknesses

- Requirement text alone is not always enough to infer exact success payload content.
- AI-facing endpoints have weaker oracle quality because semantic correctness is harder to assert than validation behavior.
- Some black-box expectations are implementation-shaped because defaults and clamp behavior are documented through API contracts rather than pure business requirements.

## 5. Comparison With Traditional Non-AI Technique

### Traditional Manual Approach

- tester reads the API spec manually
- tester writes partitions and boundaries by hand
- tester assembles testcase tables one endpoint at a time

### AI-Assisted Approach

- model identifies variables and candidate partitions quickly
- model produces a consistent testcase schema across endpoints
- model reduces repetitive testcase authoring effort

### Pros

- faster testcase drafting
- more uniform testcase structure
- easier expansion from one endpoint to many endpoints
- easier report generation because artifacts are already structured

### Cons

- the model may generate duplicated or overly similar cases
- the model may miss hidden business rules not clearly written in the requirement
- AI output still needs human review before execution

## 6. Limitations Encountered

- Pure natural-language requirements may omit exact defaults, status domains, and error-message expectations.
- LLMs can over-generate testcases that look plausible but add little new coverage.
- For AI-generated business content, expected semantic output is harder to verify than HTTP status and schema correctness.

## 7. Improvement Strategy

- refine the prompt to force one baseline case plus one invalid case per critical field
- add schema constraints so the output stays machine-readable
- separate validation-oriented tests from semantic-quality tests
- add post-processing rules to remove duplicates and keep high-value boundary cases
- extend the next iteration with executable API scripts and measured pass/fail results

## 8. Suggested Report Narrative

This project shows that LLM-assisted black-box testing is a practical fit for REST APIs with explicit input contracts. The AI workflow is especially effective at rapidly generating equivalence partitions, numeric boundaries, and decision-table combinations. The main limitation is that test oracles are strongest for validation and interface behavior, but weaker for semantic AI output quality. Therefore, the most effective usage pattern is human-reviewed AI generation followed by automated execution in a later step.
