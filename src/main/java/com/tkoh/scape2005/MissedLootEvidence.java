/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Reconcile offered loot and an actual receipt in either event order. */
public final class MissedLootEvidence {
    private static final class Pending {
        long quantity; final int expiry;
        Pending(long quantity, int expiry) { this.quantity = quantity; this.expiry = expiry; }
    }
    private final Map<Integer, ArrayDeque<Pending>> offered = new HashMap<>();
    private final Map<Integer, ArrayDeque<Pending>> received = new HashMap<>();
    public void clear() { offered.clear(); received.clear(); }
    public long offer(int id, long quantity, int tick) { return reconcile(id, quantity, tick, received, offered); }
    public long receipt(int id, long quantity, int tick) { return reconcile(id, quantity, tick, offered, received); }
    private long reconcile(int id, long quantity, int tick, Map<Integer, ArrayDeque<Pending>> opposite, Map<Integer, ArrayDeque<Pending>> own) {
        if (quantity <= 0) { throw new IllegalArgumentException("Nonpositive loot quantity"); }
        prune(opposite, tick); prune(own, tick);
        long remaining = quantity;
        ArrayDeque<Pending> queue = opposite.get(id);
        while (remaining > 0 && queue != null && !queue.isEmpty()) {
            Pending pending = queue.peek(); long matched = Math.min(remaining, pending.quantity);
            remaining -= matched; pending.quantity -= matched;
            if (pending.quantity == 0) { queue.remove(); }
        }
        if (remaining > 0) { own.computeIfAbsent(id, ignored -> new ArrayDeque<>()).add(new Pending(remaining, tick + 100)); }
        return remaining;
    }
    private void prune(Map<Integer, ArrayDeque<Pending>> records, int tick) {
        records.values().forEach(queue -> queue.removeIf(pending -> pending.expiry < tick));
        records.values().removeIf(ArrayDeque::isEmpty);
    }
}
