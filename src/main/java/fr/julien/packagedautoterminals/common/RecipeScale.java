package fr.julien.packagedautoterminals.common;

import java.util.List;

import thelm.packagedauto.api.IRecipeInfo;

/**
 * The arithmetic of scaling every recipe of a group (D44), kept apart so it runs without
 * Minecraft.
 */
public final class RecipeScale {

    /** Smallest and largest factor the panel accepts. */
    public static final int MIN_FACTOR = 2;
    public static final int MAX_FACTOR = 64;

    private RecipeScale() {}

    /**
     * One amount, multiplied or divided.
     *
     * @return the new amount, or -1 when a division does not fall right or the result passes
     *     {@code max}.
     */
    public static long scale(long amount, int factor, boolean divide, long max) {
        if (divide) {
            return amount % factor == 0 ? amount / factor : -1;
        }
        long result = amount * factor;
        return result <= max ? result : -1;
    }

    /**
     * The first two positions that hold equal recipes, or {@code null} when all differ.
     *
     * <p>Two recipes that become equal once scaled would merge in the holder, and one of them
     * would disappear without anyone asking.
     */
    public static int[] firstDuplicate(List<IRecipeInfo> recipes) {
        for (int first = 0; first < recipes.size(); first++) {
            for (int second = first + 1; second < recipes.size(); second++) {
                if (recipes.get(first).equals(recipes.get(second))) {
                    return new int[] {first, second};
                }
            }
        }
        return null;
    }
}
