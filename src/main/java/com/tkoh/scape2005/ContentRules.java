/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Explicit compatibility rules for client UI labels, not guessed release dates. */
public final class ContentRules {
    private ContentRules() { }
    private static final Set<String> OLD_PRAYERS = new HashSet<>(Arrays.asList(
        "thick skin", "burst of strength", "clarity of thought", "rock skin", "superhuman strength",
        "improved reflexes", "rapid restore", "rapid heal", "protect item", "steel skin", "ultimate strength",
        "incredible reflexes", "protect from magic", "protect from missiles", "protect from melee",
        "retribution", "redemption", "smite"));
    private static final Set<String> NEW_SKILLS = new HashSet<>(Arrays.asList(
        "farming", "construction", "hunter", "sailing"));
    private static final Set<String> NEW_PRAYERS = new HashSet<>(Arrays.asList(
        "sharp eye", "mystic will", "hawk eye", "mystic lore", "eagle eye", "mystic might",
        "chivalry", "piety", "preserve", "rigour", "augury", "deadeye", "mystic vigour"));
    private static final Set<String> NEW_SPELLS = new HashSet<>(Arrays.asList(
        "lumbridge home teleport", "home teleport", "teleport to house", "ape atoll teleport",
        "enchant crossbow bolt", "enchant crossbow bolts", "kourend castle teleport", "civitas illa fortis teleport",
        "varlamore teleport", "lunar", "arceuus", "thralls", "resurrect lesser ghost", "resurrect lesser skeleton",
        "resurrect lesser zombie", "resurrect superior ghost", "resurrect superior skeleton", "resurrect superior zombie",
        "resurrect greater ghost", "resurrect greater skeleton", "resurrect greater zombie"));
    private static final Set<String> HISTORICAL_MASTERS = new HashSet<>(Arrays.asList(
        "turael", "mazchna", "vannaka", "chaeldar", "duradel"));
    private static final Set<String> MODERN_MASTERS = new HashSet<>(Arrays.asList(
        "spria", "aya", "nieve", "steve", "konar quo maten", "krystilia", "kuradal", "mortimer"));
    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").replace('\u00a0', ' ')
            .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    public static boolean forbiddenLabel(String value) {
        String name = normalize(value).replaceFirst("^level \\d+:?\\s*", "");
        return NEW_SKILLS.contains(name) || NEW_PRAYERS.contains(name) || NEW_SPELLS.contains(name)
            || name.equals("achievement diaries") || name.equals("achievement diary")
            || name.startsWith("ruinous powers") || name.startsWith("resurrect ")
            || rewardSource(name) == RuleBook.Status.BLOCKED
            || name.startsWith("lunar spellbook") || name.startsWith("arceuus spellbook");
    }
    public static boolean oldPrayer(String value) { return OLD_PRAYERS.contains(normalize(value)); }
    public static boolean modernMaster(String value) { return MODERN_MASTERS.contains(normalize(value)); }
    public static boolean historicalMaster(String value) { return HISTORICAL_MASTERS.contains(normalize(value)); }
    public static RuleBook.Status rewardSource(String value) {
        String name = normalize(value);
        if (Arrays.asList("barrows", "crystal chest", "fishing trawler", "shades of mort'ton",
            "clue scroll (easy)", "clue scroll (medium)", "clue scroll (hard)").contains(name)) { return RuleBook.Status.ALLOWED; }
        if (Arrays.asList("wintertodt", "tempoross", "guardians of the rift", "chambers of xeric", "theatre of blood",
            "tombs of amascut", "the gauntlet", "the corrupted gauntlet", "clue scroll (beginner)",
            "clue scroll (elite)", "clue scroll (master)").contains(name)) { return RuleBook.Status.BLOCKED; }
        return RuleBook.Status.UNKNOWN;
    }
}
