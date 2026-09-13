package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.GroupNames;
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderRole;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.common.RecipeWriter;
import fr.julien.packagedautoterminals.common.TerminalContext;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
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
    public static final int WIDTH = 258;
    public static final int HEIGHT = 312;
    /** Champ de nom du groupe, sur la ligne de titre. */
    public static final int NAME_LEFT = 8;
    public static final int NAME_TOP = 4;
    public static final int NAME_WIDTH = 162;
    public static final int NAME_HEIGHT = 14;
    /** Rangée d'onglets : une case par recette du groupe. */
    public static final int TAB_LEFT = 8;
    public static final int TAB_TOP = 32;
    public static final int TAB_COUNT = 10;
    /** Coin haut-gauche de la grille des entrées, 9 sur 9. */
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 52;
    /** Coin haut-gauche des sorties, 3 sur 3. */
    public static final int OUTPUT_LEFT = 190;
    public static final int OUTPUT_TOP = 92;
    /** Coin haut-gauche de l'aperçu des colis, 3 sur 3. */
    public static final int PREVIEW_LEFT = 190;
    public static final int PREVIEW_TOP = 152;
    public static final int PLAYER_INVENTORY_TOP = 230;
    /** Décalage horizontal de l'inventaire. AE2 pose ses cases à 8 + colonne * 18 + décalage. */
    public static final int PLAYER_INVENTORY_OFFSET_X = 12;
    /** Quantité maximale d'un emplacement de recette. */
    public static final int MAX_SLOT_COUNT = 4096;

    private final TerminalContext terminal;
    public final EditorInventory editor;

    /** Machine visée, et rang de la recette. Un rang négatif signifie « nouvelle recette ». */
    public final int dimension;
    public final BlockPos pos;
    /**
     * Rang de la recette dans le groupe. Négatif tant qu'elle n'existe pas.
     *
     * <p>Il n'est pas final : après l'enregistrement d'une recette neuve, l'éditeur bascule
     * sur la recette créée. Sans cela, un second appui sur Enregistrer en ajouterait une
     * copie.
     */
    private int index;

    public ContainerPatEditor(InventoryPlayer inventory, TerminalContext terminal,
                              EditorInventory editor, int dimension, BlockPos pos, int index) {
        super(inventory, terminal.host());
        this.terminal = terminal;
        this.editor = editor;
        this.dimension = dimension;
        this.pos = pos;
        this.index = index;

        bindEditorSlots();
        bindPlayerInventory(inventory, PLAYER_INVENTORY_OFFSET_X, PLAYER_INVENTORY_TOP);
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
    /**
     * Icônes des onglets : la sortie de chaque recette du groupe.
     *
     * <p>Ce sont de **vrais** emplacements. La synchronisation des objets vers le client est
     * donc prise en charge par le conteneur vanilla, sans paquet de notre part.
     */
    public final net.minecraft.inventory.InventoryBasic tabs =
            new net.minecraft.inventory.InventoryBasic("tabs", false, TAB_COUNT);

    private void bindEditorSlots() {
        IItemHandler handler = new InvWrapper(editor);

        IItemHandler tabHandler = new InvWrapper(tabs);
        for (int tab = 0; tab < TAB_COUNT; tab++) {
            addSlotToContainer(new SlotTab(tabHandler, tab,
                    TAB_LEFT + tab * 18, TAB_TOP));
        }

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

    /** Onglet : il montre la sortie d'une recette, et ne se manipule pas comme un objet. */
    private static final class SlotTab extends AppEngSlot {
        SlotTab(IItemHandler inventory, int index, int x, int y) {
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
            if (slot instanceof SlotTab) {
                selectTab(slot.getSlotIndex());
                return ItemStack.EMPTY;
            }
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
                dirty = true;
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

        World world = getPlayerInv().player.world;
        if (!world.isRemote) {
            // Le nom est relu à chaque cycle : une autre fenêtre a pu le changer.
            groupName = GroupNames.get(world).get(pos);
            refreshTabs();
        }
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
        dirty = true;
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
        dirty = true;
        detectAndSendChanges();
    }

    /**
     * Écrit la recette dans **toutes les machines du groupe**.
     *
     * <p>PackagedAuto exige la même recette dans le Packager et dans l'Unpackager. Écrire
     * d'un seul côté casserait l'automatisation en silence.
     *
     * <p>Cas d'un groupe incomplet, fréquent : le joueur vient de poser une paire et encode
     * sa première recette, si bien que les deux porte-recettes ne partagent encore rien. Si
     * le réseau ne compte qu'une seule machine du rôle manquant, elle rejoint la cible. S'il
     * y en a plusieurs, le terminal n'invente rien et le dit.
     *
     * @return vrai si au moins une machine a été modifiée.
     */
    public boolean save() {
        if (editor.recipeInfo == null) {
            return false;
        }
        IGrid grid = terminal.grid();
        if (grid == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return false;
        }

        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        if (group == null) {
            return false;
        }

        IRecipeInfo oldRecipe = index >= 0 && index < group.recipes.size()
                ? group.recipes.get(index)
                : null;

        List<ProviderSnapshot> targets = new ArrayList<>(group.machines);
        ProviderRole missing = ProviderPairing.missingRoleOf(group);
        if (oldRecipe == null && missing != null) {
            ProviderSnapshot partner = ProviderPairing.findLonePartner(all, group, missing);
            if (partner != null) {
                targets.add(partner);
            }
        }

        RecipeWriter.Result result =
                RecipeWriter.apply(grid, getActionSource(), targets, oldRecipe, editor.recipeInfo);
        if (result.changed == 0) {
            tell("gui.packagedautoterminals.write_failed");
            return false;
        }

        // Le joueur reçoit toujours un retour. Le silence laissait croire à un échec.
        tell(result.changed == 1
                        ? "gui.packagedautoterminals.applied_to_one"
                        : "gui.packagedautoterminals.applied_to",
                result.changed);
        if (result.withoutHolder > 0) {
            tell("gui.packagedautoterminals.no_blank_holder");
        } else if (result.changed == 1 && missing != null) {
            tell("gui.packagedautoterminals.no_partner");
        }

        // L'éditeur suit la recette qu'il vient d'écrire : le prochain enregistrement la
        // modifiera, au lieu d'en créer une copie.
        ProviderPairing.Group after = currentGroup();
        if (after != null) {
            for (int i = 0; i < after.recipes.size(); i++) {
                if (after.recipes.get(i).equals(editor.recipeInfo)) {
                    index = i;
                    currentTab = i;
                    break;
                }
            }
        }
        dirty = false;
        pendingTab = Integer.MIN_VALUE;
        return true;
    }

    /**
     * Écrit le nom du groupe dans **toutes** ses machines.
     *
     * <p>Le nom est rangé par machine, car un groupe se recompose à chaque scan. Voir
     * {@link GroupNames}.
     */
    public void renameGroup(String name) {
        IGrid grid = terminal.grid();
        if (grid == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        if (group == null) {
            return;
        }

        World world = getPlayerInv().player.world;
        for (ProviderSnapshot machine : group.machines) {
            World target = world.provider.getDimension() == machine.dimension
                    ? world
                    : net.minecraftforge.common.DimensionManager.getWorld(machine.dimension);
            if (target != null) {
                GroupNames.get(target).set(machine.pos, name);
            }
        }
        groupName = name == null ? "" : name;
        tell("gui.packagedautoterminals.renamed");
    }

    /**
     * Nom courant du groupe.
     *
     * <p>{@code SyncData} d'AE2 sait transmettre une chaîne : le champ part donc vers le
     * client sans paquet supplémentaire.
     */
    @GuiSync(1)
    public String groupName = "";

    /** Rang de la recette affichée dans la rangée d'onglets. Négatif : l'onglet de création. */
    @GuiSync(2)
    public int currentTab = -1;
    /** Nombre de recettes du groupe. Sert au client pour placer l'onglet de création. */
    @GuiSync(3)
    public int recipeCount;
    /** Première recette montrée dans la rangée. Les flèches la déplacent. */
    @GuiSync(4)
    public int tabOffset;
    /** Message à montrer, clé et paramètres assemblés. */
    @GuiSync(10)
    public String feedback = "";
    /** Compteur de messages. Il change même quand le texte se répète. */
    @GuiSync(11)
    public int feedbackCount;

    /** L'éditeur porte-t-il une modification non enregistrée ? */
    private boolean dirty;
    /** Onglet demandé alors qu'un travail non enregistré était en cours. */
    private int pendingTab = Integer.MIN_VALUE;

    /** Message affiché dans la fenêtre, et non dans la barre d'action. */
    private void tell(String key, Object... arguments) {
        feedback = Feedback.pack(key, arguments);
        feedbackCount++;
    }

    /**
     * Bascule sur une autre recette du groupe.
     *
     * <p>Un travail non enregistré n'est jamais perdu sans avertissement : le premier clic
     * prévient, le second bascule.
     */
    public void selectTab(int slot) {
        int target = tabOffset + slot;
        if (target >= recipeCount) {
            target = -1;
        }

        if (dirty && pendingTab != target) {
            pendingTab = target;
            tell("gui.packagedautoterminals.unsaved");
            return;
        }
        pendingTab = Integer.MIN_VALUE;
        load(target);
    }

    /**
     * Remplit la rangée d'onglets avec la sortie de chaque recette du groupe.
     *
     * <p>La dernière case reste vide : c'est l'onglet de création. Les emplacements étant
     * réels, le client reçoit ces objets sans paquet supplémentaire.
     */
    private void refreshTabs() {
        ProviderPairing.Group group = currentGroup();
        List<IRecipeInfo> recipes = group == null ? new ArrayList<>() : group.recipes;
        recipeCount = recipes.size();

        if (tabOffset > Math.max(0, recipeCount + 1 - TAB_COUNT)) {
            tabOffset = Math.max(0, recipeCount + 1 - TAB_COUNT);
        }

        for (int slot = 0; slot < TAB_COUNT; slot++) {
            int recipe = tabOffset + slot;
            ItemStack icon = ItemStack.EMPTY;
            if (recipe < recipes.size()) {
                List<ItemStack> outputs = recipes.get(recipe).getOutputs();
                if (!outputs.isEmpty()) {
                    icon = outputs.get(0).copy();
                }
            }
            if (!ItemStack.areItemStacksEqual(tabs.getStackInSlot(slot), icon)) {
                tabs.setInventorySlotContents(slot, icon);
            }
        }
    }

    /** Déplace la rangée d'onglets, quand le groupe porte plus de recettes qu'elle n'a de cases. */
    public void scrollTabs(boolean forward) {
        int maximum = Math.max(0, recipeCount + 1 - TAB_COUNT);
        tabOffset = Math.max(0, Math.min(maximum, tabOffset + (forward ? 1 : -1)));
    }

    /** Charge la recette de rang donné, ou vide l'éditeur pour une création. */
    private void load(int target) {
        currentTab = target;
        index = target;
        editor.clear();

        if (target < 0) {
            editor.recipeType = defaultRecipeType();
        } else {
            ProviderPairing.Group group = currentGroup();
            if (group != null && target < group.recipes.size()) {
                editor.load(group.recipes.get(target));
            }
        }
        editor.updateRecipeInfo();
        dirty = false;
    }

    /** Supprime la recette en cours, dans toutes les machines du groupe. */
    public void deleteCurrent() {
        if (index < 0) {
            tell("gui.packagedautoterminals.nothing_to_delete");
            return;
        }
        IGrid grid = grid();
        ProviderPairing.Group group = currentGroup();
        if (grid == null || group == null || index >= group.recipes.size()
                || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        RecipeWriter.Result result = RecipeWriter.apply(grid, getActionSource(),
                group.machines, group.recipes.get(index), null);
        tell(result.changed == 1
                        ? "gui.packagedautoterminals.applied_to_one"
                        : "gui.packagedautoterminals.applied_to",
                result.changed);
        load(-1);
    }

    /** Vide la grille, sans rien écrire dans les machines. */
    public void clearGrid() {
        editor.clear();
        editor.recipeType = editor.recipeType == null ? defaultRecipeType() : editor.recipeType;
        editor.updateRecipeInfo();
        dirty = true;
        tell("gui.packagedautoterminals.cleared");
    }

    /** Grille du terminal, ou {@code null}. */
    private IGrid grid() {
        return terminal.grid();
    }

    /** Groupe visé, recalculé à la demande. */
    private ProviderPairing.Group currentGroup() {
        IGrid grid = grid();
        if (grid == null) {
            return null;
        }
        return ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                dimension, pos);
    }

    /** Referme l'éditeur et rouvre le terminal, à la même part. */
    public void backToTerminal() {
        terminal.openTerminal(getPlayerInv().player);
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
        dirty = true;
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
