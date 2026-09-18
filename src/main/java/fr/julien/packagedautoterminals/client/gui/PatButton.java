package fr.julien.packagedautoterminals.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * A vanilla button that keeps its bottom edge at any height.
 *
 * <p>FIXED, reported by a player: "the back button is a bit cut at the bottom".
 *
 * <p>{@code GuiButton.drawButton} of 1.12.2 blits {@code this.height} rows from the **top** of a
 * texture that is twenty rows tall:
 *
 * <pre>
 * drawTexturedModalRect(x, y, 0, 46 + state * 20, width / 2, height);
 * </pre>
 *
 * <p>A button of fourteen rows therefore loses the last six, and the bottom bevel with them. The
 * button reads as flat, and as cut. Only a button of exactly twenty rows escapes it.
 *
 * <p>This class blits the texture in four pieces instead: the top half from the top of the
 * texture, the bottom half from its bottom. Both edges survive, whatever the height. The rest of
 * the drawing follows the vanilla method, so nothing else changes.
 */
public class PatButton extends GuiButton {

    private static final ResourceLocation WIDGETS =
            new ResourceLocation("textures/gui/widgets.png");
    /** Row of the first button state in the sheet, and height of one state. */
    private static final int SHEET_TOP = 46;
    private static final int SHEET_HEIGHT = 20;
    /** Width of the button in the sheet. The right half is read from its right edge. */
    private static final int SHEET_WIDTH = 200;

    private static final int TEXT_DISABLED = 0xA0A0A0;
    private static final int TEXT_HOVERED = 0xFFFFA0;
    private static final int TEXT_NORMAL = 0xE0E0E0;

    public PatButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        mc.getTextureManager().bindTexture(WIDGETS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int state = getHoverState(hovered);
        int top = SHEET_TOP + state * SHEET_HEIGHT;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.blendFunc(770, 771);

        // The halves are split so that an odd height loses no row: the lower half takes the
        // remainder.
        int left = width / 2;
        int right = width - left;
        int upper = height / 2;
        int lower = height - upper;
        int bottomRow = top + SHEET_HEIGHT - lower;

        drawTexturedModalRect(x, y, 0, top, left, upper);
        drawTexturedModalRect(x + left, y, SHEET_WIDTH - right, top, right, upper);
        drawTexturedModalRect(x, y + upper, 0, bottomRow, left, lower);
        drawTexturedModalRect(x + left, y + upper, SHEET_WIDTH - right, bottomRow, right, lower);

        mouseDragged(mc, mouseX, mouseY);

        int colour = TEXT_NORMAL;
        if (!enabled) {
            colour = TEXT_DISABLED;
        } else if (hovered) {
            colour = TEXT_HOVERED;
        }
        drawCenteredString(font, displayString, x + width / 2, y + (height - 8) / 2, colour);
    }
}
