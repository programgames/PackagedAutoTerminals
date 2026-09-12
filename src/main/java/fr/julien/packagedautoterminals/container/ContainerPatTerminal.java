package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.MachineSnapshot;
import fr.julien.packagedautoterminals.common.NetworkItems;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.network.PacketProviderList;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;

/**
 * Conteneur du terminal. Le serveur reste l'autorité (décision D05) : il construit
 * l'instantané, le client ne fait que l'afficher.
 */
public class ContainerPatTerminal extends AEBaseContainer {

    // L'intervalle de rafraîchissement vient de la configuration, PatConfig.refreshTicks.

    // Géométrie de la fenêtre. Ces valeurs doivent rester identiques à celles de
    // tools/make_gui_texture.py, qui dessine la planche.
    /** Largeur de la fenêtre. */
    public static final int WIDTH = 195;
    /** Hauteur de la fenêtre. */
    public static final int HEIGHT = 224;
    /** Nombre de rangées visibles dans la liste. */
    public static final int ROWS = 6;
    /** Hauteur d'une rangée, en pixels. */
    public static final int ROW_HEIGHT = 18;
    /** Bord gauche de la zone de liste. */
    public static final int LIST_LEFT = 8;
    /** Haut de la zone de liste. */
    public static final int LIST_TOP = 18;
    /** Largeur de la zone de liste. */
    public static final int LIST_WIDTH = 160;
    /** Bord gauche de l'ascenseur. */
    public static final int SCROLL_LEFT = 171;
    /** Haut de l'inventaire du joueur. */
    public static final int PLAYER_INVENTORY_TOP = 142;
    /** Champ de recherche, sur la ligne de titre. */
    public static final int SEARCH_LEFT = 96;
    public static final int SEARCH_TOP = 4;
    public static final int SEARCH_WIDTH = 72;
    public static final int SEARCH_HEIGHT = 12;

    private final PartPatTerminal terminal;
    private int ticks;
    private NBTTagCompound lastSent;

    /** Côté client seulement. Rempli par {@link PacketProviderList}. */
    public List<ProviderSnapshot> providers = new ArrayList<>();
    /** Côté client seulement. Machines d'exécution, pour l'onglet Machines. */
    public List<MachineSnapshot> machines = new ArrayList<>();
    /** Taille du dernier paquet reçu ou envoyé, en octets. Sert à la mesure du lot 2. */
    public int lastPayloadBytes;

    public ContainerPatTerminal(InventoryPlayer inventory, PartPatTerminal terminal) {
        // PIÈGE : le constructeur (InventoryPlayer, TileEntity, IPart) exige une TileEntity.
        // Avec `null`, `canInteractWith` échoue et la fenêtre se referme aussitôt, sans
        // erreur. AE2 utilise lui-même la version (InventoryPlayer, Object) pour ses parts,
        // qui retrouve seule la tuile hôte.
        super(inventory, terminal);
        this.terminal = terminal;

        // PIÈGE : `AEBaseContainer.addSlotToContainer` refuse un `Slot` vanilla et lève
        // « Invalid Slot […] for AE Container instead of AppEngSlot ». La fenêtre ne s'ouvre
        // alors jamais. AE2 fournit sa propre liaison d'inventaire, qui pose des AppEngSlot.
        bindPlayerInventory(inventory, 0, PLAYER_INVENTORY_TOP);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(getPlayerInv().player instanceof EntityPlayerMP)) {
            return;
        }
        if (ticks++ % Math.max(1, PatConfig.refreshTicks) != 0) {
            return;
        }
        // PIÈGE évité : la comparaison doit porter sur l'ENSEMBLE du message. Comparer les
        // seuls fournisseurs laisserait passer un changement d'état des machines, qui ne
        // serait alors jamais envoyé.
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag("Providers", buildPayload());
        wrapper.setTag("Machines", buildMachinePayload());
        if (wrapper.equals(lastSent)) {
            return;
        }
        lastSent = wrapper;
        PatNetwork.CHANNEL.sendTo(new PacketProviderList(wrapper),
                (EntityPlayerMP) getPlayerInv().player);
    }

    private NBTTagList buildPayload() {
        NBTTagList list = new NBTTagList();
        IGridNode node = terminal.getGridNode();
        IGrid grid = node == null ? null : node.getGrid();
        for (ProviderSnapshot snapshot : ProviderScanner.scan(grid)) {
            list.appendTag(snapshot.writeToNBT());
        }
        return list;
    }

    /**
     * Supprime une recette du porte-recettes d'une machine.
     *
     * <p>Trois vérifications avant toute écriture :
     *
     * <ol>
     *   <li>la machine est sur la grille de ce terminal, et non une position inventée ;
     *   <li>le joueur possède le droit {@code BUILD} d'AE2 ;
     *   <li>l'indice existe dans la liste actuelle.
     * </ol>
     *
     * <p>L'écriture se termine par {@code setPatternStack}. C'est cet appel, et lui seul,
     * qui déclenche {@code updatePatternList} puis {@code postPatternChange} chez
     * PackagedAuto. Modifier le NBT du porte-recettes sans réécrire l'emplacement
     * laisserait AE2 sur une vue périmée. Voir docs/PACKAGEDAUTO-MODEL.md, section 7.2.
     */
    public void removeRecipe(int dimension, BlockPos pos, int index) {
        IGridNode node = terminal.getGridNode();
        IPackageProvidingMachine machine =
                ProviderScanner.find(node == null ? null : node.getGrid(), dimension, pos);
        if (machine == null) {
            return;
        }
        if (!hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        ItemStack holder = machine.getPatternStack();
        if (holder.isEmpty() || !(holder.getItem() instanceof IRecipeListItem)) {
            return;
        }
        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return;
        }

        List<IRecipeInfo> recipes = new ArrayList<>(recipeList.getRecipeList());
        if (index < 0 || index >= recipes.size()) {
            return;
        }
        recipes.remove(index);

        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        machine.setPatternStack(holder);

        // L'instantané suivant partira au prochain rafraîchissement. On force l'envoi pour
        // que le joueur voie sa suppression tout de suite.
        ticks = 0;
        lastSent = null;
    }

    /** Machines d'exécution du réseau. Vide si l'onglet est désactivé en configuration. */
    private NBTTagList buildMachinePayload() {
        NBTTagList list = new NBTTagList();
        if (!PatConfig.machinesTab) {
            return list;
        }
        IGridNode node = terminal.getGridNode();
        IGrid grid = node == null ? null : node.getGrid();
        for (MachineSnapshot snapshot : MachineSnapshot.scan(grid)) {
            list.appendTag(snapshot.writeToNBT());
        }
        return list;
    }

    /**
     * Prépare une nouvelle recette sur cette machine, puis ouvre l'éditeur.
     *
     * <p>Si la machine n'a pas de porte-recettes, le terminal en prend un vierge sur le
     * réseau ME et l'y insère (décision D10). Sans porte-recettes disponible, il refuse et
     * l'explique dans la barre d'action.
     */
    public void newRecipe(int dimension, BlockPos pos) {
        IGridNode node = terminal.getGridNode();
        IGrid grid = node == null ? null : node.getGrid();
        IPackageProvidingMachine machine = ProviderScanner.find(grid, dimension, pos);
        if (machine == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        if (machine.getPatternStack().isEmpty() && !insertBlankHolder(grid, machine)) {
            tell("gui.packagedautoterminals.no_blank_holder");
            return;
        }
        openEditor(dimension, pos, -1);
    }

    /** Prend un porte-recettes vierge sur le réseau, et le pose dans la machine. */
    private boolean insertBlankHolder(IGrid grid, IPackageProvidingMachine machine) {
        Item holderItem = NetworkItems.findRecipeHolder();
        if (holderItem == null) {
            return false;
        }
        ItemStack holder = NetworkItems.extractOne(grid, new ItemStack(holderItem), getActionSource());
        if (holder.isEmpty()) {
            return false;
        }
        machine.setPatternStack(holder);
        return true;
    }

    /** Message court dans la barre d'action du joueur. */
    private void tell(String key) {
        EntityPlayer player = getPlayerInv().player;
        if (player instanceof EntityPlayerMP) {
            player.sendStatusMessage(new TextComponentTranslation(key), true);
        }
    }

    /**
     * Ouvre l'éditeur sur une recette existante, ou sur une recette vide.
     *
     * <p>Les mêmes vérifications que pour la suppression s'appliquent : la machine doit être
     * sur cette grille, et le joueur doit avoir le droit {@code BUILD}.
     */
    public void openEditor(int dimension, BlockPos pos, int index) {
        IGridNode node = terminal.getGridNode();
        IPackageProvidingMachine machine =
                ProviderScanner.find(node == null ? null : node.getGrid(), dimension, pos);
        if (machine == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        IRecipeInfo recipe = null;
        ItemStack holder = machine.getPatternStack();
        if (!holder.isEmpty() && holder.getItem() instanceof IRecipeListItem) {
            IRecipeList recipeList = ((IRecipeListItem) holder.getItem()).getRecipeList(holder);
            List<IRecipeInfo> recipes = recipeList == null ? null : recipeList.getRecipeList();
            if (recipes != null && index >= 0 && index < recipes.size()) {
                recipe = recipes.get(index);
            }
        }

        EntityPlayer player = getPlayerInv().player;
        PatGuiHandler.setPendingEdit(player, dimension, pos, index);
        PatGuiHandler.setPendingRecipe(player, recipe);
        player.openGui(PackagedAutoTerminals.instance,
                PatGuiHandler.EDITOR + terminal.getSide().ordinal(), player.world,
                terminal.getTile().getPos().getX(), terminal.getTile().getPos().getY(),
                terminal.getTile().getPos().getZ());
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
