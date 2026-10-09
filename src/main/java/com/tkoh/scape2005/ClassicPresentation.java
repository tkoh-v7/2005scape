/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;

/** Reversible changes; never removes ground nodes or server-side items. */
public class ClassicPresentation {
    @Inject private Client client;
    @Inject private ConfigManager configs;
    private final Map<ItemComposition, Integer> originalModels = new IdentityHashMap<>();
    private final Map<Widget, Boolean> hiddenWidgets = new WeakHashMap<>();
    private final Map<String, String> oldSettings = new HashMap<>();
    private static final String SKIN = "AROUND_2005";
    private boolean stopping;
    @Inject private com.google.gson.Gson gson;
    private SpellPolicy spells;
    private QuestPolicy quests;
    private ActivityRules activities;

    void start(boolean classic) {
        stopping = false;
        spells = new SpellPolicy(gson); activities = new ActivityRules(gson); quests = new QuestPolicy(gson);
        recoverSettings();
        if (classic) {
            set("gameframe", SKIN);
            set("hdHealthBars", "false");
            set("hdMenu", "false");
            set("rsCrossSprites", "false");
        }
        clearCaches();
    }
    private void set(String key, String value) {
        oldSettings.putIfAbsent(key, configs.getConfiguration("interfaceStyles", key));
        configs.setConfiguration("scape2005", "presentationBackupV1", gson.newBuilder().serializeNulls().create().toJson(oldSettings));
        configs.setConfiguration("interfaceStyles", key, value);
    }
    private void recoverSettings() {
        String json = configs.getConfiguration("scape2005", "presentationBackupV1");
        if (json == null) { return; }
        Map<String, String> backup = gson.fromJson(json,
            new com.google.gson.reflect.TypeToken<Map<String, String>>() { }.getType());
        if (backup == null) { return; }
        backup.forEach((key, old) -> {
            String ours = key.equals("gameframe") ? SKIN : "false";
            if (ours.equals(configs.getConfiguration("interfaceStyles", key))) {
                if (old == null) { configs.unsetConfiguration("interfaceStyles", key); }
                else { configs.setConfiguration("interfaceStyles", key, old); }
            }
        });
        configs.unsetConfiguration("scape2005", "presentationBackupV1");
    }
    void hideModel(ItemComposition item) {
        if (stopping || item.getInventoryModel() == -1) { return; }
        originalModels.putIfAbsent(item, item.getInventoryModel());
        // A missing inventory model yields no ground model or item sprite. Worn player
        // models are untouched. This preserves every eligible item in a mixed pile.
        item.setInventoryModel(-1);
    }
    void filterWidgets() {
        Widget[] roots = client.getWidgetRoots();
        if (roots == null) { return; }
        java.util.Set<Widget> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Widget root : roots) { visit(root, visited, 0); }
    }
    private void visit(Widget widget, java.util.Set<Widget> visited, int depth) {
        if (widget == null || depth > 20 || !visited.add(widget)) { return; }
        int group = widget.getId() >>> 16;
        // Keep activity HUDs and quest dialogues visible so their guards retain
        // context and the player can read the warning and decline/leave.
        if (activities.forInterface(group) == null && !QuestPolicy.dialogueInterface(group)
            && (spells.restrictedWidget(widget) || ContentRules.forbiddenLabel(widget.getName()) || ContentRules.forbiddenLabel(widget.getText()) || quests.forbidden(widget.getName()) || quests.forbidden(widget.getText()))) {
            if (!widget.isHidden()) { hiddenWidgets.putIfAbsent(widget, false); widget.setHidden(true); }
            return;
        }
        Widget[][] branches = {widget.getChildren(), widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()};
        for (Widget[] branch : branches) {
            if (branch != null) { for (Widget child : branch) { visit(child, visited, depth + 1); } }
        }
    }
    void restore() {
        restoreVisuals();
        restoreSettings();
    }
    // ConfigManager already points at the new profile when ProfileChanged fires.
    // Leave the old profile's durable backup for recovery when it is next used.
    void profileChanged(boolean classic) {
        restoreVisuals();
        oldSettings.clear();
        start(classic);
    }
    private void restoreVisuals() {
        stopping = true;
        originalModels.forEach((item, model) -> { if (item.getInventoryModel() == -1) { item.setInventoryModel(model); } });
        originalModels.clear();
        hiddenWidgets.forEach((widget, original) -> widget.setHidden(original));
        hiddenWidgets.clear();
        clearCaches();
    }
    private void restoreSettings() {
        oldSettings.forEach((key, old) -> {
            String ours = key.equals("gameframe") ? SKIN : "false";
            if (ours.equals(configs.getConfiguration("interfaceStyles", key))) {
                if (old == null) { configs.unsetConfiguration("interfaceStyles", key); }
                else { configs.setConfiguration("interfaceStyles", key, old); }
            }
        });
        oldSettings.clear();
        configs.unsetConfiguration("scape2005", "presentationBackupV1");
        clearCaches();
    }
    void clearCaches() {
        if (client.getItemModelCache() != null) { client.getItemModelCache().reset(); }
        if (client.getItemSpriteCache() != null) { client.getItemSpriteCache().reset(); }
    }
}
