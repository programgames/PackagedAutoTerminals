package fr.julien.packagedautoterminals.item;

import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.ConfigManager;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.PatConfig;
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
 * Terminal PackagedAuto sans fil.
 *
 * <p>Il ouvre **lui-même** sa fenêtre, et n'appelle jamais
 * {@code WirelessRegistry.openWirelessTerminalGui}. Motif, vérifié dans le bytecode
 * d'AE2UEL : cette méthode caste le gestionnaire rendu par {@link #getGuiHandler} en
 * {@code appeng.core.sync.GuiBridge}, une énumération interne. Tout mod tiers qui
 * l'utiliserait recevrait une {@code ClassCastException}. C'est aussi le choix de
 * {@code cell-terminal}, qui ouvre sa fenêtre par son propre paquet.
 *
 * <p>Le reste de {@link IWirelessTermHandler} sert bien : l'énergie, la clé de liaison et la
 * portée sont gérées par AE2, via {@code WirelessTerminalGuiObject}.
 */
public class ItemWirelessPatTerminal extends AEBasePoweredItem implements IWirelessTermHandler {

    /** Énergie stockée, en unités AE. Même ordre de grandeur que les terminaux d'AE2. */
    private static final double POWER_CAPACITY = 1_600_000d;
    /** Coût d'une ouverture, en unités AE. */
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

        int slot = player.inventory.currentItem;
        player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS, world, slot, 0, 0);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * Le terminal ne rejoue pas son animation d'équipement à chaque décharge.
     *
     * <p>Vanilla compare l'ancienne et la nouvelle pile, **NBT compris**, pour décider de
     * rejouer l'animation. Or l'énergie vit dans le NBT : le terminal ouvert se déchargeant
     * une fois par seconde, la main du joueur sursautait à chaque prélèvement.
     *
     * <p>Seul un vrai changement d'objet, ou un changement d'emplacement, mérite l'animation.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack,
                                               boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Même motif : une décharge ne doit pas interrompre le minage en cours. */
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
        // Le terminal n'a aucun réglage propre. AE2 appelle pourtant cette méthode : il faut
        // donc rendre un gestionnaire vide, jamais `null`.
        return new ConfigManager((manager, setting, value) -> { });
    }

    @Override
    public IGuiHandler getGuiHandler(ItemStack stack) {
        // Jamais utilisé : voir l'explication en tête de classe. AE2 casterait ce résultat.
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

    /** Coût d'une ouverture, lu par le conteneur pour prélever l'énergie. */
    public static double powerPerOpen() {
        return POWER_PER_OPEN;
    }

    /** Identifiant de mode pour AE2 Wireless Universal Terminal, réglable en configuration. */
    public static byte wutMode() {
        return (byte) PatConfig.wutModeId;
    }
}
