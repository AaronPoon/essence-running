package com.essencerunning;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Essence Running"
)
public class EssenceRunningPlugin extends Plugin {

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
	protected void startUp() throws Exception {
		keyManager.registerKeyListener(inputListener);
	}

	@Override
	protected void shutDown() throws Exception {
		keyManager.unregisterKeyListener(inputListener);
	}

	@Provides
	EssenceRunningConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(EssenceRunningConfig.class);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {

		if (config.swapBankOp()) {
			swapBankOp(menuEntryAdded);
		}
		if (config.swapERBankQuantityOp()) {
			swapERBankQuantityOp(menuEntryAdded);
		}
	}

	private void swapERBankQuantityOp(MenuEntryAdded menuEntryAdded) {

		final String target = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
		final EssenceRunningItem item = EssenceRunningItem.of(target);

		// Withdraw- op 1 is the current withdraw amount 1/5/10/x
		if (item != null && shiftModifier && menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 1
				&& menuEntryAdded.getOption().startsWith("Withdraw-")) {

			MenuEntry[] menuEntries = client.getMenuEntries();
			final String withdrawQuantity = "Withdraw-" + item.getQuantity();

			// Find the custom withdraw quantity option
			for (int i = menuEntries.length - 1; i >= 0; --i) {
				MenuEntry entry = menuEntries[i];

				if (entry.getOption().equals(withdrawQuantity)) {
					menuEntries[i] = menuEntries[menuEntries.length - 1];
					menuEntries[menuEntries.length - 1] = entry;

					client.setMenuEntries(menuEntries);
					break;
				}
			}
		}
	}

	private void swapBankOp(MenuEntryAdded menuEntryAdded) {

		// Deposit- op 2 is the current deposit amount 1/5/10/x
		if (shiftModifier && menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 2
				&& menuEntryAdded.getOption().startsWith("Deposit-")) {

			MenuEntry[] menuEntries = client.getMenuEntries();

			// Find the extra menu option; they don't have fixed names, so check
			// based on the menu identifier
			for (int i = menuEntries.length - 1; i >= 0; --i) {
				MenuEntry entry = menuEntries[i];

				// The extra options are always option 9
				if (entry.getType() == MenuAction.CC_OP_LOW_PRIORITY.getId() && entry.getIdentifier() == 9
						&& !entry.getOption().equals("Empty")) { // exclude Runecraft pouch's "Empty" option

					// we must also raise the priority of the op so it doesn't get sorted later
					entry.setType(MenuAction.CC_OP.getId());

					menuEntries[i] = menuEntries[menuEntries.length - 1];
					menuEntries[menuEntries.length - 1] = entry;

					client.setMenuEntries(menuEntries);
					break;
				}
			}
		}
	}
}
