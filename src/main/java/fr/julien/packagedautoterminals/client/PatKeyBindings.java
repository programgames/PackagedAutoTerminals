package fr.julien.packagedautoterminals.client;

import fr.julien.packagedautoterminals.Reference;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Touche d'ouverture du terminal sans fil.
 *
 * <p>Elle part **non liée**, code {@code 0}, comme les quatre touches d'ae2exttable. Une
 * touche liée d'office entrerait en conflit avec les quatre d'AE2, avec le Maj + P de Cell
 * Terminal, ou avec n'importe quel autre mod du pack. Le joueur la règle lui-même.
 *
 * <p>Le contexte est {@code IN_GAME}, et non {@code UNIVERSAL} : l'événement
 * {@code InputEvent.KeyInputEvent} ne se déclenche que sans fenêtre ouverte. Annoncer un
 * contexte plus large ferait signaler des conflits qui ne peuvent pas se produire.
 */
@SideOnly(Side.CLIENT)
public final class PatKeyBindings {

    private static final String CATEGORY = "key." + Reference.MOD_ID + ".category";

    public static final KeyBinding OPEN_TERMINAL = new KeyBinding(
            "key." + Reference.MOD_ID + ".open_terminal",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, 0, CATEGORY);

    private PatKeyBindings() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_TERMINAL);
    }
}
