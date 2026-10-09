package com.tkoh.scape2005;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuestPolicyTest {
    @Test public void	lastQuestsBeforeCutoffRemainAvailable() {
        QuestPolicy policy = new QuestPolicy(new com.google.gson.Gson());
        assertTrue(policy.allowed("The Giant Dwarf"));
        assertTrue(policy.allowed("The Lost Tribe"));
        assertFalse(policy.forbidden("Desert Treasure I"));
        assertTrue(policy.forbidden("Recruitment Drive"));
        assertTrue(policy.forbidden("Recipe for Disaster"));
    }
    @Test public void arbitraryTextIsNotMistakenForAQuest() {
        assertFalse(new QuestPolicy(new com.google.gson.Gson()).forbidden("Bank chest"));
        assertFalse(new QuestPolicy(new com.google.gson.Gson()).forbidden("Quest list"));
    }
}
