package fr.julien.packagedautoterminals.common;

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

    /** Separator between the key and its arguments. No text contains it. */
    private static final String SEPARATOR = "";

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

    /** Arguments carried by a packed string. */
    public static Object[] arguments(String packed) {
        int end = packed.indexOf(SEPARATOR);
        if (end < 0) {
            return new Object[0];
        }
        return packed.substring(end + 1).split(SEPARATOR);
    }
}
