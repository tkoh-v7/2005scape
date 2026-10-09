/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.widgets.Widget;

/** A spell action must match the local period whitelist, not a modern denylist. */
public final class SpellPolicy {
    private static final int SPELLBOOK = 218;
    private static final int AUTOCAST = 201;
    private static final class Data { String[] allowed; }
    private final Set<String> allowed = new HashSet<>();
    public SpellPolicy(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(SpellPolicy.class.getResourceAsStream("/historical-spells.json"), StandardCharsets.UTF_8)) {
            Arrays.stream(gson.fromJson(reader, Data.class).allowed).map(SpellPolicy::name).forEach(allowed::add);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load spell rules", ex); }
    }
    public static String name(String label) {
        return ContentRules.normalize(label).split("\\s*->\\s*", 2)[0]
            .replaceFirst("^(cast|autocast|defensive autocast)\\s+", "")
            .replaceFirst("^level \\d+:?\\s*", "").trim();
    }
    public boolean allowed(String label) { return allowed.contains(name(label)); }
    public static boolean spellInterface(int group) { return group == SPELLBOOK || group == AUTOCAST; }
    public boolean restrictedAction(String option, String target, int group, Widget selected) {
        String action = ContentRules.normalize(option);
        if (spellInterface(group) && (action.equals("cast") || action.contains("autocast"))) { return !allowed(target); }
        if (selected != null && selected.getId() >>> 16 == SPELLBOOK) {
            String label = selected.getName();
            if (label == null || label.isEmpty()) { label = selected.getText(); }
            return !allowed(label);
        }
        return false;
    }
    public boolean restrictedWidget(Widget widget) {
        if (widget.getId() >>> 16 != SPELLBOOK || widget.getActions() == null) { return false; }
        boolean cast = Arrays.stream(widget.getActions()).anyMatch(action -> "cast".equals(ContentRules.normalize(action)));
        if (!cast) { return false; }
        String label = widget.getName();
        if (label == null || label.isEmpty()) { label = widget.getText(); }
        return !allowed(label);
    }
}
