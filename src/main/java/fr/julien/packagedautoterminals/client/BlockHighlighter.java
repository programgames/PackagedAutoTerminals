package fr.julien.packagedautoterminals.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.PatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Marks blocks in the world, so the player can find them from the terminal.
 *
 * <p>Everything is drawn **without a depth test**: machines almost always sit behind a wall,
 * and a marker you cannot see would be useless.
 *
 * <p>Three layers, from the closest to the most distant reading:
 *
 * <ol>
 *   <li>a tinted cube, which shows the machine even against a wall of the same colour;
 *   <li>a thick outline, which gives the exact block;
 *   <li>a beam going up, which is seen from across the base. Without it the player had to
 *       already look the right way to find the marker.
 * </ol>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class BlockHighlighter {

    /** Length of one beat, in milliseconds. */
    private static final long PERIOD = 900L;
    /** Height of the beam, in blocks. */
    private static final double BEAM_HEIGHT = 64.0;
    /** Half width of the beam. A thin beam is lost against a bright sky. */
    private static final double BEAM_HALF = 0.18;
    /** Width of the outline, in pixels. */
    private static final float LINE_WIDTH = 4.0f;

    private static final float RED = 0.15f;
    private static final float GREEN = 0.85f;
    private static final float BLUE = 1.0f;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private BlockHighlighter() {}

    /** Marks these positions, in this dimension, for the configured time. */
    public static void highlight(int dimension, List<BlockPos> positions) {
        long expiry = System.currentTimeMillis() + PatConfig.highlightSeconds * 1000L;
        ENTRIES.clear();
        for (BlockPos pos : positions) {
            ENTRIES.add(new Entry(dimension, pos, expiry));
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null) {
            ENTRIES.clear();
            return;
        }

        long now = System.currentTimeMillis();
        for (Iterator<Entry> iterator = ENTRIES.iterator(); iterator.hasNext(); ) {
            if (iterator.next().expiry <= now) {
                iterator.remove();
            }
        }
        if (ENTRIES.isEmpty()) {
            return;
        }

        int dimension = player.world.provider.getDimension();
        float partial = event.getPartialTicks();
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial;

        // The beat goes from faint to strong, but never vanishes: an outline missing at the
        // wrong moment reads as a failure.
        float beat = 0.55f + 0.45f * (float) Math.abs(Math.sin(Math.PI * (now % PERIOD) / PERIOD));

        GlStateManager.pushMatrix();
        GlStateManager.translate(-x, -y, -z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        // Without this line the beam disappears as soon as the player stands inside it: the
        // game would only keep the faces turned away.
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GL11.glLineWidth(LINE_WIDTH);

        for (Entry entry : ENTRIES) {
            if (entry.dimension != dimension) {
                continue;
            }
            draw(entry.pos, beat);
        }

        GL11.glLineWidth(1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** The three layers of one mark. */
    private static void draw(BlockPos pos, float beat) {
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.02);
        RenderGlobal.renderFilledBox(box, RED, GREEN, BLUE, 0.30f * beat);
        RenderGlobal.drawSelectionBoundingBox(box, RED, GREEN, BLUE, Math.min(1.0f, beat));

        AxisAlignedBB beam = new AxisAlignedBB(
                pos.getX() + 0.5 - BEAM_HALF, pos.getY() + 1.0, pos.getZ() + 0.5 - BEAM_HALF,
                pos.getX() + 0.5 + BEAM_HALF, pos.getY() + BEAM_HEIGHT,
                pos.getZ() + 0.5 + BEAM_HALF);
        RenderGlobal.renderFilledBox(beam, RED, GREEN, BLUE, 0.22f * beat);
    }

    private static final class Entry {
        final int dimension;
        final BlockPos pos;
        final long expiry;

        Entry(int dimension, BlockPos pos, long expiry) {
            this.dimension = dimension;
            this.pos = pos;
            this.expiry = expiry;
        }
    }
}
