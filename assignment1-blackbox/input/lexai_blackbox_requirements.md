# LexAI Black-Box Requirement Summary

## 1. Testing Goal

Design a black-box testing tool that reads requirement-level API specifications and produces:

- input variables
- equivalence partitions
- boundary values
- decision tables
- concrete testcases

The tool must stay focused on externally observable behavior:

- request parameters and payload validation
- default values
- filtering behavior
- pagination behavior
- response envelope structure
- error responses for invalid input and missing resources

## 2. System Under Test

System name: `LexAI`

Testing target: public REST APIs exposed by the backend

## 3. Common Response Rules

### R-COMMON-01

Successful requests return an `ApiResponse` envelope with:

- `code = "SUCCESS"`
- `message = "ok"`
- `data = <payload>`
- `timestamp = <server time>`

### R-COMMON-02

Validation failures return HTTP `400` with:

- `code = "FAILED"`
- a validation message
- `data = { fieldName: errorMessage }` for field-level validation failures

### R-COMMON-03

Malformed JSON or invalid enum/type values return HTTP `400` with:

- `code = "FAILED"`
- a message indicating invalid JSON or field type

### R-COMMON-04

Missing resources return HTTP `404` with:

- `code = "FAILED"`
- a message indicating the resource does not exist

## 4. API Requirements

## 4.1 `GET /api/contracts`

### R-CT-LIST-01

The API returns a paginated contract list.

### R-CT-LIST-02

Optional filters:

- `keyword`
- `status`
- `type`

### R-CT-LIST-03

If `keyword` is present, the search matches contract name or contract number.

### R-CT-LIST-04

If `page` is omitted, the default page is `0`.

### R-CT-LIST-05

If `page < 0`, the effective page is clamped to `0`.

### R-CT-LIST-06

If `size` is omitted, the default size is `20`.

### R-CT-LIST-07

If `size < 1`, the effective size is clamped to `1`.

### R-CT-LIST-08

If `size > 100`, the effective size is clamped to `100`.

## 4.2 `POST /api/contracts`

### R-CT-CREATE-01

Required non-blank fields:

- `name`
- `contractType`
- `partyA`
- `partyB`

### R-CT-CREATE-02

If `amount` is omitted, the system uses `0`.

### R-CT-CREATE-03

If `status` is omitted, the system uses `DRAFT`.

### R-CT-CREATE-04

If `source` is omitted or blank, the system uses a default source label.

## 4.3 `PUT /api/contracts/{id}/status`

### R-CT-STATUS-01

The request body must contain a non-null `status`.

### R-CT-STATUS-02

Valid `status` values:

- `DRAFT`
- `UNDER_REVIEW`
- `SIGNED`
- `IN_PROGRESS`
- `COMPLETED`
- `TERMINATED`

### R-CT-STATUS-03

If the contract id does not exist, the API returns `404`.

## 4.4 `GET /api/tasks`

### R-TS-LIST-01

The API returns tasks ordered by creation time descending.

### R-TS-LIST-02

Optional filter:

- `status`

### R-TS-LIST-03

Valid task status values:

- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`
- `REJECTED`

## 4.5 `PUT /api/tasks/{id}/status`

### R-TS-STATUS-01

The request body must contain a non-null `status`.

### R-TS-STATUS-02

Valid `status` values:

- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`
- `REJECTED`

### R-TS-STATUS-03

If the task id does not exist, the API returns `404`.

## 4.6 `POST /api/legal/consultation`

### R-LC-01

`question` is required and must be non-blank.

### R-LC-02

`facts` is optional.

### R-LC-03

A successful request returns a structured consultation response in the standard `ApiResponse` envelope.

## 4.7 `POST /api/legal/contract-review`

### R-LR-01

`contractTitle` is required and must be non-blank.

### R-LR-02

`contractContent` is required and must be non-blank.

### R-LR-03

A successful request returns a structured review response in the standard `ApiResponse` envelope.

## 4.8 `POST /api/legal/contract-draft`

### R-LD-01

Required non-blank fields:

- `contractName`
- `contractType`
- `partyA`
- `partyB`

### R-LD-02

If `amount` is provided, it must be a positive integer.

### R-LD-03

Optional fields:

- `duration`
- `requirements`

### R-LD-04

A successful request returns generated contract content in the standard `ApiResponse` envelope.

## 5. Black-Box Testing Focus

The test generator should emphasize:

- valid vs invalid input partitions
- omitted fields vs explicitly provided fields
- numeric and pagination boundaries
- enum-domain validity
- observable default-value behavior
- stable error handling behavior

## 6. Expected Deliverables

The generated output should contain:

- endpoint-level partitions
- boundary values
- decision table rows
- concrete testcases with request, expected status, and expected result summary
