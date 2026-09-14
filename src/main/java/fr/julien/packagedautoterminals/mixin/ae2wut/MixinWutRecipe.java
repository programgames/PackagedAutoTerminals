package fr.julien.packagedautoterminals.mixin.ae2wut;

import java.util.Map;

import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the universal terminal absorb our wireless terminal.
 *
 * <p>A single injection is enough. {@code AllWUTRecipe.getIngredient()} returns the
 * "mode to consumed item" table. Two things read it, and both are generic:
 *
 * <ol>
 *   <li>{@code DynamicUniversalRecipe.registerRecipes()} creates one shapeless recipe per
 *       entry, which adds the mode to the {@code modes} array of the universal terminal;
 *   <li>{@code AllWUTRecipe.reciperRegister()} builds the "all in one" recipe.
 * </ol>
 *
 * <p>The returned table is a mutable {@code HashMap}: we write into it directly, rather than
 * returning a copy, so that both readers see the same thing.
 */
@Mixin(targets = "com.circulation.ae2wut.recipes.AllWUTRecipe", remap = false)
public abstract class MixinWutRecipe {

    @Inject(method = "getIngredient", at = @At("RETURN"), require = 1)
    private static void packagedautoterminals$addIngredient(
            CallbackInfoReturnable<Map<Integer, ItemStack>> callback) {
        Map<Integer, ItemStack> ingredients = callback.getReturnValue();
        if (ingredients != null) {
            ingredients.put(WutSupport.mode(), WutSupport.terminal());
        }
    }
}
