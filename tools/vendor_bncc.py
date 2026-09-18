#!/usr/bin/env python3
"""Vendor a pinned, attributed BNCC snapshot as Android offline assets.

Run explicitly with: python3 tools/vendor_bncc.py
Requires network only while updating the repository, NEVER at app runtime. No API key.
Source: bncc-dev/bncc-dados, dados-2026.07.1, dataset CC BY 4.0.
All upstream file Git blob hashes and record counts must match or the import fails.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import re
import urllib.request
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/bncc/catalog.json"
REVISION = "daabd7dd63ae0cac0aa520b6189e79f95c24f583"
BASE = f"https://raw.githubusercontent.com/bncc-dev/bncc-dados/{REVISION}/"
FILES = {
    "infantil": ("dados/bncc-2018/educacao-infantil.json", "587766665dec60255dc3fa567b76fcc9247bb981"),
    "fundamental": ("dados/bncc-2018/ensino-fundamental.json", "3c50a51158d74177c62ce1d16afa61b2a0874c89"),
    "medio": ("dados/bncc-2018/ensino-medio.json", "4a84eb781828e15aa222ba3db4bf61382fafa0e8"),
    "computacao": ("dados/computacao-2022/computacao.json", "0818dd280b956ea26da15bcf7a5bc5e6c938594e"),
    "estrutura": ("dados/bncc-2018/estrutura.json", "f44cbd7fa4a9bc082c60bf0fc28ee929e012a5a4"),
}


def fetch_verified(path: str, expected_git_blob_sha: str) -> dict:
    with urllib.request.urlopen(BASE + path, timeout=45) as response:
        data = response.read(5_000_001)
    if len(data) > 5_000_000:
        raise ValueError(f"Unexpected oversized BNCC source: {path}")
    git_sha = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    if git_sha != expected_git_blob_sha:
        raise ValueError(f"Upstream content mismatch for {path}: {git_sha}")
    return json.loads(data.decode("utf-8"))


def named_entities(node: object) -> dict[str, str]:
    """Build human-readable names from the source taxonomy; never fabricate names."""
    found: dict[str, str] = {}
    if isinstance(node, dict):
        if isinstance(node.get("id"), str) and isinstance(node.get("nome"), str):
            found[node["id"]] = node["nome"]
        for child in node.values():
            found.update(named_entities(child))
    elif isinstance(node, list):
        for child in node:
            found.update(named_entities(child))
    return found


def normalized_record(raw: dict, complementary: bool, taxonomy: dict[str, str]) -> dict:
    code, text = raw["codigo"], raw["texto"]
    if not isinstance(code, str) or not re.fullmatch(r"(?:EI|EF|EM)[A-Z0-9]{6,14}", code):
        raise ValueError(f"Malformed BNCC code: {code!r}")
    if not isinstance(text, str) or len(text.strip()) < 10:
        raise ValueError(f"Missing text for BNCC code: {code}")
    source = raw.get("fonte", {})
    if not isinstance(source, dict) or not source.get("arquivo"):
        raise ValueError(f"Missing source provenance for BNCC code: {code}")
    stage = code[:2]
    component = raw.get("componente") or raw.get("campo_experiencias") or raw.get("area")
    if complementary:
        component = "Computação"
    if isinstance(component, dict):
        component = component.get("nome") or component.get("id")
    if isinstance(component, str):
        component = taxonomy.get(component, component)
    else:
        component = ""
    years = raw.get("anos", [])
    if not isinstance(years, list):
        years = []
    return {
        "codigo": code,
        "texto": text.strip(),
        "etapa": stage,
        "componente": component,
        "anos": years,
        "grupo_etario": taxonomy.get(raw.get("grupo_etario", ""), ""),
        "complemento": complementary,
        "documento": source.get("documento", raw.get("documento", "")),
        "arquivo_fonte": source["arquivo"],
        "localizador_pdf": source.get("localizador_pdf", ""),
        "vigencia": raw.get("vigencia", {}).get("status", ""),
    }


def build_catalog(datasets: dict[str, dict]) -> dict:
    taxonomy = named_entities(datasets["estrutura"])
    groups = [
        (datasets["infantil"]["objetivos"], False),
        (datasets["fundamental"]["habilidades"], False),
        (datasets["medio"]["habilidades"], False),
        (datasets["computacao"]["objetivos_ei"], True),
        (datasets["computacao"]["habilidades_ef"], True),
        (datasets["computacao"]["habilidades_em"], True),
    ]
    rows = [normalized_record(item, complementary, taxonomy) for items, complementary in groups for item in items]
    counts = Counter("computacao" if row["complemento"] else row["etapa"] for row in rows)
    if len(rows) != 1721 or counts["computacao"] != 141 or len({row["codigo"] for row in rows}) != 1721:
        raise ValueError(f"Incomplete or duplicate BNCC snapshot: {len(rows)} rows; counts={dict(counts)}")
    rows.sort(key=lambda item: item["codigo"])
    return {
        "versao": "dados-2026.07.1",
        "revisao": REVISION,
        "licenca_dados": "CC BY 4.0",
        "atribuicao": "bncc.dev (mantido pela Profy) — https://github.com/bncc-dev/bncc-dados",
        "aviso": "Dataset independente, não oficial do MEC; textos rastreados aos documentos oficiais.",
        "quantidade": len(rows),
        "aprendizagens": rows,
    }


def main() -> None:
    datasets = {key: fetch_verified(*source) for key, source in FILES.items()}
    catalog = build_catalog(datasets)
    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"BNCC offline: {catalog['quantidade']} registros verificados -> {DEST.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
