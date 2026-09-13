package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import thelm.packagedauto.api.IRecipeInfo;

/**
 * Regroupe les machines qui travaillent ensemble.
 *
 * <p>Le joueur crée une paire de porte-recettes, puis leur ajoute les mêmes recettes au fil
 * du temps. Le terminal doit donc présenter une recette **une seule fois**, et appliquer
 * toute modification aux deux côtés.
 *
 * <p>Le critère de regroupement est le **partage d'au moins une recette**. Il résiste au cas
 * qui nous intéresse le plus : si une recette manque d'un côté, les autres suffisent à
 * maintenir le groupe, et le manque devient visible au lieu de casser l'appariement.
 */
public final class ProviderPairing {

    private ProviderPairing() {}

    /** Un groupe : les machines appariées, et l'union de leurs recettes. */
    public static final class Group {

        public final List<ProviderSnapshot> machines = new ArrayList<>();
        public final List<IRecipeInfo> recipes = new ArrayList<>();

        /** Machines qui portent cette recette. */
        public List<ProviderSnapshot> carriers(IRecipeInfo recipe) {
            List<ProviderSnapshot> carriers = new ArrayList<>();
            for (ProviderSnapshot machine : machines) {
                if (contains(machine, recipe)) {
                    carriers.add(machine);
                }
            }
            return carriers;
        }

        /**
         * Cette recette est-elle complète, donc exécutable par AE2 ?
         *
         * <p>Elle l'est si le groupe porte un rôle qui se suffit, ou si la recette figure à
         * la fois dans un Packager et dans un Unpackager.
         */
        public boolean isComplete(IRecipeInfo recipe) {
            boolean packager = false;
            boolean unpackager = false;
            for (ProviderSnapshot machine : carriers(recipe)) {
                if (machine.role == ProviderRole.COMPLETE || machine.role == ProviderRole.UNKNOWN) {
                    return true;
                }
                packager |= machine.role == ProviderRole.PACKAGER;
                unpackager |= machine.role == ProviderRole.UNPACKAGER;
            }
            return packager && unpackager;
        }

        /** Rôle manquant pour cette recette, ou {@code null} si elle est complète. */
        public ProviderRole missingRole(IRecipeInfo recipe) {
            if (isComplete(recipe)) {
                return null;
            }
            for (ProviderSnapshot machine : carriers(recipe)) {
                if (machine.role.needsPartner()) {
                    return machine.role.partner();
                }
            }
            return null;
        }

        /** Nom affiché du groupe : les machines qui le composent. */
        public String title() {
            Set<String> names = new LinkedHashSet<>();
            for (ProviderSnapshot machine : machines) {
                names.add(machine.name);
            }
            return String.join(" + ", names);
        }
    }

    /**
     * Forme les groupes.
     *
     * <p>L'algorithme est une fusion par proche en proche : chaque machine part seule, puis
     * rejoint le premier groupe avec lequel elle partage une recette. Les groupes ainsi
     * rejoints fusionnent entre eux, car une machine peut faire le pont.
     */
    public static List<Group> group(List<ProviderSnapshot> providers) {
        List<Group> groups = new ArrayList<>();

        for (ProviderSnapshot provider : providers) {
            List<Group> shared = new ArrayList<>();
            for (Group group : groups) {
                if (sharesRecipe(group, provider)) {
                    shared.add(group);
                }
            }

            Group target;
            if (shared.isEmpty()) {
                target = new Group();
                groups.add(target);
            } else {
                // La machine fait le pont entre plusieurs groupes : ils n'en font plus qu'un.
                target = shared.get(0);
                for (int i = 1; i < shared.size(); i++) {
                    Group merged = shared.get(i);
                    target.machines.addAll(merged.machines);
                    for (IRecipeInfo recipe : merged.recipes) {
                        addDistinct(target.recipes, recipe);
                    }
                    groups.remove(merged);
                }
            }

            target.machines.add(provider);
            for (IRecipeInfo recipe : provider.recipes) {
                addDistinct(target.recipes, recipe);
            }
        }
        return groups;
    }

    /** Groupe qui contient cette machine, ou {@code null}. */
    public static Group groupOf(List<Group> groups, int dimension, net.minecraft.util.math.BlockPos pos) {
        for (Group group : groups) {
            for (ProviderSnapshot machine : group.machines) {
                if (machine.dimension == dimension && machine.pos.equals(pos)) {
                    return group;
                }
            }
        }
        return null;
    }

    /**
     * Machine isolée du rôle manquant, quand le groupe n'a pas encore de partenaire.
     *
     * <p>Cas courant : le joueur vient de poser une paire, encode sa première recette, et
     * les deux porte-recettes ne partagent donc encore rien. Si le réseau ne compte qu'une
     * seule machine du rôle manquant, il n'y a pas d'ambiguïté : c'est elle.
     *
     * @return la machine, ou {@code null} si le choix serait ambigu.
     */
    public static ProviderSnapshot findLonePartner(List<ProviderSnapshot> all, Group group,
                                                   ProviderRole missing) {
        if (missing == null) {
            return null;
        }
        ProviderSnapshot found = null;
        for (ProviderSnapshot candidate : all) {
            if (candidate.role != missing || group.machines.contains(candidate)) {
                continue;
            }
            if (found != null) {
                // Plusieurs candidats : nous ne devinons pas.
                return null;
            }
            found = candidate;
        }
        return found;
    }

    /** Rôle absent du groupe, alors qu'une de ses machines l'attend. */
    public static ProviderRole missingRoleOf(Group group) {
        boolean packager = false;
        boolean unpackager = false;
        for (ProviderSnapshot machine : group.machines) {
            if (machine.role == ProviderRole.COMPLETE || machine.role == ProviderRole.UNKNOWN) {
                return null;
            }
            packager |= machine.role == ProviderRole.PACKAGER;
            unpackager |= machine.role == ProviderRole.UNPACKAGER;
        }
        if (packager && !unpackager) {
            return ProviderRole.UNPACKAGER;
        }
        return unpackager && !packager ? ProviderRole.PACKAGER : null;
    }

    private static boolean sharesRecipe(Group group, ProviderSnapshot provider) {
        for (IRecipeInfo recipe : provider.recipes) {
            for (IRecipeInfo known : group.recipes) {
                if (known.equals(recipe)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(ProviderSnapshot machine, IRecipeInfo recipe) {
        for (IRecipeInfo known : machine.recipes) {
            if (known.equals(recipe)) {
                return true;
            }
        }
        return false;
    }

    private static void addDistinct(List<IRecipeInfo> recipes, IRecipeInfo recipe) {
        for (IRecipeInfo known : recipes) {
            if (known.equals(recipe)) {
                return;
            }
        }
        recipes.add(recipe);
    }
}
