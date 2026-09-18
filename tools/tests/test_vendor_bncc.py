"""Synthetic local-only regression tests; no internet, no teacher data."""
import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "vendor_bncc.py"
SPEC = importlib.util.spec_from_file_location("vendor_bncc", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(module)


class VendorBnccTests(unittest.TestCase):
    def setUp(self):
        self.source = {
            "codigo": "EF05MA07",
            "texto": "Texto de fixture sintética; não representa habilidade oficial.",
            "componente": "ef-comp-ma",
            "anos": [5],
            "fonte": {"documento": "bncc-2018", "arquivo": "fixture.pdf", "localizador_pdf": "página 1"},
            "vigencia": {"status": "vigente"},
        }

    def test_normalization_preserves_traceability(self):
        item = module.normalized_record(self.source, False, {"ef-comp-ma": "Matemática"})
        self.assertEqual("EF05MA07", item["codigo"])
        self.assertEqual("Matemática", item["componente"])
        self.assertEqual([5], item["anos"])
        self.assertEqual("página 1", item["localizador_pdf"])
        self.assertFalse(item["complemento"])

    def test_computing_is_explicitly_identified_as_supplement(self):
        source = dict(self.source, codigo="EF05CO07")
        item = module.normalized_record(source, True, {})
        self.assertTrue(item["complemento"])
        self.assertEqual("Computação", item["componente"])

    def test_missing_source_fails_closed(self):
        source = dict(self.source, fonte={})
        with self.assertRaisesRegex(ValueError, "source provenance"):
            module.normalized_record(source, False, {})

    def test_incomplete_dataset_fails_closed(self):
        datasets = {
            "estrutura": {},
            "infantil": {"objetivos": []},
            "fundamental": {"habilidades": [self.source]},
            "medio": {"habilidades": []},
            "computacao": {"objetivos_ei": [], "habilidades_ef": [], "habilidades_em": []},
        }
        with self.assertRaisesRegex(ValueError, "Incomplete"):
            module.build_catalog(datasets)


if __name__ == "__main__":
    unittest.main()
