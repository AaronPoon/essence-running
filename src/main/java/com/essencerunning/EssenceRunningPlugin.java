package com.essencerunning;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectID;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuShouldLeftClick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
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
    private KeyManager keyManager;
    @Inject
    private MouseManager mouseManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ShiftClickInputListener inputListener;
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
        keyManager.registerKeyListener(inputListener);
        mouseManager.registerMouseListener(inputListener);
        overlayManager.add(overlay);
        overlayManager.add(statisticsOverlay);
        overlayManager.add(clanChatOverlay);
        session = new EssenceRunningSession();
        MAX_ENTRIES = config.clanChatOverlayHeight().getOption();
        clanMessages = EssenceRunningUtils.getClanMessagesMap(MAX_ENTRIES);
    }

    @Override
    protected void shutDown() {
        keyManager.unregisterKeyListener(inputListener);
        mouseManager.unregisterMouseListener(inputListener);
        infoBoxManager.removeIf(t -> t instanceof EssenceRunningTickCounter);
        overlayManager.remove(overlay);
        overlayManager.remove(statisticsOverlay);
        overlayManager.remove(clanChatOverlay);
        session = null;
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

        if (config.shiftClickCustomization() && shiftModifier) {
            final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
            final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();

            if (config.swapOfferAll() && option.equals("offer")) {
                EssenceRunningUtils.swap(client, optionIndexes, "offer-all", option, target, index, true);
            }

            if (menuEntry.getType() == MenuAction.EXAMINE_ITEM.getId()) {
                shiftClickCustomization(target, index);
            }
        }
    }

    private void shiftClickCustomization(final String target, final int index) {

        String optionA = null;

        if (target.equals("pure essence")) {
            optionA = config.pureEssence().getOption();
        } else if (target.equals("small pouch") || target.equals("medium pouch") || target.equals("large pouch") || target.equals("giant pouch")) {
            optionA = config.essencePouch().getOption();
        } else if (target.equals("binding necklace")) {
            optionA = config.bindingNecklace().getOption();
        } else if (target.startsWith("ring of dueling")) {
            optionA = config.ringOfDueling().getOption();
        } else if (target.startsWith("stamina potion")) {
            optionA = config.staminaPotion().getOption();
        } else if (target.startsWith("energy potion")) {
            optionA = config.staminaPotion().getOption();
        } else if (target.equals("earth talisman")) {
            optionA = config.earthTalisman().getOption();
        } else if (target.startsWith("crafting cape")) {
            optionA = config.craftingCape().getOption();
        }

        if (optionA != null) {
            EssenceRunningUtils.swapPrevious(client, optionIndexes, optionA.toLowerCase(), target, index);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(final MenuEntryAdded menuEntryAdded) {
        if (config.shiftClickCustomization() && shiftModifier) {
            // The client sorts the MenuEntries for priority after the ClientTick event so have to swap bank in MenuEntryAdded event
            if (config.swapBankOp()) {
                final String target = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
                if (!target.equals("binding necklace") || !config.excludeBindingNecklaceOp()) {
                    EssenceRunningUtils.swapBankOp(client, menuEntryAdded);
                }
            }
            if (config.swapBankWithdrawOp()) {
                EssenceRunningUtils.swapBankWithdrawOp(client, menuEntryAdded);
            }
        }
    }

    @Subscribe
    public void onMenuShouldLeftClick(final MenuShouldLeftClick menuShouldLeftClick) {
        if (config.enableRunecrafterMode() && config.preventFireRunes()) {
            // Option is 'Craft-rune' on the Fire Altar
            EssenceRunningUtils.forceRightClick(client, menuShouldLeftClick, ObjectID.ALTAR_34764);
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
