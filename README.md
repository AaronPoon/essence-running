![](https://i.imgur.com/W01NA9z.png)
# Essence Running

## Background
[Release 1.6.0](https://runelite.net/blog/show/2019-12-19-1.6.0-Release) added support for external plugins to [RuneLite](https://runelite.net/). External plugins are plugins which are maintained by community members and other developers that are not officially affiliated with the project.

All external plugins are verified by RuneLite for safety, to ensure they are not malicious, and are also not breaking [Jagex's rules](https://secure.runescape.com/m=news/another-message-about-unofficial-clients?oldschool=1), which Runelite have agreed to as a project.

Developers submit their plugins to the [plugin hub repository](https://github.com/runelite/plugin-hub) for inclusion.

The plugin hub can be browsed in the client by scrolling to the bottom of the configuration panel.

## Purpose
There exists an entire community of OSRS players dedicated to running pure essence to a Runecrafter to boost their Runecrafting experience. The purpose of this plugin is to improve the QoL for essence runners.

This community can be found here: http://discord.gg/8Rznqh5

## API
### Enhanced ShiftClickInputListener
RuneLite's MenuEntrySwapperPlugin utilizes a `KeyListener` that listens for 'Shift' `KeyEvents`. These 'Shift' events are handled by saving the state of the 'Shift' key in an in-memory variable:
```java
private boolean shiftModifier = false;
```
When the 'Shift' key is pressed, `shiftModifier` is set to `true` and when the 'Shift' key is released, `shiftModifier` is set back to `false`. However, only the application that is in focus can receive KeyEvents. Therefore, if the 'Shift' key was pressed but was not released until after another application had already taken focus, this in-memory variable will get in a weird state where you are no longer pressing 'Shift' but `shiftModifier` is still `true`.

RuneLite handles this problem by simply always setting the `shiftModifier` back to `false` whenever it loses focus.
```java
@Subscribe
public void onFocusChanged(FocusChanged event)
{
    if (!event.isFocused())
    {
        shiftModifier = false;
    }
}
```
However, this has the opposite problem that can occur if RuneLite loses focus, 'Shift' is never released, and RuneLite regains focus while you are still physically pressing down 'Shift'. According to `shiftModifier`, you are not pressing 'Shift' so all the custom shift-click options are lost, even though you are still physically pressing 'Shift'.

This is entirely not desirable when you consider the Altscaping nature of essence runners. Therefore, I had to work around this limitation by introducing a `MouseListener` to my version of `ShiftClickInputListener`.
```java
@Override
public MouseEvent mousePressed(MouseEvent mouseEvent) {
    plugin.setShiftModifier(mouseEvent.isShiftDown());
    return mouseEvent;
}

@Override
public MouseEvent mouseEntered(final MouseEvent mouseEvent) {
    plugin.setShiftModifier(mouseEvent.isShiftDown());
    return mouseEvent;
}
```
The 'mouseEntered' event simply re-sets the `shiftModifier` whenever the mouse cursor enters the RuneLite client, regardless of whether the client has focus or not. Thus, regardless of what happened to the 'Shift' key while RuneLite was not in focus, whenever you position your mouse back into RuneLite, it will recalculate what `shiftModifier` should really be.

In addition, 'mousePressed' is also needed in the situation where you enter the RuneLite client without 'Shift' being pressed and you press 'Shift' before the client regains focus. This will allow you have utilize the shift-click options even in this scenario. The tooltip will initially display the incorrect default click option but when you press the mouse, it will correctly swap to the shift-click option.

However, being as this is an external plugin, I can't utilize this new feature and also use the existing 'Customizable shift-click' feature in RuneLite. Alas, if there are any features that I wish to use in RuneLite's MenuEntrySwapperPlugin, I will have to clone them.

## Features
### Swap Offer-All
Swaps the 'Offer' option to 'Offer-All' when holding shift.

### Swap Bank Op
Swaps the extra menu option in banks (Wield, Eat, etc.) when holding shift. A clone of the existing 'Swap Bank Op' in RuneLite to make use of the enhanced shift-click input listener. In addition this excludes the swapping to 'Empty' because it is never desirable to empty a Runecraft pouch in banks while running essence.

### Swap Bank Withdraw Op
Swaps the 'Withdraw-' quantity to 'Withdraw-1' in banks of the following items when holding shift:
* Binding necklace
* Energy potion(1,2,3,4)
* Stamina potion (1,2,3,4)
* Ring of dueling(1,2,3,4,5,6,7,8)
* Earth talisman (swaps to 'Withdraw-2' if and only if '2' was the most recently used 'Withdraw-X' quantity

### Highlight Binding necklace
Highlights 'Binding necklace' in your inventory upon selected criteria:
* Off (turn the feature off)
* Equipped (if you do not have an item equipped in your amulet slot)
* 25 (if the trade screen displays your trading partner has 25 free inventory slots)
* 26 (if the trade screen displays your trading partner has 26 free inventory slots)

### Highlight Ring of dueling
Highlights all 'Ring of dueling(8)' in the bank interface with a red border if you do not have an item equipped in your ring slot.

### Highlight Trade Sent
Highlights chat box with a red border to indicate there is currently not an active trade request. Upon successfully sending a trade offer request, the border will change to green.

### Customizable shift-click
Allows customization of shift-clicks on items. A clone of the existing 'Customizable shift-click' in RuneLite to make use of the enhanced shift-click input listener. However, rather than completely cloning the entire framework, I simply limited the customization of shift-click items to the ones associated with essence running. These can be configured in the plugin directly:
* Pure essence
* (Small, Medium, Large, Giant) pouch
* Binding necklace
* Ring of dueling (1,2,3,4,5,6,7,8)
* Stamina potion (1,2,3,4) - also controls Energy potion (1,2,3,4)
* Earth talisman
* Crafting cape

### Prevent Fire runes
Forces menu to open when you click the Fire Altar if you would accidentally craft Fire runes.

### Session Statistics
Displays statistics such as Pure essence/Binding necklace traded per runner and Fire runes crafted.

## Sticky Keys
With this plugin, it is entirely possible to run essence with the 'Shift' key pressed at all times. Of course while it is possible to physically press 'Shift' down at all times, it is much simpler to turn on Sticky Keys. You can find many guides on how to use Sticky Keys online but the settings that I use for essence running are:
![sticky keys](https://i.imgur.com/G7rKqj0.png)
