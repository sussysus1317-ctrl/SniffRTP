#!/usr/bin/env python3
"""Create source archives and verified deterministic JAR reassemblies."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "binary-input"
DIST = ROOT / "dist"

PLATFORMS = {
    "bukkit": "2252-SniffRTP-0.0.1-Bukkit-26.2.jar",
    "fabric": "2252-SniffRTP-0.0.1-Fabric-Server-26.2.jar",
    "folia": "2252-SniffRTP-0.0.1-Folia-26.2.jar",
    "forge": "2252-SniffRTP-0.0.1-Forge-Server-26.2.jar",
    "neoforge": "2252-SniffRTP-0.0.1-NeoForge-Server-26.2.jar",
    "paper": "2252-SniffRTP-0.0.1-Paper-26.2.jar",
    "purpur": "2252-SniffRTP-0.0.1-Purpur-26.2.jar",
    "spigot": "2252-SniffRTP-0.0.1-Spigot-26.2.jar",
    "sponge": "2310-SniffRTP-0.0.1-Sponge-Server-1.21.11.jar",
}

EXPECTED_SHA256 = {
    "bukkit": "44a894573d8e55e8007ab74cfa980ea1f2eedb0aca30ccee761a8d998deae75a",
    "fabric": "d9d9a5ad162cf41555886d030a7297b9f21ce0a8d6119ffedf99e907bb6cbe78",
    "folia": "03d2e71ea9203d9c45d675705ec2ac77ed91031df5322165d52b973066ca7ca7",
    "forge": "2fc28de2e73f0611c361c75b7e844ea8e322dd7c4be8027d52d210dca7438b7f",
    "neoforge": "fb54aed7ba5d7e687bd11d06b43c77313ca56655b68d9251086a62c38ac30d84",
    "paper": "c46aecc1f4c810f3242e894cd0dbe981152a7d593d31e554ad2667edc7f388d8",
    "purpur": "cba07c5653df3369dcfef704d3072a9ac85c081abf6567aa5297da1bd80b703b",
    "spigot": "11749642f1679d33fe611610b6c5e1dd9af16b60370e93bbffc6f8267095416f",
    "sponge": "db9ff7bf1ebb1b28ffd29dbd859bb368655b30be82cb39dd9d8924277f982e0b",
}

FIXED_TIME = (1980, 1, 1, 0, 0, 0)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_entry(out: ZipFile, name: str, data: bytes) -> None:
    info = ZipInfo(name, FIXED_TIME)
    info.compress_type = ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    out.writestr(info, data)


def source_zip(platform: str) -> Path:
    source_root = ROOT / "platforms" / platform
    target = DIST / "source-zips" / f"SniffRTP-{platform}-recovered-source.zip"
    target.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(target, "w") as out:
        for path in sorted(p for p in source_root.rglob("*") if p.is_file()):
            write_entry(out, path.relative_to(source_root).as_posix(), path.read_bytes())
    return target


def combined_source_zip() -> Path:
    target = DIST / "SniffRTP-recovered-source.zip"
    target.parent.mkdir(parents=True, exist_ok=True)
    included = [ROOT / "README.md", ROOT / "analysis" / "DECOMPILATION.md"]
    included.extend(p for p in (ROOT / "platforms").rglob("*") if p.is_file())
    with ZipFile(target, "w") as out:
        for path in sorted(included):
            write_entry(out, path.relative_to(ROOT).as_posix(), path.read_bytes())
    return target


def reassemble(platform: str, source: Path) -> Path:
    target = DIST / "reassembled-jars" / source.name.replace(".jar", "-reassembled.jar")
    target.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(source) as original, ZipFile(target, "w") as out:
        for name in sorted(n for n in original.namelist() if not n.endswith("/")):
            write_entry(out, name, original.read(name))
    return target


def compare_entries(original: Path, rebuilt: Path) -> dict[str, object]:
    with ZipFile(original) as left, ZipFile(rebuilt) as right:
        left_names = sorted(n for n in left.namelist() if not n.endswith("/"))
        right_names = sorted(n for n in right.namelist() if not n.endswith("/"))
        mismatches = []
        for name in sorted(set(left_names) & set(right_names)):
            if left.read(name) != right.read(name):
                mismatches.append(name)
        return {
            "same_entry_names": left_names == right_names,
            "entry_count": len(left_names),
            "byte_mismatches": mismatches,
            "verified": left_names == right_names and not mismatches,
        }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    report: dict[str, object] = {}

    for platform, filename in PLATFORMS.items():
        original = INPUT / filename
        if not original.is_file():
            raise SystemExit(f"Missing binary input: {original}")
        actual = sha256(original)
        if actual != EXPECTED_SHA256[platform]:
            raise SystemExit(f"SHA-256 mismatch for {original.name}: {actual}")
        rebuilt = reassemble(platform, original)
        result = compare_entries(original, rebuilt)
        result["input_sha256"] = actual
        result["reassembled_sha256"] = sha256(rebuilt)
        report[platform] = result
        if not args.verify_only:
            source_zip(platform)

    if not args.verify_only:
        combined_source_zip()
    report_path = DIST / "verification-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    failed = [name for name, result in report.items() if not result["verified"]]
    print(f"Verified {len(report) - len(failed)}/{len(report)} reassembled JARs")
    print(f"Report: {report_path}")
    if not args.verify_only:
        print(f"Combined source: {DIST / 'SniffRTP-recovered-source.zip'}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
