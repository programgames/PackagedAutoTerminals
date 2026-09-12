package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.TerminalContext;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFake;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * Éditeur d'une recette, ouvert depuis le terminal.
 *
 * <p>Les emplacements sont des copies fantômes : cliquer avec un objet en main y dépose son
 * image, sans consommer l'objet. Ce sont les classes de PackagedAuto, {@link SlotFalseCopy}
 * et {@link SlotPreview}, donc le comportement est identique à celui de l'Encoder.
 *
 * <p>La recette est construite **par le serveur** (décision D05), à chaque changement
 * d'emplacement. Le client ne fait qu'afficher le résultat.
 */
public class ContainerPatEditor extends AEBaseContainer {

    // Géométrie de la fenêtre. Ces valeurs doivent rester identiques à celles de
    // tools/make_gui_texture.py. La disposition reprend celle du Package Recipe Encoder.
    public static final int WIDTH = 252;
    public static final int HEIGHT = 282;
    /** Coin haut-gauche de la grille des entrées, 9 sur 9. */
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 20;
    /** Coin haut-gauche des sorties, 3 sur 3. */
    public static final int OUTPUT_LEFT = 190;
    public static final int OUTPUT_TOP = 80;
    /** Coin haut-gauche de l'aperçu des colis, 3 sur 3. */
    public static final int PREVIEW_LEFT = 190;
    public static final int PREVIEW_TOP = 140;
    public static final int PLAYER_INVENTORY_TOP = 200;
    /** Quantité maximale d'un emplacement de recette. */
    public static final int MAX_SLOT_COUNT = 4096;

    private final TerminalContext terminal;
    public final EditorInventory editor;

    /** Machine visée, et rang de la recette. Un rang négatif signifie « nouvelle recette ». */
    public final int dimension;
    public final BlockPos pos;
    public final int index;

    public ContainerPatEditor(InventoryPlayer inventory, TerminalContext terminal,
                              EditorInventory editor, int dimension, BlockPos pos, int index) {
        super(inventory, terminal.host());
        this.terminal = terminal;
        this.editor = editor;
        this.dimension = dimension;
        this.pos = pos;
        this.index = index;

        bindEditorSlots();
        bindPlayerInventory(inventory, 0, PLAYER_INVENTORY_TOP);
    }

    /**
     * Pose les 99 emplacements, toujours dans le même ordre et de la même classe des deux
     * côtés.
     *
     * <p>PIÈGE évité ici : si la classe d'un emplacement dépendait du type de recette, le
     * client et le serveur pourraient en poser de différentes, car le client ignore le type
     * au moment de construire la fenêtre. La synchronisation des emplacements se ferait
     * alors de travers. Le droit d'écrire est donc vérifié ailleurs : par
     * {@code EditorInventory.isItemValidForSlot}, côté serveur, et par le grisage dans la
     * fenêtre.
     */
    private void bindEditorSlots() {
        IItemHandler handler = new InvWrapper(editor);
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new SlotFake(handler, row * 9 + column,
                        GRID_LEFT + column * 18, GRID_TOP + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlotToContainer(new SlotFake(handler,
                        EditorInventory.INPUT_SLOTS + row * 3 + column,
                        OUTPUT_LEFT + column * 18, OUTPUT_TOP + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlotToContainer(new SlotResult(handler,
                        EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS + row * 3 + column,
                        PREVIEW_LEFT + column * 18, PREVIEW_TOP + row * 18));
            }
        }
    }

    /** Emplacement d'aperçu : il montre le résultat calculé, et refuse toute manipulation. */
    private static final class SlotResult extends AppEngSlot {
        SlotResult(IItemHandler inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            setNotDraggable();
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }
    }

    /**
     * Comportement des emplacements fantômes.
     *
     * <p>Le conteneur, et non l'emplacement, porte ce comportement. C'est aussi le choix de
     * PackagedAuto, dans {@code ContainerTileBase.slotClick}.
     *
     * <p>PIÈGE : {@code AEBaseContainer.addSlotToContainer} refuse tout emplacement qui
     * n'hérite pas d'{@code AppEngSlot}. Les emplacements de PackagedAuto sont donc
     * inutilisables ici. AE2 fournit les siens, dont {@link SlotFake}, qui implémente en
     * prime {@code IJEITargetSlot} : le glisser-déposer depuis JEI arrivera sans travail
     * supplémentaire.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = inventorySlots.get(slotId);
            if (slot instanceof SlotResult) {
                return ItemStack.EMPTY;
            }
            if (slot instanceof SlotFake) {
                if (!editor.isEditable(slot.getSlotIndex())) {
                    return ItemStack.EMPTY;
                }
                if (clickType == ClickType.PICKUP && dragType == 1) {
                    slot.putStack(ItemStack.EMPTY);
                } else if (clickType == ClickType.QUICK_MOVE) {
                    slot.putStack(ItemStack.EMPTY);
                } else {
                    ItemStack held = player.inventory.getItemStack();
                    slot.putStack(held.isEmpty() ? ItemStack.EMPTY : held.copy());
                }
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    /**
     * Type de recette courant, synchronisé par AE2.
     *
     * <p>PIÈGE : {@code updateProgressBar} est `final` dans {@code AEBaseContainer}. Le
     * mécanisme vanilla est donc inutilisable. AE2 fournit le sien : tout champ annoté
     * {@link GuiSync} part vers le client, et {@link #onUpdate} le signale à l'arrivée.
     */
    @GuiSync(0)
    public int recipeTypeId = -1;

    @Override
    public void detectAndSendChanges() {
        // Hors de portée, l'éditeur se referme comme le terminal : il écrit sur le même
        // réseau, et n'a plus le droit d'y toucher.
        if (!terminal.stillValid()) {
            setValidContainer(false);
            return;
        }
        recipeTypeId = editor.recipeType == null ? -1 : RecipeTypeRegistry.getId(editor.recipeType);
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if ("recipeTypeId".equals(field)) {
            int id = (Integer) newValue;
            editor.recipeType = id < 0 ? null : RecipeTypeRegistry.getRecipeType(id);
            // Le client doit recalculer son aperçu : un changement de type modifie la
            // recette sans qu'aucun emplacement ne bouge.
            editor.updateRecipeInfo();
        }
        super.onUpdate(field, oldValue, newValue);
    }

    /**
     * Remplit l'éditeur depuis une recette de JEI.
     *
     * <p>Le serveur ne fait pas confiance à la correspondance reçue : il impose le type,
     * puis n'écrit que dans les emplacements que ce type active.
     */
    public void fillFromRecipe(int typeId, Int2ObjectMap<ItemStack> transfer) {
        IRecipeType type = RecipeTypeRegistry.getRecipeType(typeId);
        if (type == null) {
            return;
        }
        editor.clear();
        editor.recipeType = type;

        for (Int2ObjectMap.Entry<ItemStack> entry : transfer.int2ObjectEntrySet()) {
            int slot = entry.getIntKey();
            if (slot < 0 || slot >= EditorInventory.SIZE || !editor.isEditable(slot)) {
                continue;
            }
            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                editor.setInventorySlotContents(slot, stack.copy());
            }
        }
        editor.updateRecipeInfo();
        detectAndSendChanges();
    }

    /**
     * Ajuste la quantité d'un emplacement.
     *
     * <p>La limite haute n'est pas 64 : PackagedAuto sait écrire de grandes quantités, par
     * {@code MiscUtil.saveItemWithLargeCount}. Les recettes de traitement en ont besoin.
     */
    public void changeSlotCount(int slot, int delta) {
        if (!editor.isEditable(slot)) {
            return;
        }
        ItemStack stack = editor.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        int count = Math.max(1, Math.min(MAX_SLOT_COUNT, stack.getCount() + delta));
        ItemStack changed = stack.copy();
        changed.setCount(count);
        editor.setInventorySlotContents(slot, changed);
        detectAndSendChanges();
    }

    /**
     * Écrit la recette courante dans le porte-recettes de la machine.
     *
     * @return vrai si l'écriture a eu lieu.
     */
    public boolean save() {
        if (editor.recipeInfo == null) {
            return false;
        }
        IPackageProvidingMachine machine =
                ProviderScanner.find(terminal.grid(), dimension, pos);
        if (machine == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return false;
        }

        ItemStack holder = machine.getPatternStack();
        if (holder.isEmpty() || !(holder.getItem() instanceof IRecipeListItem)) {
            return false;
        }
        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return false;
        }

        List<IRecipeInfo> recipes = new ArrayList<>(recipeList.getRecipeList());
        if (index >= 0 && index < recipes.size()) {
            recipes.set(index, editor.recipeInfo);
        } else {
            recipes.add(editor.recipeInfo);
        }

        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        // Seul cet appel prévient AE2. Voir docs/PACKAGEDAUTO-MODEL.md, section 7.2.
        machine.setPatternStack(holder);
        return true;
    }

    /** Referme l'éditeur et rouvre le terminal, à la même part. */
    public void backToTerminal() {
        EntityPlayer player = getPlayerInv().player;
        terminal.openTerminal(player);
    }

    /**
     * Passe au type de recette suivant ou précédent, puis reconstruit la recette.
     *
     * <p>Le cas du type nul est traité ici : sur une recette neuve, l'éditeur peut s'ouvrir
     * sans type, et {@code getNextRecipeType} n'a alors aucun point de départ.
     */
    public void cycleRecipeType(boolean forward) {
        if (editor.recipeType == null) {
            editor.recipeType = defaultRecipeType();
        } else {
            editor.recipeType = RecipeTypeRegistry.getNextRecipeType(editor.recipeType, forward);
        }
        editor.updateRecipeInfo();
    }

    /**
     * Type proposé à l'ouverture d'une recette neuve.
     *
     * <p>Le craft de base vient en premier s'il existe : c'est le cas le plus courant. Le
     * Package Crafter peut être désactivé en configuration, auquel cas ce type n'est pas
     * enregistré, et le premier type disponible fait l'affaire.
     */
    public static IRecipeType defaultRecipeType() {
        NavigableMap<ResourceLocation, IRecipeType> registry = RecipeTypeRegistry.getRegistry();
        if (registry.isEmpty()) {
            return null;
        }
        IRecipeType crafting = registry.get(new ResourceLocation("packagedauto", "crafting"));
        return crafting != null ? crafting : registry.firstEntry().getValue();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
