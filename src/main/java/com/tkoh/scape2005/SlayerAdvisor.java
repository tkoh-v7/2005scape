/* SPDX-License-Identifier: GPL-3.0-only */
package com.tkoh.scape2005;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;

/** Reads RuneLite's detected task; never invents or sends a server assignment. */
public class SlayerAdvisor {
    @Inject private Client client;
    @Inject private ConfigManager configs;
    String advise(RuleBook rules) {
        String task = configs.getRSProfileConfiguration("slayer", "taskName");
        String amount = configs.getRSProfileConfiguration("slayer", "amount");
        String location = configs.getRSProfileConfiguration("slayer", "taskLocation");
        if (task == null || task.isEmpty() || "0".equals(amount)) { return "No active task detected"; }
        List<RuleBook.Npc> options = rules.slayerCandidates(task, client.getRealSkillLevel(Skill.SLAYER));
        if (options.isEmpty()) {
            return task + ": no eligible target in the snapshot. Check Burthorpe replacement; it may be refused and reset your streak.";
        }
        String names = options.stream().map(npc -> npc.name).distinct().limit(4).collect(Collectors.joining(", "));
        String result = task + ": " + names + ". Use a historical location.";
        if (location != null && !location.isEmpty()) {
            result += " Task requires " + location + "; do not ignore that location restriction.";
        }
        return result;
    }
}
