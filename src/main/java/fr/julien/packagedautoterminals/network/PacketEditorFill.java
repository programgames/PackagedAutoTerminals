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
 * Remplit l'éditeur depuis une recette de JEI.
 *
 * <p>Le client envoie le type visé et la correspondance « emplacement → objet ». Le serveur
 * vérifie chaque emplacement avant d'écrire : une correspondance falsifiée ne peut donc pas
 * remplir un emplacement que le type n'active pas.
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

    /** Taille maximale acceptée, par sécurité : la grille de l'éditeur. */
    public static int maxSlots() {
        return EditorInventory.SIZE;
    }
}
