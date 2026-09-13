package fr.julien.packagedautoterminals.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Rôle d'une machine porteuse de recettes.
 *
 * <p>PackagedAuto demande **deux** porte-recettes par automatisation, avec la même recette
 * dans les deux : l'un dans le Packager, l'autre dans l'Unpackager. Les deux publient vers
 * AE2, mais pas la même chose :
 *
 * <ul>
 *   <li>le Packager publie {@code PackageCraftingPatternHelper} : objets vers colis ;
 *   <li>l'Unpackager publie {@code RecipeCraftingPatternHelper} : ingrédients vers produit.
 * </ul>
 *
 * <p>Sans la copie du Packager, AE2 ne sait pas fabriquer le colis. Sans celle de
 * l'Unpackager, il ignore ce que la recette produit.
 *
 * <p>Le rôle se déduit du **nom de la classe**, jamais de la classe elle-même : le mod
 * fonctionne donc sans les addons. {@code IPackageProvidingMachine} ne permet pas de le
 * déduire : les deux machines exposent exactement les mêmes méthodes.
 */
public enum ProviderRole {

    /** Fabrique les colis. Il lui faut un Unpackager apparié. */
    PACKAGER,
    /** Défait les colis et déclare la recette finale. Il lui faut un Packager apparié. */
    UNPACKAGER,
    /** Assure les deux rôles à lui seul. Aucune paire n'est nécessaire. */
    COMPLETE,
    /** Machine d'un addon que nous ne connaissons pas. Aucun diagnostic n'est émis. */
    UNKNOWN;

    private static final Map<String, ProviderRole> BY_CLASS = new HashMap<>();

    static {
        BY_CLASS.put("thelm.packagedauto.tile.TilePackager", PACKAGER);
        BY_CLASS.put("thelm.packagedauto.tile.TileUnpackager", UNPACKAGER);
        // Le Packaging Provider publie les deux familles de patterns, plus la sienne. Il se
        // suffit donc à lui-même.
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

    /** Ce rôle attend-il une machine du rôle complémentaire ? */
    public boolean needsPartner() {
        return this == PACKAGER || this == UNPACKAGER;
    }

    /** Rôle attendu en face, ou {@code null} si ce rôle se suffit. */
    public ProviderRole partner() {
        if (this == PACKAGER) {
            return UNPACKAGER;
        }
        return this == UNPACKAGER ? PACKAGER : null;
    }
}
