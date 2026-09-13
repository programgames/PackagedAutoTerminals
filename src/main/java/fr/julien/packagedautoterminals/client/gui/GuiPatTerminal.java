package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.common.CrafterTypes;
import fr.julien.packagedautoterminals.common.MachineSnapshot;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeType;

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
    private static final int COLOR_WARNING = 0x803030;

    private static final int BUTTON_VIEW = 0;

    private final ContainerPatTerminal terminalContainer;
    private GuiTextField search;
    private GuiButton viewButton;
    /** Faux : onglet des patterns. Vrai : onglet des machines. */
    private boolean machinesView;

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

        // Le fond du champ est dessiné dans la planche : le widget ne peint que le texte.
        String previous = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer,
                guiLeft + ContainerPatTerminal.SEARCH_LEFT + 2,
                guiTop + ContainerPatTerminal.SEARCH_TOP + 2,
                ContainerPatTerminal.SEARCH_WIDTH - 4, ContainerPatTerminal.SEARCH_HEIGHT - 4);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(64);
        search.setTextColor(COLOR_TEXT);
        search.setText(previous);
        // Le champ prend le focus tout de suite : le joueur ouvre le terminal pour chercher.
        search.setFocused(true);

        buttonList.clear();
        if (PatConfig.machinesTab) {
            viewButton = new GuiButton(BUTTON_VIEW, guiLeft + LIST_LEFT, guiTop + 2, 84, 14, "");
            buttonList.add(viewButton);
            updateViewButton();
        }

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
        // Sans l'onglet Machines, le bouton n'existe pas : le titre reprend sa place.
        if (!PatConfig.machinesTab) {
            fontRenderer.drawString(
                    trim(I18n.format("gui.packagedautoterminals.pat_terminal"), 84),
                    LIST_LEFT, 6, COLOR_TEXT);
        }

        // Libellé de l'inventaire, et résumé du réseau. Le résumé parle au joueur ; la
        // taille du paquet, qui ne dit rien à personne, passe dans l'infobulle. Elle sert à
        // trancher la révision R2.
        int summaryY = ContainerPatTerminal.PLAYER_INVENTORY_TOP - 11;
        fontRenderer.drawString(I18n.format("gui.packagedautoterminals.inventory"),
                LIST_LEFT, summaryY, COLOR_TEXT);

        String summary = networkSummary();
        int summaryX = LIST_LEFT + LIST_WIDTH - fontRenderer.getStringWidth(summary);
        fontRenderer.drawString(summary, summaryX, summaryY, COLOR_DIM);

        int localX = mouseX - offsetX;
        int localY = mouseY - offsetY;
        if (localX >= summaryX && localX <= LIST_LEFT + LIST_WIDTH
                && localY >= summaryY - 1 && localY <= summaryY + 8) {
            drawTooltip(localX, localY, java.util.Arrays.asList(
                    summary,
                    TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.payload",
                            terminalContainer.lastPayloadBytes)));
        }

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
        if (line.warning != null) {
            fontRenderer.drawString(trim(line.warning, LIST_WIDTH - 8), LIST_LEFT + 4,
                    y + TEXT_OFFSET, COLOR_WARNING);
            return;
        }

        if (line.crafter != null) {
            drawItem(LIST_LEFT + 2, y + 1, line.crafter.icon);
            fontRenderer.drawString(trim(line.crafter.name, 86), LIST_LEFT + 22,
                    y + TEXT_OFFSET, COLOR_TEXT);
            String state = I18n.format(!line.crafter.active
                    ? "gui.packagedautoterminals.inactive"
                    : (line.crafter.busy
                            ? "gui.packagedautoterminals.busy"
                            : "gui.packagedautoterminals.idle"));
            fontRenderer.drawString(state,
                    LIST_LEFT + LIST_WIDTH - 4 - fontRenderer.getStringWidth(state),
                    y + TEXT_OFFSET, COLOR_DIM);
            return;
        }

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

    private void updateViewButton() {
        if (viewButton != null) {
            viewButton.displayString = I18n.format(machinesView
                    ? "gui.packagedautoterminals.tab_machines"
                    : "gui.packagedautoterminals.tab_patterns");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_VIEW) {
            machinesView = !machinesView;
            updateViewButton();
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Sans cet appel, le trait du curseur ne clignote jamais : le joueur croit que le
        // champ n'a pas le focus.
        search.updateCursorCounter();
    }

    /**
     * Le champ de recherche se dessine ici, et non dans {@code drawFG}.
     *
     * <p>PIÈGE corrigé : il porte des coordonnées **absolues**, car {@code mouseClicked} lui
     * transmet des coordonnées absolues. Or {@code drawFG} dessine dans un repère déjà
     * décalé à l'angle de la fenêtre. Le texte partait donc deux fois plus loin, hors de
     * l'écran : ni le texte saisi, ni le curseur n'étaient visibles.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        search.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Échap et la touche d'inventaire doivent fermer la fenêtre, même si le champ a le
        // focus. Sans cette exception, le joueur reste piégé dans le terminal.
        if (search.isFocused() && keyCode != Keyboard.KEY_ESCAPE
                && !mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            if (search.textboxKeyTyped(typedChar, keyCode)) {
                // La plage de l'ascenseur est recalculée à chaque dessin ; elle se recale
                // donc seule sur la liste filtrée.
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        search.mouseClicked(mouseX, mouseY, mouseButton);
        // Clic gauche sur une machine : nouvelle recette.
        // Clic droit sur une recette : l'éditer. Maj + clic droit : la supprimer.
        Line clicked = machinesView ? null : lineUnder(mouseX - guiLeft, mouseY - guiTop);
        if (mouseButton == 0 && clicked != null && clicked.machine != null) {
            PatNetwork.CHANNEL.sendToServer(new PacketRecipeAction(
                    clicked.machine.dimension, clicked.machine.pos, -1,
                    isShiftKeyDown()
                            ? PacketRecipeAction.ACTION_REMOVE_HOLDER
                            : PacketRecipeAction.ACTION_NEW));
            return;
        }
        if (mouseButton == 1) {
            Line line = clicked;
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
        if (line.warning != null) {
            lines.add(line.warning);
            lines.add(TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.orphan_help"));
            return lines;
        }
        if (line.crafter != null) {
            lines.add(line.crafter.name);
            lines.add(TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.position",
                    line.crafter.pos.getX(), line.crafter.pos.getY(), line.crafter.pos.getZ(),
                    line.crafter.dimension));
            return lines;
        }
        if (line.machine != null) {
            lines.add(line.machine.name);
            lines.add(TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.position",
                    line.machine.pos.getX(), line.machine.pos.getY(), line.machine.pos.getZ(),
                    line.machine.dimension));
            lines.add(TextFormatting.GRAY + stateOf(line.machine));
            lines.add("");
            lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.new_hint"));
            if (line.machine.holderPresent) {
                lines.add(TextFormatting.DARK_GRAY
                        + I18n.format("gui.packagedautoterminals.remove_holder_hint"));
            }
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
            if (shown == PatConfig.tooltipStacks) {
                lines.add(TextFormatting.DARK_GRAY + I18n.format(
                        "gui.packagedautoterminals.more", stacks.size() - shown));
                return;
            }
            lines.add("  " + stack.getCount() + " × " + stack.getDisplayName());
            shown++;
        }
    }

    /** Résumé lisible du réseau : machines porteuses et recettes encodées. */
    private String networkSummary() {
        int recipes = 0;
        for (ProviderSnapshot provider : terminalContainer.providers) {
            recipes += provider.recipes.size();
        }
        return I18n.format("gui.packagedautoterminals.summary",
                terminalContainer.providers.size(), recipes);
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

    /**
     * Construit les rangées affichées, filtrées par la recherche.
     *
     * <p>Une machine reste visible si son nom correspond, ou si l'une de ses recettes
     * correspond. Sans cette règle, une recette trouvée apparaîtrait sans sa machine, et le
     * joueur ne saurait pas où elle se trouve.
     */
    private List<Line> buildLines() {
        String filter = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        return machinesView ? buildMachineLines(filter) : buildPatternLines(filter);
    }

    /**
     * Onglet Machines : les crafters du réseau, précédés des recettes orphelines.
     *
     * <p>Une recette est orpheline quand aucun crafter du réseau ne sait exécuter son type.
     * C'est l'erreur la plus fréquente en jeu, et aucun autre mod ne la signale.
     */
    private List<Line> buildMachineLines(String filter) {
        List<Line> lines = new ArrayList<>();

        Set<String> present = new HashSet<>();
        for (MachineSnapshot machine : terminalContainer.machines) {
            present.add(machine.machineClass);
        }

        Set<String> reported = new LinkedHashSet<>();
        for (ProviderSnapshot provider : terminalContainer.providers) {
            for (IRecipeInfo recipe : provider.recipes) {
                IRecipeType type = recipe.getRecipeType();
                if (!CrafterTypes.isDiagnosable(type)) {
                    continue;
                }
                if (!present.contains(CrafterTypes.machineClassFor(type))) {
                    reported.add(type.getLocalizedNameShort());
                }
            }
        }
        for (String type : reported) {
            lines.add(Line.warning(I18n.format("gui.packagedautoterminals.orphan", type)));
        }

        for (MachineSnapshot machine : terminalContainer.machines) {
            if (filter.isEmpty() || machine.name.toLowerCase(Locale.ROOT).contains(filter)) {
                lines.add(Line.crafter(machine));
            }
        }
        return lines;
    }

    private List<Line> buildPatternLines(String filter) {
        List<Line> lines = new ArrayList<>();

        for (ProviderSnapshot provider : terminalContainer.providers) {
            boolean machineMatches = filter.isEmpty()
                    || provider.name.toLowerCase(Locale.ROOT).contains(filter);

            List<Line> recipes = new ArrayList<>();
            for (int index = 0; index < provider.recipes.size(); index++) {
                IRecipeInfo recipe = provider.recipes.get(index);
                if (machineMatches || matches(recipe, filter)) {
                    recipes.add(Line.recipe(provider, recipe, index));
                }
            }

            if (machineMatches || !recipes.isEmpty()) {
                lines.add(Line.machine(provider));
                lines.addAll(recipes);
            }
        }
        return lines;
    }

    /** Une recette correspond par son type, ses sorties ou ses entrées. */
    private boolean matches(IRecipeInfo recipe, String filter) {
        if (recipe.getRecipeType().getLocalizedNameShort().toLowerCase(Locale.ROOT).contains(filter)) {
            return true;
        }
        for (ItemStack stack : recipe.getOutputs()) {
            if (!stack.isEmpty()
                    && stack.getDisplayName().toLowerCase(Locale.ROOT).contains(filter)) {
                return true;
            }
        }
        for (ItemStack stack : recipe.getInputs()) {
            if (!stack.isEmpty()
                    && stack.getDisplayName().toLowerCase(Locale.ROOT).contains(filter)) {
                return true;
            }
        }
        return false;
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
        final MachineSnapshot crafter;
        final String warning;

        static Line machine(ProviderSnapshot machine) {
            return new Line(machine, null, null, -1, null, null);
        }

        static Line recipe(ProviderSnapshot owner, IRecipeInfo recipe, int index) {
            return new Line(null, owner, recipe, index, null, null);
        }

        static Line crafter(MachineSnapshot crafter) {
            return new Line(null, null, null, -1, crafter, null);
        }

        static Line warning(String warning) {
            return new Line(null, null, null, -1, null, warning);
        }

        private Line(ProviderSnapshot machine, ProviderSnapshot owner, IRecipeInfo recipe,
                     int recipeIndex, MachineSnapshot crafter, String warning) {
            this.machine = machine;
            this.owner = owner;
            this.recipe = recipe;
            this.recipeIndex = recipeIndex;
            this.crafter = crafter;
            this.warning = warning;
        }
    }
}
