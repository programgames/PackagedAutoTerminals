package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Messages travel as one string, key and arguments together.
 *
 * <p>Two releases were spent on this class. Version 0.1.0-beta.5 printed "Format error"
 * instead of every count, because an argument came back as text where the language file
 * expects a whole number.
 */
public class FeedbackTest {

    @Test
    public void aMessageWithNoArgumentIsItsOwnKey() {
        String packed = Feedback.pack("gui.packagedautoterminals.write_failed");
        assertEquals("gui.packagedautoterminals.write_failed", Feedback.key(packed));
        assertEquals(0, Feedback.arguments(packed).length);
    }

    @Test
    public void aCountComesBackAsANumberAndNotAsText() {
        // String.format("%d", "2") throws, Forge catches it, and the line becomes
        // "Format error: ...". Every count of the mod went through that path.
        String packed = Feedback.pack("gui.packagedautoterminals.applied_to", 2);
        assertArrayEquals(new Object[] {2L}, Feedback.arguments(packed));
    }

    @Test
    public void severalArgumentsKeepTheirOrder() {
        String packed = Feedback.pack("gui.packagedautoterminals.applied_partial", 3, 1);
        assertArrayEquals(new Object[] {3L, 1L}, Feedback.arguments(packed));
    }

    @Test
    public void aWordStaysAWord() {
        String packed = Feedback.pack("gui.packagedautoterminals.orphan", "Ultimate");
        assertArrayEquals(new Object[] {"Ultimate"}, Feedback.arguments(packed));
    }

    @Test
    public void aRefusalIsRecognisedByItsWholeKey() {
        assertTrue(Feedback.isRefusal(
                Feedback.pack("gui.packagedautoterminals.group_full", 20)));
        assertTrue(Feedback.isRefusal(
                Feedback.pack("gui.packagedautoterminals.applied_partial_full", 1, 1)));
    }

    @Test
    public void aSuccessIsNotARefusal() {
        assertFalse(Feedback.isRefusal(
                Feedback.pack("gui.packagedautoterminals.applied_to", 2)));
        assertFalse(Feedback.isRefusal(
                Feedback.pack("gui.packagedautoterminals.applied_with_holder", 2, 1)));
    }

    @Test
    public void theTestOnARefusalReadsTheKeyAndNotAPieceOfIt() {
        // The screens used to test substrings. "applied_no_partner" matched "no_" and turned
        // red, while "applied_partial" stayed green, although both report the same thing.
        assertFalse(Feedback.isRefusal("gui.packagedautoterminals.no_result"));
        assertTrue(Feedback.isRefusal("gui.packagedautoterminals.applied_no_partner"));
    }
}
