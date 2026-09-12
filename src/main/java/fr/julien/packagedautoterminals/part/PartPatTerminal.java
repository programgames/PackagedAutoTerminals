package fr.julien.packagedautoterminals.part;

import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractPartDisplay;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Terminal câblé. Il se pose sur un câble ME, comme l'Interface Terminal d'AE2. */
public class PartPatTerminal extends AbstractPartDisplay {

    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(Reference.MOD_ID, "part/pat_terminal_off");
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(Reference.MOD_ID, "part/pat_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public PartPatTerminal(ItemStack stack) {
        super(stack);
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, EnumHand hand, Vec3d position) {
        if (super.onPartActivate(player, hand, position)) {
            return true;
        }
        if (player.world.isRemote) {
            return true;
        }
        BlockPos pos = getTile().getPos();
        PackagedAutoTerminals.LOGGER.info("Ouverture du terminal en {} face {}", pos, getSide());
        player.openGui(PackagedAutoTerminals.instance, getSide().ordinal(), player.world,
                pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
