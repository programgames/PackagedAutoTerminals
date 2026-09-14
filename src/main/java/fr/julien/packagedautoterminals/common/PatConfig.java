package fr.julien.packagedautoterminals.common;

import fr.julien.packagedautoterminals.Reference;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Mod settings. The file is written to {@code config/packagedautoterminals.cfg}. */
@Config(modid = Reference.MOD_ID, name = Reference.MOD_ID)
public final class PatConfig {

    @Config.Comment({
            "Interval between two network scans, in ticks.",
            "The terminal only sends a packet when the contents change; raising this value",
            "relieves a busy server, at the cost of a less responsive display."
    })
    @Config.RangeInt(min = 5, max = 200)
    @Config.LangKey("config.packagedautoterminals.refresh_ticks")
    public static int refreshTicks = 20;

    @Config.Comment({
            "Mode id of the terminal inside AE2 Wireless Universal Terminal.",
            "Change it only on a conflict with another third-party mod: two mods that pick",
            "the same id replace each other."
    })
    @Config.RangeInt(min = 0, max = 127)
    @Config.LangKey("config.packagedautoterminals.wut_mode_id")
    public static int wutModeId = 41;

    @Config.Comment({
            "Energy drawn by the wireless terminal, in AE per tick.",
            "The wired terminal draws nothing extra: the grid already pays for its node.",
            "At zero, the wireless terminal becomes free."
    })
    @Config.RangeDouble(min = 0.0, max = 1000.0)
    @Config.LangKey("config.packagedautoterminals.wireless_power")
    public static double wirelessPowerPerTick = 1.0;

    @Config.Comment("Shows the Machines tab and the diagnostic for orphan recipes.")
    @Config.LangKey("config.packagedautoterminals.machines_tab")
    public static boolean machinesTab = true;

    @Config.Comment({
            "Maximum number of items listed in a recipe tooltip.",
            "Beyond that, the tooltip shows how many are left."
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
