# Pytest API Execution

This folder turns the generated black-box testcase artifact into executable `pytest` API tests.

## What It Covers

- all `56` generated black-box testcases
- request execution against the running `LexAI` backend
- common envelope assertions
- endpoint-specific assertions for pagination, defaults, validation, and response structure

## Default Base URL

The tests read `LEXAI_BASE_URL` from the environment.

Default:

```text
http://localhost:8081/api
```

## Fastest Way To Run

Use the provided runner from the repo root:

```powershell
./assignment1-blackbox/run_api_pytest.ps1
```

The runner:

- starts the backend on port `18081`
- forces `mock` AI mode
- uses an isolated in-memory H2 database
- waits for health check success
- runs `pytest`
- stops the backend process
- defaults to the current repo path `D:\Desktop\LexAI`

If your `LexAI` repo is elsewhere:

```powershell
$env:LEXAI_REPO_ROOT='D:\path\to\LexAI'
powershell -ExecutionPolicy Bypass -File .\assignment1-blackbox\run_api_pytest.ps1
```

## Manual Run

If the backend is already running:

```powershell
$env:LEXAI_BASE_URL='http://localhost:8081/api'
pytest assignment1-blackbox/pytest_api -q
```
