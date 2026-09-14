package fr.julien.packagedautoterminals.client;

import java.io.IOException;

import fr.julien.packagedautoterminals.PackagedAutoTerminals;
import fr.julien.packagedautoterminals.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Checks that the mod resources are actually seen by the game.
 *
 * <p>The check waits for the first client tick. Run earlier, at
 * {@code FMLLoadCompleteEvent}, it gives a false negative: Minecraft reloads its resources,
 * hence its translations, AFTER the mods are loaded.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class ClientDiagnostics {

    private static boolean done;

    private ClientDiagnostics() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) {
            return;
        }
        done = true;
        MinecraftForge.EVENT_BUS.unregister(ClientDiagnostics.class);

        String key = "item." + Reference.MOD_ID + ".pat_terminal.name";
        String value = I18n.format(key);
        if (key.equals(value)) {
            PackagedAutoTerminals.LOGGER.error(
                    "Mod translations are NOT loaded: {} stays raw.", key);
        } else {
            PackagedAutoTerminals.LOGGER.info("Translations loaded: {} = {}", key, value);
        }

        ModContainer container = Loader.instance().getIndexedModList().get(Reference.MOD_ID);
        PackagedAutoTerminals.LOGGER.info("Mod source: {}",
                container == null ? "not found" : container.getSource());

        PackagedAutoTerminals.LOGGER.info("Resource domains: {}",
                Minecraft.getMinecraft().getResourceManager().getResourceDomains());

        for (String path : new String[] {"lang/en_us.lang", "lang/fr_fr.lang",
                                         "models/item/pat_terminal.json",
                                         "textures/items/part/pat_terminal.png",
                "textures/parts/pat_terminal_bright.png",
                "textures/parts/pat_terminal_medium.png",
                "textures/parts/pat_terminal_dark.png"}) {
            ResourceLocation location = new ResourceLocation(Reference.MOD_ID, path);
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(location);
                PackagedAutoTerminals.LOGGER.info("  resource FOUND: {}", location);
            } catch (IOException exception) {
                PackagedAutoTerminals.LOGGER.error("  resource MISSING: {}", location);
            }
        }
    }
}
