package fr.julien.packagedautoterminals.network;

import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.MiscUtil;

/**
 * Edit command, from the client to the server.
 *
 * <p>The client sends an **intent**, never a recipe to write (decision D05). It names the machine
 * by its position, not by a session id: the order of a scan can change between two refreshes.
 * The server then checks that this machine really is on the terminal grid, and that the
 * player is allowed to touch it.
 */
public class PacketRecipeAction implements IMessage {

    /** Removes {@link #recipe}, which the client saw at index {@link #index}. */
    public static final byte ACTION_REMOVE = 0;
    /** Opens the editor on {@link #recipe}, which the client saw at index {@link #index}. */
    public static final byte ACTION_EDIT = 1;
    /** Writes the editor recipe. The editor stays open. */
    public static final byte ACTION_SAVE = 2;
    /** Changes the recipe type in the editor. {@link #index} is 1 forward, 0 backward. */
    public static final byte ACTION_CYCLE_TYPE = 3;
    /** Adds a recipe to this machine, and opens the editor on it. */
    public static final byte ACTION_NEW = 4;
    /** Takes the recipe holder out of this machine, and stores it in the network. */
    public static final byte ACTION_REMOVE_HOLDER = 5;
    /** Closes the editor and reopens the terminal. */
    public static final byte ACTION_BACK = 6;
    /** Deletes the recipe being edited. */
    public static final byte ACTION_DELETE = 7;
    /** Clears the editor grid, without writing anything. */
    public static final byte ACTION_CLEAR = 8;
    /** Scrolls the tab row. {@link #index} is 1 forward, 0 backward. */
    public static final byte ACTION_SCROLL_TABS = 9;
    /** Takes a blank recipe holder out of the network for each machine of the group. */
    public static final byte ACTION_INSERT_HOLDER = 10;
    /** Copies every recipe of the group into the machines of the group that lack it. */
    public static final byte ACTION_REPAIR_GROUP = 11;
    /** Holder mode: copies the recipes missing from one holder into it, both ways. */
    public static final byte ACTION_SYNC_HOLDERS = 12;
    /** Opens the holder mode from the terminal. */
    public static final byte ACTION_OPEN_HOLDERS = 13;
    /** Scales every recipe (D44). {@link #index} is the factor, negative to divide. */
    public static final byte ACTION_SCALE_ALL = 14;

    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    public int index;
    public byte action;
    /**
     * The recipe that {@link #ACTION_REMOVE} and {@link #ACTION_EDIT} name, in the PackagedAuto
     * NBT format, as the snapshot carried it, or {@code null}. The server only looks it up in its
     * own lists and writes its own copy, never this one.
     */
    public NBTTagCompound recipe;

    public PacketRecipeAction() {}

    public PacketRecipeAction(int dimension, BlockPos pos, int index, byte action) {
        this(dimension, pos, index, action, null);
    }

    public PacketRecipeAction(int dimension, BlockPos pos, int index, byte action,
                              NBTTagCompound recipe) {
        this.dimension = dimension;
        this.pos = pos;
        this.index = index;
        this.action = action;
        this.recipe = recipe;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        pos = BlockPos.fromLong(buf.readLong());
        index = buf.readInt();
        action = buf.readByte();
        recipe = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeLong(pos.toLong());
        buf.writeInt(index);
        buf.writeByte(action);
        ByteBufUtils.writeTag(buf, recipe);
    }

    public static class Handler implements IMessageHandler<PacketRecipeAction, IMessage> {
        @Override
        public IMessage onMessage(PacketRecipeAction message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatTerminal) {
                    ContainerPatTerminal container = (ContainerPatTerminal) player.openContainer;
                    IRecipeInfo recipe = message.recipe == null
                            ? null
                            : MiscUtil.readRecipeFromNBT(message.recipe);
                    if (message.action == ACTION_REMOVE) {
                        container.removeRecipe(message.dimension, message.pos, message.index,
                                recipe);
                    } else if (message.action == ACTION_EDIT) {
                        container.openEditor(message.dimension, message.pos, message.index,
                                recipe);
                    } else if (message.action == ACTION_NEW) {
                        container.newRecipe(message.dimension, message.pos);
                    } else if (message.action == ACTION_REMOVE_HOLDER) {
                        container.removeHolder(message.dimension, message.pos);
                    } else if (message.action == ACTION_INSERT_HOLDER) {
                        container.insertHolders(message.dimension, message.pos);
                    } else if (message.action == ACTION_REPAIR_GROUP) {
                        container.repairGroup(message.dimension, message.pos);
                    } else if (message.action == ACTION_OPEN_HOLDERS) {
                        container.openHolderEditor();
                    }
                    return;
                }
                if (player.openContainer instanceof ContainerPatEditor) {
                    ContainerPatEditor editor = (ContainerPatEditor) player.openContainer;
                    if (message.action == ACTION_SAVE) {
                        // We stay in the editor: the player must see the message, and be
                        // able to chain a second edit.
                        editor.save();
                    } else if (message.action == ACTION_BACK) {
                        editor.backToTerminal();
                    } else if (message.action == ACTION_CYCLE_TYPE) {
                        editor.cycleRecipeType(message.index == 1);
                    } else if (message.action == ACTION_DELETE) {
                        editor.deleteCurrent();
                    } else if (message.action == ACTION_CLEAR) {
                        editor.clearGrid();
                    } else if (message.action == ACTION_SCROLL_TABS) {
                        editor.scrollTabs(message.index == 1);
                    } else if (message.action == ACTION_SYNC_HOLDERS) {
                        editor.syncHolders();
                    } else if (message.action == ACTION_SCALE_ALL) {
                        editor.scaleAll(Math.abs(message.index), message.index < 0);
                    }
                }
            });
            return null;
        }
    }
}
