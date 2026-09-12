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
 * Une machine qui **exécute** les colis : Package Crafter, Basic à Ultimate, Extreme…
 *
 * <p>Ces machines ne portent aucune recette. Elles ne figurent donc pas dans l'onglet des
 * patterns (décision D02). L'onglet Machines les liste pour une seule raison : dire quelles
 * recettes encodées n'ont personne pour les exécuter.
 */
public class MachineSnapshot {

    public String name = "";
    public ItemStack icon = ItemStack.EMPTY;
    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    public boolean active;
    public boolean busy;
    /** Nom de la classe de la tuile. Sert à la correspondance avec les types de recettes. */
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

    /** Parcourt la grille et décrit chaque machine d'exécution. */
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

        // isBusy() interroge la machine, sans rien exécuter. C'est la seule information
        // d'état que l'API expose.
        snapshot.busy = machine.isBusy();

        if (machine instanceof TileEntity) {
            TileEntity tile = (TileEntity) machine;
            snapshot.pos = tile.getPos();
            snapshot.dimension = tile.getWorld().provider.getDimension();
        }
        return snapshot;
    }
}
