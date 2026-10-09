/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Local acquisition methods, separate from Kirk's choice of gear goals. */
public final class AcquisitionRules {
    public static final class Shop {
        public String name;
        public String[] aliases;
        public int[] regions;
        public String availableBy;
        public String source;
        public boolean matches(String text, int region) {
            boolean location = regions == null || regions.length == 0 || Arrays.stream(regions).anyMatch(value -> value == region);
            String normalized = title(text);
            return location && (title(name).equals(normalized)
                || aliases != null && Arrays.stream(aliases).anyMatch(alias -> title(alias).equals(normalized)));
        }
        private static String title(String value) { return ContentRules.normalize(value).replaceAll("[’']", "").replaceAll("[!.,]", "").trim(); }
    }
    public static final class QuestReward {
        public String quest;
        public int[] items;
        public String source;
    }
    public static final class Purchase {
        public String npc;
        public int item;
        public int coins;
        public int[] regions;
        public String source;
        public String blockedAfterQuestStarted;
    }
    private static final class Data {
        Shop[] shops;
        QuestReward[] questRewards;
        Purchase[] purchases;
    }
    private final Data data;
    private final List<ProductionEvidence.Recipe> recipes;
    public AcquisitionRules(Gson gson) {
        data = read(gson, "/historical-acquisitions.json", Data.class);
        recipes = Arrays.asList(read(gson, "/historical-recipes.json", ProductionEvidence.Recipe[].class));
    }
    private static <T> T read(Gson gson, String name, Class<T> type) {
        try (InputStreamReader reader = new InputStreamReader(AcquisitionRules.class.getResourceAsStream(name), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not read acquisition data", ex); }
    }
    public List<ProductionEvidence.Recipe> recipes() { return recipes; }
    public QuestReward[] questRewards() { return data.questRewards; }
    public Purchase[] purchases() { return data.purchases; }
    public Shop shop(String text, int region) {
        return Arrays.stream(data.shops).filter(shop -> shop.matches(text, region)).findFirst().orElse(null);
    }
}
