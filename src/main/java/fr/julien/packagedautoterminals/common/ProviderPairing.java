package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import thelm.packagedauto.api.IRecipeInfo;

/**
 * Groups the machines that work together.
 *
 * <p>The player creates a pair of recipe holders, then adds the same recipes to both over
 * time. The terminal must therefore show a recipe **only once**, and apply every edit to
 * both sides.
 *
 * <p>The grouping criterion is **sharing at least one recipe**. It survives the case we care
 * about most: when a recipe is missing on one side, the others still hold the group
 * together, and the gap becomes visible instead of breaking the pairing.
 */
public final class ProviderPairing {

    private ProviderPairing() {}

    /** A group: the paired machines, and the union of their recipes. */
    public static final class Group {

        public final List<ProviderSnapshot> machines = new ArrayList<>();
        public final List<IRecipeInfo> recipes = new ArrayList<>();

        /** Machines that carry this recipe. */
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
         * Is this recipe complete, hence runnable by AE2?
         *
         * <p>It is, when the group carries a self-sufficient role, or when the recipe sits
         * in both a Packager and an Unpackager.
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

        /** Role missing for this recipe, or {@code null} when it is complete. */
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

        /** Name given by the player, or {@code null}. The first one found wins. */
        public String customName() {
            for (ProviderSnapshot machine : machines) {
                if (machine.customName != null && !machine.customName.isEmpty()) {
                    return machine.customName;
                }
            }
            return null;
        }

        /** Is the group exactly one Packager and Unpackager pair? */
        public boolean isPair() {
            if (machines.size() != 2) {
                return false;
            }
            ProviderRole first = machines.get(0).role;
            ProviderRole second = machines.get(1).role;
            return first.needsPartner() && second == first.partner();
        }

        /** Name of the single machine in the group. */
        public String singleName() {
            Set<String> names = new LinkedHashSet<>();
            for (ProviderSnapshot machine : machines) {
                names.add(machine.name);
            }
            return names.iterator().next();
        }

        /** Distinct machines, for the tooltip. */
        public int size() {
            return machines.size();
        }
    }

    /**
     * Builds the groups.
     *
     * <p>The algorithm merges step by step: every machine starts alone, then joins the first
     * group it shares a recipe with. Groups joined that way merge together, because one
     * machine can bridge them.
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
                // The machine bridges several groups: they become a single one.
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

        // The name comes first: it is the only link the player set themselves.
        mergeNamed(groups);
        mergeLonePartners(groups);
        mergeEmptyPair(groups);
        return groups;
    }

    /**
     * Joins the machines the player has named together.
     *
     * <p>The name is written on **every** machine of the group at naming time. Two machines
     * that carry the same name are therefore paired by the player's decision, never by
     * deduction.
     *
     * <p>This link survives where recipe sharing does not. Without it, a pair whose last
     * recipe is removed splits into two rows, and a network holding a third machine of the
     * same role forbids any automatic reunion: the choice would be ambiguous. The name
     * removes the ambiguity.
     */
    private static void mergeNamed(List<Group> groups) {
        Map<String, Group> byName = new LinkedHashMap<>();
        for (Group group : new ArrayList<>(groups)) {
            String name = group.customName();
            if (name == null) {
                continue;
            }
            Group first = byName.get(name);
            if (first == null) {
                byName.put(name, group);
                continue;
            }
            first.machines.addAll(group.machines);
            for (IRecipeInfo recipe : group.recipes) {
                addDistinct(first.recipes, recipe);
            }
            groups.remove(group);
        }
    }

    /**
     * Attaches an empty machine to the group that is waiting for exactly its role.
     *
     * <p>A common case, and a confusing one without this rule: the player encoded a recipe
     * in the Unpackager, but not yet in the Packager. The two share no recipe, form two
     * groups, and each one complains about the other being absent while they sit side by
     * side.
     *
     * <p>After attaching, the header stops reporting a missing role, because the group does
     * carry both machines. Only the **recipe** stays flagged in red, because it is still on
     * one side only. That is exactly the useful piece of information.
     *
     * <p>The merge only happens when the choice is certain: a single empty machine of that
     * role, and a single group waiting for it.
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
     * Joins a brand new pair, whose two recipe holders are still empty.
     *
     * <p>With no recipe, none can be shared: the two machines would form two groups, and the
     * terminal would show two rows for what is already a pair. The merge only happens when
     * the choice is certain: exactly one empty group of each role. Beyond that, we do not
     * guess.
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

    /** Group that contains this machine, or {@code null}. */
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
     * Lone machine of the missing role, when the group has no partner yet.
     *
     * <p>A common case: the player has just placed a pair, encodes the first recipe, and the
     * two recipe holders share nothing yet. When the network holds a single machine of the
     * missing role, there is no ambiguity: that is the one.
     *
     * @return the machine, or {@code null} when the choice would be ambiguous.
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
                // Several candidates: we do not guess.
                return null;
            }
            found = candidate;
        }
        return found;
    }

    /**
     * Role absent from the group, while one of its machines expects it.
     *
     * <p>A group **with no recipe at all** misses nothing: the player has just placed the
     * machines and encoded nothing yet. Reporting an absence at that point would make no
     * sense.
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
