package fr.julien.packagedautoterminals.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.julien.packagedautoterminals.Reference;
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
 * Fait clignoter des blocs dans le monde, pour les retrouver depuis le terminal.
 *
 * <p>Le contour se dessine **sans test de profondeur** : les machines sont presque toujours
 * derrière un mur, et un repérage invisible ne servirait à rien.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class BlockHighlighter {

    /** Durée du clignotement, en millisecondes. */
    private static final long DURATION = 5_000L;
    /** Période d'un battement, en millisecondes. */
    private static final long PERIOD = 600L;

    private static final float RED = 0.15f;
    private static final float GREEN = 0.85f;
    private static final float BLUE = 1.0f;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private BlockHighlighter() {}

    /** Marque ces positions, dans cette dimension, pour les cinq prochaines secondes. */
    public static void highlight(int dimension, List<BlockPos> positions) {
        long expiry = System.currentTimeMillis() + DURATION;
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

        // Le battement va de discret à franc, sans jamais disparaître : un contour absent
        // au mauvais moment se lit comme une panne.
        float beat = 0.35f + 0.45f * (float) Math.abs(Math.sin(Math.PI * (now % PERIOD) / PERIOD));

        GlStateManager.pushMatrix();
        GlStateManager.translate(-x, -y, -z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GL11.glLineWidth(3.0f);

        for (Entry entry : ENTRIES) {
            if (entry.dimension != dimension) {
                continue;
            }
            AxisAlignedBB box = new AxisAlignedBB(entry.pos).grow(0.01);
            RenderGlobal.drawSelectionBoundingBox(box, RED, GREEN, BLUE, beat);
        }

        GL11.glLineWidth(1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
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
