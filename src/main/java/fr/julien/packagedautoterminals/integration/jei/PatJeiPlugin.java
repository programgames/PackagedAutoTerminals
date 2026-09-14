package fr.julien.packagedautoterminals.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

/**
 * JEI integration.
 *
 * <p>A single entry point: transferring a recipe from JEI into the editor. The handler is
 * declared **universal**, hence called for every category. That is the most robust choice: it
 * avoids listing the categories at load time, and therefore catches the addons that register
 * their types after us.
 *
 * <p>This class is only loaded when JEI is present. Forge does not follow absent classes as
 * long as nothing references them, and {@code @JEIPlugin} is only read by JEI.
 */
@JEIPlugin
public class PatJeiPlugin implements IModPlugin {

    /** JEI error message factory. Filled in at registration time. */
    public static IRecipeTransferHandlerHelper transferHelper;

    @Override
    public void register(IModRegistry registry) {
        transferHelper = registry.getJeiHelpers().recipeTransferHandlerHelper();
        registry.getRecipeTransferRegistry()
                .addUniversalRecipeTransferHandler(new PatTransferHandler());
    }
}
