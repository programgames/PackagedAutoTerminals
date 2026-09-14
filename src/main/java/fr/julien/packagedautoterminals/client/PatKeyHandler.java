package fr.julien.packagedautoterminals.client;

import fr.julien.packagedautoterminals.network.PacketOpenTerminal;
import fr.julien.packagedautoterminals.network.PatNetwork;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Envoie l'intention d'ouverture au serveur.
 *
 * <p>Le client ne décide de rien : il dit seulement que la touche est tombée. Le serveur
 * cherche l'objet, vérifie la liaison, la station de sécurité et l'énergie, puis ouvre la
 * fenêtre. C'est la règle 2 du projet, et c'est aussi ce que fait Cell Terminal.
 */
@SideOnly(Side.CLIENT)
public final class PatKeyHandler {

    private PatKeyHandler() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // `isPressed` ne rend vrai qu'une fois par appui. `isKeyDown` enverrait un paquet
        // par image tant que la touche reste enfoncée.
        if (PatKeyBindings.OPEN_TERMINAL.isPressed()) {
            PatNetwork.CHANNEL.sendToServer(new PacketOpenTerminal());
        }
    }
}
