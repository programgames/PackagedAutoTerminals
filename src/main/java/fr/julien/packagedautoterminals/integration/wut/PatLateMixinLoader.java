package fr.julien.packagedautoterminals.integration.wut;

import java.util.Collections;
import java.util.List;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

/**
 * Declares our mixin configuration to MixinBooter.
 *
 * <p>MixinBooter walks the mod jars and instantiates everything that implements this
 * interface. No manifest entry, no coremod: this is the path AE2WUT itself follows, with
 * {@code WUTLateMixinLoader}.
 *
 * <p>This class is only loaded when MixinBooter is present. Without it, the class is never
 * read, and the mod starts with no integration. Without AE2WUT, the configuration stays
 * queued and is not applied.
 */
public class PatLateMixinLoader implements ILateMixinLoader {

    private static final String CONFIG = "mixins." + Reference.MOD_ID + ".ae2wut.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        return CONFIG.equals(mixinConfig) && Loader.isModLoaded(Reference.AE2WUT);
    }
}
