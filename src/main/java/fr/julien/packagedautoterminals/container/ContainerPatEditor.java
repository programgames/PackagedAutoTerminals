package fr.julien.packagedautoterminals.container;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.helpers.InventoryAction;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.FluidPackets;
import fr.julien.packagedautoterminals.common.GroupNames;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderRole;
import fr.julien.packagedautoterminals.common.ProviderScanner;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.common.RecipeScale;
import fr.julien.packagedautoterminals.common.RecipeWriter;
import fr.julien.packagedautoterminals.common.TerminalContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeListItem;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.RecipeTypeRegistry;
import thelm.packagedauto.recipe.RecipeTypeProcessing;
import thelm.packagedauto.tile.TileEncoder;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFake;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Editor for one recipe, opened from the terminal.
 *
 * <p>The slots are ghost copies: clicking with an item in hand drops its image there, without
 * consuming the item. The behaviour matches the Encoder.
 *
 * <p>Both sides rebuild the recipe on every slot change, as the PackagedAuto Encoder does. The
 * client uses its copy for the preview and the Save button only. The server writes its own
 * copy, at Save (decision D05).
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
    /**
     * The recipe the editor opened on, or {@code null} for a creation.
     *
     * <p>PITFALL: {@link #index} alone is not an identity. It names a **position** in a group that
     * is rebuilt from a fresh scan on every write. Another player, or a hopper, can change the
     * recipe holder while this screen is open, and the recipe at that position is then a different
     * one. Saving replaced it, and destroyed a recipe nobody meant to touch.
     *
     * <p>Every write therefore names the recipe itself, and refuses when the group no longer
     * carries it. {@code IRecipeInfo} implements {@code equals}: {@code RecipeWriter.indexOf} and
     * {@code save} already rely on it.
     */
    private IRecipeInfo openedRecipe;

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
        this.openedRecipe = editor.recipeInfo;

        this.holders = null;

        bindEditorSlots();
        if (terminal.isWireless()) {
            lockPlayerInventorySlot(terminal.inventorySlot());
        }
        bindPlayerInventory(inventory, PLAYER_INVENTORY_OFFSET_X, PLAYER_INVENTORY_TOP);
    }

    /**
     * Holder mode (D43): the editor works on the recipe holders put in its two slots, with no
     * ME network, no link and no range.
     *
     * <p>PITFALL: the two-argument constructor of {@code AEBaseContainer} throws on a null
     * anchor ("Must have a valid anchor"). The four-argument one takes three nulls, read in the
     * AE2UEL 0.56.5 bytecode, and nothing in it needs a host.
     */
    public ContainerPatEditor(InventoryPlayer inventory, EditorInventory editor) {
        super(inventory, null, null, null);
        this.terminal = null;
        this.editor = editor;
        this.dimension = 0;
        this.pos = BlockPos.ORIGIN;
        this.index = -1;
        this.holders = new HolderInventory();

        bindEditorSlots();
        firstHolderSlot = inventorySlots.size();
        addSlotToContainer(new SlotHolder(holders, 0, HOLDER_LEFT, HOLDER_TOP));
        addSlotToContainer(new SlotHolder(holders, 1, TWIN_LEFT, HOLDER_TOP));
        firstPlayerSlot = inventorySlots.size();
        bindPlayerInventory(inventory, PLAYER_INVENTORY_OFFSET_X, PLAYER_INVENTORY_TOP);
    }

    /** The main and the twin recipe holder. {@code null} outside holder mode. */
    public final InventoryBasic holders;
    /** Holder slots sit on the title line, where the group name lives in the other mode. */
    public static final int HOLDER_LEFT = 9;
    public static final int TWIN_LEFT = 80;
    public static final int HOLDER_TOP = 3;
    private int firstHolderSlot;
    private int firstPlayerSlot;
    /** Last stack read in each holder slot, and its recipes. See {@link #recipesIn}. */
    private final ItemStack[] seenHolders = {ItemStack.EMPTY, ItemStack.EMPTY};
    private final List<List<IRecipeInfo>> seenRecipes =
            new ArrayList<>(java.util.Arrays.asList(new ArrayList<>(), new ArrayList<>()));

    /** One recipe holder per slot: a stack of two would take a single write for both. */
    private static final class HolderInventory extends InventoryBasic {
        HolderInventory() {
            super("holders", false, 2);
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        // The empty stack is valid, otherwise AE2 paints the empty slot red.
        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return stack.isEmpty() || stack.getItem() instanceof IRecipeListItem;
        }
    }

    private static final class SlotHolder extends AppEngSlot {
        SlotHolder(InventoryBasic holders, int index, int x, int y) {
            super(new InvWrapper(holders), index, x, y);
        }
    }

    public boolean isHolderMode() {
        return holders != null;
    }

    /**
     * Tab icons: the output of each recipe of the group.
     *
     * <p>These are **real** slots. Item synchronisation to the client is therefore handled by
     * the vanilla container, with no packet of our own.
     */
    public final net.minecraft.inventory.InventoryBasic tabs =
            new net.minecraft.inventory.InventoryBasic("tabs", false, TAB_COUNT);

    /**
     * Places the tabs and the 99 editor slots, always in the same order and of the same class
     * on both sides.
     *
     * <p>PITFALL avoided here: if the class of a slot depended on the recipe type, client and
     * server could place different ones, because the client does not know the type when it
     * builds the screen. Slot synchronisation would then go wrong. The right to write is
     * therefore checked elsewhere: by {@code EditorInventory.isItemValidForSlot} on the
     * server, and by the greying out in the screen.
     */
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

    /** The slot of a tab. {@link #bindEditorSlots} places the tabs first. */
    public Slot tabSlot(int tab) {
        return getSlot(tab);
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

        /**
         * The slot is always "valid", so AE2 never paints it red.
         *
         * <p>PITFALL, hit twice. {@code AEBaseGui.drawSlot} paints every slot it judges invalid
         * with the colour {@code 0x66FF6666}, a red veil. The package preview therefore sat on a
         * red background.
         *
         * <p>Removing an {@code isItemValid} override is **not** enough, and that first fix
         * failed. {@code AppEngSlot.isItemValid} delegates to the item handler, hence to
         * {@code EditorInventory.isItemValidForSlot}, which answers {@code isEditable}: false for
         * a preview slot. The veil came back through the inventory.
         *
         * <p>Answering {@code Valid} here stops the test at its source. It changes nothing else:
         * {@code slotClick} intercepts this slot and moves nothing, and {@code canTakeStack}
         * refuses every withdrawal.
         */
        @Override
        public hasCalculatedValidness getIsValid() {
            return hasCalculatedValidness.Valid;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }
    }

    /**
     * A tab switches recipes, and a preview slot ignores the click.
     *
     * <p>The client runs this method too, as a prediction, from
     * {@code PlayerControllerMP.windowClick}. Only the server switches the tab: it alone knows
     * the recipes, and it sends the result.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = inventorySlots.get(slotId);
            if (slot instanceof SlotTab) {
                if (!player.world.isRemote) {
                    selectTab(slot.getSlotIndex());
                }
                return ItemStack.EMPTY;
            }
            if (slot instanceof SlotResult) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    /**
     * Behaviour of the ghost slots.
     *
     * <p>PITFALL: a click on a {@link SlotFake} never reaches {@link #slotClick}.
     * {@code AEBaseGui.handleMouseClick} turns it into a {@code PacketInventoryAction}, which the
     * server hands to this method. The slot rules of the editor therefore live here.
     *
     * <p>The rules on top of AE2: a slot the type disables refuses everything, the right click
     * with an empty hand empties the slot, and any change marks the work as unsaved.
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
    public void doAction(EntityPlayerMP player, InventoryAction action, int slotId, long id) {
        Slot slot = slotId >= 0 && slotId < inventorySlots.size() ? inventorySlots.get(slotId) : null;
        if (!(slot instanceof SlotFake)) {
            super.doAction(player, action, slotId, id);
            return;
        }
        if (!editor.isEditable(slot.getSlotIndex())) {
            return;
        }
        ItemStack before = slot.getStack().copy();
        if (action == InventoryAction.SPLIT_OR_PLACE_SINGLE && player.inventory.getItemStack().isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            super.doAction(player, action, slotId, id);
        }
        if (!ItemStack.areItemStacksEqual(before, slot.getStack())) {
            dirty = true;
        }
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

    /** Elapsed ticks, so range and the group are only checked at the refresh rate. */
    private int ticks;

    /**
     * Target group as of the last scan, for the tab row.
     *
     * <p>A scan reads the recipes of every holder on the network, so it runs at the refresh rate
     * and after each write, never on every tick. No write trusts this copy: each one scans
     * again first, because the server stays the authority.
     */
    private ProviderPairing.Group scannedGroup;

    @Override
    public void detectAndSendChanges() {
        World world = getPlayerInv().player.world;
        if (!world.isRemote && holders != null) {
            refreshTabs();
            drift = !holders.getStackInSlot(0).isEmpty() && !holders.getStackInSlot(1).isEmpty()
                    && !sameRecipes(recipesIn(0), recipesIn(1));
        } else if (!world.isRemote) {
            if (terminal.itemLeft(getPlayerInv().player)) {
                setValidContainer(false);
                sendChanges();
                return;
            }
            // The editor follows the same rule as the terminal: out of range or out of
            // energy, it closes instead of showing a recipe it can no longer write.
            if (ticks++ % Math.max(1, PatConfig.refreshTicks) == 0) {
                String refusal = terminal.refusal(getPlayerInv().player, PatConfig.refreshTicks);
                if (refusal != null) {
                    setValidContainer(false);
                    TerminalContext.refuse(
                            (net.minecraft.entity.player.EntityPlayerMP) getPlayerInv().player,
                            refusal);
                    sendChanges();
                    return;
                }
                rescan();
            }
            // The name is read again on every cycle: another screen may have changed it.
            groupName = GroupNames.get(world).get(dimension, pos);
            refreshTabs();
        }
        sendChanges();
    }

    /**
     * Sends the changed slots and fields. The slot edits call it instead of
     * {@link #detectAndSendChanges}, so a click never counts as a tick, and never scans.
     */
    private void sendChanges() {
        recipeTypeId = editor.recipeType == null ? -1 : RecipeTypeRegistry.getId(editor.recipeType);
        super.detectAndSendChanges();
        // The slots go after the type, never before: the client rebuilds the recipe on the slots
        // it receives, and a stale type that cannot set outputs wipes the ones just sent.
        if (sendAllSlots) {
            sendAllSlots = false;
            for (net.minecraft.inventory.IContainerListener listener : listeners) {
                listener.sendAllContents(this, getInventory());
            }
        }
    }

    /** Set by {@link #load}: the next {@link #sendChanges} also sends every slot at once. */
    private boolean sendAllSlots;

    /**
     * A full send rebuilds the recipe once on the client, not once per slot: a craft recipe
     * searches the whole recipe registry.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void setAll(List<ItemStack> stacks) {
        editor.beginGroup();
        try {
            super.setAll(stacks);
        } finally {
            editor.endGroup();
        }
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        // AE2 also applies a PacketProgressBar sent by the client on the server. Only the
        // server sets the type: the client follows it.
        if ("recipeTypeId".equals(field) && getPlayerInv().player.world.isRemote) {
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
        if (isDisabled(type)) {
            tell("gui.packagedautoterminals.type_disabled");
            return;
        }
        editor.clear();
        editor.recipeType = type;

        // One rebuild for the whole transfer, not one per slot. See EditorInventory.grouping.
        editor.beginGroup();
        try {
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
        } finally {
            editor.endGroup();
        }
        dirty = true;
        sendChanges();
    }

    /**
     * Drops one item into a ghost slot: the drag and drop from JEI.
     *
     * <p>A normal click goes through {@link #doAction}, which copies the held item. A drag
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
        sendChanges();
    }

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
        changeSlotCount(slot, amount - amountOf(editor.getStackInSlot(slot)));
    }

    /**
     * Amount held by one stack, in the unit that stack uses.
     *
     * <p>An item counts in items. A fluid packet counts in millibuckets, and its item count always
     * stays at one. Every amount rule of the editor goes through this method, so the wheel, the
     * amount panel and the proportions work the same way on both.
     */
    public static int amountOf(ItemStack stack) {
        int fluid = FluidPackets.amountOf(stack);
        return fluid >= 0 ? fluid : stack.getCount();
    }

    /** Upper bound of one stack: 4096 items, or a billion millibuckets. */
    public static int maxAmountOf(ItemStack stack) {
        return FluidPackets.isPacket(stack) ? FluidPackets.MAX_AMOUNT : MAX_SLOT_COUNT;
    }

    /** The same stack, carrying another amount. */
    private static ItemStack withAmount(ItemStack stack, int amount) {
        if (FluidPackets.isPacket(stack)) {
            return FluidPackets.withAmount(stack, amount);
        }
        ItemStack changed = stack.copy();
        changed.setCount(amount);
        return changed;
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
        int previous = amountOf(editor.getStackInSlot(slot));
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
            long scaled = (long) amountOf(stack) * amount;
            if (scaled % previous != 0) {
                tell("gui.packagedautoterminals.ratio_not_exact");
                return;
            }
            int max = maxAmountOf(stack);
            if (scaled / previous > max) {
                tell("gui.packagedautoterminals.ratio_too_large", max);
                return;
            }
        }

        // Every slot of the recipe moves at once, so the recipe is rebuilt once.
        editor.beginGroup();
        try {
            for (int index = 0; index < last; index++) {
                ItemStack stack = editor.getStackInSlot(index);
                if (stack.isEmpty() || !editor.isEditable(index)) {
                    continue;
                }
                editor.setInventorySlotContents(index,
                        withAmount(stack, (int) ((long) amountOf(stack) * amount / previous)));
            }
        } finally {
            editor.endGroup();
        }
        dirty = true;
        sendChanges();
    }

    /**
     * Adjusts the amount in one slot.
     *
     * <p>The upper bound is not 64: PackagedAuto can write large amounts, through
     * {@code MiscUtil.saveItemWithLargeCount}. Processing recipes need that.
     */
    public void changeSlotCount(int slot, int delta) {
        if (!editor.isEditable(slot)) {
            return;
        }
        ItemStack stack = editor.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        long amount = (long) amountOf(stack) + delta;
        amount = Math.max(1L, Math.min(maxAmountOf(stack), amount));
        editor.setInventorySlotContents(slot, withAmount(stack, (int) amount));
        dirty = true;
        sendChanges();
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
        if (holders != null) {
            return saveToHolders();
        }
        IGrid grid = terminal.grid();
        if (grid == null || !allowed()) {
            return false;
        }

        List<ProviderSnapshot> all = ProviderScanner.scan(grid);
        ProviderPairing.Group group =
                ProviderPairing.groupOf(ProviderPairing.group(all), dimension, pos);
        if (group == null) {
            // The machine was broken while this screen was open. Pressing Save used to do
            // nothing at all, and say nothing either.
            tell("gui.packagedautoterminals.machine_gone");
            return false;
        }

        // The recipe this editor opened on, not whatever now sits at its old position. See the
        // comment on `openedRecipe`.
        IRecipeInfo oldRecipe = openedRecipe;
        if (oldRecipe != null && indexOf(group.recipes, oldRecipe) < 0) {
            tell("gui.packagedautoterminals.group_changed");
            return false;
        }

        // A recipe holder holds twenty recipes at most, and the Package Recipe Encoder shows
        // exactly that many. Writing a twenty first one would produce a recipe the player could
        // never open again in their Encoder. The refusal happens here, before any machine is
        // touched, so no group is ever left half written.
        if (oldRecipe == null && group.recipes.size() >= RecipeWriter.maxRecipes()) {
            tell("gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes());
            return false;
        }

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
        show(result.feedback(missing != null));
        if (result.changed == 0) {
            return false;
        }
        followWritten();
        return true;
    }

    /**
     * The editor follows the recipe it has just written: the next save will edit it, instead
     * of creating a copy.
     */
    private void followWritten() {
        if (holders == null) {
            rescan();
        }
        List<IRecipeInfo> after = currentRecipes();
        if (after != null) {
            int written = indexOf(after, editor.recipeInfo);
            if (written >= 0) {
                index = written;
                currentTab = written;
                openedRecipe = after.get(written);
            }
        }
        dirty = false;
        pendingTab = Integer.MIN_VALUE;
    }

    /**
     * Holder mode: writes the recipe into every holder in the two slots (D43).
     *
     * <p>A holder that lacks the edited recipe receives the new one, like a machine of a group
     * that drifted. A new recipe that fits in one holder only is refused in both, before any
     * write: a half write is the drift the twin slot exists to prevent.
     */
    private boolean saveToHolders() {
        IRecipeInfo oldRecipe = openedRecipe;
        if (oldRecipe != null && indexOf(holderRecipes(), oldRecipe) < 0) {
            tell("gui.packagedautoterminals.group_changed");
            return false;
        }
        List<ItemStack> targets = insertedHolders();
        if (targets.isEmpty()) {
            tell("gui.packagedautoterminals.insert_holder");
            return false;
        }
        for (ItemStack holder : targets) {
            List<IRecipeInfo> recipes = RecipeWriter.recipesOf(holder);
            if (indexOf(recipes, oldRecipe) < 0 && indexOf(recipes, editor.recipeInfo) < 0
                    && recipes.size() >= RecipeWriter.maxRecipes()) {
                tell("gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes());
                return false;
            }
        }
        int changed = writeHolders(targets, oldRecipe, editor.recipeInfo);
        if (changed == 0) {
            tell("gui.packagedautoterminals.write_failed");
            return false;
        }
        tell("gui.packagedautoterminals.holders_written", changed);
        followWritten();
        return true;
    }

    private int writeHolders(List<ItemStack> targets, IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        int changed = 0;
        for (ItemStack holder : targets) {
            if (RecipeWriter.editHolder(holder, oldRecipe, newRecipe)
                    == RecipeWriter.Outcome.CHANGED) {
                changed++;
            }
        }
        holders.markDirty();
        return changed;
    }

    /**
     * Copies every recipe missing from one holder into it, both ways. Deletes nothing.
     *
     * <p>A blank holder in the twin slot therefore becomes a copy of the main one.
     */
    public void syncHolders() {
        if (holders == null) {
            return;
        }
        List<ItemStack> targets = insertedHolders();
        if (targets.size() < 2) {
            tell("gui.packagedautoterminals.insert_holder");
            return;
        }
        List<IRecipeInfo> all = holderRecipes();
        if (all.size() > RecipeWriter.maxRecipes()) {
            tell("gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes());
            return;
        }
        int copied = 0;
        for (ItemStack holder : targets) {
            List<IRecipeInfo> recipes = new ArrayList<>(RecipeWriter.recipesOf(holder));
            int added = RecipeWriter.addMissing(recipes, all);
            if (added > 0 && RecipeWriter.setRecipes(holder, recipes)) {
                copied += added;
            }
        }
        holders.markDirty();
        tell("gui.packagedautoterminals.holders_synced", copied);
    }

    /** The holders in the two slots, main first. */
    private List<ItemStack> insertedHolders() {
        List<ItemStack> inserted = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            if (!holders.getStackInSlot(slot).isEmpty()) {
                inserted.add(holders.getStackInSlot(slot));
            }
        }
        return inserted;
    }

    /**
     * Recipes of one holder slot.
     *
     * <p>Reading a holder rebuilds every recipe from its NBT, and a craft recipe looks itself up
     * in the recipe book while doing so. This runs on every container tick, so the list is only
     * read again when the stack changes.
     */
    private List<IRecipeInfo> recipesIn(int slot) {
        ItemStack stack = holders.getStackInSlot(slot);
        if (!ItemStack.areItemStacksEqual(stack, seenHolders[slot])) {
            seenHolders[slot] = stack.copy();
            seenRecipes.set(slot, RecipeWriter.recipesOf(stack));
        }
        return seenRecipes.get(slot);
    }

    /** Distinct recipes of both holders, main first: the tab row of holder mode. */
    private List<IRecipeInfo> holderRecipes() {
        List<IRecipeInfo> all = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            for (IRecipeInfo recipe : recipesIn(slot)) {
                if (indexOf(all, recipe) < 0) {
                    all.add(recipe);
                }
            }
        }
        return all;
    }

    private static boolean sameRecipes(List<IRecipeInfo> first, List<IRecipeInfo> second) {
        for (IRecipeInfo recipe : first) {
            if (indexOf(second, recipe) < 0) {
                return false;
            }
        }
        for (IRecipeInfo recipe : second) {
            if (indexOf(first, recipe) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Holder mode: the two holders differ. The screen then shows the Sync button. */
    @GuiSync(6)
    public boolean drift;

    /**
     * Writes the group name into **every** one of its machines.
     *
     * <p>The name is stored per machine, because a group is rebuilt on every scan. See
     * {@link GroupNames}.
     */
    public void renameGroup(String name) {
        if (holders != null) {
            return;
        }
        IGrid grid = terminal.grid();
        if (grid == null || !allowed()) {
            return;
        }
        List<ProviderPairing.Group> groups = ProviderPairing.group(ProviderScanner.scan(grid));
        ProviderPairing.Group group = ProviderPairing.groupOf(groups, dimension, pos);
        if (group == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }

        // A name is the explicit link between two groups, so two groups that share one are
        // joined into a single row, and every later write reaches all of their machines.
        // That is the feature, and nothing used to warn the player before it happened.
        String clean = GroupNames.sanitize(name);
        boolean merges = false;
        if (!clean.isEmpty()) {
            for (ProviderPairing.Group other : groups) {
                if (other != group && clean.equals(other.customName())) {
                    merges = true;
                    break;
                }
            }
        }

        // One storage for the whole server: GroupNames.get hands back the overworld
        // instance whatever world it is given. The key carries the dimension.
        GroupNames names = GroupNames.get(getPlayerInv().player.world);
        for (ProviderSnapshot machine : group.machines) {
            names.set(machine.dimension, machine.pos, name);
        }
        groupName = clean;
        rescan();
        tell(merges
                ? "gui.packagedautoterminals.name_merges"
                : "gui.packagedautoterminals.renamed");
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

    /**
     * May this player change the machines of this network?
     *
     * <p>A refusal used to return without a word, so a player without the BUILD permission
     * pressed Save and read nothing at all.
     */
    private boolean allowed() {
        if (hasAccess(SecurityPermissions.BUILD, false)) {
            return true;
        }
        tell("gui.packagedautoterminals.no_permission");
        return false;
    }

    /** Message shown inside the screen, not in the action bar. */
    private void tell(String key, Object... arguments) {
        show(Feedback.pack(key, arguments));
    }

    private void show(String packed) {
        feedback = packed;
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
            if (!canCreate()) {
                tell("gui.packagedautoterminals.group_full", RecipeWriter.maxRecipes());
                return;
            }
            target = -1;
        }
        if (target == currentTab && !dirty) {
            // The tab is already open. Reloading it changed nothing on screen but resent the
            // whole inventory, so the held item bobbed: a player read that as a click that
            // did something.
            return;
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
        List<IRecipeInfo> recipes = currentRecipes();
        if (recipes == null) {
            recipes = new ArrayList<>();
        }
        recipeCount = recipes.size();

        if (tabOffset > maxTabOffset()) {
            tabOffset = maxTabOffset();
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
                sendTab(slot, icon);
            }
        }
    }

    /**
     * Sends one tab icon right away, past the vanilla filter.
     *
     * <p>FIXED: a recipe holder dropped into holder mode showed no tab until the player
     * clicked one. Read in Forge 14.23.5.2847: {@code NetHandlerPlayServer.processClickWindow}
     * runs {@code detectAndSendChanges} with {@code isChangingQuantityOnly} set, and
     * {@code EntityPlayerMP.sendSlotContents} drops every slot update meanwhile, while the
     * container records it as sent. Dropping a holder is such a click, so the tabs it brought
     * never reached the screen, until the full resend of a tab click.
     *
     * <p>The tab slots are the first ones added, so a tab index is also its slot number.
     */
    private void sendTab(int slot, ItemStack icon) {
        for (net.minecraft.inventory.IContainerListener listener : listeners) {
            if (listener instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ((net.minecraft.entity.player.EntityPlayerMP) listener).connection.sendPacket(
                        new net.minecraft.network.play.server.SPacketSetSlot(windowId, slot, icon));
            }
        }
    }

    /**
     * Can this group still take one more recipe?
     *
     * <p>Both sides compute it: {@code TileEncoder.patternSlots} comes from the PackagedAuto
     * config, which the client loads too. No field travels for this.
     */
    public boolean canCreate() {
        return recipeCount < RecipeWriter.maxRecipes();
    }

    /**
     * Last offset the tab row can reach. Zero means the row shows everything.
     *
     * <p>The row shows {@code TAB_COUNT} slots, which is twenty, and a recipe holder holds
     * twenty recipes at most. The creation tab is the only reason the row could ever overflow,
     * and it disappears once the group is full. The offset is therefore zero in practice, and
     * the arrows stay greyed out.
     *
     * <p>That is the real answer to the player report: the row used to slide by one slot on a
     * full group, and the player read the slide as a deleted recipe. Now nothing slides.
     */
    public int maxTabOffset() {
        return Math.max(0, recipeCount + (canCreate() ? 1 : 0) - TAB_COUNT);
    }

    /**
     * Scrolls the tab row, when the group holds more recipes than the row has slots.
     *
     * <p>The step is a **whole row**, not one slot. A one-slot step shifted every icon sideways,
     * and a player read it as "the arrow deleted my last recipe". A row step moves the icons far
     * enough that the scrolling is obvious.
     *
     * <p>The last step is clamped, so the row always ends on the creation tab.
     */
    public void scrollTabs(boolean forward) {
        int step = forward ? TAB_COLUMNS : -TAB_COLUMNS;
        tabOffset = Math.max(0, Math.min(maxTabOffset(), tabOffset + step));
    }

    /** Loads the recipe at the given index, or clears the editor for a creation. */
    private void load(int target) {
        currentTab = target;
        index = target;
        editor.clear();

        openedRecipe = null;
        if (target < 0) {
            // FIXED: a new recipe used to restart on the default type, and the player had to
            // cycle back to the type of the group every time. It now keeps the type of the tab
            // the player came from: a group almost always holds a single type.
            if (editor.recipeType == null) {
                editor.recipeType = defaultRecipeType();
            }
        } else {
            List<IRecipeInfo> recipes = currentRecipes();
            if (recipes != null && target < recipes.size()) {
                openedRecipe = recipes.get(target);
                editor.load(openedRecipe);
            }
        }
        editor.updateRecipeInfo();
        dirty = false;

        // Full send: differential synchronisation left the grid empty on screen after a tab
        // change.
        sendAllSlots = true;
    }

    /** Deletes the current recipe, in every machine of the group. */
    public void deleteCurrent() {
        if (index < 0 || openedRecipe == null) {
            tell("gui.packagedautoterminals.nothing_to_delete");
            return;
        }
        if (holders != null) {
            // Same rule as online: the recipe goes from every holder that lists it.
            if (indexOf(holderRecipes(), openedRecipe) < 0) {
                tell("gui.packagedautoterminals.group_changed");
                return;
            }
            int changed = writeHolders(insertedHolders(), openedRecipe, null);
            if (changed == 0) {
                tell("gui.packagedautoterminals.write_failed");
                return;
            }
            tell("gui.packagedautoterminals.holders_written", changed);
            keepAsNewRecipe();
            return;
        }
        IGrid grid = grid();
        if (grid == null || !allowed()) {
            return;
        }
        ProviderPairing.Group group = rescan();
        if (group == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }
        // Same rule as save: the recipe is named, never its position.
        if (indexOf(group.recipes, openedRecipe) < 0) {
            tell("gui.packagedautoterminals.group_changed");
            return;
        }

        RecipeWriter.Result result = RecipeWriter.apply(grid, getActionSource(),
                group.machines, openedRecipe, null);
        rescan();
        show(result.feedback(false));
        if (result.changed == 0) {
            return;
        }
        keepAsNewRecipe();
    }

    /**
     * After a delete, the grid keeps the deleted recipe on the creation tab: Save writes it back.
     * It is unsaved work, so leaving the tab asks first.
     */
    private void keepAsNewRecipe() {
        index = -1;
        currentTab = -1;
        openedRecipe = null;
        dirty = true;
        pendingTab = Integer.MIN_VALUE;
    }

    /** Position of a recipe in a list, by equality. -1 when the list no longer carries it. */
    private static int indexOf(List<IRecipeInfo> recipes, IRecipeInfo recipe) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).equals(recipe)) {
                return i;
            }
        }
        return -1;
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

    /**
     * Multiplies or divides every recipe of the group, or of the two holders (D44).
     *
     * <p>Every recipe is rebuilt and checked first; nothing is written unless all of them pass.
     * Craft recipes are locked to one per cell, so they are skipped and counted.
     */
    public void scaleAll(int factor, boolean divide) {
        if (factor < RecipeScale.MIN_FACTOR || factor > RecipeScale.MAX_FACTOR) {
            return;
        }
        if (dirty) {
            // Not `unsaved`: that text says "click again to leave it", which fits a tab switch
            // and not this button. A second click here changes nothing.
            tell("gui.packagedautoterminals.scale_unsaved");
            return;
        }
        ProviderPairing.Group group = holders == null ? rescan() : null;
        List<IRecipeInfo> recipes = currentRecipes();
        if (recipes == null) {
            tell("gui.packagedautoterminals.machine_gone");
            return;
        }

        List<IRecipeInfo> from = new ArrayList<>();
        List<IRecipeInfo> to = new ArrayList<>();
        // The list as it will read afterwards, to catch two recipes that become one.
        List<IRecipeInfo> after = new ArrayList<>();
        int skipped = 0;
        World world = getPlayerInv().player.world;
        for (IRecipeInfo recipe : recipes) {
            IRecipeType type = recipe.getRecipeType();
            if (type == null || !type.canSetOutput()) {
                skipped++;
                after.add(recipe);
                continue;
            }
            IRecipeInfo scaled = scaled(recipe, factor, divide, world);
            if (scaled == null) {
                return;
            }
            from.add(recipe);
            to.add(scaled);
            after.add(scaled);
        }
        if (from.isEmpty()) {
            tell("gui.packagedautoterminals.scale_nothing");
            return;
        }
        int[] duplicate = RecipeScale.firstDuplicate(after);
        if (duplicate != null) {
            tell("gui.packagedautoterminals.scale_collision",
                    nameOf(recipes.get(duplicate[0])), nameOf(recipes.get(duplicate[1])));
            return;
        }

        int changed;
        if (holders != null) {
            changed = 0;
            for (ItemStack holder : insertedHolders()) {
                if (RecipeWriter.mapHolder(holder, from, to)) {
                    changed++;
                }
            }
            holders.markDirty();
        } else {
            IGrid grid = grid();
            if (grid == null || !allowed()) {
                return;
            }
            changed = RecipeWriter.mapMachines(grid, group.machines, from, to);
            rescan();
        }
        if (changed == 0) {
            tell("gui.packagedautoterminals.write_failed");
            return;
        }
        if (skipped > 0) {
            tell("gui.packagedautoterminals.recipes_scaled_skipped", from.size(), skipped);
        } else {
            tell(from.size() == 1
                            ? "gui.packagedautoterminals.recipes_scaled_one"
                            : "gui.packagedautoterminals.recipes_scaled",
                    from.size());
        }

        // The open tab follows its recipe to its scaled version.
        int opened = indexOf(from, openedRecipe);
        if (opened >= 0) {
            List<IRecipeInfo> now = currentRecipes();
            int target = now == null ? -1 : indexOf(now, to.get(opened));
            if (target >= 0) {
                load(target);
            }
        }
    }

    /**
     * One recipe, scaled, rebuilt through {@code generateFromStacks} like a recipe typed by
     * hand. Tells the player and answers {@code null} when it cannot be scaled.
     */
    private IRecipeInfo scaled(IRecipeInfo recipe, int factor, boolean divide, World world) {
        EditorInventory scratch = new EditorInventory(world, recipe.getRecipeType());
        scratch.load(recipe);
        scratch.beginGroup();
        try {
            for (int slot = 0; slot < EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS; slot++) {
                ItemStack stack = scratch.getStackInSlot(slot);
                if (stack.isEmpty() || !scratch.isEditable(slot)) {
                    continue;
                }
                long amount = RecipeScale.scale(amountOf(stack), factor, divide, maxAmountOf(stack));
                if (amount < 0) {
                    tell(divide
                                    ? "gui.packagedautoterminals.scale_not_exact"
                                    : "gui.packagedautoterminals.scale_too_large",
                            nameOf(recipe));
                    return null;
                }
                scratch.setInventorySlotContents(slot, withAmount(stack, (int) amount));
            }
        } finally {
            scratch.endGroup();
        }
        if (scratch.recipeInfo == null) {
            tell("gui.packagedautoterminals.scale_invalid", nameOf(recipe));
            return null;
        }
        return scratch.recipeInfo;
    }

    /** What a message calls a recipe: its first output. */
    private static String nameOf(IRecipeInfo recipe) {
        List<ItemStack> outputs = recipe.getOutputs();
        return outputs.isEmpty() ? "?" : outputs.get(0).getDisplayName();
    }

    /** Recipes of the tab row: the group, or the two holders. {@code null} when unknown. */
    private List<IRecipeInfo> currentRecipes() {
        if (holders != null) {
            return holderRecipes();
        }
        return scannedGroup == null ? null : scannedGroup.recipes;
    }

    /** Scans the network now, and keeps the target group for the tab row. */
    private ProviderPairing.Group rescan() {
        IGrid grid = grid();
        scannedGroup = grid == null ? null
                : ProviderPairing.groupOf(ProviderPairing.group(ProviderScanner.scan(grid)),
                        dimension, pos);
        return scannedGroup;
    }

    /** Closes the editor and reopens the terminal, on the same part. */
    public void backToTerminal() {
        // Holder mode has no terminal behind it: Back and Escape close the screen.
        if (terminal == null) {
            getPlayerInv().player.closeScreen();
            return;
        }
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
            editor.recipeType = nextEnabledType(editor.recipeType, forward);
        }
        editor.updateRecipeInfo();
        dirty = true;
    }

    static IRecipeType nextEnabledType(IRecipeType type, boolean forward) {
        IRecipeType next = type;
        do {
            next = RecipeTypeRegistry.getNextRecipeType(next, !forward);
        } while (isDisabled(next));
        return next;
    }

    /**
     * Same rule as {@code InventoryEncoderPattern.cycleRecipeType} of PackagedAuto: the config
     * {@code disabled_recipe_types} hides a type, except Processing.
     */
    public static boolean isDisabled(IRecipeType type) {
        return type != null && type != RecipeTypeProcessing.INSTANCE
                && TileEncoder.disabledRecipeTypes.contains(type.getName().toString());
    }

    /**
     * Type offered when a new recipe is opened.
     *
     * <p>Basic crafting comes first when it exists: that is the most common case. Processing
     * comes next.
     *
     * <p>FIXED: the fallback used to be the first registered type. The Package Crafter can be
     * disabled in the config, and MeatballCraft does it, so {@code crafting} is not registered.
     * The registry is sorted by name, and its first entry is then
     * {@code positioned_processing}, a rare type that every new recipe opened on.
     */
    public static IRecipeType defaultRecipeType() {
        NavigableMap<ResourceLocation, IRecipeType> registry = RecipeTypeRegistry.getRegistry();
        if (registry.isEmpty()) {
            return null;
        }
        for (String name : new String[] {"crafting", "processing"}) {
            IRecipeType type = registry.get(new ResourceLocation("packagedauto", name));
            if (type != null && !isDisabled(type)) {
                return type;
            }
        }
        return registry.firstEntry().getValue();
    }

    /**
     * Shift click only moves recipe holders, between the player inventory and the two holder
     * slots. Everything else stays still: the ghost slots take no real item.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (holders == null || index < firstHolderSlot || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlots.get(index);
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (index < firstPlayerSlot) {
            mergeItemStack(stack, firstPlayerSlot, inventorySlots.size(), true);
        } else if (stack.getItem() instanceof IRecipeListItem) {
            mergeItemStack(stack, firstHolderSlot, firstPlayerSlot, false);
        }
        slot.putStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
        return ItemStack.EMPTY;
    }

    /** Holder mode: both holders go back to the player, or drop at their feet. */
    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (holders != null && !player.world.isRemote) {
            clearContainer(player, player.world, holders);
        }
    }
}
