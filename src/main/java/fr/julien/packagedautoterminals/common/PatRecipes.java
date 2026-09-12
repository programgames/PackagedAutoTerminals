package fr.julien.packagedautoterminals.common;

import java.util.Optional;

import appeng.api.AEApi;
import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.Reference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Recette de fabrication du terminal.
 *
 * <p>Elle est enregistrée **par le code**, et non en JSON. Motif : les objets d'AE2 se
 * distinguent par leur métadonnée, pas par leur nom. Un JSON devrait écrire ce nombre en
 * dur, et casserait à la moindre renumérotation. L'API d'AE2 rend la bonne pile, quelle que
 * soit la version.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PatRecipes {

    private PatRecipes() {}

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Optional<ItemStack> terminal = AEApi.instance().definitions().parts().terminal().maybeStack(1);
        Item holder = NetworkItems.findRecipeHolder();

        if (!terminal.isPresent() || holder == null) {
            PackagedAutoTerminals.LOGGER.warn(
                    "Recette du terminal non enregistree : terminal AE2 present={}, porte-recettes present={}",
                    terminal.isPresent(), holder != null);
            return;
        }

        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.fromStacks(terminal.get()));
        ingredients.add(Ingredient.fromStacks(new ItemStack(holder)));

        ResourceLocation name = new ResourceLocation(Reference.MOD_ID, "pat_terminal");
        ShapelessRecipes recipe = new ShapelessRecipes(Reference.MOD_ID,
                new ItemStack(PatItems.TERMINAL), ingredients);
        recipe.setRegistryName(name);
        event.getRegistry().register(recipe);
    }
}
