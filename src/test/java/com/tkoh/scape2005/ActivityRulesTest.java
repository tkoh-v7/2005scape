package com.tkoh.scape2005;

import org.junit.Test;
import static org.junit.Assert.*;

public class ActivityRulesTest {
    @Test public void activityGuardPreservesMovementAndExits() {
        assertFalse(ActivityRules.participation("WALK", "Walk here"));
        assertFalse(ActivityRules.participation("GAME_OBJECT_FIRST_OPTION", "Exit"));
        assertFalse(ActivityRules.participation("GAME_OBJECT_FIRST_OPTION", "Climb-up"));
        assertTrue(ActivityRules.participation("GAME_OBJECT_FIRST_OPTION", "Chop"));
        assertTrue(ActivityRules.participation("NPC_SECOND_OPTION", "Attack"));
        assertFalse(ActivityRules.participation("CC_OP", "Eat"));
    }
    @Test public void historicalBarrowsIsNotMistakenForModernActivity() {
        ActivityRules rules = new ActivityRules(new com.google.gson.Gson());
        assertNull(rules.forInterface(24));
        assertEquals("Wintertodt", rules.forInterface(396));
    }
}
