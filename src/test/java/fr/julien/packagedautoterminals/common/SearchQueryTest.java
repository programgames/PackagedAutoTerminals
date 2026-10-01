package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** The search field, split into criteria. No Minecraft class takes part. */
public class SearchQueryTest {

    @Test
    public void anEmptyFieldCarriesNoCriterion() {
        assertTrue(SearchQuery.parse("").isEmpty());
        assertTrue(SearchQuery.parse("   ").isEmpty());
        assertTrue(SearchQuery.parse(null).isEmpty());
    }

    @Test
    public void plainWordsGoToTheText() {
        SearchQuery query = SearchQuery.parse("iron ingot");
        assertEquals(Arrays.asList("iron", "ingot"), query.text);
        assertEquals(Collections.emptyList(), query.mods);
        assertEquals(Collections.emptyList(), query.types);
    }

    @Test
    public void theAtSignTargetsTheMod() {
        SearchQuery query = SearchQuery.parse("@thermal");
        assertEquals(Collections.singletonList("thermal"), query.mods);
        assertTrue(query.text.isEmpty());
    }

    @Test
    public void theHashSignTargetsTheRecipeType() {
        SearchQuery query = SearchQuery.parse("#processing");
        assertEquals(Collections.singletonList("processing"), query.types);
        assertTrue(query.text.isEmpty());
    }

    @Test
    public void theThreeKindsOfCriteriaMixInOneSearch() {
        SearchQuery query = SearchQuery.parse("plate @thermal #crafting steel");
        assertEquals(Arrays.asList("plate", "steel"), query.text);
        assertEquals(Collections.singletonList("thermal"), query.mods);
        assertEquals(Collections.singletonList("crafting"), query.types);
    }

    @Test
    public void theSearchIgnoresTheCaseAndTheExtraSpaces() {
        SearchQuery query = SearchQuery.parse("  IRON   @AE2  ");
        assertEquals(Collections.singletonList("iron"), query.text);
        assertEquals(Collections.singletonList("ae2"), query.mods);
    }

    @Test
    public void aLonePrefixIsASearchBeingTypedAndCarriesNothing() {
        // The player has typed the prefix and not the word yet. Treating it as a plain word
        // would empty the list under their hands.
        assertTrue(SearchQuery.parse("@").isEmpty());
        assertTrue(SearchQuery.parse("#").isEmpty());
    }
}
