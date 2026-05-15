# Pytest Execution Summary

## Command

```powershell
powershell -ExecutionPolicy Bypass -File .\assignment1-blackbox\run_api_pytest.ps1
```

## Result

- executable test framework: `pytest`
- generated black-box cases executed: `56`
- passed: `56`
- failed: `0`

## Notes

- the runner starts the backend on port `18081`
- AI mode is forced to `mock` for deterministic execution
- an isolated in-memory H2 database is used for the run
- invalid enum query-parameter handling was fixed so query conversion errors now return HTTP `400` instead of `500`
