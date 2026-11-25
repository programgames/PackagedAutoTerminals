package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.julien.packagedautoterminals.network.PacketProviderList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;
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
    /** The limit measured when each machine of a pair sent its own copy of the recipes. */
    private static final int PAIRS_WITHOUT_DEDUPLICATION = 27;

    @Test
    public void bothMachinesOfAPairPointToOneCopyOfTheirRecipes() {
        NBTTagCompound wrapper = snapshot(1, 20);

        assertEquals(1, wrapper.getTagList("RecipeLists", 9).tagCount());
        NBTTagList providers = wrapper.getTagList("Providers", 10);
        assertEquals(providers.getCompoundTagAt(0).getInteger("Recipes"),
                providers.getCompoundTagAt(1).getInteger("Recipes"));
    }

    @Test
    public void eachMachineReadsBackItsOwnRecipeList() {
        RecipeTypeRegistry.registerRecipeType(RecipeTypeProcessing.INSTANCE);
        List<ProviderSnapshot> written = new ArrayList<>();
        for (int recipes = 1; recipes <= 2; recipes++) {
            for (int side = 0; side < 2; side++) {
                ProviderSnapshot machine = new ProviderSnapshot();
                for (int recipe = 0; recipe < recipes; recipe++) {
                    machine.recipes.add(new EmptyProcessingRecipe());
                }
                written.add(machine);
            }
        }
        NBTTagCompound wrapper = new NBTTagCompound();
        ProviderSnapshot.writeAll(written, wrapper);

        List<ProviderSnapshot> read = ProviderSnapshot.readAll(wrapper);

        assertEquals(2, wrapper.getTagList("RecipeLists", 9).tagCount());
        assertEquals(4, read.size());
        for (int machine = 0; machine < 4; machine++) {
            assertEquals(written.get(machine).recipes.size(), read.get(machine).recipes.size());
        }
    }

    @Test
    public void theSnapshotLimitIsMeasured() throws IOException {
        int pairs = largestSnapshotTheClientAccepts();
        System.out.printf("Snapshot limit: %d pairs of 2 machines with 20 processing recipes,"
                + " %d counted bytes%n", pairs, countedBytes(snapshot(pairs, 20)));
        assertTrue("each machine still sends its own name and position, so the limit grows by"
                + " half at least, not by twice", pairs > PAIRS_WITHOUT_DEDUPLICATION * 3 / 2);
    }

    @Test
    public void theServerSendsANoticeInsteadOfASnapshotPastTheCap() throws IOException {
        int pairs = 0;
        while (countedBytes(snapshot(pairs + 1, 20)) <= PacketProviderList.CAP) {
            pairs++;
        }
        System.out.printf("Server cap: %d pairs of 2 machines with 20 processing recipes%n", pairs);

        assertTrue(PacketProviderList.CAP < CLIENT_LIMIT);
        assertTrue(PacketProviderList.capped(snapshot(pairs, 20)).payload.hasKey("Providers"));
        NBTTagCompound tooBig = snapshot(pairs + 1, 20);
        PacketProviderList received = overTheWire(PacketProviderList.capped(tooBig));
        assertFalse(received.payload.hasKey("Providers"));
        assertEquals(countedBytes(tooBig), received.payload.getLong("TooBig"));
        assertTrue(received.payload.getBoolean("MachinesTab"));
    }

    @Test
    public void theSnapshotOfThePlayersNetworkFitsWithAMargin() throws IOException {
        // Decision D38: 20 machines and 110 recipes, hence 10 pairs of 11 recipes.
        NBTTagCompound wrapper = snapshot(10, 11);
        long counted = countedBytes(wrapper);
        System.out.printf("Snapshot of the player's network: %d counted bytes, %d%% of the limit%n",
                counted, counted * 100 / CLIENT_LIMIT);
        assertTrue(counted <= CLIENT_LIMIT / 2);
        assertEquals(wrapper, PacketProviderList.capped(wrapper).payload);
    }

    /**
     * The tag {@code ContainerPatTerminal.detectAndSendChanges} sends, with the Machines tab on
     * and no crafter. Each pair carries its own recipes, so no two pairs share a list.
     */
    private static NBTTagCompound snapshot(int pairs, int recipesPerMachine) {
        List<ProviderSnapshot> providers = new ArrayList<>();
        for (int pair = 0; pair < pairs; pair++) {
            for (ProviderRole role : new ProviderRole[] {ProviderRole.PACKAGER, ProviderRole.UNPACKAGER}) {
                ProviderSnapshot machine = new ProviderSnapshot();
                machine.id = providers.size();
                machine.name = role == ProviderRole.PACKAGER ? "Packager" : "Unpackager";
                machine.pos = new BlockPos(-1200 + pair, 64, 3400 + role.ordinal());
                machine.active = true;
                machine.holderPresent = true;
                machine.role = role;
                for (int recipe = 0; recipe < recipesPerMachine; recipe++) {
                    machine.recipes.add(new ProcessingRecipe(pair, recipe));
                }
                providers.add(machine);
            }
        }
        NBTTagCompound wrapper = new NBTTagCompound();
        ProviderSnapshot.writeAll(providers, wrapper);
        wrapper.setTag("Machines", new NBTTagList());
        wrapper.setBoolean("MachinesTab", true);
        return wrapper;
    }

    private static int largestSnapshotTheClientAccepts() throws IOException {
        int pairs = 0;
        while (clientAccepts(snapshot(pairs + 1, 20))) {
            pairs++;
        }
        return pairs;
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

    /** The packet as the client decodes it, limit included. */
    private static PacketProviderList overTheWire(PacketProviderList sent) {
        ByteBuf buf = Unpooled.buffer();
        sent.toBytes(buf);
        PacketProviderList received = new PacketProviderList();
        received.fromBytes(buf);
        return received;
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

        private final int pair;

        ProcessingRecipe(int pair, int recipe) {
            super(pair + "/" + recipe);
            this.pair = pair;
        }

        @Override
        public IRecipeType getRecipeType() {
            return RecipeTypeProcessing.INSTANCE;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setTag("Input", stacks(4, pair));
            tag.setTag("Output", stacks(1, pair));
            return tag;
        }
    }

    /** A processing recipe with no stack, which the client reads back without any item. */
    private static final class EmptyProcessingRecipe extends FakeRecipe {

        EmptyProcessingRecipe() {
            super("empty");
        }

        @Override
        public IRecipeType getRecipeType() {
            return RecipeTypeProcessing.INSTANCE;
        }
    }

    /** Stacks as {@code MiscUtil.saveAllItems} writes them. */
    private static NBTTagList stacks(int count, int damage) {
        NBTTagList list = new NBTTagList();
        for (int index = 0; index < count; index++) {
            NBTTagCompound stack = new NBTTagCompound();
            stack.setByte("Index", (byte) index);
            stack.setString("id", "thermalfoundation:material");
            stack.setByte("Count", (byte) 4);
            stack.setShort("Damage", (short) damage);
            list.appendTag(stack);
        }
        return list;
    }
}
