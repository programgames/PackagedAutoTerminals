package fr.julien.packagedautoterminals.container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import thelm.packagedauto.api.RecipeTypeRegistry;
import thelm.packagedauto.recipe.RecipeTypeCrafting;
import thelm.packagedauto.recipe.RecipeTypeProcessing;
import thelm.packagedauto.tile.TileEncoder;

/**
 * The editor skips the types that the PackagedAuto config disables, as the Package Recipe
 * Encoder does. Processing always stays.
 */
public class DisabledRecipeTypesTest {

    @Before
    public void registerTypes() {
        RecipeTypeRegistry.registerRecipeType(RecipeTypeCrafting.INSTANCE);
        RecipeTypeRegistry.registerRecipeType(RecipeTypeProcessing.INSTANCE);
    }

    @After
    public void enableEveryType() {
        TileEncoder.disabledRecipeTypes = Collections.emptySet();
    }

    @Test
    public void aDisabledCraftingTypeIsNotTheDefault() {
        TileEncoder.disabledRecipeTypes = Collections.singleton("packagedauto:crafting");
        assertSame(RecipeTypeProcessing.INSTANCE, ContainerPatEditor.defaultRecipeType());
    }

    @Test
    public void theCycleSkipsADisabledType() {
        TileEncoder.disabledRecipeTypes = Collections.singleton("packagedauto:crafting");
        assertSame(RecipeTypeProcessing.INSTANCE,
                ContainerPatEditor.nextEnabledType(RecipeTypeProcessing.INSTANCE, true));
        assertSame(RecipeTypeProcessing.INSTANCE,
                ContainerPatEditor.nextEnabledType(RecipeTypeProcessing.INSTANCE, false));
    }

    @Test
    public void processingCannotBeDisabled() {
        TileEncoder.disabledRecipeTypes = Collections.singleton("packagedauto:processing");
        assertFalse(ContainerPatEditor.isDisabled(RecipeTypeProcessing.INSTANCE));
    }
}
