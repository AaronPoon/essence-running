package com.essencerunning;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.Color;
import java.util.Map;

@Slf4j
@PluginDescriptor(
    name = "Essence Running"
)
public class EssenceRunningPlugin extends Plugin {

    private static final String SENDING_TRADE_OFFER = "Sending trade offer...";
    private static final String ACCEPTED_TRADE = "Accepted trade.";
    private static final String CRAFTED_FIRE_RUNES = "You bind the temple's power into fire runes.";
    private static final String WAITING_OTHER_PLAYER = "Waiting for other player...";
    private static final int FIRE_RUNE_EXPERIENCE = 7;
    private static final String MAGIC_IMBUE_EXPIRED_MESSAGE = "Your Magic Imbue charge has ended.";
    private static final String MAGIC_IMBUE_MESSAGE = "You are charged to combine runes!";
    private static final String MAGIC_IMBUE_WARNING = "Your Magic Imbue spell charge is running out...";
    private static final int MAGIC_IMBUE_DURATION = 20;
    private static final int MAGIC_IMBUE_WARNING_DURATION = 10;
    private final ArrayListMultimap<String, Integer> optionIndexes = ArrayListMultimap.create();
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private EssenceRunningConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private EssenceRunningOverlay overlay;
    @Inject
    private EssenceRunningStatisticsOverlay statisticsOverlay;
    @Inject
    private EssenceRunningClanChatOverlay clanChatOverlay;
    @Inject
    private SpriteManager spriteManager;
    @Inject
    private InfoBoxManager infoBoxManager;
    @Setter
    private boolean shiftModifier = false;
    @Getter
    private EssenceRunningSession session;

    @Getter
    private boolean ringEquipped = false;

    @Getter
    private boolean amuletEquipped = false;

    @Getter
    private boolean tradeSent = false;

    @Getter
    private Map<Integer, String> clanMessages;
    private int MAX_ENTRIES = 0;

    private int runecraftXp = 0;
    private boolean craftedFireRunes = false;

    private EssenceRunningTickCounter counter;
    private boolean isFirstMessage = false;
    private boolean isCounting = false;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        overlayManager.add(statisticsOverlay);
        overlayManager.add(clanChatOverlay);
        session = new EssenceRunningSession();
        MAX_ENTRIES = config.clanChatOverlayHeight().getOption();
        clanMessages = EssenceRunningUtils.getClanMessagesMap(MAX_ENTRIES);
    }

    @Override
    protected void shutDown() {
        infoBoxManager.removeIf(t -> t instanceof EssenceRunningTickCounter);
        overlayManager.remove(overlay);
        overlayManager.remove(statisticsOverlay);
        overlayManager.remove(clanChatOverlay);
        session = null;
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

        swapMenuEntry(menuEntries.length - 1, menuEntries[menuEntries.length - 1]);
    }

    private void swapMenuEntry(final int index, final MenuEntry menuEntry) {

        if (config.leftClickCustomization()) {
            final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
            final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();

            if (config.swapOfferAll() && option.equals("offer")) {
                EssenceRunningUtils.swap(client, optionIndexes, "offer-all", option, target, index, true);
            }

            leftClickCustomization(target, option, index);
        }
    }

    private void leftClickCustomization(final String target, final String option, final int index) {

        String optionA = null;

        if (client.getWidget(WidgetInfo.BANK_CONTAINER) != null) {
            final EssenceRunningItem item = EssenceRunningItem.of(target);
            if (config.swapBankWithdrawOp() && item != null) {
                optionA = "Withdraw-" + item.getWithdrawQuantity();
            }
        } else {
            if (target.equals("small pouch") || target.equals("medium pouch") || target.equals("large pouch") || target.equals("giant pouch") || target.equals("colossal pouch")) {
                optionA = config.essencePouch().getOption();
            } else if (target.equals("binding necklace")) {
                optionA = config.bindingNecklace().getOption();
            } else if (target.startsWith("ring of the elements")) {
                optionA = "Fire Altar";
            } else if (target.startsWith("ring of dueling")) {
                optionA = "PvP Arena";
            } else if (target.startsWith("crafting cape")) {
                optionA = config.craftingCape().getOption();
            } else if (target.equals("amulet of eternal glory")) {
                optionA = "Edgeville";
            }
        }

        if (optionA != null) {
            EssenceRunningUtils.swap(client, optionIndexes, optionA.toLowerCase(), option, target, index, true);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(final MenuEntryAdded menuEntryAdded) {
        if (config.leftClickCustomization()) {
            // The client sorts the MenuEntries for priority after the ClientTick event so have to swap bank in MenuEntryAdded event
            if (config.swapBankOp()) {
                final String target = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
                if (!target.startsWith("binding necklace") || !config.excludeBindingNecklaceOp()) {
                    EssenceRunningUtils.swapBankOp(client, menuEntryAdded);
                }
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        if (config.enableRunecrafterMode() && config.preventFireRunes()) {
            if (menuOptionClicked.getId() == ObjectID.ALTAR_34764 // Fire Altar
                    && menuOptionClicked.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION) { // Craft-rune
                menuOptionClicked.consume();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Essence Running has prevented you from accidentally creating Fire Runes!", "");
            }
        }
        if (config.enableRunnerMode() && config.preventTradeCancel()) {
            final String option = Text.removeTags(menuOptionClicked.getMenuOption()).toLowerCase();
            final String target = Text.removeTags(menuOptionClicked.getMenuTarget()).toLowerCase();
            final Widget textField = client.getWidget(334, 4);
            if (textField != null && textField.getText().equals(WAITING_OTHER_PLAYER) &&
                    ((target.startsWith("crafting cape") && option.equals("teleport"))
                            || (target.startsWith("ring of dueling") && option.equals("pvp arena"))
                            || (target.equals("ring of the elements") && (option.equals("last destination") || option.equals("fire altar")))
                            || (target.equals("amulet of eternal glory") && option.equals("edgeville"))
                            || (target.equals("small pouch") && option.equals("empty"))
                            || (target.equals("medium pouch") && option.equals("empty"))
                            || (target.equals("large pouch") && option.equals("empty"))
                            || (target.equals("giant pouch") && option.equals("empty"))
                            || (target.equals("colossal pouch") && option.equals("empty")))) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Essence Running has prevented you from accidentally cancelling the Trade!", "");
                menuOptionClicked.consume();
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(final GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            tradeSent = false;
            session.reset();
            runecraftXp = 0;
            craftedFireRunes = false;
            clanMessages.clear();
        } else if (event.getGameState() == GameState.LOGGED_IN) {
            amuletEquipped = EssenceRunningUtils.itemEquipped(client, EquipmentInventorySlot.AMULET);
            ringEquipped = EssenceRunningUtils.itemEquipped(client, EquipmentInventorySlot.RING);
        }
    }

    @Subscribe
    public void onItemContainerChanged(final ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.EQUIPMENT)) {
            amuletEquipped = EssenceRunningUtils.itemEquipped(client, EquipmentInventorySlot.AMULET);
            ringEquipped = EssenceRunningUtils.itemEquipped(client, EquipmentInventorySlot.RING);
        }
    }

    @Subscribe
    public void onChatMessage(final ChatMessage event) {
        if (event.getMessage().equals(SENDING_TRADE_OFFER)) {
            tradeSent = true;
        } else if (event.getMessage().equals(ACCEPTED_TRADE)) {
            if (config.enableRunecrafterMode() && config.sessionStatistics()) {
                // Trade widgets are still available at this point
                EssenceRunningUtils.computeItemsTraded(client, session);
            }
        } else if (event.getMessage().equals(CRAFTED_FIRE_RUNES)) {
            craftedFireRunes = true;
        } else if (event.getSender() != null) {
            final String message = event.getName() + ": " + event.getMessage();
            clanMessages.put(message.hashCode(), message);
        }

        if (config.filterTradeMessages() && event.getType() == ChatMessageType.TRADE) {
            ChatLineBuffer buffer = client.getChatLineMap().get(ChatMessageType.TRADE.getType());
            buffer.removeMessageNode(event.getMessageNode());
            clientThread.invoke(() -> client.runScript(ScriptID.BUILD_CHATBOX));
        }

        if (config.enableRunecrafterMode() && config.showAccurateMagicImbue() && event.getMessage().equals(MAGIC_IMBUE_MESSAGE))
        {
            createTickCounter(MAGIC_IMBUE_DURATION);
            isFirstMessage = true;
        }

        if (config.enableRunecrafterMode() && config.showAccurateMagicImbue() && event.getMessage().equals(MAGIC_IMBUE_WARNING))
        {
            if (isFirstMessage)
            {
                if (counter == null)
                    createTickCounter(MAGIC_IMBUE_WARNING_DURATION);
                else
                    isCounting = true;
                isFirstMessage = false;
            }
        }

        if (event.getMessage().equals(MAGIC_IMBUE_EXPIRED_MESSAGE))
        {
            removeTickCounter();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (counter == null)
        {
            return;
        }

        if (counter.getCount() > -1) {
            if (counter.getCount() == 0)
            {
                counter.setTextColor(Color.RED);
            }
            if (isCounting) {
                if (counter.getCount() == MAGIC_IMBUE_WARNING_DURATION + 1)
                {
                    isCounting = false;
                }
                counter.setCount(counter.getCount() - 1);
            }
        }
        else
        {
            removeTickCounter();
        }
    }

    @Subscribe
    public void onWidgetLoaded(final WidgetLoaded event) {
        if (event.getGroupId() == WidgetID.PLAYER_TRADE_SCREEN_GROUP_ID) {
            tradeSent = false;
        }
    }

    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
        if (event.getGroup().equals("essencerunning")) {
            if (!config.sessionStatistics() && (!session.getRunners().isEmpty() || session.getTotalFireRunesCrafted() != 0)) {
                session.reset();
            }
            if (MAX_ENTRIES != config.clanChatOverlayHeight().getOption()) {
                MAX_ENTRIES = config.clanChatOverlayHeight().getOption();
                Map<Integer, String> temp = EssenceRunningUtils.getClanMessagesMap(MAX_ENTRIES);
                temp.putAll(clanMessages);
                clanMessages = temp;
            }
            if (!config.showAccurateMagicImbue() || !config.enableRunecrafterMode())
            {
                removeTickCounter();
            }
        }
    }

    @Subscribe
    public void onStatChanged(final StatChanged statChanged) {
        if (statChanged.getSkill() == Skill.RUNECRAFT) {
            if (config.enableRunecrafterMode() && config.sessionStatistics()) {
                if (craftedFireRunes) {
                    session.updateCrafterStatistic((statChanged.getXp() - runecraftXp) / FIRE_RUNE_EXPERIENCE);
                    craftedFireRunes = false;
                }
                runecraftXp = statChanged.getXp();
            }
        }
    }

    private void createTickCounter(int duration)
    {
        if (counter == null)
        {
            counter = new EssenceRunningTickCounter(null, this, duration);
            spriteManager.getSpriteAsync(SpriteID.SPELL_MAGIC_IMBUE, 0, counter);
            counter.setTooltip("Magic imbue");
            infoBoxManager.addInfoBox(counter);
            isCounting = true;
        }
        else
        {
            counter.setCount(duration);
            counter.setTextColor(Color.WHITE);
        }
    }

    private void removeTickCounter()
    {
        if (counter == null)
        {
            return;
        }

        infoBoxManager.removeInfoBox(counter);
        counter = null;
        isCounting = false;
    }
}
