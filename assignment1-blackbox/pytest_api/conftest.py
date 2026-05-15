import json
import os
from pathlib import Path

import pytest
import requests


DEFAULT_BASE_URL = "http://localhost:8081/api"
CASE_FILE = Path(__file__).resolve().parents[1] / "output" / "lexai_blackbox_testcases.json"


def _load_cases():
    payload = json.loads(CASE_FILE.read_text(encoding="utf-8"))
    flattened = []
    for endpoint in payload["endpoints"]:
        for case in endpoint["test_cases"]:
            row = dict(case)
            row["endpoint_id"] = endpoint["id"]
            row["endpoint_name"] = endpoint["name"]
            row["endpoint_method"] = endpoint["method"]
            row["endpoint_path"] = endpoint["path"]
            flattened.append(row)
    return payload, flattened


@pytest.fixture(scope="session")
def generated_payload():
    payload, _ = _load_cases()
    return payload


@pytest.fixture(scope="session")
def generated_cases():
    _, flattened = _load_cases()
    return flattened


@pytest.fixture(scope="session")
def api_base_url():
    return os.getenv("LEXAI_BASE_URL", DEFAULT_BASE_URL).rstrip("/")


@pytest.fixture(scope="session")
def api_session():
    session = requests.Session()
    session.headers.update({"Accept": "application/json"})
    yield session
    session.close()


@pytest.fixture(scope="session", autouse=True)
def verify_backend_ready(api_session, api_base_url):
    try:
        response = api_session.get(f"{api_base_url}/system/health", timeout=10)
    except requests.RequestException as exc:
        pytest.exit(
            f"Backend is not reachable at {api_base_url}. "
            f"Start the backend first or run assignment1-blackbox/run_api_pytest.ps1. "
            f"Details: {exc}"
        )

    if response.status_code != 200:
        pytest.exit(
            f"Backend health check failed at {api_base_url}/system/health "
            f"with HTTP {response.status_code}."
        )
