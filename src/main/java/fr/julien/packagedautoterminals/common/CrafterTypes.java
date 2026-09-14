package fr.julien.packagedautoterminals.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thelm.packagedauto.api.IRecipeType;

/**
 * Mapping from "recipe type" to "machine that runs it".
 *
 * <p>This table is **hand written**, on purpose. Revision R1 gives the proof: the API does
 * not allow it to be derived.
 *
 * <ul>
 *   <li>{@code IPackageCraftingMachine.acceptPackage(…, boolean)} is not a simulation mode.
 *       Its default implementation delegates to the three-argument method, which actually
 *       runs the craft. No dry run is possible.
 *   <li>{@code IRecipeType.getRepresentation()} points at the station the craft comes from,
 *       for example the vanilla crafting table or the Extended Crafting one, not at the
 *       matching Package Crafter.
 * </ul>
 *
 * <p>Machines are named by the **name** of their class, never by the class itself: the mod
 * therefore compiles and runs without the addons. An unknown type yields no diagnostic at
 * all, rather than a wrong one.
 */
public final class CrafterTypes {

    private static final Map<ResourceLocation, String> MACHINE_BY_TYPE = new HashMap<>();

    /**
     * Block to show for a recipe type.
     *
     * <p>The tile class serves the diagnostic; it gives no icon. The registered block name,
     * on the other hand, yields an {@link ItemStack} even when no machine of that kind sits
     * on the network. These names come from the language files of the mods, read inside
     * their jars, not from memory.
     */
    private static final Map<ResourceLocation, ResourceLocation> BLOCK_BY_TYPE = new HashMap<>();

    /** Icons already built. Rendering goes through here on every frame. */
    private static final Map<ResourceLocation, ItemStack> ICON_CACHE = new HashMap<>();

    static {
        map("packagedauto", "crafting", "thelm.packagedauto.tile.TileCrafter",
                "packagedauto", "crafter");

        map("packagedexcrafting", "basic", "thelm.packagedexcrafting.tile.TileBasicCrafter",
                "packagedexcrafting", "basic_crafter");
        map("packagedexcrafting", "advanced", "thelm.packagedexcrafting.tile.TileAdvancedCrafter",
                "packagedexcrafting", "advanced_crafter");
        map("packagedexcrafting", "elite", "thelm.packagedexcrafting.tile.TileEliteCrafter",
                "packagedexcrafting", "elite_crafter");
        map("packagedexcrafting", "ultimate", "thelm.packagedexcrafting.tile.TileUltimateCrafter",
                "packagedexcrafting", "ultimate_crafter");
        map("packagedexcrafting", "combination", "thelm.packagedexcrafting.tile.TileCombinationCrafter",
                "packagedexcrafting", "combination_crafter");
        map("packagedexcrafting", "ender", "thelm.packagedexcrafting.tile.TileEnderCrafter",
                "packagedexcrafting", "ender_crafter");

        map("packagedavaritia", "extreme", "thelm.packagedavaritia.tile.TileExtremeCrafter",
                "packagedavaritia", "extreme_crafter");
    }

    /**
     * Machines that **forward** a package without crafting anything.
     *
     * <ul>
     *   <li>the Positioned Package Distributor sends each package to a marked position, for
     *       the {@code Positioned} recipe type;
     *   <li>the Package Crafting Machine Proxy relays to a machine designated by a marker,
     *       often too far away to reach the Unpackager.
     * </ul>
     *
     * <p>Both accept packages, so the API reports them as crafting machines. Mixing them
     * with crafters would suggest they can produce something.
     */
    private static final java.util.Set<String> ROUTERS = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "thelm.packagedauto.tile.TileDistributor",
                    "thelm.packagedauto.tile.TileCraftingProxy"));

    private CrafterTypes() {}

    /** Does this machine only route packages? */
    public static boolean isRouter(String machineClass) {
        return ROUTERS.contains(machineClass);
    }

    private static void map(String modid, String type, String machineClass,
                            String blockModid, String blockName) {
        ResourceLocation key = new ResourceLocation(modid, type);
        MACHINE_BY_TYPE.put(key, machineClass);
        BLOCK_BY_TYPE.put(key, new ResourceLocation(blockModid, blockName));
    }

    /**
     * Icon of the machine that runs this type, or {@link ItemStack#EMPTY}.
     *
     * <p>The result is empty in two cases: the type needs no known machine, such as
     * {@code processing}, or the addon that carries the machine is not installed. The caller
     * then falls back to the written name of the type.
     */
    public static ItemStack iconFor(IRecipeType type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation key = type.getName();
        ItemStack cached = ICON_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation block = BLOCK_BY_TYPE.get(key);
        ItemStack icon = ItemStack.EMPTY;
        if (block != null) {
            Item item = ForgeRegistries.ITEMS.getValue(block);
            if (item != null) {
                icon = new ItemStack(item);
            }
        }
        ICON_CACHE.put(key, icon);
        return icon;
    }

    /** Name of the expected machine class, or {@code null} if the type is unknown. */
    public static String machineClassFor(IRecipeType type) {
        return type == null ? null : MACHINE_BY_TYPE.get(type.getName());
    }

    /**
     * Does the type require a machine that we can recognise?
     *
     * <p>A type without a machine, such as {@code processing}, sends its packages to any
     * inventory: no diagnostic makes sense for it.
     */
    public static boolean isDiagnosable(IRecipeType type) {
        return type != null && type.hasMachine() && MACHINE_BY_TYPE.containsKey(type.getName());
    }

    /** Full table, read only. Useful for tests and for the startup log. */
    public static Map<ResourceLocation, String> all() {
        return Collections.unmodifiableMap(MACHINE_BY_TYPE);
    }
}
