package com.tkoh.scape2005;

import org.junit.Test;
import static org.junit.Assert.*;

public class AcquisitionEvidenceTest {
    @Test public void neitherOfferedLootNorExistingPossessionIsSufficient() {
        AcquisitionEvidence evidence = new AcquisitionEvidence();
        evidence.proof(3140, 10); assertTrue(evidence.match(10).isEmpty());
        evidence.gain(4087, 10); assertTrue(evidence.match(10).isEmpty());
        evidence.gain(3140, 11); assertTrue(evidence.match(11).contains(3140));
        assertTrue(evidence.match(12).isEmpty());
    }
    @Test public void rewardMayArriveAfterInventoryNotification() {
        AcquisitionEvidence evidence = new AcquisitionEvidence();
        evidence.gain(3140, 10); evidence.proof(3140, 12);
        assertTrue(evidence.match(12).contains(3140));
    }
    @Test public void stalePossessionChangesAreNotMatched() {
        AcquisitionEvidence evidence = new AcquisitionEvidence();
        evidence.gain(3140, 1); evidence.proof(3140, 10);
        assertTrue(evidence.match(10).isEmpty());
    }
    @Test public void loginAndBankBaselinesClearProofs() {
        AcquisitionEvidence evidence = new AcquisitionEvidence();
        evidence.proof(3140, 1); evidence.clear(); evidence.gain(3140, 2);
        assertTrue(evidence.match(2).isEmpty());
    }
    @Test public void modernSourceCannotBeOverwrittenByHistoricalRecipe() {
        AcquisitionEvidence evidence = new AcquisitionEvidence();
        evidence.proof(4151, 10, "BLOCKED:Modern reward");
        evidence.proof(4151, 11, "Historical production recipe");
        evidence.gain(4151, 11);
        assertEquals("BLOCKED:Modern reward", evidence.matchSources(11).get(4151));
    }
}
