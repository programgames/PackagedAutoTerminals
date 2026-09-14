package fr.julien.packagedautoterminals.client;

import java.util.Collections;

import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Gives an icon to the universal terminal when it is set to our mode.
 *
 * <h2>The defect</h2>
 *
 * <p>The AE2WUT 1.0.5 model is an {@code item/generated} with ten overrides, from
 * {@code mode: 1} to {@code mode: 114514}. A Minecraft override applies as soon as the value
 * is **greater than or equal to** the threshold, and the game keeps the **last** one that
 * matches. Every mode above 9 therefore lands on the mode 9 override, which points at
 * {@code ae2exttable:item/wireless_ultimate_crafting_terminal}. Without that addon, the game
 * shows the purple and black checkerboard of a missing model.
 *
 * <p>The defect does not come from our mode 41: Cell Terminal, with its mode 11, hits it too.
 * Recent AE2WUT versions have replaced those overrides with a baked model and a
 * {@code regIcon} method. Version 1.0.5, the one in the modpack, does not have it.
 *
 * <h2>The fix</h2>
 *
 * <p>We wrap the baked AE2WUT model. The wrapper changes nothing, except one thing: on our
 * mode it returns the model of **our** wireless terminal. Any other mode falls back to the
 * original override list, untouched.
 *
 * <p>Nothing is written into the AE2WUT resources. Without AE2WUT, the model we look for is
 * absent and the wrapper is never installed.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
public final class WutModelPatch {

    private static final ModelResourceLocation UNIVERSAL = new ModelResourceLocation(
            Reference.AE2WUT + ":wireless_universal_terminal", "inventory");
    private static final ModelResourceLocation OURS = new ModelResourceLocation(
            Reference.MOD_ID + ":wireless_pat_terminal", "inventory");

    private WutModelPatch() {}

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        IBakedModel universal = event.getModelRegistry().getObject(UNIVERSAL);
        IBakedModel ours = event.getModelRegistry().getObject(OURS);
        if (universal == null || ours == null) {
            return;
        }
        event.getModelRegistry().putObject(UNIVERSAL, new ModeAware(universal, ours));
    }

    /** The AE2WUT model, unchanged, except for its override list. */
    private static final class ModeAware extends BakedModelWrapper<IBakedModel> {

        private final ItemOverrideList overrides;

        ModeAware(IBakedModel universal, IBakedModel ours) {
            super(universal);
            this.overrides = new ModeOverrides(universal, ours);
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }
    }

    /** Our mode first; everything else goes to the original list. */
    private static final class ModeOverrides extends ItemOverrideList {

        private final IBakedModel universal;
        private final IBakedModel ours;

        ModeOverrides(IBakedModel universal, IBakedModel ours) {
            super(Collections.emptyList());
            this.universal = universal;
            this.ours = ours;
        }

        @Override
        public IBakedModel handleItemState(IBakedModel model, ItemStack stack, World world,
                                           EntityLivingBase entity) {
            if (WutSupport.showsOurMode(stack)) {
                return ours;
            }
            // PITFALL: pass `universal`, not `model`. `model` is our wrapper: handing it
            // back would make the override lookup loop on itself.
            return universal.getOverrides().handleItemState(universal, stack, world, entity);
        }
    }
}
