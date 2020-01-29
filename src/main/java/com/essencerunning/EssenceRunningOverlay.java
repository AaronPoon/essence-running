package com.essencerunning;

import java.awt.*;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class EssenceRunningOverlay extends Overlay {

    private final EssenceRunningPlugin plugin;
    private final Client client;
    private final EssenceRunningConfig config;

    private static final String FREE_INVENTORY_SLOTS = " free inventory slots.";

    @Inject
    private EssenceRunningOverlay(final EssenceRunningPlugin plugin, final Client client, final EssenceRunningConfig config) {

        super(plugin);
        this.plugin = plugin;
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(final Graphics2D graphics) {

        switch(config.highlightBindingNecklace()) {
            case EQUIPPED:
                final Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
                if (!plugin.isAmuletEquipped() && !inventory.isHidden()) {
                    for (final WidgetItem item : inventory.getWidgetItems()) {
                        if (!item.getWidget().isHidden() && item.getId() == ItemID.BINDING_NECKLACE) {
                            drawShape(graphics, item.getCanvasBounds(), Color.RED);
                        }
                    }
                }
                break;
            case TWENTY_FIVE:
            case TWENTY_SIX:
                if (matchFreeInventorySlots()) {
                    drawWidgetChildren(graphics, client.getWidget(WidgetID.PLAYER_TRADE_INVENTORY_GROUP_ID, 0), ItemID.BINDING_NECKLACE);
                }
                break;
            default:
                break;
        }

        final Widget chatbox = client.getWidget(WidgetInfo.CHATBOX);
        if (config.highlightTradeSent() && chatbox != null && !chatbox.isHidden()) {
            drawShape(graphics, chatbox.getBounds(), plugin.isTradeSent() ? Color.GREEN : Color.RED);
        }

        if (config.highlightRingOfDueling() && !plugin.isRingEquipped()) {
            drawWidgetChildren(graphics, client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER), ItemID.RING_OF_DUELING8);
            drawWidgetChildren(graphics, client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER), ItemID.RING_OF_DUELING8);
        }

        return null;
    }

    private void drawWidgetChildren(final Graphics2D graphics, final Widget widget, final int itemId) {
        if (widget != null) {
            for (final Widget item : widget.getChildren()) {
                if (!item.isHidden() && item.getItemId() == itemId) {
                    drawShape(graphics, item.getBounds(), Color.RED);
                }
            }
        }
    }

    private void drawShape(final Graphics2D graphics, final Shape shape, final Color color) {
        final Color previousColor = graphics.getColor();
        graphics.setColor(color);
        graphics.draw(shape);
        graphics.setColor(previousColor);
    }

    private boolean matchFreeInventorySlots() {
        final Widget freeSlots = client.getWidget(WidgetID.PLAYER_TRADE_SCREEN_GROUP_ID, 9);
        return freeSlots != null && freeSlots.getText().endsWith(config.highlightBindingNecklace().getOption() + FREE_INVENTORY_SLOTS);
    }
}
