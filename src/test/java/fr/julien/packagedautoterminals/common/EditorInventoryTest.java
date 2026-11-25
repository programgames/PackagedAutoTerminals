package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import thelm.packagedauto.recipe.RecipeTypeCrafting;
import thelm.packagedauto.recipe.RecipeTypeProcessing;

/**
 * Which editor slot accepts an item. The server refuses every ghost slot write on this rule.
 */
public class EditorInventoryTest {

    private static final int FIRST_OUTPUT = EditorInventory.INPUT_SLOTS;
    private static final int FIRST_PREVIEW = EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS;

    @Test
    public void aCraftingRecipeOnlyTakesItsCentralSquare() {
        EditorInventory editor = new EditorInventory(null, RecipeTypeCrafting.INSTANCE);
        assertTrue(editor.isEditable(40));
        assertFalse(editor.isEditable(0));
        assertFalse(editor.isEditable(FIRST_OUTPUT));
        assertFalse(editor.isEditable(FIRST_PREVIEW));
    }

    @Test
    public void aProcessingRecipeTakesItsOutputsButNeverThePreview() {
        EditorInventory editor = new EditorInventory(null, RecipeTypeProcessing.INSTANCE);
        assertTrue(editor.isEditable(0));
        assertTrue(editor.isEditable(FIRST_OUTPUT));
        assertFalse(editor.isEditable(FIRST_PREVIEW));
    }

    @Test
    public void noTypeMeansNoSlot() {
        assertFalse(new EditorInventory(null, null).isEditable(40));
    }
}
