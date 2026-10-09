package com.tkoh.scape2005;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductionEvidenceTest {
    private Map<Integer, Long> holdings(long material, long output) {
        Map<Integer, Long> result = new HashMap<>(); result.put(1, material); result.put(2, output); return result;
    }
    private ProductionEvidence.Recipe recipe() {
        ProductionEvidence.Recipe recipe = new ProductionEvidence.Recipe();
        recipe.output = 2; recipe.inputs = Collections.singletonMap(1, 2); recipe.skill = "SMITHING"; recipe.xp = 25;
        return recipe;
    }
    @Test public void materialsAndXpAreRequiredTogether() {
        ProductionEvidence tracker = new ProductionEvidence();
        tracker.baseline(holdings(2, 0), Collections.singletonMap("SMITHING", 100));
        assertTrue(tracker.update(holdings(2, 1), Collections.singletonMap("SMITHING", 125), Arrays.asList(recipe())).isEmpty());
        tracker.baseline(holdings(2, 0), Collections.singletonMap("SMITHING", 100));
        assertTrue(tracker.update(holdings(0, 1), Collections.singletonMap("SMITHING", 100), Arrays.asList(recipe())).isEmpty());
        tracker.baseline(holdings(2, 0), Collections.singletonMap("SMITHING", 100));
        assertTrue(tracker.update(holdings(0, 1), Collections.singletonMap("SMITHING", 125), Arrays.asList(recipe())).contains(2));
    }
    @Test public void existingOutputsAndBankTransfersDoNotCount() {
        ProductionEvidence tracker = new ProductionEvidence();
        tracker.baseline(holdings(2, 1), Collections.singletonMap("SMITHING", 100));
        assertTrue(tracker.update(holdings(2, 1), Collections.singletonMap("SMITHING", 100), Arrays.asList(recipe())).isEmpty());
    }
    @Test public void batchesRequireEnoughMaterialsForEveryOutput() {
        ProductionEvidence tracker = new ProductionEvidence();
        tracker.baseline(holdings(2, 0), Collections.singletonMap("SMITHING", 100));
        assertTrue(tracker.update(holdings(0, 2), Collections.singletonMap("SMITHING", 150), Arrays.asList(recipe())).isEmpty());
    }
}
