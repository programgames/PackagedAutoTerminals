package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Drops one item into one ghost slot of the editor.
 *
 * <p>This is the drag and drop from JEI. A normal click needs no packet: the vanilla
 * container already carries it, through {@code slotClick}. A drag from JEI carries no held
 * item, so the client must name the item itself.
 *
 * <p>The server trusts neither the slot nor the item: {@code setGhostSlot} checks the slot
 * against the recipe type, and clamps the amount.
 */
public class PacketEditorGhost implements IMessage {

    public int slot;
    public ItemStack stack = ItemStack.EMPTY;

    public PacketEditorGhost() {}

    public PacketEditorGhost(int slot, ItemStack stack) {
        this.slot = slot;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        stack = ByteBufUtils.readItemStack(buf);
        if (stack == null) {
            stack = ItemStack.EMPTY;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        ByteBufUtils.writeItemStack(buf, stack);
    }

    public static class Handler implements IMessageHandler<PacketEditorGhost, IMessage> {
        @Override
        public IMessage onMessage(PacketEditorGhost message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.slot < 0 || message.slot >= EditorInventory.SIZE) {
                    return;
                }
                if (player.openContainer instanceof ContainerPatEditor) {
                    ((ContainerPatEditor) player.openContainer)
                            .setGhostSlot(message.slot, message.stack);
                }
            });
            return null;
        }
    }
}
