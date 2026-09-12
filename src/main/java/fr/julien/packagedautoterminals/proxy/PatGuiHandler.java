package fr.julien.packagedautoterminals.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import fr.julien.packagedautoterminals.client.gui.GuiPatEditor;
import fr.julien.packagedautoterminals.client.gui.GuiPatTerminal;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import thelm.packagedauto.api.IRecipeInfo;

/**
 * AE2 ouvre ses propres fenêtres par une énumération interne, fermée aux mods tiers. On
 * passe donc par le gestionnaire de FML.
 *
 * <p>L'identifiant porte deux informations : la fenêtre voulue, et la face de la part.
 */
public class PatGuiHandler implements IGuiHandler {

    /** Identifiants 0 à 5 : le terminal, sur la face correspondante. */
    public static final int TERMINAL = 0;
    /** Identifiants 10 à 15 : l'éditeur de recette. */
    public static final int EDITOR = 10;

    /**
     * Cible de l'édition en cours, par joueur.
     *
     * <p>Motif : {@code openGui} ne transporte qu'une position et un identifiant. La machine
     * visée et le rang de la recette passent donc par ici. Le serveur écrit l'entrée juste
     * avant d'ouvrir la fenêtre, et la relit aussitôt.
     */
    private static final Map<UUID, EditTarget> PENDING = new HashMap<>();

    public static void setPendingEdit(EntityPlayer player, int dimension, BlockPos pos, int index) {
        PENDING.put(player.getUniqueID(), new EditTarget(dimension, pos, index));
    }

    private static PartPatTerminal findTerminal(World world, int id, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof IPartHost)) {
            return null;
        }
        IPart part = ((IPartHost) tile).getPart(AEPartLocation.fromOrdinal(id % EDITOR));
        return part instanceof PartPatTerminal ? (PartPatTerminal) part : null;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        PartPatTerminal terminal = findTerminal(world, id, x, y, z);
        if (terminal == null) {
            return null;
        }
        if (id < EDITOR) {
            return new ContainerPatTerminal(player.inventory, terminal);
        }

        EditTarget target = PENDING.remove(player.getUniqueID());
        if (target == null) {
            return null;
        }
        EditorInventory editor = new EditorInventory(world, null);
        if (target.recipe != null) {
            editor.load(target.recipe);
        }
        return new ContainerPatEditor(player.inventory, terminal, editor,
                target.dimension, target.pos, target.index);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        PartPatTerminal terminal = findTerminal(world, id, x, y, z);
        if (terminal == null) {
            return null;
        }
        if (id < EDITOR) {
            return new GuiPatTerminal(player.inventory, terminal);
        }
        // Le client ignore la cible : le serveur seule l'utilise, au moment d'écrire.
        return new GuiPatEditor(player.inventory, terminal,
                new EditorInventory(world, null), 0, BlockPos.ORIGIN, -1);
    }

    /** Machine visée et rang de la recette. Un rang négatif signifie « nouvelle recette ». */
    public static final class EditTarget {
        final int dimension;
        final BlockPos pos;
        final int index;
        IRecipeInfo recipe;

        EditTarget(int dimension, BlockPos pos, int index) {
            this.dimension = dimension;
            this.pos = pos;
            this.index = index;
        }

        public void setRecipe(IRecipeInfo recipe) {
            this.recipe = recipe;
        }
    }

    /** Complète la cible en attente avec la recette à charger dans l'éditeur. */
    public static void setPendingRecipe(EntityPlayer player, IRecipeInfo recipe) {
        EditTarget target = PENDING.get(player.getUniqueID());
        if (target != null) {
            target.setRecipe(recipe);
        }
    }
}
