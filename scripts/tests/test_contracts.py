"""Regression checks for contract validation, not AI quality evaluation."""

from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_contracts as contracts


class ContractValidationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        api = contracts.ROOT / "docs/api"
        cls.external = contracts.load_document(api / "external.openapi.yaml")
        cls.internal = contracts.load_document(api / "internal.openapi.yaml")
        cls.fixtures = json.loads((api / "examples/contract-cases.json").read_text(encoding="utf-8"))

    def roundtrip(self, name):
        return deepcopy(next(case for case in self.fixtures["roundTrips"] if case["name"] == name))

    def test_all_contracts_examples_and_cases(self):
        contracts.check()

    def test_duplicate_yaml_key_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "duplicate.yaml"
            path.write_text("key: first\nkey: second\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                contracts.load_document(path)

    def test_missing_reference_is_rejected(self):
        document = deepcopy(self.external)
        document["components"]["schemas"]["QuestionInput"]["properties"]["choices"]["items"] = {"$ref": "#/components/schemas/Typo"}
        with self.assertRaises(KeyError):
            contracts.validate_document(document)

    def test_remote_reference_is_rejected_without_fetch(self):
        document = deepcopy(self.external)
        document["components"]["schemas"]["Choice"] = {"$ref": "https://example.invalid/schema"}
        with self.assertRaisesRegex(ValueError, "Only local"):
            contracts.validate_document(document)

    def test_path_parameter_is_required(self):
        document = deepcopy(self.external)
        document["paths"]["/questions/{id}"]["get"]["parameters"] = []
        with self.assertRaises(Exception):
            contracts.validate_document(document)

    def test_nonfinite_embedding_is_rejected(self):
        case = self.roundtrip("valid-embed")
        case["response"]["items"][0]["vector"][0] = float("nan")
        with self.assertRaisesRegex(ValueError, "NaN/Infinity"):
            contracts.validate_roundtrip(self.internal, "embed", case["request"], case["response"])

    def test_batch_order_is_preserved(self):
        case = self.roundtrip("valid-embed")
        second = "00000000-0000-4000-8000-000000000019"
        case["request"]["texts"].append({"id": second, "text": "두 번째 합성 입력"})
        case["response"]["items"].insert(0, {"id": second, "vector": [1.0] + [0.0] * 1023})
        with self.assertRaisesRegex(ValueError, "IDs/order"):
            contracts.validate_roundtrip(self.internal, "embed", case["request"], case["response"])

    def test_version_mismatch_is_rejected(self):
        case = self.roundtrip("valid-tokenize")
        case["response"]["tokenizerVersion"] = "different-fixture-version"
        with self.assertRaisesRegex(ValueError, "version"):
            contracts.validate_roundtrip(self.internal, "tokenize", case["request"], case["response"])

    def test_supported_requires_real_evidence(self):
        case = self.roundtrip("valid-verify")
        case["response"].update(verdict="SUPPORTED", evidences=[])
        with self.assertRaisesRegex(ValueError, "requires real"):
            contracts.validate_roundtrip(self.internal, "verify", case["request"], case["response"])

    def test_reference_date_mismatch_is_rejected(self):
        case = self.roundtrip("valid-verify")
        case["request"]["referenceDate"] = "2024-01-01"
        with self.assertRaisesRegex(ValueError, "Reference dates"):
            contracts.validate_roundtrip(self.internal, "verify", case["request"], case["response"])

    def test_utf16_offset_and_fabricated_quote_are_rejected(self):
        for name in ("utf16-offset-not-codepoint", "fabricated-quote"):
            case = self.roundtrip(name)
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "Quote"):
                contracts.validate_roundtrip(self.internal, "verify", case["request"], case["response"])


if __name__ == "__main__":
    unittest.main()
