package fr.julien.packagedautoterminals.integration.jei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.julien.packagedautoterminals.client.gui.GuiPatEditor;
import fr.julien.packagedautoterminals.network.PacketEditorGhost;
import fr.julien.packagedautoterminals.network.PatNetwork;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.item.ItemStack;

/**
 * Drag and drop from JEI into one slot of the editor.
 *
 * <p>The recipe transfer of {@link PatTransferHandler} fills the whole grid at once. This
 * handler serves the other need: putting **one** item, or one fluid, into the slot the player
 * points at. Both live side by side, and neither replaces the other.
 *
 * <p>The client sends an intent, and writes nothing (decision D05). The server checks the
 * slot against the recipe type before it accepts the item.
 */
public class PatGhostHandler implements IGhostIngredientHandler<GuiPatEditor> {

    @Override
    public <I> List<Target<I>> getTargets(GuiPatEditor gui, I ingredient, boolean doStart) {
        // The conversion runs **before** the targets are built. A fluid with no bucket
        // therefore lights up nothing at all, instead of accepting a drop that would do
        // nothing.
        if (JeiIngredients.toStack(ingredient).isEmpty()) {
            return Collections.emptyList();
        }

        List<Target<I>> targets = new ArrayList<>();
        for (Map.Entry<Integer, Rectangle> entry : gui.editableSlotAreas().entrySet()) {
            targets.add(new SlotTarget<I>(entry.getKey(), entry.getValue()));
        }
        return targets;
    }

    @Override
    public void onComplete() {}

    /** One editable ghost slot, as a drop target. */
    private static final class SlotTarget<I> implements Target<I> {

        private final int slot;
        private final Rectangle area;

        SlotTarget(int slot, Rectangle area) {
            this.slot = slot;
            this.area = area;
        }

        @Override
        public Rectangle getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            ItemStack stack = JeiIngredients.toStack(ingredient);
            if (stack.isEmpty()) {
                return;
            }
            PatNetwork.CHANNEL.sendToServer(new PacketEditorGhost(slot, stack));
        }
    }
}
