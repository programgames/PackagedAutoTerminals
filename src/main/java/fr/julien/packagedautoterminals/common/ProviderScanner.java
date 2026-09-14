package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;

/**
 * Discovers the machines that carry recipes, on an ME grid.
 *
 * <p>The criterion is {@code instanceof IPackageProvidingMachine} (decision D02). These are
 * the only blocks that hold a Package Recipe Holder. Crafters hold none: their
 * {@code currentRecipe} field only lives for the duration of one craft.
 *
 * <p>The walk goes through {@code getMachinesClasses()} then {@code getMachines(cls)}, not
 * through {@code getNodes()}, which would also visit every cable. No addon class is known at
 * compile time: any future addon is picked up without changing this code.
 */
public final class ProviderScanner {

    private ProviderScanner() {}

    public static List<ProviderSnapshot> scan(IGrid grid) {
        List<ProviderSnapshot> found = new ArrayList<>();
        if (grid == null) {
            return found;
        }
        int nextId = 0;
        for (Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
            if (!IPackageProvidingMachine.class.isAssignableFrom(machineClass)) {
                continue;
            }
            for (IGridNode node : grid.getMachines(machineClass)) {
                IGridHost machine = node.getMachine();
                if (!(machine instanceof IPackageProvidingMachine)) {
                    continue;
                }
                found.add(describe(nextId++, node, (IPackageProvidingMachine) machine));
            }
        }
        return found;
    }

    /**
     * Finds a machine by its position, **on the terminal grid**.
     *
     * <p>Every write goes through here. The client sends a position; the server does not
     * trust it. When the machine is not on this grid, the method returns {@code null} and
     * the command is ignored.
     */
    public static IPackageProvidingMachine find(IGrid grid, int dimension, BlockPos pos) {
        if (grid == null) {
            return null;
        }
        for (Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
            if (!IPackageProvidingMachine.class.isAssignableFrom(machineClass)) {
                continue;
            }
            for (IGridNode node : grid.getMachines(machineClass)) {
                IGridHost machine = node.getMachine();
                if (!(machine instanceof IPackageProvidingMachine) || !(machine instanceof TileEntity)) {
                    continue;
                }
                TileEntity tile = (TileEntity) machine;
                if (tile.getPos().equals(pos)
                        && tile.getWorld().provider.getDimension() == dimension) {
                    return (IPackageProvidingMachine) machine;
                }
            }
        }
        return null;
    }

    private static ProviderSnapshot describe(int id, IGridNode node, IPackageProvidingMachine machine) {
        ProviderSnapshot snapshot = new ProviderSnapshot();
        snapshot.id = id;
        snapshot.active = node.isActive();
        snapshot.role = ProviderRole.of(machine);
        snapshot.icon = node.getGridBlock().getMachineRepresentation();
        snapshot.name = snapshot.icon.isEmpty()
                ? machine.getClass().getSimpleName()
                : snapshot.icon.getDisplayName();

        if (machine instanceof TileEntity) {
            TileEntity tile = (TileEntity) machine;
            snapshot.pos = tile.getPos();
            snapshot.dimension = tile.getWorld().provider.getDimension();
            if (!tile.getWorld().isRemote) {
                snapshot.customName = GroupNames.get(tile.getWorld()).get(tile.getPos());
            }
        }

        ItemStack holder = machine.getPatternStack();
        snapshot.holderPresent = !holder.isEmpty();
        if (snapshot.holderPresent && holder.getItem() instanceof IRecipeListItem) {
            IRecipeList list = ((IRecipeListItem) holder.getItem()).getRecipeList(holder);
            if (list != null && list.getRecipeList() != null) {
                snapshot.recipes.addAll(list.getRecipeList());
            }
        }
        return snapshot;
    }

    /** Translation without going through the client, usable on both sides. */
    public static String translate(String key, Object... args) {
        return I18n.translateToLocalFormatted(key, args);
    }
}
