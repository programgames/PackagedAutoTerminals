package fr.julien.packagedautoterminals;

import fr.julien.packagedautoterminals.client.PatKeyBindings;
import fr.julien.packagedautoterminals.client.PatKeyHandler;
import fr.julien.packagedautoterminals.common.PatDiagnostics;
import fr.julien.packagedautoterminals.integration.wut.WutEventHandler;
import fr.julien.packagedautoterminals.integration.wut.WutSupport;
import fr.julien.packagedautoterminals.common.PatItems;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Terminaux AE2 pour consulter et modifier les recettes de PackagedAuto à distance.
 *
 * <p>Le mod cible les machines qui implémentent {@code IPackageProvidingMachine} :
 * Packager, Unpackager et Packaging Provider. Ce sont les seuls blocs qui portent un
 * Package Recipe Holder, donc des recettes modifiables. Voir docs/PACKAGEDAUTO-MODEL.md.
 */
@Mod(
        modid = Reference.MOD_ID,
        name = Reference.MOD_NAME,
        version = Reference.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        // `after` sans `required` : AE2WUT reste facultatif, mais il doit se charger avant
        // nous. Ses classes de recette lisent notre objet dès l'enregistrement.
        dependencies = "required-after:" + Reference.AE2
                + ";required-after:" + Reference.PACKAGED_AUTO
                + ";after:" + Reference.AE2WUT
)
public class PackagedAutoTerminals {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @Mod.Instance(Reference.MOD_ID)
    public static PackagedAutoTerminals instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} : pre-init", Reference.MOD_NAME, Reference.VERSION);
        PatItems.registerPartModels();
        PatItems.registerWirelessHandler();
        PatNetwork.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new PatGuiHandler());
        if (event.getSide().isClient()) {
            registerKeys();
        }
        if (WutSupport.isLoaded()) {
            MinecraftForge.EVENT_BUS.register(WutEventHandler.class);
            LOGGER.info("{} : AE2WUT detecte, mode {}", Reference.MOD_NAME, WutSupport.mode());
        }
        LOGGER.info("{} : init", Reference.MOD_NAME);
    }

    /**
     * Déclare la touche d'ouverture, côté client seulement.
     *
     * <p>La méthode est isolée pour que la machine virtuelle ne charge jamais les classes
     * clientes sur un serveur dédié. Le test de côté dans {@code init} suffit à l'éviter,
     * car Java ne résout une classe qu'au moment où le code qui l'utilise s'exécute.
     *
     * <p>Elle ne porte **pas** {@code @SideOnly} : FML retire ces méthodes du serveur, et
     * l'appel resté dans {@code init} pointerait alors vers rien. Le test de côté suffit.
     */
    private static void registerKeys() {
        PatKeyBindings.register();
        MinecraftForge.EVENT_BUS.register(PatKeyHandler.class);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        PatDiagnostics.logRecipeTypes();
    }
}
