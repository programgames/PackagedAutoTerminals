package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.CrafterTypes;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.FluidPackets;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.network.PacketEditorSlot;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PacketRenameGroup;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.common.TerminalContext;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import thelm.packagedauto.api.IRecipeType;

/**
 * Recipe editor: creation and editing in one screen.
 *
 * <p>The layout follows the Package Recipe Encoder: recipe row on top, 9 by 9 grid on the
 * left, arrow, outputs and package preview on the right, inventory at the bottom.
 *
 * <p>One substantive difference: the top row does not show the slots of a recipe holder, but
 * **the recipes of the group**. The last slot, empty, creates one. Creating and editing
 * become the same gesture.
 *
 * <p>The sheet is 512 by 512: the screen exceeds 256 pixels in both directions.
 */
public class GuiPatEditor extends AEBaseGui {

    private static final int BUTTON_PREVIOUS_TYPE = 0;
    private static final int BUTTON_NEXT_TYPE = 1;
    private static final int BUTTON_SAVE = 2;
    private static final int BUTTON_BACK = 3;
    private static final int BUTTON_DELETE = 4;
    private static final int BUTTON_CLEAR = 5;
    private static final int BUTTON_TABS_PREVIOUS = 6;
    private static final int BUTTON_TABS_NEXT = 7;
    /** First id of the amount panel. Its buttons never reach {@code actionPerformed}. */
    private static final int BUTTON_PANEL = 100;

    private static final int SHEET = 512;

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_DIM = 0x808080;
    private static final int COLOR_WARNING = 0x803030;
    private static final int COLOR_OK = 0x2E7D32;
    /** Text of the input fields, light on their dark background. */
    private static final int COLOR_FIELD_TEXT = 0xE0E0E0;
    /** Veil drawn over the slots the recipe type does not enable. */
    private static final int COLOR_DISABLED = 0xA0303030;
    /** Frame of the open tab. */
    private static final int COLOR_SELECTED = 0xFF2E7D32;
    /** Dot drawn on the tab that holds unsaved work. */
    private static final int COLOR_UNSAVED = 0xFFCC3030;

    /**
     * Amount panel: size, in screen pixels.
     *
     * <p>It holds the item name, the six step buttons, the field and the two commands.
     */
    private static final int PANEL_WIDTH = 122;
    private static final int PANEL_HEIGHT = 126;
    /** Item icon of the title line. */
    private static final int PANEL_ICON_LEFT = 5;
    private static final int PANEL_ICON_TOP = 5;
    /** Tops of the four rows, inside the panel. */
    private static final int PANEL_PLUS_TOP = 25;
    private static final int PANEL_FIELD_TOP = 48;
    private static final int PANEL_MINUS_TOP = 64;
    private static final int PANEL_BOX_TOP = 88;
    private static final int PANEL_COMMAND_TOP = 101;
    /** Tick box of the proportions. */
    private static final int PANEL_BOX_SIZE = 10;
    /** A vanilla button is twenty pixels tall. Any other height reads as foreign. */
    private static final int PANEL_BUTTON_HEIGHT = 20;
    /** Number of buttons of the panel: six steps, then Set and Cancel. */
    private static final int PANEL_BUTTONS = 8;
    /**
     * Steps of the six buttons, and the factors they become while Shift is held.
     *
     * <p>Both tables are the ones of the Package Recipe Encoder, read in
     * {@code GuiItemAmountSpecifying.getIncrements} and {@code getMultipliers}.
     */
    private static final int[] AMOUNT_STEPS = {1, 10, 64};
    /**
     * Steps of a fluid slot, in millibuckets.
     *
     * <p>A fluid packet counts in millibuckets, and a recipe asks for a thousand of them at a
     * time. Steps of one, ten and sixty-four would make the player click all day.
     */
    private static final int[] AMOUNT_STEPS_FLUID = {10, 100, 1000};
    private static final int[] AMOUNT_FACTORS = {2, 3, 5};
    /**
     * Colours of the panel, taken from the vanilla bevel.
     *
     * <p>A screen of the game is a grey plate: black outline, white on the top and left
     * edges, dark grey on the bottom and right ones. The flat dark box that came before did
     * not belong to the game.
     */
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_OUTLINE = 0xFF000000;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_SHADOW = 0xFF555555;
    /** Tick box: the recess of a slot, and a green tick. */
    private static final int COLOR_BOX_EDGE = 0xFF373737;
    private static final int COLOR_BOX_FILL = 0xFF8B8B8B;
    private static final int COLOR_BOX_TICK = 0xFF2E7D32;

    private static final long MESSAGE_DURATION = 3_000L;
    private static final int MESSAGE_TOP = 20;
    private static final int MESSAGE_WIDTH = ContainerPatEditor.WIDTH - 16;

    /** Centre of the right column, to centre the type name and its icon. */
    private static final int RIGHT_CENTER = ContainerPatEditor.OUTPUT_LEFT + 27;

    /**
     * Crafting machine, below the type icon.
     *
     * <p>It sits in the free band between the type icon, which ends at 87, and the first
     * output row, which starts at {@code OUTPUT_TOP}.
     */
    private static final int MACHINE_LEFT = RIGHT_CENTER - 8;
    private static final int MACHINE_TOP = 90;

    /**
     * Draws the amount of a fluid in a slot, the way the AE2 fluid terminals do.
     *
     * <p>It is AE2 code, so the number reads the same here and in the rest of the network. It
     * also scales the text down on its own, and never overflows the sixteen pixels of a slot.
     */
    private final appeng.fluids.client.render.FluidStackSizeRenderer fluidSize =
            new appeng.fluids.client.render.FluidStackSizeRenderer();

    private final ContainerPatEditor editorContainer;
    private GuiButton saveButton;
    private GuiButton deleteButton;
    /** The two arrows of the tab row. They are greyed out when the row cannot move. */
    private GuiButton tabsPreviousButton;
    private GuiButton tabsNextButton;
    private GuiTextField nameField;
    /** Small amount panel, opened with the left click on a filled slot. */
    private GuiTextField amountField;
    private int amountSlot = -1;
    /** Item of the slot being edited. It gives the title of the panel. */
    private ItemStack amountStack = ItemStack.EMPTY;
    /** Upper bound accepted in the field. */
    private int amountMax = ContainerPatEditor.MAX_SLOT_COUNT;
    /** Steps of the six buttons: items or millibuckets, depending on the open slot. */
    private int[] amountSteps = AMOUNT_STEPS;
    /** Top left corner of the panel, in screen coordinates. */
    private int panelLeft;
    private int panelTop;
    /**
     * Keeps the proportions of the recipe when an amount changes.
     *
     * <p>The field is **static**: the choice survives the closing of the editor, and the
     * player does not tick the box again for every slot of the same recipe.
     */
    private static boolean keepRatio;
    /**
     * The eight buttons of the panel.
     *
     * <p>They are real {@link GuiButton} objects, for the vanilla look, but they stay out of
     * {@code buttonList}: the screen draws that list **before** the items of the slots, and
     * the panel would then sit under the grid. They are drawn by hand, at the end.
     */
    private final java.util.List<GuiButton> panelButtons = new java.util.ArrayList<>();

    private int lastFeedbackCount;
    private String message = "";
    private long messageExpiry;
    private boolean messageRefused;
    /** True when the line had to be trimmed: the tooltip then gives the whole text. */
    private boolean messageTrimmed;

    public GuiPatEditor(InventoryPlayer inventory, TerminalContext terminal,
                        EditorInventory editor, int dimension, BlockPos pos, int index) {
        super(new ContainerPatEditor(inventory, terminal, editor, dimension, pos, index));
        this.editorContainer = (ContainerPatEditor) inventorySlots;
        this.xSize = ContainerPatEditor.WIDTH;
        this.ySize = ContainerPatEditor.HEIGHT;
    }


    /** Keeps this screen inside a small window. See {@link GuiScaleFit}. */
    private final GuiScaleFit scaleFit = new GuiScaleFit();

    @Override
    public void onGuiClosed() {
        scaleFit.restore(mc);
        super.onGuiClosed();
    }

    @Override
    public void initGui() {
        // The scale must be settled before anything is placed: every widget below reads
        // `width` and `height`.
        if (scaleFit.apply(this, mc, xSize, ySize)) {
            return;
        }
        super.initGui();

        // A resize moves the whole screen. The panel carries absolute coordinates, so it
        // would stay behind, next to nothing.
        closeAmountField();

        String previous = nameField == null ? editorContainer.groupName : nameField.getText();
        // Dark background, light text: same reason as the terminal search field.
        nameField = new GuiTextField(0, fontRenderer,
                guiLeft + ContainerPatEditor.NAME_LEFT + 5,
                guiTop + ContainerPatEditor.NAME_TOP + 4,
                ContainerPatEditor.NAME_WIDTH - 10, 8);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(32);
        nameField.setTextColor(COLOR_FIELD_TEXT);
        nameField.setText(previous == null ? "" : previous);

        buttonList.clear();
        buttonList.add(new PatButton(BUTTON_BACK, guiLeft + 176, guiTop + 4, 74, 14,
                I18n.format("gui.packagedautoterminals.back")));

        tabsPreviousButton =
                new PatButton(BUTTON_TABS_PREVIOUS, guiLeft + 190, guiTop + 32, 14, 16, "<");
        tabsNextButton =
                new PatButton(BUTTON_TABS_NEXT, guiLeft + 208, guiTop + 32, 14, 16, ">");
        buttonList.add(tabsPreviousButton);
        buttonList.add(tabsNextButton);

        buttonList.add(new PatButton(BUTTON_PREVIOUS_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT, guiTop + 68, 10, 18, "<"));
        buttonList.add(new PatButton(BUTTON_NEXT_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT + 44, guiTop + 68, 10, 18, ">"));

        // The buttons start below the package preview, which reaches down to 226. They had
        // stayed at their old height when the screen grew by twenty pixels, and Save
        // overlapped the last preview row.
        int buttonTop = ContainerPatEditor.PREVIEW_TOP + 3 * 18 + 6;
        saveButton = new PatButton(BUTTON_SAVE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + buttonTop, 54, 16, I18n.format("gui.packagedautoterminals.save"));
        deleteButton = new PatButton(BUTTON_DELETE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + buttonTop + 18, 54, 16, I18n.format("gui.packagedautoterminals.delete"));
        buttonList.add(saveButton);
        buttonList.add(deleteButton);
        buttonList.add(new PatButton(BUTTON_CLEAR, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + buttonTop + 36, 54, 16, I18n.format("gui.packagedautoterminals.clear")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BUTTON_PREVIOUS_TYPE:
                send(PacketRecipeAction.ACTION_CYCLE_TYPE, 0);
                break;
            case BUTTON_NEXT_TYPE:
                send(PacketRecipeAction.ACTION_CYCLE_TYPE, 1);
                break;
            case BUTTON_TABS_PREVIOUS:
                send(PacketRecipeAction.ACTION_SCROLL_TABS, 0);
                break;
            case BUTTON_TABS_NEXT:
                send(PacketRecipeAction.ACTION_SCROLL_TABS, 1);
                break;
            case BUTTON_SAVE:
                // The name goes with the recipe: the player does not confirm twice.
                sendName();
                send(PacketRecipeAction.ACTION_SAVE, 0);
                break;
            case BUTTON_DELETE:
                send(PacketRecipeAction.ACTION_DELETE, 0);
                break;
            case BUTTON_CLEAR:
                send(PacketRecipeAction.ACTION_CLEAR, 0);
                break;
            case BUTTON_BACK:
                sendName();
                send(PacketRecipeAction.ACTION_BACK, 0);
                break;
            default:
                super.actionPerformed(button);
        }
    }

    private void send(byte action, int value) {
        PatNetwork.CHANNEL.sendToServer(new PacketRecipeAction(
                editorContainer.dimension, editorContainer.pos, value, action));
    }

    private void sendName() {
        if (nameField != null && !nameField.getText().equals(editorContainer.groupName)) {
            PatNetwork.CHANNEL.sendToServer(new PacketRenameGroup(nameField.getText()));
        }
    }

    /**
     * Wheel over a slot: adjusts its amount.
     *
     * <p>Detection happens here, not through the AE2 {@code mouseWheelEvent}: that method is
     * only called on a screen with a scrollbar, and the editor has none. The wheel therefore
     * did nothing.
     *
     * <p>Shift multiplies the step by ten, Ctrl by sixty-four. Processing recipes often need
     * whole stacks.
     */
    @Override
    public void handleMouseInput() throws IOException {
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        // While the panel is open, the wheel would change the slot behind the player back,
        // and the panel would keep showing the old amount.
        if (wheel != 0 && amountField == null) {
            int x = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int y = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
            Slot slot = slotUnder(x, y);
            if (slot != null) {
                if (amountLocked()) {
                    tellAmountLocked();
                    return;
                }
                int step = wheelStep(slot.getStack());
                PatNetwork.CHANNEL.sendToServer(
                        new PacketEditorSlot(slot.getSlotIndex(), wheel > 0 ? step : -step));
                return;
            }
        }
        super.handleMouseInput();
    }

    /**
     * Does this recipe type forbid any amount other than one?
     *
     * <p>A craft recipe consumes exactly one item per cell. Read in
     * {@code RecipeInfoCrafting.generateFromStacks}, and in the same method of every tier of
     * {@code PackagedExCrafting} and of {@code PackagedAvaritia}: each one calls
     * {@code setCount(1)} on every ingredient before it looks the recipe up. Worse, it calls it
     * on the **stacks themselves**, not on copies, so a typed amount was undone in the same
     * instant.
     *
     * <p>{@code canSetOutput()} separates the two families exactly, over the eleven types of the
     * pack: false for every craft type, true for every processing type. The test therefore names
     * no mod, and an unknown addon falls on the right side by itself.
     */
    private boolean amountLocked() {
        IRecipeType type = editorContainer.editor.recipeType;
        return type != null && !type.canSetOutput();
    }

    /** Says why the amount cannot move. The panel used to open, and to change nothing. */
    private void tellAmountLocked() {
        message = I18n.format("gui.packagedautoterminals.amount_locked");
        messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
        messageRefused = true;
        lastFeedbackCount = editorContainer.feedbackCount;
    }

    /**
     * Step of one wheel notch over this slot.
     *
     * <p>Items move by one, ten or sixty-four. A fluid packet counts in millibuckets, so the same
     * gesture moves by a hundred, a thousand or ten thousand. One millibucket per notch would take
     * a thousand notches to fill a bucket.
     */
    private int wheelStep(ItemStack stack) {
        if (FluidPackets.isPacket(stack)) {
            return isCtrlKeyDown() ? 10000 : (isShiftKeyDown() ? 1000 : 100);
        }
        return isCtrlKeyDown() ? 64 : (isShiftKeyDown() ? 10 : 1);
    }

    /**
     * Editable slot under these screen coordinates, or {@code null}.
     *
     * <p>The computation is done here rather than through the AE2 {@code getSlot}: we know
     * exactly which slots accept an amount.
     */
    private Slot slotUnder(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (!(slot instanceof SlotFake)
                    || !editorContainer.editor.isEditable(slot.getSlotIndex())) {
                continue;
            }
            int x = guiLeft + slot.xPos;
            int y = guiTop + slot.yPos;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        nameField.updateCursorCounter();
        if (amountField != null) {
            amountField.updateCursorCounter();
        }

        // The name comes from the server. We only overwrite it when the player is not
        // typing, otherwise every cycle would erase their input.
        if (!nameField.isFocused() && !nameField.getText().equals(editorContainer.groupName)) {
            nameField.setText(editorContainer.groupName == null ? "" : editorContainer.groupName);
        }
    }

    /**
     * Left click on a filled slot: the amount panel opens.
     *
     * <p>This is the gesture of the Package Recipe Encoder, read in
     * {@code GuiContainerTileBase.handleMouseClick}. That method opens its panel under five
     * conditions: left click, click type other than QUICK_MOVE, empty hand, ghost slot that
     * is enabled, and a slot that is not empty. {@link #slotUnder} already checks the ghost
     * slot and the enabling.
     *
     * <p>The left click therefore no longer empties the slot. The right click keeps that
     * task, and the amount zero does it too, exactly as in the Encoder.
     *
     * <p>The middle click still opens the same panel. The gesture existed before, and
     * removing it would break the habit of a player who uses it.
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (amountField != null) {
            amountPanelClick(mouseX, mouseY, mouseButton);
            return;
        }

        if ((mouseButton == 0 || mouseButton == 2) && !isShiftKeyDown()
                && mc.player.inventory.getItemStack().isEmpty()) {
            Slot slot = slotUnder(mouseX, mouseY);
            if (slot != null && !slot.getStack().isEmpty()) {
                if (amountLocked()) {
                    tellAmountLocked();
                    return;
                }
                openAmountField(slot);
                return;
            }
        }

        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Click while the panel is open. It always takes the click.
     *
     * <p>A click outside the panel cancels it, and reaches nothing else. One gesture, one
     * effect: the player never empties a slot while aiming at the Cancel button.
     */
    private void amountPanelClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && panelBox().contains(mouseX, mouseY)) {
            keepRatio = !keepRatio;
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
                    .getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }
        if (mouseButton == 0) {
            for (int index = 0; index < PANEL_BUTTONS; index++) {
                if (!panelButtons.get(index).mousePressed(mc, mouseX, mouseY)) {
                    continue;
                }
                panelButtons.get(index).playPressSound(mc.getSoundHandler());
                if (index == PANEL_BUTTONS - 2) {
                    applyAmountField();
                } else if (index == PANEL_BUTTONS - 1) {
                    closeAmountField();
                } else {
                    stepAmount(index);
                }
                return;
            }
        }
        if (amountField.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (mouseX < panelLeft || mouseX >= panelLeft + PANEL_WIDTH
                || mouseY < panelTop || mouseY >= panelTop + PANEL_HEIGHT) {
            closeAmountField();
        }
    }

    /**
     * Area of one button of the panel.
     *
     * <p>Indexes 0 to 2: the plus row. 3 to 5: the minus row. 6: Set. 7: Cancel.
     */
    private java.awt.Rectangle panelButton(int index) {
        if (index >= PANEL_BUTTONS - 2) {
            return new java.awt.Rectangle(panelLeft + 4 + (index - (PANEL_BUTTONS - 2)) * 58,
                    panelTop + PANEL_COMMAND_TOP, 56, PANEL_BUTTON_HEIGHT);
        }
        return new java.awt.Rectangle(panelLeft + 4 + (index % 3) * 38,
                panelTop + (index < 3 ? PANEL_PLUS_TOP : PANEL_MINUS_TOP),
                36, PANEL_BUTTON_HEIGHT);
    }

    /** Builds the eight buttons, once the panel knows where it sits. */
    private void buildPanelButtons() {
        panelButtons.clear();
        for (int index = 0; index < PANEL_BUTTONS; index++) {
            java.awt.Rectangle area = panelButton(index);
            panelButtons.add(new PatButton(BUTTON_PANEL + index,
                    area.x, area.y, area.width, area.height, ""));
        }
    }

    /**
     * Area of the tick box, and of its label.
     *
     * <p>The label belongs to the box: clicking the text ticks it too. A box of ten pixels
     * is a small target.
     */
    private java.awt.Rectangle panelBox() {
        return new java.awt.Rectangle(panelLeft + 5, panelTop + PANEL_BOX_TOP,
                PANEL_WIDTH - 10, PANEL_BOX_SIZE);
    }

    /** Label of one button. Shift turns the steps into factors. */
    private String panelLabel(int index, boolean factor) {
        if (index == PANEL_BUTTONS - 2) {
            return I18n.format("gui.packagedautoterminals.set");
        }
        if (index == PANEL_BUTTONS - 1) {
            return I18n.format("gui.packagedautoterminals.cancel");
        }
        boolean plus = index < 3;
        int column = index % 3;
        if (factor) {
            return (plus ? "x" : "/") + AMOUNT_FACTORS[column];
        }
        return (plus ? "+" : "-") + amountSteps[column];
    }

    /**
     * Applies one step to the field. Nothing travels to the server yet.
     *
     * <p>The computation runs on a {@code long}: a factor applied to the upper bound
     * overflows no int before the clamping.
     */
    private void stepAmount(int index) {
        long value = Math.max(0, parseAmount());
        boolean plus = index < 3;
        int column = index % 3;
        if (isShiftKeyDown()) {
            value = plus ? value * AMOUNT_FACTORS[column] : value / AMOUNT_FACTORS[column];
        } else {
            value = plus ? value + amountSteps[column] : value - amountSteps[column];
        }
        amountField.setText(String.valueOf(Math.max(0L, Math.min(amountMax, value))));
    }

    /**
     * Amount typed in the field, or -1 when the text is not a number.
     *
     * <p>The reading uses a {@code long}: a fluid field accepts ten digits, and
     * {@code Integer.parseInt} would throw on a number above two billion. The value is clamped
     * instead, so a typing slip never silently cancels the change.
     */
    private int parseAmount() {
        try {
            long value = Long.parseLong(amountField.getText().trim());
            return (int) Math.max(0L, Math.min(amountMax, value));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /**
     * Opens the amount panel over one slot.
     *
     * <p>A fluid packet is edited in millibuckets, up to a billion. Its item count always stays
     * at one, so reading the count would show "1" for a thousand millibuckets of water.
     * {@code ContainerPatEditor.amountOf} gives the right unit for both cases.
     */
    private void openAmountField(Slot slot) {
        amountSlot = slot.getSlotIndex();
        amountStack = slot.getStack().copy();
        boolean fluid = FluidPackets.isPacket(amountStack);
        amountMax = ContainerPatEditor.maxAmountOf(amountStack);
        amountSteps = fluid ? AMOUNT_STEPS_FLUID : AMOUNT_STEPS;

        // The panel opens above the slot, then slides back inside the screen. Without the
        // clamping, a slot of the last row pushed it off the bottom edge.
        panelLeft = Math.max(2, Math.min(width - PANEL_WIDTH - 2, guiLeft + slot.xPos - 8));
        panelTop = Math.max(2,
                Math.min(height - PANEL_HEIGHT - 2, guiTop + slot.yPos - PANEL_HEIGHT - 4));

        buildPanelButtons();
        amountField = new GuiTextField(1, fontRenderer,
                panelLeft + 6, panelTop + PANEL_FIELD_TOP, PANEL_WIDTH - 14, 12);
        amountField.setMaxStringLength(fluid ? 10 : 4);
        amountField.setTextColor(COLOR_FIELD_TEXT);
        amountField.setText(String.valueOf(ContainerPatEditor.amountOf(slot.getStack())));
        amountField.setFocused(true);
        amountField.setSelectionPos(0);
        nameField.setFocused(false);
    }

    /**
     * Rectangle of the amount panel, in screen coordinates, or {@code null} when it is closed.
     *
     * <p>JEI reads it through {@code PatGuiAreas}, so its tooltips stop covering the buttons.
     */
    public java.awt.Rectangle panelArea() {
        if (amountField == null) {
            return null;
        }
        return new java.awt.Rectangle(panelLeft - 1, panelTop - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2);
    }

    private void closeAmountField() {
        amountField = null;
        amountSlot = -1;
        amountStack = ItemStack.EMPTY;
        amountSteps = AMOUNT_STEPS;
        panelButtons.clear();
    }

    /**
     * Sends the typed amount.
     *
     * <p>Zero empties the slot, like the Encoder. A malformed text changes nothing, and
     * deserves no error message.
     */
    private void applyAmountField() {
        if (amountField == null) {
            return;
        }
        int amount = parseAmount();
        if (amount >= 0) {
            PatNetwork.CHANNEL.sendToServer(new PacketEditorSlot(
                    amountSlot, Math.min(amountMax, amount), true, keepRatio));
        }
        closeAmountField();
    }

    /**
     * Puts the OpenGL colour back to white, and makes sure the call really happens.
     *
     * <p>PITFALL, measured on a screenshot. The plate of the panel came out at **exactly 40%**
     * of its grey: 79 instead of 198. The dark text on it then became unreadable. The buttons,
     * which go through a texture, stayed correct.
     *
     * <p>Cause: {@code GlStateManager.color} caches the last colour it set, and skips the call
     * when the new one matches. Whatever ran before the panel left OpenGL on another colour
     * without going through that cache. Cache and OpenGL disagreed, so the call inside
     * {@code drawRect} was skipped, and every rectangle of the panel was multiplied by the stale
     * colour. The same trap already cost the chevron of the terminal, see the changelog of
     * 0.1.0-beta.1.
     *
     * <p>Setting a **different** colour first forces a real call, and puts the two back in step.
     * One call alone would be skipped again.
     */
    private static void resetColour() {
        net.minecraft.client.renderer.GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws the panel, over everything else.
     *
     * <p>Order matters: the plate, then the item, then the buttons and the field. The item
     * leaves the lighting of the item renderer behind it, so the lighting is put back off
     * before any text is drawn.
     */
    private void drawAmountPanel(int mouseX, int mouseY, float partialTicks) {
        if (amountField == null) {
            return;
        }
        resetColour();
        drawPanelPlate();

        if (!amountStack.isEmpty()) {
            drawItem(panelLeft + PANEL_ICON_LEFT, panelTop + PANEL_ICON_TOP, amountStack);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            resetColour();

            String title = fontRenderer.trimStringToWidth(
                    amountStack.getDisplayName(), PANEL_WIDTH - 30);
            fontRenderer.drawString(title, panelLeft + PANEL_ICON_LEFT + 20,
                    panelTop + PANEL_ICON_TOP + 4, COLOR_TEXT);
        }

        // The item renderer ran in between, so the colour needs the same repair again.
        resetColour();
        drawPanelBox(mouseX, mouseY);

        boolean factor = isShiftKeyDown();
        for (int index = 0; index < PANEL_BUTTONS; index++) {
            GuiButton button = panelButtons.get(index);
            button.displayString = panelLabel(index, factor);
            button.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        amountField.drawTextBox();
    }

    /**
     * The grey plate of the panel, with the vanilla bevel.
     *
     * <p>Black outline, white on the top and left edges, dark grey on the bottom and right
     * ones. That is the relief of every screen of the game.
     */
    private void drawPanelPlate() {
        int right = panelLeft + PANEL_WIDTH;
        int bottom = panelTop + PANEL_HEIGHT;
        drawRect(panelLeft - 1, panelTop - 1, right + 1, bottom + 1, COLOR_PANEL_OUTLINE);
        drawRect(panelLeft, panelTop, right, bottom, COLOR_PANEL);
        drawRect(panelLeft, panelTop, right - 1, panelTop + 1, COLOR_PANEL_LIGHT);
        drawRect(panelLeft, panelTop, panelLeft + 1, bottom - 1, COLOR_PANEL_LIGHT);
        drawRect(panelLeft + 1, bottom - 1, right, bottom, COLOR_PANEL_SHADOW);
        drawRect(right - 1, panelTop + 1, right, bottom, COLOR_PANEL_SHADOW);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (amountField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                applyAmountField();
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeAmountField();
                return;
            }
            // Only digits and editing keys make sense here.
            if (Character.isDigit(typedChar) || keyCode == Keyboard.KEY_BACK
                    || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_LEFT
                    || keyCode == Keyboard.KEY_RIGHT) {
                amountField.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        if (nameField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                sendName();
                nameField.setFocused(false);
                return;
            }
            // Escape closes the screen, even from the field: without this exception, the
            // player would stay trapped in the editor.
            if (keyCode != Keyboard.KEY_ESCAPE && nameField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }

        // E2: Enter saves. The player no longer has to aim at a button after every change.
        // The button stays the obvious path; the key is the shortcut.
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (saveButton != null && saveButton.enabled) {
                actionPerformed(saveButton);
            }
            return;
        }

        // E2: Escape goes back to the terminal, instead of closing everything. A second
        // Escape then closes the terminal. A player editing several recipes no longer starts
        // over from the world every time.
        if (keyCode == Keyboard.KEY_ESCAPE) {
            send(PacketRecipeAction.ACTION_BACK, 0);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /**
     * The field is drawn here, outside the shifted frame of {@code drawFG}, because it
     * carries absolute coordinates. Same pitfall as in the terminal.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (messageTrimmed && System.currentTimeMillis() <= messageExpiry
                && mouseX >= guiLeft + 8 && mouseX <= guiLeft + 8 + MESSAGE_WIDTH
                && mouseY >= guiTop + MESSAGE_TOP && mouseY <= guiTop + MESSAGE_TOP + 8) {
            drawHoveringText(java.util.Collections.singletonList(message), mouseX, mouseY);
        }
        drawMachineTooltip(mouseX, mouseY);
        nameField.drawTextBox();
        drawAmountPanel(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        bindTexture(Reference.MOD_ID, "guis/pat_editor.png");
        drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, xSize, ySize, SHEET, SHEET);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        EditorInventory editor = editorContainer.editor;

        if (nameField != null && nameField.getText().isEmpty() && !nameField.isFocused()) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.name_hint"),
                    ContainerPatEditor.NAME_LEFT + 5, ContainerPatEditor.NAME_TOP + 4, 0x707070);
        }

        drawMessage(editor);
        drawSelectedTab();
        drawRecipeType(editor.recipeType);

        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.inventory"),
                8, ContainerPatEditor.PLAYER_INVENTORY_TOP - 11, COLOR_TEXT);

        drawDisabledSlots(editor);

        if (saveButton != null) {
            saveButton.enabled = editor.recipeInfo != null;
        }
        if (deleteButton != null) {
            deleteButton.enabled = editorContainer.currentTab >= 0;
        }
        // A dead arrow used to look broken. The player pressed the left one, nothing moved, and
        // they reported a bug. The arrow now says so itself.
        if (tabsPreviousButton != null) {
            tabsPreviousButton.enabled = editorContainer.tabOffset > 0;
        }
        if (tabsNextButton != null) {
            tabsNextButton.enabled = editorContainer.tabOffset < editorContainer.maxTabOffset();
        }
    }

    /** Server message, or recipe state, on the line below the title. */
    private void drawMessage(EditorInventory editor) {
        if (editorContainer.feedbackCount != lastFeedbackCount) {
            lastFeedbackCount = editorContainer.feedbackCount;
            message = I18n.format(Feedback.key(editorContainer.feedback),
                    Feedback.arguments(editorContainer.feedback));
            messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
            // One list, in Feedback, for both screens. Each used to carry its own set of
            // substrings, and they had already drifted apart.
            messageRefused = Feedback.isRefusal(editorContainer.feedback);
        }

        if (!message.isEmpty() && System.currentTimeMillis() <= messageExpiry) {
            fontRenderer.drawString(fitMessage(message), 8, MESSAGE_TOP,
                    messageRefused ? COLOR_WARNING : COLOR_OK);
            return;
        }
        messageTrimmed = false;
        if (editor.recipeInfo == null) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.invalid"),
                    8, 20, COLOR_WARNING);
        }
    }

    /**
     * Trims the line when it overflows the frame.
     *
     * <p>The message line has a single row: the tab row starts right below. A translation
     * that was too long therefore spilled out of the screen, across the background. The whole
     * text stays readable in the tooltip.
     */
    private String fitMessage(String text) {
        if (fontRenderer.getStringWidth(text) <= MESSAGE_WIDTH) {
            messageTrimmed = false;
            return text;
        }
        messageTrimmed = true;
        return fontRenderer.trimStringToWidth(text, MESSAGE_WIDTH - 6) + "...";
    }

    /**
     * Draws one slot. A fluid packet gets its amount, not its item count.
     *
     * <p>FIXED, reported by a player: a slot that held four thousand millibuckets of water still
     * showed a small **1** in its corner. That 1 is the item count of the packet, and the count
     * of a packet never leaves one, by the rule of AE2 Fluid Crafting.
     *
     * <p>Read in {@code AEBaseGui.drawSlot}: AE2 draws the item, then calls
     * {@code StackSizeRenderer.renderStackSize} with the item count. No hook changes that number,
     * so this method takes the slot over for a fluid packet: it draws the item, then the amount,
     * through the AE2 {@code FluidStackSizeRenderer}.
     *
     * <p>That renderer counts in **buckets**, like every AE2 fluid screen: 4000 mB reads "4", and
     * 250 mB reads "0.25". It shrinks its own text, so the number never overflows the slot.
     * Writing "4000" there would have covered the whole square.
     */
    @Override
    public void drawSlot(Slot slot) {
        net.minecraftforge.fluids.FluidStack fluid = FluidPackets.fluidOf(slot.getStack());
        if (fluid == null) {
            super.drawSlot(slot);
            return;
        }
        drawItem(slot.xPos, slot.yPos, slot.getStack());
        fluidSize.renderStackSize(fontRenderer,
                appeng.fluids.util.AEFluidStack.fromFluidStack(fluid), slot.xPos, slot.yPos);
    }

    /**
     * Frames the open tab. The creation tab follows the last recipe.
     *
     * <p>FIXED: this method placed the frame on a **single** row, with
     * {@code TAB_LEFT + slot * 18}. The row has held ten columns over two rows for a while. Any
     * tab past the tenth therefore drew its green frame far to the right, outside the screen.
     * The placement now matches the one of {@code bindEditorSlots}.
     */
    private void drawSelectedTab() {
        int slot = editorContainer.currentTab < 0
                ? editorContainer.recipeCount - editorContainer.tabOffset
                : editorContainer.currentTab - editorContainer.tabOffset;
        if (slot < 0 || slot >= ContainerPatEditor.TAB_COUNT) {
            return;
        }
        int x = ContainerPatEditor.TAB_LEFT + (slot % ContainerPatEditor.TAB_COLUMNS) * 18;
        int y = ContainerPatEditor.TAB_TOP + (slot / ContainerPatEditor.TAB_COLUMNS) * 18;
        drawRect(x - 1, y - 1, x + 17, y, COLOR_SELECTED);
        drawRect(x - 1, y + 16, x + 17, y + 17, COLOR_SELECTED);
        drawRect(x - 1, y, x, y + 16, COLOR_SELECTED);
        drawRect(x + 16, y, x + 17, y + 16, COLOR_SELECTED);

        // E5: a red dot says "this work is not saved". It is drawn in the corner, over three
        // pixels, so it does not hide the produced item.
        if (editorContainer.dirty) {
            drawRect(x + 12, y + 1, x + 16, y + 5, COLOR_UNSAVED);
        }
    }

    /** Type name, centred, and its icon, the way the Encoder does it. */
    private void drawRecipeType(IRecipeType type) {
        String name = type == null
                ? I18n.format("gui.packagedautoterminals.no_type")
                : type.getLocalizedNameShort();
        fontRenderer.drawString(name, RIGHT_CENTER - fontRenderer.getStringWidth(name) / 2,
                58, COLOR_DIM);

        if (type == null) {
            return;
        }
        Object representation = type.getRepresentation();
        if (representation instanceof ItemStack) {
            drawItem(RIGHT_CENTER - 8, 69, (ItemStack) representation);
        }

        // The source station above says where the recipe comes from. This one says who will
        // run it on the network. They differ, and the player needs both: an Elite recipe is
        // indeed made on an Extended Crafting table, but the Elite Package Crafter is the
        // block that must be placed.
        ItemStack machine = CrafterTypes.iconFor(type);
        if (!machine.isEmpty()) {
            drawItem(MACHINE_LEFT, MACHINE_TOP, machine);
        }
    }

    /** Tooltip of the crafting machine. Absolute coordinates. */
    private void drawMachineTooltip(int mouseX, int mouseY) {
        ItemStack machine = CrafterTypes.iconFor(editorContainer.editor.recipeType);
        if (machine.isEmpty()) {
            return;
        }
        if (mouseX < guiLeft + MACHINE_LEFT || mouseX >= guiLeft + MACHINE_LEFT + 16
                || mouseY < guiTop + MACHINE_TOP || mouseY >= guiTop + MACHINE_TOP + 16) {
            return;
        }
        drawHoveringText(java.util.Arrays.asList(
                machine.getDisplayName(),
                net.minecraft.util.text.TextFormatting.GRAY
                        + I18n.format("gui.packagedautoterminals.machine_needed")),
                mouseX, mouseY);
    }

    /**
     * Editable ghost slots, and the area each one covers on screen.
     *
     * <p>The drag and drop from JEI needs it. The geometry is read from the slots themselves,
     * and never recomputed from the layout constants: the two can therefore never drift
     * apart.
     *
     * <p>Only {@link SlotFake} slots are offered. The tabs and the package preview are
     * {@code AppEngSlot} slots, so they stay out on their own, with no test of their own.
     *
     * @return slot index of {@link EditorInventory}, to its area in screen coordinates.
     */
    public java.util.Map<Integer, java.awt.Rectangle> editableSlotAreas() {
        java.util.Map<Integer, java.awt.Rectangle> areas = new java.util.LinkedHashMap<>();
        EditorInventory editor = editorContainer.editor;
        for (Slot slot : editorContainer.inventorySlots) {
            if (!(slot instanceof SlotFake) || !editor.isEditable(slot.getSlotIndex())) {
                continue;
            }
            areas.put(slot.getSlotIndex(),
                    new java.awt.Rectangle(guiLeft + slot.xPos, guiTop + slot.yPos, 16, 16));
        }
        return areas;
    }

    /**
     * Tick box of the proportions.
     *
     * <p>The tick is a filled square inside the box, and not a character: the game font has
     * no reliable tick mark.
     */
    private void drawPanelBox(int mouseX, int mouseY) {
        java.awt.Rectangle row = panelBox();
        int left = row.x;
        int right = left + PANEL_BOX_SIZE;
        boolean hover = row.contains(mouseX, mouseY);

        // PITFALL fixed: the tick used COLOR_FIELD_TEXT, which carries **no** alpha byte.
        // `drawRect` then painted it fully transparent, and the box looked dead. Every
        // colour of a `drawRect` must start with FF.
        drawRect(left, row.y, right, row.y + PANEL_BOX_SIZE, COLOR_BOX_EDGE);
        drawRect(left + 1, row.y + 1, right - 1, row.y + PANEL_BOX_SIZE - 1, COLOR_BOX_FILL);
        if (keepRatio) {
            drawRect(left + 2, row.y + 2, right - 2, row.y + PANEL_BOX_SIZE - 2,
                    COLOR_BOX_TICK);
        }

        String label = fontRenderer.trimStringToWidth(
                I18n.format("gui.packagedautoterminals.keep_ratio"), PANEL_WIDTH - 26);
        fontRenderer.drawString(label, right + 5, row.y + 1,
                hover ? COLOR_OK : COLOR_TEXT);
    }

    /**
     * Veil over every slot the type does not enable. The server already refuses them; the
     * veil saves the player from trying.
     *
     * <p>FIXED: the nine output slots of a craft recipe are not editable, and the veil turned
     * them into a black square. They are not a forbidden zone; they **show the crafted result**,
     * which {@code EditorInventory.updateRecipeInfo} now writes there. The veil skips them, as
     * the Encoder does.
     */
    private void drawDisabledSlots(EditorInventory editor) {
        boolean readOnlyOutput =
                editor.recipeType != null && !editor.recipeType.canSetOutput();
        for (int slot = 0; slot < EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS; slot++) {
            if (editor.isEditable(slot)) {
                continue;
            }
            if (readOnlyOutput && slot >= EditorInventory.INPUT_SLOTS) {
                continue;
            }
            int x;
            int y;
            if (slot < EditorInventory.INPUT_SLOTS) {
                x = ContainerPatEditor.GRID_LEFT + (slot % 9) * 18;
                y = ContainerPatEditor.GRID_TOP + (slot / 9) * 18;
            } else {
                int output = slot - EditorInventory.INPUT_SLOTS;
                x = ContainerPatEditor.OUTPUT_LEFT + (output % 3) * 18;
                y = ContainerPatEditor.OUTPUT_TOP + (output / 3) * 18;
            }
            drawRect(x, y, x + 16, y + 16, COLOR_DISABLED);
        }
    }
}
