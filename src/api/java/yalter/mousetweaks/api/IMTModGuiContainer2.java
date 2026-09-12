package yalter.mousetweaks.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

/**
 * API publique de Mouse Tweaks, requise à la compilation seulement.
 *
 * <p>Motif : {@code appeng.client.gui.AEBaseGui} implémente cette interface. Sans elle sur
 * le chemin de compilation, toute classe qui hérite d'AEBaseGui échoue sur
 * {@code cannot access IMTModGuiContainer2}. Mouse Tweaks publie cette API pour que les
 * mods l'embarquent. Les signatures reprennent exactement l'original.
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
