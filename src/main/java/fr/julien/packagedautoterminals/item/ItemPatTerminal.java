package fr.julien.packagedautoterminals.item;

import appeng.api.parts.IPartItem;
import appeng.parts.PartPlacement;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * L'item qui pose la part sur un câble.
 *
 * <p>Deux moitiés sont nécessaires, et l'oubli de la seconde rend l'item inerte, sans
 * aucune erreur :
 *
 * <ol>
 *   <li>{@link IPartItem#createPartFromItemStack} dit à AE2 quelle part construire ;
 *   <li>{@link #onItemUse} déclenche la pose. AE2 n'intercepte pas le clic droit pour les
 *       items d'autres mods. Sans cette redirection vers {@link PartPlacement}, le clic
 *       droit sur un câble ME ne fait rien.
 * </ol>
 */
public class ItemPatTerminal extends Item implements IPartItem<PartPatTerminal> {

    public ItemPatTerminal() {
        setMaxStackSize(64);
        setHasSubtypes(false);
    }

    @Override
    public PartPatTerminal createPartFromItemStack(ItemStack stack) {
        return new PartPatTerminal(stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        return PartPlacement.place(player.getHeldItem(hand), pos, facing, player, hand, world);
    }
}
