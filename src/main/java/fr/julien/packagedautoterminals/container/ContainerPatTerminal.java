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
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.RecipeWriter;
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
import thelm.packagedauto.api.IRecipeInfo;

/**
 * Conteneur du terminal. Le serveur reste l'autorité (décision D05) : il construit
 * l'instantané, le client ne fait que l'afficher.
 */
public class ContainerPatTerminal extends AEBaseContainer {

    // L'intervalle de rafraîchissement vient de la configuration, PatConfig.refreshTicks.

    // Géométrie de la fenêtre. Ces valeurs doivent rester identiques à celles de
    // tools/make_gui_texture.py, qui dessine la planche.
    /** Largeur de la fenêtre. */
    public static final int WIDTH = 222;
    /** Hauteur de la fenêtre. */
    public static final int HEIGHT = 228;
    /** Nombre de rangées visibles dans la liste. */
    public static final int ROWS = 6;
    /** Hauteur d'une rangée, en pixels. */
    public static final int ROW_HEIGHT = 18;
    /** Bord gauche de la zone de liste. */
    public static final int LIST_LEFT = 8;
    /** Haut de la zone de liste. */
    public static final int LIST_TOP = 22;
    /** Largeur de la zone de liste. */
    public static final int LIST_WIDTH = 190;
    /** Bord gauche de l'ascenseur. */
    public static final int SCROLL_LEFT = 202;
    /** Décalage horizontal de l'inventaire, pour le centrer dans la fenêtre élargie. */
    public static final int PLAYER_INVENTORY_OFFSET_X = 22;
    /** Haut de l'inventaire du joueur. */
    public static final int PLAYER_INVENTORY_TOP = 146;
    /** Champ de recherche, sur la ligne de titre. */
    public static final int SEARCH_LEFT = 110;
    public static final int SEARCH_TOP = 4;
    public static final int SEARCH_WIDTH = 87;
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
        bindPlayerInventory(inventory, PLAYER_INVENTORY_OFFSET_X, PLAYER_INVENTORY_TOP);
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
     * Supprime une recette, **de toutes les machines du groupe**.
     *
     * <p>PackagedAuto exige la même recette dans le Packager et dans l'Unpackager. La
     * supprimer d'un seul côté casserait l'automatisation sans le moindre message.
     *
     * <p>L'indice porte sur la liste du **groupe**, telle que le terminal l'affiche, et non
     * sur celle d'une machine : le client et le serveur calculent le même regroupement, à
     * partir des mêmes données.
     */
    /** Machines d'exécution du réseau. Vide si l'onglet est désactivé en configuration. */
    private NBTTagList buildMachinePayload() {
        NBTTagList list = new NBTTagList();
        if (!PatConfig.machinesTab) {
            return list;
        }
        for (MachineSnapshot snapshot : MachineSnapshot.scan(grid())) {
            list.appendTag(snapshot.writeToNBT());
        }
        return list;
    }

    /** Grille du terminal, ou {@code null} s'il n'est relié à rien. */
    private IGrid grid() {
        IGridNode node = terminal.getGridNode();
        return node == null ? null : node.getGrid();
    }

    public void removeRecipe(int dimension, BlockPos pos, int index) {
        IGrid grid = grid();
        if (grid == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        if (group == null || index < 0 || index >= group.recipes.size()) {
            return;
        }

        int changed = RecipeWriter.apply(grid, group.machines, group.recipes.get(index), null);
        if (changed > 1) {
            tell("gui.packagedautoterminals.applied_to", changed);
        }
        refreshNow();
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

    /**
     * Renvoie au réseau les porte-recettes de **tout le groupe**.
     *
     * <p>Les deux machines d'une paire portent la même recette. Ne vider qu'un côté
     * laisserait une automatisation à moitié déclarée. Le groupe part donc ensemble.
     *
     * <p>Si le réseau refuse un porte-recettes, faute de place, la machine le garde. Rien ne
     * peut se perdre.
     */
    public void removeHolder(int dimension, BlockPos pos) {
        IGrid grid = grid();
        if (grid == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        if (group == null) {
            return;
        }

        int removed = 0;
        boolean refused = false;
        for (ProviderSnapshot snapshot : group.machines) {
            IPackageProvidingMachine machine =
                    ProviderScanner.find(grid, snapshot.dimension, snapshot.pos);
            if (machine == null) {
                continue;
            }
            ItemStack holder = machine.getPatternStack();
            if (holder.isEmpty()) {
                continue;
            }
            if (!NetworkItems.insert(grid, holder, getActionSource()).isEmpty()) {
                refused = true;
                continue;
            }
            machine.setPatternStack(ItemStack.EMPTY);
            removed++;
        }

        if (refused) {
            tell("gui.packagedautoterminals.network_full");
        } else if (removed == 0) {
            tell("gui.packagedautoterminals.no_holder_here");
        }
        refreshNow();
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
    private void tell(String key, Object... arguments) {
        EntityPlayer player = getPlayerInv().player;
        if (player instanceof EntityPlayerMP) {
            player.sendStatusMessage(new TextComponentTranslation(key, arguments), true);
        }
    }

    /**
     * Ouvre l'éditeur sur une recette du groupe, ou sur une recette vide.
     *
     * <p>Les mêmes vérifications que pour la suppression s'appliquent : la machine doit être
     * sur cette grille, et le joueur doit avoir le droit {@code BUILD}.
     */
    public void openEditor(int dimension, BlockPos pos, int index) {
        IGrid grid = grid();
        if (grid == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }
        if (ProviderScanner.find(grid, dimension, pos) == null) {
            return;
        }

        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        IRecipeInfo recipe = group != null && index >= 0 && index < group.recipes.size()
                ? group.recipes.get(index)
                : null;

        EntityPlayer player = getPlayerInv().player;
        PatGuiHandler.setPendingEdit(player, dimension, pos, index);
        PatGuiHandler.setPendingRecipe(player, recipe);
        player.openGui(PackagedAutoTerminals.instance,
                PatGuiHandler.EDITOR + terminal.getSide().ordinal(), player.world,
                terminal.getTile().getPos().getX(), terminal.getTile().getPos().getY(),
                terminal.getTile().getPos().getZ());
    }

    /** Force l'envoi d'un nouvel instantané, pour que le joueur voie le changement aussitôt. */
    private void refreshNow() {
        ticks = 0;
        lastSent = null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
