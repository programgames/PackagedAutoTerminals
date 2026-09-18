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
import fr.julien.packagedautoterminals.common.TerminalContext;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
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
 * List of the machines and of their recipes.
 *
 * <p>Three layout rules, learned from the failure of the first version:
 *
 * <ol>
 *   <li>one single line of text per row. The machine state goes on the right, never below its
 *       name, otherwise the two lines overlap inside 18 pixels;
 *   <li>the recipe is indented, to show which machine it belongs to;
 *   <li>detail goes into the tooltip, not into the line.
 * </ol>
 */
public class GuiPatTerminal extends AEBaseGui {

    private static final int ROWS = ContainerPatTerminal.ROWS;
    private static final int ROW_HEIGHT = ContainerPatTerminal.ROW_HEIGHT;
    private static final int LIST_LEFT = ContainerPatTerminal.LIST_LEFT;
    private static final int LIST_TOP = ContainerPatTerminal.LIST_TOP;
    private static final int LIST_WIDTH = ContainerPatTerminal.LIST_WIDTH;

    /** Text offset, to centre an 8 pixel line inside an 18 pixel row. */
    private static final int TEXT_OFFSET = 5;
    /**
     * Indent of a recipe row.
     *
     * <p>It is twenty pixels since the collapse chevron arrived: the group icon moved ten
     * pixels to the right, and a recipe must stay indented behind it.
     */
    private static final int INDENT = 20;

    /** Collapse chevron: left edge, and size. */
    private static final int CHEVRON_LEFT = LIST_LEFT + 2;
    private static final int CHEVRON_SIZE = 8;
    private static final int CHEVRON_U = 330;
    private static final int CHEVRON_V = 20;
    /** The second chevron, the expanded one, sits next to the first on the sheet. */
    private static final int CHEVRON_OPEN_U = CHEVRON_U + 10;

    /** Group icon, and start of its name, after the chevron. */
    private static final int GROUP_ICON = LIST_LEFT + 12;
    private static final int GROUP_TEXT = LIST_LEFT + 32;

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_DIM = 0x808080;
    private static final int COLOR_WARNING = 0x803030;
    /** E4: every other row, very slightly darkened. */
    private static final int COLOR_STRIPE = 0x14000000;
    /** E1: hovered row. */
    private static final int COLOR_HOVER = 0x4066A0D0;

    /** E3: cross that clears the search, inside the field. */
    private static final int CLEAR_LEFT =
            ContainerPatTerminal.SEARCH_LEFT + ContainerPatTerminal.SEARCH_WIDTH - 12;
    private static final int CLEAR_TOP = ContainerPatTerminal.SEARCH_TOP + 2;
    private static final int CLEAR_SIZE = 9;

    /** "Locate" pin: position on the sheet, and size. */
    private static final int LOCATE_U = 330;
    private static final int LOCATE_V = 4;
    private static final int LOCATE_SIZE = 12;
    /** How long a message stays on screen, in milliseconds. */
    private static final long MESSAGE_DURATION = 3_000L;
    private static final int COLOR_OK = 0x2E7D32;
    /** Sheet size, in pixels. */
    private static final int SHEET = 512;
    /** Text of the input fields, light on their dark background. */
    private static final int COLOR_FIELD_TEXT = 0xE0E0E0;
    /** Left edge of the button, inside a group row. */
    /**
     * The pin, the type name and the machine icon share the **same** right edge. Without
     * that, the right column wobbled by two pixels from one row to the next.
     */
    private static final int LOCATE_LEFT = LIST_LEFT + LIST_WIDTH - LOCATE_SIZE - 4;

    /**
     * Crafting machine of a group, left of the pin.
     *
     * <p>Sixteen pixels for the icon, and four of gap before the pin.
     */
    private static final int GROUP_MACHINE_LEFT = LOCATE_LEFT - 20;

    private static final int BUTTON_VIEW = 0;

    private final ContainerPatTerminal terminalContainer;
    private GuiTextField search;
    private GuiButton viewButton;
    /** False: patterns tab. True: machines tab. */
    private boolean machinesView;
    /**
     * Groups expanded by hand, by their key.
     *
     * <p>The list always opens collapsed: this set therefore starts empty, and it never
     * leaves the client. A group has no stable identity — it is rebuilt on every scan — so
     * the key is the **smallest** position of its machines, which does not depend on the scan
     * order.
     */
    private final Set<Long> expanded = new HashSet<>();
    private int lastFeedbackCount;
    private String message = "";
    private long messageExpiry;
    private boolean messageRefused;

    public GuiPatTerminal(InventoryPlayer inventory, TerminalContext terminal) {
        super(new ContainerPatTerminal(inventory, terminal));
        this.terminalContainer = (ContainerPatTerminal) inventorySlots;
        this.xSize = ContainerPatTerminal.WIDTH;
        this.ySize = ContainerPatTerminal.HEIGHT;
        setScrollBar(new GuiScrollbar());
    }


    /**
     * The GUI scale the player chose, kept aside while this screen shrinks it. -1: untouched.
     *
     * <p>See {@link GuiScaleFit} for the reason, and for the reading of {@code ScaledResolution}
     * that gives the rule.
     */
    private int playerGuiScale = -1;

    /**
     * Lowers the GUI scale when this screen does not fit the window.
     *
     * @return true when the scale changed. The caller then returns at once: changing the scale
     *     runs {@code setWorldAndResolution}, which calls {@code initGui} again, and the second
     *     pass builds the screen at the right size.
     */
    private boolean fitToWindow() {
        if (playerGuiScale < 0) {
            playerGuiScale = mc.gameSettings.guiScale;
        }
        int wanted = GuiScaleFit.scaleFor(mc, playerGuiScale, xSize, ySize);
        if (wanted == mc.gameSettings.guiScale) {
            return false;
        }
        mc.gameSettings.guiScale = wanted;
        net.minecraft.client.gui.ScaledResolution size = GuiScaleFit.resolution(mc);
        setWorldAndResolution(mc, size.getScaledWidth(), size.getScaledHeight());
        return true;
    }

    /**
     * Gives the player their GUI scale back.
     *
     * <p>The setting only ever changed in memory, so a crash with the screen open leaves the
     * options file of the player untouched.
     */
    @Override
    public void onGuiClosed() {
        if (playerGuiScale >= 0) {
            mc.gameSettings.guiScale = playerGuiScale;
            playerGuiScale = -1;
        }
        super.onGuiClosed();
    }

    @Override
    public void initGui() {
        // The scale must be settled before anything is placed: every widget below reads
        // `width` and `height`.
        if (fitToWindow()) {
            return;
        }
        super.initGui();

        // The field background is drawn on the sheet: the widget only paints the text.
        // PITFALL: every piece of game text is drawn with a drop shadow. On a light panel it
        // reads as a second letter offset by one pixel, and the input looks blurry. The field
        // background is therefore dark, and the text light, as in AE2.
        String previous = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer,
                guiLeft + ContainerPatTerminal.SEARCH_LEFT + 3,
                guiTop + ContainerPatTerminal.SEARCH_TOP + 3,
                // E3: the width always leaves ten pixels for the cross, so the typed text
                // never runs underneath it.
                ContainerPatTerminal.SEARCH_WIDTH - 16, 8);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(64);
        search.setTextColor(COLOR_FIELD_TEXT);
        search.setText(previous);
        // The field takes focus right away: the player opens the terminal to search.
        search.setFocused(true);

        buttonList.clear();
        if (PatConfig.machinesTab) {
            // The button starts at 4, and not at 2: the panel bevel takes the first three
            // pixels of the screen, and the top edge of the button was cut by it.
            viewButton = new PatButton(BUTTON_VIEW, guiLeft + LIST_LEFT, guiTop + 4, 84, 14, "");
            buttonList.add(viewButton);
            updateViewButton();
        }

        getScrollBar()
                .setLeft(ContainerPatTerminal.SCROLL_LEFT)
                .setTop(LIST_TOP)
                .setHeight(ROWS * ROW_HEIGHT);
    }

    /**
     * The sheet draws sixteen rows; the screen only shows {@code ROWS}. The top is copied as
     * it is, then the bottom of the sheet is placed right under the last displayed row.
     * Changing the row count therefore needs no redrawing.
     */
    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        bindTexture(Reference.MOD_ID, "guis/pat_terminal.png");
        int listBottom = ContainerPatTerminal.LIST_TOP + ROWS * ROW_HEIGHT;
        int sheetBottom = ContainerPatTerminal.LIST_TOP
                + ContainerPatTerminal.SHEET_ROWS * ROW_HEIGHT;

        drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, xSize, listBottom,
                SHEET, SHEET);
        drawModalRectWithCustomSizedTexture(offsetX, offsetY + listBottom, 0, sheetBottom,
                xSize, ContainerPatTerminal.FOOTER, SHEET, SHEET);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        // Without the Machines tab, the button does not exist: the title takes its place.
        if (!PatConfig.machinesTab) {
            // The space reaches the search field, not the width of the old button: that
            // button does not exist in this mode.
            fontRenderer.drawString(
                    trim(I18n.format("gui.packagedautoterminals.pat_terminal"),
                            ContainerPatTerminal.SEARCH_LEFT - 6 - LIST_LEFT),
                    LIST_LEFT, 6, COLOR_TEXT);
        }

        // Inventory label, and network summary. The summary speaks to the player; the packet
        // size, which means nothing to anyone, goes into the tooltip. It serves to settle
        // revision R2.
        // The summary takes the whole line. The "Inventory" label is gone: it took half the
        // space, and taught nobody anything.
        int summaryY = ContainerPatTerminal.PLAYER_INVENTORY_TOP - 11;

        // The rows are built before the summary: the result counter depends on them.
        List<Line> lines = buildLines();
        getScrollBar().setRange(0, Math.max(0, lines.size() - ROWS), 2);

        // The message takes the place of the summary for three seconds. The title line is
        // already taken by the tab and the search, and the game action bar is drawn below the
        // screen, hence out of sight.
        String message = currentMessage();
        if (message != null) {
            fontRenderer.drawString(trim(message, LIST_WIDTH), LIST_LEFT, summaryY,
                    messageRefused ? COLOR_WARNING : COLOR_OK);
        } else {
            fontRenderer.drawString(networkSummary(), LIST_LEFT, summaryY, COLOR_TEXT);
            drawResultCount(lines, summaryY);
        }

        // E3: the cross only shows when there is something to clear.
        if (hasQuery()) {
            fontRenderer.drawString("×", CLEAR_LEFT + 2, CLEAR_TOP, COLOR_FIELD_TEXT);
        }

        if (lines.isEmpty()) {
            // The message is wrapped to the frame width: otherwise it spills over the
            // scrollbar, then out of the screen.
            // Two causes, two messages: the network is empty, or the search returns nothing.
            // Mixing them up sent the player checking their cables for nothing.
            boolean filtered = search != null && !search.getText().trim().isEmpty();
            List<String> wrapped = fontRenderer.listFormattedStringToWidth(
                    I18n.format(filtered
                            ? "gui.packagedautoterminals.no_result"
                            : "gui.packagedautoterminals.empty"), LIST_WIDTH - 10);
            for (int row = 0; row < wrapped.size(); row++) {
                fontRenderer.drawString(wrapped.get(row), LIST_LEFT + 4,
                        LIST_TOP + TEXT_OFFSET + row * 10, COLOR_DIM);
            }
            return;
        }

        int first = getScrollBar().getCurrentScroll();
        // Mouse coordinates are absolute, drawing is relative to the screen.
        int hoverRow = rowUnder(mouseX - offsetX, mouseY - offsetY);

        // E4 then E1. Both bands are drawn **before** the text, otherwise they would cover
        // it. The striping follows the absolute row index, not its place on screen: without
        // that, the pattern would jump on every scrollbar notch.
        for (int row = 0; row < ROWS && first + row < lines.size(); row++) {
            int top = LIST_TOP + row * ROW_HEIGHT;
            if ((first + row) % 2 == 1) {
                drawRect(LIST_LEFT, top, LIST_LEFT + LIST_WIDTH, top + ROW_HEIGHT, COLOR_STRIPE);
            }
            if (row == hoverRow) {
                drawRect(LIST_LEFT, top, LIST_LEFT + LIST_WIDTH, top + ROW_HEIGHT, COLOR_HOVER);
            }
        }

        for (int row = 0; row < ROWS && first + row < lines.size(); row++) {
            drawLine(lines.get(first + row), LIST_TOP + row * ROW_HEIGHT);
        }

        if (hoverRow >= 0 && first + hoverRow < lines.size()) {
            drawTooltip(mouseX - offsetX, mouseY - offsetY,
                    tooltipFor(lines.get(first + hoverRow)));
        }
    }

    /**
     * Binds the sheet, and resets the colour multiplier to opaque white.
     *
     * <p>PITFALL: `Gui.drawRect` leaves `GlStateManager.color` set to the colour it was given.
     * The striping uses COLOR_STRIPE, whose alpha is 0x14, hence 0.078. The alpha test of the
     * game discards every fragment below 0.1, so the next textured draw disappeared entirely.
     * The defect only hit the **first** icon of the frame: the first `drawItem` resets the
     * colour to white, and every later icon then drew correctly.
     */
    private void bindSheet() {
        bindTexture(Reference.MOD_ID, "guis/pat_terminal.png");
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        // PITFALL: `Gui.drawRect` ends with `disableBlend`. The striping of the rows, and the
        // veil of an absent machine, therefore left blending off. The transparent pixels of
        // the pin then drew opaque black, and the icon sat on a black square.
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void drawLine(Line line, int y) {
        if (line.warning != null) {
            fontRenderer.drawString(trim(line.warning, LIST_WIDTH - 8), LIST_LEFT + 4,
                    y + TEXT_OFFSET, line.section ? COLOR_TEXT : COLOR_WARNING);
            return;
        }

        if (line.crafter != null) {
            drawItem(LIST_LEFT + 2, y + 1, line.crafter.icon);
            String state = I18n.format(!line.crafter.active
                    ? "gui.packagedautoterminals.inactive"
                    : (line.crafter.busy
                            ? "gui.packagedautoterminals.busy"
                            : "gui.packagedautoterminals.idle"));
            drawRight(state, y, COLOR_DIM);
            fontRenderer.drawString(
                    trim(line.crafter.name, budget(LIST_LEFT + 22, state, LIST_LEFT + LIST_WIDTH - 4)),
                    LIST_LEFT + 22, y + TEXT_OFFSET, COLOR_TEXT);
            return;
        }

        if (line.isGroupHeader()) {
            // A group with no recipe has nothing to expand: it carries no chevron, so no
            // click stays without effect.
            if (!line.group.recipes.isEmpty()) {
                bindSheet();
                // PITFALL: `drawTexturedModalRect` assumes a 256 by 256 sheet. Ours is 512:
                // any coordinate beyond 256 fell out of range, and the game drew a piece of
                // the frame instead of the icon.
                drawModalRectWithCustomSizedTexture(CHEVRON_LEFT, y + 5,
                        isExpanded(line.group) ? CHEVRON_OPEN_U : CHEVRON_U, CHEVRON_V,
                        CHEVRON_SIZE, CHEVRON_SIZE, SHEET, SHEET);
            }
            drawItem(GROUP_ICON, y + 1, line.anchor().icon);

            // The crafting machine sits on the group row, and no longer on each recipe: on
            // the network it is always placed next to the Unpackager of the group.
            IRecipeType machineType = groupMachineType(line.group);
            ItemStack machine = CrafterTypes.iconFor(machineType);
            int stateRight = machine.isEmpty() ? LOCATE_LEFT - 4 : GROUP_MACHINE_LEFT - 4;

            String state = groupState(line.group);
            fontRenderer.drawString(
                    trim(groupTitle(line.group), budget(GROUP_TEXT, state, stateRight)),
                    GROUP_TEXT, y + TEXT_OFFSET, COLOR_TEXT);
            fontRenderer.drawString(state,
                    stateRight - fontRenderer.getStringWidth(state), y + TEXT_OFFSET,
                    COLOR_DIM);

            if (!machine.isEmpty()) {
                drawItem(GROUP_MACHINE_LEFT, y + 1, machine);
            }

            // The sheet is bound for the icon, then text rendering takes over again.
            bindSheet();
            drawModalRectWithCustomSizedTexture(LOCATE_LEFT, y + 3, LOCATE_U, LOCATE_V,
                    LOCATE_SIZE, LOCATE_SIZE, SHEET, SHEET);
            return;
        }

        List<ItemStack> outputs = line.recipe.getOutputs();
        if (!outputs.isEmpty()) {
            drawItem(LIST_LEFT + INDENT, y + 1, outputs.get(0));
        }
        String output = outputs.isEmpty()
                ? I18n.format("gui.packagedautoterminals.no_output")
                : outputs.get(0).getDisplayName();

        // A recipe present on only one side of the pair is shown in red: AE2 cannot run
        // it.
        boolean complete = line.group.isComplete(line.recipe);
        IRecipeType type = line.recipe.getRecipeType();
        int textLeft = LIST_LEFT + INDENT + 20;
        int rightEdge = LIST_LEFT + LIST_WIDTH - 4;

        // The type name, and not the machine icon: the machine now sits on the group row.
        String typeName = type.getLocalizedNameShort();
        drawRight(typeName, y, complete ? COLOR_DIM : COLOR_WARNING);
        int textWidth = budget(textLeft, typeName, rightEdge);

        fontRenderer.drawString(trim(output, textWidth), textLeft, y + TEXT_OFFSET,
                complete ? COLOR_TEXT : COLOR_WARNING);
    }

    /**
     * Key of a group, for the collapsed state.
     *
     * <p>The smallest position of its machines. It changes neither with the scan order, nor
     * when a machine with a larger position is added.
     */
    private static long keyOf(ProviderPairing.Group group) {
        long key = Long.MAX_VALUE;
        for (ProviderSnapshot machine : group.machines) {
            key = Math.min(key, machine.pos.toLong());
        }
        return key;
    }

    private boolean isExpanded(ProviderPairing.Group group) {
        return expanded.contains(keyOf(group));
    }

    /**
     * The single crafting machine of a group, or {@code null}.
     *
     * <p>The icon only appears when every recipe that **needs** a machine names the same one.
     * Two different machines in one group show nothing: the row would otherwise state
     * something false.
     *
     * <p>The recipes whose type needs no machine, such as {@code processing}, are left out of
     * the comparison. They target no machine, so they cannot disagree with one.
     */
    private IRecipeType groupMachineType(ProviderPairing.Group group) {
        IRecipeType found = null;
        String foundClass = null;
        for (IRecipeInfo recipe : group.recipes) {
            IRecipeType type = recipe.getRecipeType();
            String machineClass = CrafterTypes.machineClassFor(type);
            if (machineClass == null) {
                continue;
            }
            if (foundClass == null) {
                found = type;
                foundClass = machineClass;
            } else if (!foundClass.equals(machineClass)) {
                return null;
            }
        }
        return found;
    }

    /** Is a machine able to run this type placed on the network? */
    private boolean onNetwork(IRecipeType type) {
        String machineClass = CrafterTypes.machineClassFor(type);
        if (machineClass == null) {
            return false;
        }
        for (MachineSnapshot machine : terminalContainer.machines) {
            if (machineClass.equals(machine.machineClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Space left for the name, between its left margin and the right-aligned text.
     *
     * <p>These widths used to be hard coded, and came from the 256 pixel screen. Since the
     * widening to 320, a name was trimmed in the middle of an area that stayed empty. The
     * computation now follows the real width of the right-hand text, whatever the
     * translation.
     */
    private int budget(int left, String right, int rightEdge) {
        return rightEdge - fontRenderer.getStringWidth(right) - 6 - left;
    }

    /**
     * E3: number of recipes found, aligned to the right of the summary.
     *
     * <p>It only shows during a search. Outside a search the network summary already says
     * everything, and a second count would duplicate it.
     */
    private void drawResultCount(List<Line> lines, int y) {
        if (!hasQuery()) {
            return;
        }
        int found = 0;
        for (Line line : lines) {
            if (line.recipe != null) {
                found++;
            }
        }
        String text = I18n.format(found == 1
                ? "gui.packagedautoterminals.result"
                : "gui.packagedautoterminals.results", found);
        fontRenderer.drawString(text,
                LIST_LEFT + LIST_WIDTH - fontRenderer.getStringWidth(text), y, COLOR_DIM);
    }

    /** Does the search carry any text? */
    private boolean hasQuery() {
        return search != null && !search.getText().isEmpty();
    }

    /** Text aligned to the right of the list area. */
    private void drawRight(String text, int y, int color) {
        fontRenderer.drawString(text,
                LIST_LEFT + LIST_WIDTH - 4 - fontRenderer.getStringWidth(text),
                y + TEXT_OFFSET, color);
    }

    /**
     * Server message, shown below the title line.
     *
     * <p>It replaces the game action bar, which is drawn **below** the screen and therefore
     * went unnoticed.
     */
    private String currentMessage() {
        if (terminalContainer.feedbackCount != lastFeedbackCount) {
            lastFeedbackCount = terminalContainer.feedbackCount;
            message = I18n.format(Feedback.key(terminalContainer.feedback),
                    Feedback.arguments(terminalContainer.feedback));
            messageExpiry = System.currentTimeMillis() + MESSAGE_DURATION;
            // A refusal is recognised by its key: no translation needed to tell.
            messageRefused = terminalContainer.feedback.contains("no_")
                    || terminalContainer.feedback.contains("failed");
        }
        if (message.isEmpty() || System.currentTimeMillis() > messageExpiry) {
            return null;
        }
        return message;
    }

    /**
     * Label of a group.
     *
     * <p>The name given by the player wins. Otherwise a pair announces itself as one: that
     * says more than "Packager +1", and it does not depend on the length of the names.
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

    /** State of a group: recipe count, or missing role. */
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
        // Without this call the caret never blinks: the player believes the field does not
        // have focus.
        search.updateCursorCounter();
    }

    /**
     * The search field is drawn here, and not in {@code drawFG}.
     *
     * <p>PITFALL fixed: it carries **absolute** coordinates, because {@code mouseClicked}
     * hands it absolute coordinates. But {@code drawFG} draws in a frame already shifted to
     * the corner of the screen. The text therefore went twice as far, off screen: neither the
     * typed text nor the caret was visible.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        search.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // The field keeps every letter, including the inventory key: typing "e" in a search
        // used to close the screen. Only Escape leaves it.
        if (search.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            if (search.textboxKeyTyped(typedChar, keyCode)) {
                // The scrollbar range is recomputed on every draw; it therefore settles on
                // the filtered list by itself.
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // E3: the cross comes before the field, otherwise the click would only move the caret.
        if (hasQuery() && overClear(mouseX - guiLeft, mouseY - guiTop)) {
            search.setText("");
            search.setFocused(true);
            return;
        }
        search.mouseClicked(mouseX, mouseY, mouseButton);
        // Left click on a machine: new recipe.
        // Right click on a recipe: edit it. Shift + right click: delete it.
        Line clicked = machinesView ? null : lineUnder(mouseX - guiLeft, mouseY - guiTop);
        if (clicked != null && clicked.isGroupHeader()
                && overLocate(mouseX - guiLeft, mouseY - guiTop)) {
            locate(clicked.group);
            return;
        }
        // The chevron comes first: without this early return, the same click would also
        // create a recipe, because a group row already answers the left click.
        if (clicked != null && clicked.isGroupHeader() && mouseButton == 0
                && !clicked.group.recipes.isEmpty()
                && overChevron(mouseX - guiLeft, mouseY - guiTop)) {
            long key = keyOf(clicked.group);
            if (!expanded.remove(key)) {
                expanded.add(key);
            }
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

    /** Is the mouse over the search cross? Relative coordinates. */
    private boolean overClear(int x, int y) {
        return x >= CLEAR_LEFT && x < CLEAR_LEFT + CLEAR_SIZE
                && y >= CLEAR_TOP && y < CLEAR_TOP + CLEAR_SIZE;
    }

    /** Is the mouse over the chevron of the hovered row? Relative coordinates. */
    private boolean overChevron(int x, int y) {
        int row = rowUnder(x, y);
        if (row < 0) {
            return false;
        }
        // The clickable area covers the whole row height: an eight pixel chevron is missed
        // every other time.
        int top = LIST_TOP + row * ROW_HEIGHT;
        return x >= CHEVRON_LEFT - 2 && x < CHEVRON_LEFT + CHEVRON_SIZE + 2
                && y >= top && y < top + ROW_HEIGHT;
    }

    /** Is the mouse over the locate button of the hovered row? */
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
     * Blinks the machines of the group, and closes the screen.
     *
     * <p>Without the close, the player would not see the world, hence nothing at all.
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

    /** Displayed row under the mouse, or {@code null}. Relative coordinates. */
    private Line lineUnder(int x, int y) {
        int row = rowUnder(x, y);
        if (row < 0) {
            return null;
        }
        List<Line> lines = buildLines();
        int index = getScrollBar().getCurrentScroll() + row;
        return index < lines.size() ? lines.get(index) : null;
    }

    /** Row under the mouse, or -1. Coordinates are relative to the screen. */
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
                // This message only shows when the group carries at least one recipe:
                // `missingRoleOf` returns `null` on an empty group.
                lines.add(TextFormatting.RED + I18n.format(
                        "gui.packagedautoterminals.missing_group_help",
                        I18n.format("gui.packagedautoterminals.role_"
                                + missing.name().toLowerCase(Locale.ROOT))));
            }
            IRecipeType machineType = groupMachineType(line.group);
            if (machineType != null) {
                lines.add(machineHint(machineType));
            }
            lines.add("");
            lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.new_hint"));
            lines.add(TextFormatting.DARK_GRAY
                    + I18n.format("gui.packagedautoterminals.remove_holder_hint"));
            lines.add(TextFormatting.DARK_GRAY
                    + I18n.format("gui.packagedautoterminals.locate_hint"));
            if (!line.group.recipes.isEmpty()) {
                lines.add(TextFormatting.DARK_GRAY
                        + I18n.format("gui.packagedautoterminals.expand_hint"));
            }
            return lines;
        }

        // Inputs and outputs no longer appear here: the list serves to find a recipe, the
        // editor to read it in detail.
        lines.add(line.recipe.getRecipeType().getLocalizedName());
        lines.add(machineHint(line.recipe.getRecipeType()));
        if (!line.group.isComplete(line.recipe)) {
            ProviderRole missing = line.group.missingRole(line.recipe);
            lines.add(TextFormatting.RED + I18n.format("gui.packagedautoterminals.missing_help",
                    I18n.format("gui.packagedautoterminals.role_"
                            + (missing == null ? "unknown" : missing.name().toLowerCase(Locale.ROOT)))));
        }
        lines.add("");
        lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.edit_hint"));
        lines.add(TextFormatting.DARK_GRAY + I18n.format("gui.packagedautoterminals.delete_hint"));
        return lines;
    }

    /**
     * Tooltip line about the crafting machine.
     *
     * <p>Three cases, three sentences: the machine is there, the machine is missing from the
     * network, or the type needs no machine. The third case covers {@code processing}, which
     * sends its package to any inventory.
     */
    private String machineHint(IRecipeType type) {
        ItemStack machine = CrafterTypes.iconFor(type);
        if (machine.isEmpty()) {
            return TextFormatting.GRAY
                    + I18n.format("gui.packagedautoterminals.machine_none");
        }
        if (onNetwork(type)) {
            return TextFormatting.GRAY + I18n.format(
                    "gui.packagedautoterminals.machine_used", machine.getDisplayName());
        }
        return TextFormatting.RED + I18n.format(
                "gui.packagedautoterminals.machine_absent", machine.getDisplayName());
    }

    /** Readable network summary: providing machines and encoded recipes. */
    private String networkSummary() {
        // Recipes are counted **per group**, not per machine. A pair carries the same recipe
        // on both sides: counting it twice reported four recipes where the list showed
        // two.
        int recipes = 0;
        for (ProviderPairing.Group group
                : ProviderPairing.group(terminalContainer.providers)) {
            recipes += group.recipes.size();
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
        // The singular has its own key: "1 recipes" stands out at once in game.
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
     * Builds the displayed rows, filtered by the search.
     *
     * <p>A machine stays visible when its name matches, or when one of its recipes matches.
     * Without this rule, a recipe found would appear without its machine, and the player
     * would not know where it sits.
     */
    private List<Line> buildLines() {
        Query query = Query.parse(search == null ? "" : search.getText());
        return machinesView ? buildMachineLines(query) : buildPatternLines(query);
    }

    /**
     * Search split into criteria.
     *
     * <p>Three prefixes, taken from the AE2 and JEI terminals: {@code @} targets the source
     * mod, {@code #} targets the recipe type, and the rest is searched in the names. All
     * criteria must be satisfied at once.
     *
     * <p>{@code @} and the plain text both read the **produced** items only, never the
     * ingredients. See {@link GuiPatTerminal#outputStacks}.
     */
    private static final class Query {
        final List<String> text = new ArrayList<>();
        final List<String> mods = new ArrayList<>();
        final List<String> types = new ArrayList<>();

        static Query parse(String raw) {
            Query query = new Query();
            for (String token : raw.trim().toLowerCase(Locale.ROOT).split(" +")) {
                if (token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("@") && token.length() > 1) {
                    query.mods.add(token.substring(1));
                } else if (token.startsWith("#") && token.length() > 1) {
                    query.types.add(token.substring(1));
                } else {
                    query.text.add(token);
                }
            }
            return query;
        }

        boolean isEmpty() {
            return text.isEmpty() && mods.isEmpty() && types.isEmpty();
        }
    }

    /**
     * Machines tab: the crafters of the network, preceded by the orphan recipes.
     *
     * <p>A recipe is an orphan when no crafter on the network can run its type. That is the
     * most frequent mistake in game, and no other mod reports it.
     */
    private List<Line> buildMachineLines(Query query) {
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

        // Crafters first, then routers. In a flat list a Proxy looks like a crafter, while
        // it builds nothing.
        List<Line> crafters = new ArrayList<>();
        List<Line> routers = new ArrayList<>();
        for (MachineSnapshot machine : terminalContainer.machines) {
            if (!query.isEmpty() && !matchesText(query, machine.name)) {
                continue;
            }
            if (CrafterTypes.isRouter(machine.machineClass)) {
                routers.add(Line.crafter(machine));
            } else {
                crafters.add(Line.crafter(machine));
            }
        }

        if (!crafters.isEmpty()) {
            lines.add(Line.section(I18n.format("gui.packagedautoterminals.section_crafters")));
            lines.addAll(crafters);
        }
        if (!routers.isEmpty()) {
            lines.add(Line.section(I18n.format("gui.packagedautoterminals.section_routers")));
            lines.addAll(routers);
        }
        return lines;
    }

    /**
     * Patterns tab: one header per machine group, then its recipes, once only.
     *
     * <p>PackagedAuto needs the same recipe in the Packager and in the Unpackager. Showing
     * them separately would display the same thing twice, and invite the player to edit only
     * one of them.
     */
    private List<Line> buildPatternLines(Query query) {
        List<Line> lines = new ArrayList<>();
        boolean searching = !query.isEmpty();

        for (ProviderPairing.Group group : ProviderPairing.group(terminalContainer.providers)) {
            boolean titleMatches = query.isEmpty()
                    || (query.types.isEmpty() && query.mods.isEmpty()
                            && matchesText(query, groupTitle(group)));

            List<Line> recipes = new ArrayList<>();
            for (int index = 0; index < group.recipes.size(); index++) {
                IRecipeInfo recipe = group.recipes.get(index);
                if (titleMatches || matches(recipe, query)) {
                    recipes.add(Line.recipe(group, recipe, index));
                }
            }

            if (!titleMatches && recipes.isEmpty()) {
                continue;
            }
            lines.add(Line.group(group));

            // Two rules, said in a single line: outside a search the group opens on the
            // chevron; during a search it opens on the result found. Expanding through the
            // search does not touch `expanded`: clearing the field therefore returns the list
            // to its collapsed state, and the player finds back what they had opened.
            if (searching || isExpanded(group)) {
                lines.addAll(recipes);
            }
        }
        return lines;
    }

    /** Does a recipe satisfy **all** the criteria? */
    private boolean matches(IRecipeInfo recipe, Query query) {
        if (query.isEmpty()) {
            return true;
        }

        for (String type : query.types) {
            String name = recipe.getRecipeType().getLocalizedNameShort().toLowerCase(Locale.ROOT);
            String id = recipe.getRecipeType().getName().getResourcePath().toLowerCase(Locale.ROOT);
            if (!name.contains(type) && !id.contains(type)) {
                return false;
            }
        }

        for (String mod : query.mods) {
            if (!hasMod(recipe, mod)) {
                return false;
            }
        }

        for (String text : query.text) {
            if (!hasText(recipe, text)) {
                return false;
            }
        }
        return true;
    }

    /** Does one of the produced items come from this mod? */
    private boolean hasMod(IRecipeInfo recipe, String mod) {
        for (ItemStack stack : outputStacks(recipe)) {
            if (stack.getItem().getRegistryName() != null
                    && stack.getItem().getRegistryName().getResourceDomain()
                            .toLowerCase(Locale.ROOT).contains(mod)) {
                return true;
            }
        }
        return false;
    }

    /** Does one of the produced items carry this text in its name? */
    private boolean hasText(IRecipeInfo recipe, String text) {
        for (ItemStack stack : outputStacks(recipe)) {
            if (stack.getDisplayName().toLowerCase(Locale.ROOT).contains(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What the recipe **produces**, and nothing else.
     *
     * <p>The search used to read the inputs too. Typing "elite" then returned every recipe
     * that merely consumes an Elite part, such as "ME Interface" or "Ultimate Crafting
     * Table". The player looks for the recipe that makes an item, so only the outputs count.
     */
    private List<ItemStack> outputStacks(IRecipeInfo recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : recipe.getOutputs()) {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private boolean matchesText(Query query, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String text : query.text) {
            if (!lower.contains(text)) {
                return false;
            }
        }
        return !query.text.isEmpty();
    }

    /**
     * A displayed row: either a machine, or a recipe.
     *
     * <p>A recipe row also carries its machine and its index in the recipe holder. Edit
     * commands name the machine by its **position**, never by its rank in the list: the scan
     * order can change from one refresh to the next.
     */
    /**
     * A displayed row.
     *
     * <p>A recipe row carries its **group** and the index of the recipe inside that group.
     * Edit commands therefore name a single recipe, and not one copy out of two.
     */
    private static final class Line {
        final ProviderPairing.Group group;
        final IRecipeInfo recipe;
        final int recipeIndex;
        final MachineSnapshot crafter;
        final String warning;
        boolean section;

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

        /** Subheading of the Machines tab: "Crafters", then "Routers". */
        static Line section(String title) {
            Line line = new Line(null, null, -1, null, title);
            line.section = true;
            return line;
        }

        boolean isGroupHeader() {
            return group != null && recipe == null;
        }

        /** Machine used as the entry point for commands. Any one of the group will do. */
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
