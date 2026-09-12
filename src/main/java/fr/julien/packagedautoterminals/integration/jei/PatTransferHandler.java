package fr.julien.packagedautoterminals.integration.jei;

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
 * Transfère une recette de JEI vers l'éditeur.
 *
 * <p>Aucune conversion n'est écrite ici. PackagedAuto fournit déjà
 * {@code IRecipeType.getRecipeTransferMap(IRecipeLayout, catégorie)}, qui rend la
 * correspondance « emplacement → objet » pour ses propres indices. C'est exactement la
 * raison de la contrainte D22 : nos indices d'emplacements sont restés ceux de l'Encoder.
 *
 * <p>Le client n'applique rien lui-même : il envoie la correspondance au serveur, qui
 * reconstruit la recette (décision D05).
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
     * Premier type de recette qui déclare cette catégorie JEI.
     *
     * <p>Le registre est relu à chaque transfert, et non mis en cache : un addon peut
     * enregistrer ses types après le chargement de JEI.
     */
    private static IRecipeType findType(String category) {
        for (Map.Entry<ResourceLocation, IRecipeType> entry
                : RecipeTypeRegistry.getRegistry().entrySet()) {
            IRecipeType type = entry.getValue();
            if (type.getJEICategories() != null && type.getJEICategories().contains(category)) {
                return type;
            }
        }
        return null;
    }

    private static IRecipeTransferError error(String key) {
        if (PatJeiPlugin.transferHelper == null) {
            return null;
        }
        return PatJeiPlugin.transferHelper.createUserErrorWithTooltip(
                net.minecraft.client.resources.I18n.format(key));
    }
}
