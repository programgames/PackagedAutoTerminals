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
import fr.julien.packagedautoterminals.common.PatConfig;
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
 * Editor for one recipe, opened from the terminal.
 *
 * <p>The slots are ghost copies: clicking with an item in hand drops its image there, without
 * consuming the item. The behaviour matches the Encoder.
 *
 * <p>The recipe is built **by the server** (decision D05), on every slot change. The client
 * only displays the result.
 */
public class ContainerPatEditor extends AEBaseContainer {

    // Screen geometry. These values must stay identical to the ones in
    // tools/make_gui_texture.py. The layout follows the Package Recipe Encoder.
    public static final int WIDTH = 258;
    public static final int HEIGHT = 338;
    /** Group name field, on the title line. */
    public static final int NAME_LEFT = 8;
    public static final int NAME_TOP = 4;
    public static final int NAME_WIDTH = 162;
    public static final int NAME_HEIGHT = 16;
    /** Tab row: one slot per recipe of the group. */
    public static final int TAB_LEFT = 8;
    public static final int TAB_TOP = 32;
    /** Ten columns over two rows, like the Package Recipe Encoder. */
    public static final int TAB_COLUMNS = 10;
    public static final int TAB_ROWS = 2;
    public static final int TAB_COUNT = TAB_COLUMNS * TAB_ROWS;
    /** Top left corner of the input grid, 9 by 9. */
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 72;
    /** Top left corner of the outputs, 3 by 3. */
    public static final int OUTPUT_LEFT = 190;
    public static final int OUTPUT_TOP = 112;
    /** Top left corner of the package preview, 3 by 3. */
    public static final int PREVIEW_LEFT = 190;
    public static final int PREVIEW_TOP = 172;
    public static final int PLAYER_INVENTORY_TOP = 256;
    /** Horizontal offset of the inventory. AE2 places slots at 8 + column * 18 + offset. */
    public static final int PLAYER_INVENTORY_OFFSET_X = 12;
    /** Maximum amount in one recipe slot. */
    public static final int MAX_SLOT_COUNT = 4096;

    private final TerminalContext terminal;
    public final EditorInventory editor;

    /** Target machine, and recipe index. A negative index means "new recipe". */
    public final int dimension;
    public final BlockPos pos;
    /**
     * Index of the recipe in the group. Negative for as long as it does not exist.
     *
     * <p>It is not final: after a new recipe is saved, the editor switches to the created
     * recipe. Without that, pressing Save a second time would add a copy.
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
        // Without this line, the green frame sat on the creation tab, because `currentTab`
        // stayed -1 until a switch happened.
        this.currentTab = index;

        bindEditorSlots();
        bindPlayerInventory(inventory, PLAYER_INVENTORY_OFFSET_X, PLAYER_INVENTORY_TOP);
    }

    /**
     * Places the 99 slots, always in the same order and of the same class on both sides.
     *
     * <p>PITFALL avoided here: if the class of a slot depended on the recipe type, client and
     * server could place different ones, because the client does not know the type when it
     * builds the screen. Slot synchronisation would then go wrong. The right to write is
     * therefore checked elsewhere: by {@code EditorInventory.isItemValidForSlot} on the
     * server, and by the greying out in the screen.
     */
    /**
     * Tab icons: the output of each recipe of the group.
     *
     * <p>These are **real** slots. Item synchronisation to the client is therefore handled by
     * the vanilla container, with no packet of our own.
     */
    public final net.minecraft.inventory.InventoryBasic tabs =
            new net.minecraft.inventory.InventoryBasic("tabs", false, TAB_COUNT);

    private void bindEditorSlots() {
        IItemHandler handler = new InvWrapper(editor);

        IItemHandler tabHandler = new InvWrapper(tabs);
        for (int tab = 0; tab < TAB_COUNT; tab++) {
            addSlotToContainer(new SlotTab(tabHandler, tab,
                    TAB_LEFT + (tab % TAB_COLUMNS) * 18,
                    TAB_TOP + (tab / TAB_COLUMNS) * 18));
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

    /** Tab: it shows the output of a recipe, and is not handled like an item. */
    private static final class SlotTab extends AppEngSlot {
        SlotTab(IItemHandler inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            setNotDraggable();
        }

        // PITFALL: do NOT refuse the item here. AE2 paints every slot it considers invalid
        // in red, and the tab ended up crossed out in red. The click is intercepted by the
        // container anyway, which never moves anything.

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }
    }

    /** Preview slot: it shows the computed result, and refuses any handling. */
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
     * Behaviour of the ghost slots.
     *
     * <p>The container, not the slot, carries this behaviour. That is also the PackagedAuto
     * choice, in {@code ContainerTileBase.slotClick}.
     *
     * <p>PITFALL: {@code AEBaseContainer.addSlotToContainer} refuses any slot that does not
     * extend {@code AppEngSlot}. The PackagedAuto slots are therefore unusable here. AE2
     * provides its own, including {@link SlotFake}.
     *
     * <p>Correction: the drag and drop from JEI does **not** come for free with
     * {@code SlotFake}. The AE2 handler only reads {@code IJEITargetSlot} through
     * {@code IJEIGhostIngredients}, which the **screen** must implement. We register our own
     * handler instead, on our screen class. See {@code PatGhostHandler}.
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
     * Current recipe type, synchronised by AE2.
     *
     * <p>PITFALL: {@code updateProgressBar} is `final` in {@code AEBaseContainer}. The vanilla
     * mechanism is therefore unusable. AE2 provides its own: every field annotated with
     * {@link GuiSync} travels to the client, and {@link #onUpdate} reports it on arrival.
     */
    @GuiSync(0)
    public int recipeTypeId = -1;

    /** Elapsed ticks, so range is only checked at the refresh rate. */
    private int ticks;

    @Override
    public void detectAndSendChanges() {
        recipeTypeId = editor.recipeType == null ? -1 : RecipeTypeRegistry.getId(editor.recipeType);

        World world = getPlayerInv().player.world;
        if (!world.isRemote) {
            // The editor follows the same rule as the terminal: out of range or out of
            // energy, it closes instead of showing a recipe it can no longer write.
            if (ticks++ % Math.max(1, PatConfig.refreshTicks) == 0) {
                String refusal = terminal.refusal(PatConfig.refreshTicks);
                if (refusal != null) {
                    setValidContainer(false);
                    TerminalContext.refuse(
                            (net.minecraft.entity.player.EntityPlayerMP) getPlayerInv().player,
                            refusal);
                    super.detectAndSendChanges();
                    return;
                }
            }
            // The name is read again on every cycle: another screen may have changed it.
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
            // The client must recompute its preview: a type change alters the recipe
            // without any slot moving.
            editor.updateRecipeInfo();
        }
        super.onUpdate(field, oldValue, newValue);
    }

    /**
     * Fills the editor from a JEI recipe.
     *
     * <p>The server does not trust the mapping it receives: it forces the type, then writes
     * only into the slots that type enables.
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
     * Drops one item into a ghost slot: the drag and drop from JEI.
     *
     * <p>A normal click goes through {@link #slotClick}, which copies the held item. A drag
     * from JEI carries no held item, so the item travels in the packet. The server checks the
     * slot here, and nowhere else.
     */
    public void setGhostSlot(int slot, ItemStack stack) {
        if (!editor.isEditable(slot)) {
            return;
        }
        if (stack.isEmpty()) {
            editor.setInventorySlotContents(slot, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(Math.max(1, Math.min(MAX_SLOT_COUNT, copy.getCount())));
            editor.setInventorySlotContents(slot, copy);
        }
        dirty = true;
        detectAndSendChanges();
    }

    /**
     * Adjusts the amount in one slot.
     *
     * <p>The upper bound is not 64: PackagedAuto can write large amounts, through
     * {@code MiscUtil.saveItemWithLargeCount}. Processing recipes need that.
     */
    /**
     * Sets the amount of one slot, typed in the amount panel.
     *
     * <p>Zero empties the slot. That is the rule of the Package Recipe Encoder, read in
     * {@code GuiItemAmountSpecifying.onOkButtonPressed}: it clamps to zero, then sends a
     * stack of zero, which is an empty stack.
     *
     * <p>The wheel keeps its own floor of one, in {@link #changeSlotCount}. Scrolling down
     * must never delete an item by surprise.
     */
    public void setSlotCount(int slot, int amount) {
        if (amount <= 0) {
            setGhostSlot(slot, ItemStack.EMPTY);
            return;
        }
        changeSlotCount(slot, amount - editor.getStackInSlot(slot).getCount());
    }

    /**
     * Sets the amount of one slot, and keeps the proportions of the recipe.
     *
     * <p>The new amount gives a ratio. Every other filled slot the type enables follows that
     * ratio. A recipe that made one item from two ingredients still makes ten from twenty.
     *
     * <p>The rule of the refusal comes from AE2UEL, read in
     * {@code ContainerPatternEncoder.divide}: it tests **every** slot first, and changes
     * nothing at all when a single one does not divide. The terminal does the same, and says
     * why. No recipe is ever written half scaled.
     *
     * <p>Only the slots the type enables take part. Writing anywhere else would break the
     * rule the whole editor follows.
     */
    public void setSlotCount(int slot, int amount, boolean scale) {
        if (!scale) {
            setSlotCount(slot, amount);
            return;
        }
        int previous = editor.getStackInSlot(slot).getCount();
        if (!editor.isEditable(slot) || previous <= 0 || amount <= 0) {
            setSlotCount(slot, amount);
            return;
        }

        int last = EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS;
        for (int index = 0; index < last; index++) {
            ItemStack stack = editor.getStackInSlot(index);
            if (stack.isEmpty() || !editor.isEditable(index)) {
                continue;
            }
            long scaled = (long) stack.getCount() * amount;
            if (scaled % previous != 0) {
                tell("gui.packagedautoterminals.ratio_not_exact");
                return;
            }
            if (scaled / previous > MAX_SLOT_COUNT) {
                tell("gui.packagedautoterminals.ratio_too_large", MAX_SLOT_COUNT);
                return;
            }
        }

        for (int index = 0; index < last; index++) {
            ItemStack stack = editor.getStackInSlot(index);
            if (stack.isEmpty() || !editor.isEditable(index)) {
                continue;
            }
            ItemStack changed = stack.copy();
            changed.setCount((int) ((long) stack.getCount() * amount / previous));
            editor.setInventorySlotContents(index, changed);
        }
        dirty = true;
        detectAndSendChanges();
    }

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
     * Writes the recipe into **every machine of the group**.
     *
     * <p>PackagedAuto requires the same recipe in the Packager and in the Unpackager. Writing
     * one side only would break the automation silently.
     *
     * <p>The incomplete group case is common: the player has just placed a pair and encodes
     * the first recipe, so the two recipe holders share nothing yet. When the network holds a
     * single machine of the missing role, it joins the targets. When there are several, the
     * terminal invents nothing and says so.
     *
     * @return true when at least one machine was changed.
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

        // The player always gets one piece of feedback, and **only one**. Two successive
        // `tell` calls overwrote each other: the player saw the warning, never the
        // confirmation, and no longer knew whether the write had succeeded.
        if (result.withoutHolder > 0) {
            tell("gui.packagedautoterminals.applied_partial",
                    result.changed, result.withoutHolder);
        } else if (result.changed == 1 && missing != null) {
            tell("gui.packagedautoterminals.applied_no_partner", result.changed);
        } else {
            tell(result.changed == 1
                            ? "gui.packagedautoterminals.applied_to_one"
                            : "gui.packagedautoterminals.applied_to",
                    result.changed);
        }

        // The editor follows the recipe it has just written: the next save will edit it,
        // instead of creating a copy.
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
     * Writes the group name into **every** one of its machines.
     *
     * <p>The name is stored per machine, because a group is rebuilt on every scan. See
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
     * Current name of the group.
     *
     * <p>The AE2 {@code SyncData} can carry a string: the field therefore travels to the
     * client with no extra packet.
     */
    @GuiSync(1)
    public String groupName = "";

    /** Index of the displayed recipe in the tab row. Negative: the creation tab. */
    @GuiSync(2)
    public int currentTab = -1;
    /** Number of recipes in the group. Used by the client to place the creation tab. */
    @GuiSync(3)
    public int recipeCount;
    /** First recipe shown in the row. The arrows move it. */
    @GuiSync(4)
    public int tabOffset;
    /** Message to show, key and arguments packed together. */
    @GuiSync(10)
    public String feedback = "";
    /** Message counter. It changes even when the text repeats. */
    @GuiSync(11)
    public int feedbackCount;

    /**
     * Does the editor hold an unsaved change?
     *
     * <p>The field is **synchronised**: the client needs it to mark the open tab with a dot.
     * Without that dot, the player only learned about work in progress after clicking another
     * tab and being refused.
     */
    @GuiSync(5)
    public boolean dirty;
    /** Tab requested while unsaved work was in progress. */
    private int pendingTab = Integer.MIN_VALUE;

    /** Message shown inside the screen, not in the action bar. */
    private void tell(String key, Object... arguments) {
        feedback = Feedback.pack(key, arguments);
        feedbackCount++;
    }

    /**
     * Switches to another recipe of the group.
     *
     * <p>Unsaved work is never lost without a warning: the first click warns, the second
     * switches.
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
     * Fills the tab row with the output of each recipe of the group.
     *
     * <p>The last slot stays empty: that is the creation tab. Since the slots are real, the
     * client receives these items with no extra packet.
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

    /** Scrolls the tab row, when the group holds more recipes than the row has slots. */
    public void scrollTabs(boolean forward) {
        int maximum = Math.max(0, recipeCount + 1 - TAB_COUNT);
        tabOffset = Math.max(0, Math.min(maximum, tabOffset + (forward ? 1 : -1)));
    }

    /** Loads the recipe at the given index, or clears the editor for a creation. */
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

        // Full send: differential synchronisation left the grid empty on screen after a tab
        // change.
        for (net.minecraft.inventory.IContainerListener listener : listeners) {
            listener.sendAllContents(this, getInventory());
        }
    }

    /** Deletes the current recipe, in every machine of the group. */
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

    /** Clears the grid, without writing anything into the machines. */
    public void clearGrid() {
        editor.clear();
        editor.recipeType = editor.recipeType == null ? defaultRecipeType() : editor.recipeType;
        editor.updateRecipeInfo();
        dirty = true;
        tell("gui.packagedautoterminals.cleared");
    }

    /** Grid of the terminal, or {@code null}. */
    private IGrid grid() {
        return terminal.grid();
    }

    /** Target group, recomputed on demand. */
    private ProviderPairing.Group currentGroup() {
        IGrid grid = grid();
        if (grid == null) {
            return null;
        }
        return ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                dimension, pos);
    }

    /** Closes the editor and reopens the terminal, on the same part. */
    public void backToTerminal() {
        terminal.openTerminal(getPlayerInv().player);
    }

    /**
     * Moves to the next or previous recipe type, then rebuilds the recipe.
     *
     * <p>The null type case is handled here: on a new recipe, the editor can open with no
     * type, and {@code getNextRecipeType} then has no starting point.
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
     * Type offered when a new recipe is opened.
     *
     * <p>Basic crafting comes first when it exists: that is the most common case. The Package
     * Crafter can be disabled in the config, in which case that type is not registered, and
     * the first available type will do.
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
