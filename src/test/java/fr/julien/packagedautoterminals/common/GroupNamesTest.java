package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

/**
 * The names the player gives to machine groups.
 *
 * <p>Two defects fixed here are checked below: a name was stored by position alone, so two
 * machines at the same coordinates in two dimensions shared it, and a name read from the wire
 * carried whatever the sender wrote.
 */
public class GroupNamesTest {

    private static final BlockPos POS = new BlockPos(100, 64, 100);

    @Test
    public void aNameIsReadBackWhereItWasWritten() {
        GroupNames names = new GroupNames();
        names.set(0, POS, "ingots");
        assertEquals("ingots", names.get(0, POS));
    }

    @Test
    public void twoDimensionsNoLongerShareOneEntry() {
        GroupNames names = new GroupNames();
        names.set(0, POS, "overworld");
        names.set(-1, POS, "nether");

        assertEquals("overworld", names.get(0, POS));
        assertEquals("nether", names.get(-1, POS));
    }

    @Test
    public void anUnknownMachineHasNoName() {
        assertEquals("", new GroupNames().get(0, POS));
    }

    @Test
    public void anEmptyNameRemovesTheEntry() {
        GroupNames names = new GroupNames();
        names.set(0, POS, "ingots");
        names.set(0, POS, "   ");
        assertEquals("", names.get(0, POS));
    }

    @Test
    public void theSectionSignIsDropped() {
        // It opens a colour code in every Minecraft screen, and would paint the rest of the
        // row, or hide the state of the group.
        assertEquals("red", GroupNames.sanitize("§cred"));
    }

    @Test
    public void controlCharactersAreDropped() {
        // The feedback separator is one of them.
        assertEquals("ab", GroupNames.sanitize("a\u0001b"));
    }

    @Test
    public void aLongNameIsCutToThirtyTwoCharacters() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            tooLong.append('x');
        }
        assertEquals(GroupNames.MAX_LENGTH, GroupNames.sanitize(tooLong.toString()).length());
    }

    @Test
    public void aNameOfAnotherAlphabetIsCutByCharacterAndNotByByte() {
        StringBuilder japanese = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            japanese.append('包');
        }
        String clean = GroupNames.sanitize(japanese.toString());
        assertEquals(GroupNames.MAX_LENGTH, clean.length());
        assertTrue(clean.indexOf('�') < 0);
    }

    @Test
    public void nullIsAnEmptyName() {
        assertEquals("", GroupNames.sanitize(null));
    }

    @Test
    public void theNamesSurviveASaveAndAReload() {
        GroupNames saved = new GroupNames();
        saved.set(0, POS, "overworld");
        saved.set(-1, POS, "nether");

        GroupNames loaded = new GroupNames();
        loaded.readFromNBT(saved.writeToNBT(new NBTTagCompound()));

        assertEquals("overworld", loaded.get(0, POS));
        assertEquals("nether", loaded.get(-1, POS));
    }

    @Test
    public void anEntryWrittenBeforeTheFixIsReadAsTheOverworld() {
        // The old format carried the position alone. A single player world keeps its
        // machines in dimension zero, which is what a missing field reads as.
        NBTTagCompound old = new NBTTagCompound();
        net.minecraft.nbt.NBTTagList entries = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setLong("Pos", POS.toLong());
        entry.setString("Name", "ingots");
        entries.appendTag(entry);
        old.setTag("Entries", entries);

        GroupNames loaded = new GroupNames();
        loaded.readFromNBT(old);

        assertEquals("ingots", loaded.get(0, POS));
    }
}
