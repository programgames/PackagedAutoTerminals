package fr.julien.packagedautoterminals.integration.wut;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.network.PacketOpenTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Opens our screen when the player clicks a universal terminal set to our mode.
 *
 * <p>Why an event, and not a third injection: the AE2WUT right click lives in
 * {@code Item.onItemRightClick}, a **Minecraft** method. Its name differs between the
 * development workspace and the shipped game, {@code onItemRightClick} on one side,
 * {@code func_77659_a} on the other. Injecting there would need a remapping table, hence an
 * annotation processor, hence a whole extra build chain. The Forge event does the same work,
 * and it runs **before** the item method.
 */
public final class WutEventHandler {

    private WutEventHandler() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!WutSupport.isOurMode(stack)) {
            return;
        }

        // PITFALL: **never** cancel on the client. `PlayerControllerMP.processRightClick`
        // returns as soon as the event is cancelled, and no longer sends the right-click
        // packet. The server would see nothing, and no screen would open. We let the client
        // click run its course: the AE2WUT method has no case for our mode, so it does
        // nothing.
        if (event.getWorld().isRemote) {
            return;
        }

        // On the server, cancelling stops AE2WUT from opening its own screen on top.
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

        // Same fix as ItemWirelessPatTerminal, and it mattered more here: the event is already
        // cancelled above, so a universal terminal used from the offhand opened neither our
        // screen nor the AE2WUT one.
        int slot = event.getHand() == net.minecraft.util.EnumHand.OFF_HAND
                ? PacketOpenTerminal.OFFHAND
                : player.inventory.currentItem;
        player.openGui(PackagedAutoTerminals.instance, PatGuiHandler.WIRELESS,
                event.getWorld(), slot, 0, 0);
    }
}
