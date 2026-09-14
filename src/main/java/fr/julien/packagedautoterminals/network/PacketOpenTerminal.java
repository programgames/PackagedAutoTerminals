package fr.julien.packagedautoterminals.network;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * « La touche est tombée. Ouvre mon terminal. »
 *
 * <p>Le paquet ne porte aucune donnée : le serveur cherche l'objet lui-même. Le client ne
 * saurait pas dire lequel des deux terminaux possibles est valide, car la portée, la clé de
 * liaison et l'énergie ne vivent que sur le serveur.
 *
 * <p>Ordre de recherche, repris de Cell Terminal :
 *
 * <ol>
 *   <li>l'inventaire principal, pour notre terminal sans fil ;
 *   <li>la main gauche, emplacement 40 ;
 *   <li>le terminal universel d'AE2WUT qui a absorbé notre mode, aux mêmes endroits.
 * </ol>
 *
 * <p>Notre terminal passe en premier : il est le choix explicite du joueur. Le terminal
 * universel sert de secours.
 */
public class PacketOpenTerminal implements IMessage {

    /** Emplacement de la main gauche, dans l'indexation continue de l'inventaire. */
    private static final int OFFHAND = 40;
    /** Derniers emplacements de l'inventaire principal, barre d'action comprise. */
    private static final int MAIN_SIZE = 36;

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketOpenTerminal, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenTerminal message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            // PIÈGE de Forge : le gestionnaire tourne sur le fil réseau. Ouvrir une fenêtre
            // depuis ce fil produit des états incohérents. On repasse par le fil du serveur.
            player.getServerWorld().addScheduledTask(() -> open(player));
            return null;
        }

        private void open(EntityPlayerMP player) {
            if (tryOurs(player) || tryUniversal(player)) {
                return;
            }
            player.sendStatusMessage(
                    new TextComponentTranslation("gui.packagedautoterminals.no_terminal"), true);
        }

        /** Notre propre terminal sans fil. */
        private boolean tryOurs(EntityPlayerMP player) {
            for (int slot = 0; slot < MAIN_SIZE; slot++) {
                ItemStack stack = player.inventory.getStackInSlot(slot);
                if (stack.getItem() instanceof ItemWirelessPatTerminal && check(player, stack, slot)) {
                    return true;
                }
            }
            ItemStack offhand = player.inventory.getStackInSlot(OFFHAND);
            return offhand.getItem() instanceof ItemWirelessPatTerminal
                    && check(player, offhand, OFFHAND);
        }

        /**
         * Le terminal universel d'AE2WUT, s'il a absorbé notre mode.
         *
         * <p>Le mode courant n'entre pas dans la recherche. Le joueur qui appuie sur notre
         * touche veut notre terminal, quel que soit le réglage de sa molette. Le serveur
         * bascule donc le mode avant d'ouvrir.
         */
        private boolean tryUniversal(EntityPlayerMP player) {
            for (int slot = 0; slot < MAIN_SIZE; slot++) {
                ItemStack stack = player.inventory.getStackInSlot(slot);
                if (WutSupport.hasOurMode(stack) && check(player, stack, slot)) {
                    WutSupport.switchToOurMode(stack);
                    return true;
                }
            }
            ItemStack offhand = player.inventory.getStackInSlot(OFFHAND);
            if (WutSupport.hasOurMode(offhand) && check(player, offhand, OFFHAND)) {
                WutSupport.switchToOurMode(offhand);
                return true;
            }
            return false;
        }

        /**
         * Trois contrôles, puis l'ouverture. L'ordre suit celui de Cell Terminal et d'AE2.
         *
         * @return vrai si l'objet est **le bon candidat**, même quand un contrôle échoue.
         *     Sans cette nuance, la recherche continuerait après un terminal non lié, et le
         *     joueur n'apprendrait jamais pourquoi rien ne s'ouvre.
         */
        private boolean check(EntityPlayerMP player, ItemStack stack, int slot) {
            IWirelessTermHandler handler =
                    AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
            if (handler == null) {
                return false;
            }

            String key = handler.getEncryptionKey(stack);
            if (key.isEmpty()) {
                refuse(player, "gui.packagedautoterminals.not_linked");
                return true;
            }

            // PIÈGE : le constructeur de WirelessTerminalGuiObject appelle Long.parseLong sur
            // cette clé. Une clé illisible y lèverait une exception.
            ILocatable station;
            try {
                station = AEApi.instance().registries().locatable()
                        .getLocatableBy(Long.parseLong(key));
            } catch (NumberFormatException malformed) {
                refuse(player, "gui.packagedautoterminals.not_linked");
                return true;
            }
            if (station == null) {
                refuse(player, "gui.packagedautoterminals.not_linked");
                return true;
            }

            if (!handler.hasPower(player, ItemWirelessPatTerminal.powerPerOpen(), stack)) {
                refuse(player, "gui.packagedautoterminals.no_power");
                return true;
            }

            player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS,
                    player.world, slot, 0, 0);
            return true;
        }

        private void refuse(EntityPlayerMP player, String key) {
            player.sendStatusMessage(new TextComponentTranslation(key), true);
        }
    }
}
