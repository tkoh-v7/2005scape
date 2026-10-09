/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class Scape2005Overlay extends OverlayPanel {
    private static final Color GOLD = new Color(255, 210, 96);
    private final Scape2005Plugin plugin;
    @Inject Scape2005Overlay(Scape2005Plugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setBackgroundColor(new Color(62, 52, 40, 235));
        panelComponent.setPreferredSize(new Dimension(270, 0));
    }
    @Override public Dimension render(Graphics2D graphics) {
        ChallengeState state = plugin.state();
        if (state == null) { return null; }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("2005Scape | 22 June 2005").color(GOLD).build());
        line("Missed loot", String.format("%,d gp", state.missedValue));
        line("Valuation", state.marketValue ? "Cached GE" : "High Alchemy");
        line("Database", plugin.rules().items().size() + " item records");
        long goalCount = plugin.rules().goals().stream().filter(goal -> goal.group.equals("Full dragon")).count();
        long achieved = plugin.rules().goals().stream().filter(goal -> goal.group.equals("Full dragon") && goal.complete(state.acquired)).count();
        line("Dragon set", achieved + " / " + goalCount + " slots");
        plugin.rules().goals().stream().filter(goal -> goal.group.equals("Full dragon"))
            .forEach(goal -> line(goal.name, goal.complete(state.acquired) ? "Obtained" : "To obtain"));
        line("Status", plugin.notice());
        return super.render(graphics);
    }
    private void line(String left, String right) {
        panelComponent.getChildren().add(LineComponent.builder().left(left).right(right)
            .leftColor(GOLD).rightColor(Color.WHITE).build());
    }
}
