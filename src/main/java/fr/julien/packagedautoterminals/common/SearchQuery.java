package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A search typed in the terminal, split into criteria.
 *
 * <p>Three prefixes, taken from the AE2 and JEI terminals: {@code @} targets the source mod,
 * {@code #} targets the recipe type, and the rest is searched in the names. All criteria must
 * be satisfied at once.
 *
 * <p>The class carries no Minecraft type, so it is tested without the game. The matching
 * itself stays in the screen: it reads item names, which only the client knows.
 */
public final class SearchQuery {

    /** Plain words, searched in the displayed names. */
    public final List<String> text = new ArrayList<>();
    /** Words after {@code @}, matched against the source mod. */
    public final List<String> mods = new ArrayList<>();
    /** Words after {@code #}, matched against the recipe type. */
    public final List<String> types = new ArrayList<>();

    private SearchQuery() {}

    /**
     * Reads a raw search field.
     *
     * <p>A lone prefix, such as {@code @} with nothing behind it, is a search being typed. It
     * carries no criterion, and is dropped: keeping it would empty the list under the hands
     * of the player.
     */
    public static SearchQuery parse(String raw) {
        SearchQuery query = new SearchQuery();
        if (raw == null) {
            return query;
        }
        for (String token : raw.trim().toLowerCase(Locale.ROOT).split(" +")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith("@") && token.length() > 1) {
                query.mods.add(token.substring(1));
            } else if (token.startsWith("#") && token.length() > 1) {
                query.types.add(token.substring(1));
            } else if (!token.equals("@") && !token.equals("#")) {
                query.text.add(token);
            }
        }
        return query;
    }

    public boolean isEmpty() {
        return text.isEmpty() && mods.isEmpty() && types.isEmpty();
    }
}
