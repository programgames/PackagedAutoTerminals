package fr.julien.packagedautoterminals.integration.wut;

import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.PatItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;

/**
 * Bridge to AE2 Wireless Universal Terminal, the mod that merges several wireless terminals
 * into a single item.
 *
 * <p>AE2WUT exposes **no** extension interface. The proof is in its bytecode, and revision
 * R4 of {@code docs/DECISIONS.md} records it:
 *
 * <ul>
 *   <li>{@code getAllMode()} builds a hard-coded list of integers, 0 to 9;
 *   <li>{@code getWirelessName(int)} is a {@code tableswitch} from 1 to 9;
 *   <li>{@code AllWUTRecipe.getIngredient()} fills a hard-coded table.
 * </ul>
 *
 * <p>Three injections are still enough, because all three methods return a value we can
 * extend. The rest of AE2WUT is already generic: the wheel reads the {@code modes} array of
 * the item, and the assembly recipe is built from the ingredient table.
 */
public final class WutSupport {

    /** Registry name of the AE2WUT item. */
    private static final ResourceLocation ITEM =
            new ResourceLocation(Reference.AE2WUT, "wireless_universal_terminal");

    /** Class of the AE2WUT item, reached through reflection only. */
    private static final String ITEM_CLASS =
            "com.circulation.ae2wut.item.ItemWirelessUniversalTerminal";

    private static final String KEY_MODE = "mode";
    private static final String KEY_MODES = "modes";

    /**
     * Id of our mode, read only once.
     *
     * <p>PITFALL: the injections run during the registry events, which come before the
     * {@code preInit} of our mod. Forge only fills {@link PatConfig} at {@code preInit}.
     * Reading the setting without care would give the default value at registration time,
     * then the configured value later: two ids for a single terminal. We therefore force the
     * file to be read, then keep the result.
     */
    private static Integer mode;

    private WutSupport() {}

    public static boolean isLoaded() {
        return Loader.isModLoaded(Reference.AE2WUT);
    }

    public static synchronized int mode() {
        if (mode == null) {
            try {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
            } catch (Throwable ignored) {
                // The file does not exist yet: the default value will do.
            }
            mode = PatConfig.wutModeId;
        }
        return mode;
    }

    /** One copy of our wireless terminal, for the assembly recipe. */
    public static ItemStack terminal() {
        return new ItemStack(PatItems.WIRELESS_TERMINAL);
    }

    /**
     * Name shown after the item name, in the AE2WUT style.
     *
     * <p>Translation goes through {@code util.text.translation.I18n}, not through the class
     * of the same name in {@code client.resources}. The latter does not exist on a dedicated
     * server: loading it there would stop the game. This one lives on both sides.
     */
    @SuppressWarnings("deprecation")
    public static String modeName() {
        return "§6("
                + I18n.translateToLocal("item.packagedautoterminals.wireless_pat_terminal.name")
                + ")";
    }

    public static boolean isUniversalTerminal(ItemStack stack) {
        return !stack.isEmpty() && ITEM.equals(stack.getItem().getRegistryName());
    }

    /**
     * Is this item a universal terminal set to our mode, and does it really carry it?
     *
     * <p>Both questions matter. {@code mode} says what the player picked with the wheel;
     * {@code modes} says what the item actually absorbed. Without the second one, a brand new
     * universal terminal would open our screen without ever having swallowed our terminal.
     */
    public static boolean isOurMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return hasOurMode(stack) && tag.getInteger(KEY_MODE) == mode();
    }

    /**
     * Does this universal terminal **show** our mode?
     *
     * <p>Rendering only looks at the current mode, not at the {@code modes} array. A creative
     * item, set to our mode without having absorbed us, must carry our icon rather than a
     * missing model.
     */
    public static boolean showsOurMode(ItemStack stack) {
        if (!isUniversalTerminal(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getInteger(KEY_MODE) == mode();
    }

    /**
     * Has this universal terminal **absorbed** our terminal, whatever its current mode?
     *
     * <p>The opening key relies on this question, not on {@link #isOurMode}. A player who
     * presses our key wants our terminal, without having to turn the wheel first.
     */
    public static boolean hasOurMode(ItemStack stack) {
        if (!isUniversalTerminal(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        for (int absorbed : tag.getIntArray(KEY_MODES)) {
            if (absorbed == mode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Switches a universal terminal to our mode.
     *
     * <p>Writing {@code mode} into the NBT is not enough. AE2WUT stores the crafting grid of
     * the mode being left into its cache, through {@code nbtChangeB}, then restores the one
     * of the requested mode, through {@code nbtChange}. Skipping those two calls would make
     * the player lose the grid of the crafting terminal they leave.
     *
     * <p>Both methods are called through reflection. AE2WUT is not on our compile path, and
     * must not be: see decision D33. If they disappear, we fall back to the plain write, and
     * the mod keeps working.
     */
    public static void switchToOurMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return;
        }
        if (!callWut(stack)) {
            tag.setInteger(KEY_MODE, mode());
        }
    }

    /** True when both AE2WUT methods ran successfully. */
    private static boolean callWut(ItemStack stack) {
        try {
            Class<?> type = Class.forName(ITEM_CLASS);
            Field instanceField = type.getField("INSTANCE");
            Object instance = instanceField.get(null);
            Method stash = type.getMethod("nbtChangeB", ItemStack.class);
            Method apply = type.getMethod("nbtChange", ItemStack.class, int.class);
            stash.invoke(instance, stack);
            apply.invoke(instance, stack, mode());
            return true;
        } catch (Throwable missing) {
            PackagedAutoTerminals.LOGGER.warn(
                    "AE2WUT changed shape: falling back to a plain mode switch.", missing);
            return false;
        }
    }
}
