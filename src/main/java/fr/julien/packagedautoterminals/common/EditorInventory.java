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
 * Les emplacements de l'éditeur de recette.
 *
 * <p>La disposition des **indices** reprend celle de l'Encoder de PackagedAuto, et doit le
 * rester :
 *
 * <ul>
 *   <li>0 à 80 : les entrées, une grille de 9 sur 9 ;
 *   <li>81 à 89 : les sorties, modifiables seulement si {@code canSetOutput()} ;
 *   <li>90 à 98 : l'aperçu du résultat, jamais modifiable.
 * </ul>
 *
 * <p>Motif : {@code IRecipeType.getEnabledSlots()} et
 * {@code IRecipeType.getRecipeTransferMap()} raisonnent sur ces indices. Les changer
 * couperait le transfert depuis JEI (contrainte D22). La position à l'écran, elle, nous
 * appartient.
 *
 * <p>Cette classe ne dépend d'aucune tuile. L'Encoder amont, lui, exige un
 * {@code TileEncoder} ; nous ne pouvons donc pas le réutiliser tel quel.
 */
public class EditorInventory implements IInventory {

    public static final int INPUT_SLOTS = 81;
    public static final int OUTPUT_SLOTS = 9;
    public static final int PREVIEW_SLOTS = 9;
    public static final int SIZE = INPUT_SLOTS + OUTPUT_SLOTS + PREVIEW_SLOTS;

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** Type courant. Il décide des emplacements actifs et de la forme de la grille. */
    public IRecipeType recipeType;
    /** Recette construite à partir des emplacements. {@code null} tant qu'elle est invalide. */
    public IRecipeInfo recipeInfo;

    /** Le monde sert à {@code generateFromStacks}, qui résout les recettes de craft. */
    private final World world;

    public EditorInventory(World world, IRecipeType recipeType) {
        this.world = world;
        this.recipeType = recipeType;
    }

    /** Recharge l'éditeur depuis une recette existante. */
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
     * Reconstruit la recette à partir des emplacements.
     *
     * <p>La découpe des listes reprend celle de l'Encoder : les 81 premiers emplacements
     * sont les entrées, et les sorties ne comptent que si le type les rend modifiables.
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

    /** Un emplacement est modifiable si le type l'active, et s'il n'est pas un aperçu. */
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
