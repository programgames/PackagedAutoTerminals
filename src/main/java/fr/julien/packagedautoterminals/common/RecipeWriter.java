package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import net.minecraft.item.ItemStack;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;

/**
 * Writes a recipe into the recipe holders of a machine group.
 *
 * <p>PackagedAuto requires the **same** recipe in the Packager and in the Unpackager.
 * Editing one side only breaks the automation silently. Every write therefore goes through
 * here, and applies to all machines of the group.
 *
 * <p>Each write ends with {@code setPatternStack}. That call, and only that call, triggers
 * {@code updatePatternList} then {@code postPatternChange} in PackagedAuto. See
 * docs/PACKAGEDAUTO-MODEL.md, section 7.2.
 */
public final class RecipeWriter {

    private RecipeWriter() {}

    /** Result of a group write. */
    public static final class Result {
        /** Machines actually changed. */
        public int changed;
        /** Machines skipped for lack of an available recipe holder. */
        public int withoutHolder;
    }

    /**
     * Applies a change to every machine in the list.
     *
     * <p>A machine of the group that does **not** carry the target recipe receives it anyway
     * on an edit. That is the whole point: repairing an out-of-sync pair in one gesture.
     * Without this rule, the machine left behind would stay behind.
     *
     * <p>A machine **without a recipe holder is left untouched**, and counted in
     * {@link Result#withoutHolder}. The terminal reports it, but takes nothing from the
     * network on its own: pulling an item out of storage is the player's decision.
     *
     * @param oldRecipe target recipe. {@code null} for an addition.
     * @param newRecipe recipe to write. {@code null} for a removal.
     */
    public static Result apply(IGrid grid, IActionSource source, List<ProviderSnapshot> machines,
                               IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        Result result = new Result();
        for (ProviderSnapshot snapshot : machines) {
            switch (applyTo(grid, source, snapshot, oldRecipe, newRecipe)) {
                case CHANGED:
                    result.changed++;
                    break;
                case NO_HOLDER:
                    result.withoutHolder++;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private enum Outcome { CHANGED, UNCHANGED, NO_HOLDER }

    private static Outcome applyTo(IGrid grid, IActionSource source, ProviderSnapshot snapshot,
                                   IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        IPackageProvidingMachine machine =
                ProviderScanner.find(grid, snapshot.dimension, snapshot.pos);
        if (machine == null) {
            return Outcome.UNCHANGED;
        }

        ItemStack holder = machine.getPatternStack();
        if (holder.isEmpty()) {
            // A removal has nothing to do on an empty machine; a write, on the other hand,
            // needs a recipe holder that the player put there themselves.
            return newRecipe == null ? Outcome.UNCHANGED : Outcome.NO_HOLDER;
        }
        if (!(holder.getItem() instanceof IRecipeListItem)) {
            return Outcome.UNCHANGED;
        }

        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return Outcome.UNCHANGED;
        }

        List<IRecipeInfo> recipes = new ArrayList<>(recipeList.getRecipeList());
        int index = indexOf(recipes, oldRecipe);

        if (newRecipe == null) {
            if (index < 0) {
                return Outcome.UNCHANGED;
            }
            recipes.remove(index);
        } else if (index >= 0) {
            recipes.set(index, newRecipe);
        } else {
            // The machine does not carry this recipe yet: it receives it.
            if (indexOf(recipes, newRecipe) >= 0) {
                return Outcome.UNCHANGED;
            }
            recipes.add(newRecipe);
        }

        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        machine.setPatternStack(holder);
        return Outcome.CHANGED;
    }

    private static int indexOf(List<IRecipeInfo> recipes, IRecipeInfo recipe) {
        if (recipe == null) {
            return -1;
        }
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).equals(recipe)) {
                return i;
            }
        }
        return -1;
    }
}
