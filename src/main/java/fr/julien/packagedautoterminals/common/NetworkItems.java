package fr.julien.packagedautoterminals.common;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thelm.packagedauto.api.IRecipeListItem;

/** Access to the ME network storage, to take a blank recipe holder. */
public final class NetworkItems {

    private static Item recipeHolder;
    private static boolean searched;

    private NetworkItems() {}

    /**
     * Finds the recipe holder item.
     *
     * <p>The search walks the registry and keeps the first item that implements
     * {@link IRecipeListItem}. No name is hard coded: an addon shipping its own recipe
     * holder would therefore be accepted.
     */
    public static Item findRecipeHolder() {
        // PITFALL avoided: never cache a null result. A call made too early, before item
        // registration, would freeze the absence for the whole game.
        if (searched && recipeHolder != null) {
            return recipeHolder;
        }
        searched = true;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof IRecipeListItem) {
                recipeHolder = item;
                break;
            }
        }
        return recipeHolder;
    }

    /**
     * Stores an item into the network.
     *
     * @return what could not be stored.
     */
    public static ItemStack insert(IGrid grid, ItemStack stack, IActionSource source) {
        if (grid == null || stack.isEmpty()) {
            return stack;
        }
        IStorageGrid storage = grid.getCache(IStorageGrid.class);
        IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        if (storage == null || energy == null) {
            return stack;
        }

        IStorageChannel<IAEItemStack> channel =
                AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IMEMonitor<IAEItemStack> inventory = storage.getInventory(channel);
        IAEItemStack request = inventory == null ? null : channel.createStack(stack);
        if (request == null) {
            return stack;
        }

        IAEItemStack remainder = AEApi.instance().storage()
                .poweredInsert(energy, inventory, request, source, Actionable.MODULATE);
        return remainder == null ? ItemStack.EMPTY : remainder.createItemStack();
    }

    /**
     * Takes one copy of this item out of the network.
     *
     * <p>Extraction goes through {@code poweredExtraction}: it therefore draws network
     * energy, and honours the action source, hence the AE2 security.
     *
     * @return the extracted stack, or an empty stack when the network has none.
     */
    public static ItemStack extractOne(IGrid grid, ItemStack prototype, IActionSource source) {
        if (grid == null || prototype.isEmpty()) {
            return ItemStack.EMPTY;
        }
        IStorageGrid storage = grid.getCache(IStorageGrid.class);
        IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        if (storage == null || energy == null) {
            return ItemStack.EMPTY;
        }

        IStorageChannel<IAEItemStack> channel =
                AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IMEMonitor<IAEItemStack> inventory = storage.getInventory(channel);
        if (inventory == null) {
            return ItemStack.EMPTY;
        }

        IAEItemStack request = channel.createStack(prototype);
        if (request == null) {
            return ItemStack.EMPTY;
        }
        request.setStackSize(1);

        IAEItemStack extracted = AEApi.instance().storage()
                .poweredExtraction(energy, inventory, request, source, Actionable.MODULATE);
        return extracted == null ? ItemStack.EMPTY : extracted.createItemStack();
    }
}
