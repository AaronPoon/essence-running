package com.essencerunning;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("essencerunning")
public interface EssenceRunningConfig extends Config {

	@ConfigItem(
			position = 0,
			keyName = "swapOfferAll",
			name = "Swap Offer-All",
			description = "Swaps the 'Offer' option to 'Offer-All' when holding shift"
	)
	default boolean swapOfferAll() {
		return false;
	}

	@ConfigItem(
			position = 1,
			keyName = "swapBankOp",
			name = "Swap Bank Op",
			description = "Swaps the extra menu option in banks (Wield, Eat, etc.) when holding shift"
	)
	default boolean swapBankOp() {
		return false;
	}

	@ConfigItem(
			position = 2,
			keyName = "swapBankWithdrawOp",
			name = "Swap Bank Withdraw Op",
			description = "Swaps the Withdraw/Deposit quantity of certain items (Ring of dueling, Binding necklace, etc.) when holding shift"
	)
	default boolean swapBankWithdrawOp() {
		return false;
	}

	@ConfigItem(
			position = 10,
			keyName = "shiftClickCustomization",
			name = "Customizable shift-click",
			description = "Allows customization of shift-clicks on items below that persist even when RuneLite loses focus"
	)
	default boolean shiftClickCustomization() {
		return true;
	}

	@ConfigItem(
			position = 11,
			keyName = "pureEssence",
			name = "Pure essence",
			description = "Customize shift-click of 'Pure essence' in inventory"
	)
	default EssenceRunningItemShiftClick.RingOfDueling pureEssence() {
		return EssenceRunningItemShiftClick.RingOfDueling.USE;
	}

	@ConfigItem(
			position = 12,
			keyName = "essencePouch",
			name = "Essence pouch",
			description = "Customize shift-click of 'Essence pouch' in inventory"
	)
	default EssenceRunningItemShiftClick.EssencePouch essencePouch() {
		return EssenceRunningItemShiftClick.EssencePouch.EMPTY;
	}

	@ConfigItem(
			position = 13,
			keyName = "bindingNecklace",
			name = "Binding necklace",
			description = "Customize shift-click of 'Binding necklace' in inventory"
	)
	default EssenceRunningItemShiftClick.BindingNecklace bindingNecklace() {
		return EssenceRunningItemShiftClick.BindingNecklace.USE;
	}

	@ConfigItem(
			position = 14,
			keyName = "ringOfDueling",
			name = "Ring of dueling",
			description = "Customize shift-click of 'Ring of dueling' in inventory"
	)
	default EssenceRunningItemShiftClick.RingOfDueling ringOfDueling() {
		return EssenceRunningItemShiftClick.RingOfDueling.WEAR;
	}

	@ConfigItem(
			position = 15,
			keyName = "staminaPotion",
			name = "Stamina potion",
			description = "Customize shift-click of 'Stamina potion' and 'Energy potion' in inventory"
	)
	default EssenceRunningItemShiftClick.StaminaPotion staminaPotion() {
		return EssenceRunningItemShiftClick.StaminaPotion.DRINK;
	}

	@ConfigItem(
			position = 16,
			keyName = "earthTalisman",
			name = "Earth talisman",
			description = "Customize shift-click of 'Earth talisman' in inventory"
	)
	default EssenceRunningItemShiftClick.EarthTalisman earthTalisman() {
		return EssenceRunningItemShiftClick.EarthTalisman.USE;
	}

	@ConfigItem(
			position = 17,
			keyName = "craftingCape",
			name = "Crafting cape",
			description = "Customize shift-click of 'Crafting cape' in inventory"
	)
	default EssenceRunningItemShiftClick.CraftingCape craftingCape() {
		return EssenceRunningItemShiftClick.CraftingCape.TELEPORT;
	}
}
