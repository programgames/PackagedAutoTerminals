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
 * AE2 terminals to browse and edit PackagedAuto recipes remotely.
 *
 * <p>The mod targets machines that implement {@code IPackageProvidingMachine}:
 * Packager, Unpackager and Packaging Provider. These are the only blocks that hold a
 * Package Recipe Holder, hence editable recipes. See docs/PACKAGEDAUTO-MODEL.md.
 */
@Mod(
        modid = Reference.MOD_ID,
        name = Reference.MOD_NAME,
        version = Reference.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        // `after` without `required`: AE2WUT stays optional, but it must load before us.
        // Its recipe classes read our item as soon as registration happens.
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
        LOGGER.info("{} {}: pre-init", Reference.MOD_NAME, Reference.VERSION);
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
            LOGGER.info("{}: AE2WUT detected, mode {}", Reference.MOD_NAME, WutSupport.mode());
        }
        LOGGER.info("{}: init", Reference.MOD_NAME);
    }

    /**
     * Registers the opening key, on the client side only.
     *
     * <p>The method is isolated so that the virtual machine never loads client classes on a
     * dedicated server. The side check in {@code init} is enough to avoid that, because Java
     * only resolves a class when the code that uses it actually runs.
     *
     * <p>It does **not** carry {@code @SideOnly}: FML strips such methods from the server,
     * and the call left in {@code init} would then point at nothing. The side check suffices.
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
