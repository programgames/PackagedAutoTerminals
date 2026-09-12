package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;

import appeng.client.gui.AEBaseGui;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.EditorInventory;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;

/**
 * Éditeur d'une recette.
 *
 * <p>La planche mesure 256 sur 512, car la fenêtre dépasse les 256 pixels de haut que
 * suppose {@code drawTexturedModalRect}. Le dessin passe donc par
 * {@code drawModalRectWithCustomSizedTexture}.
 */
public class GuiPatEditor extends AEBaseGui {

    private static final int BUTTON_PREVIOUS_TYPE = 0;
    private static final int BUTTON_NEXT_TYPE = 1;
    private static final int BUTTON_SAVE = 2;

    private static final int SHEET_WIDTH = 256;
    private static final int SHEET_HEIGHT = 512;

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_DIM = 0x808080;
    /** Voile posé sur les emplacements que le type de recette n'active pas. */
    private static final int COLOR_DISABLED = 0xA0303030;

    private final ContainerPatEditor editorContainer;
    private GuiButton saveButton;

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
        buttonList.clear();
        buttonList.add(new GuiButton(BUTTON_PREVIOUS_TYPE, guiLeft + 178, guiTop + 140, 20, 20, "<"));
        buttonList.add(new GuiButton(BUTTON_NEXT_TYPE, guiLeft + 212, guiTop + 140, 20, 20, ">"));
        saveButton = new GuiButton(BUTTON_SAVE, guiLeft + 178, guiTop + 164, 54, 20,
                I18n.format("gui.packagedautoterminals.save"));
        buttonList.add(saveButton);
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
                send(PacketRecipeAction.ACTION_SAVE, 0);
                break;
            default:
                super.actionPerformed(button);
        }
    }

    private void send(byte action, int value) {
        PatNetwork.CHANNEL.sendToServer(new PacketRecipeAction(
                editorContainer.dimension, editorContainer.pos, value, action));
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

        String title = editor.recipeType == null
                ? I18n.format("gui.packagedautoterminals.no_type")
                : editor.recipeType.getLocalizedName();
        fontRenderer.drawString(title, 8, 6, COLOR_TEXT);

        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.outputs"),
                ContainerPatEditor.OUTPUT_LEFT, ContainerPatEditor.OUTPUT_TOP - 10, COLOR_DIM);
        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.result"),
                ContainerPatEditor.PREVIEW_LEFT, ContainerPatEditor.PREVIEW_TOP - 10, COLOR_DIM);

        // Voile sur tout emplacement que le type n'active pas. Le serveur les refuse déjà ;
        // le voile évite au joueur d'essayer.
        for (int slot = 0; slot < EditorInventory.INPUT_SLOTS + EditorInventory.OUTPUT_SLOTS; slot++) {
            if (editor.isEditable(slot)) {
                continue;
            }
            int x = slot < EditorInventory.INPUT_SLOTS
                    ? ContainerPatEditor.GRID_LEFT + (slot % 9) * 18
                    : ContainerPatEditor.OUTPUT_LEFT + ((slot - EditorInventory.INPUT_SLOTS) % 3) * 18;
            int y = slot < EditorInventory.INPUT_SLOTS
                    ? ContainerPatEditor.GRID_TOP + (slot / 9) * 18
                    : ContainerPatEditor.OUTPUT_TOP + ((slot - EditorInventory.INPUT_SLOTS) / 3) * 18;
            drawRect(x, y, x + 16, y + 16, COLOR_DISABLED);
        }

        if (saveButton != null) {
            saveButton.enabled = editor.recipeInfo != null;
        }
        if (editor.recipeInfo == null) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.invalid"),
                    ContainerPatEditor.PREVIEW_LEFT, 130, COLOR_DIM);
        }
    }
}
