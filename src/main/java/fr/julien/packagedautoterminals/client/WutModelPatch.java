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
 * Donne une image au terminal universel réglé sur notre mode.
 *
 * <h2>Le défaut</h2>
 *
 * <p>Le modèle d'AE2WUT 1.0.5 est un {@code item/generated} muni de dix surcharges, de
 * {@code mode: 1} à {@code mode: 114514}. Or une surcharge de Minecraft s'applique dès que
 * la valeur est **supérieure ou égale** au seuil, et le jeu retient la **dernière** qui
 * correspond. Tout mode au-delà de 9 tombe donc sur la surcharge du mode 9, qui désigne
 * {@code ae2exttable:item/wireless_ultimate_crafting_terminal}. Sans cet addon, le jeu
 * affiche le damier violet et noir du modèle manquant.
 *
 * <p>Le défaut ne vient pas de notre mode 41 : Cell Terminal, avec son mode 11, le subit
 * aussi. Les versions récentes d'AE2WUT ont d'ailleurs remplacé ces surcharges par un modèle
 * cuit et une méthode {@code regIcon}. La 1.0.5, celle du modpack, ne l'a pas.
 *
 * <h2>Le correctif</h2>
 *
 * <p>On enveloppe le modèle cuit d'AE2WUT. L'enveloppe ne change rien, sauf une chose : sur
 * notre mode, elle rend le modèle de **notre** terminal sans fil. Tout autre mode retourne
 * à la liste de surcharges d'origine, intacte.
 *
 * <p>Rien n'est écrit dans les ressources d'AE2WUT. Sans AE2WUT, le modèle cherché est
 * absent et l'enveloppe ne se pose pas.
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

    /** Le modèle d'AE2WUT, inchangé, sauf sa liste de surcharges. */
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

    /** Notre mode d'abord ; tout le reste part à la liste d'origine. */
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
            // PIÈGE : il faut passer `universal`, et non `model`. `model` est notre
            // enveloppe : la lui donner ferait boucler la recherche de surcharge sur
            // elle-même.
            return universal.getOverrides().handleItemState(universal, stack, world, entity);
        }
    }
}
