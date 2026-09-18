package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.common.Feedback;
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
import fr.julien.packagedautoterminals.common.TerminalContext;
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
 * Terminal container. The server stays the authority (decision D05): it builds the snapshot,
 * the client only displays it.
 */
public class ContainerPatTerminal extends AEBaseContainer {

    // The refresh interval comes from the config, PatConfig.refreshTicks.

    // Screen geometry. These values must stay identical to the ones in
    // tools/make_gui_texture.py, which draws the sheet.
    /** Screen width. */
    public static final int WIDTH = 320;
    /** Visible rows. The sheet draws sixteen: this number can grow without redrawing it. */
    public static final int ROWS = 12;
    /** Rows drawn in the sheet. Used to locate the bottom of the screen. */
    public static final int SHEET_ROWS = 16;
    /** Height of the screen footer: label, inventory, margin. */
    public static final int FOOTER = 100;
    /** Height of one row, in pixels. */
    public static final int ROW_HEIGHT = 18;
    /** Left edge of the list area. */
    public static final int LIST_LEFT = 8;
    /** Top of the list area. */
    public static final int LIST_TOP = 22;
    /** Width of the list area. */
    public static final int LIST_WIDTH = 288;
    /** Screen height, derived from the row count. */
    public static final int HEIGHT = LIST_TOP + ROWS * ROW_HEIGHT + FOOTER;
    /** Left edge of the scrollbar. */
    public static final int SCROLL_LEFT = 300;
    /** Horizontal offset of the inventory, to centre it in the widened screen. */
    public static final int PLAYER_INVENTORY_OFFSET_X = 71;
    /** Top of the player inventory. */
    public static final int PLAYER_INVENTORY_TOP = LIST_TOP + ROWS * ROW_HEIGHT + 16;
    /** Search field, on the title line. */
    public static final int SEARCH_LEFT = 150;
    public static final int SEARCH_TOP = 4;
    public static final int SEARCH_WIDTH = 146;
    public static final int SEARCH_HEIGHT = 12;

    private final TerminalContext terminal;
    private int ticks;
    private NBTTagCompound lastSent;

    /** Client side only. Filled in by {@link PacketProviderList}. */
    public List<ProviderSnapshot> providers = new ArrayList<>();
    /** Client side only. Crafting machines, for the Machines tab. */
    public List<MachineSnapshot> machines = new ArrayList<>();
    /** Size of the last packet received or sent, in bytes. Used by the batch 2 measurement. */
    public int lastPayloadBytes;

    /** Message to show the player, key and arguments packed together. */
    @GuiSync(10)
    public String feedback = "";
    /** Message counter. It changes even when the text repeats. */
    @GuiSync(11)
    public int feedbackCount;

    public ContainerPatTerminal(InventoryPlayer inventory, TerminalContext terminal) {
        // PITFALL: the (InventoryPlayer, TileEntity, IPart) constructor requires a
        // TileEntity. With `null`, `canInteractWith` fails and the screen closes right away,
        // with no error. AE2 itself uses the (InventoryPlayer, Object) version for its parts,
        // which finds the host tile on its own.
        super(inventory, terminal.host());
        this.terminal = terminal;

        // PITFALL: `AEBaseContainer.addSlotToContainer` refuses a vanilla `Slot` and throws
        // "Invalid Slot [...] for AE Container instead of AppEngSlot". The screen then never
        // opens. AE2 provides its own inventory binding, which places AppEngSlot instances.
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

        // Range, link and energy. A refusal closes the screen on the next tick.
        String refusal = terminal.refusal(PatConfig.refreshTicks);
        if (refusal != null) {
            setValidContainer(false);
            TerminalContext.refuse((EntityPlayerMP) getPlayerInv().player, refusal);
            return;
        }
        // PITFALL avoided: the comparison must cover the WHOLE message. Comparing the
        // providers alone would miss a machine state change, which would then never be
        // sent.
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
        IGrid grid = terminal.grid();
        for (ProviderSnapshot snapshot : ProviderScanner.scan(grid)) {
            list.appendTag(snapshot.writeToNBT());
        }
        return list;
    }

    /**
     * Removes a recipe, **from every machine of the group**.
     *
     * <p>PackagedAuto requires the same recipe in the Packager and in the Unpackager.
     * Removing it from one side only would break the automation without a single message.
     *
     * <p>The index refers to the **group** list, as the terminal shows it, not to the list of
     * one machine: client and server compute the same grouping, from the same data.
     */
    /** Crafting machines of the network. Empty when the tab is disabled in the config. */
    private NBTTagList buildMachinePayload() {
        NBTTagList list = new NBTTagList();
        if (!PatConfig.machinesTab) {
            return list;
        }
        for (MachineSnapshot snapshot : MachineSnapshot.scan(terminal.grid())) {
            list.appendTag(snapshot.writeToNBT());
        }
        return list;
    }

    /** Grid of the terminal, or {@code null} when it is linked to nothing. */
    private IGrid grid() {
        return terminal.grid();
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

        RecipeWriter.Result result = RecipeWriter.apply(grid, getActionSource(),
                group.machines, group.recipes.get(index), null);
        tell(result.changed == 1
                        ? "gui.packagedautoterminals.applied_to_one"
                        : "gui.packagedautoterminals.applied_to",
                result.changed);
        refreshNow();
    }

    /**
     * Opens the editor on a brand new recipe.
     *
     * <p>The terminal pulls **nothing** out of the network on its own: when the machine has
     * no recipe holder, it refuses and says so. Taking an item out of storage stays the
     * player's decision.
     */
    public void newRecipe(int dimension, BlockPos pos) {
        IGrid grid = grid();
        IPackageProvidingMachine machine = ProviderScanner.find(grid, dimension, pos);
        if (machine == null || !hasAccess(SecurityPermissions.BUILD, false)) {
            return;
        }

        if (machine.getPatternStack().isEmpty()) {
            tell("gui.packagedautoterminals.insert_holder_first");
            return;
        }
        // Same bound as the editor: a recipe holder carries twenty recipes at most, and the
        // Package Recipe Encoder shows exactly that many.
        if (recipeCountOf(dimension, pos) >= RecipeWriter.maxRecipes()) {
            tell("gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes());
            return;
        }
        openEditor(dimension, pos, -1);
    }

    /** How many recipes the group of this machine already carries. */
    private int recipeCountOf(int dimension, BlockPos pos) {
        IGrid grid = grid();
        if (grid == null) {
            return 0;
        }
        ProviderPairing.Group group = ProviderPairing.groupOf(
                ProviderPairing.group(ProviderScanner.scan(grid)), dimension, pos);
        return group == null ? 0 : group.recipes.size();
    }

    /**
     * Sends the recipe holders of **the whole group** back to the network.
     *
     * <p>Both machines of a pair carry the same recipe. Emptying one side only would leave an
     * automation half declared. The group therefore goes together.
     *
     * <p>When the network refuses a recipe holder, for lack of room, the machine keeps it.
     * Nothing can be lost.
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
        } else {
            tell(removed == 1
                            ? "gui.packagedautoterminals.applied_to_one"
                            : "gui.packagedautoterminals.applied_to",
                    removed);
        }
        refreshNow();
    }

    /** Message shown inside the screen, not in the action bar. */
    private void tell(String key, Object... arguments) {
        feedback = Feedback.pack(key, arguments);
        feedbackCount++;
    }

    /**
     * Opens the editor on a recipe of the group, or on an empty recipe.
     *
     * <p>The same checks as for removal apply: the machine must be on this grid, and the
     * player must hold the {@code BUILD} permission.
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
        terminal.openEditor(player);
    }

    /** Forces a new snapshot to be sent, so the player sees the change right away. */
    private void refreshNow() {
        ticks = 0;
        lastSent = null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
