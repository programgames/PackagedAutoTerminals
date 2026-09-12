package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Change la quantité d'un emplacement de l'éditeur.
 *
 * <p>Les recettes de type {@code Processing} demandent des quantités précises. PackagedAuto
 * ouvre pour cela une fenêtre dédiée. Le terminal fait plus simple : la molette au-dessus
 * d'un emplacement ajuste sa quantité.
 */
public class PacketEditorSlot implements IMessage {

    public int slot;
    public int delta;

    public PacketEditorSlot() {}

    public PacketEditorSlot(int slot, int delta) {
        this.slot = slot;
        this.delta = delta;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        delta = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(delta);
    }

    public static class Handler implements IMessageHandler<PacketEditorSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketEditorSlot message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatEditor) {
                    ((ContainerPatEditor) player.openContainer)
                            .changeSlotCount(message.slot, message.delta);
                }
            });
            return null;
        }
    }
}
