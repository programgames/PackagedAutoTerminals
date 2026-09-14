package fr.julien.packagedautoterminals.mixin.ae2wut;

import java.util.Map;

import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fait absorber notre terminal sans fil par le terminal universel.
 *
 * <p>Une seule greffe suffit. {@code AllWUTRecipe.getIngredient()} rend la table
 * « mode → objet à consommer ». Deux choses la lisent, et toutes deux sont génériques :
 *
 * <ol>
 *   <li>{@code DynamicUniversalRecipe.registerRecipes()} crée une recette sans forme par
 *       entrée, qui ajoute le mode au tableau {@code modes} du terminal universel ;
 *   <li>{@code AllWUTRecipe.reciperRegister()} bâtit la recette « tout en un ».
 * </ol>
 *
 * <p>La table rendue est une {@code HashMap} modifiable : on y écrit directement, plutôt que
 * d'en rendre une copie, pour que les deux lecteurs voient la même chose.
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
