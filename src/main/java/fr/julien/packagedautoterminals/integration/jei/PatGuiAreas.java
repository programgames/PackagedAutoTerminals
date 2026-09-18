package fr.julien.packagedautoterminals.integration.jei;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import fr.julien.packagedautoterminals.client.gui.GuiPatEditor;
import mezz.jei.api.gui.IAdvancedGuiHandler;

/**
 * Tells JEI which part of the screen belongs to us.
 *
 * <p>The amount panel floats over the screen, and it overflows the frame on purpose: the panel
 * opens above the slot the player clicked. JEI knew nothing about it. The mouse therefore still
 * pointed at an ingredient of the JEI list, and JEI drew its tooltip **over** the buttons of the
 * panel. The player could not read "Set" or "Cancel".
 *
 * <p>{@code getGuiExtraAreas} is the JEI answer to that exact case. JEI leaves the returned
 * rectangles alone: no tooltip, and no ingredient under the mouse there.
 *
 * <p>The rectangle only exists while the panel is open, so the JEI list stays usable the rest of
 * the time.
 */
public class PatGuiAreas implements IAdvancedGuiHandler<GuiPatEditor> {

    @Override
    public Class<GuiPatEditor> getGuiContainerClass() {
        return GuiPatEditor.class;
    }

    @Override
    public List<Rectangle> getGuiExtraAreas(GuiPatEditor gui) {
        Rectangle panel = gui.panelArea();
        return panel == null ? Collections.<Rectangle>emptyList() : Collections.singletonList(panel);
    }

    @Override
    public Object getIngredientUnderMouse(GuiPatEditor gui, int mouseX, int mouseY) {
        return null;
    }
}
