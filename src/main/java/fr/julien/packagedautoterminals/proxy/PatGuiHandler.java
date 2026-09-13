package fr.julien.packagedautoterminals.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * AE2 ouvre ses propres fenêtres par une énumération interne, fermée aux mods tiers. On
 * passe donc par le gestionnaire de FML.
 *
 * <p>L'identifiant porte la fenêtre voulue et sa source :
 *
 * <ul>
 *   <li>0 à 6 : terminal câblé, sur la face correspondante ;
 *   <li>10 à 16 : éditeur ouvert depuis cette part ;
 *   <li>20 : terminal sans fil. La coordonnée x porte l'emplacement d'inventaire ;
 *   <li>21 : éditeur ouvert depuis le terminal sans fil.
 * </ul>
 */
public class PatGuiHandler implements IGuiHandler {

    public static final int TERMINAL = 0;
    public static final int EDITOR = 10;
    public static final int WIRELESS = 20;
    public static final int WIRELESS_EDITOR = 21;

    /**
     * Cible de l'édition en cours, par joueur.
     *
     * <p>Motif : {@code openGui} ne transporte que trois entiers, déjà pris par la position
     * ou l'emplacement d'inventaire. La machine visée et le rang de la recette passent donc
     * par ici. Le serveur écrit l'entrée juste avant d'ouvrir la fenêtre, et la relit
     * aussitôt. Le contexte du terminal, lui, se reconstruit depuis l'identifiant : il n'a
     * pas besoin de cette table.
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
        // Le client ignore la cible : seul le serveur l'utilise, au moment d'écrire.
        return new GuiPatEditor(player.inventory, context,
                new EditorInventory(world, null), 0, BlockPos.ORIGIN, -1);
    }

    private static boolean isEditor(int id) {
        return id == WIRELESS_EDITOR || (id >= EDITOR && id < WIRELESS);
    }

    /** Reconstruit la source du terminal à partir de l'identifiant. */
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
     * Construit l'objet de fenêtre sans fil d'AE2.
     *
     * <p>PIÈGE : son constructeur lit la clé de liaison et appelle {@code Long.parseLong}.
     * Sur un terminal jamais lié, la clé est vide et l'appel lève une exception. AE2 vérifie
     * ce point avant de construire l'objet ; nous devons faire de même.
     */
    private static TerminalContext wireless(EntityPlayer player, World world, int slot) {
        if (slot < 0 || slot >= player.inventory.getSizeInventory()) {
            return null;
        }
        ItemStack stack = player.inventory.getStackInSlot(slot);
        if (!(stack.getItem() instanceof ItemWirelessPatTerminal)) {
            return null;
        }
        ItemWirelessPatTerminal handler = (ItemWirelessPatTerminal) stack.getItem();
        if (handler.getEncryptionKey(stack).isEmpty()) {
            return null;
        }
        return TerminalContext.ofWireless(
                new WirelessTerminalGuiObject(handler, stack, player, world, slot, 0, 0));
    }

    /** Machine visée et rang de la recette. Un rang négatif signifie « nouvelle recette ». */
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
