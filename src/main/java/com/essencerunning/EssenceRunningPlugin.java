package com.essencerunning;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
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

	@Inject
	private MouseManager mouseManager;

	@Setter
	private boolean shiftModifier = false;

	private final ArrayListMultimap<String, Integer> optionIndexes = ArrayListMultimap.create();

	@Override
	protected void startUp() throws Exception {
		keyManager.registerKeyListener(inputListener);
		mouseManager.registerMouseListener(inputListener);
	}

	@Override
	protected void shutDown() throws Exception {
		keyManager.unregisterKeyListener(inputListener);
		mouseManager.unregisterMouseListener(inputListener);
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event) {
		if (!event.isFocused()) {
			shiftModifier = false;
		}
	}

	@Provides
	EssenceRunningConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(EssenceRunningConfig.class);
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick) {

		// The menu is not rebuilt when it is open, so don't swap or else it will
		// repeatedly swap entries
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) {
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();

		// Build option map for quick lookup in findIndex
		int idx = 0;
		optionIndexes.clear();
		for (MenuEntry entry : menuEntries) {
			String option = Text.removeTags(entry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}

		// Perform swaps
		idx = 0;
		for (MenuEntry entry : menuEntries) {
			swapMenuEntry(idx++, entry);
		}
	}

	private void swapMenuEntry(int index, MenuEntry menuEntry) {

		final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
		final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();

		if (config.swapOfferAll() && shiftModifier && option.equals("offer")) {
			swap("offer-all", option, target, index, true);
		}
	}

	private void swap(String optionA, String optionB, String target, int index, boolean strict) {

		MenuEntry[] menuEntries = client.getMenuEntries();

		int thisIndex = findIndex(menuEntries, index, optionB, target, strict);
		int optionIdx = findIndex(menuEntries, thisIndex, optionA, target, strict);

		if (thisIndex >= 0 && optionIdx >= 0) {
			swap(optionIndexes, menuEntries, optionIdx, thisIndex);
		}
	}

	private int findIndex(MenuEntry[] entries, int limit, String option, String target, boolean strict) {

		if (strict) {
			List<Integer> indexes = optionIndexes.get(option);

			// We want the last index which matches the target, as that is what is top-most
			// on the menu
			for (int i = indexes.size() - 1; i >= 0; --i) {
				int idx = indexes.get(i);
				MenuEntry entry = entries[idx];
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				// Limit to the last index which is prior to the current entry
				if (idx <= limit && entryTarget.equals(target)) {
					return idx;
				}
			}
		}
		else {
			// Without strict matching we have to iterate all entries up to the current limit...
			for (int i = limit; i >= 0; i--) {
				MenuEntry entry = entries[i];
				String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target)) {
					return i;
				}
			}

		}

		return -1;
	}

	private void swap(ArrayListMultimap<String, Integer> optionIndexes, MenuEntry[] entries, int index1, int index2) {

		MenuEntry entry = entries[index1];
		entries[index1] = entries[index2];
		entries[index2] = entry;

		client.setMenuEntries(entries);

		// Rebuild option indexes
		optionIndexes.clear();
		int idx = 0;
		for (MenuEntry menuEntry : entries) {
			String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {

		// The client sorts the MenuEntries for priority after the ClientTick event so have to swap bank in MenuEntryAdded event
		if (config.swapBankOp()) {
			swapBankOp(menuEntryAdded);
		}
		if (config.swapBankWithdrawOp()) {
			// Technically Withdraw-1 is always the same priority as current withdraw amount but Withdraw-X is a lower priority
			swapBankWithdrawOp(menuEntryAdded);
		}
	}

	private void swapBankWithdrawOp(MenuEntryAdded menuEntryAdded) {

		final String target = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
		final EssenceRunningItem item = EssenceRunningItem.of(target);

		// Withdraw- op 1 is the current withdraw amount 1/5/10/x
		if (item != null && shiftModifier && menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 1
				&& menuEntryAdded.getOption().startsWith("Withdraw-")) {

			MenuEntry[] menuEntries = client.getMenuEntries();
			final String withdrawQuantity = "Withdraw-" + item.getWithdrawQuantity();

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

			// Find the extra menu option; they don't have fixed names, so check based on the menu identifier
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
