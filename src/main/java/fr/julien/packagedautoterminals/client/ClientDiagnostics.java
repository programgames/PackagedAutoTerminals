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
 * Vérifie que les ressources du mod sont bien vues par le jeu.
 *
 * <p>Le contrôle attend le premier tick client. Testé plus tôt, à
 * {@code FMLLoadCompleteEvent}, il donne un faux négatif : Minecraft recharge ses
 * ressources, donc ses traductions, APRÈS le chargement des mods.
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
                    "Les traductions du mod NE sont PAS chargees : {} reste brut.", key);
        } else {
            PackagedAutoTerminals.LOGGER.info("Traductions chargees : {} = {}", key, value);
        }

        ModContainer container = Loader.instance().getIndexedModList().get(Reference.MOD_ID);
        PackagedAutoTerminals.LOGGER.info("Source du mod : {}",
                container == null ? "introuvable" : container.getSource());

        PackagedAutoTerminals.LOGGER.info("Domaines de ressources : {}",
                Minecraft.getMinecraft().getResourceManager().getResourceDomains());

        for (String path : new String[] {"lang/en_us.lang", "lang/fr_fr.lang",
                                         "models/item/pat_terminal.json",
                                         "textures/items/pat_terminal.png"}) {
            ResourceLocation location = new ResourceLocation(Reference.MOD_ID, path);
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(location);
                PackagedAutoTerminals.LOGGER.info("  ressource TROUVEE : {}", location);
            } catch (IOException exception) {
                PackagedAutoTerminals.LOGGER.error("  ressource ABSENTE : {}", location);
            }
        }
    }
}
