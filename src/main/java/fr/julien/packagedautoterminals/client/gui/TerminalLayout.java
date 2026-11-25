package fr.julien.packagedautoterminals.client.gui;

/**
 * Geometry of the terminal screen. These values must stay identical to the ones in
 * tools/make_gui_texture.py, which draws the sheet.
 *
 * <p>The container keeps only the position of the player inventory, which places its slots.
 */
public final class TerminalLayout {

    private TerminalLayout() {}

    /** Screen width. */
    public static final int WIDTH = 320;
    /**
     * Visible rows. The sheet draws sixteen: this number can grow without redrawing it.
     *
     * <p>Ten keeps the screen at 331 pixels, under the 360 a 1080 pixel display gives at GUI
     * scale 3. Twelve made it 367, and {@code GuiScaleFit} dropped that display to scale 2.
     */
    public static final int ROWS = 10;
    /** Rows drawn in the sheet. Used to locate the bottom of the screen. */
    public static final int SHEET_ROWS = 16;
    /** Height of the screen footer: status bar, inventory, margin. */
    public static final int FOOTER = 104;
    /**
     * Title band: it carries the screen name and the search field.
     *
     * <p>Twenty rows, not eighteen. The search field takes fourteen with its border, and the
     * frame bevel of the window owns the first three rows of the screen. At eighteen the
     * field had to start on that bevel, and its top edge was drawn outside the band.
     */
    public static final int BAND_HEIGHT = 20;
    /**
     * Tab strip, under the band.
     *
     * <p>Two pixels of panel separate it from the band, and three from the list. Without them
     * the three bands read as one block, and the tabs looked glued to the title.
     */
    public static final int TAB_STRIP_TOP = BAND_HEIGHT + 2;
    public static final int TAB_HEIGHT = 22;
    public static final int TAB_WIDTH = 92;
    /** Gap between two tabs. Two pixels read as one wide tab cut in half. */
    public static final int TAB_GAP = 4;
    /** Height of one row, in pixels. */
    public static final int ROW_HEIGHT = 18;
    /** Left edge of the list area. */
    public static final int LIST_LEFT = 8;
    /** Top of the list area, under the band and the tab strip. */
    public static final int LIST_TOP = TAB_STRIP_TOP + TAB_HEIGHT + 3;
    /** Width of the list area. */
    public static final int LIST_WIDTH = 288;
    /** Screen height, derived from the row count. */
    public static final int HEIGHT = LIST_TOP + ROWS * ROW_HEIGHT + FOOTER;
    /** Left edge of the scrollbar. */
    public static final int SCROLL_LEFT = 300;
    /** Status bar: the counts, in a shallow recess under the list. */
    public static final int STATUS_TOP = LIST_TOP + ROWS * ROW_HEIGHT + 4;
    public static final int STATUS_HEIGHT = 12;
    public static final int STATUS_WIDTH = 304;
    /** The bar spans the list and the scrollbar, so it starts where the list starts. */
    public static final int STATUS_LEFT = LIST_LEFT;
    /** Search field, on the title line. */
    public static final int SEARCH_LEFT = 150;
    public static final int SEARCH_TOP = 5;
    public static final int SEARCH_WIDTH = 146;
    public static final int SEARCH_HEIGHT = 12;
}
