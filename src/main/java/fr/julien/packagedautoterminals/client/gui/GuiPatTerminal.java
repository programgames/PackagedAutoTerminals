package fr.julien.packagedautoterminals.client.gui;

import java.util.ArrayList;
import java.util.List;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import thelm.packagedauto.api.IRecipeInfo;

/**
 * Affichage en lecture seule du lot 2 : une ligne par machine, puis une ligne par recette.
 * L'édition arrive au lot 3.
 */
public class GuiPatTerminal extends AEBaseGui {

    private static final int ROWS = ContainerPatTerminal.ROWS;
    private static final int ROW_HEIGHT = 18;

    private final ContainerPatTerminal terminalContainer;

    public GuiPatTerminal(InventoryPlayer inventory, PartPatTerminal terminal) {
        super(new ContainerPatTerminal(inventory, terminal));
        this.terminalContainer = (ContainerPatTerminal) inventorySlots;
        this.xSize = ContainerPatTerminal.WIDTH;
        this.ySize = ContainerPatTerminal.HEIGHT;
        setScrollBar(new GuiScrollbar());
    }

    @Override
    public void initGui() {
        super.initGui();
        getScrollBar().setLeft(175).setTop(ContainerPatTerminal.LIST_TOP)
                .setHeight(ROWS * ROW_HEIGHT - 2);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.pat_terminal"), 8, 6, 0x404040);

        // Mesure du lot 2 : elle décide si le découpage en chunks est nécessaire (révision R2).
        String size = terminalContainer.lastPayloadBytes + " o";
        fontRenderer.drawString(size, 168 - fontRenderer.getStringWidth(size), 6, 0xA0A0A0);

        List<Line> lines = buildLines();
        getScrollBar().setRange(0, Math.max(0, lines.size() - ROWS), 2);
        int first = getScrollBar().getCurrentScroll();

        if (lines.isEmpty()) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.empty"), 8, 24, 0x808080);
            return;
        }

        for (int row = 0; row < ROWS && first + row < lines.size(); row++) {
            Line line = lines.get(first + row);
            int y = ContainerPatTerminal.LIST_TOP + row * ROW_HEIGHT;
            if (line.machine != null) {
                drawItem(9, y, line.machine.icon);
                fontRenderer.drawString(line.machine.name, 28, y + 1, 0x404040);
                fontRenderer.drawString(stateOf(line.machine), 28, y + 10, 0x808080);
            } else {
                List<ItemStack> outputs = line.recipe.getOutputs();
                if (!outputs.isEmpty()) {
                    drawItem(27, y, outputs.get(0));
                }
                fontRenderer.drawString(line.recipe.getRecipeType().getLocalizedNameShort(),
                        46, y + 5, 0x404040);
            }
        }
    }

    private String stateOf(ProviderSnapshot machine) {
        if (!machine.active) {
            return I18n.format("gui.packagedautoterminals.inactive");
        }
        if (!machine.holderPresent) {
            return I18n.format("gui.packagedautoterminals.no_holder");
        }
        return I18n.format("gui.packagedautoterminals.recipes", machine.recipes.size());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        // Fond repris d'AE2 : aucune ressource n'est copiée, seule la référence est partagée.
        bindTexture("appliedenergistics2", "guis/newinterfaceterminal.png");
        // La planche d'AE2 fait 256x256 et se découpe en trois morceaux : l'entête, les
        // lignes, puis le bas avec l'inventaire du joueur.
        drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ContainerPatTerminal.LIST_TOP);
        for (int row = 0; row < ROWS; row++) {
            drawTexturedModalRect(offsetX, offsetY + ContainerPatTerminal.LIST_TOP + row * ROW_HEIGHT,
                    0, ContainerPatTerminal.LIST_TOP, xSize, ROW_HEIGHT);
        }
        drawTexturedModalRect(offsetX, offsetY + ContainerPatTerminal.PLAYER_INVENTORY_TOP - 14,
                0, 158, xSize, 96);
    }

    private List<Line> buildLines() {
        List<Line> lines = new ArrayList<>();
        for (ProviderSnapshot provider : terminalContainer.providers) {
            lines.add(new Line(provider, null));
            for (IRecipeInfo recipe : provider.recipes) {
                lines.add(new Line(null, recipe));
            }
        }
        return lines;
    }

    private static final class Line {
        final ProviderSnapshot machine;
        final IRecipeInfo recipe;

        Line(ProviderSnapshot machine, IRecipeInfo recipe) {
            this.machine = machine;
            this.recipe = recipe;
        }
    }
}
