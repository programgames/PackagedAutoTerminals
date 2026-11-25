package fr.julien.packagedautoterminals.common;

import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import thelm.packagedauto.api.IPackagePattern;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeType;

/**
 * A recipe named by one word, for the grouping tests.
 *
 * <p>{@link ProviderPairing} only ever asks a recipe whether it equals another one. Every
 * other method of the interface is there to satisfy the compiler, and answers nothing.
 */
class FakeRecipe implements IRecipeInfo {

    private final String name;

    FakeRecipe(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FakeRecipe && name.equals(((FakeRecipe) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        return tag;
    }

    @Override
    public IRecipeType getRecipeType() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<IPackagePattern> getPatterns() {
        return Collections.emptyList();
    }

    @Override
    public List<ItemStack> getInputs() {
        return Collections.emptyList();
    }

    @Override
    public List<ItemStack> getOutputs() {
        return Collections.emptyList();
    }

    @Override
    public void generateFromStacks(List<ItemStack> inputs, List<ItemStack> outputs, World world) {}

    @Override
    public Int2ObjectMap<ItemStack> getEncoderStacks() {
        return new Int2ObjectOpenHashMap<>();
    }
}
