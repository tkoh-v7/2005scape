/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Serialized separately for each RuneScape profile. */
public final class ChallengeState {
    public int schemaVersion = 1;
    public String started = Instant.now().toString();
    public long missedValue;
    public long missedQuantity;
    public boolean marketValue;
    public Set<Integer> acquired = new HashSet<>();
    public Map<Integer, String> acquisitionSources = new HashMap<>();
    // Same-ID units cannot be distinguished by the server; quarantine the entire
    // item type after a witnessed modern-source receipt until it is disposed of.
    public Set<Integer> quarantined = new HashSet<>();
    public Map<Integer, Receipt> pendingReceipts = new HashMap<>();
    public Map<Integer, String> blockedSources = new HashMap<>();
    public static final class Receipt {
        public String observed = Instant.now().toString();
        public String source = "Source not recognised";
    }

    public long miss(long quantity, long unitPrice) {
        if (quantity <= 0 || unitPrice < 0) { throw new IllegalArgumentException("Invalid loot value"); }
        long value = Math.multiplyExact(quantity, unitPrice);
        long nextValue = Math.addExact(missedValue, value);
        long nextQuantity = Math.addExact(missedQuantity, quantity);
        missedValue = nextValue;
        missedQuantity = nextQuantity;
        return value;
    }

    /** Holdings baseline includes inventory, equipment, and any visible bank container. */
    public static final class Acquisitions {
        private Map<Integer, Long> previous;
        public void baseline(Map<Integer, Long> holdings) { previous = new HashMap<>(holdings); }
        public void reset() { previous = null; }
        public Set<Integer> update(Map<Integer, Long> holdings) {
            Set<Integer> gained = new HashSet<>();
            if (previous != null) {
                holdings.forEach((id, count) -> {
                    if (count > previous.getOrDefault(id, 0L)) { gained.add(id); }
                });
            }
            baseline(holdings);
            return gained;
        }
    }
}
