package fr.julien.packagedautoterminals.client;

import fr.julien.packagedautoterminals.Reference;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Opening key for the wireless terminal.
 *
 * <p>It ships **unbound**, code {@code 0}, like the four keys of ae2exttable. A key bound by
 * default would clash with the four AE2 keys, with the Shift + P of Cell Terminal, or with
 * any other mod in the pack. The player binds it themselves.
 *
 * <p>The context is {@code IN_GAME}, not {@code UNIVERSAL}: the
 * {@code InputEvent.KeyInputEvent} event only fires when no screen is open. Declaring a
 * wider context would report conflicts that cannot happen.
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
