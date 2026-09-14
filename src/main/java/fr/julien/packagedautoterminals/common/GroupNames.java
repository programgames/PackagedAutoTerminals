package fr.julien.packagedautoterminals.common;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Names the player gives to machine groups.
 *
 * <p>The name is stored **per machine**, not per group: a group has no stable identity, it
 * is rebuilt on every scan. Renaming a group therefore writes the same name into all of its
 * machines, and the group shows the first one it finds. The name thus survives a machine
 * being added or removed.
 *
 * <p>The storage belongs to our mod: PackagedAuto knows nothing about it, and nothing is
 * lost when this mod is removed.
 */
public class GroupNames extends WorldSavedData {

    private static final String NAME = "packagedautoterminals_names";
    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_POS = "Pos";
    private static final String KEY_NAME = "Name";

    /** Maximum length of a name. Beyond that it would not fit on a row. */
    public static final int MAX_LENGTH = 32;

    private final java.util.Map<Long, String> names = new java.util.HashMap<>();

    public GroupNames() {
        super(NAME);
    }

    public GroupNames(String name) {
        super(name);
    }

    /** Instance for the given world. It is created on first use. */
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
