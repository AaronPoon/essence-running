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
	public void onFocusChanged(final FocusChanged event) {
		if (!event.isFocused()) {
			shiftModifier = false;
		}
	}

	@Provides
	EssenceRunningConfig provideConfig(final ConfigManager configManager) {
		return configManager.getConfig(EssenceRunningConfig.class);
	}

	@Subscribe
	public void onClientTick(final ClientTick clientTick) {

		// The menu is not rebuilt when it is open, so don't swap or else it will repeatedly swap entries
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) {
			return;
		}

		final MenuEntry[] menuEntries = client.getMenuEntries();

		// Build option map for quick lookup in findIndex
		int idx = 0;
		optionIndexes.clear();
		for (MenuEntry entry : menuEntries) {
			final String option = Text.removeTags(entry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}

		// Perform swaps
		idx = 0;
		for (MenuEntry entry : menuEntries) {
			swapMenuEntry(idx++, entry);
		}
	}

	private void swapMenuEntry(final int index, final MenuEntry menuEntry) {

		final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
		final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();

		if (config.swapOfferAll() && shiftModifier && option.equals("offer")) {
			swap("offer-all", option, target, index, true);
		}

		if (config.shiftClickCustomization() && shiftModifier && menuEntry.getType() == MenuAction.EXAMINE_ITEM.getId()) {
			shiftClickCustomization(target, index);
		}
	}

	private void shiftClickCustomization(final String target, final int index) {

		String optionA = null;

		if (target.equals("pure essence")) {
			optionA = config.pureEssence().getOption();
		}
		else if (target.equals("small pouch") || target.equals("medium pouch") || target.equals("large pouch") || target.equals("giant pouch")) {
			optionA = config.essencePouch().getOption();
		}
		else if (target.equals("binding necklace")) {
			optionA = config.bindingNecklace().getOption();
		}
		else if (target.startsWith("ring of dueling")) {
			optionA = config.ringOfDueling().getOption();
		}
		else if (target.startsWith("stamina potion")) {
			optionA = config.staminaPotion().getOption();
		}
		else if (target.startsWith("energy potion")) {
			optionA = config.staminaPotion().getOption();
		}
		else if (target.equals("earth talisman")) {
			optionA = config.earthTalisman().getOption();
		}
		else if (target.startsWith("crafting cape")) {
			optionA = config.craftingCape().getOption();
		}

		if (optionA != null) {
			swapPrevious(optionA.toLowerCase(), target, index);
		}
	}

	private void swapPrevious(final String optionA, // the desired option
							  final String target,
							  final int index) { // the index of examine

		if (index > 0) {
			// examine is always the last option for an item and the one before it is the default displayed option
			final MenuEntry previousEntry = client.getMenuEntries()[index - 1];
			final String previousOption = Text.removeTags(previousEntry.getOption()).toLowerCase();
			final String previousTarget = Text.removeTags(previousEntry.getTarget()).toLowerCase();

			if (target.equals(previousTarget) && !optionA.equals(previousOption)) {
				swap(optionA, previousOption, target, index - 1, true);
			}
		}
	}

	private void swap(final String optionA,
					  final String optionB,
					  final String target,
					  final int index,
					  final boolean strict) {

		final MenuEntry[] menuEntries = client.getMenuEntries();
		final int thisIndex = findIndex(menuEntries, index, optionB, target, strict);
		final int optionIdx = findIndex(menuEntries, thisIndex, optionA, target, strict);

		if (thisIndex >= 0 && optionIdx >= 0) {
			swap(optionIndexes, menuEntries, optionIdx, thisIndex);
		}
	}

	private int findIndex(final MenuEntry[] entries,
						  final int limit,
						  final String option,
						  final String target,
						  final boolean strict) {

		if (strict) {
			List<Integer> indexes = optionIndexes.get(option);

			// We want the last index which matches the target, as that is what is top-most on the menu
			for (int i = indexes.size() - 1; i >= 0; --i) {
				final int idx = indexes.get(i);
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
				final MenuEntry entry = entries[i];
				final String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
				final String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target)) {
					return i;
				}
			}

		}

		return -1;
	}

	private void swap(final ArrayListMultimap<String, Integer> optionIndexes,
					  final MenuEntry[] entries,
					  final int index1,
					  final int index2) {

		final MenuEntry entry = entries[index1];
		entries[index1] = entries[index2];
		entries[index2] = entry;

		client.setMenuEntries(entries);

		// Rebuild option indexes
		optionIndexes.clear();
		int idx = 0;
		for (MenuEntry menuEntry : entries) {
			final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(final MenuEntryAdded menuEntryAdded) {

		// The client sorts the MenuEntries for priority after the ClientTick event so have to swap bank in MenuEntryAdded event
		if (config.swapBankOp()) {
			swapBankOp(menuEntryAdded);
		}
		if (config.swapBankWithdrawOp()) {
			swapBankWithdrawOp(menuEntryAdded);
		}
	}

	private void swapBankWithdrawOp(final MenuEntryAdded menuEntryAdded) {

		final String target = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
		final EssenceRunningItem item = EssenceRunningItem.of(target);

		// Withdraw- op 1 is the current withdraw amount 1/5/10/x
		if (item != null && shiftModifier && menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 1
				&& menuEntryAdded.getOption().startsWith("Withdraw-")) {

			final MenuEntry[] menuEntries = client.getMenuEntries();
			final String withdrawQuantity = "Withdraw-" + item.getWithdrawQuantity();

			// Find the custom withdraw quantity option
			for (int i = menuEntries.length - 1; i >= 0; --i) {
				final MenuEntry entry = menuEntries[i];

				if (entry.getOption().equals(withdrawQuantity)) {
					menuEntries[i] = menuEntries[menuEntries.length - 1];
					menuEntries[menuEntries.length - 1] = entry;

					client.setMenuEntries(menuEntries);
					break;
				}
			}
		}
	}

	private void swapBankOp(final MenuEntryAdded menuEntryAdded) {

		// Deposit- op 2 is the current deposit amount 1/5/10/x
		if (shiftModifier && menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 2
				&& menuEntryAdded.getOption().startsWith("Deposit-")) {

			final MenuEntry[] menuEntries = client.getMenuEntries();

			// Find the extra menu option; they don't have fixed names, so check based on the menu identifier
			for (int i = menuEntries.length - 1; i >= 0; --i) {
				final MenuEntry entry = menuEntries[i];

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
