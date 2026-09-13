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
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.client.BlockHighlighter;
import fr.julien.packagedautoterminals.common.CrafterTypes;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.MachineSnapshot;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderRole;
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
import net.minecraft.util.math.BlockPos;
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

    /** Icône « œil » : position dans la planche, et taille. */
    private static final int LOCATE_U = 0;
    private static final int LOCATE_V = 232;
    private static final int LOCATE_SIZE = 12;
    /** Durée d'affichage d'un message, en millisecondes. */
    private static final long MESSAGE_DURATION = 3_000L;
    private static final int COLOR_OK = 0x2E7D32;
    /** Bord gauche du bouton, dans la rangée d'un groupe. */
    private static final int LOCATE_LEFT = LIST_LEFT + LIST_WIDTH - LOCATE_SIZE - 2;

    private static final int BUTTON_VIEW = 0;

    private final ContainerPatTerminal terminalContainer;
    private GuiTextField search;
    private GuiButton viewButton;
    /** Faux : onglet des patterns. Vrai : onglet des machines. */
    private boolean machinesView;
    private int lastFeedbackCount;
    private String message = "";
    private long messageExpiry;
    private boolean messageRefused;

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
        // Le résumé occupe toute la ligne. Le libellé « Inventaire » a disparu : il tenait
        // la moitié de la place, et n'apprenait rien à personne.
        int summaryY = ContainerPatTerminal.PLAYER_INVENTORY_TOP - 11;
        String summary = networkSummary();

        // Le message prend la place du résumé pendant trois secondes. La ligne de titre est
        // déjà prise par l'onglet et la recherche, et la barre d'action du jeu se dessine
        // sous la fenêtre, donc hors de vue.
        String message = currentMessage();
        if (message != null) {
            fontRenderer.drawString(trim(message, LIST_WIDTH), LIST_LEFT, summaryY,
                    messageRefused ? COLOR_WARNING : COLOR_OK);
        } else {
            fontRenderer.drawString(summary, LIST_LEFT, summaryY, COLOR_TEXT);
        }

        int localX = mouseX - offsetX;
        int localY = mouseY - offsetY;
        if (localX >= LIST_LEFT && localX <= LIST_LEFT + fontRenderer.getStringWidth(summary)
                && localY >= summaryY - 1 && localY <= summaryY + 8) {
            drawTooltip(localX, localY, java.util.Arrays.asList(
                    summary,
                    TextFormatting.GRAY + I18n.format("gui.packagedautoterminals.payload",
                            terminalContainer.lastPayloadBytes)));
        }

        List<Line> lines = buildLines();
        getScrollBar().setRange(0, Math.max(0, lines.size() - ROWS), 2);

        if (lines.isEmpty()) {
            // Le message est découpé à la largeur du cadre : sinon il déborde sur
            // l'ascenseur, puis hors de la fenêtre.
            List<String> wrapped = fontRenderer.listFormattedStringToWidth(
                    I18n.format("gui.packagedautoterminals.empty"), LIST_WIDTH - 10);
            for (int row = 0; row < wrapped.size(); row++) {
                fontRenderer.drawString(wrapped.get(row), LIST_LEFT + 4,
                        LIST_TOP + TEXT_OFFSET + row * 10, COLOR_DIM);
            }
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
            drawRight(state, y, COLOR_DIM);
            return;
        }

        if (line.isGroupHeader()) {
            drawItem(LIST_LEFT + 2, y + 1, line.anchor().icon);
            fontRenderer.drawString(trim(groupTitle(line.group), 96), LIST_LEFT + 22,
                    y + TEXT_OFFSET, COLOR_TEXT);

            String state = groupState(line.group);
            fontRenderer.drawString(state,
                    LOCATE_LEFT - 4 - fontRenderer.getStringWidth(state), y + TEXT_OFFSET,
                    COLOR_DIM);

            // La planche est reliée pour l'icône, puis le rendu du texte reprend la main.
            bindTexture(Reference.MOD_ID, "guis/pat_terminal.png");
            drawTexturedModalRect(LOCATE_LEFT, y + 3, LOCATE_U, LOCATE_V,
                    LOCATE_SIZE, LOCATE_SIZE);
            return;
        }

        List<ItemStack> outputs = line.recipe.getOutputs();
        if (!outputs.isEmpty()) {
            drawItem(LIST_LEFT + INDENT, y + 1, outputs.get(0));
        }
        String output = outputs.isEmpty()
                ? I18n.format("gui.packagedautoterminals.no_output")
                : outputs.get(0).getDisplayName();

        // Une recette présente d'un seul côté de la paire s'affiche en rouge : AE2 ne peut
        // pas l'exécuter.
        boolean complete = line.group.isComplete(line.recipe);
        fontRenderer.drawString(trim(output, 82), LIST_LEFT + INDENT + 20, y + TEXT_OFFSET,
                complete ? COLOR_TEXT : COLOR_WARNING);
        drawRight(line.recipe.getRecipeType().getLocalizedNameShort(), y,
                complete ? COLOR_DIM : COLOR_WARNING);
    }

    /** Texte aligné à droite de la zone de liste. */
    private void drawRight(String text, int y, int color) {
        fontRenderer.drawString(text,
                LIST_LEFT + LIST_WIDTH - 4 - fontRenderer.getStringWidth(text),
                y + TEXT_OFFSET, color);
    }

    /**
     * Message du serveur, affiché sous la ligne de titre.
     *
     * <p>Il remplace la barre d'action du jeu, qui se dessine **sous** la fenêtre et passait
     * donc inaperçue.
     */
    private String currentMessage() {
        if (terminalContainer.feedbackCount != lastFeedbackCount) {
            lastFeedbackCount = terminalContainer.feedbackCount;
            message = I18n.format(Feedback.key(terminalContainer.feedback),
                    Feedback.arguments(terminalContainer.feedback));
            messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
            // Un refus se reconnaît à sa clé : rien à traduire pour le savoir.
            messageRefused = terminalContainer.feedback.contains("no_")
                    || terminalContainer.feedback.contains("failed");
        }
        if (message.isEmpty() || System.currentTimeMillis() > messageExpiry) {
            return null;
        }
        return message;
    }

    /**
     * Étiquette d'un groupe.
     *
     * <p>Le nom donné par le joueur prime. Sinon, une paire s'annonce comme telle : c'est
     * plus parlant que « Packager +1 », et cela ne dépend pas de la longueur des noms.
     */
    private String groupTitle(ProviderPairing.Group group) {
        String custom = group.customName();
        if (custom != null) {
            return custom;
        }
        if (group.isPair()) {
            return I18n.format("gui.packagedautoterminals.pair");
        }
        if (group.size() == 1) {
            return group.singleName();
        }
        return I18n.format("gui.packagedautoterminals.group_of", group.size());
    }

    /** État d'un groupe : nombre de recettes, ou rôle manquant. */
    private String groupState(ProviderPairing.Group group) {
        if (group.recipes.isEmpty()) {
            return I18n.format("gui.packagedautoterminals.no_recipe_yet");
        }
        ProviderRole missing = ProviderPairing.missingRoleOf(group);
        if (missing != null) {
            return I18n.format("gui.packagedautoterminals.missing_"
                    + missing.name().toLowerCase(Locale.ROOT));
        }
        int count = group.recipes.size();
        return I18n.format(count == 1
                ? "gui.packagedautoterminals.recipe"
                : "gui.packagedautoterminals.recipes", count);
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
        if (clicked != null && clicked.isGroupHeader()
                && overLocate(mouseX - guiLeft, mouseY - guiTop)) {
            locate(clicked.group);
            return;
        }
        if (clicked != null && clicked.group != null) {
            ProviderSnapshot anchor = clicked.anchor();
            if (mouseButton == 0 && clicked.isGroupHeader()) {
                send(anchor, -1, isShiftKeyDown()
                        ? PacketRecipeAction.ACTION_REMOVE_HOLDER
                        : PacketRecipeAction.ACTION_NEW);
                return;
            }
            if (mouseButton == 1 && clicked.recipe != null) {
                send(anchor, clicked.recipeIndex, isShiftKeyDown()
                        ? PacketRecipeAction.ACTION_REMOVE
                        : PacketRecipeAction.ACTION_EDIT);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** La souris est-elle sur le bouton de repérage de la rangée survolée ? */
    private boolean overLocate(int x, int y) {
        int row = rowUnder(x, y);
        if (row < 0) {
            return false;
        }
        int top = LIST_TOP + row * ROW_HEIGHT + 3;
        return x >= LOCATE_LEFT && x < LOCATE_LEFT + LOCATE_SIZE
                && y >= top && y < top + LOCATE_SIZE;
    }

    /**
     * Fait clignoter les machines du groupe, et ferme la fenêtre.
     *
     * <p>Sans la fermeture, le joueur ne verrait pas le monde, donc rien du tout.
     */
    private void locate(ProviderPairing.Group group) {
        List<BlockPos> positions = new ArrayList<>();
        for (ProviderSnapshot machine : group.machines) {
            positions.add(machine.pos);
        }
        BlockHighlighter.highlight(group.machines.get(0).dimension, positions);
        mc.player.closeScreen();
    }

    private void send(ProviderSnapshot anchor, int index, byte action) {
        PatNetwork.CHANNEL.sendToServer(
                new PacketRecipeAction(anchor.dimension, anchor.pos, index, action));
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

        if (line.isGroupHeader()) {
            lines.add(groupTitle(line.group));
            for (ProviderSnapshot machine : line.group.machines) {
                lines.add(TextFormatting.GRAY + machine.name + " : "
                        + I18n.format("gui.packagedautoterminals.position",
                                machine.pos.getX(), machine.pos.getY(), machine.pos.getZ(),
                                machine.dimension));
            }
            ProviderRole missing = ProviderPairing.missingRoleOf(line.group);
            if (missing != null) {
                // Ce message ne s'affiche que si le groupe porte au moins une recette :
                // `missingRoleOf` rend `null` sur un groupe vide.
                lines.add(TextFormatting.RED + I18n.format(
                        "gui.packagedautoterminals.missing_group_help",
                        I18n.format("gui.packagedautoterminals.role_"
                                + missing.name().toLowerCase(Locale.ROOT))));
            }
            lines.add("");
            lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.new_hint"));
            lines.add(TextFormatting.DARK_GRAY
                    + I18n.format("gui.packagedautoterminals.remove_holder_hint"));
            lines.add(TextFormatting.DARK_GRAY
                    + I18n.format("gui.packagedautoterminals.locate_hint"));
            return lines;
        }

        lines.add(line.recipe.getRecipeType().getLocalizedName());
        if (!line.group.isComplete(line.recipe)) {
            ProviderRole missing = line.group.missingRole(line.recipe);
            lines.add(TextFormatting.RED + I18n.format("gui.packagedautoterminals.missing_help",
                    I18n.format("gui.packagedautoterminals.role_"
                            + (missing == null ? "unknown" : missing.name().toLowerCase(Locale.ROOT)))));
        }
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
        int machines = terminalContainer.providers.size();
        return I18n.format(machines == 1
                        ? "gui.packagedautoterminals.summary_machine"
                        : "gui.packagedautoterminals.summary_machines", machines)
                + " · "
                + I18n.format(recipes == 1
                        ? "gui.packagedautoterminals.summary_recipe"
                        : "gui.packagedautoterminals.summary_recipes", recipes);
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

    /**
     * Onglet Patterns : un en-tête par groupe de machines, puis ses recettes, une seule fois.
     *
     * <p>PackagedAuto demande la même recette dans le Packager et dans l'Unpackager. Les
     * afficher séparément montrerait deux fois la même chose, et inviterait à n'en modifier
     * qu'une.
     */
    private List<Line> buildPatternLines(String filter) {
        List<Line> lines = new ArrayList<>();

        for (ProviderPairing.Group group : ProviderPairing.group(terminalContainer.providers)) {
            boolean titleMatches = filter.isEmpty()
                    || groupTitle(group).toLowerCase(Locale.ROOT).contains(filter);

            List<Line> recipes = new ArrayList<>();
            for (int index = 0; index < group.recipes.size(); index++) {
                IRecipeInfo recipe = group.recipes.get(index);
                if (titleMatches || matches(recipe, filter)) {
                    recipes.add(Line.recipe(group, recipe, index));
                }
            }

            if (titleMatches || !recipes.isEmpty()) {
                lines.add(Line.group(group));
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
    /**
     * Une rangée affichée.
     *
     * <p>Une rangée de recette porte son **groupe** et l'indice de la recette dans ce
     * groupe. Les ordres d'édition désignent ainsi une recette unique, et non une copie
     * parmi deux.
     */
    private static final class Line {
        final ProviderPairing.Group group;
        final IRecipeInfo recipe;
        final int recipeIndex;
        final MachineSnapshot crafter;
        final String warning;

        static Line group(ProviderPairing.Group group) {
            return new Line(group, null, -1, null, null);
        }

        static Line recipe(ProviderPairing.Group group, IRecipeInfo recipe, int index) {
            return new Line(group, recipe, index, null, null);
        }

        static Line crafter(MachineSnapshot crafter) {
            return new Line(null, null, -1, crafter, null);
        }

        static Line warning(String warning) {
            return new Line(null, null, -1, null, warning);
        }

        boolean isGroupHeader() {
            return group != null && recipe == null;
        }

        /** Machine qui sert de point d'entrée aux ordres. N'importe laquelle du groupe suffit. */
        ProviderSnapshot anchor() {
            return group.machines.get(0);
        }

        private Line(ProviderPairing.Group group, IRecipeInfo recipe, int recipeIndex,
                     MachineSnapshot crafter, String warning) {
            this.group = group;
            this.recipe = recipe;
            this.recipeIndex = recipeIndex;
            this.crafter = crafter;
            this.warning = warning;
        }
    }
}
