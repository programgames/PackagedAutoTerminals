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

    /** Résultat d'une écriture de groupe. */
    public static final class Result {
        /** Machines réellement modifiées. */
        public int changed;
        /** Machines écartées faute de porte-recettes disponible. */
        public int withoutHolder;
    }

    /**
     * Applique un changement à chaque machine de la liste.
     *
     * <p>Une machine du groupe qui ne porte **pas** la recette visée la reçoit quand même,
     * lors d'une modification. C'est tout l'intérêt : réparer une paire désynchronisée d'un
     * seul geste. Sans cette règle, la machine en retard resterait en retard.
     *
     * <p>Une machine **sans porte-recettes est laissée intacte**, et comptée dans
     * {@link Result#withoutHolder}. Le terminal le dit, mais ne prend rien dans le réseau de
     * lui-même : sortir un objet du stockage est une décision du joueur.
     *
     * @param oldRecipe recette visée. {@code null} pour un ajout.
     * @param newRecipe recette à écrire. {@code null} pour une suppression.
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
            // Une suppression n'a rien à faire sur une machine vide ; une écriture, elle,
            // exige un porte-recettes que le joueur aura posé lui-même.
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
            // La machine ne porte pas encore cette recette : elle la reçoit.
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
