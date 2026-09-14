package fr.julien.packagedautoterminals.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.helpers.WirelessTerminalGuiObject;
import fr.julien.packagedautoterminals.client.gui.GuiPatEditor;
import fr.julien.packagedautoterminals.client.gui.GuiPatTerminal;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.TerminalContext;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import thelm.packagedauto.api.IRecipeInfo;

/**
 * AE2 opens its own screens through an internal enum, closed to third-party mods. We
 * therefore go through the FML handler.
 *
 * <p>The id carries both the wanted screen and its source:
 *
 * <ul>
 *   <li>0 to 6: wired terminal, on the matching face;
 *   <li>10 to 16: editor opened from that part;
 *   <li>20: wireless terminal. The x coordinate carries the inventory slot;
 *   <li>21: editor opened from the wireless terminal.
 * </ul>
 */
public class PatGuiHandler implements IGuiHandler {

    public static final int TERMINAL = 0;
    public static final int EDITOR = 10;
    public static final int WIRELESS = 20;
    public static final int WIRELESS_EDITOR = 21;

    /**
     * Target of the current edit, per player.
     *
     * <p>Reason: {@code openGui} only carries three integers, already taken by the position
     * or the inventory slot. The target machine and the recipe index therefore go through
     * here. The server writes the entry just before opening the screen, and reads it back
     * right away. The terminal context, on the other hand, is rebuilt from the id: it does
     * not need this table.
     */
    private static final Map<UUID, EditTarget> PENDING = new HashMap<>();

    public static void setPendingEdit(EntityPlayer player, int dimension, BlockPos pos, int index) {
        PENDING.put(player.getUniqueID(), new EditTarget(dimension, pos, index));
    }

    public static void setPendingRecipe(EntityPlayer player, IRecipeInfo recipe) {
        EditTarget target = PENDING.get(player.getUniqueID());
        if (target != null) {
            target.recipe = recipe;
        }
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TerminalContext context = context(id, player, world, x, y, z);
        if (context == null) {
            return null;
        }
        if (!isEditor(id)) {
            return new ContainerPatTerminal(player.inventory, context);
        }

        EditTarget target = PENDING.remove(player.getUniqueID());
        if (target == null) {
            return null;
        }
        EditorInventory editor = new EditorInventory(world, ContainerPatEditor.defaultRecipeType());
        if (target.recipe != null) {
            editor.load(target.recipe);
        }
        return new ContainerPatEditor(player.inventory, context, editor,
                target.dimension, target.pos, target.index);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TerminalContext context = context(id, player, world, x, y, z);
        if (context == null) {
            return null;
        }
        if (!isEditor(id)) {
            return new GuiPatTerminal(player.inventory, context);
        }
        // The client ignores the target: only the server uses it, at write time.
        return new GuiPatEditor(player.inventory, context,
                new EditorInventory(world, null), 0, BlockPos.ORIGIN, -1);
    }

    private static boolean isEditor(int id) {
        return id == WIRELESS_EDITOR || (id >= EDITOR && id < WIRELESS);
    }

    /** Rebuilds the terminal source from the id. */
    private static TerminalContext context(int id, EntityPlayer player, World world,
                                           int x, int y, int z) {
        if (id >= WIRELESS) {
            return wireless(player, world, x);
        }
        int side = id - (id >= EDITOR ? EDITOR : TERMINAL);
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof IPartHost)) {
            return null;
        }
        IPart part = ((IPartHost) tile).getPart(AEPartLocation.fromOrdinal(side));
        return part instanceof PartPatTerminal
                ? TerminalContext.ofPart((PartPatTerminal) part)
                : null;
    }

    /**
     * Builds the AE2 wireless screen object.
     *
     * <p>PITFALL: its constructor reads the link key and calls {@code Long.parseLong}. On a
     * terminal that was never linked, the key is empty and the call throws. AE2 checks this
     * before building the object; we must do the same.
     *
     * <p>Two items can open this screen: ours, and the Wireless Universal Terminal set to
     * our mode. The handler therefore comes from the AE2 registry, not from a cast to our
     * class. Everything else — range, energy, key — goes through
     * {@code WirelessTerminalGuiObject}, which only knows the interface.
     */
    private static TerminalContext wireless(EntityPlayer player, World world, int slot) {
        if (slot < 0 || slot >= player.inventory.getSizeInventory()) {
            return null;
        }
        ItemStack stack = player.inventory.getStackInSlot(slot);
        boolean ours = stack.getItem() instanceof ItemWirelessPatTerminal;
        if (!ours && !WutSupport.isOurMode(stack)) {
            return null;
        }
        IWirelessTermHandler handler =
                AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
        if (handler == null || handler.getEncryptionKey(stack).isEmpty()) {
            return null;
        }
        return TerminalContext.ofWireless(
                new WirelessTerminalGuiObject(handler, stack, player, world, slot, 0, 0));
    }

    /** Target machine and recipe index. A negative index means "new recipe". */
    private static final class EditTarget {
        final int dimension;
        final BlockPos pos;
        final int index;
        IRecipeInfo recipe;

        EditTarget(int dimension, BlockPos pos, int index) {
            this.dimension = dimension;
            this.pos = pos;
            this.index = index;
        }
    }
}
