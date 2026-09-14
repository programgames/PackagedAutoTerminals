package fr.julien.packagedautoterminals.mixin.ae2wut;

import java.util.Arrays;

import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds our terminal to the AE2WUT mode list.
 *
 * <p>The target class is named by its **name**, not by {@code ItemWirelessUniversal
 * Terminal.class}. Reason: that class carries MekEng, AE2FC and ae2exttable types in its
 * signatures. Naming it by class would force those four mods onto the compile path, against
 * rule 7. By name, nothing is needed.
 *
 * <p>The two target methods carry **no** Minecraft type in their signature: {@code ()[I} and
 * {@code (I)Ljava/lang/String;}. No remapping table is therefore required, and the mixin
 * applies the same way in development and in game.
 *
 * <p>{@code require = 1} makes startup fail when AE2WUT changes shape. That is deliberate: a
 * clean stop beats a terminal that disappears without a word.
 */
@Mixin(targets = "com.circulation.ae2wut.item.ItemWirelessUniversalTerminal", remap = false)
public abstract class MixinWutTerminal {

    /**
     * Our mode joins the list the universal terminal can carry.
     *
     * <p>The rest follows on its own: {@code allMode} feeds the "all in one" recipe and the
     * wheel, which computes its bound from the highest mode present.
     */
    @Inject(method = "getAllMode", at = @At("RETURN"), cancellable = true, require = 1)
    private static void packagedautoterminals$addMode(CallbackInfoReturnable<int[]> callback) {
        int[] modes = callback.getReturnValue();
        for (int existing : modes) {
            if (existing == WutSupport.mode()) {
                return;
            }
        }
        int[] extended = Arrays.copyOf(modes, modes.length + 1);
        extended[modes.length] = WutSupport.mode();
        callback.setReturnValue(extended);
    }

    /**
     * Name of our mode, shown after the item name and in its tooltip.
     *
     * <p>AE2WUT returns an empty string for an unknown mode: without this injection, the
     * player would see "Wireless Universal Terminal" without knowing what it is set to.
     */
    @Inject(method = "getWirelessName", at = @At("HEAD"), cancellable = true, require = 1)
    private static void packagedautoterminals$name(int value,
                                                   CallbackInfoReturnable<String> callback) {
        if (value == WutSupport.mode()) {
            callback.setReturnValue(WutSupport.modeName());
        }
    }
}
