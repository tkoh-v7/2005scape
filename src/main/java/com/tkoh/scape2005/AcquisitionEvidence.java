/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A goal needs a post-baseline possession gain and an attributed acquisition. */
public final class AcquisitionEvidence {
    private final Map<Integer, Integer> proofs = new HashMap<>();
    private final Map<Integer, Integer> gains = new HashMap<>();
    private final Map<Integer, String> sources = new HashMap<>();
    public void clear() { proofs.clear(); gains.clear(); sources.clear(); }
    public void proof(int id, int tick) { proof(id, tick, "Attributed reward"); }
    public void proof(int id, int tick, String source) {
        // A blocked receipt always takes priority over another same-item proof.
        if (sources.getOrDefault(id, "").startsWith("BLOCKED:") && proofs.getOrDefault(id, 0) >= tick) { return; }
        proofs.put(id, tick + 100); sources.put(id, source);
    }
    public void gain(int id, int tick) { gains.put(id, tick + 3); }
    public Set<Integer> match(int tick) {
        return new HashSet<>(matchSources(tick).keySet());
    }
    public Map<Integer, String> matchSources(int tick) {
        proofs.values().removeIf(expiry -> expiry < tick);
        gains.values().removeIf(expiry -> expiry < tick);
        sources.keySet().retainAll(proofs.keySet());
        Set<Integer> matched = new HashSet<>(proofs.keySet());
        matched.retainAll(gains.keySet());
        Map<Integer, String> result = new HashMap<>();
        matched.forEach(id -> { result.put(id, sources.remove(id)); proofs.remove(id); gains.remove(id); });
        return result;
    }
}
