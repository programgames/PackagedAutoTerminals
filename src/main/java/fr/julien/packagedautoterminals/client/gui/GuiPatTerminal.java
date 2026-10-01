package fr.julien.packagedautoterminals.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.client.BlockHighlighter;
import fr.julien.packagedautoterminals.common.CrafterTypes;
import fr.julien.packagedautoterminals.common.Feedback;
import fr.julien.packagedautoterminals.common.MachineSnapshot;
import fr.julien.packagedautoterminals.common.NetworkItems;
import fr.julien.packagedautoterminals.common.PatConfig;
import fr.julien.packagedautoterminals.common.ProviderPairing;
import fr.julien.packagedautoterminals.common.ProviderRole;
import fr.julien.packagedautoterminals.common.ProviderSnapshot;
import fr.julien.packagedautoterminals.common.SearchQuery;
import fr.julien.packagedautoterminals.container.ContainerPatEditor;
import fr.julien.packagedautoterminals.container.ContainerPatTerminal;
import fr.julien.packagedautoterminals.network.PacketRecipeAction;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.common.TerminalContext;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
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

    /** Small glyphs, on one line of the sheet under the tabs. */
    private static final int GLYPH_V = 90;
    private static final int SEARCH_ICON_U = 330;
    private static final int SEARCH_ICON_SIZE = 8;
    private static final int EJECT_U = 342;
    private static final int INSERT_U = 414;
    private static final int PENCIL_U = 356;
    private static final int CROSS_U = 370;
    private static final int ACTION_SIZE = 10;
    private static final int SORT_U = 384;
    private static final int FILTER_U = 400;
    private static final int TOOL_SIZE = 12;
    /** The drawing shown when the list holds nothing. */
    private static final int EMPTY_U = 330;
    private static final int EMPTY_V = 108;
    private static final int EMPTY_SIZE = 32;

    /** Separator under a group header, and guide in front of its recipes. */
    private static final int COLOR_RULE = 0x22000000;
    /** Ring around the search field while it has focus. */
    private static final int COLOR_FOCUS = 0x90FFFFFF;
    /** Plate under a tool button that is turned on. */
    private static final int COLOR_TOOL_ON = 0x50000000;

    /** The two states of a tab, stored right of the screen on the sheet. */
    private static final int TAB_OFF_U = 330;
    private static final int TAB_OFF_V = 40;
    private static final int TAB_ON_U = 330;
    private static final int TAB_ON_V = TAB_OFF_V + ContainerPatTerminal.TAB_HEIGHT + 2;

    /**
     * State chip: a tinted plate behind the right-hand text of a row.
     *
     * <p>Plain grey text had to be read word by word. A chip gives the eye a shape to catch,
     * and a colour that says whether the player has something to do. Thermal and Mekanism
     * both use the same device.
     */
    private static final int CHIP_FILL = 0x26000000;
    private static final int CHIP_EDGE = 0x3D000000;
    private static final int CHIP_WARNING_FILL = 0x2EA02020;
    private static final int CHIP_WARNING_EDGE = 0x55902020;

    /** Sort orders of the list, in the order the button cycles through them. */
    private static final int SORT_NAME = 0;
    private static final int SORT_MACHINE = 1;
    private static final int SORT_RECIPES = 2;
    private static final int SORT_COUNT = 3;

    private final ContainerPatTerminal terminalContainer;
    /** The two tab icons, resolved on first use. */
    private ItemStack[] tabIcons;
    /** Order of the rows. The tool button cycles it. */
    private int sortMode = SORT_NAME;
    /** Show only the rows the player has to act on. */
    private boolean onlyProblems;
    /** Row under the pointer, in the visible window. Negative when there is none. */
    private int hoveredRow = -1;
    private GuiTextField search;
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
    /**
     * The rows of the list, built once per change.
     *
     * <p>{@code drawFG} runs on every frame. It used to call {@link #buildLines} there, and
     * that method regroups the machines by comparing their recipes two by two, then formats
     * every row. On a network of fifty machines carrying twenty recipes each, the screen paid
     * for tens of thousands of recipe comparisons per second, for a list that changes about
     * once a second.
     *
     * <p>The rows are now rebuilt only when something they depend on moves: the snapshot from
     * the server, the search text, the open tab, or the set of unfolded groups.
     */
    private List<Line> cachedLines;
    private String cacheKey = "";

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

        // The field background is drawn on the sheet: the widget only paints the text.
        // PITFALL: every piece of game text is drawn with a drop shadow. On a light panel it
        // reads as a second letter offset by one pixel, and the input looks blurry. The field
        // background is therefore dark, and the text light, as in AE2.
        String previous = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer,
                guiLeft + ContainerPatTerminal.SEARCH_LEFT + 13,
                guiTop + ContainerPatTerminal.SEARCH_TOP + 2,
                // The text starts after the magnifier and stops before the cross, so it never
                // runs under either one.
                ContainerPatTerminal.SEARCH_WIDTH - 26, 8);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(64);
        search.setTextColor(COLOR_FIELD_TEXT);
        search.setText(previous);
        // The field takes focus right away: the player opens the terminal to search.
        search.setFocused(true);

        buttonList.clear();

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
        // The title now lives in the band, and stays there whatever the open tab. It used to
        // disappear the moment the Machines button took its place.
        fontRenderer.drawString(
                trim(I18n.format("gui.packagedautoterminals.pat_terminal"),
                        ContainerPatTerminal.SEARCH_LEFT - 6 - LIST_LEFT),
                LIST_LEFT, 7, COLOR_TEXT);

        drawTabs();
        drawTools();
        drawSearchDecoration();

        // The counts sit in the status bar, a shallow recess under the list.
        int summaryY = ContainerPatTerminal.STATUS_TOP + 2;

        // The rows are built before the summary: the result counter depends on them.
        List<Line> lines = lines();
        getScrollBar().setRange(0, Math.max(0, lines.size() - ROWS), 2);

        // The message takes the place of the summary for three seconds. The title line is
        // already taken by the tab and the search, and the game action bar is drawn below the
        // screen, hence out of sight.
        String message = currentMessage();
        // Four pixels of air inside the status bar. The text used to start on its border.
        int statusLeft = ContainerPatTerminal.STATUS_LEFT + 4;
        if (message != null) {
            fontRenderer.drawString(trim(message, ContainerPatTerminal.STATUS_WIDTH - 8),
                    statusLeft, summaryY, messageRefused ? COLOR_WARNING : COLOR_OK);
        } else {
            fontRenderer.drawString(summary(), statusLeft, summaryY, COLOR_TEXT);
            drawResultCount(lines, summaryY);
        }

        // E3: the cross only shows when there is something to clear.
        if (hasQuery()) {
            fontRenderer.drawString("×", CLEAR_LEFT + 2, CLEAR_TOP, COLOR_FIELD_TEXT);
        }

        if (lines.isEmpty()) {
            drawEmptyScreen();
            drawToolTooltip(mouseX - offsetX, mouseY - offsetY);
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
            drawLine(lines.get(first + row), LIST_TOP + row * ROW_HEIGHT, row == hoverRow);
        }

        drawToolTooltip(mouseX - offsetX, mouseY - offsetY);
        if (hoverRow >= 0 && first + hoverRow < lines.size()) {
            Line line = lines.get(first + hoverRow);
            int action = actionUnder(line, mouseX - offsetX, mouseY - offsetY);
            drawTooltip(mouseX - offsetX, mouseY - offsetY,
                    action < 0 ? tooltipFor(line) : actionTooltip(line, action));
        }
    }

    /**
     * The magnifier, and the ring that says the field has focus.
     *
     * <p>A dark rectangle said neither what the field was for nor whether it was listening.
     */
    private void drawSearchDecoration() {
        bindSheet();
        drawModalRectWithCustomSizedTexture(ContainerPatTerminal.SEARCH_LEFT + 3,
                ContainerPatTerminal.SEARCH_TOP + 2, SEARCH_ICON_U, GLYPH_V,
                SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SHEET, SHEET);
        if (search != null && search.isFocused()) {
            int left = ContainerPatTerminal.SEARCH_LEFT - 1;
            int top = ContainerPatTerminal.SEARCH_TOP - 1;
            int right = left + ContainerPatTerminal.SEARCH_WIDTH + 2;
            int bottom = top + ContainerPatTerminal.SEARCH_HEIGHT + 2;
            drawRect(left, top, right, top + 1, COLOR_FOCUS);
            drawRect(left, bottom - 1, right, bottom, COLOR_FOCUS);
            drawRect(left, top, left + 1, bottom, COLOR_FOCUS);
            drawRect(right - 1, top, right, bottom, COLOR_FOCUS);
            resetDrawState();
        }
    }

    /** Left edge of a tool button, by its index, counted from the right of the strip. */
    private static int toolLeft(int index) {
        return LIST_LEFT + LIST_WIDTH + 8 - (index + 1) * (TOOL_SIZE + 6);
    }

    private static int toolTop() {
        return ContainerPatTerminal.TAB_STRIP_TOP + (ContainerPatTerminal.TAB_HEIGHT - TOOL_SIZE) / 2;
    }

    /**
     * The two tool buttons of the strip: the sort order, and the filter.
     *
     * <p>On a network of twenty machines, reading every row to find the one that is wrong
     * costs more than the terminal saves. These two buttons do that reading.
     */
    private void drawTools() {
        int top = toolTop();
        if (onlyProblems) {
            drawRect(toolLeft(0) - 2, top - 2, toolLeft(0) + TOOL_SIZE + 2, top + TOOL_SIZE + 2,
                    COLOR_TOOL_ON);
            resetDrawState();
        }
        bindSheet();
        drawModalRectWithCustomSizedTexture(toolLeft(0), top, FILTER_U, GLYPH_V,
                TOOL_SIZE, TOOL_SIZE, SHEET, SHEET);
        drawModalRectWithCustomSizedTexture(toolLeft(1), top, SORT_U, GLYPH_V,
                TOOL_SIZE, TOOL_SIZE, SHEET, SHEET);
    }

    /** What the tool button under the pointer does. Nothing is drawn when there is none. */
    private void drawToolTooltip(int x, int y) {
        int tool = toolUnder(x, y);
        if (tool < 0) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(tool == 0
                ? I18n.format("gui.packagedautoterminals.filter_problems")
                : sortLabel());
        drawTooltip(x, y, lines);
    }

    /** Index of the tool button under these relative coordinates, or -1. */
    private int toolUnder(int x, int y) {
        int top = toolTop();
        if (y < top || y >= top + TOOL_SIZE) {
            return -1;
        }
        for (int index = 0; index < 2; index++) {
            if (x >= toolLeft(index) && x < toolLeft(index) + TOOL_SIZE) {
                return index;
            }
        }
        return -1;
    }

    /** Name of the order the sort button is on. */
    private String sortLabel() {
        if (sortMode == SORT_MACHINE) {
            return I18n.format("gui.packagedautoterminals.sort_machine");
        }
        if (sortMode == SORT_RECIPES) {
            return I18n.format("gui.packagedautoterminals.sort_recipes");
        }
        return I18n.format("gui.packagedautoterminals.sort_name");
    }

    /** Left edge of a tab, by its index. */
    private static int tabLeft(int index) {
        return LIST_LEFT + index * (ContainerPatTerminal.TAB_WIDTH + ContainerPatTerminal.TAB_GAP);
    }

    /** How many tabs the screen shows. The Machines tab can be turned off in the config. */
    private static int tabCount() {
        return PatConfig.machinesTab ? 2 : 1;
    }

    /**
     * Icon of a tab: a recipe holder for the patterns, a crafter for the machines.
     *
     * <p>Both are resolved once. One walks the item registry, the other reads the recipe type
     * registry, and the strip is drawn on every frame.
     */
    private ItemStack tabIcon(int index) {
        if (tabIcons == null) {
            Item holder = NetworkItems.findRecipeHolder();
            tabIcons = new ItemStack[] {
                holder == null ? ItemStack.EMPTY : new ItemStack(holder),
                CrafterTypes.iconFor(ContainerPatEditor.defaultRecipeType()),
            };
        }
        return tabIcons[index];
    }

    private static String tabName(int index) {
        return I18n.format(index == 0
                ? "gui.packagedautoterminals.tab_patterns"
                : "gui.packagedautoterminals.tab_machines");
    }

    /**
     * The tab strip.
     *
     * <p>A single button used to toggle the two views, and the title disappeared behind it.
     * Two tabs say what exists, which one is open, and leave the title alone. That is the AE2
     * device, and the vanilla one.
     */
    private void drawTabs() {
        for (int index = 0; index < tabCount(); index++) {
            boolean active = (index == 1) == machinesView;
            int left = tabLeft(index);
            bindSheet();
            drawModalRectWithCustomSizedTexture(left, ContainerPatTerminal.TAB_STRIP_TOP,
                    active ? TAB_ON_U : TAB_OFF_U, active ? TAB_ON_V : TAB_OFF_V,
                    ContainerPatTerminal.TAB_WIDTH, ContainerPatTerminal.TAB_HEIGHT,
                    SHEET, SHEET);

            // Eight pixels of air on the left, six between the icon and the label, eight
            // after it. The icon used to sit four pixels from the bevel, and the label four
            // from the icon, so the tab read as full even when it was mostly empty.
            ItemStack icon = tabIcon(index);
            int textLeft = left + 10;
            if (!icon.isEmpty()) {
                drawItem(left + 8, ContainerPatTerminal.TAB_STRIP_TOP + 3, icon);
                textLeft = left + 30;
            }
            fontRenderer.drawString(
                    trim(tabName(index), left + ContainerPatTerminal.TAB_WIDTH - 8 - textLeft),
                    textLeft,
                    // An eight pixel line centred in a twenty two pixel tab.
                    ContainerPatTerminal.TAB_STRIP_TOP + 7,
                    active ? COLOR_TEXT : COLOR_DIM);
        }
    }

    /** Index of the tab under these relative coordinates, or -1. */
    private int tabUnder(int x, int y) {
        if (y < ContainerPatTerminal.TAB_STRIP_TOP
                || y >= ContainerPatTerminal.TAB_STRIP_TOP + ContainerPatTerminal.TAB_HEIGHT) {
            return -1;
        }
        for (int index = 0; index < tabCount(); index++) {
            int left = tabLeft(index);
            if (x >= left && x < left + ContainerPatTerminal.TAB_WIDTH) {
                return index;
            }
        }
        return -1;
    }

    /**
     * A state chip, right aligned, with its text inside.
     *
     * <p>PITFALL, already paid for twice in this project: {@code drawRect} leaves the colour
     * of {@code GlStateManager} set to the one it was given, and ends with {@code disableBlend}.
     * The next textured draw then disappears, or draws its transparent pixels black. The chip
     * therefore puts both back before it returns.
     */
    private void drawChip(String text, int right, int y, boolean warning) {
        int width = fontRenderer.getStringWidth(text);
        int left = right - width - 8;
        int top = y + 2;
        int bottom = y + 16;
        drawRect(left, top, right, bottom, warning ? CHIP_WARNING_FILL : CHIP_FILL);
        int edge = warning ? CHIP_WARNING_EDGE : CHIP_EDGE;
        drawRect(left, top, right, top + 1, edge);
        drawRect(left, bottom - 1, right, bottom, edge);
        drawRect(left, top, left + 1, bottom, edge);
        drawRect(right - 1, top, right, bottom, edge);
        fontRenderer.drawString(text, left + 4, y + TEXT_OFFSET,
                warning ? COLOR_WARNING : COLOR_TEXT);
        resetDrawState();
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

    /**
     * What the screen shows when it lists nothing.
     *
     * <p>Two causes, two messages: the network holds no machine, or the search matches none.
     * Mixing them up sent the player checking their cables for nothing. Each one now carries
     * a second line that says what to do next, under a faded drawing.
     */
    private void drawEmptyScreen() {
        boolean filtered = onlyProblems
                || (search != null && !search.getText().trim().isEmpty());
        int middle = LIST_LEFT + LIST_WIDTH / 2;
        int top = LIST_TOP + (ROWS * ROW_HEIGHT) / 2 - 34;

        bindSheet();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.45F);
        drawModalRectWithCustomSizedTexture(middle - EMPTY_SIZE / 2, top,
                EMPTY_U, EMPTY_V, EMPTY_SIZE, EMPTY_SIZE, SHEET, SHEET);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawCentred(I18n.format(filtered
                ? "gui.packagedautoterminals.no_result"
                : "gui.packagedautoterminals.empty"), middle, top + EMPTY_SIZE + 8, COLOR_TEXT);
        drawCentred(I18n.format(filtered
                ? "gui.packagedautoterminals.no_result_hint"
                : "gui.packagedautoterminals.empty_hint"),
                middle, top + EMPTY_SIZE + 22, COLOR_DIM);
    }

    /** One line of text, centred on an abscissa, trimmed to the list. */
    private void drawCentred(String text, int middle, int y, int color) {
        String shown = trim(text, LIST_WIDTH - 16);
        fontRenderer.drawString(shown, middle - fontRenderer.getStringWidth(shown) / 2, y, color);
    }

    /**
     * Puts the drawing state back after a {@code drawRect}.
     *
     * <p>PITFALL, paid for twice in this project: {@code drawRect} leaves the colour of
     * {@code GlStateManager} set to the one it was given, and ends with {@code disableBlend}.
     * The next textured draw then vanishes, or paints its transparent pixels black.
     */
    private void resetDrawState() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void drawLine(Line line, int y, boolean hovered) {
        if (line.warning != null) {
            String text = trim(line.warning, LIST_WIDTH - 8);
            fontRenderer.drawString(text, LIST_LEFT + 4, y + TEXT_OFFSET,
                    line.section ? COLOR_TEXT : COLOR_WARNING);
            if (line.section) {
                // A rule from the end of the title to the right edge. It closes the heading,
                // and the eye then reads the rows under it as one block.
                int from = LIST_LEFT + 8 + fontRenderer.getStringWidth(text);
                drawRect(from, y + 9, LIST_LEFT + LIST_WIDTH - 4, y + 10, COLOR_RULE);
                resetDrawState();
            }
            return;
        }

        if (line.crafter != null) {
            drawItem(LIST_LEFT + 2, y + 1, line.crafter.icon);
            String state = crafterState(line);
            boolean warning = crafterStateColor(line) == COLOR_WARNING;
            drawChip(state, LIST_LEFT + LIST_WIDTH - 4, y, warning);
            fontRenderer.drawString(
                    trim(line.crafter.name,
                            budget(LIST_LEFT + 22, state, LIST_LEFT + LIST_WIDTH - 10)),
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
                    trim(groupTitle(line.group), budget(GROUP_TEXT, state, stateRight - 6)),
                    GROUP_TEXT, y + TEXT_OFFSET, COLOR_TEXT);
            if (hovered) {
                // The buttons take the place of the chip. The state is in the tooltip anyway,
                // and a row that grew a button would push every column sideways.
                //
                // One button, two directions. A group that lacks a holder wants one; a group
                // that has them all can give them back. The arrow says which way it goes.
                bindSheet();
                drawModalRectWithCustomSizedTexture(stateRight - ACTION_SIZE, y + 4,
                        needsHolder(line.group) ? INSERT_U : EJECT_U, GLYPH_V,
                        ACTION_SIZE, ACTION_SIZE, SHEET, SHEET);
            } else {
                drawChip(state, stateRight, y,
                        ProviderPairing.missingRoleOf(line.group) != null);
            }

            if (!machine.isEmpty()) {
                drawItem(GROUP_MACHINE_LEFT, y + 1, machine);
            }

            // The sheet is bound for the icon, then text rendering takes over again.
            bindSheet();
            drawModalRectWithCustomSizedTexture(LOCATE_LEFT, y + 3, LOCATE_U, LOCATE_V,
                    LOCATE_SIZE, LOCATE_SIZE, SHEET, SHEET);
            // A rule under the header: the rows below belong to it.
            drawRect(LIST_LEFT + 2, y + ROW_HEIGHT - 1, LIST_LEFT + LIST_WIDTH - 2,
                    y + ROW_HEIGHT, COLOR_RULE);
            resetDrawState();
            return;
        }

        // A guide from the group header down to its last recipe. The chevron used to be the
        // only sign that these rows belonged to the row above.
        drawRect(LIST_LEFT + 15, y, LIST_LEFT + 16, y + ROW_HEIGHT, COLOR_RULE);
        resetDrawState();

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
        int nameRight = hovered ? rightEdge - 2 * (ACTION_SIZE + 4) : rightEdge;
        fontRenderer.drawString(typeName,
                nameRight - fontRenderer.getStringWidth(typeName), y + TEXT_OFFSET,
                complete ? COLOR_DIM : COLOR_WARNING);
        int textWidth = budget(textLeft, typeName, nameRight);

        fontRenderer.drawString(trim(output, textWidth), textLeft, y + TEXT_OFFSET,
                complete ? COLOR_TEXT : COLOR_WARNING);

        if (hovered) {
            // Edit and remove, the two gestures the row answers. They were written nowhere on
            // the screen: the player had to read the wiki to learn them.
            bindSheet();
            drawModalRectWithCustomSizedTexture(rightEdge - 2 * ACTION_SIZE - 4, y + 4,
                    PENCIL_U, GLYPH_V, ACTION_SIZE, ACTION_SIZE, SHEET, SHEET);
            drawModalRectWithCustomSizedTexture(rightEdge - ACTION_SIZE, y + 4,
                    CROSS_U, GLYPH_V, ACTION_SIZE, ACTION_SIZE, SHEET, SHEET);
        }
    }

    /** Action buttons of a row. They only exist while the pointer is on it. */
    private static final int BUTTON_EJECT = 0;
    private static final int BUTTON_EDIT = 1;
    private static final int BUTTON_REMOVE = 2;

    /**
     * Which action button sits under these relative coordinates, or -1.
     *
     * <p>The caller must already know that the pointer is on this row.
     */
    private int actionUnder(Line line, int x, int y) {
        int row = rowUnder(x, y);
        if (row < 0) {
            return -1;
        }
        int top = LIST_TOP + row * ROW_HEIGHT + 4;
        if (y < top || y >= top + ACTION_SIZE) {
            return -1;
        }
        int rightEdge = LIST_LEFT + LIST_WIDTH - 4;
        if (line.isGroupHeader()) {
            ItemStack machine = CrafterTypes.iconFor(groupMachineType(line.group));
            int stateRight = machine.isEmpty() ? LOCATE_LEFT - 4 : GROUP_MACHINE_LEFT - 4;
            return x >= stateRight - ACTION_SIZE && x < stateRight ? BUTTON_EJECT : -1;
        }
        if (line.recipe == null) {
            return -1;
        }
        if (x >= rightEdge - 2 * ACTION_SIZE - 4 && x < rightEdge - ACTION_SIZE - 4) {
            return BUTTON_EDIT;
        }
        return x >= rightEdge - ACTION_SIZE && x < rightEdge ? BUTTON_REMOVE : -1;
    }

    /**
     * Does a machine of this group still wait for a recipe holder?
     *
     * <p>One machine without a holder is enough: the group cannot run until it has one, and
     * giving is then more useful than taking back.
     */
    private boolean needsHolder(ProviderPairing.Group group) {
        for (ProviderSnapshot machine : group.machines) {
            if (!machine.holderPresent) {
                return true;
            }
        }
        return false;
    }

    /** What a button of the hovered row does, in one line. */
    private List<String> actionTooltip(Line line, int action) {
        List<String> lines = new ArrayList<>();
        if (action == BUTTON_EJECT) {
            lines.add(I18n.format(needsHolder(line.group)
                    ? "gui.packagedautoterminals.insert_holder_hint"
                    : "gui.packagedautoterminals.remove_holder_hint"));
        } else if (action == BUTTON_EDIT) {
            lines.add(I18n.format("gui.packagedautoterminals.edit_hint"));
        } else {
            lines.add(I18n.format("gui.packagedautoterminals.delete_hint"));
        }
        return lines;
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
     * The right-hand text of a crafter row.
     *
     * <p>It used to read "idle" almost always, which taught the player nothing.
     * {@code TileCrafter.isBusy} answers true while a craft runs, or while items sit in the
     * input slots, and a craft lasts a handful of ticks. The terminal samples that state once
     * a second, so a healthy network read "idle" from top to bottom.
     *
     * <p>The row now says what the player can act on. A machine with no channel does nothing,
     * in silence. A machine that no encoded recipe targets is hardware placed for nothing,
     * which is the mirror of the orphan report above the list. Everything else is a count.
     */
    private String crafterState(Line line) {
        if (!line.crafter.active) {
            return I18n.format("gui.packagedautoterminals.inactive");
        }
        if (line.crafter.busy) {
            return I18n.format("gui.packagedautoterminals.busy");
        }
        if (line.crafterRecipes == 0) {
            return I18n.format("gui.packagedautoterminals.crafter_unused");
        }
        return I18n.format(line.crafterRecipes == 1
                ? "gui.packagedautoterminals.recipe"
                : "gui.packagedautoterminals.recipes", line.crafterRecipes);
    }

    /** Red when the player has something to fix, dim otherwise. */
    private int crafterStateColor(Line line) {
        return !line.crafter.active || (!line.crafter.busy && line.crafterRecipes == 0)
                ? COLOR_WARNING
                : COLOR_DIM;
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
                ContainerPatTerminal.STATUS_LEFT + ContainerPatTerminal.STATUS_WIDTH - 4
                        - fontRenderer.getStringWidth(text), y, COLOR_DIM);
    }

    /** The click of a vanilla button, so a tab and a tool answer like the rest of the game. */
    private void clickSound() {
        mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord
                .getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** Does the search carry any text? */
    private boolean hasQuery() {
        return search != null && !search.getText().isEmpty();
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
            // FIXED: this test knew two words while the editor knew five, so the refusals of
            // the terminal itself, insert_holder_first and group_full, were drawn green.
            messageRefused = Feedback.isRefusal(terminalContainer.feedback);
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
        int tool = toolUnder(mouseX - guiLeft, mouseY - guiTop);
        if (tool >= 0) {
            if (tool == 0) {
                onlyProblems = !onlyProblems;
            } else {
                sortMode = (sortMode + 1) % SORT_COUNT;
            }
            getScrollBar().setRange(0, 0, 2);
            clickSound();
            return;
        }

        int tab = tabUnder(mouseX - guiLeft, mouseY - guiTop);
        if (tab >= 0) {
            boolean wanted = tab == 1;
            if (wanted != machinesView) {
                machinesView = wanted;
                // GuiScrollbar has no setter for the position. Collapsing the range to
                // zero clamps it, and drawFG gives the bar its real range on the next frame.
                getScrollBar().setRange(0, 0, 2);
                clickSound();
            }
            return;
        }
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
        // The buttons of the hovered row come before the row itself: both answer the same
        // click, and the button is the one the player aimed at.
        if (clicked != null && clicked.group != null) {
            int action = actionUnder(clicked, mouseX - guiLeft, mouseY - guiTop);
            if (action == BUTTON_EJECT) {
                send(clicked.anchor(), -1, needsHolder(clicked.group)
                        ? PacketRecipeAction.ACTION_INSERT_HOLDER
                        : PacketRecipeAction.ACTION_REMOVE_HOLDER);
                return;
            }
            if (action == BUTTON_EDIT) {
                send(clicked.anchor(), clicked.recipeIndex, PacketRecipeAction.ACTION_EDIT);
                return;
            }
            if (action == BUTTON_REMOVE) {
                send(clicked.anchor(), clicked.recipeIndex, PacketRecipeAction.ACTION_REMOVE);
                return;
            }
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
        List<Line> lines = lines();
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

    /**
     * Readable summary of what the open tab shows.
     *
     * <p>FIXED: the same line was drawn on both tabs, and it always counted the Packagers,
     * the Unpackagers and the Packaging Providers. On the Machines tab it therefore announced
     * twenty two machines under a list of eleven crafters, which are not the same machines at
     * all.
     */
    private String summary() {
        if (machinesView) {
            int crafters = terminalContainer.machines.size();
            return I18n.format(crafters == 1
                    ? "gui.packagedautoterminals.summary_machine"
                    : "gui.packagedautoterminals.summary_machines", crafters);
        }
        return networkSummary();
    }

    /** Providing machines and encoded recipes, for the Patterns tab. */
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
    /**
     * The rows to draw, rebuilt only when their sources have changed.
     *
     * <p>The key names every source: the snapshot counter of the container, the search text,
     * the open tab, and the unfolded groups. {@code expanded} holds the position of each
     * unfolded group, so its hash code changes as soon as one is folded or unfolded.
     */
    private List<Line> lines() {
        String key = terminalContainer.revision
                + "|" + (search == null ? "" : search.getText())
                + "|" + machinesView + "|" + sortMode + "|" + onlyProblems
                + "|" + expanded.size() + ":" + expanded.hashCode();
        if (cachedLines == null || !key.equals(cacheKey)) {
            cachedLines = buildLines();
            cacheKey = key;
        }
        return cachedLines;
    }

    private List<Line> buildLines() {
        SearchQuery query = SearchQuery.parse(search == null ? "" : search.getText());
        return machinesView ? buildMachineLines(query) : buildPatternLines(query);
    }

    /**
     * Machines tab: the crafters of the network, preceded by the orphan recipes.
     *
     * <p>A recipe is an orphan when no crafter on the network can run its type. That is the
     * most frequent mistake in game, and no other mod reports it.
     */
    private List<Line> buildMachineLines(SearchQuery query) {
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

        // How many encoded recipes each kind of machine can run. The table is the one the
        // orphan report reads: a recipe type names the class of the machine that runs it.
        // Recipes are counted once per group, as everywhere else: a pair carries the same
        // recipe on both sides.
        Map<String, Integer> recipesByMachine = new HashMap<>();
        for (ProviderPairing.Group group
                : ProviderPairing.group(terminalContainer.providers)) {
            for (IRecipeInfo recipe : group.recipes) {
                IRecipeType type = recipe.getRecipeType();
                if (!CrafterTypes.isDiagnosable(type)) {
                    continue;
                }
                String machineClass = CrafterTypes.machineClassFor(type);
                Integer known = recipesByMachine.get(machineClass);
                recipesByMachine.put(machineClass, known == null ? 1 : known + 1);
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
            Line line = Line.crafter(machine);
            Integer count = recipesByMachine.get(machine.machineClass);
            line.crafterRecipes = count == null ? 0 : count;
            if (onlyProblems && machine.active && line.crafterRecipes > 0) {
                continue;
            }
            if (CrafterTypes.isRouter(machine.machineClass)) {
                routers.add(line);
            } else {
                crafters.add(line);
            }
        }

        sortCrafters(crafters);
        sortCrafters(routers);

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
    private List<Line> buildPatternLines(SearchQuery query) {
        List<Line> lines = new ArrayList<>();
        boolean searching = !query.isEmpty();

        List<ProviderPairing.Group> groups =
                ProviderPairing.group(terminalContainer.providers);
        sortGroups(groups);

        for (ProviderPairing.Group group : groups) {
            if (onlyProblems && !hasProblem(group)) {
                continue;
            }
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

    /**
     * Does this group need the player?
     *
     * <p>Three cases, and only three: a role is missing, a machine has no channel, or a
     * recipe sits on one side of the pair only. A group with no recipe at all is a group the
     * player has not started yet, which is not a fault.
     */
    private boolean hasProblem(ProviderPairing.Group group) {
        if (ProviderPairing.missingRoleOf(group) != null) {
            return true;
        }
        for (ProviderSnapshot machine : group.machines) {
            if (!machine.active) {
                return true;
            }
        }
        for (IRecipeInfo recipe : group.recipes) {
            if (!group.isComplete(recipe)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Orders the groups.
     *
     * <p>By name is the order a player reads. By machine gathers the groups that feed the same
     * crafter, which is how a factory is laid out. By recipe count puts the busiest first.
     * Every order ends on the name, so the list never shuffles between two refreshes.
     */
    private void sortGroups(List<ProviderPairing.Group> groups) {
        final int mode = sortMode;
        Collections.sort(groups, new Comparator<ProviderPairing.Group>() {
            @Override
            public int compare(ProviderPairing.Group left, ProviderPairing.Group right) {
                if (mode == SORT_RECIPES && left.recipes.size() != right.recipes.size()) {
                    return right.recipes.size() - left.recipes.size();
                }
                if (mode == SORT_MACHINE) {
                    int order = machineName(left).compareToIgnoreCase(machineName(right));
                    if (order != 0) {
                        return order;
                    }
                }
                return groupTitle(left).compareToIgnoreCase(groupTitle(right));
            }
        });
    }

    /** Name of the crafting machine of a group, or an empty string when it has none. */
    private String machineName(ProviderPairing.Group group) {
        ItemStack machine = CrafterTypes.iconFor(groupMachineType(group));
        return machine.isEmpty() ? "" : machine.getDisplayName();
    }

    /** Orders the crafters. By machine reads as by name here: the machine is the name. */
    private void sortCrafters(List<Line> crafters) {
        final int mode = sortMode;
        Collections.sort(crafters, new Comparator<Line>() {
            @Override
            public int compare(Line left, Line right) {
                if (mode == SORT_RECIPES && left.crafterRecipes != right.crafterRecipes) {
                    return right.crafterRecipes - left.crafterRecipes;
                }
                return left.crafter.name.compareToIgnoreCase(right.crafter.name);
            }
        });
    }

    /** Does a recipe satisfy **all** the criteria? */
    private boolean matches(IRecipeInfo recipe, SearchQuery query) {
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

    private boolean matchesText(SearchQuery query, String name) {
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
        /**
         * Encoded recipes of the network that this machine can run.
         *
         * <p>It is computed while the rows are built, and not while they are drawn. The count
         * walks the groups, and the drawing runs on every frame.
         */
        int crafterRecipes;

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
