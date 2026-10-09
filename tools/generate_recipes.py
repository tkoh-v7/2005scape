"""Generate historical production methods from explicit ordinary recipes."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
rows = json.loads((RES / "historical-items.json").read_text(encoding="utf-8"))
by_name = {r["name"].casefold(): r["id"] for r in rows if r.get("released") and r["released"] <= "2005-06-22" and not r.get("placeholder") and not r.get("noted")}
recipes = []


def add(output, inputs, skill, xp, source, quantity=1):
    if output.casefold() not in by_name or any(name.casefold() not in by_name for name in inputs):
        return
    recipes.append({"output": by_name[output.casefold()], "outputQuantity": quantity,
                    "inputs": {str(by_name[name.casefold()]): count for name, count in inputs.items()},
                    "skill": skill, "xp": xp, "source": source})


for metal, bar_xp in (("Bronze", 12.5), ("Iron", 25), ("Steel", 37.5), ("Mithril", 50), ("Adamant", 62.5), ("Rune", 75)):
    for piece, bars in (("dagger", 1), ("med helm", 1), ("sword", 1), ("mace", 1), ("scimitar", 2),
                        ("longsword", 2), ("full helm", 2), ("sq shield", 2), ("claws", 2), ("warhammer", 3),
                        ("battleaxe", 3), ("2h sword", 3), ("chainbody", 3), ("platelegs", 3), ("plateskirt", 3),
                        ("kiteshield", 3), ("platebody", 5)):
        add(metal + " " + piece, {metal + " bar": bars}, "SMITHING", bars * bar_xp, "https://oldschool.runescape.wiki/w/Smithing")
    add(metal + " arrowtips", {metal + " bar": 1}, "SMITHING", bar_xp, "https://oldschool.runescape.wiki/w/Smithing", 15)
    add(metal + " arrow", {"Headless arrow": 15, metal + " arrowtips": 15}, "FLETCHING",
        {"Bronze": 6, "Iron": 15, "Steel": 37.5, "Mithril": 75, "Adamant": 112.5, "Rune": 187.5}[metal],
        "https://oldschool.runescape.wiki/w/Fletching", 15)
for colour, xp in (("Green", 62), ("Blue", 70), ("Red", 78), ("Black", 86)):
    for piece, hides in (("vambraces", 1), ("chaps", 2), ("body", 3)):
        add(colour + " d'hide " + piece, {colour + " dragon leather": hides}, "CRAFTING", xp * hides,
            "https://oldschool.runescape.wiki/w/Crafting")
for wood, logs, short_xp, long_xp in (("", "Logs", 5, 10), ("Oak ", "Oak logs", 16.5, 25),
                                    ("Willow ", "Willow logs", 33.3, 41.5), ("Maple ", "Maple logs", 50, 58.3),
                                    ("Yew ", "Yew logs", 67.5, 75), ("Magic ", "Magic logs", 83.3, 91.5)):
    for bow, xp in (("shortbow", short_xp), ("longbow", long_xp)):
        name = (wood + bow).capitalize()
        add(name + " (u)", {logs: 1}, "FLETCHING", xp, "https://oldschool.runescape.wiki/w/Fletching")
        add(name, {name + " (u)": 1, "Bow string": 1}, "FLETCHING", xp, "https://oldschool.runescape.wiki/w/Fletching")
add("Dragon sq shield", {"Shield left half": 1, "Shield right half": 1}, "SMITHING", 75,
    "https://oldschool.runescape.wiki/w/Dragon_sq_shield")
for gem, cut_xp, ring_xp, necklace_xp, amulet_xp in (("Sapphire", 50, 40, 55, 65), ("Emerald", 67.5, 55, 60, 70),
                                                  ("Ruby", 85, 70, 75, 85), ("Diamond", 107.5, 85, 90, 100),
                                                  ("Dragonstone", 137.5, 100, 105, 150)):
    add(gem, {"Uncut " + gem.lower(): 1}, "CRAFTING", cut_xp, "https://oldschool.runescape.wiki/w/Crafting")
    for jewellery, xp in (("ring", ring_xp), ("necklace", necklace_xp), ("amulet (u)", amulet_xp)):
        add(gem + " " + jewellery, {gem: 1, "Gold bar": 1}, "CRAFTING", xp, "https://oldschool.runescape.wiki/w/Crafting")
for output, base, xp in (("Amulet of magic", "Sapphire amulet", 17.5), ("Amulet of defence", "Emerald amulet", 37),
                        ("Amulet of strength", "Ruby amulet", 59), ("Amulet of power", "Diamond amulet", 67),
                        ("Amulet of glory", "Dragonstone amulet", 78)):
    # Elemental staves may replace elemental runes; the cosmic rune is mandatory.
    add(output, {base: 1, "Cosmic rune": 1}, "MAGIC", xp, "https://oldschool.runescape.wiki/w/Enchant_Jewellery")
for gem in ("Sapphire", "Emerald", "Ruby", "Diamond", "Dragonstone"):
    add(gem + " amulet", {gem + " amulet (u)": 1, "Ball of wool": 1}, "CRAFTING", 4,
        "https://oldschool.runescape.wiki/w/Stringing_amulets")
for output, material, xp in (("Bow string", "Flax", 15), ("Ball of wool", "Wool", 2.5)):
    add(output, {material: 1}, "CRAFTING", xp, "https://oldschool.runescape.wiki/w/Spinning_wheel")
for output, hides, xp in (("Leather gloves", 1, 13.8), ("Leather boots", 1, 16.2), ("Leather cowl", 1, 18.5),
                         ("Leather vambraces", 1, 22), ("Leather body", 1, 25), ("Leather chaps", 1, 27)):
    add(output, {"Leather": hides}, "CRAFTING", xp, "https://oldschool.runescape.wiki/w/Leather")
add("Hardleather body", {"Hard leather": 1}, "CRAFTING", 35, "https://oldschool.runescape.wiki/w/Hardleather_body")
for bar, ores, xp in (("Bronze", {"Copper ore": 1, "Tin ore": 1}, 6.2), ("Iron", {"Iron ore": 1}, 12.5),
                      ("Silver", {"Silver ore": 1}, 13.7), ("Steel", {"Iron ore": 1, "Coal": 2}, 17.5),
                      ("Gold", {"Gold ore": 1}, 22.5), ("Mithril", {"Mithril ore": 1, "Coal": 4}, 30),
                      ("Adamant", {"Adamantite ore": 1, "Coal": 6}, 37.5), ("Rune", {"Runite ore": 1, "Coal": 8}, 50)):
    add(bar + " bar", ores, "SMITHING", xp, "https://oldschool.runescape.wiki/w/Smelting")
for fish, raw, xp in (("Shrimps", "Raw shrimps", 30), ("Anchovies", "Raw anchovies", 30),
                       ("Sardine", "Raw sardine", 40), ("Herring", "Raw herring", 50), ("Trout", "Raw trout", 70),
                       ("Pike", "Raw pike", 80), ("Salmon", "Raw salmon", 90), ("Tuna", "Raw tuna", 100),
                       ("Lobster", "Raw lobster", 120), ("Bass", "Raw bass", 130),
                       ("Swordfish", "Raw swordfish", 140), ("Shark", "Raw shark", 210),
                       ("Sea turtle", "Raw sea turtle", 211.3), ("Manta ray", "Raw manta ray", 216.3)):
    add(fish, {raw: 1}, "COOKING", xp, "https://oldschool.runescape.wiki/w/Cooking")
for name, unfinished, secondary, xp in (("Attack potion", "Guam potion (unf)", "Eye of newt", 25),
    ("Antipoison", "Marrentill potion (unf)", "Unicorn horn dust", 37.5),
    ("Strength potion", "Tarromin potion (unf)", "Limpwurt root", 50),
    ("Restore potion", "Harralander potion (unf)", "Red spiders' eggs", 62.5),
    ("Energy potion", "Harralander potion (unf)", "Chocolate dust", 67.5),
    ("Defence potion", "Ranarr potion (unf)", "White berries", 75),
    ("Prayer potion", "Ranarr potion (unf)", "Snape grass", 87.5),
    ("Super attack", "Irit potion (unf)", "Eye of newt", 100),
    ("Superantipoison", "Irit potion (unf)", "Unicorn horn dust", 106.3),
    ("Super energy", "Avantoe potion (unf)", "Mort myre fungus", 117.5),
    ("Super strength", "Kwuarm potion (unf)", "Limpwurt root", 125),
    ("Super restore", "Snapdragon potion (unf)", "Red spiders' eggs", 142.5),
    ("Super defence", "Cadantine potion (unf)", "White berries", 150),
    ("Ranging potion", "Dwarf weed potion (unf)", "Wine of zamorak", 162.5),
    ("Magic potion", "Lantadyme potion (unf)", "Potato cactus", 172.5)):
    add(name + "(3)", {unfinished: 1, secondary: 1}, "HERBLORE", xp, "https://oldschool.runescape.wiki/w/Herblore")
(RES / "historical-recipes.json").write_text(json.dumps(recipes, indent=2) + "\n", encoding="utf-8")
print(f"Generated {len(recipes)} eligible production recipes")
