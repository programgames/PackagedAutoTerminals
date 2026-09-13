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
 * Une machine fournisseuse de patterns, telle que le terminal l'affiche.
 *
 * <p>La sérialisation des recettes réutilise {@link MiscUtil}, donc le format NBT de
 * PackagedAuto lui-même. Le terminal n'invente aucun format, et ne peut donc pas diverger
 * de l'amont.
 */
public class ProviderSnapshot {

    /** Identifiant stable pour cette session de GUI. Sert aux ordres du client. */
    public int id;
    /** Nom affiché de la machine. */
    public String name = "";
    /** Icône de la machine, fournie par {@code IGridBlock.getMachineRepresentation()}. */
    public ItemStack icon = ItemStack.EMPTY;
    public int dimension;
    public BlockPos pos = BlockPos.ORIGIN;
    /** Le nœud de grille est actif : alimenté et doté d'un canal. */
    public boolean active;
    /** Un Package Recipe Holder occupe l'emplacement de la machine. */
    public boolean holderPresent;
    /** Packager, Unpackager, ou machine qui se suffit. Décide de l'appariement. */
    public ProviderRole role = ProviderRole.UNKNOWN;
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
        snapshot.recipes = MiscUtil.readRecipeListFromNBT(tag.getTagList("Recipes", 10));
        return snapshot;
    }
}
