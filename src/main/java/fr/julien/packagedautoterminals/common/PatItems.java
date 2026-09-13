package fr.julien.packagedautoterminals.common;

import appeng.api.AEApi;
import fr.julien.packagedautoterminals.Reference;
import fr.julien.packagedautoterminals.item.ItemPatTerminal;
import fr.julien.packagedautoterminals.item.ItemWirelessPatTerminal;
import fr.julien.packagedautoterminals.part.PartPatTerminal;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PatItems {

    public static final CreativeTabs TAB = new CreativeTabs(Reference.MOD_ID) {
        // Mappings snapshot_20171003 : la méthode s'appelle getTabIconItem, et non createIcon.
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(TERMINAL);
        }
    };

    public static final ItemPatTerminal TERMINAL = new ItemPatTerminal();
    public static final ItemWirelessPatTerminal WIRELESS_TERMINAL = new ItemWirelessPatTerminal();

    private PatItems() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        TERMINAL.setRegistryName(Reference.MOD_ID, "pat_terminal");
        TERMINAL.setUnlocalizedName(Reference.MOD_ID + ".pat_terminal");
        TERMINAL.setCreativeTab(TAB);
        event.getRegistry().register(TERMINAL);

        WIRELESS_TERMINAL.setRegistryName(Reference.MOD_ID, "wireless_pat_terminal");
        WIRELESS_TERMINAL.setUnlocalizedName(Reference.MOD_ID + ".wireless_pat_terminal");
        WIRELESS_TERMINAL.setCreativeTab(TAB);
        event.getRegistry().register(WIRELESS_TERMINAL);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(TERMINAL, 0,
                new ModelResourceLocation(TERMINAL.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(WIRELESS_TERMINAL, 0,
                new ModelResourceLocation(WIRELESS_TERMINAL.getRegistryName(), "inventory"));
    }

    /**
     * Déclare le terminal sans fil au registre d'AE2.
     *
     * <p>Cet enregistrement sert à la liaison, à l'énergie et à la portée. Il ne sert **pas**
     * à ouvrir la fenêtre : voir l'explication dans {@link ItemWirelessPatTerminal}.
     */
    public static void registerWirelessHandler() {
        AEApi.instance().registries().wireless().registerWirelessHandler(WIRELESS_TERMINAL);
    }

    /**
     * AE2 ne cuit que les modèles de part qu'on lui déclare. Sans cet appel, la part est
     * invisible dans le monde, sans aucune erreur dans le journal.
     */
    public static void registerPartModels() {
        AEApi.instance().registries().partModels()
                .registerModels(PartPatTerminal.MODEL_OFF, PartPatTerminal.MODEL_ON);
    }
}
