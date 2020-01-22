package com.essencerunning;

import net.runelite.client.input.KeyListener;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

public class ShiftClickInputListener implements KeyListener {

    @Inject
    private EssenceRunningPlugin plugin;

    @Override
    public void keyTyped(final KeyEvent e) {

    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            plugin.setShiftModifier(true);
        }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            plugin.setShiftModifier(false);
        }
    }
}
