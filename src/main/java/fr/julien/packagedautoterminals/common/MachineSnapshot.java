package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import thelm.packagedauto.api.IPackageCraftingMachine;

/**
 * A machine that **runs** the packages: Package Crafter, Basic to Ultimate, Extreme, and so
 * on.
 *
 * <p>These machines carry no recipe. They therefore do not appear in the patterns tab
 * (decision D02). The Machines tab lists them for one reason only: to tell which encoded
 * recipes have nobody to run them.
 */
public class MachineSnapshot {

    public String name = "";
    public ItemStack icon = ItemStack.EMPTY;
    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    public boolean active;
    public boolean busy;
    /** Name of the tile class. Used to match against the recipe types. */
    public String machineClass = "";

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", name);
        tag.setTag("Icon", icon.writeToNBT(new NBTTagCompound()));
        tag.setInteger("Dim", dimension);
        tag.setLong("Pos", pos.toLong());
        tag.setBoolean("Active", active);
        tag.setBoolean("Busy", busy);
        tag.setString("Class", machineClass);
        return tag;
    }

    public static MachineSnapshot readFromNBT(NBTTagCompound tag) {
        MachineSnapshot snapshot = new MachineSnapshot();
        snapshot.name = tag.getString("Name");
        snapshot.icon = new ItemStack(tag.getCompoundTag("Icon"));
        snapshot.dimension = tag.getInteger("Dim");
        snapshot.pos = BlockPos.fromLong(tag.getLong("Pos"));
        snapshot.active = tag.getBoolean("Active");
        snapshot.busy = tag.getBoolean("Busy");
        snapshot.machineClass = tag.getString("Class");
        return snapshot;
    }

    /** Walks the grid and describes every crafting machine. */
    public static List<MachineSnapshot> scan(IGrid grid) {
        List<MachineSnapshot> found = new ArrayList<>();
        if (grid == null) {
            return found;
        }
        for (Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
            if (!IPackageCraftingMachine.class.isAssignableFrom(machineClass)) {
                continue;
            }
            for (IGridNode node : grid.getMachines(machineClass)) {
                IGridHost host = node.getMachine();
                if (!(host instanceof IPackageCraftingMachine)) {
                    continue;
                }
                found.add(describe(node, (IPackageCraftingMachine) host));
            }
        }
        return found;
    }

    private static MachineSnapshot describe(IGridNode node, IPackageCraftingMachine machine) {
        MachineSnapshot snapshot = new MachineSnapshot();
        snapshot.active = node.isActive();
        snapshot.icon = node.getGridBlock().getMachineRepresentation();
        snapshot.machineClass = machine.getClass().getName();
        snapshot.name = snapshot.icon.isEmpty()
                ? machine.getClass().getSimpleName()
                : snapshot.icon.getDisplayName();

        // isBusy() queries the machine without running anything. It is the only state
        // information the API exposes.
        snapshot.busy = machine.isBusy();

        if (machine instanceof TileEntity) {
            TileEntity tile = (TileEntity) machine;
            snapshot.pos = tile.getPos();
            snapshot.dimension = tile.getWorld().provider.getDimension();
        }
        return snapshot;
    }
}
