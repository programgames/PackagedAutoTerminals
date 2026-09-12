package fr.julien.packagedautoterminals.item;

import appeng.api.parts.IPartItem;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** L'item qui pose la part sur un câble. AE2 appelle {@link #createPartFromItemStack}. */
public class ItemPatTerminal extends Item implements IPartItem<PartPatTerminal> {

    public ItemPatTerminal() {
        setMaxStackSize(64);
        setHasSubtypes(false);
    }

    @Override
    public PartPatTerminal createPartFromItemStack(ItemStack stack) {
        return new PartPatTerminal(stack);
    }
}
