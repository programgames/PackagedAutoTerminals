package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public class RecipeScaleTest {

    @Test
    public void multiplies() {
        assertEquals(12, RecipeScale.scale(3, 4, false, 4096));
    }

    @Test
    public void refusesPastTheLimit() {
        assertEquals(-1, RecipeScale.scale(3000, 2, false, 4096));
    }

    @Test
    public void dividesExactly() {
        assertEquals(3, RecipeScale.scale(12, 4, true, 4096));
    }

    @Test
    public void refusesARemainder() {
        assertEquals(-1, RecipeScale.scale(3, 2, true, 4096));
    }

    @Test
    public void findsTheFirstCollision() {
        assertArrayEquals(new int[] {1, 3}, RecipeScale.firstDuplicate(Arrays.asList(
                new FakeRecipe("a"), new FakeRecipe("b"), new FakeRecipe("c"), new FakeRecipe("b"))));
    }

    @Test
    public void noCollision() {
        assertNull(RecipeScale.firstDuplicate(Arrays.asList(
                new FakeRecipe("a"), new FakeRecipe("b"))));
    }
}
