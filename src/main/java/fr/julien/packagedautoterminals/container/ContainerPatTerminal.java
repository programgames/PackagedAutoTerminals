package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.MachineSnapshot;
import fr.julien.packagedautoterminals.common.NetworkItems;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.RecipeWriter;
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
    /** Height of the screen footer: status bar, inventory, margin. */
    public static final int FOOTER = 104;
    /**
     * Title band: it carries the screen name and the search field.
     *
     * <p>Twenty rows, not eighteen. The search field takes fourteen with its border, and the
     * frame bevel of the window owns the first three rows of the screen. At eighteen the
     * field had to start on that bevel, and its top edge was drawn outside the band.
     */
    public static final int BAND_HEIGHT = 20;
    /**
     * Tab strip, under the band.
     *
     * <p>Two pixels of panel separate it from the band, and three from the list. Without them
     * the three bands read as one block, and the tabs looked glued to the title.
     */
    public static final int TAB_STRIP_TOP = BAND_HEIGHT + 2;
    public static final int TAB_HEIGHT = 22;
    public static final int TAB_WIDTH = 92;
    /** Gap between two tabs. Two pixels read as one wide tab cut in half. */
    public static final int TAB_GAP = 4;
    /** Height of one row, in pixels. */
    public static final int ROW_HEIGHT = 18;
    /** Left edge of the list area. */
    public static final int LIST_LEFT = 8;
    /** Top of the list area, under the band and the tab strip. */
    public static final int LIST_TOP = TAB_STRIP_TOP + TAB_HEIGHT + 3;
    /** Width of the list area. */
    public static final int LIST_WIDTH = 288;
    /** Screen height, derived from the row count. */
    public static final int HEIGHT = LIST_TOP + ROWS * ROW_HEIGHT + FOOTER;
    /** Left edge of the scrollbar. */
    public static final int SCROLL_LEFT = 300;
    /** Horizontal offset of the inventory, to centre it in the widened screen. */
    public static final int PLAYER_INVENTORY_OFFSET_X = 71;
    /** Top of the player inventory. */
    public static final int PLAYER_INVENTORY_TOP = LIST_TOP + ROWS * ROW_HEIGHT + 22;
    /** Status bar: the counts, in a shallow recess under the list. */
    public static final int STATUS_TOP = LIST_TOP + ROWS * ROW_HEIGHT + 4;
    public static final int STATUS_HEIGHT = 12;
    public static final int STATUS_WIDTH = 304;
    /** The bar spans the list and the scrollbar, so it starts where the list starts. */
    public static final int STATUS_LEFT = LIST_LEFT;
    /** Search field, on the title line. */
    public static final int SEARCH_LEFT = 150;
    public static final int SEARCH_TOP = 5;
    public static final int SEARCH_WIDTH = 146;
    public static final int SEARCH_HEIGHT = 12;

    private final TerminalContext terminal;
    private int ticks;
    private NBTTagCompound lastSent;

    /** Client side only. Filled in by {@link PacketProviderList}. */
    public List<ProviderSnapshot> providers = new ArrayList<>();
    /** Client side only. Crafting machines, for the Machines tab. */
    public List<MachineSnapshot> machines = new ArrayList<>();
    /**
     * Client side only. Counts the snapshots received.
     *
     * <p>The screen rebuilds its rows only when this number moves. Before that, it rebuilt
     * them on every frame, and each rebuild regrouped the machines by comparing their recipes
     * two by two.
     */
    public int revision;
    /**
     * Size of the last payload received, in bytes. Written by {@code ClientHandler}.
     *
     * <p>It is **not shown** anywhere yet, and an earlier comment claimed it was. It exists for
     * the measurement revision **R2** asks for: R2 accepted the simple "resend everything" path
     * until a figure proves it too costly, and no figure has been taken yet. Read it in a debugger
     * on a network with many providers, or draw it in a tooltip for the length of the measurement.
     */
    public int lastPayloadBytes;
    /**
     * Client side only. Counted bytes of the snapshot the server held back, or 0.
     *
     * <p>Past {@link PacketProviderList#CAP}, the server sends this size instead of the snapshot,
     * and the screen says why the list is empty.
     */
    public long tooBigBytes;

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
        ProviderSnapshot.writeAll(ProviderScanner.scan(terminal.grid()), wrapper);
        wrapper.setTag("Machines", buildMachinePayload());
        if (wrapper.equals(lastSent)) {
            return;
        }
        lastSent = wrapper;
        PatNetwork.CHANNEL.sendTo(PacketProviderList.capped(wrapper),
                (EntityPlayerMP) getPlayerInv().player);
    }

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

    /**
     * Removes a recipe, **from every machine of the group**.
     *
     * <p>PackagedAuto requires the same recipe in the Packager and in the Unpackager.
     * Removing it from one side only would break the automation without a single message.
     *
     * <p>The index refers to the **group** list, as the terminal showed it, not to the list of
     * one machine. That list may have moved since: the recipe itself decides, and a recipe the
     * group no longer carries removes nothing.
     */
    public void removeRecipe(int dimension, BlockPos pos, int index, IRecipeInfo recipe) {
        IGrid grid = grid();
        if (grid == null || !allowed()) {
            return;
        }

        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        if (group == null) {
            return;
        }
        int found = RecipeWriter.resolve(group.recipes, index, recipe);
        if (found < 0) {
            tell("gui.packagedautoterminals.group_changed");
            return;
        }

        RecipeWriter.Result result = RecipeWriter.apply(grid, getActionSource(),
                group.machines, group.recipes.get(found), null);
        // FIXED: zero changed machines used to read as "Applied to 0 machines", in the
        // colour of a success.
        if (result.changed == 0) {
            tell("gui.packagedautoterminals.write_failed");
        } else {
            tell(result.changed == 1
                            ? "gui.packagedautoterminals.applied_to_one"
                            : "gui.packagedautoterminals.applied_to",
                    result.changed);
        }
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
        if (grid == null || !allowed()) {
            return;
        }
        // One scan serves the whole command. It used to walk the grid four times for a single
        // click: once to find the machine, once to count the recipes, then twice more inside
        // openEditor.
        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        ProviderSnapshot target = snapshotAt(all, dimension, pos);
        if (target == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }

        if (!target.holderPresent && !canSupplyHolder(grid)) {
            // Either the setting forbids taking a holder from storage, or the network has
            // none to give. Both leave the player with the same job, so both say so.
            tell(PatConfig.pullHolderFromNetwork
                    ? "gui.packagedautoterminals.no_blank_holder"
                    : "gui.packagedautoterminals.insert_holder_first");
            return;
        }

        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        // Same bound as the editor: a recipe holder carries twenty recipes at most, and the
        // Package Recipe Encoder shows exactly that many. A full group takes no new recipe,
        // but it must stay editable: the editor is the only screen that removes one. Refusing
        // to open it locked a full group out of every change, which the player reported.
        // The message goes to the action bar, because the screen changes right after.
        if (group != null && group.recipes.size() >= RecipeWriter.maxRecipes()) {
            getPlayerInv().player.sendStatusMessage(new TextComponentTranslation(
                    "gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes()), false);
            openEditor(group, dimension, pos, 0);
            return;
        }
        openEditor(group, dimension, pos, -1);
    }

    /**
     * Can the network hand out a blank Package Recipe Holder?
     *
     * <p>The question is simulated, so nothing leaves storage here. The holder is taken at
     * save time, by {@code RecipeWriter}: an editor closed without saving then costs nothing.
     */
    private boolean canSupplyHolder(IGrid grid) {
        return PatConfig.pullHolderFromNetwork
                && NetworkItems.hasBlankHolder(grid, getActionSource());
    }

    /** The machine of a scan, at this position, or {@code null} when it is not on the grid. */
    private static ProviderSnapshot snapshotAt(List<ProviderSnapshot> all, int dimension,
                                               BlockPos pos) {
        for (ProviderSnapshot snapshot : all) {
            if (snapshot.dimension == dimension && snapshot.pos.equals(pos)) {
                return snapshot;
            }
        }
        return null;
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
        if (grid == null || !allowed()) {
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

        if (refused && removed > 0) {
            // The count used to be thrown away, and the player read "the network is full"
            // while half of their machines had in fact been emptied.
            tell("gui.packagedautoterminals.holders_partial", removed, group.machines.size() - removed);
        } else if (refused) {
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

    /**
     * Gives a blank Package Recipe Holder to every machine of the group that has none.
     *
     * <p>This is the gesture the mod was missing: a new pair carries two empty machines, and
     * filling them meant walking to each one with a holder in hand. The holders come from the
     * ME network, through {@code NetworkItems.extractOne}, hence through the AE2 energy and
     * security rules. The prototype carries no NBT, so only a <b>blank</b> holder ever
     * matches: a holder that already lists recipes stays in storage.
     *
     * <p>The {@code pullHolderFromNetwork} setting does not reach here. That setting governs
     * the holder taken on its own while a recipe is saved. A click on a button is a decision
     * the player has just made.
     */
    public void insertHolders(int dimension, BlockPos pos) {
        IGrid grid = grid();
        if (grid == null || !allowed()) {
            return;
        }
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        Item blank = NetworkItems.findRecipeHolder();
        if (group == null || blank == null) {
            return;
        }

        int given = 0;
        int missing = 0;
        for (ProviderSnapshot snapshot : group.machines) {
            IPackageProvidingMachine machine =
                    ProviderScanner.find(grid, snapshot.dimension, snapshot.pos);
            if (machine == null || !machine.getPatternStack().isEmpty()) {
                continue;
            }
            ItemStack holder =
                    NetworkItems.extractOne(grid, new ItemStack(blank), getActionSource());
            if (holder.isEmpty()) {
                // The network ran out. The machines already served keep theirs.
                missing++;
                continue;
            }
            machine.setPatternStack(holder);
            given++;
        }

        if (given == 0) {
            tell("gui.packagedautoterminals.no_blank_holder");
        } else if (missing > 0) {
            tell("gui.packagedautoterminals.applied_partial", given, missing);
        } else {
            tell(given == 1
                            ? "gui.packagedautoterminals.applied_to_one"
                            : "gui.packagedautoterminals.applied_to",
                    given);
        }
        refreshNow();
    }

    /**
     * May this player change the machines of this network?
     *
     * <p>A refusal used to return without a word. On a server, a player without the BUILD
     * permission clicked a row, pressed Save, pressed delete, and nothing happened and
     * nothing was said; they read it as a broken mod.
     */
    private boolean allowed() {
        if (hasAccess(SecurityPermissions.BUILD, false)) {
            return true;
        }
        tell("gui.packagedautoterminals.no_permission");
        return false;
    }

    /**
     * Copies every recipe that sits on one side of the group into the machines that lack it.
     *
     * <p>This is the gesture the mod was built for, and it was missing. PackagedAuto needs the
     * same recipe in the Packager and in the Unpackager; the terminal knew how to tell the
     * player that a recipe was on one side only, and the only way to fix it was to open that
     * recipe and press Save, once per recipe. A pair that gained a new Unpackager therefore
     * cost twenty openings.
     *
     * <p>The recipes to copy are chosen from the snapshot, before any write: a machine served
     * by the first copy must not change the verdict on the next one.
     */
    public void repairGroup(int dimension, BlockPos pos) {
        IGrid grid = grid();
        if (grid == null || !allowed()) {
            return;
        }
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        if (group == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }

        List<IRecipeInfo> uneven = new ArrayList<>();
        for (IRecipeInfo recipe : group.recipes) {
            if (!group.isComplete(recipe)) {
                uneven.add(recipe);
            }
        }
        if (uneven.isEmpty()) {
            tell("gui.packagedautoterminals.group_already_even");
            return;
        }

        int copied = 0;
        int full = 0;
        int withoutHolder = 0;
        int supplied = 0;
        for (IRecipeInfo recipe : uneven) {
            // A null old recipe means "add it where it is missing": applyTo leaves a machine
            // that already carries it untouched, and still honours the twenty recipe cap.
            RecipeWriter.Result result =
                    RecipeWriter.apply(grid, getActionSource(), group.machines, null, recipe);
            copied += result.changed;
            full += result.full;
            withoutHolder += result.withoutHolder;
            supplied += result.supplied;
        }

        if (copied == 0) {
            tell("gui.packagedautoterminals.write_failed");
        } else if (withoutHolder > 0) {
            tell("gui.packagedautoterminals.applied_partial", copied, withoutHolder);
        } else if (full > 0) {
            tell("gui.packagedautoterminals.applied_partial_full", copied, full);
        } else if (supplied > 0) {
            tell("gui.packagedautoterminals.applied_with_holder", copied, supplied);
        } else {
            tell(copied == 1
                            ? "gui.packagedautoterminals.applied_to_one"
                            : "gui.packagedautoterminals.applied_to",
                    copied);
        }
        refreshNow();
    }

    /** Message shown inside the screen, not in the action bar. */
    private void tell(String key, Object... arguments) {
        feedback = Feedback.pack(key, arguments);
        feedbackCount++;
    }

    /**
     * Opens the editor on a recipe of the group.
     *
     * <p>The same checks as for removal apply: the machine must be on this grid, the player
     * must hold the {@code BUILD} permission, and the group must still carry the recipe.
     */
    public void openEditor(int dimension, BlockPos pos, int index, IRecipeInfo recipe) {
        IGrid grid = grid();
        if (grid == null || !allowed()) {
            return;
        }
        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        if (snapshotAt(all, dimension, pos) == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        int found = group == null ? -1 : RecipeWriter.resolve(group.recipes, index, recipe);
        if (found < 0) {
            tell("gui.packagedautoterminals.group_changed");
            return;
        }
        openEditor(group, dimension, pos, found);
    }

    /** Opens the editor on a group that the caller has already computed. */
    private void openEditor(ProviderPairing.Group group, int dimension, BlockPos pos, int index) {
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
