package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Changes the amount in one editor slot.
 *
 * <p>{@code Processing} recipes need exact amounts. PackagedAuto opens a dedicated screen
 * for that. The terminal keeps it simpler: the wheel over a slot adjusts its amount.
 */
public class PacketEditorSlot implements IMessage {

    public int slot;
    public int delta;
    /** True: {@link #delta} is the wanted amount. False: it is a step to add. */
    public boolean absolute;
    /** True: every other filled slot follows the same ratio. */
    public boolean scale;

    public PacketEditorSlot() {}

    public PacketEditorSlot(int slot, int delta) {
        this(slot, delta, false, false);
    }

    public PacketEditorSlot(int slot, int amount, boolean absolute) {
        this(slot, amount, absolute, false);
    }

    public PacketEditorSlot(int slot, int amount, boolean absolute, boolean scale) {
        this.slot = slot;
        this.delta = amount;
        this.absolute = absolute;
        this.scale = scale;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        delta = buf.readInt();
        absolute = buf.readBoolean();
        scale = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(delta);
        buf.writeBoolean(absolute);
        buf.writeBoolean(scale);
    }

    public static class Handler implements IMessageHandler<PacketEditorSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketEditorSlot message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatEditor) {
                    ContainerPatEditor editor = (ContainerPatEditor) player.openContainer;
                    if (message.absolute) {
                        editor.setSlotCount(message.slot, message.delta, message.scale);
                    } else {
                        editor.changeSlotCount(message.slot, message.delta);
                    }
                }
            });
            return null;
        }
    }
}
