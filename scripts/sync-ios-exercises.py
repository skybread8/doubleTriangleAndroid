#!/usr/bin/env python3
"""Add missing exercise resources from the generated iOS catalog, without overwriting local edits."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IOS = ROOT.parent / "doubleTriangle" / "Packages/WildforceShared/Sources/WildforceShared"
RES = ROOT / "feature/workout/src/main/res"
LOCALES = {"en": "en", "es": "es", "ca": "ca", "de": "de", "fr": "fr", "it": "it", "ja": "ja", "nl": "nl", "pt": "pt", "zh-rCN": "zh-Hans"}
FIELDS = {"englishName": "name", "englishAliases": "aliases", "category": "category", "requiredEquipment": "requiredEquipment", "compatibleGoals": "compatibleGoals", "movementPatterns": "movementPatterns", "mechanic": "mechanic", "difficulty": "difficulty", "isUnilateral": "isUnilateral", "isPerSideLoad": "isPerSideLoad", "contraindicatedRestrictions": "contraindicatedRestrictions", "substitutionCandidates": "substitutionCandidates", "userDescription": "description", "instructions": "instructions", "tips": "tips", "primaryMuscles": "primaryMuscles", "secondaryMuscles": "secondaryMuscles", "supportsExternalLoad": "supportsExternalLoad", "trackingMode": "trackingMode", "targetMetrics": "targetMetrics", "metValue": "metValue", "notesForLLM": "notesForLlm"}


def swift_value(value):
    value = value.strip().removesuffix(",")
    if value.startswith("Set(") and value.endswith(")"):
        value = value[4:-1]
    if value == "nil": return None
    if value in ("true", "false"): return value == "true"
    if value.startswith("."): return value[1:]
    if value.startswith("["):
        if re.fullmatch(r"\[(?:\s*\.\w+\s*,?)*\]", value):
            return re.findall(r"\.(\w+)", value)
        return json.loads(value)
    return json.loads(value)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ios", type=Path, default=IOS, help="WildforceShared/Sources/WildforceShared directory")
    parser.add_argument("--check", action="store_true", help="Report missing resources without writing")
    args = parser.parse_args()
    source = (args.ios / "Models/Enums/Excercices/ExerciseCatalog.swift").read_text(encoding="utf-8")
    blocks = re.findall(r"^\s*\.(\w+): ExerciseMetadata\(\s*\n(.*?)^\s*\),?\s*$", source, re.M | re.S)
    enum = (args.ios / "Models/Enums/Excercices/Exercise.swift").read_text(encoding="utf-8")
    cases = re.findall(r"^\s*case (\w+)\s*$", enum, re.M)
    if not blocks or len(blocks) != len(cases) or {x[0] for x in blocks} != set(cases):
        raise ValueError("iOS exercise cases and parsed definitions do not match; no files written")
    entries = []
    for ident, block in blocks:
        values = dict(re.findall(r"^\s*(\w+): (.*)$", block, re.M))
        if set(values) != set(FIELDS) | {"exercise"} or swift_value(values["exercise"]) != ident:
            raise ValueError(f"Unexpected iOS fields for {ident}; no files written")
        entry = {"id": ident, **{dest: swift_value(values[src]) for src, dest in FIELDS.items()}}
        entry["assets"] = {"verticalImageKey": ident + "_{gender}.jpeg", "tutorialImageKey": ident + "_{gender}.png"}
        entries.append(entry)
    translations = json.loads((args.ios / "Resources/Localizable.xcstrings").read_text(encoding="utf-8"))["strings"]
    def translate(text, locale):
        value = translations.get(text, {}).get("localizations", {}).get(locale, {}).get("stringUnit", {}).get("value", "")
        return value if value and "\ufffd" not in value else text
    files = {}
    def load(path):
        if path not in files: files[path] = json.loads(path.read_text(encoding="utf-8"))
        return files[path]
    changed = set()
    def add(path, key, value):
        data = load(path)
        if key not in data:
            data[key] = value
            changed.add(path)
    catalog_path = RES / "raw/exercise_catalog.json"
    catalog = load(catalog_path)
    existing = {item["id"] for item in catalog["exercises"]}
    for e in entries:
        ident = e["id"]
        if ident not in existing:
            catalog["exercises"].append(e)
            changed.add(catalog_path)
        names = {}
        for locale in LOCALES.values():
            localized = [translations.get(text, {}).get("localizations", {}).get(locale, {}).get("stringUnit", {}).get("value", "") for text in [e["name"], *e["aliases"]]]
            names[locale] = [x for x in localized if x and "\ufffd" not in x]
        add(RES / "raw/ios_exercise_metadata.json", ident, {"name": e["name"], "aliases": e["aliases"], "translations": names, "mechanic": e["mechanic"], "primaryMuscles": e["primaryMuscles"], "substitutionCandidates": e["substitutionCandidates"], "isPerSideLoad": e["isPerSideLoad"]})
        add(RES / "raw/ios_exercise_editor_metadata.json", ident.lower(), {"mode": e["trackingMode"], "weight": e["supportsExternalLoad"]})
        for directory, locale in [("raw", "en"), *[("raw-" + k, v) for k, v in LOCALES.items()]]:
            guide = {"description": translate(e["description"], locale), "instructions": [translate(x, locale) for x in e["instructions"]], "tips": [translate(x, locale) for x in e["tips"]]}
            add(RES / directory / "exercise_guide.json", ident, guide)
    if catalog_path in changed: catalog["metadata"]["exerciseCount"] = len(catalog["exercises"])
    for path in sorted(changed):
        print(("Would update " if args.check else "Updating ") + str(path.relative_to(ROOT)))
    if args.check:
        return bool(changed)
    for path in changed:
        compact = path.name in ("ios_exercise_metadata.json", "ios_exercise_editor_metadata.json")
        path.write_text(json.dumps(files[path], ensure_ascii=False, indent=None if compact else 2, separators=(",", ":") if compact else None) + "\n", encoding="utf-8")
    return False


if __name__ == "__main__":
    raise SystemExit(main())
