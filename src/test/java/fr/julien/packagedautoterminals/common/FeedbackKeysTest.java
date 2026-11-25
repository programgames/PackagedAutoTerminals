package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * Every key a container sends with {@code tell}, or packs with {@code Feedback.pack}, is
 * classified, and translated.
 *
 * <p>The screens draw a message in red only when {@link Feedback#isRefusal} knows its key.
 * A refusal missing from that list reads in the colour of a success: version 0.1.0-beta.5
 * fixed three of them, and four more came back afterwards.
 */
public class FeedbackKeysTest {

    private static final String PREFIX = "gui.packagedautoterminals.";

    /** Keys drawn in the colour of a success. A new key goes here, or into Feedback.REFUSALS. */
    private static final Set<String> SUCCESSES = new HashSet<>(Arrays.asList(
            "applied_to",
            "applied_to_one",
            "applied_with_holder",
            "cleared",
            "group_already_even",
            "holders_synced",
            "holders_written",
            "name_merges",
            "recipes_scaled",
            "recipes_scaled_one",
            "recipes_scaled_skipped",
            "renamed"));

    /**
     * A whole {@code tell(...)} or {@code Feedback.pack(...)} statement, so both keys of a
     * ternary are read. {@code RecipeWriter.Result.feedback} packs its keys without {@code tell}.
     */
    private static final Pattern TELL_CALL = Pattern.compile("\\b(?:tell|pack)\\(([^;]*);");
    private static final Pattern KEY = Pattern.compile("\"" + Pattern.quote(PREFIX) + "([a-z_]+)\"");

    @Test
    public void everyToldKeyIsEitherARefusalOrASuccessAndIsTranslated() throws IOException {
        Set<String> told = toldKeys();
        assertFalse("no tell(...) found under src/main/java", told.isEmpty());
        Properties english = english();

        List<String> problems = new ArrayList<>();
        for (String key : told) {
            boolean refusal = Feedback.isRefusal(PREFIX + key);
            if (refusal == SUCCESSES.contains(key)) {
                problems.add(key + (refusal
                        ? " is both in Feedback.REFUSALS and in SUCCESSES"
                        : " is neither in Feedback.REFUSALS nor in SUCCESSES"));
            }
            if (!english.containsKey(PREFIX + key)) {
                problems.add(key + " is missing from en_us.lang");
            }
        }
        assertEquals(new ArrayList<String>(), problems);
    }

    private static Set<String> toldKeys() throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            sources = files.filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
        Set<String> keys = new TreeSet<>();
        for (Path source : sources) {
            Matcher call = TELL_CALL.matcher(
                    new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            while (call.find()) {
                Matcher key = KEY.matcher(call.group(1));
                while (key.find()) {
                    keys.add(key.group(1));
                }
            }
        }
        return keys;
    }

    private static Properties english() throws IOException {
        Properties english = new Properties();
        try (Reader reader = Files.newBufferedReader(
                Paths.get("src/main/resources/assets/packagedautoterminals/lang/en_us.lang"),
                StandardCharsets.UTF_8)) {
            english.load(reader);
        }
        return english;
    }
}
