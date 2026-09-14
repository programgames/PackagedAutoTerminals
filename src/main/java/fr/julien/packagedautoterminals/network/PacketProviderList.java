package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.client.ClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Full snapshot of the providing machines. Server to client. */
public class PacketProviderList implements IMessage {

    public NBTTagCompound payload = new NBTTagCompound();
    public int payloadBytes;

    public PacketProviderList() {}

    public PacketProviderList(NBTTagCompound payload) {
        this.payload = payload;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        payloadBytes = buf.readableBytes();
        payload = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, payload);
    }

    public static class Handler implements IMessageHandler<PacketProviderList, IMessage> {
        @Override
        public IMessage onMessage(PacketProviderList message, MessageContext context) {
            net.minecraft.client.Minecraft.getMinecraft()
                    .addScheduledTask(() -> ClientHandler.applyProviderList(message));
            return null;
        }
    }
}
