package fr.julien.packagedautoterminals.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Role of a machine that carries recipes.
 *
 * <p>PackagedAuto needs **two** recipe holders per automation, with the same recipe in both:
 * one in the Packager, one in the Unpackager. Both publish to AE2, but not the same thing:
 *
 * <ul>
 *   <li>the Packager publishes {@code PackageCraftingPatternHelper}: items to package;
 *   <li>the Unpackager publishes {@code RecipeCraftingPatternHelper}: ingredients to output.
 * </ul>
 *
 * <p>Without the Packager copy, AE2 does not know how to build the package. Without the
 * Unpackager copy, it does not know what the recipe produces.
 *
 * <p>The role is derived from the **class name**, never from the class itself: the mod
 * therefore runs without the addons. {@code IPackageProvidingMachine} does not allow the
 * role to be derived: both machines expose exactly the same methods.
 */
public enum ProviderRole {

    /** Builds the packages. It needs a paired Unpackager. */
    PACKAGER,
    /** Unpacks the packages and declares the final recipe. It needs a paired Packager. */
    UNPACKAGER,
    /** Fills both roles on its own. No pair is needed. */
    COMPLETE,
    /** Machine from an addon we do not know. No diagnostic is emitted. */
    UNKNOWN;

    private static final Map<String, ProviderRole> BY_CLASS = new HashMap<>();

    static {
        BY_CLASS.put("thelm.packagedauto.tile.TilePackager", PACKAGER);
        BY_CLASS.put("thelm.packagedauto.tile.TileUnpackager", UNPACKAGER);
        // The Packaging Provider publishes both pattern families, plus its own. It is
        // therefore self-sufficient.
        BY_CLASS.put("thelm.packagingprovider.tile.TilePackagingProvider", COMPLETE);
    }

    public static ProviderRole of(Object machine) {
        if (machine == null) {
            return UNKNOWN;
        }
        ProviderRole role = BY_CLASS.get(machine.getClass().getName());
        return role == null ? UNKNOWN : role;
    }

    public static ProviderRole fromOrdinal(int ordinal) {
        ProviderRole[] values = values();
        return ordinal < 0 || ordinal >= values.length ? UNKNOWN : values[ordinal];
    }

    /** Does this role expect a machine of the complementary role? */
    public boolean needsPartner() {
        return this == PACKAGER || this == UNPACKAGER;
    }

    /** Role expected on the other side, or {@code null} when this role is self-sufficient. */
    public ProviderRole partner() {
        if (this == PACKAGER) {
            return UNPACKAGER;
        }
        return this == UNPACKAGER ? PACKAGER : null;
    }
}
