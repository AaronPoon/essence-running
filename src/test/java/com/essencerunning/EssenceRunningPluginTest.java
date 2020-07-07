package com.essencerunning;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EssenceRunningPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(EssenceRunningPlugin.class);
        RuneLite.main(args);
    }
}
