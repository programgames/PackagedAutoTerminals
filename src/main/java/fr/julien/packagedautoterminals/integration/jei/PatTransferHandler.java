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
     * Type de recette le plus **précis** pour cette catégorie JEI.
     *
     * <p>PIÈGE corrigé. Prendre le premier type qui déclare la catégorie donnait un résultat
     * dépendant de l'ordre du registre. En décompilant `RecipeTypeProcessing`, on voit que
     * sa méthode `getJEICategories` rend **toutes** les catégories de JEI dès que JEI est
     * chargé : le type Processing, et les types Ordered et Positioned qui en héritent, sont
     * des fourre-tout. Une recette de l'Ultimate Table basculait donc en « Positioned ».
     *
     * <p>Le critère est donc la **largeur** de la liste déclarée : un type qui ne nomme que
     * deux catégories sait ce qu'il fait ; un type qui les nomme toutes se contente
     * d'accepter. Le plus étroit gagne, et le fourre-tout ne sert que de dernier recours.
     * La règle ne cite aucun mod par son nom, donc un addon inconnu en profite aussi.
     *
     * <p>Le registre est relu à chaque transfert, et non mis en cache : un addon peut
     * enregistrer ses types après le chargement de JEI.
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
