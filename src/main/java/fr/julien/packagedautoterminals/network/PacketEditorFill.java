package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Fills the editor from a JEI recipe.
 *
 * <p>The client sends the target type and the "slot to item" mapping. The server checks each
 * slot before writing: a forged mapping therefore cannot fill a slot the type does not
 * enable.
 */
public class PacketEditorFill implements IMessage {

    private static final String KEY_SLOT = "Slot";
    private static final String KEY_STACK = "Stack";

    public int typeId;
    public Int2ObjectMap<ItemStack> stacks = new Int2ObjectOpenHashMap<>();

    public PacketEditorFill() {}

    public PacketEditorFill(int typeId, Int2ObjectMap<ItemStack> stacks) {
        this.typeId = typeId;
        this.stacks = stacks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        typeId = buf.readInt();
        int count = buf.readInt();
        stacks = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            if (tag != null) {
                stacks.put(tag.getInteger(KEY_SLOT),
                        new ItemStack(tag.getCompoundTag(KEY_STACK)));
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(typeId);
        buf.writeInt(stacks.size());
        for (Int2ObjectMap.Entry<ItemStack> entry : stacks.int2ObjectEntrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(KEY_SLOT, entry.getIntKey());
            tag.setTag(KEY_STACK, entry.getValue().writeToNBT(new NBTTagCompound()));
            ByteBufUtils.writeTag(buf, tag);
        }
    }

    public static class Handler implements IMessageHandler<PacketEditorFill, IMessage> {
        @Override
        public IMessage onMessage(PacketEditorFill message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerPatEditor)) {
                    return;
                }
                ContainerPatEditor container = (ContainerPatEditor) player.openContainer;
                container.fillFromRecipe(message.typeId, message.stacks);
            });
            return null;
        }
    }

    /** Maximum size accepted, as a safety net: the editor grid. */
    public static int maxSlots() {
        return EditorInventory.SIZE;
    }
}
