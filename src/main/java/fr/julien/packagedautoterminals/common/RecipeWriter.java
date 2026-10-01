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

    private enum Outcome { CHANGED, UNCHANGED, NO_HOLDER, FULL }

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
        if (!(holder.getItem() instanceof IRecipeListItem)) {
            return giveBack(grid, source, holder, borrowed, Outcome.UNCHANGED);
        }

        IRecipeListItem holderItem = (IRecipeListItem) holder.getItem();
        IRecipeList recipeList = holderItem.getRecipeList(holder);
        if (recipeList == null) {
            return giveBack(grid, source, holder, borrowed, Outcome.UNCHANGED);
        }

        List<IRecipeInfo> recipes = new ArrayList<>(recipeList.getRecipeList());
        int index = indexOf(recipes, oldRecipe);

        if (newRecipe == null) {
            if (index < 0) {
                return Outcome.UNCHANGED;
            }
            recipes.remove(index);
        } else if (index >= 0) {
            recipes.set(index, newRecipe);
        } else {
            // The machine does not carry this recipe yet: it receives it.
            if (indexOf(recipes, newRecipe) >= 0) {
                return giveBack(grid, source, holder, borrowed, Outcome.UNCHANGED);
            }
            // Last guard. The editor refuses a full group before it reaches this point, so
            // nothing is ever written by halves.
            if (recipes.size() >= maxRecipes()) {
                return giveBack(grid, source, holder, borrowed, Outcome.FULL);
            }
            recipes.add(newRecipe);
        }

        recipeList.setRecipeList(recipes);
        holderItem.setRecipeList(holder, recipeList);
        machine.setPatternStack(holder);
        if (borrowed) {
            result.supplied++;
        }
        return Outcome.CHANGED;
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
