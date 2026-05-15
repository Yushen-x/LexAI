# LLM-Based Dynamic Black-Box Testing for LexAI REST APIs

## Summary

- Endpoint Count: 8
- Equivalence Partition Count: 63
- Boundary Set Count: 4
- Decision Row Count: 19
- Test Case Count: 56

## CT_LIST GET /api/contracts

- Input variables: 5
- Equivalence partitions: 6
- Boundary sets: 3
- Decision rows: 2
- Concrete testcases: 13

### Sample Testcases

- `CT_LIST-TC01` Baseline valid request -> HTTP 200
- `CT_LIST-TC02` Invalid enum for status -> HTTP 400
- `CT_LIST-TC03` Omit optional field page for default handling -> HTTP 200
- `CT_LIST-TC04A` Lower boundary for page = -1 -> HTTP 200

## CT_CREATE POST /api/contracts

- Input variables: 7
- Equivalence partitions: 17
- Boundary sets: 0
- Decision rows: 3
- Concrete testcases: 13

### Sample Testcases

- `CT_CREATE-TC01` Baseline valid request -> HTTP 200
- `CT_CREATE-TC02` Missing required field name -> HTTP 400
- `CT_CREATE-TC03` Blank string for name -> HTTP 400
- `CT_CREATE-TC04` Missing required field contractType -> HTTP 400

## CT_STATUS PUT /api/contracts/{id}/status

- Input variables: 2
- Equivalence partitions: 6
- Boundary sets: 0
- Decision rows: 4
- Concrete testcases: 4

### Sample Testcases

- `CT_STATUS-TC01` Baseline valid request -> HTTP 200
- `CT_STATUS-TC02` Missing required field status -> HTTP 400
- `CT_STATUS-TC03` Invalid enum for status -> HTTP 400
- `CT_STATUS-TC04` Missing resource id for id -> HTTP 404

## TS_LIST GET /api/tasks

- Input variables: 1
- Equivalence partitions: 2
- Boundary sets: 0
- Decision rows: 0
- Concrete testcases: 2

### Sample Testcases

- `TS_LIST-TC01` Baseline valid request -> HTTP 200
- `TS_LIST-TC02` Invalid enum for status -> HTTP 400

## TS_STATUS PUT /api/tasks/{id}/status

- Input variables: 2
- Equivalence partitions: 6
- Boundary sets: 0
- Decision rows: 4
- Concrete testcases: 4

### Sample Testcases

- `TS_STATUS-TC01` Baseline valid request -> HTTP 200
- `TS_STATUS-TC02` Missing required field status -> HTTP 400
- `TS_STATUS-TC03` Invalid enum for status -> HTTP 400
- `TS_STATUS-TC04` Missing resource id for id -> HTTP 404

## LEGAL_CONSULTATION POST /api/legal/consultation

- Input variables: 2
- Equivalence partitions: 4
- Boundary sets: 0
- Decision rows: 2
- Concrete testcases: 3

### Sample Testcases

- `LEGAL_CONSULTATION-TC01` Baseline valid request -> HTTP 200
- `LEGAL_CONSULTATION-TC02` Missing required field question -> HTTP 400
- `LEGAL_CONSULTATION-TC03` Blank string for question -> HTTP 400

## LEGAL_REVIEW POST /api/legal/contract-review

- Input variables: 2
- Equivalence partitions: 6
- Boundary sets: 0
- Decision rows: 2
- Concrete testcases: 5

### Sample Testcases

- `LEGAL_REVIEW-TC01` Baseline valid request -> HTTP 200
- `LEGAL_REVIEW-TC02` Missing required field contractTitle -> HTTP 400
- `LEGAL_REVIEW-TC03` Blank string for contractTitle -> HTTP 400
- `LEGAL_REVIEW-TC04` Missing required field contractContent -> HTTP 400

## LEGAL_DRAFT POST /api/legal/contract-draft

- Input variables: 7
- Equivalence partitions: 16
- Boundary sets: 1
- Decision rows: 2
- Concrete testcases: 12

### Sample Testcases

- `LEGAL_DRAFT-TC01` Baseline valid request -> HTTP 200
- `LEGAL_DRAFT-TC02` Missing required field contractName -> HTTP 400
- `LEGAL_DRAFT-TC03` Blank string for contractName -> HTTP 400
- `LEGAL_DRAFT-TC04` Missing required field contractType -> HTTP 400

