package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
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

    public NBTTagCompound writeToNBT() {
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
        tag.setTag("Recipes", MiscUtil.writeRecipeListToNBT(new NBTTagList(), recipes));
        return tag;
    }

    public static ProviderSnapshot readFromNBT(NBTTagCompound tag) {
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
        snapshot.recipes = MiscUtil.readRecipeListFromNBT(tag.getTagList("Recipes", 10));
        return snapshot;
    }
}
