package fr.julien.packagedautoterminals.common;

import fr.julien.packagedautoterminals.Reference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Fluids, the way PackagedAuto really carries them: an AE2FC <b>Fluid Packet</b>.
 *
 * <p>PackagedAuto stores a recipe as {@code ItemStack} lists, and knows nothing about fluids.
 * Its addon {@code PackagedFluidCrafting} fills that gap, and it does not use buckets. Read in
 * {@code thelm.packagedfluidcrafting.util.MixinHooks.packToPacket}: a dragged {@code FluidStack}
 * becomes {@code FakeFluids.packFluid2Packet(fluid)}, an item of AE2 Fluid Crafting. Read in
 * {@code GuiFluidAmountSpecifying.onOkButtonPressed}: the amount is edited in the packet, and the
 * stack keeps a count of one.
 *
 * <p>The packet is pure NBT, so this class builds it without compiling against AE2FC. Format read
 * in {@code com.glodblock.github.common.item.fake.FakeFluids$2.packStack}:
 *
 * <pre>
 * item  ae2fc:fluid_packet, count 1
 * nbt   { FluidStack: &lt;FluidStack.writeToNBT&gt; }
 * </pre>
 *
 * <p>Gases follow the same shape, in {@code ae2fc:gas_packet} under the tag {@code GasStack}. They
 * are left out: no Mekanism means no way to verify the gas format in the game, and working rule 1
 * forbids writing from memory.
 */
public final class FluidPackets {

    /** Upper bound of a fluid amount, taken from {@code MixinHooks.displayAmountSpecifyingGui}. */
    public static final int MAX_AMOUNT = 1_000_000_000;

    private static final ResourceLocation PACKET_ITEM =
            new ResourceLocation("ae2fc", "fluid_packet");
    private static final String FLUID_TAG = "FluidStack";

    /**
     * The packet item, looked up once and then kept.
     *
     * <p>A failed lookup is **not** cached. A call made before the item registry is filled would
     * otherwise answer "no AE2FC" for the whole session.
     */
    private static Item packetItem;

    private FluidPackets() {}

    /**
     * True when a fluid can enter a recipe.
     *
     * <p>Two conditions, and both are needed. AE2FC provides the packet item. PackagedFluidCrafting
     * teaches the Packager and the Crafter to read it, through
     * {@code ItemHandlerConverting}. Without the addon, a packet would sit in the recipe and no
     * machine would ever fill it.
     */
    public static boolean available() {
        return Loader.isModLoaded(Reference.PACKAGED_FLUID_CRAFTING) && item() != null;
    }

    /** The packet item, or {@code null} when AE2FC is absent. */
    private static Item item() {
        if (packetItem == null) {
            packetItem = ForgeRegistries.ITEMS.getValue(PACKET_ITEM);
        }
        return packetItem;
    }

    /** True when this stack is a fluid packet that carries a readable fluid. */
    public static boolean isPacket(ItemStack stack) {
        return amountOf(stack) >= 0;
    }

    /**
     * Amount of fluid in this stack, in millibuckets.
     *
     * @return -1 when the stack is not a fluid packet. The caller then uses the item count.
     */
    public static int amountOf(ItemStack stack) {
        FluidStack fluid = fluidOf(stack);
        return fluid == null ? -1 : fluid.amount;
    }

    /** Fluid carried by this stack, or {@code null}. */
    public static FluidStack fluidOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != item()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(FLUID_TAG)) {
            return null;
        }
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(FLUID_TAG));
        return fluid == null || fluid.amount <= 0 ? null : fluid;
    }

    /**
     * The same packet, with another amount.
     *
     * @return an empty stack when the amount is zero or less, as the Encoder does.
     */
    public static ItemStack withAmount(ItemStack stack, int amount) {
        FluidStack fluid = fluidOf(stack);
        if (fluid == null) {
            return stack;
        }
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        FluidStack changed = fluid.copy();
        changed.amount = Math.min(MAX_AMOUNT, amount);
        return pack(changed);
    }

    /** Fluid packet for this fluid, or an empty stack when none can be built. */
    public static ItemStack pack(FluidStack fluid) {
        Item item = item();
        if (item == null || fluid == null || fluid.getFluid() == null || fluid.amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(FLUID_TAG, fluid.writeToNBT(new NBTTagCompound()));
        stack.setTagCompound(tag);
        return stack;
    }
}
