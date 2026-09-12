package fr.julien.packagedautoterminals.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

/**
 * Intégration JEI.
 *
 * <p>Un seul point d'entrée : le transfert d'une recette de JEI vers l'éditeur. Le
 * gestionnaire est déclaré **universel**, donc appelé pour toute catégorie. C'est le choix
 * le plus robuste : il évite d'énumérer les catégories au chargement, et capte donc les
 * addons qui enregistrent leurs types après nous.
 *
 * <p>Cette classe n'est chargée que si JEI est présent. Forge ne suit pas les classes
 * absentes tant que rien ne les référence, et {@code @JEIPlugin} n'est lu que par JEI.
 */
@JEIPlugin
public class PatJeiPlugin implements IModPlugin {

    /** Fabrique des messages d'erreur de JEI. Renseignée à l'enregistrement. */
    public static IRecipeTransferHandlerHelper transferHelper;

    @Override
    public void register(IModRegistry registry) {
        transferHelper = registry.getJeiHelpers().recipeTransferHandlerHelper();
        registry.getRecipeTransferRegistry()
                .addUniversalRecipeTransferHandler(new PatTransferHandler());
    }
}
