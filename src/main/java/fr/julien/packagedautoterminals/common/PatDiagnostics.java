package fr.julien.packagedautoterminals.common;

import java.util.Map;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import net.minecraft.util.ResourceLocation;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;

/**
 * Startup diagnostics.
 *
 * <p>They answer two questions without forcing the player to dig through the game: which
 * recipe types are actually registered, and whether the mod translations are loaded.
 */
public final class PatDiagnostics {

    private PatDiagnostics() {}

    /** Lists the recipe types seen by the PackagedAuto registry. */
    public static void logRecipeTypes() {
        Map<ResourceLocation, IRecipeType> registry = RecipeTypeRegistry.getRegistry();
        PackagedAutoTerminals.LOGGER.info("Registered recipe types: {}", registry.size());
        for (Map.Entry<ResourceLocation, IRecipeType> entry : registry.entrySet()) {
            IRecipeType type = entry.getValue();
            PackagedAutoTerminals.LOGGER.info("  - {} | short={} | machine={} | slots={}",
                    entry.getKey(),
                    type.getLocalizedNameShort(),
                    type.hasMachine(),
                    type.getEnabledSlots().size());
        }
    }
}
