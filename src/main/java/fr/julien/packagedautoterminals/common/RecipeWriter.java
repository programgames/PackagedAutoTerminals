package fr.julien.packagedautoterminals.common;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import thelm.packagedauto.api.IPackageProvidingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeList;
import thelm.packagedauto.api.IRecipeListItem;

/**
 * Writes a recipe into the recipe holders of a machine group.
 *
 * <p>PackagedAuto requires the **same** recipe in the Packager and in the Unpackager.
 * Editing one side only breaks the automation silently. Every write therefore goes through
 * here, and applies to all machines of the group.
 *
 * <p>Each write ends with {@code setPatternStack}. That call, and only that call, triggers
 * {@code updatePatternList} then {@code postPatternChange} in PackagedAuto. See
 * docs/PACKAGEDAUTO-MODEL.md, section 7.2.
 */
public final class RecipeWriter {

    private RecipeWriter() {}

    /**
     * How many recipes one recipe holder can carry.
     *
     * <p>Read in {@code PackagedAutoConfig}: the entry {@code pattern_slots} drives
     * {@code TileEncoder.patternSlots}, and Forge bounds it to **1 to 20**. A Package Recipe
     * Encoder can therefore never show a twenty first recipe.
     *
     * <p>The field is read at run time, not copied, so a pack that lowers the entry is followed.
     *
     * <p>FIXED: the terminal used to add without any bound. A player could write a recipe that
     * their own Encoder would never be able to open again.
     */
    public static int maxRecipes() {
        return Math.max(1, thelm.packagedauto.tile.TileEncoder.patternSlots);
    }

    /** Result of a group write. */
    public static final class Result {
        /** Machines actually changed. */
        public int changed;
        /** Machines skipped for lack of an available recipe holder. */
        public int withoutHolder;
        /** Machines whose recipe holder already carries {@link #maxRecipes()} recipes. */
        public int full;
        /** Blank recipe holders taken from the ME network for this write. */
        public int supplied;
    }

    /**
     * Applies a change to every machine in the list.
     *
     * <p>A machine of the group that does **not** carry the target recipe receives it anyway
     * on an edit. That is the whole point: repairing an out-of-sync pair in one gesture.
     * Without this rule, the machine left behind would stay behind.
     *
     * <p>A machine **without a recipe holder** receives a blank one from the ME network, when
     * {@code PatConfig.pullHolderFromNetwork} allows it. That setting is on by default: a
     * machine with no holder otherwise forced the player to walk to it, which is the one
     * thing this mod exists to remove. The extraction goes through
     * {@code NetworkItems.extractOne}, hence through the AE2 energy and security rules, and
     * the prototype carries no NBT, so only a **blank** holder ever matches.
     *
     * <p>When the setting is off, or the network holds no blank holder, the machine stays
     * untouched and is counted in {@link Result#withoutHolder}.
     *
     * @param oldRecipe target recipe. {@code null} for an addition.
     * @param newRecipe recipe to write. {@code null} for a removal.
     */
    public static Result apply(IGrid grid, IActionSource source, List<ProviderSnapshot> machines,
                               IRecipeInfo oldRecipe, IRecipeInfo newRecipe) {
        Result result = new Result();
        for (ProviderSnapshot snapshot : machines) {
            switch (applyTo(grid, source, snapshot, oldRecipe, newRecipe, result)) {
                case CHANGED:
                    result.changed++;
                    break;
                case NO_HOLDER:
                    result.withoutHolder++;
                    break;
                case FULL:
                    result.full++;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    public enum Outcome { CHANGED, UNCHANGED, NO_HOLDER, FULL }

    /**
     * The machine of a snapshot, without walking the grid again when it is already known.
     *
     * <p>The snapshot carries the machine on the server. The tile is tested all the same: a
     * block broken between the scan and the write would leave an invalid one behind.
     */
    private static IPackageProvidingMachine machineOf(IGrid grid, ProviderSnapshot snapshot) {
        IPackageProvidingMachine known = snapshot.machine;
        if (known instanceof TileEntity && !((TileEntity) known).isInvalid()) {
            return known;
        }
        return ProviderScanner.find(grid, snapshot.dimension, snapshot.pos);
    }

    private static Outcome applyTo(IGrid grid, IActionSource source, ProviderSnapshot snapshot,
                                   IRecipeInfo oldRecipe, IRecipeInfo newRecipe, Result result) {
        IPackageProvidingMachine machine = machineOf(grid, snapshot);
        if (machine == null) {
            return Outcome.UNCHANGED;
        }

        ItemStack holder = machine.getPatternStack();
        // A holder taken from the network goes back there on every path that writes nothing.
        // Otherwise a refusal would swallow the item.
        boolean borrowed = false;
        if (holder.isEmpty()) {
            // A removal has nothing to do on an empty machine.
            if (newRecipe == null) {
                return Outcome.UNCHANGED;
            }
            holder = takeBlankHolder(grid, source);
            if (holder.isEmpty()) {
                return Outcome.NO_HOLDER;
            }
            borrowed = true;
        }
        Outcome outcome = editHolder(holder, oldRecipe, newRecipe);
        if (outcome != Outcome.CHANGED) {
            return giveBack(grid, source, holder, borrowed, outcome);
        }
        machine.setPatternStack(holder);
        if (borrowed) {
            result.supplied++;
        }
        return Outcome.CHANGED;
    }

    /**
     * Applies a change to one recipe holder, in place, with no machine and no network.
     *
     * <p>The machine path above and the holder mode of the editor (D43) both go through here,
     * so a holder edited by hand follows the same rules as one edited in its machine.
     *
     * @return {@link Outcome#CHANGED} when the holder now carries the change.
     */
    public static Outcome editHolder(ItemStack holder, IRecipeInfo oldRecipe,
                                     IRecipeInfo newRecipe) {
        if (!(holder.getItem() instanceof IRecipeListItem)) {
            return Outcome.UNCHANGED;
        }
        List<IRecipeInfo> recipes = new ArrayList<>(recipesOf(holder));
        Outcome outcome = edit(recipes, oldRecipe, newRecipe, maxRecipes());
        if (outcome != Outcome.CHANGED) {
            return outcome;
        }
        return setRecipes(holder, recipes) ? Outcome.CHANGED : Outcome.UNCHANGED;
    }

    /**
     * The list part of {@link #editHolder}: applies the change to {@code recipes}, in place.
     *
     * @return {@link Outcome#CHANGED} when the list now carries the change. The list is left
     *         untouched otherwise.
     */
    public static Outcome edit(List<IRecipeInfo> recipes, IRecipeInfo oldRecipe,
                               IRecipeInfo newRecipe, int max) {
        int index = indexOf(recipes, oldRecipe);

        if (newRecipe == null) {
            if (index < 0) {
                return Outcome.UNCHANGED;
            }
            // Every copy goes, not the first one.
            //
            // A recipe holder can list the same recipe twice, and the terminal shows the
            // distinct ones. Removing one copy left the row on screen, so the player clicked
            // delete again, and again, with nothing telling them why.
            while (index >= 0) {
                recipes.remove(index);
                index = indexOf(recipes, oldRecipe);
            }
        } else if (index >= 0) {
            recipes.set(index, newRecipe);
        } else {
            // The machine does not carry this recipe yet: it receives it.
            if (indexOf(recipes, newRecipe) >= 0) {
                return Outcome.UNCHANGED;
            }
            // Last guard. The editor refuses a full group before it reaches this point, so
            // nothing is ever written by halves.
            if (recipes.size() >= max) {
                return Outcome.FULL;
            }
            recipes.add(newRecipe);
        }
        return Outcome.CHANGED;
    }

    /**
     * Replaces every recipe of {@code from} by the recipe at the same position in {@code to},
     * in one write per holder, keeping the order (D44).
     *
     * <p>One recipe at a time through {@link #apply} would pass through states where a scaled
     * recipe equals one not yet scaled, and the holder would briefly list it twice.
     */
    public static boolean mapHolder(ItemStack holder, List<IRecipeInfo> from,
                                    List<IRecipeInfo> to) {
        List<IRecipeInfo> recipes = new ArrayList<>(recipesOf(holder));
        return map(recipes, from, to) && setRecipes(holder, recipes);
    }

    /** The list part of {@link #mapHolder}, in place. True when one recipe changed. */
    public static boolean map(List<IRecipeInfo> recipes, List<IRecipeInfo> from,
                              List<IRecipeInfo> to) {
        boolean changed = false;
        for (int i = 0; i < recipes.size(); i++) {
            int match = indexOf(from, recipes.get(i));
            if (match >= 0) {
                recipes.set(i, to.get(match));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Appends to {@code recipes} every recipe of {@code wanted} it lacks, in order. Deletes
     * nothing, so a blank list becomes a copy of {@code wanted}.
     *
     * @return how many recipes were appended
     */
    public static int addMissing(List<IRecipeInfo> recipes, List<IRecipeInfo> wanted) {
        int before = recipes.size();
        for (IRecipeInfo recipe : wanted) {
            if (indexOf(recipes, recipe) < 0) {
                recipes.add(recipe);
            }
        }
        return recipes.size() - before;
    }

    /** {@link #mapHolder} on every machine of a group. A machine republishes its patterns once. */
    public static int mapMachines(IGrid grid, List<ProviderSnapshot> machines,
                                  List<IRecipeInfo> from, List<IRecipeInfo> to) {
        int changed = 0;
        for (ProviderSnapshot snapshot : machines) {
            IPackageProvidingMachine machine = machineOf(grid, snapshot);
            if (machine == null) {
                continue;
            }
            ItemStack holder = machine.getPatternStack();
            if (!holder.isEmpty() && mapHolder(holder, from, to)) {
                machine.setPatternStack(holder);
                changed++;
            }
        }
        return changed;
    }

    /** Recipes listed by a holder. Empty for anything that is not a recipe holder. */
    public static List<IRecipeInfo> recipesOf(ItemStack holder) {
        if (!(holder.getItem() instanceof IRecipeListItem)) {
            return new ArrayList<>();
        }
        IRecipeList recipeList = ((IRecipeListItem) holder.getItem()).getRecipeList(holder);
        return recipeList == null ? new ArrayList<>() : recipeList.getRecipeList();
    }

    /** Replaces the list of a holder, in place. False when the item refuses. */
    public static boolean setRecipes(ItemStack holder, List<IRecipeInfo> recipes) {
        if (!(holder.getItem() instanceof IRecipeListItem)) {
            return false;
        }
        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return false;
        }
        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        return true;
    }

    /**
     * Takes one blank Package Recipe Holder out of the ME network.
     *
     * <p>The prototype carries no NBT. AE2 matches an item, its metadata and its tag, so a
     * holder that already lists recipes never answers this request: only a blank one leaves
     * the network.
     */
    private static ItemStack takeBlankHolder(IGrid grid, IActionSource source) {
        if (!PatConfig.pullHolderFromNetwork) {
            return ItemStack.EMPTY;
        }
        Item blank = NetworkItems.findRecipeHolder();
        if (blank == null) {
            return ItemStack.EMPTY;
        }
        return NetworkItems.extractOne(grid, new ItemStack(blank), source);
    }

    /** Puts a borrowed holder back into the network, then answers the given outcome. */
    private static Outcome giveBack(IGrid grid, IActionSource source, ItemStack holder,
                                    boolean borrowed, Outcome outcome) {
        if (borrowed) {
            NetworkItems.insert(grid, holder, source);
        }
        return outcome;
    }

    private static int indexOf(List<IRecipeInfo> recipes, IRecipeInfo recipe) {
        if (recipe == null) {
            return -1;
        }
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).equals(recipe)) {
                return i;
            }
        }
        return -1;
    }
}
