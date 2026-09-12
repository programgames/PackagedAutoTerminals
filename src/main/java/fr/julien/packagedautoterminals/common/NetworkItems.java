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

/** Accès au stockage du réseau ME, pour prendre un porte-recettes vierge. */
public final class NetworkItems {

    private static Item recipeHolder;
    private static boolean searched;

    private NetworkItems() {}

    /**
     * Trouve l'item porte-recettes.
     *
     * <p>La recherche parcourt le registre et retient le premier item qui implémente
     * {@link IRecipeListItem}. Aucun nom n'est écrit en dur : un addon qui fournirait son
     * propre porte-recettes serait donc accepté.
     */
    public static Item findRecipeHolder() {
        // PIÈGE évité : ne jamais mémoriser un résultat nul. Un appel trop tôt, avant
        // l'enregistrement des items, figerait l'absence pour toute la partie.
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
     * Range un objet dans le réseau.
     *
     * @return ce qui n'a pas pu être rangé.
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
     * Retire un exemplaire de cet objet du réseau.
     *
     * <p>L'extraction passe par {@code poweredExtraction} : elle consomme donc l'énergie du
     * réseau, et respecte la source d'action, donc la sécurité d'AE2.
     *
     * @return la pile extraite, ou une pile vide si le réseau n'en a pas.
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
