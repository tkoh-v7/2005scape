/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.runelite.api.coords.WorldPoint;

/** Conservative bounds around recognised modern areas; not a historical map. */
public final class AreaPolicy {
    private static final class Area { String name; int minX, maxX, minY, maxY; String source; }
    private final Area[] areas;
    public AreaPolicy(Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(AreaPolicy.class.getResourceAsStream("/historical-areas.json"), StandardCharsets.UTF_8)) {
            areas = gson.fromJson(reader, Area[].class);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not load area rules", ex); }
    }
    public String restricted(WorldPoint point) {
        if (point == null) { return null; }
        for (Area area : areas) {
            if (point.getX() >= area.minX && point.getX() <= area.maxX && point.getY() >= area.minY && point.getY() <= area.maxY) { return area.name; }
        }
        return null;
    }
}
