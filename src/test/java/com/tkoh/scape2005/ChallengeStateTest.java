package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChallengeStateTest {
    @Test public void initialPossessionsDoNotCount() {
        ChallengeState.Acquisitions tracker = new ChallengeState.Acquisitions();
        assertTrue(tracker.update(Collections.singletonMap(3140, 1L)).isEmpty());
        assertTrue(tracker.update(Collections.singletonMap(3140, 1L)).isEmpty());
        assertTrue(tracker.update(Collections.singletonMap(3140, 2L)).contains(3140));
    }
    @Test public void transfersDoNotCountWhenAggregateIsUnchanged() {
        ChallengeState.Acquisitions tracker = new ChallengeState.Acquisitions();
        tracker.baseline(Collections.singletonMap(3140, 1L));
        assertTrue(tracker.update(Collections.singletonMap(3140, 1L)).isEmpty());
    }
    @Test public void loginBaselineDoesNotCreditExistingItems() {
        ChallengeState.Acquisitions tracker = new ChallengeState.Acquisitions();
        tracker.baseline(Collections.emptyMap());
        tracker.reset();
        assertTrue(tracker.update(Collections.singletonMap(3140, 1L)).isEmpty());
    }
    @Test public void quantityAndLifetimeSurviveSerialization() {
        ChallengeState state = new ChallengeState();
        assertEquals(6000000000L, state.miss(3, 2000000000));
        state.acquired.add(3140);
        state.acquisitionSources.put(3140, "Barrows");
        state.quarantined.add(4151);
        Gson gson = new Gson();
        ChallengeState loaded = gson.fromJson(gson.toJson(state), ChallengeState.class);
        assertEquals(6000000000L, loaded.missedValue);
        assertEquals(3L, loaded.missedQuantity);
        assertTrue(loaded.acquired.contains(3140));
        assertEquals("Barrows", loaded.acquisitionSources.get(3140));
        assertTrue(loaded.quarantined.contains(4151));
        assertEquals(state.started, loaded.started);
    }
    @Test(expected = IllegalArgumentException.class) public void negativeQuantityRejected() {
        new ChallengeState().miss(-1, 10);
    }
    @Test public void overflowDoesNotPartiallyMutateLedger() {
        ChallengeState state = new ChallengeState();
        state.missedQuantity = Long.MAX_VALUE;
        try { state.miss(1, 10); fail("Expected overflow"); }
        catch (ArithmeticException expected) { assertEquals(0, state.missedValue); }
    }
}
