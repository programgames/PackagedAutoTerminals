package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Renomme le groupe de machines ouvert dans l'éditeur.
 *
 * <p>Le nom part vers **toutes** les machines du groupe : un groupe n'a pas d'identité
 * stable, il se recompose à chaque scan. Voir {@code GroupNames}.
 */
public class PacketRenameGroup implements IMessage {

    public String name = "";

    public PacketRenameGroup() {}

    public PacketRenameGroup(String name) {
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
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
