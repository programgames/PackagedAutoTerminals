package fr.julien.packagedautoterminals.integration.jei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * Converts a JEI ingredient into the {@link ItemStack} a ghost slot can hold.
 *
 * <p>The editor stores items only: {@code EditorInventory} is an {@code IInventory}, and
 * PackagedAuto reads its recipes from {@code ItemStack} lists. A fluid therefore enters the
 * grid as the container that holds it.
 *
 * <p>The bucket is the only container built here. Forge resolves it through
 * {@code FluidUtil.getFilledBucket}, which covers water, lava, milk and every fluid of the
 * universal bucket. A fluid with no bucket yields an empty stack, and the caller then offers
 * no target: the player sees at once that the drop cannot work.
 *
 * <p>Real fluid slots belong to {@code PackagedFluidCrafting}, hence to version 2. See
 * decision D09.
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
            return bucketOf((FluidStack) ingredient);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Filled bucket for this fluid.
     *
     * <p>The amount of the dragged fluid is ignored: a bucket holds one bucket. The player
     * adjusts the count with the wheel, as for any other slot.
     */
    private static ItemStack bucketOf(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack bucket = FluidUtil.getFilledBucket(
                new FluidStack(fluid.getFluid(), Fluid.BUCKET_VOLUME));
        return bucket == null ? ItemStack.EMPTY : bucket;
    }
}
