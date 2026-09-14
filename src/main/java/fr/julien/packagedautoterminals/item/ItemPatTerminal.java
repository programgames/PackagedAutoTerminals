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
 * The item that places the part on a cable.
 *
 * <p>Two halves are needed, and forgetting the second one makes the item inert, with no
 * error at all:
 *
 * <ol>
 *   <li>{@link IPartItem#createPartFromItemStack} tells AE2 which part to build;
 *   <li>{@link #onItemUse} triggers the placement. AE2 does not intercept the right click
 *       for items from other mods. Without this redirection to {@link PartPlacement}, right
 *       clicking an ME cable does nothing.
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
