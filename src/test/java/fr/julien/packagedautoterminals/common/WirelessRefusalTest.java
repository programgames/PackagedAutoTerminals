package fr.julien.packagedautoterminals.common;

import static fr.julien.packagedautoterminals.common.TerminalContext.wirelessRefusal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * The first failing question names the refusal. An unlinked terminal is also out of range, and
 * out of range it cannot ask the network for BUILD: the earlier question must win.
 */
public class WirelessRefusalTest {

    private static final String PREFIX = "gui.packagedautoterminals.";

    @Test
    public void anUnlinkedTerminalSaysSoWhateverElseFails() {
        assertEquals(PREFIX + "not_linked", wirelessRefusal(false, false, false, false));
    }

    @Test
    public void rangeComesBeforePermissionAndPower() {
        assertEquals(PREFIX + "out_of_range", wirelessRefusal(true, false, false, false));
    }

    @Test
    public void permissionComesBeforePower() {
        assertEquals(PREFIX + "no_permission", wirelessRefusal(true, true, false, false));
        assertEquals(PREFIX + "no_power", wirelessRefusal(true, true, true, false));
    }

    @Test
    public void nothingFailsNothingRefuses() {
        assertNull(wirelessRefusal(true, true, true, true));
    }
}
