from datetime import datetime
from urllib.parse import quote

import pytest


def load_flattened_cases():
    from conftest import _load_cases

    _, flattened = _load_cases()
    return flattened


ALL_CASES = load_flattened_cases()


def parse_iso_datetime(value):
    if value is None:
        return None
    normalized = value.replace("Z", "+00:00")
    return datetime.fromisoformat(normalized)


def build_url(base_url, request_spec):
    path = request_spec["path"]
    if base_url.endswith("/api") and path.startswith("/api/"):
        path = path[len("/api"):]
    for key, value in request_spec.get("path_params", {}).items():
        path = path.replace(f"{{{key}}}", quote(str(value), safe=""))
    return f"{base_url}{path}"


def send_request(api_session, api_base_url, case):
    request_spec = case["request"]
    method = request_spec["method"].upper()
    url = build_url(api_base_url, request_spec)
    query = request_spec.get("query")
    body = request_spec.get("body")

    kwargs = {"timeout": 20}
    if query:
        kwargs["params"] = query
    if body is not None and method not in {"GET", "DELETE"}:
        kwargs["json"] = body

    response = api_session.request(method, url, **kwargs)
    return response, response.json()


def assert_common_envelope(response, payload, case):
    assert response.status_code == case["expected_http_status"], case["id"]
    assert isinstance(payload, dict), case["id"]
    assert set(payload.keys()) >= {"code", "message", "data", "timestamp"}, case["id"]
    assert isinstance(payload["message"], str) and payload["message"], case["id"]
    assert isinstance(payload["timestamp"], str) and payload["timestamp"], case["id"]

    if case["expected_http_status"] == 200:
        assert payload["code"] == "SUCCESS", case["id"]
        assert payload["message"] == "ok", case["id"]
    else:
        assert payload["code"] == "FAILED", case["id"]


def extract_field_name(case):
    title = case["title"]
    prefixes = [
        "Missing required field ",
        "Blank string for ",
        "Invalid enum for ",
        "Omit optional field "
    ]
    for prefix in prefixes:
        if title.startswith(prefix):
            tail = title[len(prefix):]
            return tail.split(" ")[0]
    if "Positive boundary for " in title:
        tail = title.split("Positive boundary for ", 1)[1]
        return tail.split(" = ", 1)[0]
    return None


def assert_field_error_shape(payload, case):
    field_name = extract_field_name(case)
    if not field_name:
        return
    if case["expected_http_status"] != 400:
        return
    if case["title"].startswith("Invalid enum for "):
        return

    assert isinstance(payload["data"], dict), case["id"]
    assert field_name in payload["data"], case["id"]


def assert_contract_list(payload, case):
    data = payload["data"]
    assert isinstance(data, dict), case["id"]
    assert isinstance(data["content"], list), case["id"]
    assert isinstance(data["totalElements"], int), case["id"]
    assert isinstance(data["totalPages"], int), case["id"]
    assert isinstance(data["page"], int), case["id"]
    assert isinstance(data["size"], int), case["id"]

    query = case["request"].get("query", {})
    expected_page = max(query.get("page", 0), 0)
    expected_size = min(max(query.get("size", 20), 1), 100)

    assert data["page"] == expected_page, case["id"]
    assert data["size"] == expected_size, case["id"]


def assert_contract_create(payload, case):
    data = payload["data"]
    body = case["request"]["body"]
    assert isinstance(data["id"], int) and data["id"] > 0, case["id"]
    assert isinstance(data["contractNo"], str) and data["contractNo"].startswith("LX-"), case["id"]
    assert data["name"] == body["name"], case["id"]
    assert data["contractType"] == body["contractType"], case["id"]
    assert data["partyA"] == body["partyA"], case["id"]
    assert data["partyB"] == body["partyB"], case["id"]

    expected_amount = body.get("amount", 0)
    assert float(data["amount"]) == float(expected_amount), case["id"]

    expected_status = body.get("status", "DRAFT")
    assert data["status"] == expected_status, case["id"]

    if "source" in body and str(body["source"]).strip():
        assert data["source"] == body["source"], case["id"]
    else:
        assert isinstance(data["source"], str) and data["source"].strip(), case["id"]


def assert_contract_status(payload, case):
    data = payload["data"]
    expected_id = case["request"]["path_params"]["id"]
    expected_status = case["request"]["body"]["status"]

    assert data["id"] == expected_id, case["id"]
    assert data["status"] == expected_status, case["id"]
    assert isinstance(data["contractNo"], str) and data["contractNo"], case["id"]


def assert_task_list(payload, case):
    data = payload["data"]
    assert isinstance(data, list), case["id"]
    created_times = []
    for item in data:
        assert isinstance(item["id"], int), case["id"]
        assert isinstance(item["taskNo"], str) and item["taskNo"], case["id"]
        assert isinstance(item["status"], str) and item["status"], case["id"]
        created_times.append(parse_iso_datetime(item["createdAt"]))

    assert created_times == sorted(created_times, reverse=True), case["id"]

    status_filter = case["request"].get("query", {}).get("status")
    if status_filter:
        assert all(item["status"] == status_filter for item in data), case["id"]


def assert_task_status(payload, case):
    data = payload["data"]
    expected_id = case["request"]["path_params"]["id"]
    expected_status = case["request"]["body"]["status"]

    assert data["id"] == expected_id, case["id"]
    assert data["status"] == expected_status, case["id"]
    assert isinstance(data["taskNo"], str) and data["taskNo"], case["id"]


def assert_legal_consultation(payload, case):
    data = payload["data"]
    assert isinstance(data["category"], str) and data["category"], case["id"]
    assert isinstance(data["legalBasis"], list) and data["legalBasis"], case["id"]
    assert isinstance(data["recommendations"], list) and data["recommendations"], case["id"]
    assert isinstance(data["riskAlerts"], list) and data["riskAlerts"], case["id"]


def assert_legal_review(payload, case):
    data = payload["data"]
    assert isinstance(data["risks"], list) and data["risks"], case["id"]
    assert isinstance(data["missingClauses"], list), case["id"]
    assert isinstance(data["summary"], str) and data["summary"], case["id"]

    for risk in data["risks"]:
        assert set(risk.keys()) >= {"level", "clause", "issue", "suggestion"}, case["id"]
        assert risk["level"] in {"LOW", "MEDIUM", "HIGH"}, case["id"]


def assert_legal_draft(payload, case):
    data = payload["data"]
    body = case["request"]["body"]

    assert isinstance(data["generatedContent"], str) and data["generatedContent"], case["id"]
    assert isinstance(data["summary"], str) and data["summary"], case["id"]
    assert isinstance(data["generatedAt"], str) and data["generatedAt"], case["id"]

    assert body["contractName"] in data["generatedContent"], case["id"]
    assert body["partyA"] in data["generatedContent"], case["id"]
    assert body["partyB"] in data["generatedContent"], case["id"]


def assert_endpoint_specific(payload, case):
    endpoint_id = case["endpoint_id"]
    if case["expected_http_status"] != 200:
        assert_field_error_shape(payload, case)
        return

    if endpoint_id == "CT_LIST":
        assert_contract_list(payload, case)
    elif endpoint_id == "CT_CREATE":
        assert_contract_create(payload, case)
    elif endpoint_id == "CT_STATUS":
        assert_contract_status(payload, case)
    elif endpoint_id == "TS_LIST":
        assert_task_list(payload, case)
    elif endpoint_id == "TS_STATUS":
        assert_task_status(payload, case)
    elif endpoint_id == "LEGAL_CONSULTATION":
        assert_legal_consultation(payload, case)
    elif endpoint_id == "LEGAL_REVIEW":
        assert_legal_review(payload, case)
    elif endpoint_id == "LEGAL_DRAFT":
        assert_legal_draft(payload, case)


@pytest.mark.parametrize("case", ALL_CASES, ids=lambda case: case["id"])
def test_generated_blackbox_case(api_session, api_base_url, case):
    response, payload = send_request(api_session, api_base_url, case)
    assert_common_envelope(response, payload, case)
    assert_endpoint_specific(payload, case)
