import argparse
import json
from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path


def load_spec(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def field_location_groups(endpoint):
    request = endpoint.get("request", {})
    return {
        "query": request.get("query", []),
        "path": request.get("path_params", []),
        "body": request.get("body", [])
    }


def valid_value(field):
    if "sample_valid" in field:
        return deepcopy(field["sample_valid"])
    field_type = field.get("type")
    if field_type == "string":
        return "sample"
    if field_type == "enum":
        return field["values"][0]
    if field_type == "integer":
        return max(field.get("min_effective", 0), 0)
    if field_type == "long":
        return 1
    if field_type == "decimal":
        return 1.0
    if field_type == "list_string":
        return ["sample item"]
    return "sample"


def invalid_enum_value(field):
    if field.get("type") == "enum":
        return "UNKNOWN_VALUE"
    return None


def build_valid_request(endpoint):
    req = {"method": endpoint["method"], "path": endpoint["path"]}
    groups = field_location_groups(endpoint)

    if groups["path"]:
        req["path_params"] = {field["name"]: valid_value(field) for field in groups["path"]}

    if groups["query"]:
        req["query"] = {}
        for field in groups["query"]:
            if field["name"] in {"page", "size"}:
                req["query"][field["name"]] = valid_value(field)

    if groups["body"]:
        req["body"] = {
            field["name"]: valid_value(field)
            for field in groups["body"]
            if field.get("required") or "default" in field
        }
        for field in groups["body"]:
            if not field.get("required") and field["name"] in {"amount", "facts"}:
                req["body"][field["name"]] = valid_value(field)
        if not req["body"]:
            req.pop("body", None)

    return req


def input_variables(endpoint):
    rows = []
    for location, fields in field_location_groups(endpoint).items():
        for field in fields:
            rows.append({
                "name": field["name"],
                "location": location,
                "type": field["type"],
                "required": field.get("required", False)
            })
    return rows


def partitions_for_field(field, location):
    rows = []
    base_id = f"{location.upper()}-{field['name']}"
    field_type = field["type"]

    if field.get("required"):
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP1",
            "class": "valid",
            "description": "Required field provided with a valid value."
        })
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP2",
            "class": "invalid",
            "description": "Required field omitted from the request."
        })

    if field_type == "string" and field.get("non_blank"):
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP3",
            "class": "invalid",
            "description": "Field is present but blank or whitespace-only."
        })

    if field_type == "enum":
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP4",
            "class": "valid",
            "description": "Field uses one of the documented enum values."
        })
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP5",
            "class": "invalid",
            "description": "Field uses an enum value outside the documented domain."
        })

    if field.get("positive"):
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP6",
            "class": "valid",
            "description": "Numeric value is strictly greater than zero."
        })
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP7",
            "class": "invalid",
            "description": "Numeric value is zero or negative."
        })

    if not field.get("required") and "default" in field:
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP8",
            "class": "valid",
            "description": f"Optional field omitted and server applies default value {field['default']}."
        })

    if not rows:
        rows.append({
            "field": field["name"],
            "partition_id": f"{base_id}-EP0",
            "class": "valid",
            "description": "Optional field provided with a valid value."
        })

    return rows


def boundaries_for_field(field, location):
    rows = []
    field_type = field["type"]

    if field_type in {"integer", "long", "decimal"} and "min_effective" in field:
        min_value = field["min_effective"]
        rows.append({
            "field": field["name"],
            "location": location,
            "values": [min_value - 1, min_value, min_value + 1],
            "rationale": "Boundary set around the lower effective limit."
        })

    if field_type in {"integer", "long"} and "max_effective" in field:
        max_value = field["max_effective"]
        rows.append({
            "field": field["name"],
            "location": location,
            "values": [max_value - 1, max_value, max_value + 1],
            "rationale": "Boundary set around the upper effective limit."
        })

    if field.get("positive"):
        rows.append({
            "field": field["name"],
            "location": location,
            "values": [-1, 0, 1],
            "rationale": "Boundary set for a positive-only numeric field."
        })

    return rows


def decision_rows(endpoint):
    rows = []
    groups = field_location_groups(endpoint)
    body = groups["body"]
    query = groups["query"]

    if body:
        required_present = any(field.get("required") for field in body)
        has_defaults = any("default" in field for field in body)
        if required_present:
            rows.append({
                "conditions": {
                    "required_fields_valid": True,
                    "optional_fields_omitted": False
                },
                "expected_result": "Request succeeds with HTTP 200 and returns SUCCESS envelope."
            })
            rows.append({
                "conditions": {
                    "required_fields_valid": False,
                    "optional_fields_omitted": False
                },
                "expected_result": "Request fails with HTTP 400 due to validation error."
            })
        if has_defaults:
            rows.append({
                "conditions": {
                    "required_fields_valid": True,
                    "optional_fields_omitted": True
                },
                "expected_result": "Request succeeds and server applies documented default values."
            })

    for field in query:
        if "min_effective" in field or "max_effective" in field:
            rows.append({
                "conditions": {
                    "parameter": field["name"],
                    "outside_effective_range": True
                },
                "expected_result": "Request still succeeds and the server normalizes the parameter."
            })

    path_fields = groups["path"]
    if path_fields:
        rows.append({
            "conditions": {
                "resource_exists": True
            },
            "expected_result": "Request succeeds with HTTP 200."
        })
        rows.append({
            "conditions": {
                "resource_exists": False
            },
            "expected_result": "Request fails with HTTP 404 and FAILED envelope."
        })

    return rows


def missing_required_case(endpoint, field, case_id):
    request = build_valid_request(endpoint)
    body = request.get("body", {})
    query = request.get("query", {})
    body.pop(field["name"], None)
    query.pop(field["name"], None)
    return {
        "id": case_id,
        "title": f"Missing required field {field['name']}",
        "request": request,
        "expected_http_status": 400,
        "expected_result": "FAILED response with field-level validation error.",
        "coverage_tags": ["EP"]
    }


def blank_string_case(endpoint, field, location, case_id):
    request = build_valid_request(endpoint)
    target_key = "body" if location == "body" else "query"
    request.setdefault(target_key, {})[field["name"]] = "   "
    return {
        "id": case_id,
        "title": f"Blank string for {field['name']}",
        "request": request,
        "expected_http_status": 400,
        "expected_result": "FAILED response because blank text violates non-blank validation.",
        "coverage_tags": ["EP"]
    }


def invalid_enum_case(endpoint, field, location, case_id):
    request = build_valid_request(endpoint)
    target_key = "body" if location == "body" else "query"
    request.setdefault(target_key, {})[field["name"]] = invalid_enum_value(field)
    return {
        "id": case_id,
        "title": f"Invalid enum for {field['name']}",
        "request": request,
        "expected_http_status": 400,
        "expected_result": "FAILED response because enum value is outside the accepted domain.",
        "coverage_tags": ["EP"]
    }


def positive_boundary_cases(endpoint, field, case_prefix):
    base = build_valid_request(endpoint)
    cases = []
    for suffix, value, status, result in [
        ("A", 0, 400, "FAILED response because value must be positive."),
        ("B", -1, 400, "FAILED response because value must be positive."),
        ("C", 1, 200, "SUCCESS response because value is the smallest valid positive boundary.")
    ]:
        request = deepcopy(base)
        request.setdefault("body", {})[field["name"]] = value
        cases.append({
            "id": f"{case_prefix}{suffix}",
            "title": f"Positive boundary for {field['name']} = {value}",
            "request": request,
            "expected_http_status": status,
            "expected_result": result,
            "coverage_tags": ["BVA"]
        })
    return cases


def range_boundary_cases(endpoint, field, location, case_prefix):
    base = build_valid_request(endpoint)
    cases = []
    target_key = "query" if location == "query" else "body"
    min_value = field.get("min_effective")
    max_value = field.get("max_effective")

    if min_value is not None:
        for suffix, value, result in [
            ("A", min_value - 1, "SUCCESS response with normalized lower bound behavior."),
            ("B", min_value, "SUCCESS response at lower boundary."),
            ("C", min_value + 1, "SUCCESS response just above lower boundary.")
        ]:
            request = deepcopy(base)
            request.setdefault(target_key, {})[field["name"]] = value
            cases.append({
                "id": f"{case_prefix}{suffix}",
                "title": f"Lower boundary for {field['name']} = {value}",
                "request": request,
                "expected_http_status": 200,
                "expected_result": result,
                "coverage_tags": ["BVA"]
            })

    if max_value is not None:
        for suffix, value, result in [
            ("D", max_value - 1, "SUCCESS response just below upper boundary."),
            ("E", max_value, "SUCCESS response at upper boundary."),
            ("F", max_value + 1, "SUCCESS response with normalized upper bound behavior.")
        ]:
            request = deepcopy(base)
            request.setdefault(target_key, {})[field["name"]] = value
            cases.append({
                "id": f"{case_prefix}{suffix}",
                "title": f"Upper boundary for {field['name']} = {value}",
                "request": request,
                "expected_http_status": 200,
                "expected_result": result,
                "coverage_tags": ["BVA"]
            })

    return cases


def default_omission_case(endpoint, field, case_id):
    request = build_valid_request(endpoint)
    if field["name"] in request.get("query", {}):
        request["query"].pop(field["name"], None)
        if not request["query"]:
            request.pop("query", None)
    if field["name"] in request.get("body", {}):
        request["body"].pop(field["name"], None)
        if not request["body"]:
            request.pop("body", None)
    return {
        "id": case_id,
        "title": f"Omit optional field {field['name']} for default handling",
        "request": request,
        "expected_http_status": 200,
        "expected_result": f"SUCCESS response and server applies default value for {field['name']}.",
        "coverage_tags": ["EP", "DT"]
    }


def missing_resource_case(endpoint, field, case_id):
    request = build_valid_request(endpoint)
    request.setdefault("path_params", {})[field["name"]] = field.get("sample_missing_resource", 999999)
    return {
        "id": case_id,
        "title": f"Missing resource id for {field['name']}",
        "request": request,
        "expected_http_status": 404,
        "expected_result": "FAILED response because the resource does not exist.",
        "coverage_tags": ["EP", "DT"]
    }


def valid_baseline_case(endpoint, case_id):
    return {
        "id": case_id,
        "title": "Baseline valid request",
        "request": build_valid_request(endpoint),
        "expected_http_status": 200,
        "expected_result": "SUCCESS response with valid business payload.",
        "coverage_tags": ["EP"]
    }


def concrete_test_cases(endpoint):
    cases = [valid_baseline_case(endpoint, f"{endpoint['id']}-TC01")]
    case_index = 2
    groups = field_location_groups(endpoint)

    for location in ("body", "query"):
        for field in groups[location]:
            if field.get("required"):
                cases.append(missing_required_case(endpoint, field, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1
            if field.get("type") == "string" and field.get("non_blank"):
                cases.append(blank_string_case(endpoint, field, location, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1
            if field.get("type") == "enum":
                cases.append(invalid_enum_case(endpoint, field, location, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1
            if field.get("positive"):
                cases.extend(positive_boundary_cases(endpoint, field, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1
            if "default" in field and not field.get("required"):
                cases.append(default_omission_case(endpoint, field, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1
            if "min_effective" in field or "max_effective" in field:
                cases.extend(range_boundary_cases(endpoint, field, location, f"{endpoint['id']}-TC{case_index:02d}"))
                case_index += 1

    for field in groups["path"]:
        cases.append(missing_resource_case(endpoint, field, f"{endpoint['id']}-TC{case_index:02d}"))
        case_index += 1

    return cases


def generate(spec):
    endpoints_output = []
    total_cases = 0
    total_partitions = 0
    total_boundaries = 0
    total_decisions = 0

    for endpoint in spec["endpoints"]:
        partitions = []
        boundaries = []

        for location, fields in field_location_groups(endpoint).items():
            for field in fields:
                partitions.extend(partitions_for_field(field, location))
                boundaries.extend(boundaries_for_field(field, location))

        decisions = decision_rows(endpoint)
        cases = concrete_test_cases(endpoint)

        total_cases += len(cases)
        total_partitions += len(partitions)
        total_boundaries += len(boundaries)
        total_decisions += len(decisions)

        endpoints_output.append({
            "id": endpoint["id"],
            "name": endpoint["name"],
            "method": endpoint["method"],
            "path": endpoint["path"],
            "input_variables": input_variables(endpoint),
            "equivalence_partitions": partitions,
            "boundary_values": boundaries,
            "decision_table": decisions,
            "test_cases": cases
        })

    return {
        "project": spec["project"],
        "model_artifact": spec["model_artifact"],
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "summary": {
            "endpoint_count": len(spec["endpoints"]),
            "equivalence_partition_count": total_partitions,
            "boundary_set_count": total_boundaries,
            "decision_row_count": total_decisions,
            "test_case_count": total_cases
        },
        "endpoints": endpoints_output
    }


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def write_markdown(path, generated):
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = []
    lines.append(f"# {generated['project']['title']}")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    for key, value in generated["summary"].items():
        lines.append(f"- {key.replace('_', ' ').title()}: {value}")
    lines.append("")

    for endpoint in generated["endpoints"]:
        lines.append(f"## {endpoint['id']} {endpoint['method']} {endpoint['path']}")
        lines.append("")
        lines.append(f"- Input variables: {len(endpoint['input_variables'])}")
        lines.append(f"- Equivalence partitions: {len(endpoint['equivalence_partitions'])}")
        lines.append(f"- Boundary sets: {len(endpoint['boundary_values'])}")
        lines.append(f"- Decision rows: {len(endpoint['decision_table'])}")
        lines.append(f"- Concrete testcases: {len(endpoint['test_cases'])}")
        lines.append("")
        lines.append("### Sample Testcases")
        lines.append("")
        for case in endpoint["test_cases"][:4]:
            lines.append(f"- `{case['id']}` {case['title']} -> HTTP {case['expected_http_status']}")
        lines.append("")

    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def main():
    parser = argparse.ArgumentParser(description="Generate black-box test artifacts from a structured spec.")
    parser.add_argument("--spec", required=True, help="Path to the input spec JSON.")
    parser.add_argument("--json-out", required=True, help="Path to the generated JSON output.")
    parser.add_argument("--markdown-out", required=True, help="Path to the generated Markdown summary.")
    args = parser.parse_args()

    spec = load_spec(args.spec)
    generated = generate(spec)
    write_json(Path(args.json_out), generated)
    write_markdown(Path(args.markdown_out), generated)


if __name__ == "__main__":
    main()
