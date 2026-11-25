package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The one message a group write sends back, chosen from its {@link RecipeWriter.Result}. */
public class WriteFeedbackTest {

    private static final String PREFIX = "gui.packagedautoterminals.";

    private static RecipeWriter.Result result(int changed, int withoutHolder, int full,
                                              int supplied) {
        RecipeWriter.Result result = new RecipeWriter.Result();
        result.changed = changed;
        result.withoutHolder = withoutHolder;
        result.full = full;
        result.supplied = supplied;
        return result;
    }

    private static String message(String key, Object... arguments) {
        return Feedback.pack(PREFIX + key, arguments);
    }

    @Test
    public void nothingChangedIsAFailureWhateverElseHappened() {
        assertEquals(message("write_failed"), result(0, 0, 0, 0).feedback(false));
        assertEquals(message("write_failed"), result(0, 2, 1, 0).feedback(true));
    }

    @Test
    public void onePlainSuccessUsesTheSingular() {
        assertEquals(message("applied_to_one", 1), result(1, 0, 0, 0).feedback(false));
    }

    @Test
    public void severalPlainSuccessesUseThePlural() {
        assertEquals(message("applied_to", 2), result(2, 0, 0, 0).feedback(false));
    }

    @Test
    public void aProviderWithoutHolderWinsOverEverythingElse() {
        assertEquals(message("applied_partial", 1, 1), result(1, 1, 1, 1).feedback(true));
    }

    @Test
    public void aFullHolderWinsOverAMissingPartnerAndASuppliedHolder() {
        assertEquals(message("applied_partial_full", 1, 1), result(1, 0, 1, 1).feedback(true));
    }

    @Test
    public void aMissingPartnerWinsOverASuppliedHolder() {
        assertEquals(message("applied_no_partner", 1), result(1, 0, 0, 1).feedback(true));
    }

    @Test
    public void aLonePartnerFoundForTheMissingRoleIsAPlainSuccess() {
        assertEquals(message("applied_to", 2), result(2, 0, 0, 0).feedback(true));
    }

    @Test
    public void aSuppliedHolderIsAnnouncedWithItsCount() {
        assertEquals(message("applied_with_holder", 2, 1), result(2, 0, 0, 1).feedback(false));
    }

    @Test
    public void everyHalfDoneWriteIsDrawnAsARefusalAndEveryFullOneAsASuccess() {
        assertTrue(Feedback.isRefusal(result(0, 0, 0, 0).feedback(false)));
        assertTrue(Feedback.isRefusal(result(1, 1, 0, 0).feedback(false)));
        assertTrue(Feedback.isRefusal(result(1, 0, 1, 0).feedback(false)));
        assertTrue(Feedback.isRefusal(result(1, 0, 0, 0).feedback(true)));
        assertFalse(Feedback.isRefusal(result(1, 0, 0, 1).feedback(false)));
        assertFalse(Feedback.isRefusal(result(1, 0, 0, 0).feedback(false)));
    }
}
