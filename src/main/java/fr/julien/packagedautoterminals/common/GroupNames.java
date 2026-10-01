package fr.julien.packagedautoterminals.common;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

/**
 * Names the player gives to machine groups.
 *
 * <p>The name is stored <b>per machine</b>, not per group: a group has no stable identity, it
 * is rebuilt on every scan. Renaming a group therefore writes the same name into all of its
 * machines, and the group shows the first one it finds. The name thus survives a machine
 * being added or removed.
 *
 * <p>The storage belongs to our mod: PackagedAuto knows nothing about it, and nothing is
 * lost when this mod is removed.
 *
 * <p>FIXED, two defects at once. The data used to live in {@code world.getMapStorage()} of
 * whatever world the caller held, and the key was the position alone.
 *
 * <ol>
 *   <li><b>One file, several instances.</b> {@code WorldServer.init} builds
 *       {@code this.mapStorage = new MapStorage(this.saveHandler)} for every dimension, and
 *       that save handler points at the <b>same</b> {@code data} folder. Two dimensions
 *       therefore held two instances of this class over one file, and the last one to save
 *       erased the names of the other. Everything now goes through the overworld, which gives
 *       a single instance.
 *   <li><b>A position is not an identity.</b> A Packager at 100, 64, 100 in the Nether shared
 *       its entry with a Packager at 100, 64, 100 in the overworld. The key now carries the
 *       dimension.
 * </ol>
 *
 * <p>An entry written before this fix carries no dimension. It is read as dimension zero,
 * which is where a single player world keeps its machines.
 */
public class GroupNames extends WorldSavedData {

    private static final String NAME = "packagedautoterminals_names";
    private static final String KEY_ENTRIES = "Entries";
    private static final String KEY_POS = "Pos";
    private static final String KEY_DIM = "Dim";
    private static final String KEY_NAME = "Name";

    /** Maximum length of a name. Beyond that it would not fit on a row. */
    public static final int MAX_LENGTH = 32;

    /** Key of one machine: its dimension and its position. */
    private final Map<String, String> names = new HashMap<>();

    public GroupNames() {
        super(NAME);
    }

    public GroupNames(String name) {
        super(name);
    }

    /**
     * The single instance of the server.
     *
     * <p>The overworld carries it, because its {@code MapStorage} is the one every dimension
     * writes to. The world passed in only serves as a fallback, for a client world or a test
     * where dimension zero is not loaded.
     */
    public static GroupNames get(World world) {
        World host = DimensionManager.getWorld(0);
        if (host == null) {
            host = world;
        }
        MapStorage storage = host == null ? null : host.getMapStorage();
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

    private static String keyOf(int dimension, BlockPos pos) {
        return dimension + ":" + pos.toLong();
    }

    public String get(int dimension, BlockPos pos) {
        String name = names.get(keyOf(dimension, pos));
        return name == null ? "" : name;
    }

    public void set(int dimension, BlockPos pos, String name) {
        String clean = sanitize(name);
        if (clean.isEmpty()) {
            names.remove(keyOf(dimension, pos));
        } else {
            names.put(keyOf(dimension, pos), clean);
        }
        markDirty();
    }

    /**
     * Cleans a name that comes from the network.
     *
     * <p>The section sign starts a colour code in every Minecraft screen. A name carrying it
     * would paint the rest of the row, and could hide the state of the group. The sign and
     * the letter behind it both go: a colour code is two characters, and keeping the letter
     * would leave a stray "c" in the middle of the name. Control characters are dropped for
     * the same reason: the feedback separator is one of them.
     *
     * <p>The result is cut to {@link #MAX_LENGTH} characters, never to bytes: cutting bytes
     * would split a character of a language that needs several of them.
     */
    public static String sanitize(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder(MAX_LENGTH);
        for (int i = 0; i < name.length() && clean.length() < MAX_LENGTH; i++) {
            char character = name.charAt(i);
            if (character == '§') {
                // A colour code is two characters. Dropping the sign alone would leave the
                // letter behind, so "§cred" would read "cred".
                i++;
                continue;
            }
            if (character < ' ' || character == '\u007f') {
                continue;
            }
            clean.append(character);
        }
        return clean.toString().trim();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        names.clear();
        NBTTagList list = tag.getTagList(KEY_ENTRIES, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            // An entry written before the fix carries no dimension. getInteger answers zero,
            // which is the overworld, and that is where a single player world sits.
            names.put(keyOf(entry.getInteger(KEY_DIM), BlockPos.fromLong(entry.getLong(KEY_POS))),
                    entry.getString(KEY_NAME));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            int separator = entry.getKey().indexOf(':');
            NBTTagCompound stored = new NBTTagCompound();
            stored.setInteger(KEY_DIM, Integer.parseInt(entry.getKey().substring(0, separator)));
            stored.setLong(KEY_POS, Long.parseLong(entry.getKey().substring(separator + 1)));
            stored.setString(KEY_NAME, entry.getValue());
            list.appendTag(stored);
        }
        tag.setTag(KEY_ENTRIES, list);
        return tag;
    }
}
