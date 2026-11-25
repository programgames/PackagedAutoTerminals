package fr.julien.packagedautoterminals.container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;
import thelm.packagedauto.recipe.RecipeTypeCrafting;
import thelm.packagedauto.recipe.RecipeTypeProcessing;
import thelm.packagedauto.recipe.RecipeTypeProcessingOrdered;
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
        RecipeTypeRegistry.registerRecipeType(RecipeTypeProcessingOrdered.INSTANCE);
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
        TileEncoder.disabledRecipeTypes = new HashSet<>(
                Arrays.asList("packagedauto:crafting", "packagedauto:processing_ordered"));
        assertSame(RecipeTypeProcessing.INSTANCE,
                ContainerPatEditor.nextEnabledType(RecipeTypeProcessing.INSTANCE, true));
        assertSame(RecipeTypeProcessing.INSTANCE,
                ContainerPatEditor.nextEnabledType(RecipeTypeProcessing.INSTANCE, false));
    }

    /** The Encoder right arrow passes {@code reverse = false}, which moves up one id. */
    @Test
    public void forwardMovesUpTheRegistryAsTheEncoderRightArrowDoes() {
        IRecipeType first = RecipeTypeRegistry.getRecipeType(0);
        assertSame(RecipeTypeRegistry.getRecipeType(1),
                ContainerPatEditor.nextEnabledType(first, true));
        assertSame(RecipeTypeRegistry.getRecipeType(2),
                ContainerPatEditor.nextEnabledType(first, false));
    }

    @Test
    public void processingCannotBeDisabled() {
        TileEncoder.disabledRecipeTypes = Collections.singleton("packagedauto:processing");
        assertFalse(ContainerPatEditor.isDisabled(RecipeTypeProcessing.INSTANCE));
    }
}
