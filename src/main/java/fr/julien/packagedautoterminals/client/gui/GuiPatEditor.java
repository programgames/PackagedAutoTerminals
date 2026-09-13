package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import fr.julien.packagedautoterminals.Reference;
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
 * Éditeur de recette : création et modification réunies.
 *
 * <p>La disposition reprend celle du Package Recipe Encoder : rangée de recettes en haut,
 * grille 9 sur 9 à gauche, flèche, sorties et aperçu des colis à droite, inventaire en bas.
 *
 * <p>Une seule différence de fond : la rangée du haut ne montre pas les emplacements d'un
 * porte-recettes, mais **les recettes du groupe**. La dernière case, vide, en crée une.
 * Créer et modifier deviennent le même geste.
 *
 * <p>La planche fait 512 sur 512 : la fenêtre dépasse 256 pixels dans les deux sens.
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
    /** Voile posé sur les emplacements que le type de recette n'active pas. */
    private static final int COLOR_DISABLED = 0xA0303030;
    /** Cadre de l'onglet ouvert. */
    private static final int COLOR_SELECTED = 0xFF2E7D32;

    private static final long MESSAGE_DURATION = 3_000L;

    /** Centre de la colonne de droite, pour centrer le nom du type et son icône. */
    private static final int RIGHT_CENTER = ContainerPatEditor.OUTPUT_LEFT + 27;

    private final ContainerPatEditor editorContainer;
    private GuiButton saveButton;
    private GuiButton deleteButton;
    private GuiTextField nameField;
    /** Petite boîte de saisie de quantité, ouverte au clic du milieu. */
    private GuiTextField amountField;
    private int amountSlot = -1;

    private int lastFeedbackCount;
    private String message = "";
    private long messageExpiry;
    private boolean messageRefused;

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
        nameField = new GuiTextField(0, fontRenderer,
                guiLeft + ContainerPatEditor.NAME_LEFT + 4,
                guiTop + ContainerPatEditor.NAME_TOP + 4,
                ContainerPatEditor.NAME_WIDTH - 8, 10);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(32);
        nameField.setTextColor(COLOR_TEXT);
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

        saveButton = new GuiButton(BUTTON_SAVE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + 208, 54, 16, I18n.format("gui.packagedautoterminals.save"));
        deleteButton = new GuiButton(BUTTON_DELETE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + 226, 54, 16, I18n.format("gui.packagedautoterminals.delete"));
        buttonList.add(saveButton);
        buttonList.add(deleteButton);
        buttonList.add(new GuiButton(BUTTON_CLEAR, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + 244, 54, 16, I18n.format("gui.packagedautoterminals.clear")));
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
                // Le nom part avec la recette : le joueur n'a pas à valider deux fois.
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
     * Molette sur un emplacement : ajuste sa quantité.
     *
     * <p>La détection se fait ici, et non par {@code mouseWheelEvent} d'AE2 : cette méthode
     * n'est appelée que sur une fenêtre pourvue d'un ascenseur, et l'éditeur n'en a pas. La
     * molette ne faisait donc rien.
     *
     * <p>Maj multiplie le pas par dix, Ctrl par soixante-quatre. Les recettes de traitement
     * demandent souvent des piles entières.
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
     * Emplacement modifiable sous ces coordonnées d'écran, ou {@code null}.
     *
     * <p>Le calcul est fait ici plutôt que par {@code getSlot} d'AE2 : nous savons exactement
     * quels emplacements acceptent une quantité.
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

        // Le nom vient du serveur. On ne l'écrase que si le joueur n'est pas en train de
        // l'écrire, sans quoi chaque cycle effacerait sa saisie.
        if (!nameField.isFocused() && !nameField.getText().equals(editorContainer.groupName)) {
            nameField.setText(editorContainer.groupName == null ? "" : editorContainer.groupName);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Clic du milieu sur une case remplie : saisir la quantité au clavier. La molette
        // reste là pour les ajustements rapides.
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
            // Une saisie vide ou fautive ne change rien, et ne mérite pas d'erreur.
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
            // Seuls les chiffres et l'effacement ont un sens ici.
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
            // Échap ferme la fenêtre, même depuis le champ : sans cette exception, le joueur
            // resterait piégé dans l'éditeur.
            if (keyCode != Keyboard.KEY_ESCAPE && nameField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    /**
     * Le champ se dessine ici, hors du repère décalé de {@code drawFG}, car il porte des
     * coordonnées absolues. Même piège que dans le terminal.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        nameField.drawTextBox();
        if (amountField != null) {
            // Un fond plein derrière la boîte : posée sur la grille, elle serait illisible.
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
                    ContainerPatEditor.NAME_LEFT + 4, ContainerPatEditor.NAME_TOP + 4, COLOR_DIM);
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

    /** Message du serveur, ou état de la recette, sur la ligne sous le titre. */
    private void drawMessage(EditorInventory editor) {
        if (editorContainer.feedbackCount != lastFeedbackCount) {
            lastFeedbackCount = editorContainer.feedbackCount;
            message = I18n.format(Feedback.key(editorContainer.feedback),
                    Feedback.arguments(editorContainer.feedback));
            messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
            // Un refus se reconnaît à sa clé : rien à traduire pour le savoir.
            messageRefused = editorContainer.feedback.contains("no_")
                    || editorContainer.feedback.contains("unsaved")
                    || editorContainer.feedback.contains("failed")
                    || editorContainer.feedback.contains("nothing");
        }

        if (!message.isEmpty() && System.currentTimeMillis() <= messageExpiry) {
            fontRenderer.drawString(message, 8, 20, messageRefused ? COLOR_WARNING : COLOR_OK);
            return;
        }
        if (editor.recipeInfo == null) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.invalid"),
                    8, 20, COLOR_WARNING);
        }
    }

    /** Encadre l'onglet ouvert. L'onglet de création suit la dernière recette. */
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
    }

    /** Nom du type, centré, et son icône, comme le fait l'Encoder. */
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
    }

    /**
     * Voile sur tout emplacement que le type n'active pas. Le serveur les refuse déjà ; le
     * voile évite au joueur d'essayer.
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
