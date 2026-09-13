package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;

import appeng.client.gui.AEBaseGui;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.network.PacketEditorSlot;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PacketRenameGroup;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import appeng.container.slot.SlotFake;
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
 * Éditeur d'une recette.
 *
 * <p>La mise en page reprend celle du Package Recipe Encoder : grille 9 sur 9 à gauche,
 * flèche, sorties en haut à droite, aperçu des colis en dessous, inventaire en bas. La
 * rangée des dix emplacements de motifs disparaît : le terminal édite une recette à la
 * fois.
 *
 * <p>La planche mesure 256 sur 512, car la fenêtre dépasse les 256 pixels de haut que
 * suppose {@code drawTexturedModalRect}.
 */
public class GuiPatEditor extends AEBaseGui {

    private static final int BUTTON_PREVIOUS_TYPE = 0;
    private static final int BUTTON_NEXT_TYPE = 1;
    private static final int BUTTON_SAVE = 2;
    private static final int BUTTON_BACK = 3;

    private static final int SHEET_WIDTH = 256;
    private static final int SHEET_HEIGHT = 512;

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_DIM = 0x808080;
    private static final int COLOR_WARNING = 0x803030;
    /** Voile posé sur les emplacements que le type de recette n'active pas. */
    private static final int COLOR_DISABLED = 0xA0303030;

    /** Centre de la colonne de droite, pour centrer le nom du type et son icône. */
    private static final int RIGHT_CENTER = ContainerPatEditor.OUTPUT_LEFT + 27;

    private final ContainerPatEditor editorContainer;
    private GuiButton saveButton;
    private GuiTextField nameField;

    public GuiPatEditor(InventoryPlayer inventory, PartPatTerminal terminal,
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
        nameField = new GuiTextField(0, fontRenderer, guiLeft + 10, guiTop + 6, 172, 10);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(32);
        nameField.setTextColor(COLOR_TEXT);
        nameField.setText(previous == null ? "" : previous);

        buttonList.clear();
        saveButton = new GuiButton(BUTTON_SAVE, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + 20, 54, 18, I18n.format("gui.packagedautoterminals.save"));
        buttonList.add(saveButton);
        buttonList.add(new GuiButton(BUTTON_PREVIOUS_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT - 2, guiTop + 54, 10, 18, "<"));
        buttonList.add(new GuiButton(BUTTON_NEXT_TYPE,
                guiLeft + ContainerPatEditor.OUTPUT_LEFT + 44, guiTop + 54, 10, 18, ">"));
        buttonList.add(new GuiButton(BUTTON_BACK, guiLeft + ContainerPatEditor.OUTPUT_LEFT,
                guiTop + 2, 54, 16, I18n.format("gui.packagedautoterminals.back")));
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
            case BUTTON_SAVE:
                // Le nom part avec la recette : le joueur n'a pas à valider deux fois.
                sendName();
                send(PacketRecipeAction.ACTION_SAVE, 0);
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

    /**
     * Molette sur un emplacement : ajuste sa quantité.
     *
     * <p>Maj multiplie le pas par dix, Ctrl par soixante-quatre. Les recettes de traitement
     * demandent souvent des piles entières.
     */
    @Override
    protected void mouseWheelEvent(int x, int y, int wheel) {
        Slot slot = getSlot(x, y);
        if (slot instanceof SlotFake && editorContainer.editor.isEditable(slot.getSlotIndex())) {
            int step = isCtrlKeyDown() ? 64 : (isShiftKeyDown() ? 10 : 1);
            PatNetwork.CHANNEL.sendToServer(
                    new PacketEditorSlot(slot.getSlotIndex(), wheel > 0 ? step : -step));
            return;
        }
        super.mouseWheelEvent(x, y, wheel);
    }

    private void sendName() {
        if (nameField != null && !nameField.getText().equals(editorContainer.groupName)) {
            PatNetwork.CHANNEL.sendToServer(new PacketRenameGroup(nameField.getText()));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        nameField.updateCursorCounter();

        // Le nom vient du serveur. On ne l'écrase que si le joueur n'est pas en train de
        // l'écrire, sans quoi chaque cycle effacerait sa saisie.
        if (!nameField.isFocused() && !nameField.getText().equals(editorContainer.groupName)) {
            nameField.setText(editorContainer.groupName == null ? "" : editorContainer.groupName);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
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
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        bindTexture(Reference.MOD_ID, "guis/pat_editor.png");
        drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, xSize, ySize,
                SHEET_WIDTH, SHEET_HEIGHT);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        EditorInventory editor = editorContainer.editor;

        // Le champ de nom occupe la ligne de titre. Vide, il annonce ce qu'il attend.
        if (nameField != null && nameField.getText().isEmpty() && !nameField.isFocused()) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.name_hint"),
                    10, 6, COLOR_DIM);
        }

        // Un avertissement en haut à droite, là où rien d'autre ne s'affiche. Il ne peut
        // donc chevaucher aucun emplacement.
        if (editor.recipeInfo == null) {
            String warning = I18n.format("gui.packagedautoterminals.invalid");
            fontRenderer.drawString(warning, xSize - 8 - fontRenderer.getStringWidth(warning),
                    6, COLOR_WARNING);
        }

        drawRecipeType(editor.recipeType);

        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.inventory"),
                8, ContainerPatEditor.PLAYER_INVENTORY_TOP - 11, COLOR_TEXT);

        drawDisabledSlots(editor);

        if (saveButton != null) {
            saveButton.enabled = editor.recipeInfo != null;
        }
    }

    /** Nom du type, centré, et son icône, comme le fait l'Encoder. */
    private void drawRecipeType(IRecipeType type) {
        String name = type == null
                ? I18n.format("gui.packagedautoterminals.no_type")
                : type.getLocalizedNameShort();
        fontRenderer.drawString(name, RIGHT_CENTER - fontRenderer.getStringWidth(name) / 2,
                42, COLOR_DIM);

        if (type == null) {
            return;
        }
        Object representation = type.getRepresentation();
        if (representation instanceof ItemStack) {
            drawItem(RIGHT_CENTER - 8, 55, (ItemStack) representation);
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
