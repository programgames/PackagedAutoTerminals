package fr.julien.packagedautoterminals.common;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.helpers.WirelessTerminalGuiObject;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

/**
 * Where the open terminal comes from: a part placed on a cable, or a wireless item.
 *
 * <p>This class exists so that the containers can ignore the difference. Both sources
 * implement {@link IActionHost}, hence give their grid node the same way. Only reopening the
 * screen differs, because {@code openGui} only carries three integers.
 *
 * <p>AE2 PITFALL, worked around here: {@code WirelessRegistry.openWirelessTerminalGui} casts
 * the handler returned by {@code IWirelessTermHandler.getGuiHandler} to {@code GuiBridge}, an
 * internal enum. A third-party mod going through that method would get a
 * {@code ClassCastException}. The wireless terminal therefore opens its own screen.
 */
public final class TerminalContext {

    private final IActionHost host;
    /** Wired part: position of the host tile. */
    private final BlockPos pos;
    /** Wired part: face the part sits on. */
    private final int side;
    /** Wireless item: slot in the player inventory. Otherwise -1. */
    private final int inventorySlot;

    private TerminalContext(IActionHost host, BlockPos pos, int side, int inventorySlot) {
        this.host = host;
        this.pos = pos;
        this.side = side;
        this.inventorySlot = inventorySlot;
    }

    public static TerminalContext ofPart(PartPatTerminal part) {
        return new TerminalContext(part, part.getTile().getPos(), part.getSide().ordinal(), -1);
    }

    public static TerminalContext ofWireless(WirelessTerminalGuiObject object) {
        return new TerminalContext(object, BlockPos.ORIGIN, 0, object.getInventorySlot());
    }

    public boolean isWireless() {
        return inventorySlot >= 0;
    }

    public IActionHost host() {
        return host;
    }

    /** Grid of the terminal, or {@code null} when it is linked to nothing. */
    public IGrid grid() {
        IGridNode node = host.getActionableNode();
        return node == null ? null : node.getGrid();
    }

    /**
     * Reason to close the screen, or {@code null} when everything is fine.
     *
     * <p>PITFALL fixed, first part: the range and energy checks existed, but no container
     * called them. Out of range, the wireless terminal stayed open and empty, and it never
     * drew any energy.
     *
     * <p>PITFALL fixed, second part: **the order of the three questions matters**. The
     * AE2UEL bytecode shows that {@code WirelessTerminalGuiObject.getActionableNode} calls
     * {@code rangeCheck} then returns {@code null} when no access point is in range. Asking
     * the grid first therefore reported a missing link every time the player walked away.
     * Range comes first.
     *
     * <p>Energy comes last, so it is only drawn when the rest is fine. Charging a terminal
     * that is out of range would make no sense.
     *
     * @param ticks number of ticks elapsed since the last call.
     */
    public String refusal(int ticks) {
        if (host instanceof WirelessTerminalGuiObject
                && !((WirelessTerminalGuiObject) host).rangeCheck()) {
            return "gui.packagedautoterminals.out_of_range";
        }
        if (grid() == null) {
            return "gui.packagedautoterminals.not_linked";
        }
        if (!drainPower(PatConfig.wirelessPowerPerTick * Math.max(1, ticks))) {
            return "gui.packagedautoterminals.no_power";
        }
        return null;
    }

    /**
     * Warns the player, then lets AE2 close the screen.
     *
     * <p>We do not call {@code closeScreen} ourselves in the middle of a container update.
     * `AEBaseContainer.canInteractWith` returns false as soon as the container is marked
     * invalid, and the server then closes the screen at its own pace.
     */
    public static void refuse(net.minecraft.entity.player.EntityPlayerMP player, String key) {
        player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentTranslation(key), false);
    }

    /**
     * Draws the energy of a wireless terminal.
     *
     * <p>The wired terminal draws nothing extra: the grid already pays for its node.
     *
     * @return true when the energy was available.
     */
    public boolean drainPower(double amount) {
        if (!(host instanceof WirelessTerminalGuiObject)) {
            return true;
        }
        WirelessTerminalGuiObject wireless = (WirelessTerminalGuiObject) host;
        return wireless.extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.CONFIG)
                >= amount - 0.01;
    }

    public void openTerminal(EntityPlayer player) {
        open(player, isWireless() ? PatGuiHandler.WIRELESS : PatGuiHandler.TERMINAL + side);
    }

    public void openEditor(EntityPlayer player) {
        open(player, isWireless()
                ? PatGuiHandler.WIRELESS_EDITOR
                : PatGuiHandler.EDITOR + side);
    }

    /**
     * For the part, {@code openGui} carries the block position. For the wireless item there
     * is no block: the inventory slot takes the place of the x coordinate.
     */
    private void open(EntityPlayer player, int id) {
        if (isWireless()) {
            player.openGui(PackagedAutoTerminals.instance, id, player.world, inventorySlot, 0, 0);
        } else {
            player.openGui(PackagedAutoTerminals.instance, id, player.world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
