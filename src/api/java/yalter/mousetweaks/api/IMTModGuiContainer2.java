package yalter.mousetweaks.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

/**
 * Public Mouse Tweaks API, required at compile time only.
 *
 * <p>Reason: {@code appeng.client.gui.AEBaseGui} implements this interface. Without it on the
 * compile path, every class that extends AEBaseGui fails with
 * {@code cannot access IMTModGuiContainer2}. Mouse Tweaks publishes this API so that mods can
 * bundle it. The signatures match the original exactly.
 */
public interface IMTModGuiContainer2 {

    boolean MT_isMouseTweaksDisabled();

    boolean MT_isWheelTweakDisabled();

    Container MT_getContainer();

    Slot MT_getSlotUnderMouse();

    boolean MT_isCraftingOutput(Slot slot);

    boolean MT_isIgnored(Slot slot);

    boolean MT_disableRMBDraggingFunctionality();
}
