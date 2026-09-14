package fr.julien.packagedautoterminals.client;

import fr.julien.packagedautoterminals.network.PacketOpenTerminal;
import fr.julien.packagedautoterminals.network.PatNetwork;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Sends the opening intent to the server.
 *
 * <p>The client decides nothing: it only reports that the key went down. The server looks
 * for the item, checks the binding, the security station and the energy, then opens the
 * screen. This is rule 2 of the project, and it is what Cell Terminal does too.
 */
@SideOnly(Side.CLIENT)
public final class PatKeyHandler {

    private PatKeyHandler() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // `isPressed` returns true only once per press. `isKeyDown` would send one packet
        // per frame for as long as the key stays held down.
        if (PatKeyBindings.OPEN_TERMINAL.isPressed()) {
            PatNetwork.CHANNEL.sendToServer(new PacketOpenTerminal());
        }
    }
}
