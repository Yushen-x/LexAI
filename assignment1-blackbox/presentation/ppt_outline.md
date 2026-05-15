# Presentation Outline

## Slide 1 Title

- `LLM-Based Dynamic Black-Box Testing for LexAI REST APIs`
- Team ID
- Member names
- Student IDs

## Slide 2 Background

- What `LexAI` is
- Why black-box testing is needed
- Why LLM assistance is useful

## Slide 3 Project Goal

- Input: API requirements
- Tool: LLM-assisted black-box testcase generator
- Output: structured testcases and analysis

## Slide 4 Selected Techniques

- Equivalence Partitioning
- Boundary Value Analysis
- Decision Table Testing

Explain in one sentence each.

## Slide 5 Testing Scope

- `GET /api/contracts`
- `POST /api/contracts`
- `PUT /api/contracts/{id}/status`
- `GET /api/tasks`
- `PUT /api/tasks/{id}/status`
- `POST /api/legal/consultation`
- `POST /api/legal/contract-review`
- `POST /api/legal/contract-draft`

## Slide 6 Tool Design

- requirement summary
- prompt template
- structured spec
- generator script
- JSON and Markdown outputs

## Slide 7 Prompt and Model

- model: `GPT-5`
- show the core prompt idea
- explain how the prompt asks for EP, BVA, decision tables, and concrete cases

## Slide 8 Sample Generated Output

Show a short JSON or table example with:

- one valid case
- one invalid validation case
- one boundary case
- one missing-resource case

## Slide 9 Experimental Results

Use current generated counts:

- 8 endpoints
- 63 equivalence partitions
- 4 boundary sets
- 19 decision rows
- 56 concrete testcases

## Slide 10 Comparison With Traditional Method

- manual approach vs AI-assisted approach
- pros and cons

## Slide 11 Limitations and Improvements

- limitations of current AI-generated testing
- how prompt refinement and post-processing improve quality

## Slide 12 Conclusion

- black-box testing is a strong fit for this assignment
- AI helps generate structured and reusable tests quickly
- future work: executable automation and measured defect detection

## Q&A Preparation

Be ready to answer:

- Why black-box instead of static or white-box testing?
- How do you know the generated tests are useful?
- What are the limits of using LLMs in testing?
- How would you extend this into automated execution?
