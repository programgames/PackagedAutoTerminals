package fr.julien.packagedautoterminals.network;

import java.nio.charset.StandardCharsets;

import fr.julien.packagedautoterminals.common.GroupNames;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Renames the machine group open in the editor.
 *
 * <p>The name goes to **every** machine of the group: a group has no stable identity, it is
 * rebuilt on every scan. See {@code GroupNames}.
 *
 * <p>The server reads this name from the wire, so it trusts neither its length nor its
 * content. See {@link #fromBytes}.
 */
public class PacketRenameGroup implements IMessage {

    public String name = "";

    public PacketRenameGroup() {}

    public PacketRenameGroup(String name) {
        this.name = name;
    }

    /**
     * Longest name accepted on the wire, in bytes.
     *
     * <p>A name holds {@link GroupNames#MAX_LENGTH} characters, and one character of a
     * language such as Japanese takes up to four bytes in UTF-8.
     */
    private static final int MAX_BYTES = GroupNames.MAX_LENGTH * 4;

    /**
     * Reads the name, and never allocates more than {@link #MAX_BYTES}.
     *
     * <p>FIXED: {@code ByteBufUtils.readUTF8String} reads the length the packet announces,
     * then that many bytes. A crafted packet could therefore announce a name of any size.
     * The length is now read first, then clamped against both that bound and the bytes
     * really present.
     *
     * <p>{@code GroupNames.sanitize} then drops the section sign and the control characters,
     * which would otherwise paint the row or carry a feedback separator.
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        int announced = ByteBufUtils.readVarInt(buf, 2);
        int length = Math.max(0, Math.min(Math.min(announced, MAX_BYTES), buf.readableBytes()));
        name = GroupNames.sanitize(
                buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8));
        buf.readerIndex(buf.readerIndex() + length);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, GroupNames.sanitize(name));
    }

    public static class Handler implements IMessageHandler<PacketRenameGroup, IMessage> {
        @Override
        public IMessage onMessage(PacketRenameGroup message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatEditor) {
                    ((ContainerPatEditor) player.openContainer).renameGroup(message.name);
                }
            });
            return null;
        }
    }
}
