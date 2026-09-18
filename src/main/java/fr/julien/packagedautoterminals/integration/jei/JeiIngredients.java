package fr.julien.packagedautoterminals.integration.jei;

import fr.julien.packagedautoterminals.common.FluidPackets;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * Converts a JEI ingredient into the {@link ItemStack} a ghost slot can hold.
 *
 * <p>The editor stores items only: {@code EditorInventory} is an {@code IInventory}, and
 * PackagedAuto reads its recipes from {@code ItemStack} lists.
 *
 * <p>A fluid therefore needs an item. Two of them exist, and only one is right.
 *
 * <ol>
 *   <li><b>The fluid packet</b>, when {@code PackagedFluidCrafting} is installed. That is what
 *       the addon itself writes, read in {@code MixinHooks.packToPacket}. The Packager and the
 *       Crafter turn the packet back into fluid. See {@link FluidPackets}.
 *   <li><b>The filled bucket</b>, otherwise. Without the addon no machine reads a packet, so the
 *       bucket is the only item that can still carry the fluid, as an ordinary ingredient.
 * </ol>
 *
 * <p>FIXED: the bucket used to be the only answer. A player who dragged a fluid got a Water Bucket
 * where the recipe wanted 1000 mB of water, and the recipe never ran.
 *
 * <p>A fluid that yields neither a packet nor a bucket gives an empty stack, and the caller then
 * offers no target: the player sees at once that the drop cannot work.
 */
public final class JeiIngredients {

    private JeiIngredients() {}

    /** Ingredient dragged out of JEI, as an item. Empty when no item can carry it. */
    public static ItemStack toStack(Object ingredient) {
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        if (ingredient instanceof FluidStack) {
            return fluidStackOf((FluidStack) ingredient);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Item that carries this fluid.
     *
     * <p>The dragged amount is kept in the packet, exactly as {@code PackagedFluidCrafting} keeps
     * it. JEI hands over the amount the recipe shows, which is the amount the player wants. The
     * amount panel then edits it in millibuckets.
     *
     * <p>The bucket path ignores the amount instead: a bucket holds one bucket.
     */
    private static ItemStack fluidStackOf(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return ItemStack.EMPTY;
        }
        if (FluidPackets.available()) {
            ItemStack packet = FluidPackets.pack(fluid);
            if (!packet.isEmpty()) {
                return packet;
            }
        }
        ItemStack bucket = FluidUtil.getFilledBucket(
                new FluidStack(fluid.getFluid(), Fluid.BUCKET_VOLUME));
        return bucket == null ? ItemStack.EMPTY : bucket;
    }
}
