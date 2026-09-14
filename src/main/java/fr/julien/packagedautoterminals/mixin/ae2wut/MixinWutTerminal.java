package fr.julien.packagedautoterminals.mixin.ae2wut;

import java.util.Arrays;

import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ajoute notre terminal à la liste des modes d'AE2WUT.
 *
 * <p>La classe visée est désignée par son **nom**, et non par {@code ItemWirelessUniversal
 * Terminal.class}. Motif : cette classe porte dans ses signatures des types de MekEng,
 * d'AE2FC et d'ae2exttable. La citer par sa classe obligerait à mettre ces quatre mods sur
 * le chemin de compilation, contre la règle 7. Par son nom, rien n'est nécessaire.
 *
 * <p>Les deux méthodes visées n'ont **aucun** type de Minecraft dans leur signature :
 * {@code ()[I} et {@code (I)Ljava/lang/String;}. Aucune table de remappage n'est donc
 * requise, et le mixin s'applique de la même façon en développement et en jeu.
 *
 * <p>{@code require = 1} fait échouer le démarrage si AE2WUT change de forme. C'est
 * voulu : mieux vaut un arrêt net qu'un terminal qui disparaît sans un mot.
 */
@Mixin(targets = "com.circulation.ae2wut.item.ItemWirelessUniversalTerminal", remap = false)
public abstract class MixinWutTerminal {

    /**
     * Notre mode rejoint la liste que le terminal universel sait porter.
     *
     * <p>Le reste suit tout seul : {@code allMode} alimente la recette « tout en un » et la
     * molette, qui calcule sa borne à partir du plus grand mode présent.
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
     * Nom de notre mode, affiché à la suite de celui de l'objet et dans son infobulle.
     *
     * <p>AE2WUT rend une chaîne vide pour un mode inconnu : sans cette greffe, le joueur
     * verrait « Wireless Universal Terminal » sans savoir sur quoi il est réglé.
     */
    @Inject(method = "getWirelessName", at = @At("HEAD"), cancellable = true, require = 1)
    private static void packagedautoterminals$name(int value,
                                                   CallbackInfoReturnable<String> callback) {
        if (value == WutSupport.mode()) {
            callback.setReturnValue(WutSupport.modeName());
        }
    }
}
