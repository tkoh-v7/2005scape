package com.tkoh.scape2005;

import org.junit.Test;
import static org.junit.Assert.*;

public class GuardPoliciesTest {
    @Test public void modernSpellsAreExcludedButOldEscapeTeleportsRemain() {
        SpellPolicy spells = new SpellPolicy(new com.google.gson.Gson());
        assertTrue(spells.allowed("<col=00ff00>Varrock Teleport</col>"));
        assertTrue(spells.allowed("Ice Barrage"));
        assertTrue(spells.allowed("Carrallanger Teleport"));
        assertFalse(spells.allowed("Fire Surge"));
        assertFalse(spells.allowed("Lumbridge Home Teleport"));
        assertFalse(spells.allowed("Bones to Peaches"));
        assertTrue(spells.restrictedAction("Cast", "Fire Surge", 218, null));
        assertFalse(spells.restrictedAction("Cast", "Varrock Teleport", 218, null));
    }
    @Test public void modernQuestNamesAreRecognisedInsideDialogue() {
        QuestPolicy quests = new QuestPolicy(new com.google.gson.Gson());
        assertNotNull(quests.forbiddenMention("Would you like to start Recruitment Drive?"));
        assertNull(quests.forbiddenMention("Would you like to start The Lost Tribe?"));
        assertTrue(QuestPolicy.decline("No, thank you"));
    }
    @Test public void lostCityEntranceAndExitsStayAvailable() {
        InteractionPolicy interactions = new InteractionPolicy(new com.google.gson.Gson());
        assertTrue(interactions.blocked("OBJECT", "Fairy ring", "Configure"));
        assertFalse(interactions.blocked("OBJECT", "Fairy ring", "Enter"));
        assertFalse(interactions.blocked("NPC", "Ava", "Examine"));
        assertTrue(interactions.blocked("NPC", "Ava", "Talk-to"));
        assertTrue(ModernEntities.escape("Climb-up"));
        assertFalse(ModernEntities.escape("Attack"));
        assertTrue(interactions.modernDestination("Travel to Fossil Island"));
        assertFalse(interactions.modernDestination("Travel to Karamja"));
    }
    @Test public void exactModernEntityIdsDoNotDenyOldGenericMonsters() {
        ModernEntities entities = new ModernEntities(new com.google.gson.Gson());
        assertNotNull(entities.npc(135)); // POH hellhound, not the historical world hellhound.
        assertNotNull(entities.object(4515)); // Construction parlour.
        assertTrue(entities.exitObject(4525)); // Exit portal uses Enter, not Leave.
        assertNull(entities.npc(0));
    }
    @Test public void missedLootIsCountedOnceInEitherEventOrder() {
        MissedLootEvidence evidence = new MissedLootEvidence();
        assertEquals(10, evidence.offer(995, 10, 1));
        assertEquals(0, evidence.receipt(995, 4, 2));
        assertEquals(0, evidence.receipt(995, 6, 3));
        assertEquals(2, evidence.receipt(995, 2, 4));
        assertEquals(0, evidence.offer(995, 2, 5));
        evidence.clear();
        assertEquals(7, evidence.receipt(995, 7, 6));
        assertEquals(0, evidence.offer(995, 7, 7));
        assertEquals(7, evidence.offer(995, 7, 108));
    }
    @Test public void genericShopTitlesRequireHistoricalLocation() {
        AcquisitionRules rules = new AcquisitionRules(new com.google.gson.Gson());
        assertNotNull(rules.shop("General Store", 12850));
        assertNull(rules.shop("General Store", 9999));
        assertNotNull(rules.shop("Bob’s Brilliant Axes", 12850));
    }
    @Test public void modernAreasDoNotCatchOldNearbyDestinations() {
        AreaPolicy areas = new AreaPolicy(new com.google.gson.Gson());
        assertNotNull(areas.restricted(new net.runelite.api.coords.WorldPoint(3264, 6065, 0)));
        assertNotNull(areas.restricted(new net.runelite.api.coords.WorldPoint(3592, 3337, 0)));
        assertNull(areas.restricted(new net.runelite.api.coords.WorldPoint(3565, 3290, 0))); // Barrows
        assertNull(areas.restricted(new net.runelite.api.coords.WorldPoint(3489, 3288, 0))); // Mort'ton
        assertNull(areas.restricted(new net.runelite.api.coords.WorldPoint(1885, 4828, 0))); // Trawler
    }
}
