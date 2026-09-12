package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;
import thelm.packagedauto.api.RecipeTypeRegistry;
import thelm.packagedauto.slot.SlotFalseCopy;
import thelm.packagedauto.slot.SlotPreview;

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

    public static final int WIDTH = 236;
    public static final int HEIGHT = 276;
    /** Coin haut-gauche de la grille des entrées. */
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 20;
    /** Coin haut-gauche des sorties. */
    public static final int OUTPUT_LEFT = 178;
    public static final int OUTPUT_TOP = 20;
    /** Coin haut-gauche de l'aperçu du résultat. */
    public static final int PREVIEW_LEFT = 178;
    public static final int PREVIEW_TOP = 80;
    public static final int PLAYER_INVENTORY_TOP = 194;

    private final PartPatTerminal terminal;
    public final EditorInventory editor;

    /** Machine visée, et rang de la recette. Un rang négatif signifie « nouvelle recette ». */
    public final int dimension;
    public final BlockPos pos;
    public final int index;

    public ContainerPatEditor(InventoryPlayer inventory, PartPatTerminal terminal,
                              EditorInventory editor, int dimension, BlockPos pos, int index) {
        super(inventory, terminal);
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
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new SlotFalseCopy(editor, row * 9 + column,
                        GRID_LEFT + column * 18, GRID_TOP + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlotToContainer(new SlotFalseCopy(editor,
                        EditorInventory.INPUT_SLOTS + row * 3 + column,
                        OUTPUT_LEFT + column * 18, OUTPUT_TOP + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlotToContainer(new SlotPreview(editor,
                        EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS + row * 3 + column,
                        PREVIEW_LEFT + column * 18, PREVIEW_TOP + row * 18));
            }
        }
    }

    /**
     * Comportement des emplacements fantômes.
     *
     * <p>{@link SlotFalseCopy} ne suffit pas : chez PackagedAuto, c'est le conteneur qui
     * intercepte le clic, dans {@code ContainerTileBase.slotClick}. Sans cette redirection,
     * un clic sur la grille déplacerait de vrais objets.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = inventorySlots.get(slotId);
            if (slot instanceof SlotPreview) {
                return ItemStack.EMPTY;
            }
            if (slot instanceof SlotFalseCopy) {
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
        recipeTypeId = editor.recipeType == null ? -1 : RecipeTypeRegistry.getId(editor.recipeType);
        super.detectAndSendChanges();
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        if ("recipeTypeId".equals(field)) {
            int id = (Integer) newValue;
            editor.recipeType = id < 0 ? null : RecipeTypeRegistry.getRecipeType(id);
        }
        super.onUpdate(field, oldValue, newValue);
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
        IGridNode node = terminal.getGridNode();
        IPackageProvidingMachine machine =
                ProviderScanner.find(node == null ? null : node.getGrid(), dimension, pos);
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
        BlockPos host = terminal.getTile().getPos();
        player.openGui(PackagedAutoTerminals.instance,
                PatGuiHandler.TERMINAL + terminal.getSide().ordinal(), player.world,
                host.getX(), host.getY(), host.getZ());
    }

    /** Passe au type de recette suivant ou précédent, puis reconstruit la recette. */
    public void cycleRecipeType(boolean forward) {
        editor.recipeType = RecipeTypeRegistry.getNextRecipeType(editor.recipeType, forward);
        editor.updateRecipeInfo();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
