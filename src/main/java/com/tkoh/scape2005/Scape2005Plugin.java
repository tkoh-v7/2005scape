/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.IdentityHashMap;
import net.runelite.api.NPC;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.ItemComposition;
import net.runelite.api.Skill;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.interfacestyles.InterfaceStylesPlugin;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "2005Scape", description = "Personal 22 June 2005 historical challenge",
    tags = {"classic", "2005", "ironman"}, enabledByDefault = false)
@PluginDependency(InterfaceStylesPlugin.class)
@PluginDependency(LootTrackerPlugin.class)
@PluginDependency(SlayerPlugin.class)
public class Scape2005Plugin extends Plugin {
    // RuneLite InterfaceID.SHOPMAIN; kept local for compatibility with older APIs.
    private static final int SHOP_MAIN = 300;
    @Inject private Client client;
    @Inject private ConfigManager configs;
    @Inject private Scape2005Config config;
    @Inject private ItemManager itemManager;
    @Inject private OverlayManager overlays;
    @Inject private Scape2005Overlay overlay;
    @Inject private HistoricalGroundOverlay groundOverlay;
    @Inject private ClassicPresentation presentation;
    @Inject private SlayerAdvisor slayer;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private PluginManager plugins;
    private final AcquisitionEvidence evidence = new AcquisitionEvidence();
    private final MissedLootEvidence missedEvidence = new MissedLootEvidence();
    private final Map<Integer, Long> receiptQuantities = new HashMap<>();
    private final ProductionEvidence production = new ProductionEvidence();
    private final Map<Quest, QuestState> questStates = new HashMap<>();
    private final Map<Integer, Integer> shopIntents = new HashMap<>();
    private final Map<Integer, String> shopSources = new HashMap<>();
    private AcquisitionRules acquisitionRules;
    private ActivityRules activityRules;
    private InteractionPolicy interactions;
    private ModernEntities modernEntities;
    private SpellPolicy spells;
    private AreaPolicy areas;
    private QuestPolicy questPolicy;
    private boolean questsDirty = true;
    private String questReceiptSource;
    private int questReceiptExpiry;
    private final Map<Integer, Integer> unprovenGains = new HashMap<>();
    private final Set<Integer> freshReceipts = new HashSet<>();
    private final Map<Integer, net.runelite.api.coords.WorldPoint> ownDrops = new HashMap<>();
    private String modernActivity;
    private Map<Integer, Long> previousHoldings;
    private String recentMerchant;
    private int merchantExpiry;
    private final Map<TileItem, Tile> ground = new IdentityHashMap<>();
    private final Set<Overlay> displacedOverlays = new HashSet<>();
    private ChallengePanel panel;
    private NavigationButton navigation;
    private boolean active;
    private int lastWarningTick = -20;
    @Inject private Gson gson;
    private final ChallengeState.Acquisitions acquisitions = new ChallengeState.Acquisitions();
    private RuleBook rules;
    private ChallengeState state;
    private boolean bankAvailable;
    private int settlingTicks;
    private String notice = "Historical database preview";

    @Provides Scape2005Config provideConfig(ConfigManager manager) {
        return manager.getConfig(Scape2005Config.class);
    }

    @Override protected void startUp() {
        rules = RuleBook.bundled(gson);
        acquisitionRules = new AcquisitionRules(gson);
        activityRules = new ActivityRules(gson);
        interactions = new InteractionPolicy(gson); spells = new SpellPolicy(gson); questPolicy = new QuestPolicy(gson);
        areas = new AreaPolicy(gson);
        modernEntities = new ModernEntities(gson);
        active = true;
        resetSession();
        overlays.add(overlay);
        overlays.add(groundOverlay);
        panel = new ChallengePanel((id, source) -> clientThread.invokeLater(() -> confirmReceipt(id, source)),
            id -> clientThread.invokeLater(() -> declineReceipt(id)));
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = icon.createGraphics();
        graphics.setColor(new java.awt.Color(62, 52, 40)); graphics.fillRect(0, 0, 32, 32);
        graphics.setColor(new java.awt.Color(255, 210, 96)); graphics.drawRect(1, 1, 29, 29); graphics.drawString("05", 8, 21); graphics.dispose();
        navigation = NavigationButton.builder().tooltip("2005Scape").icon(icon).priority(8).panel(panel).build();
        toolbar.addNavigation(navigation);
        clientThread.invokeLater(() -> {
            if (active) { presentation.start(config.classicFrame()); scanGround(); }
        });
    }
    @Override protected void shutDown() {
        overlays.remove(overlay);
        active = false;
        overlays.remove(groundOverlay);
        if (navigation != null) { toolbar.removeNavigation(navigation); }
        restoreGroundOverlays();
        clientThread.invokeLater(() -> { if (!active) { presentation.restore(); } });
        resetSession();
    }
    private void resetSession() {
        state = null;
        acquisitions.reset();
        evidence.clear();
        missedEvidence.clear(); receiptQuantities.clear();
        production.reset(); questStates.clear(); shopIntents.clear(); shopSources.clear();
        previousHoldings = null; recentMerchant = null;
        modernActivity = null;
        questsDirty = true; questReceiptSource = null; unprovenGains.clear(); freshReceipts.clear(); ownDrops.clear();
        ground.clear();
        bankAvailable = false;
        settlingTicks = 3;
    }
    @Subscribe public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) { resetSession(); }
    @Subscribe public void onProfileChanged(ProfileChanged event) {
        resetSession();
        clientThread.invokeLater(() -> {
            if (active) { presentation.profileChanged(config.classicFrame()); scanGround(); }
        });
    }
    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) { resetSession(); }
        else { scanGround(); }
    }

    private void loadState() {
        String json = configs.getRSProfileConfiguration(Scape2005Config.GROUP, "challengeStateV1");
        if (json == null || json.isEmpty()) {
            state = new ChallengeState();
            state.marketValue = config.marketValue();
            save();
            return;
        }
        ChallengeState loaded = gson.fromJson(json, ChallengeState.class);
        if (loaded == null || loaded.schemaVersion != 1 || loaded.acquired == null
            || loaded.missedValue < 0 || loaded.missedQuantity < 0 || loaded.started == null) {
            throw new IllegalStateException("Invalid 2005Scape save; refusing to overwrite it");
        }
        state = loaded;
        if (state.acquisitionSources == null) { state.acquisitionSources = new HashMap<>(); }
        if (state.quarantined == null) { state.quarantined = new HashSet<>(); }
        if (state.pendingReceipts == null) { state.pendingReceipts = new HashMap<>(); }
        if (state.blockedSources == null) { state.blockedSources = new HashMap<>(); }
        if (config.strictSources()) { state.quarantined.addAll(state.pendingReceipts.keySet()); }
        java.time.Instant.parse(state.started);
    }
    private void save() {
        if (state != null) {
            configs.setRSProfileConfiguration(Scape2005Config.GROUP, "challengeStateV1", gson.toJson(state));
        }
    }

    int canonical(int id) {
        // Only notes are collapsed. Modern ornamented variants remain distinct.
        net.runelite.api.ItemComposition item = itemManager.getItemComposition(id);
        return item.getNote() != -1 ? item.getLinkedNoteId() : id;
    }
    boolean restricted(int id) {
        if (id < 0) { return false; }
        if (quarantined(id)) { return true; }
        RuleBook.Status status = rules.status(canonical(id));
        return status == RuleBook.Status.BLOCKED || (status == RuleBook.Status.UNKNOWN && config.strictUnknown());
    }
    private boolean quarantined(int id) {
        RuleBook.Entry entry = rules.item(id);
        int base = entry != null && entry.noted && entry.baseId != null ? entry.baseId : id;
        return state != null && (state.quarantined.contains(base) || config.strictSources() && state.pendingReceipts.containsKey(base));
    }

    @Subscribe public void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) { return; }
        if (state == null) { loadState(); }
        Map<Integer, Long> holdings = new HashMap<>();
        collect(holdings, InventoryID.INVENTORY);
        collect(holdings, InventoryID.EQUIPMENT);
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        collect(holdings, InventoryID.BANK);
        Widget bankWidget = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        boolean banking = bankWidget != null && !bankWidget.isHidden();
        boolean bankTransition = (bank != null) != bankAvailable;
        bankAvailable = bank != null;
        Map<String, Integer> xp = new HashMap<>();
        for (Skill skill : Arrays.asList(Skill.CRAFTING, Skill.SMITHING, Skill.FLETCHING, Skill.MAGIC, Skill.HERBLORE, Skill.COOKING)) {
            xp.put(skill.name(), client.getSkillExperience(skill));
        }
        modernActivity = restrictedContext();
        if (settlingTicks > 0 || bankTransition) {
            acquisitions.baseline(holdings);
            evidence.clear();
            production.baseline(holdings, xp);
            observeQuests(true); questReceiptSource = null;
            shopIntents.clear(); shopSources.clear(); recentMerchant = null;
            if (settlingTicks > 0) { settlingTicks--; }
        } else {
            if (modernActivity == null) {
                if (questsDirty && client.getTickCount() % 3 == 0) { observeQuests(false); }
                for (int id : production.update(holdings, xp, acquisitionRules.recipes())) {
                    evidence.proof(id, client.getTickCount(), "Historical production recipe");
                }
                observePurchases(holdings);
            } else {
                production.baseline(holdings, xp); observeQuests(true);
                shopIntents.clear(); shopSources.clear(); recentMerchant = null;
            }
            for (int id : acquisitions.update(holdings)) {
                if (modernActivity != null) { evidence.proof(id, client.getTickCount(), "BLOCKED:" + modernActivity); }
                if (questReceiptSource != null && questReceiptExpiry >= client.getTickCount()) {
                    evidence.proof(id, client.getTickCount(), questReceiptSource);
                }
                evidence.gain(id, client.getTickCount());
                receiptQuantities.put(id, Math.max(0, holdings.getOrDefault(id, 0L) - previousHoldings.getOrDefault(id, 0L)));
                if (rules.status(id) == RuleBook.Status.ALLOWED) {
                    unprovenGains.put(id, client.getTickCount() + 4);
                    if (config.strictSources() && !state.pendingReceipts.containsKey(id) && !state.blockedSources.containsKey(id)) {
                        state.pendingReceipts.put(id, new ChallengeState.Receipt()); freshReceipts.add(id); save();
                    }
                }
            }
            creditMatched();
            reviewUnprovenGains();
        }
        previousHoldings = new HashMap<>(holdings);
        if (banking && bank != null && state.quarantined.removeIf(id -> holdings.getOrDefault(id, 0L) == 0)) {
            state.blockedSources.keySet().retainAll(state.quarantined);
            state.pendingReceipts.keySet().removeIf(id -> holdings.getOrDefault(id, 0L) == 0);
            save(); presentation.restore(); presentation.start(config.classicFrame()); scanGround();
        }
        notice = modernActivity == null ? "Historical challenge active" : "Restricted activity: " + modernActivity;
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null) {
            for (Item item : equipment.getItems()) {
                if (item.getId() >= 0 && restricted(item.getId())) {
                    notice = "Restricted equipment: " + itemManager.getItemComposition(item.getId()).getName();
                    break;
                }
            }
        }
        if (config.contentRestrictions()) { presentation.filterWidgets(); }
        if (config.historicalLabels()) {
            overlays.removeIf(candidate -> {
                if (candidate.getClass().getName().startsWith("net.runelite.client.plugins.grounditems.")) {
                    displacedOverlays.add(candidate); return true;
                }
                return false;
            });
        }
        if (client.getTickCount() % 3 == 0) { panel.update(rules, state, notice, slayer.advise(rules)); }
    }
    private void collect(Map<Integer, Long> result, InventoryID containerId) {
        ItemContainer container = client.getItemContainer(containerId);
        if (container == null) { return; }
        for (Item item : container.getItems()) {
            if (item.getId() >= 0 && item.getQuantity() > 0) {
                ItemComposition composition = itemManager.getItemComposition(item.getId());
                if (composition.getPlaceholderTemplateId() != -1) { continue; }
                applyModel(composition);
                result.merge(canonical(item.getId()), (long) item.getQuantity(), Long::sum);
            }
        }
    }

    private void observeQuests(boolean baseline) {
        for (Quest quest : Quest.values()) {
            QuestState current = quest.getState(client);
            QuestState previous = questStates.put(quest, current);
            if (baseline || previous == null || previous == current) { continue; }
            if (!questPolicy.allowed(quest.getName()) && current != QuestState.NOT_STARTED) {
                questReceiptSource = "BLOCKED:Quest: " + quest.getName(); questReceiptExpiry = client.getTickCount() + 3;
                if (config.contentRestrictions()) { warn("Modern quest progression detected: " + quest.getName() + ". Its rewards are restricted."); }
            } else if (current == QuestState.FINISHED) {
                if (questReceiptSource == null || questReceiptExpiry < client.getTickCount() || !questReceiptSource.startsWith("BLOCKED:")) {
                    questReceiptSource = "Quest: " + quest.getName(); questReceiptExpiry = client.getTickCount() + 3;
                }
                for (AcquisitionRules.QuestReward reward : acquisitionRules.questRewards()) {
                    if (!ContentRules.normalize(quest.getName()).equals(ContentRules.normalize(reward.quest))) { continue; }
                    for (int id : reward.items) {
                        if (rules.status(id) == RuleBook.Status.ALLOWED) { evidence.proof(id, client.getTickCount(), "Quest: " + reward.quest); }
                    }
                }
            }
        }
        questsDirty = false;
        if (!baseline && questReceiptSource != null && questReceiptExpiry >= client.getTickCount()) {
            unprovenGains.keySet().forEach(id -> evidence.proof(id, client.getTickCount(), questReceiptSource));
        }
    }
    @Subscribe public void onVarbitChanged(VarbitChanged event) { questsDirty = true; }
    private boolean historicalPurchase(AcquisitionRules.Purchase purchase) {
        if (purchase.blockedAfterQuestStarted != null) {
            for (Quest quest : Quest.values()) {
                if (quest.getName().equalsIgnoreCase(purchase.blockedAfterQuestStarted)
                    && quest.getState(client) != QuestState.NOT_STARTED) { return false; }
            }
        }
        int region = client.getLocalPlayer().getWorldLocation().getRegionID();
        return purchase.regions == null || purchase.regions.length == 0
            || Arrays.stream(purchase.regions).anyMatch(value -> value == region);
    }
    private void observePurchases(Map<Integer, Long> holdings) {
        if (previousHoldings == null) { return; }
        int tick = client.getTickCount();
        shopIntents.values().removeIf(expiry -> expiry < tick);
        shopSources.keySet().retainAll(shopIntents.keySet());
        long spent = previousHoldings.getOrDefault(995, 0L) - holdings.getOrDefault(995, 0L);
        if (spent <= 0) { return; }
        for (int id : new HashSet<>(shopIntents.keySet())) {
            if (holdings.getOrDefault(id, 0L) > previousHoldings.getOrDefault(id, 0L)) {
                evidence.proof(id, tick, "Shop: " + shopSources.get(id));
                shopIntents.remove(id); shopSources.remove(id);
            }
        }
        if (recentMerchant != null && merchantExpiry >= tick) {
            for (AcquisitionRules.Purchase purchase : acquisitionRules.purchases()) {
                if (purchase.npc.equalsIgnoreCase(recentMerchant) && historicalPurchase(purchase) && spent >= purchase.coins
                    && holdings.getOrDefault(purchase.item, 0L) > previousHoldings.getOrDefault(purchase.item, 0L)) {
                    evidence.proof(purchase.item, tick, "Purchase: " + purchase.npc);
                    recentMerchant = null; break;
                }
            }
        }
    }
    private void rememberAcquisitionIntent(MenuOptionClicked event) {
        if (state == null || settlingTicks > 0 || client.getLocalPlayer() == null) { return; }
        NPC npc = event.getMenuEntry().getNpc();
        if (npc != null && Arrays.stream(acquisitionRules.purchases()).anyMatch(p -> p.npc.equalsIgnoreCase(npc.getName()) && historicalPurchase(p))) {
            recentMerchant = npc.getName(); merchantExpiry = client.getTickCount() + 30;
        }
        String option = ContentRules.normalize(event.getMenuOption());
        if (!option.startsWith("buy-") && !option.equals("buy")) { return; }
        Widget widget = event.getWidget();
        int group = widget != null ? widget.getId() >>> 16 : event.getParam1() >>> 16;
        if (group != SHOP_MAIN) { return; }
        int id = event.getItemId();
        if (id < 0 && widget != null) { id = widget.getItemId(); }
        AcquisitionRules.Shop shop = findShop();
        if (shop == null) {
            if (config.contentRestrictions() && config.strictUnknown()) {
                event.consume(); warn("This shop's historical availability is unverified. Purchase blocked.");
            }
            return;
        }
        if (id >= 0 && rules.status(canonical(id)) == RuleBook.Status.ALLOWED && !restricted(id)) {
            int base = canonical(id); shopIntents.put(base, client.getTickCount() + 20); shopSources.put(base, shop.name);
        }
    }
    private AcquisitionRules.Shop findShop() {
        Widget[] roots = client.getWidgetRoots();
        if (roots == null) { return null; }
        Set<Widget> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        int region = client.getLocalPlayer().getWorldLocation().getRegionID();
        for (Widget root : roots) {
            AcquisitionRules.Shop shop = findShop(root, region, seen, 0);
            if (shop != null) { return shop; }
        }
        return null;
    }
    private AcquisitionRules.Shop findShop(Widget widget, int region, Set<Widget> seen, int depth) {
        if (widget == null || widget.isHidden() || depth > 20 || !seen.add(widget)) { return null; }
        if (widget.getId() >>> 16 == SHOP_MAIN) {
            AcquisitionRules.Shop shop = acquisitionRules.shop(widget.getText(), region);
            if (shop != null) { return shop; }
        }
        Widget[][] branches = {widget.getChildren(), widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()};
        for (Widget[] branch : branches) {
            if (branch != null) {
                for (Widget child : branch) {
                    AcquisitionRules.Shop shop = findShop(child, region, seen, depth + 1);
                    if (shop != null) { return shop; }
                }
            }
        }
        return null;
    }

    private static boolean pickup(MenuEntry entry) {
        return entry.getType().name().startsWith("GROUND_ITEM_") && "take".equals(ContentRules.normalize(entry.getOption()));
    }
    @Subscribe(priority = -100) public void onMenuOpened(MenuOpened event) {
        MenuEntry[] entries = Arrays.stream(event.getMenuEntries())
            .filter(entry -> !hideEntry(entry))
            .toArray(MenuEntry[]::new);
        event.setMenuEntries(entries);
        client.setMenuEntries(entries);
    }
    @Subscribe(priority = -100) public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.isConsumed()) { return; }
        if (ContentRules.normalize(event.getMenuOption()).equals("examine")) { return; }
        Widget clicked = event.getWidget();
        int clickedGroup = clicked != null ? clicked.getId() >>> 16 : event.getParam1() >>> 16;
        String clickedLabel = clicked == null ? event.getMenuTarget() : clicked.getText() + " " + clicked.getName();
        if (config.contentRestrictions()) {
            Widget selectedSpell = event.getMenuAction().name().contains("WIDGET_TARGET") ? client.getSelectedWidget() : null;
            if (spells.restrictedAction(event.getMenuOption(), event.getMenuTarget(), clickedGroup, selectedSpell)) {
                event.consume(); warn("That spell is outside the 22 June 2005 whitelist."); return;
            }
            String kind = event.getMenuEntry().getNpc() != null ? "NPC" : event.getMenuAction().name().contains("OBJECT_") ? "OBJECT" : "WIDGET";
            if (kind.equals("OBJECT") && modernEntities.exitObject(event.getId()) && !event.getMenuAction().name().contains("WIDGET_TARGET")) { return; }
            NPC entityNpc = event.getMenuEntry().getNpc();
            String modernEntity = entityNpc != null ? modernEntities.npc(entityNpc.getId())
                : kind.equals("OBJECT") ? modernEntities.object(event.getId()) : null;
            if (modernEntity != null && !ModernEntities.escape(event.getMenuOption())) {
                event.consume(); warn(modernEntity + " content is outside the historical rules. Action blocked."); return;
            }
            if (interactions.blocked(kind, event.getMenuTarget(), event.getMenuOption())
                || interactions.modernDestination(event.getMenuTarget()) || interactions.modernDestination(clickedLabel)
                || clickedGroup == 76 && ContentRules.normalize(event.getMenuOption()).contains("teleport")) {
                event.consume(); warn("That activity or travel route is outside the historical rules."); return;
            }
            if (QuestPolicy.dialogueInterface(clickedGroup) && !QuestPolicy.decline(event.getMenuOption()) && !QuestPolicy.decline(clicked == null ? "" : clicked.getText())) {
                String forbidden = questPolicy.forbiddenMention(clickedLabel);
                if (forbidden == null && clickedGroup != 219) { forbidden = questPolicy.forbiddenDialogue(client.getWidgetRoots()); }
                if (forbidden != null) { event.consume(); warn("Modern quest dialogue blocked: " + forbidden); return; }
            }
        }
        String activity = restrictedContext();
        if (activity != null && (ActivityRules.participation(event.getMenuAction().name(), event.getMenuOption())
            || activityRules.restrictedInterfaceAction(clickedGroup, event.getMenuOption()))) {
            event.consume(); warn(activity + " is outside the historical rules. Participation blocked."); return;
        }
        NPC targetNpc = event.getMenuEntry().getNpc();
        if (restrictedNpc(targetNpc)) {
            event.consume();
            boolean confirmedModern = ContentRules.modernMaster(targetNpc.getName())
                || rules.npcStatus(targetNpc.getId()) == RuleBook.Status.BLOCKED
                || interactions.blocked("NPC", targetNpc.getName(), event.getMenuOption());
            warn(targetNpc.getName() + (confirmedModern
                ? " was released after 22 June 2005. Action blocked."
                : " is unverified for 22 June 2005. Action blocked."));
            return;
        }
        if (config.contentRestrictions() && (ContentRules.forbiddenLabel(event.getMenuTarget()) || questPolicy.forbidden(event.getMenuTarget()))) {
            event.consume(); warn("That content is outside the historical rules or unverified."); return;
        }
        rememberAcquisitionIntent(event);
        if (event.isConsumed()) { return; }
        rememberTransfer(event);
        Widget selected = client.getSelectedWidget();
        if (config.blockUse() && event.getMenuAction().name().contains("WIDGET_TARGET") && selected != null
            && selected.getItemId() >= 0 && restricted(selected.getItemId())) {
            event.consume(); warn("That selected item is restricted."); return;
        }
        boolean isPickup = pickup(event.getMenuEntry());
        int id = isPickup ? event.getId() : event.getItemId();
        boolean itemAction = event.isItemOp() || event.getMenuAction().name().contains("WIDGET_TARGET") || "use".equalsIgnoreCase(event.getMenuOption())
            || ContentRules.normalize(event.getMenuOption()).startsWith("buy-") || "buy".equalsIgnoreCase(event.getMenuOption());
        if (id < 0 || (!isPickup && !itemAction) || !restricted(id)) { return; }
        String option = event.getMenuOption().toLowerCase(Locale.ROOT);
        if (option.equals("drop") || option.equals("destroy") || option.equals("examine")) { return; }
        if (isPickup || config.blockUse()) {
            event.consume();
            String name = itemManager.getItemComposition(id).getName();
            warn(name + (quarantined(id) ? " was received from a restricted source. Dispose of it before using that item type."
                : rules.status(canonical(id)) == RuleBook.Status.UNKNOWN
                ? " is unverified for 22 June 2005." : " was released after 22 June 2005."));
        }
    }

    // One authoritative loot event channel. Do not also count ItemSpawned: public drops,
    // player drops, stack changes and region loading would inflate lifetime totals.
    @Subscribe public void onLootReceived(LootReceived event) {
        if (state == null || client.getGameState() != GameState.LOGGED_IN) { return; }
        RuleBook.Status source = RuleBook.Status.UNKNOWN;
        if (event.getType().name().equals("NPC")) {
            Object metadata = event.getMetadata();
            Object npcId = metadata instanceof Map ? ((Map<?, ?>) metadata).get("id") : metadata;
            if (npcId instanceof Number) { source = rules.npcStatus(((Number) npcId).intValue()); }
              if (npcId instanceof Number && modernEntities.npc(((Number) npcId).intValue()) != null) { source = RuleBook.Status.BLOCKED; }
        } else { source = ContentRules.rewardSource(event.getName()); }
        if (restrictedContext() != null) {
            source = RuleBook.Status.BLOCKED;
        }
        boolean changed = false;
        for (ItemStack stack : event.getItems()) {
            if (stack.getQuantity() <= 0) { continue; }
            int id = canonical(stack.getId());
            // Unknown is a research gap, not confirmed historical missed loot.
            if (!restricted(id) && source != RuleBook.Status.BLOCKED) {
                if (source == RuleBook.Status.ALLOWED && rules.status(id) == RuleBook.Status.ALLOWED) { evidence.proof(id, client.getTickCount(), event.getName()); }
                continue;
            }
            if (source == RuleBook.Status.BLOCKED) { evidence.proof(id, client.getTickCount(), "BLOCKED:" + event.getName()); }
            long count = missedEvidence.offer(id, stack.getQuantity(), client.getTickCount());
            if (count > 0) { recordMissed(id, count); changed = true; }
        }
        if (changed) { save(); }
        creditMatched();
    }
    private void creditMatched() {
        if (state == null || settlingTicks > 0) { return; }
        boolean changed = false;
        for (Map.Entry<Integer, String> match : evidence.matchSources(client.getTickCount()).entrySet()) {
            int id = match.getKey();
            unprovenGains.remove(id);
            if (!match.getValue().startsWith("BLOCKED:") && freshReceipts.remove(id)) {
                state.pendingReceipts.remove(id); changed = true;
            }
            if (match.getValue().startsWith("BLOCKED:")) {
                changed |= state.quarantined.add(id);
                state.pendingReceipts.remove(id); state.blockedSources.put(id, match.getValue().substring(8));
                changed = true;
                long count = receiptQuantities.getOrDefault(id, 0L);
                if (count > 0) { count = missedEvidence.receipt(id, count, client.getTickCount()); }
                if (count > 0) { recordMissed(id, count); changed = true; }
                warn("Restricted-source receipt: " + itemManager.getItemComposition(id).getName() + ". That item type is quarantined.");
                applyModel(itemManager.getItemComposition(id)); presentation.clearCaches();
            } else if (!match.getValue().startsWith("TRANSFER:") && rules.status(id) == RuleBook.Status.ALLOWED && !quarantined(id)) {
                // Record every witnessed acquisition, so later checklist edits work.
                changed |= state.acquired.add(id);
                state.acquisitionSources.putIfAbsent(id, match.getValue());
            }
            receiptQuantities.remove(id); freshReceipts.remove(id);
        }
        if (changed) { save(); }
    }
    private void rememberTransfer(MenuOptionClicked event) {
        if (state == null || client.getLocalPlayer() == null) { return; }
        if ("drop".equals(ContentRules.normalize(event.getMenuOption())) && event.getItemId() >= 0) {
            ownDrops.put(canonical(event.getItemId()), client.getLocalPlayer().getWorldLocation());
        } else if (pickup(event.getMenuEntry())) {
            int id = canonical(event.getId());
            net.runelite.api.coords.WorldPoint point = ownDrops.get(id);
            if (point == null) { return; }
            boolean sameTile = ground.entrySet().stream().anyMatch(entry -> canonical(entry.getKey().getId()) == id
                && entry.getValue().getWorldLocation().equals(point)
                && entry.getValue().getSceneLocation().getX() == event.getParam0()
                && entry.getValue().getSceneLocation().getY() == event.getParam1());
            if (sameTile) { evidence.proof(id, client.getTickCount(), "TRANSFER:Own dropped item"); ownDrops.remove(id); }
        }
    }
    private void recordMissed(int id, long quantity) {
        long price = state.marketValue ? itemManager.getItemPrice(id) : itemManager.getItemComposition(id).getHaPrice();
        long value = state.miss(quantity, Math.max(price, 0));
        chat("You would have received " + quantity + " x " + itemManager.getItemComposition(id).getName()
            + " (Value: " + String.format("%,d", value) + " gp; " + (state.marketValue ? "cached GE" : "High Alchemy")
            + "). Total missed loot: " + String.format("%,d", state.missedValue) + " gp.");
    }
    private void reviewUnprovenGains() {
        boolean changed = false;
        for (int id : new HashSet<>(unprovenGains.keySet())) {
            if (unprovenGains.get(id) >= client.getTickCount()) { continue; }
            unprovenGains.remove(id); receiptQuantities.remove(id);
            if (!config.strictSources() || state.blockedSources.containsKey(id)) { continue; }
            state.pendingReceipts.putIfAbsent(id, new ChallengeState.Receipt());
            freshReceipts.remove(id);
            boolean newlyRestricted = state.quarantined.add(id);
            changed |= newlyRestricted;
            applyModel(itemManager.getItemComposition(id));
            if (newlyRestricted) { warn("New receipt needs source review: " + itemManager.getItemComposition(id).getName() + ". Review it in the 2005Scape sidebar."); }
        }
        if (changed) { save(); presentation.clearCaches(); }
    }
    private void confirmReceipt(int id, String source) {
        if (state == null || client.getGameState() != GameState.LOGGED_IN || source == null || source.trim().isEmpty()
            || !state.pendingReceipts.containsKey(id) || state.blockedSources.containsKey(id) || rules.status(id) != RuleBook.Status.ALLOWED) { return; }
        state.pendingReceipts.remove(id); state.quarantined.remove(id);
        unprovenGains.remove(id); freshReceipts.remove(id); receiptQuantities.remove(id);
        state.acquired.add(id); state.acquisitionSources.put(id, "Manually confirmed historical source: " + source.trim());
        save(); presentation.restore(); presentation.start(config.classicFrame()); scanGround();
        chat("Historical source confirmed for " + itemManager.getItemComposition(id).getName() + ".");
        panel.update(rules, state, notice, slayer.advise(rules));
    }
    private void declineReceipt(int id) {
        if (state == null || !state.pendingReceipts.containsKey(id)) { return; }
        state.pendingReceipts.remove(id); state.blockedSources.put(id, "Unverified source kept restricted");
        unprovenGains.remove(id); freshReceipts.remove(id); receiptQuantities.remove(id);
        state.quarantined.add(id); save();
        panel.update(rules, state, notice, slayer.advise(rules));
    }
    private void warn(String text) {
        if (client.getTickCount() - lastWarningTick >= 3) { chat(text); lastWarningTick = client.getTickCount(); }
    }
    private String restrictedContext() {
        if (!config.contentRestrictions()) { return null; }
        String context = activityRules.visible(client.getWidgetRoots());
        if (context != null || client.getLocalPlayer() == null) { return context; }
        return areas.restricted(net.runelite.api.coords.WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()));
    }
    private boolean restrictedNpc(NPC npc) {
        if (!config.contentRestrictions() || npc == null) { return false; }
        if (ContentRules.modernMaster(npc.getName())) { return true; }
        if (interactions.blocked("NPC", npc.getName(), "Talk-to")) { return true; }
        RuleBook.Status status = rules.npcStatus(npc.getId());
        if (status == RuleBook.Status.BLOCKED) { return true; }
        if (npc.getCombatLevel() <= 0 || ContentRules.historicalMaster(npc.getName())) { return false; }
        return status == RuleBook.Status.BLOCKED || (status == RuleBook.Status.UNKNOWN && config.strictUnknown());
    }
    private boolean hideEntry(MenuEntry entry) {
        if (ContentRules.normalize(entry.getOption()).equals("examine")) { return false; }
        // Keep Attack selectable so the click can show the reason and be consumed.
        // Restriction is enforced before an action is sent, not by hiding the NPC.
        if (entry.getNpc() != null && ContentRules.normalize(entry.getOption()).equals("attack")) { return false; }
        if (config.hidePickup() && pickup(entry) && restricted(entry.getIdentifier())) { return true; }
        return config.contentRestrictions() && (restrictedNpc(entry.getNpc()) || (ContentRules.forbiddenLabel(entry.getTarget()) || questPolicy.forbidden(entry.getTarget())));
    }
    @Subscribe(priority = -100) public void onPostMenuSort(PostMenuSort event) {
        client.setMenuEntries(Arrays.stream(client.getMenuEntries()).filter(entry -> !hideEntry(entry)).toArray(MenuEntry[]::new));
    }
    @Subscribe public void onConfigChanged(ConfigChanged event) {
        if (!Scape2005Config.GROUP.equals(event.getGroup()) || !Arrays.asList("strictUnknown", "hidePickup", "blockUse",
            "classicFrame", "hideModels", "historicalLabels", "contentRestrictions", "strictSources").contains(event.getKey())) { return; }
        clientThread.invokeLater(() -> {
            if (active) {
                if (state != null && event.getKey().equals("strictSources")) {
                    if (config.strictSources()) { state.quarantined.addAll(state.pendingReceipts.keySet()); }
                    else { state.pendingReceipts.keySet().stream().filter(id -> !state.blockedSources.containsKey(id)).forEach(state.quarantined::remove); }
                    save();
                }
                presentation.restore(); presentation.start(config.classicFrame()); scanGround();
                if (!config.historicalLabels()) { restoreGroundOverlays(); }
            }
        });
    }
    private void applyModel(ItemComposition item) {
        if (active && config.hideModels()) {
            RuleBook.Status status = rules.status(item.getId());
            if (quarantined(item.getId()) || status == RuleBook.Status.BLOCKED || (status == RuleBook.Status.UNKNOWN && config.strictUnknown())) { presentation.hideModel(item); }
        }
    }
    @Subscribe public void onPostItemComposition(PostItemComposition event) { if (active && rules != null) { applyModel(event.getItemComposition()); } }
    @Subscribe public void onItemSpawned(ItemSpawned event) { ground.put(event.getItem(), event.getTile()); applyModel(itemManager.getItemComposition(event.getItem().getId())); }
    @Subscribe public void onItemDespawned(ItemDespawned event) { ground.remove(event.getItem()); }
    private void scanGround() {
        ground.clear(); Scene scene = client.getScene();
        if (scene == null) { return; }
        for (Tile[][] plane : scene.getTiles()) {
            for (Tile[] column : plane) {
                for (Tile tile : column) {
                    if (tile == null || tile.getGroundItems() == null) { continue; }
                    for (TileItem item : tile.getGroundItems()) { ground.put(item, tile); applyModel(itemManager.getItemComposition(item.getId())); }
                }
            }
        }
        presentation.clearCaches();
    }
    private void restoreGroundOverlays() {
        boolean enabled = plugins.getPlugins().stream().anyMatch(plugin -> plugin.getClass().getName().equals("net.runelite.client.plugins.grounditems.GroundItemsPlugin") && plugins.isPluginEnabled(plugin));
        if (enabled) { displacedOverlays.forEach(overlays::add); }
        displacedOverlays.clear();
    }
    private void chat(String text) {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[2005Scape] " + text, null);
    }
    RuleBook rules() { return rules; }
    ChallengeState state() { return state; }
    String notice() { return notice; }
    Map<TileItem, Tile> groundItems() { return ground; }
    boolean showHistoricalLabels() { return config.historicalLabels(); }
}
