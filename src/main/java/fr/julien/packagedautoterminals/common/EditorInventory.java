package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeType;

/**
 * The slots of the recipe editor.
 *
 * <p>The layout of the **indexes** follows the PackagedAuto Encoder, and must keep doing so:
 *
 * <ul>
 *   <li>0 to 80: the inputs, a 9 by 9 grid;
 *   <li>81 to 89: the outputs, editable only when {@code canSetOutput()};
 *   <li>90 to 98: the result preview, never editable.
 * </ul>
 *
 * <p>Reason: {@code IRecipeType.getEnabledSlots()} and
 * {@code IRecipeType.getRecipeTransferMap()} reason on these indexes. Changing them would
 * break the transfer from JEI (constraint D22). The position on screen, on the other hand,
 * is ours.
 *
 * <p>This class depends on no tile. The upstream Encoder requires a {@code TileEncoder}; we
 * therefore cannot reuse it as it is.
 */
public class EditorInventory implements IInventory {

    public static final int INPUT_SLOTS = 81;
    public static final int OUTPUT_SLOTS = 9;
    public static final int PREVIEW_SLOTS = 9;
    public static final int SIZE = INPUT_SLOTS + OUTPUT_SLOTS + PREVIEW_SLOTS;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** Current type. It drives the enabled slots and the shape of the grid. */
    public IRecipeType recipeType;
    /** Recipe built from the slots. {@code null} for as long as it is invalid. */
    public IRecipeInfo recipeInfo;

    /** The world is needed by {@code generateFromStacks}, which resolves crafting recipes. */
    private final World world;

    public EditorInventory(World world, IRecipeType recipeType) {
        this.world = world;
        this.recipeType = recipeType;
    }

    /** Reloads the editor from an existing recipe. */
    public void load(IRecipeInfo recipe) {
        clear();
        recipeType = recipe.getRecipeType();
        Int2ObjectMap<ItemStack> encoded = recipe.getEncoderStacks();
        if (encoded != null) {
            for (Int2ObjectMap.Entry<ItemStack> entry : encoded.int2ObjectEntrySet()) {
                int slot = entry.getIntKey();
                if (slot >= 0 && slot < SIZE) {
                    stacks.set(slot, entry.getValue().copy());
                }
            }
        }
        updateRecipeInfo();
    }

    /**
     * Rebuilds the recipe from the slots.
     *
     * <p>The list split follows the Encoder: the first 81 slots are the inputs, and the
     * outputs only count when the type makes them editable.
     */
    public void updateRecipeInfo() {
        recipeInfo = null;
        for (int slot = INPUT_SLOTS + OUTPUT_SLOTS; slot < SIZE; slot++) {
            stacks.set(slot, ItemStack.EMPTY);
        }
        if (recipeType == null) {
            return;
        }

        List<ItemStack> inputs = new ArrayList<>(stacks.subList(0, INPUT_SLOTS));
        List<ItemStack> outputs = recipeType.canSetOutput()
                ? new ArrayList<>(stacks.subList(INPUT_SLOTS, INPUT_SLOTS + OUTPUT_SLOTS))
                : Collections.emptyList();

        IRecipeInfo candidate = recipeType.getNewRecipeInfo();
        candidate.generateFromStacks(inputs, outputs, world);
        if (!candidate.isValid()) {
            return;
        }
        recipeInfo = candidate;

        List<ItemStack> results = candidate.getOutputs();
        for (int i = 0; i < PREVIEW_SLOTS && i < results.size(); i++) {
            stacks.set(INPUT_SLOTS + OUTPUT_SLOTS + i, results.get(i).copy());
        }
    }

    /** A slot is editable when the type enables it, and when it is not a preview. */
    public boolean isEditable(int slot) {
        if (recipeType == null || slot >= INPUT_SLOTS + OUTPUT_SLOTS) {
            return false;
        }
        if (slot >= INPUT_SLOTS && !recipeType.canSetOutput()) {
            return false;
        }
        return recipeType.getEnabledSlots().contains(slot);
    }

    // --- IInventory ---------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index < 0 || index >= SIZE ? ItemStack.EMPTY : stacks.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE) {
            return;
        }
        stacks.set(index, stack);
        updateRecipeInfo();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return isEditable(index);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        Collections.fill(stacks, ItemStack.EMPTY);
        recipeInfo = null;
    }

    @Override
    public String getName() {
        return "packagedautoterminals.editor";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentString(getName());
    }
}
