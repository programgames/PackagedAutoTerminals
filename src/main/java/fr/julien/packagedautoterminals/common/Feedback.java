package fr.julien.packagedautoterminals.common;

/**
 * Message court renvoyé au joueur, affiché **dans** la fenêtre.
 *
 * <p>La barre d'action du jeu ne convenait pas : elle se dessine sous la fenêtre, et le
 * message y passait inaperçu.
 *
 * <p>Le texte voyage sous forme de clé de traduction, suivie de ses paramètres. La mise en
 * forme a lieu sur le client, dans sa propre langue.
 */
public final class Feedback {

    /** Séparateur entre la clé et ses paramètres. Aucun texte ne le contient. */
    private static final String SEPARATOR = "";

    private Feedback() {}

    /** Assemble une clé et ses paramètres en une seule chaîne. */
    public static String pack(String key, Object... arguments) {
        StringBuilder packed = new StringBuilder(key);
        for (Object argument : arguments) {
            packed.append(SEPARATOR).append(argument);
        }
        return packed.toString();
    }

    /** Clé de traduction portée par une chaîne assemblée. */
    public static String key(String packed) {
        int end = packed.indexOf(SEPARATOR);
        return end < 0 ? packed : packed.substring(0, end);
    }

    /** Paramètres portés par une chaîne assemblée. */
    public static Object[] arguments(String packed) {
        int end = packed.indexOf(SEPARATOR);
        if (end < 0) {
            return new Object[0];
        }
        return packed.substring(end + 1).split(SEPARATOR);
    }
}
