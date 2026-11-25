package fr.julien.packagedautoterminals.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.GroupNames;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.junit.Test;

/**
 * The wire rules of the mod, tested without the game.
 *
 * <p>Single player links both sides without serialising anything, so a broken rule here
 * only shows on a dedicated server, or under a crafted packet.
 */
public class NetworkProtocolTest {

    @Test
    public void theChannelNameFitsTheTwentyCharactersOfCustomPayload() {
        // CPacketCustomPayload reads the channel with readString(20). Decision D37.
        assertTrue(Reference.CHANNEL.length() <= 20);
    }

    @Test
    public void aRecipeActionSurvivesTheWire() {
        ByteBuf buf = Unpooled.buffer();
        new PacketRecipeAction(-1, new BlockPos(-12, 64, 30000), -4,
                PacketRecipeAction.ACTION_SCALE_ALL).toBytes(buf);

        PacketRecipeAction read = new PacketRecipeAction();
        read.fromBytes(buf);

        assertEquals(-1, read.dimension);
        assertEquals(new BlockPos(-12, 64, 30000), read.pos);
        assertEquals(-4, read.index);
        assertEquals(PacketRecipeAction.ACTION_SCALE_ALL, read.action);
        assertFalse(buf.isReadable());
    }

    @Test
    public void theRecipeNamedByAnActionSurvivesTheWire() {
        NBTTagCompound recipe = new NBTTagCompound();
        recipe.setString("RecipeType", "packagedauto:crafting");
        ByteBuf buf = Unpooled.buffer();
        new PacketRecipeAction(0, BlockPos.ORIGIN, 3, PacketRecipeAction.ACTION_REMOVE, recipe)
                .toBytes(buf);

        PacketRecipeAction read = new PacketRecipeAction();
        read.fromBytes(buf);

        assertEquals(3, read.index);
        assertEquals(recipe, read.recipe);
        assertFalse(buf.isReadable());
    }

    @Test
    public void anEditorSlotChangeSurvivesTheWire() {
        ByteBuf buf = Unpooled.buffer();
        new PacketEditorSlot(7, -64, true, true).toBytes(buf);

        PacketEditorSlot read = new PacketEditorSlot();
        read.fromBytes(buf);

        assertEquals(7, read.slot);
        assertEquals(-64, read.delta);
        assertTrue(read.absolute);
        assertTrue(read.scale);
        assertFalse(buf.isReadable());
    }

    @Test
    public void aRenameAnnouncingMoreBytesThanItHoldsReadsWhatIsThere() {
        ByteBuf buf = Unpooled.buffer();
        // The largest length the two-byte varint of fromBytes can announce.
        ByteBufUtils.writeVarInt(buf, 16383, 2);
        for (int i = 0; i < 40; i++) {
            buf.writeByte('a');
        }

        PacketRenameGroup read = new PacketRenameGroup();
        read.fromBytes(buf);

        assertEquals(GroupNames.MAX_LENGTH, read.name.length());
        assertFalse(buf.isReadable());
    }

    @Test
    public void aFillAnnouncingTwoBillionEntriesStopsAtTheEditorSize() {
        // Each zero byte reads as an empty tag, so the loop creates no item stack. Without
        // the clamp it would walk the whole buffer.
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(0);
        buf.writeInt(Integer.MAX_VALUE);
        buf.writeZero(1000);

        PacketEditorFill read = new PacketEditorFill();
        read.fromBytes(buf);

        assertTrue(read.stacks.isEmpty());
        assertEquals(1000 - PacketEditorFill.maxSlots(), buf.readableBytes());
    }
}
