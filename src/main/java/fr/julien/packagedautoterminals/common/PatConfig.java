package fr.julien.packagedautoterminals.common;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Réglages du mod. Le fichier est écrit dans {@code config/packagedautoterminals.cfg}. */
@Config(modid = Reference.MOD_ID, name = Reference.MOD_ID)
public final class PatConfig {

    @Config.Comment({
            "Intervalle entre deux scans du reseau, en ticks.",
            "Le terminal n'envoie un paquet que si le contenu a change ; augmenter cette",
            "valeur soulage un serveur charge, au prix d'un affichage moins reactif."
    })
    @Config.RangeInt(min = 5, max = 200)
    @Config.LangKey("config.packagedautoterminals.refresh_ticks")
    public static int refreshTicks = 20;

    @Config.Comment({
            "Identifiant du mode du terminal dans AE2 Wireless Universal Terminal.",
            "A changer seulement en cas de conflit avec un autre mod tiers : deux mods qui",
            "choisissent le meme identifiant se remplacent l'un l'autre."
    })
    @Config.RangeInt(min = 0, max = 127)
    @Config.LangKey("config.packagedautoterminals.wut_mode_id")
    public static int wutModeId = 41;

    @Config.Comment("Affiche l'onglet Machines et le diagnostic des recettes orphelines.")
    @Config.LangKey("config.packagedautoterminals.machines_tab")
    public static boolean machinesTab = true;

    @Config.Comment({
            "Nombre maximal d'objets listes dans une infobulle de recette.",
            "Au-dela, l'infobulle affiche le nombre restant."
    })
    @Config.RangeInt(min = 3, max = 32)
    @Config.LangKey("config.packagedautoterminals.tooltip_stacks")
    public static int tooltipStacks = 8;

    private PatConfig() {}

    @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
    public static final class Handler {
        private Handler() {}

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Reference.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
