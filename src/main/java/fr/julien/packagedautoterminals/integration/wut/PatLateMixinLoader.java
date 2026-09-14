package fr.julien.packagedautoterminals.integration.wut;

import java.util.Collections;
import java.util.List;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

/**
 * Déclare notre configuration de mixin à MixinBooter.
 *
 * <p>MixinBooter parcourt les jars des mods et instancie tout ce qui implémente cette
 * interface. Aucune entrée de manifeste, aucun coremod : c'est la voie que suit AE2WUT
 * lui-même, avec {@code WUTLateMixinLoader}.
 *
 * <p>Cette classe ne se charge que si MixinBooter est présent. Sans lui, elle n'est jamais
 * lue, et le mod démarre sans intégration. Sans AE2WUT, la configuration reste en file et ne
 * s'applique pas.
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
