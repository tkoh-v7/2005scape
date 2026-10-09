/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Release dates refer to original RuneScape availability, not OSRS launch. */
public final class RuleBook {
    public static final LocalDate CUTOFF = LocalDate.of(2005, 6, 22);
    public enum Status { ALLOWED, BLOCKED, UNKNOWN }
    public static final class Entry {
        public int id;
        public String name;
        public String released;
        public String source;
        public String goal;
        public boolean placeholder;
        public boolean noted;
        public Integer baseId;
        public int highAlch;
        public Integer replacementOf;
        public String replacementEvidence;
        public String replacementReason;
    }
    public static final class Npc {
        public int id;
        public String name;
        public String released;
        public String source;
        public String[] categories;
        public int slayerLevel;
    }
    public static final class Goal {
        public String key;
        public String name;
        public String group;
        public int[] alternatives;
        public String notes;
        public boolean complete(Set<Integer> acquired) {
            return Arrays.stream(alternatives).anyMatch(acquired::contains);
        }
    }
    private final Map<Integer, Entry> items = new HashMap<>();
    private final Map<Integer, Npc> npcs = new HashMap<>();
    private final Gson gson;
    private List<Goal> goals = Collections.emptyList();

    public RuleBook(InputStream stream, Gson gson) {
        this.gson = gson;
        if (stream == null) { throw new IllegalStateException("Historical item data missing"); }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Entry[] rows = gson.fromJson(reader, Entry[].class);
            if (rows == null) { throw new IllegalArgumentException("Empty historical database"); }
            for (Entry row : rows) {
                if (row.id < 0 || row.name == null || row.source == null || row.source.isEmpty()) {
                    throw new IllegalArgumentException("Incomplete historical record");
                }
                if (row.released != null) { LocalDate.parse(row.released); }
                if (items.putIfAbsent(row.id, row) != null) {
                    throw new IllegalArgumentException("Duplicate historical item ID: " + row.id);
                }
            }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not read historical database", ex);
        }
    }

    public Status status(int id) {
        Entry row = items.get(id);
        if (row == null) { return Status.UNKNOWN; }
        if (row.placeholder) { return Status.BLOCKED; }
        if (row.replacementOf != null) {
            Entry original = items.get(row.replacementOf);
            if (original == null || original.placeholder || original.replacementOf != null
                || row.replacementEvidence == null || row.replacementEvidence.isEmpty()
                || row.replacementReason == null || row.replacementReason.isEmpty()) {
                return Status.UNKNOWN;
            }
            return dateStatus(original.released) == Status.ALLOWED ? Status.ALLOWED : Status.UNKNOWN;
        }
        return dateStatus(row.released);
    }
    public Entry item(int id) { return items.get(id); }
    public Map<Integer, Entry> items() { return Collections.unmodifiableMap(items); }
    public static RuleBook bundled(Gson gson) {
        RuleBook book = new RuleBook(RuleBook.class.getResourceAsStream("/historical-items.json"), gson);
        book.loadContent();
        return book;
    }
    private void loadContent() {
        Npc[] rows = resource("/historical-npcs.json", Npc[].class);
        for (Npc npc : rows) {
            if (npcs.putIfAbsent(npc.id, npc) != null) { throw new IllegalArgumentException("Duplicate NPC"); }
        }
        goals = Collections.unmodifiableList(Arrays.asList(resource("/historical-goals.json", Goal[].class)));
        for (Goal goal : goals) {
            if (goal.alternatives == null || goal.alternatives.length == 0) { throw new IllegalArgumentException("Empty goal"); }
            for (int id : goal.alternatives) {
                if (status(id) != Status.ALLOWED) { throw new IllegalArgumentException("Ineligible goal: " + goal.name); }
            }
        }
    }
    private <T> T resource(String path, Class<T> type) {
        InputStream stream = RuleBook.class.getResourceAsStream(path);
        if (stream == null) { throw new IllegalStateException("Missing resource: " + path); }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    private static Status dateStatus(String released) {
        if (released == null) { return Status.UNKNOWN; }
        return LocalDate.parse(released).isAfter(CUTOFF) ? Status.BLOCKED : Status.ALLOWED;
    }
    public Status npcStatus(int id) { return npcs.containsKey(id) ? dateStatus(npcs.get(id).released) : Status.UNKNOWN; }
    public List<Goal> goals() { return goals; }
    public boolean isGoalItem(int id) { return goals.stream().anyMatch(goal -> Arrays.stream(goal.alternatives).anyMatch(value -> value == id)); }
    public List<Npc> slayerCandidates(String task, int level) {
        String target = task.toLowerCase(Locale.ROOT).trim();
        return npcs.values().stream().filter(npc -> npcStatus(npc.id) == Status.ALLOWED && npc.slayerLevel <= level
            && npc.categories != null && Arrays.stream(npc.categories).anyMatch(category -> category.equalsIgnoreCase(target)))
            .sorted(java.util.Comparator.comparing(npc -> npc.name)).collect(Collectors.toList());
    }
    public long eligibleCount() { return items.keySet().stream().filter(id -> status(id) == Status.ALLOWED).count(); }
}
