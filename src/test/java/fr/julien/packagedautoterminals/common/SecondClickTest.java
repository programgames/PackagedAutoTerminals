package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SecondClickTest {

    private final SecondClick click = new SecondClick(3_000L);

    @Test
    public void theFirstClickOnlyAsks() {
        assertFalse(click.confirms("a", 0L));
    }

    @Test
    public void aSecondClickOnTheSameTargetConfirms() {
        click.confirms("a", 0L);
        assertTrue(click.confirms("a", 3_000L));
    }

    @Test
    public void aSecondClickTooLateAsksAgain() {
        click.confirms("a", 0L);
        assertFalse(click.confirms("a", 3_001L));
        assertTrue(click.confirms("a", 3_002L));
    }

    @Test
    public void anotherTargetAsksAgain() {
        click.confirms("a", 0L);
        assertFalse(click.confirms("b", 1L));
        assertFalse(click.confirms("a", 2L));
    }

    @Test
    public void aConfirmedClickDoesNotConfirmTheNextOne() {
        click.confirms("a", 0L);
        click.confirms("a", 1L);
        assertFalse(click.confirms("a", 2L));
    }
}
