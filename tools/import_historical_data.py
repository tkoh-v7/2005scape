"""Build small, local runtime databases from OSRSBox facts; never run at login."""
import hashlib
import json
from collections import Counter
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources"
CUTOFF = date(2005, 6, 22)


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(name, value):
    (OUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def generate():
    items_path = ROOT / "items-source.json"
    monsters_path = ROOT / ".reference-cache/monsters.json"
    items = load(items_path)
    monsters = load(monsters_path)
    overrides = {r["id"]: r for r in load(OUT / "historical-overrides.json")}
    records = []
    for value in sorted(items.values(), key=lambda x: x["id"]):
        record = dict(id=value["id"], name=value["name"], released=value.get("release_date"),
                      source=value.get("wiki_url") or "https://github.com/osrsbox/osrsbox-db",
                      placeholder=bool(value.get("placeholder")),
                      noted=bool(value.get("noted")), baseId=value.get("linked_id_item"),
                      highAlch=value.get("highalch") or 0,
                      equipment=value.get("equipment"), snapshot=value.get("last_updated"))
        if record["released"]:
            date.fromisoformat(record["released"])
        # Upstream duplicate variants may inherit a parent page's date (e.g. LMS).
        # A duplicate's original availability needs separate evidence. Notes are
        # resolved to their base by the client; placeholders can never be used.
        if value.get("duplicate") and not record["noted"] and not record["placeholder"] and record["released"] and date.fromisoformat(record["released"]) <= CUTOFF:
            record["upstreamReleased"] = record["released"]
            record["released"] = None
            record["uncertainty"] = "Duplicate variant inherits a parent release date; separate audit required."
        if record["id"] in overrides:
            record.update(overrides[record["id"]])
        records.append(record)
    ids = {r["id"] for r in records}
    records.extend(v for k, v in overrides.items() if k not in ids)
    by_id = {r["id"]: r for r in records}
    for replacement in load(OUT / "historical-replacements.json"):
        original = by_id[replacement["replacementOf"]]
        if not original.get("released") or date.fromisoformat(original["released"]) > CUTOFF or original.get("placeholder"):
            raise ValueError("Replacement must refer directly to a dated historical item")
        by_id[replacement["id"]].update(replacement)
    def eligible(record):
        original = by_id[record["replacementOf"]] if record.get("replacementOf") is not None else record
        return bool(original.get("released")) and date.fromisoformat(original["released"]) <= CUTOFF and not record.get("placeholder")
    write("historical-items.json", sorted(records, key=lambda x: x["id"]))
    npcs = [dict(id=v["id"], name=v["name"], released=v.get("release_date"),
                 source=v.get("wiki_url") or "https://github.com/osrsbox/osrsbox-db",
                 categories=v.get("category") or [], slayerLevel=v.get("slayer_level") or 0)
            for v in sorted(monsters.values(), key=lambda x: x["id"])]
    write("historical-npcs.json", npcs)

    from generate_goals import generate as generate_goals
    generate_goals()
    report = dict(cutoff=str(CUTOFF), upstream="https://github.com/osrsbox/osrsbox-db",
                  upstreamSnapshot="2021-08-05 (items; per-record dates retained)",
                  importedOn="2026-10-09", itemRecords=len(records), npcRecords=len(npcs),
                  itemsDated=sum(bool(r.get("released")) for r in records),
                  itemsEligible=sum(eligible(r) for r in records),
                  acceptedReplacementIds=sum(r.get("replacementOf") is not None for r in records),
                  missingItemDates=sum(not r.get("released") for r in records),
                  quarantinedDuplicateVariants=sum(bool(r.get("uncertainty")) for r in records),
                  sha256={"items-source.json": hashlib.sha256(items_path.read_bytes()).hexdigest(),
                          "monsters-source.json": hashlib.sha256(monsters_path.read_bytes()).hexdigest()})
    write("data-provenance.json", report)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    generate()
