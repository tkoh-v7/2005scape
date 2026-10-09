"""Validate the bundled historical data before publishing a build."""
import json
from datetime import date
from pathlib import Path
from urllib.parse import urlparse

root = Path(__file__).resolve().parents[1]
resources = root / "src/main/resources"
cutoff = date(2005, 6, 22)

def require(condition, message):
    if not condition:
        raise ValueError(message)

def read(name):
    return json.loads((resources / name).read_text(encoding="utf-8"))

def eligible(row):
    if row.get("replacementOf") is not None:
        original = items.get(row["replacementOf"])
        require(original is not None and original.get("replacementOf") is None, "Replacement must reference original directly")
        require(bool(row.get("replacementEvidence")) and bool(row.get("replacementReason")), "Replacement evidence missing")
        return eligible(original) and not row.get("placeholder")
    return bool(row.get("released")) and date.fromisoformat(row["released"]) <= cutoff and not row.get("placeholder")

def records(name):
    rows = read(name)
    require(isinstance(rows, list) and bool(rows), f"Empty {name}")
    seen = {}
    for row in rows:
        item_id = row["id"]
        require(type(item_id) is int and item_id >= 0, f"Invalid ID in {name}")
        require(item_id not in seen, f"Duplicate {name} ID {item_id}")
        require(bool(row["name"].strip()), f"Empty name {item_id}")
        source = urlparse(row["source"])
        require(source.scheme == "https" and bool(source.netloc), f"Missing evidence {item_id}")
        if row.get("released"):
            date.fromisoformat(row["released"])
        seen[item_id] = row
    return seen

items = records("historical-items.json")
replacements = read("historical-replacements.json")
require(len({r["id"] for r in replacements}) == len(replacements), "Duplicate replacement ID")
for replacement in replacements:
    require(all(items[replacement["id"]].get(k) == v for k, v in replacement.items()), "Replacement not imported")
    require(eligible(items[replacement["id"]]), "Invalid replacement")
require(eligible(items[23983]) and items[23983]["released"] == "2019-07-25", "Replacement must preserve its actual date")
require(not eligible(items[23971]), "Modern crystal armour must remain restricted")
npcs = records("historical-npcs.json")
goals = read("historical-goals.json")
keys = set()
for goal in goals:
    require(goal["key"] not in keys, "Duplicate goal")
    keys.add(goal["key"])
    require(bool(goal["name"]) and bool(goal["group"]) and bool(goal["alternatives"]), "Incomplete goal")
    for item_id in goal["alternatives"]:
        require(item_id in items and eligible(items[item_id]) and not items[item_id]["noted"], f"Ineligible goal ID {item_id}")
require(bool(goals), "Goal list is empty")
require(eligible(items[4151]) and not eligible(items[11840]) and not eligible(items[5698]), "Historical regression")
require(items[21892]["released"] == "2008-11-26", "Original platebody override missing")
for npc in npcs.values():
    require(isinstance(npc["categories"], list) and npc["slayerLevel"] >= 0, "Invalid Slayer metadata")
quests = read("historical-quests.json")
require(quests["availableBy"] == str(cutoff), "Quest cutoff mismatch")
require(len(quests["allowed"]) == 82 and len(set(quests["allowed"])) == 82, "Quest inventory mismatch")
require("The Lost Tribe" in quests["allowed"] and "Recruitment Drive" not in quests["allowed"], "Quest boundary regression")
provenance = read("data-provenance.json")
require(provenance["cutoff"] == str(cutoff), "Provenance cutoff mismatch")
require(provenance["itemRecords"] == len(items) and provenance["npcRecords"] == len(npcs), "Provenance count mismatch")
require(provenance["missingItemDates"] == sum(not r.get("released") for r in items.values()), "Unknown count mismatch")
require(provenance["itemsEligible"] == sum(eligible(r) for r in items.values()), "Eligible count mismatch")
recipes = read("historical-recipes.json")
for recipe in recipes:
    require(recipe["output"] in items and eligible(items[recipe["output"]]), "Invalid recipe output")
    require(type(recipe["outputQuantity"]) is int and recipe["outputQuantity"] > 0, "Invalid production quantity")
    require(recipe["skill"] in {"CRAFTING", "SMITHING", "FLETCHING", "MAGIC", "HERBLORE", "COOKING"} and recipe["xp"] > 0, "Invalid production XP")
    require(bool(recipe["inputs"]), "Production evidence must require materials")
    for item_id, count in recipe["inputs"].items():
        require(int(item_id) in items and eligible(items[int(item_id)]) and type(count) is int and count > 0, "Invalid recipe material")
    require(urlparse(recipe["source"]).scheme == "https", "Missing recipe source")
methods = read("historical-acquisitions.json")
for shop in methods["shops"]:
    require(date.fromisoformat(shop["availableBy"]) <= cutoff and bool(shop["name"]), "Invalid shop availability")
    require(urlparse(shop["source"]).scheme == "https", "Missing shop evidence")
for reward in methods["questRewards"]:
    require(reward["quest"] in quests["allowed"] and bool(reward["items"]), "Nonhistorical quest reward")
    require(urlparse(reward["source"]).scheme == "https", "Missing quest reward evidence")
    require(all(item_id in items and eligible(items[item_id]) for item_id in reward["items"]), "Invalid quest reward item")
for purchase in methods["purchases"]:
    require(purchase["item"] in items and eligible(items[purchase["item"]]) and purchase["coins"] > 0, "Invalid purchase")
    require(urlparse(purchase["source"]).scheme == "https", "Missing purchase source")
activities = read("historical-activities.json")
interface_ids = set()
for activity in activities:
    require(bool(activity["name"]) and bool(activity["interfaces"]), "Invalid activity")
    require(urlparse(activity["source"]).scheme == "https", "Missing activity source")
    for group in activity["interfaces"]:
        require(type(group) is int and group > 0 and group not in interface_ids, "Invalid/duplicate activity interface")
        interface_ids.add(group)
entities = read("modern-entities.json")
require(entities["cutoff"] == str(cutoff) and entities["symbolSources"], "Entity provenance missing")
entity_ids = set()
for entity in entities["records"]:
    key = (entity["kind"], entity["id"])
    require(key not in entity_ids and entity["kind"] in {"NPC", "OBJECT"} and type(entity["id"]) is int and entity["id"] >= 0, "Invalid/duplicate guarded entity")
    require(urlparse(entity["source"]).scheme == "https" and bool(entity["content"]) and bool(entity["symbol"]), "Missing entity evidence")
    entity_ids.add(key)
spells = read("historical-spells.json")
require(spells["availableBy"] == str(cutoff) and len(set(spells["allowed"])) == len(spells["allowed"]), "Invalid spell whitelist")
require("Varrock Teleport" in spells["allowed"] and "Fire Surge" not in spells["allowed"] and "Bones to Peaches" not in spells["allowed"], "Spell regression")
for area in read("historical-areas.json"):
    require(area["minX"] <= area["maxX"] and area["minY"] <= area["maxY"] and urlparse(area["source"]).scheme == "https", "Invalid area boundary")
interactions = read("historical-interactions.json")
require(interactions["cutoff"] == str(cutoff), "Interaction cutoff mismatch")
for interaction in interactions["blocked"]:
    require(interaction["kind"] in {"NPC", "OBJECT", "OPTION"} and interaction["names"] and urlparse(interaction["source"]).scheme == "https", "Invalid interaction")
require(eligible(items[7936]) and items[7936]["released"] == "2006-04-20", "Pure essence compatibility exception must preserve its date")
require((root / "third-party/OSRSBox-LICENSE.txt").is_file(), "Upstream license missing")
require("BSD 2-Clause License" in (root / "third-party/RuneLite-LICENSE.txt").read_text(encoding="utf-8"), "RuneLite license missing")
for file in (root / "src/main/java").rglob("*.java"):
    code = file.read_text(encoding="utf-8")
    require(not any(term in code for term in ("HttpClient", "URLConnection", "new URL(", "OkHttpClient")), f"Review runtime networking: {file.name}")
    require("new Gson(" not in code and "new com.google.gson.Gson(" not in code and "new com.google.gson.GsonBuilder(" not in code, f"Use RuneLite's shared Gson: {file.name}")
require(not (root / "src/main/java/com/tkoh/scape2005/DevelopmentLauncher.java").exists(), "Developer launcher must stay outside plugin JAR")
properties = (root / "runelite-plugin.properties").read_text(encoding="utf-8")
require("build=standard" in properties and "plugins=com.tkoh.scape2005.Scape2005Plugin" in properties, "Plugin Hub metadata missing")
for filename in ("LICENSE", "OSRSBox-LICENSE.txt", "RuneLite-LICENSE.txt"):
    require((resources / "META-INF" / filename).stat().st_size > 100, "License must survive standard Hub packaging")
require((root / "site/installation.html").is_file() and (root / "site/.nojekyll").is_file(), "Pages files missing")
print(f"PASS: {len(items):,} items; {len(npcs):,} NPCs; {len(goals)} goals; 82 quests; {len(recipes)} recipes; {len(activities)} activity guards; {len(entity_ids):,} scoped entity guards. No network used.")
