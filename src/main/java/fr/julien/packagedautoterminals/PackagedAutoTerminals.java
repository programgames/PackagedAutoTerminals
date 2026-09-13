package fr.julien.packagedautoterminals;

import fr.julien.packagedautoterminals.common.PatDiagnostics;
import fr.julien.packagedautoterminals.common.PatItems;
import fr.julien.packagedautoterminals.network.PatNetwork;
import fr.julien.packagedautoterminals.proxy.PatGuiHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
        dependencies = "required-after:" + Reference.AE2 + ";required-after:" + Reference.PACKAGED_AUTO
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
        LOGGER.info("{} : init", Reference.MOD_NAME);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        PatDiagnostics.logRecipeTypes();
    }
}
