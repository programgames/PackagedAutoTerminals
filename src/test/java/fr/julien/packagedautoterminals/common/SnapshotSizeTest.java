package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.recipe.RecipeTypeProcessing;

/**
 * Size of the terminal snapshot, against the limit of the client. Revision R2 asks for it.
 *
 * <p>The client reads the whole snapshot as one tag, through
 * {@code PacketBuffer.readCompoundTag}, which counts 2 097 152 bytes at most. Past it, the
 * client is disconnected.
 */
public class SnapshotSizeTest {

    private static final long CLIENT_LIMIT = 2097152L;

    @Test
    public void theSnapshotLimitIsMeasured() throws IOException {
        int pairs = 0;
        while (clientAccepts(snapshot(pairs + 1, 20))) {
            pairs++;
        }
        System.out.printf("Snapshot limit: %d pairs of 2 machines with 20 processing recipes,"
                + " %d counted bytes%n", pairs, countedBytes(snapshot(pairs, 20)));
        assertTrue(pairs > 0);
    }

    @Test
    public void theSnapshotOfThePlayersNetworkFitsWithAMargin() throws IOException {
        // Decision D38: 20 machines and 110 recipes, hence 10 pairs of 11 recipes.
        long counted = countedBytes(snapshot(10, 11));
        System.out.printf("Snapshot of the player's network: %d counted bytes, %d%% of the limit%n",
                counted, counted * 100 / CLIENT_LIMIT);
        assertTrue(counted <= CLIENT_LIMIT / 2);
    }

    /** The tag {@code ContainerPatTerminal.detectAndSendChanges} sends, with no Machines tab. */
    private static NBTTagCompound snapshot(int pairs, int recipesPerMachine) {
        NBTTagList providers = new NBTTagList();
        for (int pair = 0; pair < pairs; pair++) {
            for (ProviderRole role : new ProviderRole[] {ProviderRole.PACKAGER, ProviderRole.UNPACKAGER}) {
                ProviderSnapshot machine = new ProviderSnapshot();
                machine.id = providers.tagCount();
                machine.name = role == ProviderRole.PACKAGER ? "Packager" : "Unpackager";
                machine.pos = new BlockPos(-1200 + pair, 64, 3400 + role.ordinal());
                machine.active = true;
                machine.holderPresent = true;
                machine.role = role;
                for (int recipe = 0; recipe < recipesPerMachine; recipe++) {
                    machine.recipes.add(new ProcessingRecipe());
                }
                providers.appendTag(machine.writeToNBT());
            }
        }
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag("Providers", providers);
        wrapper.setTag("Machines", new NBTTagList());
        return wrapper;
    }

    private static boolean clientAccepts(NBTTagCompound wrapper) throws IOException {
        try {
            countedBytes(wrapper);
            return true;
        } catch (RuntimeException tooBig) {
            return false;
        }
    }

    /** Writes the tag, then reads it back the way the client does, and counts. */
    private static long countedBytes(NBTTagCompound wrapper) throws IOException {
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        CompressedStreamTools.write(wrapper, new DataOutputStream(written));
        ClientTracker tracker = new ClientTracker();
        CompressedStreamTools.read(
                new DataInputStream(new ByteArrayInputStream(written.toByteArray())), tracker);
        return tracker.counted;
    }

    /** The tracker of {@code PacketBuffer.readCompoundTag}, which also keeps its count. */
    private static final class ClientTracker extends NBTSizeTracker {

        long counted;

        ClientTracker() {
            super(CLIENT_LIMIT);
        }

        @Override
        public void read(long bits) {
            counted += bits / 8L;
            super.read(bits);
        }
    }

    /** Four inputs and one output, written as {@code RecipeInfoProcessing} writes them. */
    private static final class ProcessingRecipe extends FakeRecipe {

        ProcessingRecipe() {
            super("processing");
        }

        @Override
        public IRecipeType getRecipeType() {
            return RecipeTypeProcessing.INSTANCE;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setTag("Input", stacks(4));
            tag.setTag("Output", stacks(1));
            return tag;
        }
    }

    /** Stacks as {@code MiscUtil.saveAllItems} writes them. */
    private static NBTTagList stacks(int count) {
        NBTTagList list = new NBTTagList();
        for (int index = 0; index < count; index++) {
            NBTTagCompound stack = new NBTTagCompound();
            stack.setByte("Index", (byte) index);
            stack.setString("id", "thermalfoundation:material");
            stack.setByte("Count", (byte) 4);
            stack.setShort("Damage", (short) 128);
            list.appendTag(stack);
        }
        return list;
    }
}
