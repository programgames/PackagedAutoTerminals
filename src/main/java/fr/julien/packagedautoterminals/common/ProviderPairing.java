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

        /** Nom donné par le joueur, ou {@code null}. Le premier trouvé fait foi. */
        public String customName() {
            for (ProviderSnapshot machine : machines) {
                if (machine.customName != null && !machine.customName.isEmpty()) {
                    return machine.customName;
                }
            }
            return null;
        }

        /** Le groupe est-il exactement une paire Packager et Unpackager ? */
        public boolean isPair() {
            if (machines.size() != 2) {
                return false;
            }
            ProviderRole first = machines.get(0).role;
            ProviderRole second = machines.get(1).role;
            return first.needsPartner() && second == first.partner();
        }

        /** Nom de la seule machine du groupe. */
        public String singleName() {
            Set<String> names = new LinkedHashSet<>();
            for (ProviderSnapshot machine : machines) {
                names.add(machine.name);
            }
            return names.iterator().next();
        }

        /** Machines distinctes, pour l'infobulle. */
        public int size() {
            return machines.size();
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

        mergeLonePartners(groups);
        mergeEmptyPair(groups);
        return groups;
    }

    /**
     * Rattache une machine vide au groupe qui attend justement son rôle.
     *
     * <p>Cas courant, et déroutant sans cette règle : le joueur a encodé une recette dans
     * l'Unpackager, mais pas encore dans le Packager. Les deux ne partagent donc aucune
     * recette, forment deux groupes, et chacun se plaint de l'absence de l'autre alors
     * qu'ils sont posés côte à côte.
     *
     * <p>Après rattachement, l'en-tête cesse de crier au rôle manquant, car le groupe porte
     * bien les deux machines. Seule la **recette** reste signalée en rouge, car elle n'est
     * encore que d'un côté. C'est exactement l'information utile.
     *
     * <p>La fusion n'a lieu que si le choix est certain : une seule machine vide de ce rôle,
     * et un seul groupe qui l'attend.
     */
    private static void mergeLonePartners(List<Group> groups) {
        for (ProviderRole role : new ProviderRole[] {ProviderRole.PACKAGER, ProviderRole.UNPACKAGER}) {
            Group candidate = null;
            Group needy = null;
            int candidates = 0;
            int needies = 0;

            for (Group group : groups) {
                if (group.recipes.isEmpty() && group.machines.size() == 1
                        && group.machines.get(0).role == role) {
                    candidate = group;
                    candidates++;
                } else if (missingRoleOf(group) == role) {
                    needy = group;
                    needies++;
                }
            }

            if (candidates == 1 && needies == 1) {
                needy.machines.addAll(candidate.machines);
                groups.remove(candidate);
            }
        }
    }

    /**
     * Réunit une paire toute neuve, dont les deux porte-recettes sont encore vides.
     *
     * <p>Sans recette, aucune ne peut être partagée : les deux machines formeraient deux
     * groupes, et le terminal afficherait deux lignes pour ce qui est déjà une paire. La
     * fusion n'a lieu que si le choix est certain : exactement un groupe vide de chaque
     * rôle. Au-delà, nous ne devinons pas.
     */
    private static void mergeEmptyPair(List<Group> groups) {
        Group packager = null;
        Group unpackager = null;

        for (Group group : groups) {
            if (!group.recipes.isEmpty() || group.machines.size() != 1) {
                continue;
            }
            ProviderRole role = group.machines.get(0).role;
            if (role == ProviderRole.PACKAGER) {
                if (packager != null) {
                    return;
                }
                packager = group;
            } else if (role == ProviderRole.UNPACKAGER) {
                if (unpackager != null) {
                    return;
                }
                unpackager = group;
            }
        }

        if (packager != null && unpackager != null) {
            packager.machines.addAll(unpackager.machines);
            groups.remove(unpackager);
        }
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

    /**
     * Rôle absent du groupe, alors qu'une de ses machines l'attend.
     *
     * <p>Un groupe **sans aucune recette** ne manque de rien : le joueur vient de poser ses
     * machines, et n'a encore rien encodé. Reprocher une absence à ce stade n'aurait aucun
     * sens.
     */
    public static ProviderRole missingRoleOf(Group group) {
        if (group.recipes.isEmpty()) {
            return null;
        }
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
