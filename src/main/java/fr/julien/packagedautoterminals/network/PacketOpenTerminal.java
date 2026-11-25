package fr.julien.packagedautoterminals.network;

import java.util.function.Predicate;

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

    /**
     * Offhand slot, in the flat inventory indexing.
     *
     * <p>Public, because every path that opens the terminal needs it: an item used from the
     * offhand must name that slot, and not the hotbar slot the player happens to have selected.
     */
    public static final int OFFHAND = 40;
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
            int slot = find(player, stack -> stack.getItem() instanceof ItemWirelessPatTerminal);
            if (slot < 0) {
                // A universal terminal counts whatever its wheel says, and PatGuiHandler opens it
                // only in our mode: the switch comes before the opening.
                slot = find(player, WutSupport::hasOurMode);
                if (slot >= 0) {
                    WutSupport.switchToOurMode(player.inventory.getStackInSlot(slot));
                }
            }
            if (slot < 0) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "gui.packagedautoterminals.no_terminal"), true);
                return;
            }
            // The search stops at the first candidate, even one that PatGuiHandler refuses: the
            // player then reads why it does not open.
            player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS,
                    player.world, slot, 0, 0);
        }

        /** First slot of the main inventory, then the offhand, that holds a wanted item, or -1. */
        private static int find(EntityPlayerMP player, Predicate<ItemStack> wanted) {
            for (int slot = 0; slot < MAIN_SIZE; slot++) {
                if (wanted.test(player.inventory.getStackInSlot(slot))) {
                    return slot;
                }
            }
            return wanted.test(player.inventory.getStackInSlot(OFFHAND)) ? OFFHAND : -1;
        }
    }
}
