package com.tkoh.scape2005;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Local development entry point; not a Jagex Launcher installation mechanism. */
public final class DevelopmentLauncher {
    private DevelopmentLauncher() { }
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(Scape2005Plugin.class);
        RuneLite.main(args);
    }
}
