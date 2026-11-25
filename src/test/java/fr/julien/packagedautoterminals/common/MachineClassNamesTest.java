package fr.julien.packagedautoterminals.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * The machine tables name their classes in strings, so a typo still compiles.
 *
 * <p>A wrong role name turns the machine into {@link ProviderRole#UNKNOWN}, and every recipe
 * it carries then reads as complete. Nothing in the game would say so.
 */
public class MachineClassNamesTest {

    /** Addons absent from the test path. Their names were checked by hand in their jars. */
    private static final Set<String> ABSENT_ADDONS = new HashSet<>(Arrays.asList(
            "thelm.packagedexcrafting.tile.TileBasicCrafter",
            "thelm.packagedexcrafting.tile.TileAdvancedCrafter",
            "thelm.packagedexcrafting.tile.TileEliteCrafter",
            "thelm.packagedexcrafting.tile.TileUltimateCrafter",
            "thelm.packagedexcrafting.tile.TileCombinationCrafter",
            "thelm.packagedexcrafting.tile.TileEnderCrafter",
            "thelm.packagedavaritia.tile.TileExtremeCrafter",
            "thelm.packagingprovider.tile.TilePackagingProvider"));

    @Test
    public void everyPackagedAutoClassNamedInATableExists() throws Exception {
        Set<String> names = new HashSet<>(CrafterTypes.all().values());
        names.addAll(privateNames(CrafterTypes.class, "ROUTERS"));
        names.addAll(privateNames(ProviderRole.class, "BY_CLASS"));
        names.removeAll(ABSENT_ADDONS);

        for (String name : names) {
            Class.forName(name, false, getClass().getClassLoader());
        }
    }

    /** The strings of a private static set, or the keys of a private static map. */
    private static List<String> privateNames(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        Collection<?> items = value instanceof Map ? ((Map<?, ?>) value).keySet() : (Collection<?>) value;
        List<String> names = new ArrayList<>();
        for (Object item : items) {
            names.add((String) item);
        }
        return names;
    }
}
