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
 * "The key went down. Open my terminal."
 *
 * <p>The packet carries no data: the server looks for the item itself. The client could not
 * tell which of the two possible terminals is valid, because range, link key and energy only
 * live on the server.
 *
 * <p>Search order, taken from Cell Terminal:
 *
 * <ol>
 *   <li>the main inventory, for our wireless terminal;
 *   <li>the offhand, slot 40;
 *   <li>the AE2WUT universal terminal that absorbed our mode, in the same places.
 * </ol>
 *
 * <p>Our terminal comes first: it is the explicit choice of the player. The universal
 * terminal is the fallback.
 */
public class PacketOpenTerminal implements IMessage {

    /** Offhand slot, in the flat inventory indexing. */
    private static final int OFFHAND = 40;
    /** Size of the main inventory, hotbar included. */
    private static final int MAIN_SIZE = 36;

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketOpenTerminal, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenTerminal message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            // Forge PITFALL: the handler runs on the network thread. Opening a screen from
            // that thread produces inconsistent state. We go back to the server thread.
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

        /** Our own wireless terminal. */
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
         * The AE2WUT universal terminal, when it absorbed our mode.
         *
         * <p>The current mode plays no part in the search. A player who presses our key
         * wants our terminal, whatever their wheel is set to. The server therefore switches
         * the mode before opening.
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
         * Three checks, then the opening. The order follows Cell Terminal and AE2.
         *
         * @return true when the item is **the right candidate**, even when a check fails.
         *     Without that nuance, the search would continue past an unlinked terminal, and
         *     the player would never learn why nothing opens.
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

            // PITFALL: the WirelessTerminalGuiObject constructor calls Long.parseLong on
            // this key. An unreadable key would throw there.
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
