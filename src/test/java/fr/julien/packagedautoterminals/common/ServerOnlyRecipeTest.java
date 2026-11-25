package fr.julien.packagedautoterminals.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.IRecipeType;
import thelm.packagedauto.api.MiscUtil;
import thelm.packagedauto.api.RecipeTypeRegistry;
import thelm.packagedauto.recipe.RecipeTypeCrafting;

/**
 * A crafting recipe that only the server knows, such as one added by a server script.
 *
 * <p>The client reads it with the real {@code RecipeInfoCrafting}, which finds no
 * {@code IRecipe} here, and drops its {@code Recipe} key when it writes it again. The server
 * then took the rewrite for another recipe, and refused to edit or delete it.
 */
public class ServerOnlyRecipeTest {

    @Test
    public void theClientNamesARecipeItCouldNotRebuildWithTheTagOfTheServer() {
        RecipeTypeRegistry.registerRecipeType(RecipeTypeCrafting.INSTANCE);
        ProviderSnapshot server = new ProviderSnapshot();
        server.recipes.add(new ServerOnlyCrafting());
        NBTTagCompound wrapper = new NBTTagCompound();
        ProviderSnapshot.writeAll(Collections.singletonList(server), wrapper);

        List<ProviderSnapshot> client = ProviderSnapshot.readAll(wrapper);
        IRecipeInfo rebuilt = client.get(0).recipes.get(0);

        assertFalse(rebuilt.isValid());
        assertEquals(MiscUtil.writeRecipeToNBT(new NBTTagCompound(), server.recipes.get(0)),
                ProviderSnapshot.tagOf(client, rebuilt));
    }

    /** What {@code RecipeInfoCrafting.writeToNBT} writes on a server that knows the recipe. */
    private static final class ServerOnlyCrafting extends FakeRecipe {

        ServerOnlyCrafting() {
            super("server only");
        }

        @Override
        public IRecipeType getRecipeType() {
            return RecipeTypeCrafting.INSTANCE;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setString("Recipe", "crafttweaker:server_only");
            tag.setTag("Matrix", new NBTTagList());
            return tag;
        }
    }
}
