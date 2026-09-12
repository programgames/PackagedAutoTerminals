package fr.julien.packagedautoterminals.proxy;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import fr.julien.packagedautoterminals.client.gui.GuiPatTerminal;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * AE2 ouvre ses propres GUIs par une énumération interne, fermée aux mods tiers. On passe
 * donc par le gestionnaire de FML. L'identifiant porte la face de la part.
 */
public class PatGuiHandler implements IGuiHandler {

    private static PartPatTerminal findTerminal(World world, int id, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof IPartHost)) {
            return null;
        }
        IPart part = ((IPartHost) tile).getPart(AEPartLocation.fromOrdinal(id));
        return part instanceof PartPatTerminal ? (PartPatTerminal) part : null;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        PartPatTerminal terminal = findTerminal(world, id, x, y, z);
        return terminal == null ? null : new ContainerPatTerminal(player.inventory, terminal);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        PartPatTerminal terminal = findTerminal(world, id, x, y, z);
        return terminal == null ? null : new GuiPatTerminal(player.inventory, terminal);
    }
}
