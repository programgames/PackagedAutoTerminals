package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import net.minecraft.item.ItemStack;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;

/**
 * Écrit une recette dans les porte-recettes d'un groupe de machines.
 *
 * <p>PackagedAuto exige la **même** recette dans le Packager et dans l'Unpackager. Modifier
 * un seul côté casse l'automatisation en silence. Toute écriture passe donc par ici, et
 * s'applique à toutes les machines du groupe.
 *
 * <p>Chaque écriture se termine par {@code setPatternStack}. C'est cet appel, et lui seul,
 * qui déclenche {@code updatePatternList} puis {@code postPatternChange} chez PackagedAuto.
 * Voir docs/PACKAGEDAUTO-MODEL.md, section 7.2.
 */
public final class RecipeWriter {

    private RecipeWriter() {}

    /**
     * Applique un changement à chaque machine de la liste.
     *
     * @param oldRecipe recette visée. {@code null} pour un ajout.
     * @param newRecipe recette à écrire. {@code null} pour une suppression.
     * @return nombre de machines effectivement modifiées.
     */
    public static int apply(IGrid grid, List<ProviderSnapshot> machines,
                            IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        int changed = 0;
        for (ProviderSnapshot snapshot : machines) {
            if (applyTo(grid, snapshot, oldRecipe, newRecipe)) {
                changed++;
            }
        }
        return changed;
    }

    private static boolean applyTo(IGrid grid, ProviderSnapshot snapshot,
                                   IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        IPackageProvidingMachine machine =
                ProviderScanner.find(grid, snapshot.dimension, snapshot.pos);
        if (machine == null) {
            return false;
        }

        ItemStack holder = machine.getPatternStack();
        if (holder.isEmpty() || !(holder.getItem() instanceof IRecipeListItem)) {
            return false;
        }
        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return false;
        }

        List<IRecipeInfo> recipes = new ArrayList<>(recipeList.getRecipeList());
        int index = indexOf(recipes, oldRecipe);

        if (oldRecipe == null) {
            // Ajout. Une machine qui porte déjà cette recette n'est pas touchée deux fois.
            if (indexOf(recipes, newRecipe) >= 0) {
                return false;
            }
            recipes.add(newRecipe);
        } else if (index < 0) {
            // Cette machine ne porte pas la recette visée : rien à y faire.
            return false;
        } else if (newRecipe == null) {
            recipes.remove(index);
        } else {
            recipes.set(index, newRecipe);
        }

        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        machine.setPatternStack(holder);
        return true;
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
