package fr.julien.packagedautoterminals.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Makes a screen that is taller than the window fit, by lowering the GUI scale of the game.
 *
 * <p>FIXED, reported by a player: "when the Minecraft window is small, the terminal does not
 * adapt, while the vanilla inventory does".
 *
 * <p>The vanilla inventory adapts because it is small: 176 by 166. Read in
 * {@code ScaledResolution}, Minecraft only guarantees **320 by 240** logical pixels:
 *
 * <pre>
 * while (factor &lt; limit
 *        &amp;&amp; width / (factor + 1) &gt;= 320
 *        &amp;&amp; height / (factor + 1) &gt;= 240) factor++;
 * </pre>
 *
 * <p>Our editor is 258 by **338**. It therefore cannot fit in the guaranteed height, whatever
 * Minecraft does on its own. The Package Recipe Encoder of PackagedAuto carries the same defect,
 * at 258 by 314.
 *
 * <p>The answer is the one a player would apply by hand: take the GUI scale down one notch. The
 * screen then really is smaller, so the mouse, the tooltips and JEI all keep agreeing with each
 * other. Scaling our own drawing instead would have left every one of them on the old size.
 *
 * <p>The setting is changed **in memory only**, and put back when the screen closes. The options
 * file of the player is never written.
 */
public final class GuiScaleFit {

    /** Logical size Minecraft guarantees. Read in {@code ScaledResolution}. */
    private static final int GUARANTEED_WIDTH = 320;
    private static final int GUARANTEED_HEIGHT = 240;

    private GuiScaleFit() {}

    /**
     * GUI scale setting to use so that a screen of this size fits.
     *
     * @param playerScale the setting the player chose, kept aside by the screen. Zero is "auto".
     * @return the setting to apply. It equals {@code playerScale} when the screen already fits.
     */
    public static int scaleFor(Minecraft mc, int playerScale, int xSize, int ySize) {
        int chosen = factorOf(mc, playerScale);
        if (fits(mc, chosen, xSize, ySize)) {
            return playerScale;
        }
        for (int factor = chosen - 1; factor >= 1; factor--) {
            if (fits(mc, factor, xSize, ySize)) {
                return factor;
            }
        }
        // Even one to one is too small. Nothing better exists, so the screen keeps the smallest
        // scale and overflows as little as it can.
        return 1;
    }

    /**
     * Factor Minecraft picks for this setting, on this window.
     *
     * <p>Copied from {@code ScaledResolution}, because that class exposes the result only for
     * the **current** setting, and we need to reason about another one.
     */
    private static int factorOf(Minecraft mc, int playerScale) {
        int limit = playerScale == 0 ? 1000 : playerScale;
        int factor = 1;
        while (factor < limit
                && mc.displayWidth / (factor + 1) >= GUARANTEED_WIDTH
                && mc.displayHeight / (factor + 1) >= GUARANTEED_HEIGHT) {
            factor++;
        }
        return factor;
    }

    /** Does a screen of this size fit in the window, at this factor? */
    private static boolean fits(Minecraft mc, int factor, int xSize, int ySize) {
        return mc.displayWidth / factor >= xSize && mc.displayHeight / factor >= ySize;
    }

    /** Logical size of the window, once a setting is applied. */
    public static ScaledResolution resolution(Minecraft mc) {
        return new ScaledResolution(mc);
    }
}
