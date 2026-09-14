package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.CrafterTypes;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.common.Feedback;
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

    private final ContainerPatEditor editorContainer;
    private GuiButton saveButton;
    private GuiButton deleteButton;
    private GuiTextField nameField;
    /** Small amount input box, opened with the middle click. */
    private GuiTextField amountField;
    private int amountSlot = -1;

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

    @Override
    public void initGui() {
        super.initGui();

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
        buttonList.add(new GuiButton(BUTTON_BACK, guiLeft + 176, guiTop + 4, 74, 14,
                I18n.format("gui.packagedautoterminals.back")));

        buttonList.add(new GuiButton(BUTTON_TABS_PREVIOUS, guiLeft + 190, guiTop + 32, 14, 16, "<"));
        buttonList.add(new GuiButton(BUTTON_TABS_NEXT, guiLeft + 208, guiTop + 32, 14, 16, ">"));

        buttonList.add(new GuiButton(BUTTON_PREVIOUS_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT, guiTop + 68, 10, 18, "<"));
        buttonList.add(new GuiButton(BUTTON_NEXT_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT + 44, guiTop + 68, 10, 18, ">"));

        // The buttons start below the package preview, which reaches down to 226. They had
        // stayed at their old height when the screen grew by twenty pixels, and Save
        // overlapped the last preview row.
        int buttonTop = ContainerPatEditor.PREVIEW_TOP + 3 * 18 + 6;
        saveButton = new GuiButton(BUTTON_SAVE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + buttonTop, 54, 16, I18n.format("gui.packagedautoterminals.save"));
        deleteButton = new GuiButton(BUTTON_DELETE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + buttonTop + 18, 54, 16, I18n.format("gui.packagedautoterminals.delete"));
        buttonList.add(saveButton);
        buttonList.add(deleteButton);
        buttonList.add(new GuiButton(BUTTON_CLEAR, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
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
        if (wheel != 0) {
            int x = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int y = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
            Slot slot = slotUnder(x, y);
            if (slot != null) {
                int step = isCtrlKeyDown() ? 64 : (isShiftKeyDown() ? 10 : 1);
                PatNetwork.CHANNEL.sendToServer(
                        new PacketEditorSlot(slot.getSlotIndex(), wheel > 0 ? step : -step));
                return;
            }
        }
        super.handleMouseInput();
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Middle click on a filled slot: type the amount on the keyboard. The wheel stays
        // available for quick adjustments.
        if (mouseButton == 2) {
            Slot slot = slotUnder(mouseX, mouseY);
            if (slot != null && !slot.getStack().isEmpty()) {
                openAmountField(slot);
                return;
            }
        }
        if (amountField != null) {
            closeAmountField();
        }

        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void openAmountField(Slot slot) {
        amountSlot = slot.getSlotIndex();
        amountField = new GuiTextField(1, fontRenderer,
                guiLeft + slot.xPos - 2, guiTop + slot.yPos - 12, 40, 11);
        amountField.setMaxStringLength(4);
        amountField.setText(String.valueOf(slot.getStack().getCount()));
        amountField.setFocused(true);
        amountField.setSelectionPos(0);
        nameField.setFocused(false);
    }

    private void closeAmountField() {
        amountField = null;
        amountSlot = -1;
    }

    private void applyAmountField() {
        if (amountField == null) {
            return;
        }
        try {
            int amount = Integer.parseInt(amountField.getText().trim());
            PatNetwork.CHANNEL.sendToServer(new PacketEditorSlot(amountSlot, amount, true));
        } catch (NumberFormatException ignored) {
            // Empty or malformed input changes nothing, and deserves no error.
        }
        closeAmountField();
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
        if (amountField != null) {
            // A solid background behind the box: over the grid it would be unreadable.
            drawRect(amountField.x - 2, amountField.y - 2,
                    amountField.x + amountField.width + 2, amountField.y + 12, 0xFF202020);
            amountField.drawTextBox();
        }
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
    }

    /** Server message, or recipe state, on the line below the title. */
    private void drawMessage(EditorInventory editor) {
        if (editorContainer.feedbackCount != lastFeedbackCount) {
            lastFeedbackCount = editorContainer.feedbackCount;
            message = I18n.format(Feedback.key(editorContainer.feedback),
                    Feedback.arguments(editorContainer.feedback));
            messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
            // A refusal is recognised by its key: no translation needed to tell.
            messageRefused = editorContainer.feedback.contains("no_")
                    || editorContainer.feedback.contains("unsaved")
                    || editorContainer.feedback.contains("failed")
                    || editorContainer.feedback.contains("nothing");
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

    /** Frames the open tab. The creation tab follows the last recipe. */
    private void drawSelectedTab() {
        int slot = editorContainer.currentTab < 0
                ? editorContainer.recipeCount - editorContainer.tabOffset
                : editorContainer.currentTab - editorContainer.tabOffset;
        if (slot < 0 || slot >= ContainerPatEditor.TAB_COUNT) {
            return;
        }
        int x = ContainerPatEditor.TAB_LEFT + slot * 18;
        int y = ContainerPatEditor.TAB_TOP;
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
     * Veil over every slot the type does not enable. The server already refuses them; the
     * veil saves the player from trying.
     */
    private void drawDisabledSlots(EditorInventory editor) {
        for (int slot = 0; slot < EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS; slot++) {
            if (editor.isEditable(slot)) {
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
