/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Exact IDs scoped to known modern content; no assumption about generic names. */
public final class ModernEntities {
    private static final class Entry { String kind; int id; String content; String symbol; }
    private static final class Data { Entry[] records; }
    private final Map<Integer, String> objects = new HashMap<>();
    private final Map<Integer, String> npcs = new HashMap<>();
    private final java.util.Set<Integer> exits = new java.util.HashSet<>();
    public ModernEntities(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(ModernEntities.class.getResourceAsStream("/modern-entities.json"), StandardCharsets.UTF_8)) {
            for (Entry entry : gson.fromJson(reader, Data.class).records) {
                (entry.kind.equals("NPC") ? npcs : objects).put(entry.id, entry.content);
                if (entry.kind.equals("OBJECT") && (entry.symbol.contains("EXIT_PORTAL") || entry.symbol.endsWith("_EXIT"))) { exits.add(entry.id); }
            }
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load modern entities", ex); }
    }
    public String npc(int id) { return npcs.get(id); }
    public String object(int id) { return objects.get(id); }
    public boolean exitObject(int id) { return exits.contains(id); }
    public static boolean escape(String option) {
        String action = ContentRules.normalize(option);
        return action.equals("examine") || action.equals("walk here") || action.equals("exit")
            || action.equals("leave") || action.equals("escape") || action.equals("close")
            || action.equals("cancel") || action.equals("climb-up") || action.equals("climb-down");
    }
}
