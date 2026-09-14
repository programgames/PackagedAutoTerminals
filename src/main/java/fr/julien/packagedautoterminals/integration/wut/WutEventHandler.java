package fr.julien.packagedautoterminals.integration.wut;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Ouvre notre fenêtre quand le joueur clique un terminal universel réglé sur notre mode.
 *
 * <p>Pourquoi un événement, et non une troisième greffe : le clic droit d'AE2WUT vit dans
 * {@code Item.onItemRightClick}, une méthode **de Minecraft**. Son nom change entre le poste
 * de développement et le jeu publié, {@code onItemRightClick} d'un côté, {@code func_77659_a}
 * de l'autre. Y greffer quelque chose exigerait une table de remappage, donc un processeur
 * d'annotations, donc toute une chaîne de compilation de plus. L'événement de Forge fait le
 * même travail, et il passe **avant** la méthode de l'objet.
 */
public final class WutEventHandler {

    private WutEventHandler() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!WutSupport.isOurMode(stack)) {
            return;
        }

        // PIÈGE : ne **jamais** annuler côté client. `PlayerControllerMP.processRightClick`
        // sort dès que l'événement est annulé, et n'envoie plus le paquet de clic droit. Le
        // serveur ne verrait donc rien, et aucune fenêtre ne s'ouvrirait. On laisse le clic
        // client suivre son cours : la méthode d'AE2WUT n'a aucun cas pour notre mode, donc
        // elle ne fait rien.
        if (event.getWorld().isRemote) {
            return;
        }

        // Côté serveur, l'annulation empêche AE2WUT d'ouvrir sa propre fenêtre par-dessus.
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);

        EntityPlayer player = event.getEntityPlayer();

        IWirelessTermHandler handler =
                AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
        if (handler == null) {
            return;
        }
        if (handler.getEncryptionKey(stack).isEmpty()) {
            player.sendStatusMessage(
                    new TextComponentTranslation("gui.packagedautoterminals.not_linked"), true);
            return;
        }
        if (!handler.hasPower(player, ItemWirelessPatTerminal.powerPerOpen(), stack)) {
            player.sendStatusMessage(
                    new TextComponentTranslation("gui.packagedautoterminals.no_power"), true);
            return;
        }

        player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS,
                event.getWorld(), player.inventory.currentItem, 0, 0);
    }
}
