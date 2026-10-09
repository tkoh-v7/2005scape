/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(Scape2005Config.GROUP)
public interface Scape2005Config extends Config {
    String GROUP = "scape2005";

    @ConfigItem(keyName = "strictUnknown", name = "Restrict unverified items",
        description = "Block items and combat NPCs with missing historical evidence or IDs outside the bundled snapshot.", position = 0)
    default boolean strictUnknown() { return true; }

    @ConfigItem(keyName = "hidePickup", name = "Hide restricted pickup options",
        description = "Remove restricted Take entries. Model hiding and ground labels have separate settings.", position = 1)
    default boolean hidePickup() { return true; }

    @ConfigItem(keyName = "blockUse", name = "Block restricted item actions",
        description = "Block ordinary item actions except Drop, Destroy and Examine. Not every interface is covered.", position = 2)
    default boolean blockUse() { return true; }

    @ConfigItem(keyName = "marketValue", name = "Use cached GE value",
        description = "For a NEW challenge: cached GE instead of local High Alchemy. Existing challenge basis stays fixed.", position = 3)
    default boolean marketValue() { return false; }

    @ConfigItem(keyName = "classicFrame", name = "2005 gameframe", description = "Temporarily use RuneLite's bundled 2005 interface style.", position = 4)
    default boolean classicFrame() { return true; }
    @ConfigItem(keyName = "hideModels", name = "Hide restricted models", description = "Hide restricted ground models and inventory sprites; worn models and hostile NPCs remain visible.", position = 5)
    default boolean hideModels() { return true; }
    @ConfigItem(keyName = "historicalLabels", name = "Historical ground labels", description = "Temporarily replace built-in Ground Items overlays with eligible labels only.", position = 6)
    default boolean historicalLabels() { return true; }
    @ConfigItem(keyName = "contentRestrictions", name = "Modern content restrictions", description = "Restrict dated modern combat NPCs, later Slayer masters and recognized later skills/prayers/spells.", position = 7)
    default boolean contentRestrictions() { return true; }
    @ConfigItem(keyName = "strictSources", name = "Review unknown acquisition sources",
        description = "Hold unverified new receipts for review. Confirm a historical source in the sidebar; starting possessions are not added.", position = 8)
    default boolean strictSources() { return true; }
}
