package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.network.PacketProviderList;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Conteneur du terminal. Le serveur reste l'autorité (décision D05) : il construit
 * l'instantané, le client ne fait que l'afficher.
 */
public class ContainerPatTerminal extends AEBaseContainer {

    /** Intervalle de rafraîchissement, en ticks. Un scan par seconde suffit largement. */
    private static final int REFRESH_TICKS = 20;

    /** Largeur de la fenêtre. Le fond reprend la planche d'AE2. */
    public static final int WIDTH = 195;
    /** Hauteur de la fenêtre. */
    public static final int HEIGHT = 204;
    /** Nombre de lignes visibles dans la liste. */
    public static final int ROWS = 5;
    /** Première ligne de la liste, en pixels. */
    public static final int LIST_TOP = 19;
    /** Haut de l'inventaire du joueur, en pixels. */
    public static final int PLAYER_INVENTORY_TOP = 122;

    private final PartPatTerminal terminal;
    private int ticks;
    private NBTTagList lastSent;

    /** Côté client seulement. Rempli par {@link PacketProviderList}. */
    public List<ProviderSnapshot> providers = new ArrayList<>();
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
        if (ticks++ % REFRESH_TICKS != 0) {
            return;
        }
        NBTTagList payload = buildPayload();
        if (payload.equals(lastSent)) {
            return;
        }
        lastSent = payload;
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag("Providers", payload);
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

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
