package fr.julien.packagedautoterminals.container;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The terminal fits a 1920 by 1080 screen at GUI scale 3, by the rule of
 * {@code GuiScaleFit.fits}. At twelve rows it was 367 pixels tall, and the screen dropped to
 * scale 2 on the most common display.
 */
public class TerminalSizeTest {

    @Test
    public void theTerminalFitsA1080pScreenAtGuiScaleThree() {
        int factor = 3;
        assertTrue("width " + ContainerPatTerminal.WIDTH,
                1920 / factor >= ContainerPatTerminal.WIDTH);
        assertTrue("height " + ContainerPatTerminal.HEIGHT,
                1080 / factor >= ContainerPatTerminal.HEIGHT);
    }
}
