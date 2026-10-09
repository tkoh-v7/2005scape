/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Quest;

/** Local availability-bound registry; exact modern names are checked against RuneLite. */
public final class QuestPolicy {
    private static final class Data { String[] allowed; String[] miniquestsAllowed; }
    private final Set<String> allowed = new HashSet<>();
    private final Set<String> known = new HashSet<>();
    public QuestPolicy(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(QuestPolicy.class.getResourceAsStream("/historical-quests.json"), StandardCharsets.UTF_8)) {
            Data data = gson.fromJson(reader, Data.class);
            Arrays.stream(data.allowed).map(ContentRules::normalize).forEach(allowed::add);
            Arrays.stream(data.miniquestsAllowed).map(ContentRules::normalize).forEach(allowed::add);
            Arrays.stream(Quest.values()).map(Quest::getName).map(ContentRules::normalize).forEach(known::add);
            allowed.addAll(Arrays.asList("dragon slayer", "monkey madness", "desert treasure", "vampire slayer", "mage arena"));
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load quest availability", ex); }
    }
    public boolean forbidden(String name) { String normalized = ContentRules.normalize(name); return known.contains(normalized) && !allowed.contains(normalized); }
    public boolean allowed(String name) { return allowed.contains(ContentRules.normalize(name)); }
    public String forbiddenMention(String text) {
        String label = ContentRules.normalize(text);
        return known.stream().filter(name -> !allowed.contains(name))
            .sorted(java.util.Comparator.comparingInt(String::length).reversed())
            .filter(name -> label.matches(".*(?<![a-z0-9])" + java.util.regex.Pattern.quote(name) + "(?![a-z0-9]).*"))
            .findFirst().orElse(null);
    }
    public static boolean dialogueInterface(int group) {
        return group == 60 || group == 217 || group == 219 || group == 229 || group == 231 || group == 193 || group == 153;
    }
    public static boolean decline(String option) {
        String name = ContentRules.normalize(option);
        return name.equals("no") || name.startsWith("no,") || name.startsWith("not now")
            || name.equals("cancel") || name.equals("close") || name.equals("leave") || name.equals("back");
    }
    public String forbiddenDialogue(net.runelite.api.widgets.Widget[] roots) {
        if (roots == null) { return null; }
        Set<net.runelite.api.widgets.Widget> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (net.runelite.api.widgets.Widget root : roots) {
            String found = forbiddenDialogue(root, seen, 0);
            if (found != null) { return found; }
        }
        return null;
    }
    private String forbiddenDialogue(net.runelite.api.widgets.Widget widget, Set<net.runelite.api.widgets.Widget> seen, int depth) {
        if (widget == null || widget.isHidden() || depth > 20 || !seen.add(widget)) { return null; }
        if (dialogueInterface(widget.getId() >>> 16) && widget.getId() >>> 16 != 219) {
            String found = forbiddenMention(widget.getText() + " " + widget.getName());
            if (found != null) { return found; }
        }
        net.runelite.api.widgets.Widget[][] branches = {widget.getChildren(), widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()};
        for (net.runelite.api.widgets.Widget[] branch : branches) {
            if (branch == null) { continue; }
            for (net.runelite.api.widgets.Widget child : branch) {
                String found = forbiddenDialogue(child, seen, depth + 1);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
