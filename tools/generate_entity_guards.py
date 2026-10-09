"""Build conservative modern ID guards from explicitly scoped RuneLite namespaces.

Source files are downloaded separately into .reference-cache, never at runtime.
Namespaces describe the containing activity, not an invented release date.
"""
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "https://raw.githubusercontent.com/runelite/runelite/master/runelite-api/src/main/java/net/runelite/api/gameval/"
SCOPES = {
    "POH_": ("Construction", "Construction"),
    "HUNTING_": ("Hunter", "Hunter"),
    "FOSSIL_": ("Fossil Island", "Fossil_Island"),
    "LUNAR_": ("Lunar Diplomacy", "Lunar_Diplomacy"),
    "GODWARS_": ("God Wars Dungeon", "God_Wars_Dungeon"),
    "RAIDS_": ("Chambers of Xeric", "Chambers_of_Xeric"),
    "TOB_": ("Theatre of Blood", "Theatre_of_Blood"),
    "TOA_": ("Tombs of Amascut", "Tombs_of_Amascut"),
    "GAUNTLET_": ("The Gauntlet", "The_Gauntlet"),
    "GOTR_": ("Guardians of the Rift", "Guardians_of_the_Rift"),
    "SAILING_": ("Sailing", "Sailing"),
    "FORESTRY_": ("Forestry", "Forestry"),
    "WINTERTODT_": ("Wintertodt", "Wintertodt"),
    "TEMPOROSS_": ("Tempoross", "Tempoross"),
    "PEST_": ("Pest Control", "Pest_Control"),
    "BARBASSAULT_": ("Barbarian Assault", "Barbarian_Assault"),
    "MM2_": ("Monkey Madness II", "Monkey_Madness_II"),
    "DS2_": ("Dragon Slayer II", "Dragon_Slayer_II"),
}

records, sources = [], []
for filename, kind in (("ObjectID.java", "OBJECT"), ("ObjectID1.java", "OBJECT"), ("NpcID.java", "NPC")):
    path = ROOT / ".reference-cache" / filename
    content = path.read_text(encoding="utf-8")
    sources.append({"url": BASE + filename, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    for symbol, numeric in re.findall(r"public static final int ([A-Z0-9_]+) = (\d+);", content):
        scope = next((value for prefix, value in SCOPES.items() if symbol.startswith(prefix)), None)
        if scope:
            records.append({"kind": kind, "id": int(numeric), "symbol": symbol,
                            "content": scope[0], "source": "https://oldschool.runescape.wiki/w/" + scope[1]})
keys = [(row["kind"], row["id"]) for row in records]
if len(keys) != len(set(keys)):
    raise ValueError("Repeated guarded entity ID")
output = {"cutoff": "2005-06-22", "derivation": "Explicit post-cutoff content namespaces in RuneLite gameval; conservative partial coverage. Movement/exits remain available.",
          "symbolSources": sources, "namespaces": SCOPES, "records": records}
(ROOT / "src/main/resources/modern-entities.json").write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
print(f"Generated {len(records):,} modern entity guards")
