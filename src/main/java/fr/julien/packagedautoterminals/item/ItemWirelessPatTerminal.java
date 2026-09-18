package fr.julien.packagedautoterminals.item;

import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.ConfigManager;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.network.PacketOpenTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Wireless PackagedAuto terminal.
 *
 * <p>It opens its screen **itself**, and never calls
 * {@code WirelessRegistry.openWirelessTerminalGui}. Reason, verified in the AE2UEL bytecode:
 * that method casts the handler returned by {@link #getGuiHandler} to
 * {@code appeng.core.sync.GuiBridge}, an internal enum. Any third-party mod using it would
 * get a {@code ClassCastException}. {@code cell-terminal} makes the same choice, and opens
 * its screen through its own packet.
 *
 * <p>The rest of {@link IWirelessTermHandler} is useful: energy, link key and range are
 * handled by AE2, through {@code WirelessTerminalGuiObject}.
 */
public class ItemWirelessPatTerminal extends AEBasePoweredItem implements IWirelessTermHandler {

    /** Stored energy, in AE units. Same order of magnitude as the AE2 terminals. */
    private static final double POWER_CAPACITY = 1_600_000d;
    /** Cost of one opening, in AE units. */
    private static final double POWER_PER_OPEN = 0.5d;

    private static final String KEY_ENCRYPTION = "encryptionKey";

    public ItemWirelessPatTerminal() {
        super(POWER_CAPACITY);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (getEncryptionKey(stack).isEmpty()) {
            player.sendStatusMessage(
                    new TextComponentTranslation("gui.packagedautoterminals.not_linked"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!hasPower(player, POWER_PER_OPEN, stack)) {
            player.sendStatusMessage(
                    new TextComponentTranslation("gui.packagedautoterminals.no_power"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // FIXED: the slot used to be `currentItem` whatever the hand. A terminal used from the
        // offhand passed the **main hand** hotbar slot, and `PatGuiHandler.wireless` then read a
        // different stack than the one just checked: an empty hand opened nothing at all, and
        // another terminal in the main hand was opened in its place.
        int slot = hand == EnumHand.OFF_HAND
                ? PacketOpenTerminal.OFFHAND
                : player.inventory.currentItem;
        player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS, world, slot, 0, 0);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * The terminal does not replay its equip animation on every discharge.
     *
     * <p>Vanilla compares the old and the new stack, **NBT included**, to decide whether to
     * replay the animation. Energy lives in the NBT: with the open terminal discharging once
     * per second, the player's hand jumped on every draw.
     *
     * <p>Only a real item change, or a slot change, deserves the animation.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack,
                                               boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Same reason: a discharge must not interrupt the block being mined. */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    // --- IWirelessTermHandler ------------------------------------------------------

    @Override
    public boolean canHandle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack stack) {
        return extractAEPower(stack, amount, Actionable.MODULATE) >= amount - 0.01;
    }

    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack stack) {
        return getAECurrentPower(stack) >= amount;
    }

    @Override
    public IConfigManager getConfigManager(ItemStack stack) {
        // The terminal has no setting of its own. AE2 still calls this method: we must
        // therefore return an empty manager, never `null`.
        return new ConfigManager((manager, setting, value) -> { });
    }

    @Override
    public IGuiHandler getGuiHandler(ItemStack stack) {
        // Never used: see the explanation at the top of the class. AE2 would cast this.
        return null;
    }

    // --- INetworkEncodable ---------------------------------------------------------

    @Override
    public String getEncryptionKey(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? "" : tag.getString(KEY_ENCRYPTION);
    }

    @Override
    public void setEncryptionKey(ItemStack stack, String encryptionKey, String name) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_ENCRYPTION, encryptionKey);
    }

    /** Cost of one opening, read by the container to draw the energy. */
    public static double powerPerOpen() {
        return POWER_PER_OPEN;
    }

    /** Mode id for AE2 Wireless Universal Terminal, adjustable in the config. */
    public static byte wutMode() {
        return (byte) PatConfig.wutModeId;
    }
}
