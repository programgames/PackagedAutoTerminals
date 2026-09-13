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
 * Découvre les machines qui portent des recettes, sur une grille ME.
 *
 * <p>Le critère est {@code instanceof IPackageProvidingMachine} (décision D02). Ce sont les
 * seuls blocs qui portent un Package Recipe Holder. Les crafters n'en portent aucun : leur
 * champ {@code currentRecipe} ne dure que le temps d'un craft.
 *
 * <p>Le parcours passe par {@code getMachinesClasses()} puis {@code getMachines(cls)}, et non
 * par {@code getNodes()}, qui traverserait aussi chaque câble. Aucune classe d'addon n'est
 * connue à la compilation : tout addon futur est capté sans modifier ce code.
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
     * Retrouve une machine par sa position, **sur la grille du terminal**.
     *
     * <p>Toute écriture passe par ici. Le client envoie une position ; le serveur ne lui
     * fait pas confiance. Si la machine n'est pas sur cette grille, la méthode renvoie
     * {@code null} et l'ordre est ignoré.
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

    /** Traduction sans passer par le client, utilisable des deux côtés. */
    public static String translate(String key, Object... args) {
        return I18n.translateToLocalFormatted(key, args);
    }
}
