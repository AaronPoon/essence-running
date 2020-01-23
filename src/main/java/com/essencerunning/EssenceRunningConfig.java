package com.essencerunning;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("essencerunning")
public interface EssenceRunningConfig extends Config {

	@ConfigItem(
			keyName = "swapOfferAll",
			name = "Swap Offer-All",
			description = "Swaps the 'Offer' option to 'Offer-All' when holding shift"
	)
	default boolean swapOfferAll() {
		return false;
	}

	@ConfigItem(
			keyName = "swapBankOp",
			name = "Swap Bank Op",
			description = "Swaps the extra menu option in banks (Wield, Eat, etc.) when holding shift"
	)
	default boolean swapBankOp() {
		return false;
	}

	@ConfigItem(
			keyName = "swapBankWithdrawOp",
			name = "Swap Bank Withdraw Op",
			description = "Swaps the Withdraw/Deposit quantity of certain items (Ring of dueling, Binding necklace, etc.) when holding shift"
	)
	default boolean swapBankWithdrawOp() {
		return false;
	}
}
