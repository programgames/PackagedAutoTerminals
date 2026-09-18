package fr.julien.packagedautoterminals.integration.jei;

import fr.julien.packagedautoterminals.client.gui.GuiPatEditor;
import fr.julien.packagedautoterminals.common.PatItems;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.item.ItemStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

/**
 * JEI integration.
 *
 * <p>Two entry points into the editor:
 *
 * <ol>
 *   <li>the transfer of a whole recipe, through {@link PatTransferHandler}. The handler is
 *       declared **universal**, hence called for every category. That is the most robust
 *       choice: it avoids listing the categories at load time, and therefore catches the
 *       addons that register their types after us;
 *   <li>the drag and drop of one item or one fluid, through {@link PatGhostHandler}.
 * </ol>
 *
 * <p>The plugin also carries the in-game help: a description page per item, shown by JEI when the
 * player presses the usage key on the terminal.
 *
 * <p>A third registration is not an entry point: {@link PatGuiAreas} tells JEI to keep its hands
 * off the rectangle of the amount panel.
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
        registry.addGhostIngredientHandler(GuiPatEditor.class, new PatGhostHandler());
        registry.addAdvancedGuiHandlers(new PatGuiAreas());

        // The description page of JEI, reached with the usage key on the item. It is the only
        // in-game help the mod carries, and it costs no new dependency: JEI is already here, and
        // already optional. Without JEI the pages simply do not exist.
        registry.addIngredientInfo(new ItemStack(PatItems.TERMINAL), VanillaTypes.ITEM,
                "jei.packagedautoterminals.terminal.1",
                "jei.packagedautoterminals.terminal.2",
                "jei.packagedautoterminals.terminal.3");
        registry.addIngredientInfo(new ItemStack(PatItems.WIRELESS_TERMINAL), VanillaTypes.ITEM,
                "jei.packagedautoterminals.wireless.1",
                "jei.packagedautoterminals.wireless.2");
    }
}
