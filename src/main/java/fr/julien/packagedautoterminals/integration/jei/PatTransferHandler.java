package fr.julien.packagedautoterminals.integration.jei;

import java.util.List;
import java.util.Map;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.network.PacketEditorFill;
import fr.julien.packagedautoterminals.network.PatNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;

/**
 * Transfers a recipe from JEI into the editor.
 *
 * <p>No conversion is written here. PackagedAuto already provides
 * {@code IRecipeType.getRecipeTransferMap(IRecipeLayout, category)}, which returns the
 * "slot to item" mapping for its own indexes. That is exactly the reason for constraint D22:
 * our slot indexes stayed the ones of the Encoder.
 *
 * <p>The client applies nothing itself: it sends the mapping to the server, which rebuilds
 * the recipe (decision D05).
 */
public class PatTransferHandler implements IRecipeTransferHandler<ContainerPatEditor> {

    @Override
    public Class<ContainerPatEditor> getContainerClass() {
        return ContainerPatEditor.class;
    }

    @Override
    public IRecipeTransferError transferRecipe(ContainerPatEditor container, IRecipeLayout layout,
                                               EntityPlayer player, boolean maxTransfer,
                                               boolean doTransfer) {
        String category = layout.getRecipeCategory().getUid();

        IRecipeType type = findType(category);
        if (type == null) {
            return error("gui.packagedautoterminals.jei_no_type");
        }

        Int2ObjectMap<ItemStack> transfer = type.getRecipeTransferMap(layout, category);
        if (transfer == null || transfer.isEmpty()) {
            return error("gui.packagedautoterminals.jei_no_transfer");
        }

        if (doTransfer) {
            PatNetwork.CHANNEL.sendToServer(
                    new PacketEditorFill(RecipeTypeRegistry.getId(type), transfer));
        }
        return null;
    }

    /**
     * The **most precise** recipe type for this JEI category.
     *
     * <p>PITFALL fixed. Taking the first type that declares the category gave a result that
     * depended on the registry order. Decompiling `RecipeTypeProcessing` shows that its
     * `getJEICategories` method returns **every** JEI category as soon as JEI is loaded: the
     * Processing type, and the Ordered and Positioned types that inherit from it, are catch
     * alls. An Ultimate Table recipe therefore fell into "Positioned".
     *
     * <p>The criterion is therefore the **width** of the declared list: a type that names
     * only two categories knows what it does; a type that names them all merely accepts. The
     * narrowest wins, and the catch all only serves as a last resort. The rule names no mod,
     * so an unknown addon benefits from it too.
     *
     * <p>The registry is read again on every transfer, and not cached: an addon can register
     * its types after JEI is loaded.
     */
    private static IRecipeType findType(String category) {
        IRecipeType best = null;
        int narrowest = Integer.MAX_VALUE;
        for (Map.Entry<ResourceLocation, IRecipeType> entry
                : RecipeTypeRegistry.getRegistry().entrySet()) {
            IRecipeType type = entry.getValue();
            List<String> categories = type.getJEICategories();
            if (categories == null || !categories.contains(category)) {
                continue;
            }
            if (categories.size() < narrowest) {
                narrowest = categories.size();
                best = type;
            }
        }
        return best;
    }

    private static IRecipeTransferError error(String key) {
        if (PatJeiPlugin.transferHelper == null) {
            return null;
        }
        return PatJeiPlugin.transferHelper.createUserErrorWithTooltip(
                net.minecraft.client.resources.I18n.format(key));
    }
}
