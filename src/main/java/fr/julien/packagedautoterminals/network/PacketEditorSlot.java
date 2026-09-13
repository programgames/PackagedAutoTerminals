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
    /** Vrai : {@link #delta} est la quantité voulue. Faux : c'est un pas à ajouter. */
    public boolean absolute;

    public PacketEditorSlot() {}

    public PacketEditorSlot(int slot, int delta) {
        this(slot, delta, false);
    }

    public PacketEditorSlot(int slot, int amount, boolean absolute) {
        this.slot = slot;
        this.delta = amount;
        this.absolute = absolute;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        delta = buf.readInt();
        absolute = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(delta);
        buf.writeBoolean(absolute);
    }

    public static class Handler implements IMessageHandler<PacketEditorSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketEditorSlot message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatEditor) {
                    ContainerPatEditor editor = (ContainerPatEditor) player.openContainer;
                    if (message.absolute) {
                        editor.setSlotCount(message.slot, message.delta);
                    } else {
                        editor.changeSlotCount(message.slot, message.delta);
                    }
                }
            });
            return null;
        }
    }
}
