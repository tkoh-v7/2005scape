/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class HistoricalGroundOverlay extends Overlay {
    private final Scape2005Plugin plugin;
    @Inject private Client client;
    @Inject private ItemManager items;
    @Inject HistoricalGroundOverlay(Scape2005Plugin plugin) {
        this.plugin = plugin; setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE);
    }
    @Override public Dimension render(Graphics2D graphics) {
        if (!plugin.showHistoricalLabels() || client.getLocalPlayer() == null) { return null; }
        Map<Tile, Integer> offsets = new HashMap<>();
        for (Map.Entry<TileItem, Tile> entry : plugin.groundItems().entrySet()) {
            TileItem item = entry.getKey(); Tile tile = entry.getValue();
            if (tile.getPlane() != client.getPlane() || plugin.restricted(item.getId()) || item.getOwnership() == TileItem.OWNERSHIP_OTHER) { continue; }
            String text = items.getItemComposition(item.getId()).getName();
            if (item.getQuantity() > 1) { text += " (" + String.format("%,d", item.getQuantity()) + ")"; }
            int offset = offsets.getOrDefault(tile, 0); offsets.put(tile, offset + 14);
            Point point = Perspective.getCanvasTextLocation(client, graphics, tile.getLocalLocation(), text, 20 + offset);
            if (point != null) {
                graphics.setColor(Color.BLACK); graphics.drawString(text, point.getX() + 1, point.getY() + 1);
                graphics.setColor(new Color(255, 210, 96)); graphics.drawString(text, point.getX(), point.getY());
            }
        }
        return null;
    }
}
