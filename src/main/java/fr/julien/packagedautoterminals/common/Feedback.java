package fr.julien.packagedautoterminals.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Short message sent back to the player, shown **inside** the screen.
 *
 * <p>The game action bar did not fit: it is drawn below the screen, and the message went
 * unnoticed there.
 *
 * <p>The text travels as a translation key, followed by its arguments. Formatting happens on
 * the client, in its own language.
 */
public final class Feedback {

    /**
     * Separator between the key and its arguments. No text contains it.
     *
     * <p>It is written as an escape, not as the raw byte. The raw byte is invisible in an editor,
     * survives no copy and paste, and any tool that trims control characters would break every
     * message of the mod without a visible trace.
     */
    private static final String SEPARATOR = "\u0001";

    /**
     * Keys that report a refusal or an incomplete result. The screens draw them in the warning
     * colour.
     *
     * <p>PITFALL, fixed. Each screen used to carry its own list of **substrings**, and the two had
     * already drifted: the terminal tested two words, the editor five. Its own refusals, such as
     * {@code insert_holder_first} and {@code group_full}, were therefore drawn in the colour of a
     * success. A substring also catches by accident: {@code applied_no_partner} matched
     * {@code no_} and turned red while {@code applied_partial} stayed green, although both report
     * the same kind of half done write.
     *
     * <p>The list is now exact, and lives in one place. **A new refusal key must be added here**,
     * or it will be drawn as a success.
     *
     * <p>The two {@code applied_} entries are not refusals: the write did happen. They are listed
     * because they say that a machine was left behind, and the player has to act.
     */
    private static final Set<String> REFUSALS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(
                    "gui.packagedautoterminals.insert_holder_first",
                    "gui.packagedautoterminals.no_holder_here",
                    "gui.packagedautoterminals.network_full",
                    "gui.packagedautoterminals.group_full",
                    "gui.packagedautoterminals.write_failed",
                    "gui.packagedautoterminals.group_changed",
                    "gui.packagedautoterminals.nothing_to_delete",
                    "gui.packagedautoterminals.unsaved",
                    "gui.packagedautoterminals.ratio_not_exact",
                    "gui.packagedautoterminals.ratio_too_large",
                    "gui.packagedautoterminals.applied_partial",
                    "gui.packagedautoterminals.applied_no_partner")));

    private Feedback() {}

    /** Packs a key and its arguments into a single string. */
    public static String pack(String key, Object... arguments) {
        StringBuilder packed = new StringBuilder(key);
        for (Object argument : arguments) {
            packed.append(SEPARATOR).append(argument);
        }
        return packed.toString();
    }

    /** Translation key carried by a packed string. */
    public static String key(String packed) {
        int end = packed.indexOf(SEPARATOR);
        return end < 0 ? packed : packed.substring(0, end);
    }

    /**
     * Arguments carried by a packed string, ready for {@code I18n.format}.
     *
     * <p>FIXED, and it hid every count the mod ever printed. Packing turns an argument into text,
     * and this method used to hand the text straight back. Eleven language entries expect
     * {@code %d}, and {@code String.format("%d", "2")} throws
     * {@code IllegalFormatConversionException}. Read in {@code Locale.formatMessage} of Forge
     * 14.23.5.2847: the exception is caught and the line becomes {@code "Format error: ..."}. The
     * player therefore read "Format error: Applied to %d machines" after every single write.
     *
     * <p>A token that reads as a whole number comes back as a {@code Long}. Everything else stays
     * text, so a {@code %s} entry keeps working either way.
     */
    public static Object[] arguments(String packed) {
        int end = packed.indexOf(SEPARATOR);
        if (end < 0) {
            return new Object[0];
        }
        String[] tokens = packed.substring(end + 1).split(SEPARATOR);
        Object[] arguments = new Object[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            arguments[i] = numberOrText(tokens[i]);
        }
        return arguments;
    }

    /** Does this message report a refusal, or a write that left a machine behind? */
    public static boolean isRefusal(String packed) {
        return REFUSALS.contains(key(packed));
    }

    private static Object numberOrText(String token) {
        try {
            return Long.valueOf(token);
        } catch (NumberFormatException notANumber) {
            return token;
        }
    }
}
