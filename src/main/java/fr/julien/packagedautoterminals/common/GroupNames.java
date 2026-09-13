package fr.julien.packagedautoterminals.common;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Noms donnés par le joueur aux groupes de machines.
 *
 * <p>Le nom est rangé **par machine**, et non par groupe : un groupe n'a pas d'identité
 * stable, il se recompose à chaque scan. Renommer un groupe écrit donc le même nom dans
 * toutes ses machines, et le groupe affiche le premier qu'il trouve. Le nom survit ainsi à
 * l'ajout ou au retrait d'une machine.
 *
 * <p>Le stockage appartient à notre mod : PackagedAuto n'en sait rien, et rien ne se perd si
 * ce mod est retiré.
 */
public class GroupNames extends WorldSavedData {

    private static final String NAME = "packagedautoterminals_names";
    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_POS = "Pos";
    private static final String KEY_NAME = "Name";

    /** Longueur maximale d'un nom. Au-delà, il ne tiendrait pas sur une rangée. */
    public static final int MAX_LENGTH = 32;

    private final java.util.Map<Long, String> names = new java.util.HashMap<>();

    public GroupNames() {
        super(NAME);
    }

    public GroupNames(String name) {
        super(name);
    }

    /** Instance du monde demandé. Elle est créée à la première utilisation. */
    public static GroupNames get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new GroupNames();
        }
        GroupNames data = (GroupNames) storage.getOrLoadData(GroupNames.class, NAME);
        if (data == null) {
            data = new GroupNames();
            storage.setData(NAME, data);
        }
        return data;
    }

    public String get(BlockPos pos) {
        String name = names.get(pos.toLong());
        return name == null ? "" : name;
    }

    public void set(BlockPos pos, String name) {
        if (name == null || name.trim().isEmpty()) {
            names.remove(pos.toLong());
        } else {
            names.put(pos.toLong(), name.trim().substring(0,
                    Math.min(MAX_LENGTH, name.trim().length())));
        }
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        names.clear();
        NBTTagList list = tag.getTagList(KEY_ENTRIES, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            names.put(entry.getLong(KEY_POS), entry.getString(KEY_NAME));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (java.util.Map.Entry<Long, String> entry : names.entrySet()) {
            NBTTagCompound stored = new NBTTagCompound();
            stored.setLong(KEY_POS, entry.getKey());
            stored.setString(KEY_NAME, entry.getValue());
            list.appendTag(stored);
        }
        tag.setTag(KEY_ENTRIES, list);
        return tag;
    }
}
