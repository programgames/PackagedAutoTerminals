package fr.julien.packagedautoterminals.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import fr.julien.packagedautoterminals.client.ClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Snapshot of the providing machines, or its size when it is too big to send. Server to client. */
public class PacketProviderList implements IMessage {

    /**
     * Counted bytes past which the server sends the size of the snapshot instead of the snapshot.
     *
     * <p>The client reads the snapshot as one tag with {@code PacketBuffer.readCompoundTag}. Past
     * 2 097 152 counted bytes, that read throws, and FML disconnects the player.
     */
    public static final long CAP = 2_000_000L;

    public NBTTagCompound payload = new NBTTagCompound();
    public int payloadBytes;

    public PacketProviderList() {}

    public PacketProviderList(NBTTagCompound payload) {
        this.payload = payload;
    }

    /**
     * The snapshot itself, or, when it exceeds {@link #CAP}, its counted size under the key
     * {@code TooBig}, next to the {@code MachinesTab} setting of the server.
     */
    public static PacketProviderList capped(NBTTagCompound snapshot) {
        long counted = countedBytes(snapshot);
        if (counted <= CAP) {
            return new PacketProviderList(snapshot);
        }
        NBTTagCompound notice = new NBTTagCompound();
        notice.setLong("TooBig", counted);
        notice.setBoolean("MachinesTab", snapshot.getBoolean("MachinesTab"));
        return new PacketProviderList(notice);
    }

    /** Bytes that {@code NBTSizeTracker} counts when the client reads this tag. */
    private static long countedBytes(NBTTagCompound tag) {
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        Counter counter = new Counter();
        try {
            CompressedStreamTools.write(tag, new DataOutputStream(written));
            CompressedStreamTools.read(
                    new DataInputStream(new ByteArrayInputStream(written.toByteArray())), counter);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return counter.counted;
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

    private static final class Counter extends NBTSizeTracker {

        private long counted;

        Counter() {
            super(Long.MAX_VALUE);
        }

        @Override
        public void read(long bits) {
            counted += bits / 8L;
        }
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
