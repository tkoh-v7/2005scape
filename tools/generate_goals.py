"""Generate the runtime checklist from Kirk's editable goal definitions."""
import json
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
CUTOFF = date(2005, 6, 22)


def generate():
    items = {r["id"]: r for r in json.loads((RES / "historical-items.json").read_text(encoding="utf-8"))}
    definitions = json.loads((RES / "goal-definitions.json").read_text(encoding="utf-8"))
    def eligible(row):
        original = items.get(row.get("replacementOf", row["id"]))
        return original is not None and original.get("released") is not None and date.fromisoformat(original["released"]) <= CUTOFF and not row.get("placeholder") and not row.get("noted")
    by_name = {}
    for row in items.values():
        if eligible(row):
            by_name.setdefault(row["name"].casefold(), []).append(row["id"])
    goals, keys = [], set()
    for definition in definitions:
        for field in ("key", "name", "group"):
            if not isinstance(definition.get(field), str) or not definition[field].strip():
                raise ValueError(f"Each goal needs a non-empty {field}: {definition}")
        if definition["key"] in keys:
            raise ValueError("Duplicate goal key: " + definition["key"])
        keys.add(definition["key"])
        names = definition.get("items", [])
        explicit_ids = definition.get("itemIds", [])
        if not isinstance(names, list) or not isinstance(explicit_ids, list):
            raise ValueError("items and itemIds must be lists: " + definition["key"])
        if any(type(value) is not int for value in explicit_ids):
            raise ValueError("itemIds must contain integer IDs: " + definition["key"])
        alternatives = set(explicit_ids)
        for name in names:
            if not isinstance(name, str) or not by_name.get(name.casefold()):
                raise ValueError(f"Goal {definition['key']}: no eligible item named {name!r}; check the audit page or use itemIds")
            alternatives.update(by_name[name.casefold()])
        if not alternatives:
            raise ValueError("Goal has no alternatives: " + definition["key"])
        for item_id in alternatives:
            if type(item_id) is not int or item_id not in items or not eligible(items[item_id]):
                raise ValueError(f"Goal {definition['key']}: ineligible/unverified item ID {item_id}")
        goals.append({"key": definition["key"], "name": definition["name"], "group": definition["group"],
                      "alternatives": sorted(alternatives), "notes": definition.get("notes", "")})
    if not goals:
        raise ValueError("Checklist must contain at least one goal")
    (RES / "historical-goals.json").write_text(json.dumps(goals, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(goals)} goals from goal-definitions.json")


if __name__ == "__main__":
    generate()
