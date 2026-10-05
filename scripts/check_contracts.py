"""Validate proposed OpenAPI contracts, examples and request/response invariants."""

from __future__ import annotations

import json
import math
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker
from jsonschema.exceptions import ValidationError
from openapi_spec_validator import validate
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT202012

ROOT = Path(__file__).resolve().parents[1]
HTTP_METHODS = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}


class UniqueKeyLoader(yaml.SafeLoader):
    """Duplicate YAML keys must fail instead of silently replacing a contract."""


def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError(f"Duplicate YAML key: {key}")
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


UniqueKeyLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)


def finite_numbers(value):
    if isinstance(value, float) and not math.isfinite(value):
        raise ValueError("NaN/Infinity cannot cross the JSON API boundary")
    if isinstance(value, dict):
        for item in value.values():
            finite_numbers(item)
    elif isinstance(value, list):
        for item in value:
            finite_numbers(item)


def load_document(path):
    document = yaml.load(Path(path).read_text(encoding="utf-8"), Loader=UniqueKeyLoader)
    finite_numbers(document)
    return document


def local_refs(document):
    def walk(value):
        if isinstance(value, dict):
            if "$ref" in value:
                ref = value["$ref"]
                if not isinstance(ref, str) or not ref.startswith("#/"):
                    raise ValueError("Only local JSON Pointer $refs are allowed")
                target = document
                for part in ref[2:].split("/"):
                    target = target[part.replace("~1", "/").replace("~0", "~")]
            for item in value.values():
                walk(item)
        elif isinstance(value, list):
            for item in value:
                walk(item)

    walk(document)


def validator(document, schema):
    uri = "urn:lawground:contract"
    registry = Registry().with_resource(
        uri, Resource.from_contents(document, default_specification=DRAFT202012)
    )
    def absolute_refs(value):
        if isinstance(value, dict):
            return {key: uri + item if key == "$ref" else absolute_refs(item)
                    for key, item in value.items()}
        if isinstance(value, list):
            return [absolute_refs(item) for item in value]
        return value
    return Draft202012Validator(absolute_refs(schema), registry=registry,
                                format_checker=FormatChecker())


def validate_instance(document, schema_name, value):
    finite_numbers(value)
    uri = "urn:lawground:contract"
    registry = Registry().with_resource(
        uri, Resource.from_contents(document, default_specification=DRAFT202012)
    )
    schema = {"$ref": uri + "#/components/schemas/" + schema_name}
    Draft202012Validator(schema, registry=registry, format_checker=FormatChecker()).validate(value)


def validate_document(document):
    local_refs(document)
    validate(document)
    count = 0
    for name, schema in document["components"]["schemas"].items():
        Draft202012Validator.check_schema(schema)
    # Inline request/response/parameter examples are validated, including error bodies.
    def walk(value):
        nonlocal count
        if isinstance(value, dict):
            if "schema" in value and "example" in value:
                schema = value["schema"]
                validator(document, schema).validate(value["example"])
                count += 1
            for item in value.values():
                walk(item)
        elif isinstance(value, list):
            for item in value:
                walk(item)
    walk(document)
    return count


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate_roundtrip(document, operation, request, response):
    prefix = {"embed": "Embed", "tokenize": "Tokenize", "rerank": "Rerank", "verify": "Verify"}[operation]
    validate_instance(document, prefix + "Input", request)
    validate_instance(document, prefix + "Output", response)
    if operation in {"embed", "tokenize"}:
        expected = [item["id"] for item in request["texts"]]
        actual = [item["id"] for item in response["items"]]
        require(len(set(expected)) == len(expected), "Request text IDs must be unique")
        require(actual == expected, "Batch response IDs/order must match the request")
        version = "modelRevision" if operation == "embed" else "tokenizerVersion"
        require(request[version] == response[version], "Requested version was not used")
        if operation == "embed":
            for item in response["items"]:
                require(any(value != 0 for value in item["vector"]), "Zero vector has no cosine direction")
    elif operation == "rerank":
        expected = [item["chunkId"] for item in request["candidates"]]
        actual = [item["chunkId"] for item in response["ranked"]]
        require(len(set(expected)) == len(expected), "Candidate chunk IDs must be unique")
        require(len(set(actual)) == len(actual), "Ranked chunk IDs must be unique")
        require(set(actual) <= set(expected), "Rerank returned an unknown candidate")
        require(len(actual) <= request["topK"], "Rerank exceeded topK")
        scores = [item["score"] for item in response["ranked"]]
        require(scores == sorted(scores, reverse=True), "Ranked scores must be descending")
    else:
        require(request["referenceDate"] == request["questionSnapshot"]["referenceDate"], "Reference dates differ")
        candidates = {item["chunkId"]: item for item in request["candidates"]}
        require(len(candidates) == len(request["candidates"]), "Candidate chunk IDs must be unique")
        keys = set()
        for evidence in response["evidences"]:
            require(evidence["chunkId"] in candidates, "Verify returned an unknown chunk")
            source = candidates[evidence["chunkId"]]["sourceText"]
            start, end = evidence["start"], evidence["end"]
            require(0 <= start < end <= len(source), "Invalid code point offset range")
            require(source[start:end] == evidence["quote"], "Quote must match source[start:end] exactly")
            require(evidence["evidenceKey"] not in keys, "Evidence keys must be unique")
            keys.add(evidence["evidenceKey"])
        choices = response["choiceAnalyses"]
        if choices is not None:
            numbers = [choice["choiceNumber"] for choice in choices]
            require(set(numbers) == {1, 2, 3, 4, 5}, "Verify must return each choice number once")
            for choice in choices:
                require(set(choice["evidenceKeys"]) <= keys, "Choice references unknown evidence")
        if response["verdict"] == "SUPPORTED":
            require(bool(keys), "SUPPORTED requires real source evidence")


def check(root=ROOT):
    api = Path(root) / "docs/api"
    documents = {name: load_document(api / f"{name}.openapi.yaml") for name in ("external", "internal")}
    examples = sum(validate_document(doc) for doc in documents.values())
    fixtures = json.loads((api / "examples/contract-cases.json").read_text(encoding="utf-8"))
    count = 0
    for group in ("cases", "roundTrips"):
        for case in fixtures[group]:
            try:
                if group == "cases":
                    validate_instance(documents[case["contract"]], case["schema"], case["value"])
                else:
                    validate_roundtrip(documents["internal"], case["operation"], case["request"], case["response"])
                actual = True
            except (ValueError, ValidationError):
                actual = False
            require(actual == case["valid"], f"Unexpected validation outcome: {case['name']}")
            count += 1
    operations = sum(sum(method in HTTP_METHODS for method in path) for doc in documents.values() for path in doc["paths"].values())
    print(f"PASS: 2 OpenAPI contracts, {operations} operations, {examples} examples, {count} positive/negative cases")


if __name__ == "__main__":
    try:
        check()
    except Exception as error:
        # Print schema diagnostics, never credentials or real user inputs.
        print(f"FAIL: {type(error).__name__}: {error}")
        raise SystemExit(1)
