package fr.julien.packagedautoterminals;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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

    public static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(Reference.MOD_NAME);

    @Mod.Instance(Reference.MOD_ID)
    public static PackagedAutoTerminals instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} : pre-init", Reference.MOD_NAME, Reference.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("{} : init", Reference.MOD_NAME);
    }
}
