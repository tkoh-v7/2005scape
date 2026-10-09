/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class InteractionPolicy {
    private static final class Rule { String kind; String[] names; String[] options; String source; }
    private static final class Data { Rule[] blocked; String[] modernDestinations; }
    private final Data data;
    public InteractionPolicy(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(InteractionPolicy.class.getResourceAsStream("/historical-interactions.json"), StandardCharsets.UTF_8)) {
            data = gson.fromJson(reader, Data.class);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load interaction rules", ex); }
    }
    public boolean blocked(String kind, String target, String option) {
        String name = ContentRules.normalize(target).replaceFirst(" \\(level-\\d+\\)$", "");
        String action = ContentRules.normalize(option);
        if (Arrays.asList("examine", "walk here", "exit", "leave", "escape", "close", "cancel").contains(action)) { return false; }
        for (Rule rule : data.blocked) {
            String match = rule.kind.equals("OPTION") ? action : name;
            if ((rule.kind.equals(kind) || rule.kind.equals("OPTION"))
                && Arrays.stream(rule.names).map(ContentRules::normalize).anyMatch(match::equals)
                && (rule.options == null || Arrays.stream(rule.options).map(ContentRules::normalize).anyMatch(action::equals))) { return true; }
        }
        return false;
    }
    public boolean modernDestination(String target) {
        String name = ContentRules.normalize(target).replaceFirst("^(teleport to|travel to|sail to)\\s+", "");
        return Arrays.stream(data.modernDestinations).map(ContentRules::normalize).anyMatch(destination ->
            name.equals(destination) || name.startsWith(destination + " (") || name.endsWith(": " + destination));
    }
}
