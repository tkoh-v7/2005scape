/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Production requires an output gain, all material losses and skill XP. */
public final class ProductionEvidence {
    public static final class Recipe {
        public int output;
        public int outputQuantity = 1;
        public Map<Integer, Integer> inputs;
        public String skill;
        public double xp;
        public String source;
    }
    private Map<Integer, Long> previous;
    private Map<String, Integer> previousXp;
    public void reset() { previous = null; previousXp = null; }
    public void baseline(Map<Integer, Long> holdings, Map<String, Integer> xp) {
        previous = new HashMap<>(holdings); previousXp = new HashMap<>(xp);
    }
    public Set<Integer> update(Map<Integer, Long> holdings, Map<String, Integer> xp, Iterable<Recipe> recipes) {
        Set<Integer> produced = new HashSet<>();
        if (previous != null && previousXp != null) {
            Map<Integer, Long> remainingLosses = new HashMap<>();
            previous.forEach((id, count) -> remainingLosses.put(id, Math.max(0, count - holdings.getOrDefault(id, 0L))));
            Map<String, Integer> remainingXp = new HashMap<>();
            xp.forEach((skill, amount) -> remainingXp.put(skill, Math.max(0, amount - previousXp.getOrDefault(skill, amount))));
            for (Recipe recipe : recipes) {
                long gained = holdings.getOrDefault(recipe.output, 0L) - previous.getOrDefault(recipe.output, 0L);
                long batches = gained / recipe.outputQuantity;
                if (batches <= 0 || recipe.xp <= 0 || recipe.inputs == null || recipe.inputs.isEmpty()) { continue; }
                long requiredXp = (long) Math.floor(recipe.xp * batches);
                if (remainingXp.getOrDefault(recipe.skill, 0) < Math.max(1, requiredXp)) { continue; }
                boolean materials = recipe.inputs.entrySet().stream().allMatch(input ->
                    remainingLosses.getOrDefault(input.getKey(), 0L) >= input.getValue() * batches);
                if (materials) {
                    produced.add(recipe.output);
                    recipe.inputs.forEach((id, count) -> remainingLosses.put(id, remainingLosses.get(id) - count * batches));
                    remainingXp.put(recipe.skill, remainingXp.get(recipe.skill) - (int) requiredXp);
                }
            }
        }
        baseline(holdings, xp);
        return produced;
    }
}
