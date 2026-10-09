/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.widgets.Widget;

/** Block participation while a recognised modern activity interface is visible. */
public final class ActivityRules {
    public static final class Activity {
        public String name;
        public int[] interfaces;
        public String source;
    }
    private final Activity[] activities;
    private final java.util.Map<Integer, String> interfaceNames = new java.util.HashMap<>();
    public ActivityRules(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(ActivityRules.class.getResourceAsStream("/historical-activities.json"), StandardCharsets.UTF_8)) {
            activities = gson.fromJson(reader, Activity[].class);
            for (Activity activity : activities) {
                for (int group : activity.interfaces) { interfaceNames.put(group, activity.name); }
            }
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load activity rules", ex); }
    }
    public String forInterface(int group) {
        return interfaceNames.get(group);
    }
    public String visible(Widget[] roots) {
        if (roots == null) { return null; }
        Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Widget root : roots) {
            String name = visible(root, seen, 0);
            if (name != null) { return name; }
        }
        return null;
    }
    private String visible(Widget widget, Set<Widget> seen, int depth) {
        if (widget == null || widget.isHidden() || depth > 20 || !seen.add(widget)) { return null; }
        String name = forInterface(widget.getId() >>> 16);
        if (name != null) { return name; }
        Widget[][] branches = {widget.getChildren(), widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()};
        for (Widget[] branch : branches) {
            if (branch == null) { continue; }
            for (Widget child : branch) {
                name = visible(child, seen, depth + 1);
                if (name != null) { return name; }
            }
        }
        return null;
    }
    public static boolean participation(String action, String option) {
        String normalized = ContentRules.normalize(option);
        if (Arrays.asList("walk here", "cancel", "examine", "exit", "leave", "escape", "climb-up", "climb-down").contains(normalized)) { return false; }
        return action.startsWith("NPC_") || action.contains("OBJECT_") || action.startsWith("GROUND_ITEM_");
    }
    public boolean restrictedInterfaceAction(int group, String option) {
        return forInterface(group) != null && !Arrays.asList("close", "cancel", "exit", "leave", "back", "examine", "view", "info").contains(ContentRules.normalize(option));
    }
}
