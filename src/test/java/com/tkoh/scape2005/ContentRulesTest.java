package com.tkoh.scape2005;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContentRulesTest {
    @Test public void laterRangedPrayersAreRestrictedDespiteLowRequirements() {
        assertTrue(ContentRules.forbiddenLabel("<col=ff00ff>Sharp Eye</col>"));
        assertTrue(ContentRules.forbiddenLabel("Eagle Eye"));
        assertFalse(ContentRules.forbiddenLabel("Protect from Missiles"));
    }
    @Test public void farmingFallsAfterJuneCutoff() {
        assertTrue(ContentRules.forbiddenLabel("Farming"));
        assertTrue(ContentRules.forbiddenLabel("Construction"));
        assertFalse(ContentRules.forbiddenLabel("Slayer"));
    }
    @Test public void oldAncientsAreNotBlockedWithLaterBooks() {
        assertFalse(ContentRules.forbiddenLabel("Ice Barrage"));
        assertTrue(ContentRules.forbiddenLabel("Level 1: Lumbridge Home Teleport"));
        assertTrue(ContentRules.forbiddenLabel("Resurrect Greater Ghost"));
    }
    @Test public void sourcesDistinguishUnknownAndModern() {
        assertEquals(RuleBook.Status.ALLOWED, ContentRules.rewardSource("Barrows"));
        assertEquals(RuleBook.Status.BLOCKED, ContentRules.rewardSource("Tempoross"));
        assertEquals(RuleBook.Status.UNKNOWN, ContentRules.rewardSource("Unknown chest"));
    }
}
