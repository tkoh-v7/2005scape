package com.tkoh.scape2005;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class RuleBookTest {
    private RuleBook book(String rows) {
        return new RuleBook(new ByteArrayInputStream(rows.getBytes(StandardCharsets.UTF_8)), new com.google.gson.Gson());
    }
    private String row(int id, String date) {
        return "{\"id\":" + id + ",\"name\":\"Example\",\"released\":\"" + date
            + "\",\"source\":\"test fixture\"}";
    }
    @Test public void cutoffIsInclusive() {
        RuleBook rules = book("[" + row(1, "2005-06-21") + "," + row(2, "2005-06-22")
            + "," + row(3, "2005-06-23") + "]");
        assertEquals(RuleBook.Status.ALLOWED, rules.status(1));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(2));
        assertEquals(RuleBook.Status.BLOCKED, rules.status(3));
        assertEquals(RuleBook.Status.UNKNOWN, rules.status(4));
    }
    @Test(expected = IllegalArgumentException.class) public void duplicatesAreRejected() {
        book("[" + row(1, "2005-01-01") + "," + row(1, "2007-01-01") + "]");
    }
    @Test(expected = IllegalArgumentException.class) public void missingEvidenceIsRejected() {
        book("[{\"id\":1,\"name\":\"Example\",\"released\":\"2005-01-01\"}]");
    }
    @Test public void bundledGoalsPrecedeCutoff() {
        RuleBook rules = RuleBook.bundled(new com.google.gson.Gson());
        assertTrue(rules.items().size() > 24000);
        rules.goals().forEach(goal -> {
            for (int id : goal.alternatives) { assertEquals(goal.name, RuleBook.Status.ALLOWED, rules.status(id)); }
        });
        assertEquals(RuleBook.Status.BLOCKED, rules.status(11840));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(4151));
        assertEquals(RuleBook.Status.BLOCKED, rules.status(5698));
        assertEquals(RuleBook.Status.UNKNOWN, rules.status(20428));
        assertFalse(rules.isGoalItem(20428));
        assertEquals(RuleBook.Status.ALLOWED, rules.npcStatus(415));
    }
    @Test public void missingDateIsExplicitlyUnverified() {
        RuleBook rules = book("[{\"id\":1,\"name\":\"Example\",\"source\":\"test fixture\"}]");
        assertEquals(RuleBook.Status.UNKNOWN, rules.status(1));
    }
    @Test public void acceptedReplacementRetainsModernDateAndDoesNotAllowUpgrades() {
        RuleBook rules = RuleBook.bundled(new com.google.gson.Gson());
        assertEquals("2019-07-25", rules.item(23983).released);
        assertEquals(RuleBook.Status.ALLOWED, rules.status(23983));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(23985));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(24123));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(23991));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(23993));
        assertEquals(RuleBook.Status.ALLOWED, rules.status(24127));
        assertEquals(RuleBook.Status.BLOCKED, rules.status(24128));
        assertEquals(RuleBook.Status.BLOCKED, rules.status(23971));
    }
    @Test public void dragonLegsAndSkirtAreAlternatives() {
        RuleBook.Goal goal = new RuleBook.Goal();
        goal.alternatives = new int[] {4087, 4585};
        assertTrue(goal.complete(java.util.Collections.singleton(4087)));
        assertTrue(goal.complete(java.util.Collections.singleton(4585)));
        assertFalse(goal.complete(java.util.Collections.emptySet()));
    }
}
