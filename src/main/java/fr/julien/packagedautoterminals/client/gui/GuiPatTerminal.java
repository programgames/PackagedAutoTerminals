package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import thelm.packagedauto.api.IRecipeInfo;

/**
 * Liste des machines et de leurs recettes.
 *
 * <p>Trois règles de mise en page, issues de l'échec de la première version :
 *
 * <ol>
 *   <li>une seule ligne de texte par rangée. L'état de la machine va à droite, jamais sous
 *       son nom, sans quoi les deux lignes se chevauchent dans 18 pixels ;
 *   <li>la recette est en retrait, pour montrer à quelle machine elle appartient ;
 *   <li>le détail passe par l'infobulle, pas par la ligne.
 * </ol>
 */
public class GuiPatTerminal extends AEBaseGui {

    private static final int ROWS = ContainerPatTerminal.ROWS;
    private static final int ROW_HEIGHT = ContainerPatTerminal.ROW_HEIGHT;
    private static final int LIST_LEFT = ContainerPatTerminal.LIST_LEFT;
    private static final int LIST_TOP = ContainerPatTerminal.LIST_TOP;
    private static final int LIST_WIDTH = ContainerPatTerminal.LIST_WIDTH;

    /** Décalage du texte pour centrer une ligne de 8 pixels dans une rangée de 18. */
    private static final int TEXT_OFFSET = 5;
    /** Retrait d'une rangée de recette. */
    private static final int INDENT = 14;

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_DIM = 0x808080;

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
        getScrollBar()
                .setLeft(ContainerPatTerminal.SCROLL_LEFT)
                .setTop(LIST_TOP)
                .setHeight(ROWS * ROW_HEIGHT);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        bindTexture(Reference.MOD_ID, "guis/pat_terminal.png");
        drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.pat_terminal"),
                LIST_LEFT, 6, COLOR_TEXT);

        // Mesure du lot 2 : elle décide si le découpage en chunks est nécessaire (R2).
        String size = terminalContainer.lastPayloadBytes + " o";
        fontRenderer.drawString(size, LIST_LEFT + LIST_WIDTH - fontRenderer.getStringWidth(size),
                6, COLOR_DIM);

        List<Line> lines = buildLines();
        getScrollBar().setRange(0, Math.max(0, lines.size() - ROWS), 2);

        if (lines.isEmpty()) {
            fontRenderer.drawString(I18n.format("gui.packagedautoterminals.empty"),
                    LIST_LEFT + 4, LIST_TOP + TEXT_OFFSET, COLOR_DIM);
            return;
        }

        int first = getScrollBar().getCurrentScroll();
        // Les coordonnées de la souris sont absolues, le dessin est relatif à la fenêtre.
        int hoverRow = rowUnder(mouseX - offsetX, mouseY - offsetY);

        for (int row = 0; row < ROWS && first + row < lines.size(); row++) {
            drawLine(lines.get(first + row), LIST_TOP + row * ROW_HEIGHT);
        }

        if (hoverRow >= 0 && first + hoverRow < lines.size()) {
            drawTooltip(mouseX - offsetX, mouseY - offsetY,
                    tooltipFor(lines.get(first + hoverRow)));
        }
    }

    private void drawLine(Line line, int y) {
        if (line.machine != null) {
            drawItem(LIST_LEFT + 2, y + 1, line.machine.icon);
            fontRenderer.drawString(trim(line.machine.name, 86), LIST_LEFT + 22,
                    y + TEXT_OFFSET, COLOR_TEXT);

            String state = stateOf(line.machine);
            fontRenderer.drawString(state,
                    LIST_LEFT + LIST_WIDTH - 4 - fontRenderer.getStringWidth(state),
                    y + TEXT_OFFSET, COLOR_DIM);
            return;
        }

        List<ItemStack> outputs = line.recipe.getOutputs();
        if (!outputs.isEmpty()) {
            drawItem(LIST_LEFT + INDENT, y + 1, outputs.get(0));
        }
        String output = outputs.isEmpty()
                ? I18n.format("gui.packagedautoterminals.no_output")
                : outputs.get(0).getDisplayName();
        fontRenderer.drawString(trim(output, 82), LIST_LEFT + INDENT + 20, y + TEXT_OFFSET,
                COLOR_TEXT);

        String type = line.recipe.getRecipeType().getLocalizedNameShort();
        fontRenderer.drawString(type,
                LIST_LEFT + LIST_WIDTH - 4 - fontRenderer.getStringWidth(type),
                y + TEXT_OFFSET, COLOR_DIM);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Clic droit : ouvrir l'éditeur. Maj + clic droit : supprimer.
        if (mouseButton == 1) {
            Line line = lineUnder(mouseX - guiLeft, mouseY - guiTop);
            if (line != null && line.recipe != null) {
                PatNetwork.CHANNEL.sendToServer(new PacketRecipeAction(
                        line.owner.dimension, line.owner.pos, line.recipeIndex,
                        isShiftKeyDown()
                                ? PacketRecipeAction.ACTION_REMOVE
                                : PacketRecipeAction.ACTION_EDIT));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** Rangée affichée sous la souris, ou {@code null}. Coordonnées relatives. */
    private Line lineUnder(int x, int y) {
        int row = rowUnder(x, y);
        if (row < 0) {
            return null;
        }
        List<Line> lines = buildLines();
        int index = getScrollBar().getCurrentScroll() + row;
        return index < lines.size() ? lines.get(index) : null;
    }

    /** Rangée sous la souris, ou -1. Les coordonnées sont relatives à la fenêtre. */
    private int rowUnder(int x, int y) {
        if (x < LIST_LEFT || x > LIST_LEFT + LIST_WIDTH) {
            return -1;
        }
        if (y < LIST_TOP || y >= LIST_TOP + ROWS * ROW_HEIGHT) {
            return -1;
        }
        return (y - LIST_TOP) / ROW_HEIGHT;
    }

    private List<String> tooltipFor(Line line) {
        List<String> lines = new ArrayList<>();
        if (line.machine != null) {
            lines.add(line.machine.name);
            lines.add(TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.position",
                    line.machine.pos.getX(), line.machine.pos.getY(), line.machine.pos.getZ(),
                    line.machine.dimension));
            lines.add(TextFormatting.GRAY + stateOf(line.machine));
            return lines;
        }

        lines.add(line.recipe.getRecipeType().getLocalizedName());
        addStacks(lines, "gui.packagedautoterminals.inputs", line.recipe.getInputs());
        addStacks(lines, "gui.packagedautoterminals.outputs", line.recipe.getOutputs());
        lines.add("");
        lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.edit_hint"));
        lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.delete_hint"));
        return lines;
    }

    private void addStacks(List<String> lines, String titleKey, List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        lines.add(TextFormatting.GRAY + I18n.format(titleKey));
        int shown = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (shown == 8) {
                lines.add(TextFormatting.DARK_GRAY + I18n.format(
                        "gui.packagedautoterminals.more", stacks.size() - shown));
                return;
            }
            lines.add("  " + stack.getCount() + " × " + stack.getDisplayName());
            shown++;
        }
    }

    private String stateOf(ProviderSnapshot machine) {
        if (!machine.active) {
            return I18n.format("gui.packagedautoterminals.inactive");
        }
        if (!machine.holderPresent) {
            return I18n.format("gui.packagedautoterminals.no_holder");
        }
        // Le singulier a sa propre clé : « 1 recettes » se voit tout de suite en jeu.
        int count = machine.recipes.size();
        return I18n.format(count == 1
                ? "gui.packagedautoterminals.recipe"
                : "gui.packagedautoterminals.recipes", count);
    }

    private String trim(String text, int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return fontRenderer.trimStringToWidth(text, maxWidth - 6) + "…";
    }

    private List<Line> buildLines() {
        List<Line> lines = new ArrayList<>();
        for (ProviderSnapshot provider : terminalContainer.providers) {
            lines.add(Line.machine(provider));
            for (int index = 0; index < provider.recipes.size(); index++) {
                lines.add(Line.recipe(provider, provider.recipes.get(index), index));
            }
        }
        return lines;
    }

    /**
     * Une rangée affichée : soit une machine, soit une recette.
     *
     * <p>Une rangée de recette porte aussi sa machine et son indice dans le porte-recettes.
     * Les ordres d'édition désignent la machine par sa **position**, jamais par son rang
     * dans la liste : l'ordre du scan peut changer d'un rafraîchissement à l'autre.
     */
    private static final class Line {
        final ProviderSnapshot machine;
        final ProviderSnapshot owner;
        final IRecipeInfo recipe;
        final int recipeIndex;

        static Line machine(ProviderSnapshot machine) {
            return new Line(machine, null, null, -1);
        }

        static Line recipe(ProviderSnapshot owner, IRecipeInfo recipe, int index) {
            return new Line(null, owner, recipe, index);
        }

        private Line(ProviderSnapshot machine, ProviderSnapshot owner, IRecipeInfo recipe,
                     int recipeIndex) {
            this.machine = machine;
            this.owner = owner;
            this.recipe = recipe;
            this.recipeIndex = recipeIndex;
        }
    }
}
