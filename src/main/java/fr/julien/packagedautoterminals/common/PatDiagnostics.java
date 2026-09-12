package fr.julien.packagedautoterminals.common;

import java.util.Map;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import net.minecraft.util.ResourceLocation;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;

/**
 * Diagnostics de démarrage.
 *
 * <p>Ils répondent à deux questions sans obliger le joueur à fouiller le jeu : quels types
 * de recettes sont réellement enregistrés, et les traductions du mod sont-elles chargées.
 */
public final class PatDiagnostics {

    private PatDiagnostics() {}

    /** Liste les types de recettes vus par le registre de PackagedAuto. */
    public static void logRecipeTypes() {
        Map<ResourceLocation, IRecipeType> registry = RecipeTypeRegistry.getRegistry();
        PackagedAutoTerminals.LOGGER.info("Types de recettes enregistres : {}", registry.size());
        for (Map.Entry<ResourceLocation, IRecipeType> entry : registry.entrySet()) {
            IRecipeType type = entry.getValue();
            PackagedAutoTerminals.LOGGER.info("  - {} | court={} | machine={} | slots={}",
                    entry.getKey(),
                    type.getLocalizedNameShort(),
                    type.hasMachine(),
                    type.getEnabledSlots().size());
        }
    }
}
