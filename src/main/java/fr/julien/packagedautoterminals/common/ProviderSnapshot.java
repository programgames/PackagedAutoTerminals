package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.MiscUtil;

/**
 * A pattern-providing machine, as the terminal shows it.
 *
 * <p>Recipe serialisation reuses {@link MiscUtil}, hence the NBT format of PackagedAuto
 * itself. The terminal invents no format, and therefore cannot drift from upstream.
 */
public class ProviderSnapshot {

    /** Stable id for this GUI session. Used by the client commands. */
    public int id;
    /** Display name of the machine. */
    public String name = "";
    /** Machine icon, provided by {@code IGridBlock.getMachineRepresentation()}. */
    public ItemStack icon = ItemStack.EMPTY;
    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    /** The grid node is active: powered and given a channel. */
    public boolean active;
    /** A Package Recipe Holder sits in the machine slot. */
    public boolean holderPresent;
    /** Packager, Unpackager, or self-sufficient machine. Drives the pairing. */
    public ProviderRole role = ProviderRole.UNKNOWN;
    /** Name given by the player, or empty string. Stored by our mod, not by PackagedAuto. */
    public String customName = "";
    public List<IRecipeInfo> recipes = new ArrayList<>();

    /**
     * The machine itself, on the server only.
     *
     * <p>It never travels: {@link #writeToNBT} ignores it, so it is always {@code null} on
     * the client. It saves {@link RecipeWriter} from walking the whole grid again for every
     * machine it writes to, which made one save cost a scan per target.
     *
     * <p>A write happens in the same tick as the scan that produced this snapshot, so the
     * reference cannot age. {@link RecipeWriter} still tests the tile before using it, and
     * falls back to a fresh search.
     */
    public transient IPackageProvidingMachine machine;

    /**
     * Writes the machines into the snapshot tag. Each distinct recipe list travels once: the
     * two machines of a pair point to the same copy.
     */
    public static void writeAll(List<ProviderSnapshot> snapshots, NBTTagCompound wrapper) {
        NBTTagList providers = new NBTTagList();
        NBTTagList recipeLists = new NBTTagList();
        Map<NBTTagList, Integer> indexes = new HashMap<>();
        for (ProviderSnapshot snapshot : snapshots) {
            NBTTagList recipes = MiscUtil.writeRecipeListToNBT(new NBTTagList(), snapshot.recipes);
            int index = indexes.computeIfAbsent(recipes, list -> {
                recipeLists.appendTag(list);
                return recipeLists.tagCount() - 1;
            });
            NBTTagCompound tag = snapshot.writeToNBT();
            tag.setInteger("Recipes", index);
            providers.appendTag(tag);
        }
        wrapper.setTag("Providers", providers);
        wrapper.setTag("RecipeLists", recipeLists);
    }

    /** Reads the machines written by {@link #writeAll}. */
    public static List<ProviderSnapshot> readAll(NBTTagCompound wrapper) {
        NBTTagList recipeLists = wrapper.getTagList("RecipeLists", 9);
        NBTTagList providers = wrapper.getTagList("Providers", 10);
        List<ProviderSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < providers.tagCount(); i++) {
            NBTTagCompound tag = providers.getCompoundTagAt(i);
            ProviderSnapshot snapshot = readFromNBT(tag);
            snapshot.recipes = MiscUtil.readRecipeListFromNBT(
                    (NBTTagList) recipeLists.get(tag.getInteger("Recipes")));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Id", id);
        tag.setString("Name", name);
        tag.setTag("Icon", icon.writeToNBT(new NBTTagCompound()));
        tag.setInteger("Dim", dimension);
        tag.setLong("Pos", pos.toLong());
        tag.setBoolean("Active", active);
        tag.setBoolean("Holder", holderPresent);
        tag.setInteger("Role", role.ordinal());
        tag.setString("CustomName", customName == null ? "" : customName);
        return tag;
    }

    private static ProviderSnapshot readFromNBT(NBTTagCompound tag) {
        ProviderSnapshot snapshot = new ProviderSnapshot();
        snapshot.id = tag.getInteger("Id");
        snapshot.name = tag.getString("Name");
        snapshot.icon = new ItemStack(tag.getCompoundTag("Icon"));
        snapshot.dimension = tag.getInteger("Dim");
        snapshot.pos = BlockPos.fromLong(tag.getLong("Pos"));
        snapshot.active = tag.getBoolean("Active");
        snapshot.holderPresent = tag.getBoolean("Holder");
        snapshot.role = ProviderRole.fromOrdinal(tag.getInteger("Role"));
        snapshot.customName = tag.getString("CustomName");
        return snapshot;
    }
}
