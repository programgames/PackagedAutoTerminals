package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.tile.TileEncoder;

/**
 * The list rules of a recipe holder, tested without the game.
 *
 * <p>The holder mode of the editor (D43) moves real items of the player, and the online save
 * goes through the same rules. A mistake here corrupts a holder with no way back.
 */
public class RecipeWriterTest {

    private static final int MAX = 20;

    private static List<IRecipeInfo> recipes(String... names) {
        List<IRecipeInfo> recipes = new ArrayList<>();
        for (String name : names) {
            recipes.add(new FakeRecipe(name));
        }
        return recipes;
    }

    private static List<IRecipeInfo> full() {
        List<IRecipeInfo> recipes = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            recipes.add(new FakeRecipe("r" + i));
        }
        return recipes;
    }

    private static IRecipeInfo recipe(String name) {
        return new FakeRecipe(name);
    }

    @Test
    public void aRemovalTakesEveryCopy() {
        List<IRecipeInfo> list = recipes("a", "b", "a", "c", "a");
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, recipe("a"), null, MAX));
        assertEquals(recipes("b", "c"), list);
    }

    @Test
    public void removingAnAbsentRecipeChangesNothing() {
        List<IRecipeInfo> list = recipes("a", "b");
        assertEquals(RecipeWriter.Outcome.UNCHANGED, RecipeWriter.edit(list, recipe("z"), null, MAX));
        assertEquals(recipes("a", "b"), list);
    }

    @Test
    public void anEditReplacesInPlace() {
        List<IRecipeInfo> list = recipes("a", "b", "c");
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, recipe("b"), recipe("x"), MAX));
        assertEquals(recipes("a", "x", "c"), list);
    }

    @Test
    public void anEditInAFullListIsAccepted() {
        List<IRecipeInfo> list = full();
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, recipe("r0"), recipe("x"), MAX));
        assertEquals(MAX, list.size());
        assertEquals(recipe("x"), list.get(0));
    }

    @Test
    public void aListThatLacksTheEditedRecipeReceivesTheNewOne() {
        List<IRecipeInfo> list = recipes("a");
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, recipe("z"), recipe("b"), MAX));
        assertEquals(recipes("a", "b"), list);
    }

    @Test
    public void anAdditionGoesAtTheEnd() {
        List<IRecipeInfo> list = recipes("a", "b");
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, null, recipe("c"), MAX));
        assertEquals(recipes("a", "b", "c"), list);
    }

    @Test
    public void anAdditionFillsTheLastPlace() {
        List<IRecipeInfo> list = full();
        list.remove(0);
        assertEquals(RecipeWriter.Outcome.CHANGED, RecipeWriter.edit(list, null, recipe("x"), MAX));
        assertEquals(MAX, list.size());
    }

    @Test
    public void anAdditionToAFullListIsRefused() {
        List<IRecipeInfo> list = full();
        assertEquals(RecipeWriter.Outcome.FULL, RecipeWriter.edit(list, null, recipe("x"), MAX));
        assertEquals(full(), list);
    }

    @Test
    public void aRecipeAlreadyThereIsNotAddedTwice() {
        List<IRecipeInfo> list = full();
        assertEquals(RecipeWriter.Outcome.UNCHANGED, RecipeWriter.edit(list, null, recipe("r5"), MAX));
        assertEquals(full(), list);
    }

    @Test
    public void aMapReplacesEveryMatchInPlace() {
        List<IRecipeInfo> list = recipes("a", "b", "c", "a");
        assertTrue(RecipeWriter.map(list, recipes("a", "c"), recipes("A", "C")));
        assertEquals(recipes("A", "b", "C", "A"), list);
    }

    @Test
    public void aMapWithNoMatchChangesNothing() {
        List<IRecipeInfo> list = recipes("a", "b");
        assertFalse(RecipeWriter.map(list, recipes("z"), recipes("Z")));
        assertEquals(recipes("a", "b"), list);
    }

    /** What the sync of holder mode does: each holder receives the distinct recipes of both. */
    @Test
    public void aSyncCopiesBothWays() {
        List<IRecipeInfo> main = recipes("a", "b");
        List<IRecipeInfo> twin = recipes("b", "c");
        List<IRecipeInfo> both = recipes("a", "b", "c");
        assertEquals(1, RecipeWriter.addMissing(main, both));
        assertEquals(1, RecipeWriter.addMissing(twin, both));
        assertEquals(recipes("a", "b", "c"), main);
        assertEquals(recipes("b", "c", "a"), twin);
    }

    @Test
    public void aBlankTwinBecomesACopy() {
        List<IRecipeInfo> twin = new ArrayList<>();
        assertEquals(3, RecipeWriter.addMissing(twin, recipes("a", "b", "c")));
        assertEquals(recipes("a", "b", "c"), twin);
    }

    @Test
    public void aSyncDeletesNothing() {
        List<IRecipeInfo> main = recipes("a", "a", "b");
        assertEquals(1, RecipeWriter.addMissing(main, recipes("b", "c")));
        assertEquals(recipes("a", "a", "b", "c"), main);
    }

    @Test
    public void aRecipeStillAtItsIndexResolvesThere() {
        assertEquals(1, RecipeWriter.resolve(recipes("a", "b", "c"), 1, recipe("b")));
    }

    @Test
    public void aRecipeThatMovedResolvesToItsNewIndex() {
        assertEquals(0, RecipeWriter.resolve(recipes("b", "c"), 1, recipe("b")));
    }

    @Test
    public void aRecipeThatIsGoneResolvesToNothing() {
        assertEquals(-1, RecipeWriter.resolve(recipes("a", "c"), 1, recipe("b")));
    }

    @Test
    public void theLimitFollowsTheEncoderSlots() {
        int before = TileEncoder.patternSlots;
        try {
            TileEncoder.patternSlots = 7;
            assertEquals(7, RecipeWriter.maxRecipes());
        } finally {
            TileEncoder.patternSlots = before;
        }
    }

    @Test
    public void aMissingFieldFallsBackToTwenty() {
        assertEquals(20, RecipeWriter.patternSlots(
                "thelm.packagedauto.tile.TileEncoder", "renamedSlots").getAsInt());
        assertEquals(20, RecipeWriter.patternSlots(
                "thelm.packagedauto.tile.RenamedEncoder", "patternSlots").getAsInt());
    }

    @Test
    public void aSyncOfTwinsCopiesNothing() {
        List<IRecipeInfo> main = recipes("a", "b");
        assertEquals(0, RecipeWriter.addMissing(main, recipes("b", "a")));
        assertEquals(recipes("a", "b"), main);
    }
}
