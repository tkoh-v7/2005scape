"""Refresh the self-contained public audit from the shipped runtime facts."""

import json

import re

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

RESOURCES = ROOT / "src/main/resources"

PAGE = ROOT / "site/index.html"

def read(name):

    return json.loads((RESOURCES / name).read_text(encoding="utf-8"))

rows = []

for filename, kind in (("historical-items.json", "Item"), ("historical-npcs.json", "NPC")):

    for record in read(filename):

        released = record.get("released")

        status = "Restricted" if record.get("placeholder") or (released and released > "2005-06-22") else "Eligible" if released else "Unverified"

        reason = record.get("uncertainty") or ("No recorded release date" if not released else "")

        if record.get("placeholder"):
            reason = "Bank placeholder; not a usable item. Its parent date does not make the placeholder usable."
        elif record.get("noted"):
            reason = "Noted form of item ID " + str(record.get("baseId")) + ". Resolves to the base item when the plugin checks eligibility."

        evidence = record["source"]

        if record.get("replacementOf") is not None:

            status = "Eligible (replacement)"

            reason = record["replacementReason"] + " Original item ID: " + str(record["replacementOf"]) + ". Displayed date is the actual replacement release."

            evidence = record["replacementEvidence"]

        rows.append([kind, record["id"], record["name"], released or "", status,

                     evidence, reason, record.get("upstreamReleased") or ""])

quests = read("historical-quests.json")

for name in quests["allowed"] + quests["miniquestsAllowed"]:

    rows.append(["Quest", "", name, "by 2005-06-22", "Eligible", quests["sources"][0],

                 "Availability bound; not exact release date", ""])

methods = read("historical-acquisitions.json")

for shop in methods["shops"]:

    rows.append(["Shop", "", shop["name"], "by " + shop["availableBy"], "Eligible", shop["source"],

                 "Purchases require shop interaction, item gain and coin payment. Only historical items are eligible.", ""])

for reward in methods["questRewards"]:

    rows.append(["Reward", "", reward["quest"], "by 2005-06-22", "Eligible", reward["source"],

                 "New quest completion and item gain required. Item IDs: " + ", ".join(map(str, reward["items"])), ""])

for activity in read("historical-activities.json"):

    rows.append(["Activity", "", activity["name"], "after 2005-06-22", "Restricted", activity["source"],

                 "Participation guarded while an active interface is visible. Interface groups: " + ", ".join(map(str, activity["interfaces"])), ""])

item_names = {item["id"]: item["name"] for item in read("historical-items.json")}

for recipe in read("historical-recipes.json"):

    inputs = ", ".join(str(quantity) + " x " + item_names[int(item_id)] for item_id, quantity in recipe["inputs"].items())

    rows.append(["Recipe", recipe["output"], item_names[recipe["output"]], "historical item", "Eligible", recipe["source"],

                 inputs + "; " + recipe["skill"] + "; " + str(recipe["xp"]) + " XP. Output gain, material losses and XP gain required.", ""])

spells = read("historical-spells.json")

for name in spells["allowed"]:

    rows.append(["Spell", "", name, "by 2005-06-22", "Eligible", spells["sources"][1], "Local spell whitelist; later and unrecognised spells are restricted.", ""])

for entry in read("historical-interactions.json")["blocked"]:

    for name in entry["names"]:

        rows.append(["Interaction", "", name, "after 2005-06-22", "Restricted", entry["source"],

                     entry["kind"] + "; actions: " + ", ".join(entry.get("options", ["participation"])) + "; exits/examine preserved", ""])

areas = read("historical-areas.json")

for area in areas:

    rows.append(["Area", "", area["name"], "after 2005-06-22", "Restricted", area["source"],

                 "Conservative coordinate boundary, not an exhaustive historical map: " + str(area), ""])

for entity in read("modern-entities.json")["records"]:

    rows.append(["Modern " + ("NPC" if entity["kind"] == "NPC" else "object"), entity["id"], entity["symbol"],

                 "after 2005-06-22", "Restricted", entity["source"], "Scoped to " + entity["content"] + "; RuneLite gameval identifier; exact release date not asserted.", ""])

page = PAGE.read_text(encoding="utf-8")

data = json.dumps(rows, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")

page, count = re.subn(r'(<script id="records" type="application/json">).*?(</script>)',

                     lambda match: match[1] + data + match[2], page, flags=re.S)

if count != 1:

    raise ValueError("Audit page data placeholder missing or repeated")

if "<option>Eligible (replacement)</option>" not in page:

    page = page.replace("<option>Eligible</option>", "<option>Eligible</option><option>Eligible (replacement)</option>")

for kind in ("Shop", "Reward", "Activity", "Recipe", "Spell", "Interaction", "Area", "Modern object", "Modern NPC"):

    if "<option>" + kind + "</option>" not in page:

        page = page.replace("<option>Quest</option>", "<option>Quest</option><option>" + kind + "</option>")

page = page.replace("Regions, objects, full activity rules and later quests are not inventoried here.",

                    "Listed shops, rewards, production recipes and modern activity guards are included; this is not an exhaustive world-object or activity inventory.")

if 'href="installation.html"' not in page:

    page = page.replace('<h1>2005Scape historical audit</h1>', '<nav><a href="installation.html">Installation status</a> · <a href="https://github.com/tkoh-v7/2005scape">Plugin source</a></nav><h1>2005Scape historical audit</h1>')

PAGE.write_text(page, encoding="utf-8")

print(f"Refreshed audit page: {len(rows):,} records")

