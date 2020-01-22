package com.essencerunning;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Essence Running"
)
public class EssenceRunningPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EssenceRunningConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ShiftClickInputListener inputListener;

	@Setter
	private boolean shiftModifier = false;

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(inputListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(inputListener);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Provides
	EssenceRunningConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EssenceRunningConfig.class);
	}
}
