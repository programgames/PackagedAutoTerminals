package fr.julien.packagedautoterminals.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
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

    /**
     * The GUI scale the player chose, kept aside while a screen shrinks it. -1: untouched.
     *
     * <p>The state lives here, and not in each screen, because the two screens carried a byte for
     * byte copy of it. A copy drifts: the day one of them forgets to restore, the player keeps a
     * scale they never chose.
     */
    private int playerGuiScale = -1;

    /**
     * Lowers the GUI scale when this screen does not fit the window.
     *
     * <p>The screen calls it first thing in {@code initGui}, and returns at once when it answers
     * true: changing the scale runs {@code setWorldAndResolution}, which calls {@code initGui}
     * again, and that second pass builds the screen at the right size.
     *
     * <p>A window resize calls {@code initGui} again too. The computation always starts from the
     * setting the player chose, so growing the window gives their scale straight back.
     *
     * @return true when the scale changed.
     */
    public boolean apply(GuiScreen screen, Minecraft mc, int xSize, int ySize) {
        if (playerGuiScale < 0) {
            playerGuiScale = mc.gameSettings.guiScale;
        }
        int wanted = scaleFor(mc, playerGuiScale, xSize, ySize);
        if (wanted == mc.gameSettings.guiScale) {
            return false;
        }
        mc.gameSettings.guiScale = wanted;
        ScaledResolution size = new ScaledResolution(mc);
        screen.setWorldAndResolution(mc, size.getScaledWidth(), size.getScaledHeight());
        return true;
    }

    /**
     * Gives the player their GUI scale back. The screen calls it from {@code onGuiClosed}.
     *
     * <p>The setting only ever changed in memory, so a crash with the screen open leaves the
     * options file of the player untouched.
     */
    public void restore(Minecraft mc) {
        if (playerGuiScale >= 0) {
            mc.gameSettings.guiScale = playerGuiScale;
            playerGuiScale = -1;
        }
    }

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

}
